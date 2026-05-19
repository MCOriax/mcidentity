package io.gitlab.mcoriax.foliamc.commands;

import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.bukkit.event.MCIdentityUnequipEvent;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.commands.utils.*;
import io.gitlab.mcoriax.foliamc.util.ItemStackSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class MCIdentityCommand implements CommandExecutor {

    /**
     * The plugin instance.
     */
    private final MCIdentityPlugin plugin;
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
    public MCIdentityCommand(MCIdentityPlugin plugin) {
        this.plugin = plugin;
        this.identityKey = new NamespacedKey(plugin, "identity_uuid");
        this.professionKey = new NamespacedKey(plugin, "profession_code");
        this.textureKey = new NamespacedKey(plugin, "identity_texture");
        this.unidentifiedKey = new NamespacedKey(plugin, "is_unidentified");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.translatable("mcidentity.command.error.players_only", "This command can only be used by players.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            openIdentityGui(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("ability")) {
            if (args.length >= 3 && args[1].equalsIgnoreCase("get")) {
                if (!player.hasPermission("mcidentity.admin")) {
                    player.sendMessage(Component.translatable("mcidentity.command.error.no_permission", "You do not have permission to use this command!").color(NamedTextColor.RED));
                    return true;
                }

                String abilityCode = args[2].toLowerCase();
                MCIdentityProvider.instance.getAbilities().thenAccept(abilities -> {
                    Optional<Ability> optAbility = abilities.stream()
                            .filter(a -> a.getSystemCode().equalsIgnoreCase(abilityCode))
                            .findFirst();

                    if (optAbility.isEmpty()) {
                        player.getScheduler().run(plugin, task -> {
                            if (!plugin.isEnabled() || !player.isOnline()) return;
                            player.sendMessage(Component.translatable(
                                    "mcidentity.command.error.ability_not_found",
                                    "Ability '" + abilityCode + "' not found!",
                                    Component.text(abilityCode)
                            ).color(NamedTextColor.RED));
                        }, null);
                        return;
                    }

                    Ability ability = optAbility.get();
                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;

                        ItemStack item = new ItemStack(org.bukkit.Material.PAPER);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            String typeFallback = ability.getAbilityType() == AbilityType.ACTIVE ? "Active" : "Passive";

                            if (ability.isGlobal()) {
                                meta.displayName(Component.translatable(
                                        "mcidentity.item.ability.name.global",
                                        ability.getDisplayName() + " <" + typeFallback + "> <Global>",
                                        Component.text(ability.getDisplayName()),
                                        Component.translatable("mcidentity.ability.type." + typeFallback.toLowerCase(), typeFallback)
                                ).decoration(TextDecoration.ITALIC, false));
                            } else {
                                meta.displayName(Component.translatable(
                                        "mcidentity.item.ability.name",
                                        ability.getDisplayName() + " <" + typeFallback + ">",
                                        Component.text(ability.getDisplayName()),
                                        Component.translatable("mcidentity.ability.type." + typeFallback.toLowerCase(), typeFallback)
                                ).decoration(TextDecoration.ITALIC, false));
                            }
                            item.setItemMeta(meta);
                        }

                        player.getInventory().addItem(item);
                        player.sendMessage(Component.translatable(
                                "mcidentity.command.success.ability_given",
                                "You received the ability item for: " + ability.getDisplayName(),
                                Component.text(ability.getDisplayName())
                        ).color(NamedTextColor.GREEN));
                    }, null);
                });
                return true;
            }
            sendHelp(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("rune")) {
            if (args.length >= 3 && args[1].equalsIgnoreCase("get")) {
                if (!player.hasPermission("mcidentity.admin")) {
                    player.sendMessage(Component.translatable("mcidentity.command.error.no_permission", "You do not have permission to use this command!").color(NamedTextColor.RED));
                    return true;
                }

                String runeCode = args[2].toLowerCase();
                MCIdentityProvider.instance.getRunes().thenAccept(runes -> {
                    Optional<Rune> optRune = runes.stream()
                            .filter(r -> r.getSystemCode().equalsIgnoreCase(runeCode))
                            .findFirst();

                    if (optRune.isEmpty()) {
                        player.getScheduler().run(plugin, task -> {
                            if (!plugin.isEnabled() || !player.isOnline()) return;
                            player.sendMessage(Component.translatable(
                                    "mcidentity.command.error.rune_not_found",
                                    "Rune '" + runeCode + "' not found!",
                                    Component.text(runeCode)
                            ).color(NamedTextColor.RED));
                        }, null);
                        return;
                    }

                    Rune rune = optRune.get();
                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;

                        ItemStack item = new ItemStack(org.bukkit.Material.FLINT);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.displayName(Component.translatable(
                                    "mcidentity.item.rune.name",
                                    rune.getDisplayName() + " Rune",
                                    Component.text(rune.getDisplayName())
                            ).decoration(TextDecoration.ITALIC, false));
                            item.setItemMeta(meta);
                        }

                        player.getInventory().addItem(item);
                        player.sendMessage(Component.translatable(
                                "mcidentity.command.success.rune_given",
                                "You received the rune item for: " + rune.getDisplayName(),
                                Component.text(rune.getDisplayName())
                        ).color(NamedTextColor.GREEN));
                    }, null);
                });
                return true;
            }
            sendHelp(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("get")) {
            if (!player.hasPermission("mcidentity.admin")) {
                player.sendMessage(Component.translatable("mcidentity.command.error.no_permission", "You do not have permission to use this command!").color(NamedTextColor.RED));
                return true;
            }

            if (args.length >= 2) {
                String professionCode = args[1].toLowerCase();
                String customTexture = args.length >= 3 ? args[2] : null;

                if (professionCode.equalsIgnoreCase("random")) {
                    createRandomIdentityItem(player, customTexture);
                    return true;
                }

                MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
                    Optional<Profession> optProfession = professions.stream().filter(p -> p.getSystemCode().equalsIgnoreCase(professionCode)).findFirst();
                    if (optProfession.isEmpty()) {
                        player.getScheduler().run(plugin, task -> {
                            if (!plugin.isEnabled() || !player.isOnline()) return;
                            player.sendMessage(Component.translatable(
                                    "mcidentity.command.error.profession_not_found",
                                    "Error: Profession '" + professionCode + "' does not exist!",
                                    Component.text(professionCode)
                            ).color(NamedTextColor.RED));
                        }, null);
                        return;
                    }

                    createIdentityItem(player, optProfession.get(), customTexture);
                });
                return true;
            }

            player.sendMessage(Component.translatable("mcidentity.command.usage.get", "Usage: /identity get <profession|random> [head texture]").color(NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("unuse")) {
            if (MCIdentityManager.isOnSwapCooldown(player.getUniqueId())) {
                String remaining = MCIdentityManager.formatTime(MCIdentityManager.getRemainingSwapCooldownMillis(player.getUniqueId()));
                player.sendMessage(Component.translatable(
                        "mcidentity.command.error.swap_cooldown",
                        "You must wait " + remaining + " before swapping identities!",
                        Component.text(remaining)
                ).color(NamedTextColor.RED));
                return true;
            }

            MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
                if (optSession.isEmpty()) {
                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;
                        player.sendMessage(Component.translatable("mcidentity.command.error.no_identity_worn", "You are not currently wearing an identity!").color(NamedTextColor.RED));
                    }, null);
                    return;
                }

                IdentitySession session = optSession.get();
                UUID identityUuid = session.getIdentityUniqueId();
                String base64 = session.getContentKey();

                MCIdentityUnequipEvent unequipEvent = new MCIdentityUnequipEvent(player, identityUuid);
                org.bukkit.Bukkit.getPluginManager().callEvent(unequipEvent);

                if (unequipEvent.isCancelled()) {
                    return;
                }

                MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), null, null).thenAccept(v -> {
                    MCIdentityManager.setSwapCooldown(player.getUniqueId());
                    if (MCIdentityManager.getStatsListener() instanceof io.gitlab.mcoriax.foliamc.listeners.MCIdentityStatsListener statsListener) {
                        statsListener.restoreVisuals(player);
                    }
                    
                    player.getScheduler().run(plugin, task -> {
                        if (!player.isOnline()) return;
                        
                        player.setFoodLevel(20);
                        double maxHealth = 20.0;
                        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                            maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                        }
                        player.setHealth(maxHealth);

                        if (base64 != null) {
                            try {
                                ItemStack item = ItemStackSerializer.deserialize(base64);
                                player.getInventory().addItem(item);
                                player.sendMessage(Component.translatable("mcidentity.status.unequipped_returned", "Identity unequipped and returned to inventory.").color(NamedTextColor.GREEN));
                                MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.UNEQUIP);
                            } catch (Exception e) {
                                player.sendMessage(Component.translatable("mcidentity.command.error.recover_identity_failed", "Failed to recover identity item!").color(NamedTextColor.RED));
                                e.printStackTrace();
                            }
                        }
                    }, null);
                });
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            boolean checkedHand = false;

            if (item.getType() != org.bukkit.Material.AIR && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                String uuidStr = meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
                if (uuidStr != null) {
                    UUID uuid = UUID.fromString(uuidStr);
                    fetchAndDisplayIdentityInfo(player, uuid);
                    checkedHand = true;
                }
            }

            if (!checkedHand) {
                Optional<ActiveIdentitySession> optRamSession = MCIdentityManager.getSession(player.getUniqueId());
                if (optRamSession.isPresent()) {
                    ActiveIdentitySession session = optRamSession.get();
                    renderIdentityInfo(player, session.identityUuid(), session.identityKey(), session.properties(), session.stats());
                } else {
                    player.sendMessage(Component.translatable("mcidentity.command.error.no_identity_or_key", "You are not wearing an identity and not holding an identity key!").color(NamedTextColor.RED));
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("stats")) {
            handleStatsCommand(player, args);
            return true;
        }

        return false;
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
     * Opens the cosmetic GUI for the player.
     * @param player The player to open the GUI for.
     */
    public void openCosmeticGui(Player player) {
        OpenCosmeticGui.execute(plugin, player);
    }

    /**
     * Opens the identity ability GUI for the player.
     * @param player The player to open the GUI for.
     */
    public void openIdentityAbilityGui(Player player) {
        OpenIdentityAbilityGui.execute(plugin, player);
    }

    /**
     * Opens the identity rune GUI for the player.
     * @param player The player to open the GUI for.
     */
    public void openIdentityRuneGui(Player player) {
        OpenIdentityRuneGui.execute(plugin, player);
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
