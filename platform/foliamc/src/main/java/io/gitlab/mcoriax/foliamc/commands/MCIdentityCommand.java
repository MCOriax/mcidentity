package io.gitlab.mcoriax.foliamc.commands;

import io.gitlab.mcoriax.api.db.IMCIdentityDB;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import net.kyori.adventure.text.Component;
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

    public MCIdentityCommand(MCIdentityPlugin plugin) {
        this.plugin = plugin;
        this.identityKey = new NamespacedKey(plugin, "identity_uuid");
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
            if (args.length == 2) {
                // /identity create <sigil>
                String sigilCode = args[1];
                MCIdentityProvider.createSigil(sigilCode, sigilCode).thenAccept(v -> {
                    player.sendMessage(Component.text("Sigil '" + sigilCode + "' created successfully!"));
                }).exceptionally(ex -> {
                    player.sendMessage(Component.text("Failed to create sigil: " + ex.getMessage()));
                    return null;
                });
                return true;
            }

            if (args.length == 3) {
                // /identity create <profession> <sigil>
                String professionCode = args[1];
                String sigilCode = args[2];

                MCIdentityProvider.getSigils().thenAccept(sigils -> {
                    boolean sigilExists = sigils.stream().anyMatch(s -> s.getSystemCode().equalsIgnoreCase(sigilCode));
                    if (!sigilExists) {
                        player.sendMessage(Component.text("Error: Sigil '" + sigilCode + "' does not exist!"));
                        return;
                    }

                    MCIdentityProvider.createProfession(professionCode, professionCode).thenAccept(v -> {
                        player.sendMessage(Component.text("Profession '" + professionCode + "' created for sigil '" + sigilCode + "'!"));
                        
                        // Also create an identity key item for convenience
                        createIdentityItem(player, sigilCode, professionCode);
                    }).exceptionally(ex -> {
                        player.sendMessage(Component.text("Failed to create profession: " + ex.getMessage()));
                        return null;
                    });
                });
                return true;
            }

            player.sendMessage(Component.text("Usage: /identity create <sigil> OR /identity create <profession> <sigil>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR || !item.hasItemMeta()) {
                player.sendMessage(Component.text("You are not holding an identity key!"));
                return true;
            }

            ItemMeta meta = item.getItemMeta();
            String uuidStr = meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
            if (uuidStr == null) {
                player.sendMessage(Component.text("This item is not a valid identity key!"));
                return true;
            }

            UUID uuid = UUID.fromString(uuidStr);
            MCIdentityProvider.getIdentityKey(uuid).thenAccept(optKey -> {
                if (optKey.isEmpty()) {
                    player.sendMessage(Component.text("Identity data not found in database."));
                    return;
                }

                IdentityKey key = optKey.get();
                player.sendMessage(Component.text("--- Identity Data ---"));
                player.sendMessage(Component.text("UUID: " + uuid));
                player.sendMessage(Component.text("Sigil: " + key.getSigilCode()));
                player.sendMessage(Component.text("Profession: " + key.getProfessionCode()));

                // Fetch stats and properties
                MCIdentityProvider.getStats(uuid).thenAccept(optStats -> {
                    optStats.ifPresent(stats -> {
                        player.sendMessage(Component.text("Health: " + stats.getHealth()));
                        player.sendMessage(Component.text("Energy: " + stats.getEnergy()));
                        player.sendMessage(Component.text("Strength: " + stats.getStrength()));
                    });
                });

                MCIdentityProvider.getProperties(uuid).thenAccept(optProps -> {
                    optProps.ifPresent(props -> {
                        player.sendMessage(Component.text("Level: " + props.getLevel()));
                        player.sendMessage(Component.text("Exp: " + props.getExp()));
                    });
                });
            });
            return true;
        }

        return false;
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
                paper.setItemMeta(meta);
            }

            player.getScheduler().run(plugin, task -> {
                player.getInventory().addItem(paper);
                player.sendMessage(Component.text("Identity item given!"));
            }, null);
        });
    }
}
