package io.github.mcoriax.mcidentity.bukkit.listeners.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.IdentityKey;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.api.model.Profession;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityCreationEvent;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.util.HeadTexture;
import io.github.mcoriax.mcidentity.bukkit.util.IdentityItemRenderer;
import io.github.mcoriax.mcidentity.bukkit.util.ItemStackSerializer;
import io.github.mcoriax.mcidentity.bukkit.util.SkullTextures;
import io.github.mcoriax.mcidentity.bukkit.util.MessageComponents;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
    public static void execute(JavaPlugin plugin, Player player, UUID identityUuid, String professionCode, ItemStack itemToEquip, boolean reopenGui, NamespacedKey textureKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey) {
        MCIdentityProvider.instance.getIdentityKey(identityUuid).thenAccept(optKey -> {
            if (optKey.isEmpty()) {
                // The base profession only exists as the permanent identity
                // bound to a player's UUID; it can never come from an item.
                if (professionCode != null && BaseProfession.isBaseProfession(professionCode)) {
                    RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "The base Player profession cannot be equipped from an item!");
                    return;
                }

                MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
                    if (professions.isEmpty()) {
                        RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "No professions available!");
                        return;
                    }

                    Profession profession;
                    if (professionCode == null || professionCode.equalsIgnoreCase("random")) {
                        List<String> loadedProfCodes = new ArrayList<>(MCIdentityManager.getRegisteredProfessions().keySet());
                        loadedProfCodes.remove(BaseProfession.CODE);
                        if (loadedProfCodes.isEmpty()) {
                            RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "No professions loaded in memory!");
                            return;
                        }

                        String pickedCode = loadedProfCodes.get(ThreadLocalRandom.current().nextInt(loadedProfCodes.size()));
                        Optional<Profession> optProf = professions.stream()
                                .filter(p -> p.getSystemCode().equalsIgnoreCase(pickedCode))
                                .findFirst();

                        if (optProf.isEmpty()) {
                            List<Profession> profList = professions.stream()
                                    .filter(p -> !BaseProfession.isBaseProfession(p.getSystemCode()))
                                    .toList();
                            if (profList.isEmpty()) {
                                RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "No professions available!");
                                return;
                            }
                            profession = profList.get(ThreadLocalRandom.current().nextInt(profList.size()));
                        } else {
                            profession = optProf.get();
                        }
                    } else {
                        Optional<Profession> optProf = professions.stream()
                                .filter(p -> p.getSystemCode().equalsIgnoreCase(professionCode))
                                .findFirst();
                        if (optProf.isEmpty()) {
                            RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + MessageComponents.value(
                                    "mcidentity.gui.error.profession_not_found.prefix",
                                    "Profession '",
                                    professionCode,
                                    "mcidentity.gui.error.profession_not_found.suffix",
                                    "' not found!"
                            ));
                            return;
                        }
                        profession = optProf.get();
                    }

                    String actualProfCode = profession.getSystemCode();

                    Schedulers.forEntity(player).run(plugin, st1 -> {
                        SkullMeta skullMeta = (SkullMeta) itemToEquip.getItemMeta();
                        String finalTexture = null;
                        if (skullMeta != null) {
                            boolean isUnidentified = skullMeta.getPersistentDataContainer().has(unidentifiedKey, PersistentDataType.BYTE);
                            String pdcTexture = skullMeta.getPersistentDataContainer().get(textureKey, PersistentDataType.STRING);
                            boolean isUnidentifiedTexture = pdcTexture != null && pdcTexture.equals(HeadTexture.getUnidentified(plugin));

                            if (isUnidentified || isUnidentifiedTexture) {
                                String existingTexture = skullMeta.getPersistentDataContainer().get(textureKey, PersistentDataType.STRING);
                                if (existingTexture == null || existingTexture.equals(HeadTexture.getUnidentified(plugin))) {
                                    List<String> textures = new ArrayList<>(profession.getSkins().values());
                                    if (!textures.isEmpty()) {
                                        String activeTexture = textures.get(ThreadLocalRandom.current().nextInt(textures.size()));
                                        SkullTextures.apply(skullMeta, activeTexture);
                                        skullMeta.getPersistentDataContainer().set(textureKey, PersistentDataType.STRING, activeTexture);
                                        finalTexture = activeTexture;
                                    }
                                } else {
                                    finalTexture = existingTexture;
                                }

                                // A brand new identity has no alias yet, so it is
                                // named after its profession until one is claimed.
                                IdentityItemRenderer.apply(skullMeta, identityUuid, null, actualProfCode, 1);
                                skullMeta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, actualProfCode);
                                skullMeta.getPersistentDataContainer().remove(unidentifiedKey);
                                itemToEquip.setItemMeta(skullMeta);
                            } else {
                                finalTexture = pdcTexture;
                            }
                        }

                        final String resolvedFinalTexture = finalTexture;
                        CompletableFuture.runAsync(() -> {
                            String updatedBase64 = ItemStackSerializer.serialize(itemToEquip);
                            CreateIdentityInDB.execute(plugin, identityUuid, profession, resolvedFinalTexture).thenAccept(v -> {
                                    Schedulers.forEntity(player).run(plugin, st2 -> {
                                        MCIdentityProvider.instance.getStats(identityUuid).thenAccept(optStats -> {
                                            MCIdentityProvider.instance.getProperties(identityUuid).thenAccept(optProps -> {
                                                if (optStats.isPresent() && optProps.isPresent()) {
                                                    Schedulers.forEntity(player).run(plugin, st3 -> {
                                                        IdentityKey key = new IdentityKey() {
                                                            @Override public UUID getUniqueId() { return identityUuid; }
                                                            @Override public String getProfessionCode() { return actualProfCode; }
                                                        };
                                                        MCIdentityCreationEvent event = new MCIdentityCreationEvent(key, optProps.get(), optStats.get());
                                                        Bukkit.getPluginManager().callEvent(event);
                                                    }, null);
                                                }
                                            });
                                        });
                                    }, null);
                                    
                                    ProcessEquip.execute(plugin, player, identityUuid, itemToEquip, updatedBase64, reopenGui);
                                }).exceptionally(ex -> {
                                    String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                                    RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + MessageComponents.value(
                                            "mcidentity.gui.error.registration_failed.prefix",
                                            "Failed to register identity: ",
                                            errorMessage,
                                            "mcidentity.gui.error.registration_failed.suffix",
                                            ""
                                    ));
                                    return null;
                                });
                            }, task -> Schedulers.async().runNow(plugin, st -> task.run()));
                    }, null);
                });
            } else {
                IdentityKey key = optKey.get();
                String dbProfCode = key.getProfessionCode();

                // A stored base identity is permanently bound and never equipable
                // through an item: the primary player profession, and every
                // alternative (now of any profession) detected by its
                // deterministic UUID, so a bound identity can never be traded in.
                if (BaseProfession.isBaseProfession(dbProfCode)
                        || BaseProfession.identityIndex(player.getUniqueId(), identityUuid,
                            MCIdentityManager.getCachedBaseIdentitySlots(player.getUniqueId())) >= 0) {
                    RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "Base identities cannot be equipped from an item!");
                    return;
                }

                Schedulers.forEntity(player).run(plugin, st4 -> {
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
                                    // This identity is already stored, so it may
                                    // carry an alias and a level of its own; read
                                    // them rather than assuming a fresh level 1.
                                    MCIdentityProvider.instance.getProperties(identityUuid).thenAccept(optProps ->
                                        Schedulers.forEntity(player).run(plugin, st5 -> {
                                        String storedAlias = optProps.map(IdentityProperties::getAlias).orElse(null);
                                        int storedLevel = optProps.map(IdentityProperties::getLevel).orElse(1);
                                        IdentityItemRenderer.apply(skullMeta, identityUuid, storedAlias, dbProfCode, storedLevel);

                                        List<String> textures = new ArrayList<>(profession.getSkins().values());
                                        if (!textures.isEmpty()) {
                                            String activeTexture = textures.get(ThreadLocalRandom.current().nextInt(textures.size()));
                                            SkullTextures.apply(skullMeta, activeTexture);
                                        }

                                        skullMeta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, dbProfCode);
                                        skullMeta.getPersistentDataContainer().remove(unidentifiedKey);
                                        itemToEquip.setItemMeta(skullMeta);

                                        Schedulers.async().runNow(plugin, st -> {
                                            String base64 = ItemStackSerializer.serialize(itemToEquip);
                                            ProcessEquip.execute(plugin, player, identityUuid, itemToEquip, base64, reopenGui);
                                        });
                                    }, null));
                                } else {
                                    Schedulers.async().runNow(plugin, st -> {
                                        String base64 = ItemStackSerializer.serialize(itemToEquip);
                                        ProcessEquip.execute(plugin, player, identityUuid, itemToEquip, base64, reopenGui);
                                    });
                                }
                            });
                            return;
                        }
                    }
                    
                    Schedulers.async().runNow(plugin, st -> {
                        String base64 = ItemStackSerializer.serialize(itemToEquip);
                        ProcessEquip.execute(plugin, player, identityUuid, itemToEquip, base64, reopenGui);
                    });
                }, null);
            }
        }).exceptionally(ex -> {
            String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
            RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + MessageComponents.value(
                    "mcidentity.gui.error.fetch_identity_failed.prefix",
                    "Failed to fetch identity data: ",
                    errorMessage,
                    "mcidentity.gui.error.fetch_identity_failed.suffix",
                    ""
            ));
            return null;
        });
    }
}
