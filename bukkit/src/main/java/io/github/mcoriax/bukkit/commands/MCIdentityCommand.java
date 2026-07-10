package io.github.mcoriax.bukkit.commands;
import org.bukkit.ChatColor;
import io.github.mcoriax.bukkit.scheduler.Schedulers;

import io.github.mcoriax.api.model.*;
import io.github.mcoriax.bukkit.event.MCIdentityUnequipEvent;
import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityManager;
import io.github.mcoriax.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.commands.utils.*;
import io.github.mcoriax.bukkit.listeners.utils.EquipBaseProfession;
import io.github.mcoriax.bukkit.util.ItemStackSerializer;
import io.github.mcoriax.bukkit.util.MessageComponents;
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

import java.util.Optional;
import java.util.UUID;

public class MCIdentityCommand implements CommandExecutor {

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

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("ability")) {
            if (args.length >= 3 && args[1].equalsIgnoreCase("get")) {
                if (!player.hasPermission("mcidentity.admin")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                    return true;
                }

                String abilityCode = args[2].toLowerCase();
                MCIdentityProvider.instance.getAbilities().thenAccept(abilities -> {
                    Optional<Ability> optAbility = abilities.stream()
                            .filter(a -> a.getSystemCode().equalsIgnoreCase(abilityCode))
                            .findFirst();

                    if (optAbility.isEmpty()) {
                        Schedulers.forEntity(player).run(plugin, st -> {
                            player.sendMessage(ChatColor.RED + MessageComponents.value(
                                    "mcidentity.command.error.ability_not_found.prefix",
                                    "Ability '",
                                    abilityCode,
                                    "mcidentity.command.error.ability_not_found.suffix",
                                    "' not found!"
                            ));
                        }, null);
                        return;
                    }

                    Ability ability = optAbility.get();
                    Schedulers.forEntity(player).run(plugin, st -> {
                        ItemStack item = new ItemStack(Material.PAPER);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            String typeFallback = ability.getAbilityType() == AbilityType.ACTIVE ? "Active" : "Passive";
                            String abilityName = ability.getDisplayName() + " <" + typeFallback + ">";

                            if (ability.isGlobal()) {
                                meta.setDisplayName(abilityName + " <Global>");
                            } else {
                                meta.setDisplayName(abilityName);
                            }
                            item.setItemMeta(meta);
                        }

                        player.getInventory().addItem(item);
                        player.sendMessage(ChatColor.GREEN + MessageComponents.value(
                                "mcidentity.command.success.ability_given.prefix",
                                "You received the ability item for: ",
                                ability.getDisplayName(),
                                "mcidentity.command.success.ability_given.suffix",
                                ""
                        ));
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
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                    return true;
                }

                String runeCode = args[2].toLowerCase();
                MCIdentityProvider.instance.getRunes().thenAccept(runes -> {
                    Optional<Rune> optRune = runes.stream()
                            .filter(r -> r.getSystemCode().equalsIgnoreCase(runeCode))
                            .findFirst();

                    if (optRune.isEmpty()) {
                        Schedulers.forEntity(player).run(plugin, st -> {
                            player.sendMessage(ChatColor.RED + MessageComponents.value(
                                    "mcidentity.command.error.rune_not_found.prefix",
                                    "Rune '",
                                    runeCode,
                                    "mcidentity.command.error.rune_not_found.suffix",
                                    "' not found!"
                            ));
                        }, null);
                        return;
                    }

                    Rune rune = optRune.get();
                    Schedulers.forEntity(player).run(plugin, st -> {
                        ItemStack item = new ItemStack(Material.FLINT);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(ChatColor.GREEN + MessageComponents.value(
                                    "mcidentity.item.rune.name.prefix",
                                    "",
                                    rune.getDisplayName(),
                                    "mcidentity.item.rune.name.suffix",
                                    " Rune"
                            ));
                            item.setItemMeta(meta);
                        }

                        player.getInventory().addItem(item);
                        player.sendMessage(MessageComponents.value(
                                "mcidentity.command.success.rune_given.prefix",
                                "You received the rune item for: ",
                                rune.getDisplayName(),
                                "mcidentity.command.success.rune_given.suffix",
                                ""
                        ));
                    }, null);
                });
                return true;
            }
            sendHelp(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("get")) {
            if (!player.hasPermission("mcidentity.admin")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }

            if (args.length >= 2) {
                String professionCode = args[1].toLowerCase();
                String customTexture = args.length >= 3 ? args[2] : null;

                // The permanent base profession can never become an item.
                if (BaseProfession.isBaseProfession(professionCode)) {
                    player.sendMessage(ChatColor.RED + "The base Player profession cannot be turned into an identity item!");
                    return true;
                }

                if (professionCode.equalsIgnoreCase("random")) {
                    createRandomIdentityItem(player, customTexture);
                    return true;
                }

                MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
                    Optional<Profession> optProfession = professions.stream().filter(p -> p.getSystemCode().equalsIgnoreCase(professionCode)).findFirst();
                    if (optProfession.isEmpty()) {
                        Schedulers.forEntity(player).run(plugin, st -> {
                            player.sendMessage(ChatColor.RED + MessageComponents.value(
                                    "mcidentity.command.error.profession_not_found.prefix",
                                    "Error: Profession '",
                                    professionCode,
                                    "mcidentity.command.error.profession_not_found.suffix",
                                    "' does not exist!"
                            ));
                        }, null);
                        return;
                    }

                    createIdentityItem(player, optProfession.get(), customTexture);
                });
                return true;
            }

            player.sendMessage(ChatColor.RED + "Usage: /identity get <profession|random> [head texture]");
            return true;
        }

        if (args[0].equalsIgnoreCase("unuse")) {
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
                    Schedulers.forEntity(player).run(plugin, st -> {
                        player.sendMessage(ChatColor.RED + "You are not currently wearing an identity!");
                    }, null);
                    return;
                }

                IdentitySession session = optSession.get();
                UUID identityUuid = session.getIdentityUniqueId();
                String base64 = session.getContentKey();

                // The permanent base profession is the fallback state and can
                // never itself be unequipped.
                if (BaseProfession.isBaseIdentity(player.getUniqueId(), identityUuid)) {
                    Schedulers.forEntity(player).run(plugin, st -> {
                        player.sendMessage(ChatColor.RED + "The base Player profession cannot be unequipped.");
                    }, null);
                    return;
                }

                Schedulers.forEntity(player).run(plugin, st1 -> {
                    MCIdentityUnequipEvent unequipEvent = new MCIdentityUnequipEvent(player, identityUuid);
                    Bukkit.getPluginManager().callEvent(unequipEvent);

                    if (unequipEvent.isCancelled()) {
                        return;
                    }

                    MCIdentityManager.setSwapCooldown(player.getUniqueId());

                    if (base64 != null) {
                        Schedulers.async().runNow(plugin, st2 -> {
                            ItemStack recovered = null;
                            try {
                                recovered = ItemStackSerializer.deserialize(base64);
                            } catch (Exception e) {
                                Schedulers.forEntity(player).run(plugin, st3 -> {
                                    player.sendMessage(ChatColor.RED + "Failed to recover identity item!");
                                }, null);
                                e.printStackTrace();
                            }
                            EquipBaseProfession.execute(plugin, player, recovered,
                                    ChatColor.GREEN + "Identity unequipped. You are back to the base Player profession.", false);
                            MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.UNEQUIP);
                        });
                    } else {
                        EquipBaseProfession.execute(plugin, player, null,
                                ChatColor.GREEN + "Identity unequipped. You are back to the base Player profession.", false);
                        MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.UNEQUIP);
                    }
                }, null);
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            boolean checkedHand = false;

            if (item.getType() != Material.AIR && item.hasItemMeta()) {
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
                    player.sendMessage(ChatColor.RED + "You are not wearing an identity and not holding an identity key!");
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
