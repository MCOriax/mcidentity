package io.gitlab.mcoriax.foliamc.commands;

import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.commands.utils.*;
import io.gitlab.mcoriax.foliamc.util.ItemStackSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    private final MCIdentityPlugin plugin;
    private final NamespacedKey identityKey;
    private final NamespacedKey professionKey;
    private final NamespacedKey textureKey;
    private final NamespacedKey unidentifiedKey;

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
            sender.sendMessage("This command can only be used by players.");
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
                    player.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
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
                            player.sendMessage(Component.text("Ability '" + abilityCode + "' not found!", NamedTextColor.RED));
                        }, null);
                        return;
                    }

                    Ability ability = optAbility.get();
                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;

                        ItemStack item = new ItemStack(org.bukkit.Material.PAPER);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            String typeStr = ability.getAbilityType() == AbilityType.ACTIVE ? "Active" : "Passive";
                            StringBuilder nameBuilder = new StringBuilder();
                            nameBuilder.append(ability.getDisplayName()).append(" <").append(typeStr).append(">");
                            if (ability.isGlobal()) {
                                nameBuilder.append(" <Global>");
                            }

                            meta.displayName(Component.text(nameBuilder.toString()).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                            item.setItemMeta(meta);
                        }

                        player.getInventory().addItem(item);
                        player.sendMessage(Component.text("You received the ability item for: " + ability.getDisplayName(), NamedTextColor.GREEN));
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
                    player.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
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
                            player.sendMessage(Component.text("Rune '" + runeCode + "' not found!", NamedTextColor.RED));
                        }, null);
                        return;
                    }

                    Rune rune = optRune.get();
                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;

                        ItemStack item = new ItemStack(org.bukkit.Material.FLINT);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.displayName(Component.text(rune.getDisplayName() + " Rune").decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                            item.setItemMeta(meta);
                        }

                        player.getInventory().addItem(item);
                        player.sendMessage(Component.text("You received the rune item for: " + rune.getDisplayName(), NamedTextColor.GREEN));
                    }, null);
                });
                return true;
            }
            sendHelp(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("get")) {
            if (!player.hasPermission("mcidentity.admin")) {
                player.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
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
                            player.sendMessage(Component.text("Error: Profession '" + professionCode + "' does not exist!", NamedTextColor.RED));
                        }, null);
                        return;
                    }

                    createIdentityItem(player, optProfession.get(), customTexture);
                });
                return true;
            }

            player.sendMessage(Component.text("Usage: /identity get <profession|random> [head texture]", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("unuse")) {
            if (MCIdentityManager.isOnSwapCooldown(player.getUniqueId())) {
                String remaining = MCIdentityManager.formatTime(MCIdentityManager.getRemainingSwapCooldownMillis(player.getUniqueId()));
                player.sendMessage(Component.text("You must wait " + remaining + " before swapping identities!", NamedTextColor.RED));
                return true;
            }

            MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
                if (optSession.isEmpty()) {
                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;
                        player.sendMessage(Component.text("You are not currently wearing an identity!", NamedTextColor.RED));
                    }, null);
                    return;
                }

                IdentitySession session = optSession.get();
                UUID identityUuid = session.getIdentityUniqueId();
                String base64 = session.getContentKey();

                MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), null, null).thenAccept(v -> {
                    MCIdentityManager.setSwapCooldown(player.getUniqueId());
                    if (base64 != null) {
                        try {
                            ItemStack item = ItemStackSerializer.deserialize(base64);
                            player.getScheduler().run(plugin, task -> {
                                if (!plugin.isEnabled() || !player.isOnline()) return;
                                player.getInventory().addItem(item);
                                player.sendMessage(Component.text("Identity unequipped and returned to inventory.", NamedTextColor.GREEN));
                            }, null);
                            MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.UNEQUIP);
                        } catch (Exception e) {
                            player.getScheduler().run(plugin, task -> {
                                if (!plugin.isEnabled() || !player.isOnline()) return;
                                player.sendMessage(Component.text("Failed to recover identity item!", NamedTextColor.RED));
                            }, null);
                            e.printStackTrace();
                        }
                    }
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
                    player.sendMessage(Component.text("You are not wearing an identity and not holding an identity key!", NamedTextColor.RED));
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

    private void handleStatsCommand(Player player, String[] args) {
        HandleStatsCommand.execute(plugin, player, args);
    }

    private void sendHelp(Player player) {
        SendHelp.execute(player);
    }

    public void openIdentityGui(Player player) {
        OpenIdentityGui.execute(plugin, player);
    }

    public void openCosmeticGui(Player player) {
        OpenCosmeticGui.execute(plugin, player);
    }

    public void openIdentityAbilityGui(Player player) {
        OpenIdentityAbilityGui.execute(plugin, player);
    }

    public void openIdentityRuneGui(Player player) {
        OpenIdentityRuneGui.execute(plugin, player);
    }

    private void fetchAndDisplayIdentityInfo(Player player, UUID uuid) {
        FetchAndDisplayIdentityInfo.execute(plugin, player, uuid);
    }

    private void renderIdentityInfo(Player player, UUID uuid, IdentityKey key, IdentityProperties props, IdentityStats stats) {
        RenderIdentityInfo.execute(player, uuid, key, props, stats);
    }

    private void createRandomIdentityItem(Player player, String customTexture) {
        CreateRandomIdentityItem.execute(plugin, player, customTexture, professionKey, unidentifiedKey);
    }

    private void createIdentityItem(Player player, Profession profession, String customTexture) {
        CreateIdentityItem.execute(plugin, player, profession, customTexture, textureKey, professionKey, unidentifiedKey);
    }
}
