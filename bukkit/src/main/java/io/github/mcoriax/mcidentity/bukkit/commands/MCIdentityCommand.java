package io.github.mcoriax.mcidentity.bukkit.commands;

import io.github.mcoriax.mcidentity.bukkit.permission.Permissions;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.*;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityUnequipEvent;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.commands.utils.*;
import io.github.mcoriax.mcidentity.bukkit.listeners.utils.EquipBaseProfession;
import io.github.mcoriax.mcidentity.bukkit.util.ItemStackSerializer;
import io.github.mcoriax.mcidentity.bukkit.util.MessageComponents;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class MCIdentityCommand implements CommandExecutor {

    /**
     * Profession argument asking for a random non-base class.
     */
    private static final String RANDOM_PROFESSION = "random";

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;
    /**
     * The NamespacedKey for identity UUID.
     */
    private final NamespacedKey identityKey;
    /**
     * The NamespacedKey for profession code.
     */
    private final NamespacedKey professionKey;
    /**
     * The NamespacedKey for identity texture.
     */
    private final NamespacedKey textureKey;
    /**
     * The NamespacedKey for identification status.
     */
    private final NamespacedKey unidentifiedKey;

    /**
     * Creates a new MCIdentityCommand.
     * @param plugin The plugin instance.
     */
    public MCIdentityCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.identityKey = new NamespacedKey(plugin, "identity_uuid");
        this.professionKey = new NamespacedKey(plugin, "profession_code");
        this.textureKey = new NamespacedKey(plugin, "identity_texture");
        this.unidentifiedKey = new NamespacedKey(plugin, "is_unidentified");
    }

    /**
     * Dispatches an {@code /identity} invocation to its sub-command handler.
     * @param sender The command sender; only players are accepted.
     * @param command The command being run.
     * @param label The alias the sender typed.
     * @param args The sub-command and its arguments.
     * @return True when the invocation was handled, false to print the usage line.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            openIdentityGui(player);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> handled(() -> sendHelp(player));
            case "wiki" -> handled(() -> SendWiki.execute(player));
            case "get" -> handleGet(player, args);
            case "unequip" -> handleUnequip(player);
            case "check" -> handleCheck(player);
            case "alias" -> handled(() -> HandleAliasCommand.execute(plugin, player, args));
            case "stats" -> handled(() -> handleStatsCommand(player, args));
            case "base" -> handled(() -> HandleBaseCommand.execute(plugin, player, args));
            default -> false;
        };
    }

    /**
     * Runs a sub-command that always counts as handled.
     * @param subCommand The sub-command body.
     * @return Always true, so the usage line is not printed.
     */
    private static boolean handled(Runnable subCommand) {
        subCommand.run();
        return true;
    }

    /**
     * Handles {@code /identity get}, which mints an identity item for an admin.
     * @param player The player who issued the command.
     * @param args The command arguments.
     * @return Always true; the usage line is sent from here when needed.
     */
    private boolean handleGet(Player player, String[] args) {
        if (!player.hasPermission(Permissions.ADMIN)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /identity get <profession|random> [head texture]");
            return true;
        }

        String professionCode = args[1].toLowerCase();
        String customTexture = args.length >= 3 ? args[2] : null;

        // The permanent base profession can never become an item.
        if (BaseProfession.isBaseProfession(professionCode)) {
            player.sendMessage(ChatColor.RED + "The base Player profession cannot be turned into an identity item!");
            return true;
        }

        if (professionCode.equalsIgnoreCase(RANDOM_PROFESSION)) {
            createRandomIdentityItem(player, customTexture);
            return true;
        }

        MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
            Optional<Profession> optProfession = professions.stream().filter(p -> p.getSystemCode().equalsIgnoreCase(professionCode)).findFirst();
            if (optProfession.isEmpty()) {
                runForPlayer(player, () -> player.sendMessage(ChatColor.RED + MessageComponents.value(
                        "mcidentity.command.error.profession_not_found.prefix",
                        "Error: Profession '",
                        professionCode,
                        "mcidentity.command.error.profession_not_found.suffix",
                        "' does not exist!"
                )));
                return;
            }

            createIdentityItem(player, optProfession.get(), customTexture);
        });
        return true;
    }

    /**
     * Handles {@code /identity unequip}, returning the player to the base profession.
     * @param player The player who issued the command.
     * @return Always true.
     */
    private boolean handleUnequip(Player player) {
        if (MCIdentityManager.isOnSwapCooldown(player.getUniqueId())) {
            String remaining = MCIdentityManager.formatTime(MCIdentityManager.getRemainingSwapCooldownMillis(player.getUniqueId()));
            player.sendMessage(ChatColor.RED + MessageComponents.value(
                    "mcidentity.command.error.swap_cooldown.prefix",
                    "You must wait ",
                    remaining,
                    "mcidentity.command.error.swap_cooldown.suffix",
                    " before swapping identities!"
            ));
            return true;
        }

        MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
            if (optSession.isEmpty()) {
                runForPlayer(player, () -> player.sendMessage(ChatColor.RED + "You are not currently wearing an identity!"));
                return;
            }

            IdentitySession session = optSession.get();
            UUID identityUuid = session.getIdentityUniqueId();
            String base64 = session.getContentKey();

            // Base identities are permanently bound and switched through the
            // Player Identity GUI, never unequipped: the primary is the
            // fallback state, and alternatives (of any profession) cannot be
            // dropped or deleted.
            if (BaseProfession.identityIndex(player.getUniqueId(), identityUuid,
                    MCIdentityManager.getCachedBaseIdentitySlots(player.getUniqueId())) >= 0) {
                runForPlayer(player, () -> player.sendMessage(ChatColor.RED + "Base identities cannot be unequipped; switch between them in the Player Identity GUI."));
                return;
            }

            Schedulers.forEntity(player).run(plugin, st1 -> fireUnequip(player, identityUuid, base64), null);
        });
        return true;
    }

    /**
     * Announces the unequip and, unless a listener cancels it, restores the
     * base profession and hands the identity item back.
     * @param player The player unequipping.
     * @param identityUuid The identity being unequipped.
     * @param base64 The serialized identity item, or null when there is none.
     */
    private void fireUnequip(Player player, UUID identityUuid, String base64) {
        MCIdentityUnequipEvent unequipEvent = new MCIdentityUnequipEvent(player, identityUuid);
        Bukkit.getPluginManager().callEvent(unequipEvent);

        if (unequipEvent.isCancelled()) {
            return;
        }

        MCIdentityManager.setSwapCooldown(player.getUniqueId());

        if (base64 == null) {
            restoreBaseProfession(player, null, identityUuid);
            return;
        }

        Schedulers.async().runNow(plugin, st2 ->
                restoreBaseProfession(player, recoverItem(player, base64), identityUuid));
    }

    /**
     * Deserializes the stored identity item, warning the player when it cannot be read.
     * @param player The player unequipping.
     * @param base64 The serialized identity item.
     * @return The recovered item, or null when it could not be read.
     */
    private ItemStack recoverItem(Player player, String base64) {
        try {
            return ItemStackSerializer.deserialize(base64);
        } catch (Exception e) {
            runForPlayer(player, () -> player.sendMessage(ChatColor.RED + "Failed to recover identity item!"));
            plugin.getLogger().log(Level.WARNING,
                "Failed to recover the identity item for " + player.getName(), e);
            return null;
        }
    }

    /**
     * Puts the player back on the base profession and logs the unequip.
     * @param player The player unequipping.
     * @param recovered The identity item to hand back, or null.
     * @param identityUuid The identity being unequipped.
     */
    private void restoreBaseProfession(Player player, ItemStack recovered, UUID identityUuid) {
        EquipBaseProfession.execute(plugin, player, recovered,
                ChatColor.GREEN + "Identity unequipped. You are back to the base Player profession.", false);
        MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.UNEQUIP);
    }

    /**
     * Handles {@code /identity check}, describing the held or worn identity.
     * @param player The player who issued the command.
     * @return Always true.
     */
    private boolean handleCheck(Player player) {
        String uuidStr = heldIdentityUuid(player.getInventory().getItemInMainHand());
        if (uuidStr != null) {
            fetchAndDisplayIdentityInfo(player, UUID.fromString(uuidStr));
            return true;
        }

        Optional<ActiveIdentitySession> optRamSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optRamSession.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not wearing an identity and not holding an identity key!");
            return true;
        }

        ActiveIdentitySession session = optRamSession.get();
        renderIdentityInfo(player, session.identityUuid(), session.identityKey(), session.properties(), session.stats());
        return true;
    }

    /**
     * Reads the identity UUID stamped on a held item.
     * @param item The item in the player's main hand.
     * @return The identity UUID as text, or null when the item carries none.
     */
    private String heldIdentityUuid(ItemStack item) {
        if (item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
    }

    /**
     * Runs an action on the thread that owns the player.
     * @param player The player the action touches.
     * @param action The action to run.
     */
    private void runForPlayer(Player player, Runnable action) {
        Schedulers.forEntity(player).run(plugin, st -> action.run(), null);
    }

    /**
     * Handles the stats sub-command.
     * @param player The player who issued the command.
     * @param args The command arguments.
     */
    private void handleStatsCommand(Player player, String[] args) {
        HandleStatsCommand.execute(plugin, player, args);
    }

    /**
     * Sends help information to the player.
     * @param player The player to send help to.
     */
    private void sendHelp(Player player) {
        SendHelp.execute(player);
    }

    /**
     * Opens the main identity GUI for the player.
     * @param player The player to open the GUI for.
     */
    public void openIdentityGui(Player player) {
        OpenIdentityGui.execute(plugin, player);
    }

    /**
     * Fetches and displays identity information for a specific UUID.
     * @param player The player to display info to.
     * @param uuid The identity UUID.
     */
    private void fetchAndDisplayIdentityInfo(Player player, UUID uuid) {
        FetchAndDisplayIdentityInfo.execute(plugin, player, uuid);
    }

    /**
     * Renders identity information for the player.
     * @param player The player to display info to.
     * @param uuid The identity UUID.
     * @param key The identity key.
     * @param props The identity properties.
     * @param stats The identity stats.
     */
    private void renderIdentityInfo(Player player, UUID uuid, IdentityKey key, IdentityProperties props, IdentityStats stats) {
        RenderIdentityInfo.execute(player, uuid, key, props, stats);
    }

    /**
     * Creates a random identity item for the player.
     * @param player The player to give the item to.
     * @param customTexture The custom texture (optional).
     */
    private void createRandomIdentityItem(Player player, String customTexture) {
        CreateRandomIdentityItem.execute(plugin, player, customTexture, professionKey, unidentifiedKey);
    }

    /**
     * Creates a specific identity item for the player.
     * @param player The player to give the item to.
     * @param profession The profession for the identity.
     * @param customTexture The custom texture (optional).
     */
    private void createIdentityItem(Player player, Profession profession, String customTexture) {
        CreateIdentityItem.execute(plugin, player, profession, customTexture, textureKey, professionKey, unidentifiedKey);
    }
}
