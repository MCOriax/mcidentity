package io.gitlab.mcoriax.foliamc.listeners.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.Profession;
import io.gitlab.mcoriax.bukkit.event.MCIdentityCreationEvent;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.util.HeadTexture;
import io.gitlab.mcoriax.foliamc.util.ItemStackSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class InitializeIdentity {
    /**
     * Initialize an identity for a player.
     * @param plugin The plugin instance.
     * @param player The player for whom to initialize the identity.
     * @param identityUuid The identity UUID.
     * @param professionCode The profession code.
     * @param itemToEquip The item stack to initialize.
     * @param reopenGui Whether to reopen the GUI.
     * @param textureKey The NamespacedKey for texture.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     */
    public static void execute(MCIdentityPlugin plugin, Player player, UUID identityUuid, String professionCode, ItemStack itemToEquip, boolean reopenGui, NamespacedKey textureKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey) {
        MCIdentityProvider.instance.getIdentityKey(identityUuid).thenAccept(optKey -> {
            if (optKey.isEmpty()) {
                MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
                    if (professions.isEmpty()) {
                        RefundItemSafely.execute(plugin, player, itemToEquip, Component.translatable("mcidentity.gui.error.no_professions_available", "No professions available!").color(NamedTextColor.RED));
                        return;
                    }

                    Profession profession;
                    if (professionCode == null || professionCode.equalsIgnoreCase("random")) {
                        List<String> loadedProfCodes = new ArrayList<>(MCIdentityManager.getRegisteredProfessions().keySet());
                        if (loadedProfCodes.isEmpty()) {
                            RefundItemSafely.execute(plugin, player, itemToEquip, Component.translatable("mcidentity.gui.error.no_professions_loaded", "No professions loaded in memory!").color(NamedTextColor.RED));
                            return;
                        }
                        
                        String pickedCode = loadedProfCodes.get(ThreadLocalRandom.current().nextInt(loadedProfCodes.size()));
                        Optional<Profession> optProf = professions.stream()
                                .filter(p -> p.getSystemCode().equalsIgnoreCase(pickedCode))
                                .findFirst();
                                
                        if (optProf.isEmpty()) {
                            List<Profession> profList = new ArrayList<>(professions);
                            profession = profList.get(ThreadLocalRandom.current().nextInt(profList.size()));
                        } else {
                            profession = optProf.get();
                        }
                    } else {
                        Optional<Profession> optProf = professions.stream()
                                .filter(p -> p.getSystemCode().equalsIgnoreCase(professionCode))
                                .findFirst();
                        if (optProf.isEmpty()) {
                            RefundItemSafely.execute(plugin, player, itemToEquip, Component.translatable(
                                    "mcidentity.gui.error.profession_not_found",
                                    "Profession '" + professionCode + "' not found!",
                                    Component.text(professionCode)
                            ).color(NamedTextColor.RED));
                            return;
                        }
                        profession = optProf.get();
                    }

                    String actualProfCode = profession.getSystemCode();
                    String displayName = profession.getDisplayName();

                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;

                        SkullMeta skullMeta = (SkullMeta) itemToEquip.getItemMeta();
                        String finalTexture = null;
                        if (skullMeta != null) {
                            boolean isUnidentified = skullMeta.getPersistentDataContainer().has(unidentifiedKey, PersistentDataType.BYTE);
                            
                            PlayerProfile profile = skullMeta.getPlayerProfile();
                            boolean isUnidentifiedTexture = false;
                            if (profile != null) {
                                isUnidentifiedTexture = profile.getProperties().stream()
                                        .anyMatch(p -> p.getName().equals("textures") && p.getValue().equals(HeadTexture.getUnidentified(plugin)));
                            }

                            if (isUnidentified || isUnidentifiedTexture) {
                                String existingTexture = skullMeta.getPersistentDataContainer().get(textureKey, PersistentDataType.STRING);
                                if (existingTexture == null || existingTexture.equals(HeadTexture.getUnidentified(plugin))) {
                                    List<String> textures = new ArrayList<>(profession.getSkins().values());
                                    if (!textures.isEmpty()) {
                                        String activeTexture = textures.get(ThreadLocalRandom.current().nextInt(textures.size()));
                                        PlayerProfile newProfile = Bukkit.createProfile(UUID.randomUUID());
                                        newProfile.setProperty(new ProfileProperty("textures", activeTexture));
                                        skullMeta.setPlayerProfile(newProfile);
                                        skullMeta.getPersistentDataContainer().set(textureKey, PersistentDataType.STRING, activeTexture);
                                        finalTexture = activeTexture;
                                    }
                                } else {
                                    finalTexture = existingTexture;
                                }

                                NamedTextColor[] suitableColors = {
                                    NamedTextColor.AQUA, NamedTextColor.BLUE, NamedTextColor.DARK_AQUA,
                                    NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN, NamedTextColor.DARK_PURPLE,
                                    NamedTextColor.DARK_RED, NamedTextColor.GOLD, NamedTextColor.GREEN,
                                    NamedTextColor.LIGHT_PURPLE, NamedTextColor.RED, NamedTextColor.WHITE,
                                    NamedTextColor.YELLOW
                                };
                                NamedTextColor randomNameColor = suitableColors[ThreadLocalRandom.current().nextInt(suitableColors.length)];
                                NamedTextColor randomLevelColor = suitableColors[ThreadLocalRandom.current().nextInt(suitableColors.length)];

                                skullMeta.displayName(
                                    Component.text("[", NamedTextColor.GRAY)
                                        .append(Component.text(displayName, randomNameColor))
                                        .append(Component.text("] [", NamedTextColor.GRAY))
                                        .append(Component.text(1, randomLevelColor))
                                        .append(Component.text("]", NamedTextColor.GRAY))
                                        .decoration(TextDecoration.ITALIC, false)
                                );
                                skullMeta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, actualProfCode);
                                skullMeta.getPersistentDataContainer().remove(unidentifiedKey);
                                itemToEquip.setItemMeta(skullMeta);
                            } else if (profile != null) {
                                finalTexture = profile.getProperties().stream()
                                        .filter(p -> p.getName().equals("textures"))
                                        .map(ProfileProperty::getValue)
                                        .findFirst().orElse(null);
                            }
                        }

                        String updatedBase64 = ItemStackSerializer.serialize(itemToEquip);

                        CreateIdentityInDB.execute(plugin, identityUuid, profession, finalTexture).thenAccept(v -> {
                            player.getScheduler().run(plugin, t -> {
                                if (!plugin.isEnabled()) return;
                                
                                MCIdentityProvider.instance.getStats(identityUuid).thenAccept(optStats -> {
                                    MCIdentityProvider.instance.getProperties(identityUuid).thenAccept(optProps -> {
                                        if (optStats.isPresent() && optProps.isPresent()) {
                                            IdentityKey key = new IdentityKey() {
                                                @Override public UUID getUniqueId() { return identityUuid; }
                                                @Override public String getProfessionCode() { return actualProfCode; }
                                            };
                                            MCIdentityCreationEvent event = new MCIdentityCreationEvent(key, optProps.get(), optStats.get());
                                            Bukkit.getPluginManager().callEvent(event);
                                        }
                                    });
                                });
                            }, null);
                            
                            ProcessEquip.execute(plugin, player, identityUuid, itemToEquip, updatedBase64, reopenGui);
                        }).exceptionally(ex -> {
                            String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                            RefundItemSafely.execute(plugin, player, itemToEquip, Component.translatable(
                                    "mcidentity.gui.error.registration_failed",
                                    "Failed to register identity: " + errorMessage,
                                    Component.text(errorMessage)
                            ).color(NamedTextColor.RED));
                            return null;
                        });
                    }, null);
                });
            } else {
                IdentityKey key = optKey.get();
                String dbProfCode = key.getProfessionCode();
                
                player.getScheduler().run(plugin, task -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;

                    SkullMeta skullMeta = (SkullMeta) itemToEquip.getItemMeta();
                    if (skullMeta != null) {
                        boolean isUnidentified = skullMeta.getPersistentDataContainer().has(unidentifiedKey, PersistentDataType.BYTE);
                        String currentProf = skullMeta.getPersistentDataContainer().get(professionKey, PersistentDataType.STRING);
                        
                        if (isUnidentified || "random".equalsIgnoreCase(currentProf)) {
                            MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
                                Optional<Profession> optProf = professions.stream()
                                        .filter(p -> p.getSystemCode().equalsIgnoreCase(dbProfCode))
                                        .findFirst();
                                
                                if (optProf.isPresent()) {
                                    Profession profession = optProf.get();
                                    player.getScheduler().run(plugin, innerTask -> {
                                        if (!plugin.isEnabled() || !player.isOnline()) return;
                                        
                                        NamedTextColor[] suitableColors = {
                                            NamedTextColor.AQUA, NamedTextColor.BLUE, NamedTextColor.DARK_AQUA,
                                            NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN, NamedTextColor.DARK_PURPLE,
                                            NamedTextColor.DARK_RED, NamedTextColor.GOLD, NamedTextColor.GREEN,
                                            NamedTextColor.LIGHT_PURPLE, NamedTextColor.RED, NamedTextColor.WHITE,
                                            NamedTextColor.YELLOW
                                        };
                                        NamedTextColor randomNameColor = suitableColors[ThreadLocalRandom.current().nextInt(suitableColors.length)];
                                        NamedTextColor randomLevelColor = suitableColors[ThreadLocalRandom.current().nextInt(suitableColors.length)];

                                        skullMeta.displayName(
                                            Component.text("[", NamedTextColor.GRAY)
                                                .append(Component.text(profession.getDisplayName(), randomNameColor))
                                                .append(Component.text("] [", NamedTextColor.GRAY))
                                                .append(Component.text(1, randomLevelColor))
                                                .append(Component.text("]", NamedTextColor.GRAY))
                                                .decoration(TextDecoration.ITALIC, false)
                                        );
                                        
                                        List<String> textures = new ArrayList<>(profession.getSkins().values());
                                        if (!textures.isEmpty()) {
                                            String activeTexture = textures.get(ThreadLocalRandom.current().nextInt(textures.size()));
                                            PlayerProfile newProfile = Bukkit.createProfile(UUID.randomUUID());
                                            newProfile.setProperty(new ProfileProperty("textures", activeTexture));
                                            skullMeta.setPlayerProfile(newProfile);
                                        }

                                        skullMeta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, dbProfCode);
                                        skullMeta.getPersistentDataContainer().remove(unidentifiedKey);
                                        itemToEquip.setItemMeta(skullMeta);
                                        
                                        String base64 = ItemStackSerializer.serialize(itemToEquip);
                                        ProcessEquip.execute(plugin, player, identityUuid, itemToEquip, base64, reopenGui);
                                    }, null);
                                } else {
                                    String base64 = ItemStackSerializer.serialize(itemToEquip);
                                    ProcessEquip.execute(plugin, player, identityUuid, itemToEquip, base64, reopenGui);
                                }
                            });
                            return;
                        }
                    }
                    
                    String base64 = ItemStackSerializer.serialize(itemToEquip);
                    ProcessEquip.execute(plugin, player, identityUuid, itemToEquip, base64, reopenGui);
                }, null);
            }
        }).exceptionally(ex -> {
            String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
            RefundItemSafely.execute(plugin, player, itemToEquip, Component.translatable(
                    "mcidentity.gui.error.fetch_identity_failed",
                    "Failed to fetch identity data: " + errorMessage,
                    Component.text(errorMessage)
            ).color(NamedTextColor.RED));
            return null;
        });
    }
}
