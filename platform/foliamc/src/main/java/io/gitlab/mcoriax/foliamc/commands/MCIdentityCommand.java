package io.gitlab.mcoriax.foliamc.commands;

import io.gitlab.mcoriax.api.db.IMCIdentityDB;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.util.ItemStackSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class MCIdentityCommand implements CommandExecutor {

    private final MCIdentityPlugin plugin;
    private final NamespacedKey identityKey;
    private final NamespacedKey sigilKey;
    private final NamespacedKey professionKey;

    public MCIdentityCommand(MCIdentityPlugin plugin) {
        this.plugin = plugin;
        this.identityKey = new NamespacedKey(plugin, "identity_uuid");
        this.sigilKey = new NamespacedKey(plugin, "sigil_code");
        this.professionKey = new NamespacedKey(plugin, "profession_code");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /identity <create|check> ..."));
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length == 3 && args[1].equalsIgnoreCase("sigil")) {
                // /identity create sigil <name>
                String sigilCode = args[2];
                MCIdentityProvider.getSigils().thenAccept(sigils -> {
                    boolean exists = sigils.stream().anyMatch(s -> s.getSystemCode().equalsIgnoreCase(sigilCode));
                    if (exists) {
                        player.sendMessage(Component.text("Sigil '" + sigilCode + "' already exists!"));
                        return;
                    }
                    MCIdentityProvider.createSigil(sigilCode, sigilCode).thenAccept(v -> {
                        player.sendMessage(Component.text("Sigil '" + sigilCode + "' created successfully!"));
                    }).exceptionally(ex -> {
                        player.sendMessage(Component.text("Failed to create sigil: " + ex.getMessage()));
                        return null;
                    });
                });
                return true;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("profession")) {
                // /identity create profession <name>
                String professionCode = args[2];
                MCIdentityProvider.getProfessions().thenAccept(professions -> {
                    boolean exists = professions.stream().anyMatch(p -> p.getSystemCode().equalsIgnoreCase(professionCode));
                    if (exists) {
                        player.sendMessage(Component.text("Profession '" + professionCode + "' already exists!"));
                        return;
                    }
                    MCIdentityProvider.createProfession(professionCode, professionCode).thenAccept(v -> {
                        player.sendMessage(Component.text("Profession '" + professionCode + "' created successfully!"));
                    });
                });
                return true;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("ability")) {
                // /identity create ability <name>
                String abilityCode = args[2];
                MCIdentityProvider.getAbilities().thenAccept(abilities -> {
                    boolean exists = abilities.stream().anyMatch(a -> a.getSystemCode().equalsIgnoreCase(abilityCode));
                    if (exists) {
                        player.sendMessage(Component.text("Ability '" + abilityCode + "' already exists!"));
                        return;
                    }
                    // Default to ACTIVE and not global for now
                    MCIdentityProvider.createAbility(abilityCode, abilityCode, AbilityType.ACTIVE, false).thenAccept(v -> {
                        player.sendMessage(Component.text("Ability '" + abilityCode + "' created successfully!"));
                    });
                });
                return true;
            }

            if (args.length == 3) {
                // OLD logic: /identity create <profession> <sigil>
                String professionCode = args[1];
                String sigilCode = args[2];

                MCIdentityProvider.getSigils().thenAccept(sigils -> {
                    boolean sigilExists = sigils.stream().anyMatch(s -> s.getSystemCode().equalsIgnoreCase(sigilCode));
                    if (!sigilExists) {
                        player.sendMessage(Component.text("Error: Sigil '" + sigilCode + "' does not exist!"));
                        return;
                    }

                    MCIdentityProvider.getProfessions().thenAccept(professions -> {
                        boolean professionExists = professions.stream().anyMatch(p -> p.getSystemCode().equalsIgnoreCase(professionCode));
                        if (!professionExists) {
                            player.sendMessage(Component.text("Error: Profession '" + professionCode + "' does not exist!"));
                            return;
                        }

                        // Also create an identity key item for convenience
                        createIdentityItem(player, sigilCode, professionCode);
                    });
                });
                return true;
            }

            player.sendMessage(Component.text("Usage: /identity create <sigil|profession|ability> <name> OR /identity create <profession> <sigil>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("unuse")) {
            MCIdentityProvider.getSession(player.getUniqueId()).thenAccept(optSession -> {
                if (optSession.isEmpty()) {
                    player.sendMessage(Component.text("You are not currently wearing an identity!"));
                    return;
                }

                IdentitySession session = optSession.get();
                UUID identityUuid = session.getIdentityUniqueId();
                String base64 = session.getContentKey();

                MCIdentityProvider.setActiveIdentity(player.getUniqueId(), null, null).thenAccept(v -> {
                    if (base64 != null) {
                        try {
                            ItemStack item = ItemStackSerializer.deserialize(base64);
                            player.getScheduler().run(plugin, task -> {
                                player.getInventory().addItem(item);
                            }, null);
                            player.sendMessage(Component.text("Identity unequipped and returned to inventory."));
                            MCIdentityProvider.logAction(player.getUniqueId(), identityUuid, ActionType.UNEQUIP);
                        } catch (Exception e) {
                            player.sendMessage(Component.text("Failed to recover identity item!"));
                            e.printStackTrace();
                        }
                    }
                });
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            MCIdentityProvider.getSession(player.getUniqueId()).thenAccept(optSession -> {
                if (optSession.isPresent()) {
                    displayIdentityInfo(player, optSession.get().getIdentityUniqueId());
                } else {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item.getType() == Material.AIR || !item.hasItemMeta()) {
                        player.sendMessage(Component.text("You are not wearing an identity and not holding an identity key!"));
                        return;
                    }

                    ItemMeta meta = item.getItemMeta();
                    String uuidStr = meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
                    if (uuidStr == null) {
                        player.sendMessage(Component.text("This item is not a valid identity key!"));
                        return;
                    }

                    UUID uuid = UUID.fromString(uuidStr);
                    displayIdentityInfo(player, uuid);
                }
            });
            return true;
        }

        return false;
    }

    private void displayIdentityInfo(Player player, UUID uuid) {
        MCIdentityProvider.getIdentityKey(uuid).thenAccept(optKey -> {
            if (optKey.isEmpty()) {
                player.sendMessage(Component.text("Identity data not found in database.", NamedTextColor.RED));
                return;
            }

            IdentityKey key = optKey.get();
            
            // Fetch stats and properties together
            MCIdentityProvider.getProperties(uuid).thenAccept(optProps -> {
                MCIdentityProvider.getStats(uuid).thenAccept(optStats -> {
                    player.sendMessage(Component.text("--- Identity Data ---", NamedTextColor.GOLD, TextDecoration.BOLD));
                    player.sendMessage(Component.text("UUID: ", NamedTextColor.GRAY).append(Component.text(uuid.toString(), NamedTextColor.WHITE)));
                    player.sendMessage(Component.text("Sigil: ", NamedTextColor.GRAY).append(Component.text(key.getSigilCode(), NamedTextColor.AQUA)));
                    player.sendMessage(Component.text("Profession: ", NamedTextColor.GRAY).append(Component.text(key.getProfessionCode(), NamedTextColor.GREEN)));

                    if (optProps.isPresent()) {
                        IdentityProperties props = optProps.get();
                        player.sendMessage(Component.text("Level: ", NamedTextColor.GRAY).append(Component.text(props.getLevel(), NamedTextColor.YELLOW)));
                        player.sendMessage(Component.text("Exp: ", NamedTextColor.GRAY).append(Component.text(props.getExp(), NamedTextColor.YELLOW)));
                        player.sendMessage(Component.text("Stat Points: ", NamedTextColor.GRAY).append(Component.text(props.getStatPoints(), NamedTextColor.YELLOW)));
                    }

                    if (optStats.isPresent()) {
                        IdentityStats stats = optStats.get();
                        player.sendMessage(Component.text("Health: ", NamedTextColor.GRAY).append(Component.text(stats.getHealth(), NamedTextColor.RED)));
                        player.sendMessage(Component.text("Energy: ", NamedTextColor.GRAY).append(Component.text(stats.getEnergy(), NamedTextColor.BLUE)));
                        player.sendMessage(Component.text("Strength: ", NamedTextColor.GRAY).append(Component.text(stats.getStrength(), NamedTextColor.DARK_RED)));
                        player.sendMessage(Component.text("Defense: ", NamedTextColor.GRAY).append(Component.text(stats.getDefense(), NamedTextColor.GRAY)));
                        player.sendMessage(Component.text("Wisdom: ", NamedTextColor.GRAY).append(Component.text(stats.getWisdom(), NamedTextColor.LIGHT_PURPLE)));
                        player.sendMessage(Component.text("Luck: ", NamedTextColor.GRAY).append(Component.text(stats.getLuck(), NamedTextColor.GOLD)));
                        player.sendMessage(Component.text("Agility: ", NamedTextColor.GRAY).append(Component.text(stats.getAgility(), NamedTextColor.YELLOW)));
                        player.sendMessage(Component.text("Resilience: ", NamedTextColor.GRAY).append(Component.text(stats.getResilience(), NamedTextColor.DARK_GREEN)));
                    }
                });
            });
        });
    }

    private void createIdentityItem(Player player, String sigilCode, String professionCode) {
        UUID identityUuid = UUID.randomUUID();
        
        IdentityKey key = new IdentityKey() {
            @Override public UUID getUniqueId() { return identityUuid; }
            @Override public String getSigilCode() { return sigilCode; }
            @Override public String getProfessionCode() { return professionCode; }
        };

        IdentityProperties props = new IdentityProperties() {
            @Override public int getLevel() { return 1; }
            @Override public int getExp() { return 0; }
            @Override public int getStatPoints() { return 0; }
            @Override public String getHeadText() { return null; }
            @Override public String getModel() { return null; }
        };

        IdentityStats stats = new IdentityStats() {
            @Override public int getEnergy() { return 100; }
            @Override public int getHealth() { return 20; }
            @Override public int getStrength() { return 0; }
            @Override public int getDefense() { return 0; }
            @Override public int getWisdom() { return 0; }
            @Override public int getLuck() { return 0; }
            @Override public int getAgility() { return 0; }
            @Override public int getResilience() { return 0; }
        };

        MCIdentityProvider.createIdentity(key, props, stats).thenAccept(v -> {
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Identity: " + sigilCode + " " + professionCode));
                meta.getPersistentDataContainer().set(identityKey, PersistentDataType.STRING, identityUuid.toString());
                meta.getPersistentDataContainer().set(sigilKey, PersistentDataType.STRING, sigilCode);
                meta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, professionCode);
                paper.setItemMeta(meta);
            }

            player.getScheduler().run(plugin, task -> {
                player.getInventory().addItem(paper);
                player.sendMessage(Component.text("Identity item given!"));
            }, null);
        });
    }
}
