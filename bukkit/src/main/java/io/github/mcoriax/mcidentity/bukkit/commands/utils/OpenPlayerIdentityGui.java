package io.github.mcoriax.mcidentity.bukkit.commands.utils;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import io.github.mcoriax.mcidentity.bukkit.util.HeadTexture;
import io.github.mcoriax.mcidentity.bukkit.util.IdentityItemRenderer;
import io.github.mcoriax.mcidentity.bukkit.util.SkullTextures;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opens the paginated "Player Identity" GUI listing a player's base
 * identities: the always-available primary "player" identity plus every
 * unlocked alternative identity (each of its own non-player profession).
 * Clicking an identity head equips it; the currently equipped base identity is
 * marked in its lore.
 *
 * <p>Each alternative head shows a texture chosen at random from its own
 * profession's available textures (stable per identity), looked up before the
 * GUI is built. Alternative identities fill the content rows from
 * slot 0, up to {@link #ALTERNATIVES_PER_PAGE} per page. The GUI scales
 * dynamically: a single-page view only shows as many content rows as needed,
 * while any multi-page view uses the full six rows so the controls keep their
 * fixed places — the Back button at slot 45, the primary identity at slot 49,
 * and "Previous Page"/"Next Page" at slots 48 and 50. Pagination buttons appear
 * only when the corresponding page exists.</p>
 */
public class OpenPlayerIdentityGui {

    /**
     * The GUI title prefix; the page number is appended after it.
     */
    public static final String TITLE_PREFIX = "Player Identity - Page ";

    /**
     * Alternative identities shown per page (the five content rows).
     */
    public static final int ALTERNATIVES_PER_PAGE = 45;

    /**
     * Opens the GUI at the given page.
     * @param plugin The plugin instance.
     * @param player The player to open the GUI for.
     * @param page The 1-based page number; out-of-range values are clamped.
     */
    public static void execute(JavaPlugin plugin, Player player, int page) {
        UUID playerUuid = player.getUniqueId();
        MCIdentityProvider.instance.getBaseIdentitySlots(playerUuid).thenAccept(slots -> {
            int totalPages = Math.max(1, (slots + ALTERNATIVES_PER_PAGE - 1) / ALTERNATIVES_PER_PAGE);
            int currentPage = Math.min(Math.max(page, 1), totalPages);

            int firstIndex = (currentPage - 1) * ALTERNATIVES_PER_PAGE + 1;
            int alternativesOnPage = Math.min(slots - (firstIndex - 1), ALTERNATIVES_PER_PAGE);

            // Resolve each alternative's profession, alias, level, and head
            // texture before building the GUI, since those lookups are async. A
            // failed lookup simply leaves that head with its defaults.
            Map<Integer, Rendered> rendered = new ConcurrentHashMap<>();
            List<CompletableFuture<?>> lookups = new ArrayList<>();
            for (int i = 0; i < alternativesOnPage; i++) {
                lookups.add(resolveAlternative(playerUuid, firstIndex + i, rendered));
            }

            // The primary identity levels from passive playtime, so its level is
            // read the same way rather than assumed.
            CompletableFuture<Optional<IdentityProperties>> primaryProps =
                MCIdentityProvider.instance.getProperties(playerUuid).exceptionally(ex -> Optional.empty());
            lookups.add(primaryProps);

            Page pageInfo = new Page(currentPage, totalPages, firstIndex, alternativesOnPage);
            CompletableFuture.allOf(lookups.toArray(CompletableFuture[]::new)).thenAccept(v ->
                Schedulers.forEntity(player).run(plugin, st ->
                    player.openInventory(buildGui(plugin, player, pageInfo, rendered, primaryProps)), null));
        });
    }

    /**
     * Which slice of a player's alternatives one GUI page shows.
     *
     * @param currentPage The 1-based page being shown.
     * @param totalPages How many pages the player's slots span.
     * @param firstIndex The 1-based index of this page's first alternative.
     * @param alternativesOnPage How many alternatives this page shows.
     */
    private record Page(int currentPage, int totalPages, int firstIndex, int alternativesOnPage) {
    }

    /**
     * Reads one alternative's profession, alias, level, and texture into the
     * render map, leaving it absent when the lookup finds nothing or fails.
     * @param playerUuid The owning player.
     * @param index The 1-based alternative index.
     * @param rendered The map every resolved alternative is written into.
     * @return The lookup to wait on.
     */
    private static CompletableFuture<?> resolveAlternative(UUID playerUuid, int index, Map<Integer, Rendered> rendered) {
        UUID altUuid = BaseProfession.identityUuid(playerUuid, index);
        return MCIdentityProvider.instance.getIdentityKey(altUuid)
            .thenCompose(optKey -> {
                if (optKey.isEmpty()) {
                    return CompletableFuture.completedFuture(null);
                }
                String professionCode = optKey.get().getProfessionCode();
                return MCIdentityProvider.instance.getProperties(altUuid).thenAccept(optProps ->
                    rendered.put(index, new Rendered(
                        professionCode,
                        optProps.map(IdentityProperties::getAlias).orElse(null),
                        optProps.map(IdentityProperties::getLevel).orElse(1),
                        professionTexture(altUuid, professionCode))));
            }).exceptionally(ex -> null);
    }

    /**
     * Builds the page: one head per alternative, then the control row.
     * @param plugin The plugin instance.
     * @param player The player the GUI belongs to.
     * @param pageInfo Which slice of alternatives to show.
     * @param rendered The resolved alternatives.
     * @param primaryProps The primary identity's properties lookup.
     * @return The inventory to open.
     */
    private static Inventory buildGui(JavaPlugin plugin, Player player, Page pageInfo,
                                      Map<Integer, Rendered> rendered,
                                      CompletableFuture<Optional<IdentityProperties>> primaryProps) {
        UUID playerUuid = player.getUniqueId();

        // Content rows scale with the page; multi-page views keep the
        // full six rows so the controls stay at slots 45/48/49/50.
        int contentRows = (Math.max(pageInfo.alternativesOnPage(), 0) + 8) / 9;
        int rows = pageInfo.totalPages() > 1 ? 6 : contentRows + 1;
        int controlRowStart = (rows - 1) * 9;

        // The equipped base identity is marked in its lore.
        UUID equippedUuid = MCIdentityManager.getSession(playerUuid)
            .map(ActiveIdentitySession::identityUuid)
            .orElse(null);

        Inventory gui = Bukkit.createInventory(player, rows * 9, TITLE_PREFIX + pageInfo.currentPage());

        for (int i = 0; i < pageInfo.alternativesOnPage(); i++) {
            int index = pageInfo.firstIndex() + i;
            gui.setItem(i, alternativeIdentityItem(plugin, playerUuid, index, equippedUuid, rendered.get(index)));
        }

        addControlRow(plugin, player, gui, pageInfo, controlRowStart, equippedUuid,
            primaryProps.getNow(Optional.empty()).map(IdentityProperties::getLevel).orElse(1));
        return gui;
    }

    /**
     * Fills the bottom row with the back button, the primary identity head, and
     * whichever page buttons this page needs.
     * @param plugin The plugin instance.
     * @param player The player the GUI belongs to.
     * @param gui The inventory being built.
     * @param pageInfo Which slice of alternatives is shown.
     * @param controlRowStart The first slot of the control row.
     * @param equippedUuid The currently equipped identity, or null.
     * @param primaryLevel The primary identity's level.
     */
    private static void addControlRow(JavaPlugin plugin, Player player, Inventory gui, Page pageInfo,
                                      int controlRowStart, UUID equippedUuid, int primaryLevel) {
        ItemStack filler = fillerPane();
        for (int slot = controlRowStart; slot < controlRowStart + 9; slot++) {
            gui.setItem(slot, filler);
        }

        gui.setItem(controlRowStart, CreateCustomHead.execute(
            HeadTexture.getBackButton(plugin), "mcidentity.gui.button.back", ChatColor.YELLOW + "Back"));
        gui.setItem(controlRowStart + 4, primaryIdentityItem(plugin, player, equippedUuid, primaryLevel));
        if (pageInfo.currentPage() > 1) {
            gui.setItem(controlRowStart + 3, pageButton(ChatColor.YELLOW + "Previous Page",
                "Go to page " + (pageInfo.currentPage() - 1)));
        }
        if (pageInfo.currentPage() < pageInfo.totalPages()) {
            gui.setItem(controlRowStart + 5, pageButton(ChatColor.YELLOW + "Next Page",
                "Go to page " + (pageInfo.currentPage() + 1)));
        }
    }

    /**
     * Picks a head texture at random from all of a profession's available
     * textures, rather than a fixed one. The choice is seeded by the identity
     * UUID, so each alternative keeps its own varied texture that stays stable
     * across GUI reopens instead of reshuffling every time.
     * @param identityUuid The alternative identity's UUID (the random seed).
     * @param professionCode The profession system code.
     * @return A randomly chosen skin texture, or null when the profession has
     *         no skins.
     */
    private static String professionTexture(UUID identityUuid, String professionCode) {
        Map<Integer, String> skins = MCIdentityManager.getProfessionSkins(professionCode);
        if (skins.isEmpty()) {
            return null;
        }
        // Order deterministically by skin index so the seeded pick is stable.
        List<String> textures = new ArrayList<>(new TreeMap<>(skins).values());
        long seed = identityUuid.getMostSignificantBits() ^ identityUuid.getLeastSignificantBits();
        return textures.get(new Random(seed).nextInt(textures.size()));
    }

    /**
     * The per-identity data a head is rendered from, resolved before the GUI is
     * built because each lookup is asynchronous.
     * @param professionCode The identity's profession system code.
     * @param alias The identity's alias, or null when it has none.
     * @param level The identity's current level.
     * @param texture The profession head texture, or null for none.
     */
    private record Rendered(String professionCode, String alias, int level, String texture) {
    }

    /**
     * Builds the primary identity button: the player's own head, since the
     * primary identity shares the player's UUID. Clicking it equips the
     * primary identity.
     *
     * <p>The primary identity is the base {@code player} profession, which
     * always goes by the player's Minecraft name and never carries an alias, so
     * this button keeps its fixed name rather than taking one.</p>
     *
     * @param plugin The plugin instance, owning the persistent data key.
     * @param player The owning player.
     * @param equippedUuid The currently equipped identity UUID, or null.
     * @param level The primary identity's current level.
     * @return The button item.
     */
    private static ItemStack primaryIdentityItem(JavaPlugin plugin, Player player, UUID equippedUuid, int level) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.GOLD + "Primary Identity");
            List<String> lore = new ArrayList<>(IdentityItemRenderer.lore(BaseProfession.CODE, level));
            lore.add(equippedLoreLine(player.getUniqueId(), equippedUuid));
            meta.setLore(lore);
            stampIdentity(plugin, meta, player.getUniqueId());
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Builds the button for one alternative identity, named by its alias and
     * textured with its own profession's head texture. Clicking it equips that
     * identity.
     * @param plugin The plugin instance, owning the persistent data key.
     * @param playerUuid The owning player's UUID.
     * @param index The alternative identity index, 1-based.
     * @param equippedUuid The currently equipped identity UUID, or null.
     * @param data The identity's resolved profession, alias, level, and
     *             texture, or null when the lookup failed.
     * @return The button item.
     */
    private static ItemStack alternativeIdentityItem(JavaPlugin plugin, UUID playerUuid, int index,
                                                     UUID equippedUuid, Rendered data) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            UUID identityUuid = BaseProfession.identityUuid(playerUuid, index);
            if (data != null && data.texture() != null && !data.texture().isEmpty()
                    && meta instanceof SkullMeta skullMeta) {
                SkullTextures.apply(skullMeta, data.texture());
            }

            // Both the name and the lore are composed here rather than written
            // and read back, so nothing ever derives meaning from the head's
            // own text.
            List<String> lore = new ArrayList<>();
            if (data != null) {
                meta.setDisplayName(IdentityItemRenderer.name(identityUuid, data.alias(), data.professionCode()));
                lore.addAll(IdentityItemRenderer.lore(data.professionCode(), data.level()));
            } else {
                // The identity could not be read; name it by its slot so the
                // head is still clickable rather than blank.
                meta.setDisplayName(ChatColor.AQUA + "Alternative Identity #" + index);
            }
            lore.add(equippedLoreLine(identityUuid, equippedUuid));
            meta.setLore(lore);
            stampIdentity(plugin, meta, identityUuid);
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Records which identity a head stands for in its persistent data
     * container, so a click is resolved from the item itself rather than from
     * its position or its text.
     * @param plugin The plugin instance, owning the key.
     * @param meta The head's item meta.
     * @param identityUuid The identity the head represents.
     */
    private static void stampIdentity(JavaPlugin plugin, ItemMeta meta, UUID identityUuid) {
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "identity_uuid"), PersistentDataType.STRING, identityUuid.toString());
    }

    /**
     * Builds the lore line telling whether this identity is the equipped one.
     * @param identityUuid The identity the button represents.
     * @param equippedUuid The currently equipped identity UUID, or null.
     * @return The lore line.
     */
    private static String equippedLoreLine(UUID identityUuid, UUID equippedUuid) {
        return identityUuid.equals(equippedUuid)
            ? ChatColor.GREEN + "Currently equipped."
            : ChatColor.YELLOW + "Click to equip.";
    }

    /**
     * Builds a pagination arrow button.
     * @param name The button display name.
     * @param loreLine The single lore line describing the target page.
     * @return The button item.
     */
    private static ItemStack pageButton(String name, String loreLine) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(ChatColor.GRAY + loreLine));
            arrow.setItemMeta(meta);
        }
        return arrow;
    }

    /**
     * Builds the tooltip-less filler pane for the control row.
     * @return The filler item.
     */
    private static ItemStack fillerPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setHideTooltip(true);
            pane.setItemMeta(meta);
        }
        return pane;
    }
}
