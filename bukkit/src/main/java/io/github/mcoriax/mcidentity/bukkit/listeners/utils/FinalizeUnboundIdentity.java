package io.github.mcoriax.mcidentity.bukkit.listeners.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.IdentityKey;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
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
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FinalizeUnboundIdentity {

    /**
     * Profession code asking for a random non-base class.
     */
    private static final String RANDOM_PROFESSION = "random";

    /**
     * How many random cuts split the rolled stat points across six stats.
     */
    private static final int STAT_CUTS = 5;

    /**
     * Everything one finalize attempt needs, so the steps below can be read
     * without threading nine arguments through each of them.
     *
     * @param plugin The plugin instance.
     * @param player The player finalizing the identity.
     * @param itemToEquip The item stack to update and equip.
     * @param professionCode The profession code for the identity.
     * @param reopenGui Whether the GUI reopens afterwards.
     * @param textureKey The NamespacedKey for texture.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     * @param identityKey The NamespacedKey for identity UUID.
     */
    private record BindRequest(JavaPlugin plugin, Player player, ItemStack itemToEquip, String professionCode,
                               boolean reopenGui, NamespacedKey textureKey, NamespacedKey professionKey,
                               NamespacedKey unidentifiedKey, NamespacedKey identityKey) {
    }

    /**
     * Finalize an unbound identity for a player.
     * @param plugin The plugin instance.
     * @param player The player finalizing the identity.
     * @param itemToEquip The item stack to update and equip.
     * @param professionCode The profession code for the identity.
     * @param reopenGui Whether to reopen the GUI after finalization.
     * @param textureKey The NamespacedKey for texture.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     * @param identityKey The NamespacedKey for identity UUID.
     */
    public static void execute(JavaPlugin plugin, Player player, ItemStack itemToEquip, String professionCode, boolean reopenGui, NamespacedKey textureKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey, NamespacedKey identityKey) {
        // The base profession only exists as the permanent identity bound to
        // a player's UUID; it can never come from an item.
        if (BaseProfession.isBaseProfession(professionCode)) {
            RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "The base Player profession cannot be equipped from an item!");
            return;
        }

        BindRequest request = new BindRequest(plugin, player, itemToEquip, professionCode, reopenGui,
                textureKey, professionKey, unidentifiedKey, identityKey);
        MCIdentityProvider.instance.getProfessions().thenAccept(professions ->
                resolveProfession(request, professions)
                        .ifPresent(profession -> createBoundIdentity(request, profession)));
    }

    /**
     * Chooses the profession the item binds to, refunding the item and
     * resolving empty when none can be chosen.
     * @param request The finalize attempt in progress.
     * @param professions Every registered profession.
     * @return The chosen profession, or empty when the item was refunded.
     */
    private static Optional<Profession> resolveProfession(BindRequest request, Collection<Profession> professions) {
        if (request.professionCode().equalsIgnoreCase(RANDOM_PROFESSION)) {
            return resolveRandomProfession(request, professions);
        }
        Optional<Profession> named = findByCode(professions, request.professionCode());
        if (named.isEmpty()) {
            refund(request, ChatColor.RED + MessageComponents.value(
                    "mcidentity.gui.error.profession_not_found.prefix",
                    "Error: Profession '",
                    request.professionCode(),
                    "mcidentity.gui.error.profession_not_found.suffix",
                    "' no longer exists!"
            ));
        }
        return named;
    }

    /**
     * Picks a random non-base profession, preferring one that is both loaded in
     * memory and registered in storage.
     * @param request The finalize attempt in progress.
     * @param professions Every registered profession.
     * @return The chosen profession, or empty when the item was refunded.
     */
    private static Optional<Profession> resolveRandomProfession(BindRequest request, Collection<Profession> professions) {
        List<String> loadedProfCodes = new ArrayList<>(MCIdentityManager.getRegisteredProfessions().keySet());
        loadedProfCodes.remove(BaseProfession.CODE);
        if (loadedProfCodes.isEmpty()) {
            refund(request, ChatColor.RED + "No professions loaded in memory!");
            return Optional.empty();
        }

        Optional<Profession> picked = findByCode(professions, pickRandom(loadedProfCodes));
        if (picked.isPresent()) {
            return picked;
        }

        List<Profession> selectable = professions.stream()
                .filter(p -> !BaseProfession.isBaseProfession(p.getSystemCode()))
                .toList();
        if (selectable.isEmpty()) {
            refund(request, ChatColor.RED + "No professions available!");
            return Optional.empty();
        }
        return Optional.of(pickRandom(selectable));
    }

    /**
     * Creates the identity in storage, then stamps and equips the item.
     * @param request The finalize attempt in progress.
     * @param profession The profession the item binds to.
     */
    private static void createBoundIdentity(BindRequest request, Profession profession) {
        String actualProfCode = profession.getSystemCode();
        UUID identityUuid = UUID.randomUUID();
        String finalTexture = resolveTexture(request, profession);

        int pointsToDistribute = rollStatPoints(request.plugin(), profession.getStatsRandom(), actualProfCode);
        int[] spread = spreadPoints(pointsToDistribute);

        IdentityKey key = new IdentityKey() {
            @Override public UUID getUniqueId() { return identityUuid; }
            @Override public String getProfessionCode() { return actualProfCode; }
        };
        IdentityProperties props = newProperties(finalTexture);
        IdentityStats stats = newStats(profession.getBaseStats(), spread);

        MCIdentityProvider.instance.createIdentity(key, props, stats).thenAccept(v -> {
            Schedulers.forEntity(request.player()).run(request.plugin(), st1 -> {
                MCIdentityCreationEvent event = new MCIdentityCreationEvent(key, props, stats);
                Bukkit.getPluginManager().callEvent(event);
            }, null);

            Schedulers.forEntity(request.player()).run(request.plugin(), st2 -> {
                stampBoundItem(request, identityUuid, actualProfCode, finalTexture);
                Schedulers.async().runNow(request.plugin(), st -> {
                    String updatedBase64 = ItemStackSerializer.serialize(request.itemToEquip());
                    ProcessEquip.execute(request.plugin(), request.player(), identityUuid,
                            request.itemToEquip(), updatedBase64, request.reopenGui());
                });
            }, null);
        }).exceptionally(ex -> {
            String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
            refund(request, ChatColor.RED + MessageComponents.value(
                    "mcidentity.gui.error.registration_failed.prefix",
                    "Failed to register identity: ",
                    errorMessage,
                    "mcidentity.gui.error.registration_failed.suffix",
                    ""
            ));
            return null;
        });
    }

    /**
     * Keeps the item's own texture, or gives it one of the profession's skins
     * when it is still unidentified.
     * @param request The finalize attempt in progress.
     * @param profession The profession supplying the skins.
     * @return The texture the identity is created with, or null when it has none.
     */
    private static String resolveTexture(BindRequest request, Profession profession) {
        SkullMeta meta = (SkullMeta) request.itemToEquip().getItemMeta();
        if (meta == null) {
            return null;
        }

        String resolved = meta.getPersistentDataContainer().get(request.textureKey(), PersistentDataType.STRING);
        boolean isUnidentified = meta.getPersistentDataContainer().has(request.unidentifiedKey(), PersistentDataType.BYTE);
        boolean keepsOwnTexture = resolved != null
                && !resolved.equals(HeadTexture.getUnidentified(request.plugin()))
                && !isUnidentified;
        if (keepsOwnTexture) {
            return resolved;
        }

        List<String> textures = new ArrayList<>(profession.getSkins().values());
        return textures.isEmpty() ? resolved : pickRandom(textures);
    }

    /**
     * Reads the profession's {@code stats_random} setting, which is either a
     * fixed number or a {@code min~max} range.
     * @param plugin The plugin instance, used to report a bad setting.
     * @param statsRandomConfig The configured value.
     * @param actualProfCode The profession's system code, named in the warning.
     * @return The number of points to spread, or 0 when the setting is unusable.
     */
    private static int rollStatPoints(JavaPlugin plugin, String statsRandomConfig, String actualProfCode) {
        try {
            if (!statsRandomConfig.contains("~")) {
                return Integer.parseInt(statsRandomConfig.trim());
            }
            String[] parts = statsRandomConfig.split("~");
            if (parts.length != 2) {
                return 0;
            }
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            return ThreadLocalRandom.current().nextInt((max - min) + 1) + min;
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid stats_random configuration for " + actualProfCode + ": " + statsRandomConfig);
            return 0;
        }
    }

    /**
     * Splits the rolled points across six stats by taking five random cuts.
     * @param pointsToDistribute The number of points to spread.
     * @return Six additions, in strength, defense, wisdom, luck, agility, resilience order.
     */
    private static int[] spreadPoints(int pointsToDistribute) {
        int[] cuts = new int[STAT_CUTS];
        if (pointsToDistribute > 0) {
            for (int i = 0; i < STAT_CUTS; i++) {
                cuts[i] = ThreadLocalRandom.current().nextInt(pointsToDistribute + 1);
            }
            Arrays.sort(cuts);
        }
        return new int[] {
            cuts[0],
            cuts[1] - cuts[0],
            cuts[2] - cuts[1],
            cuts[3] - cuts[2],
            cuts[4] - cuts[3],
            pointsToDistribute - cuts[4]
        };
    }

    /**
     * Builds the properties a freshly bound identity starts with.
     * @param finalTexture The identity's head texture, or null.
     * @return The new properties.
     */
    private static IdentityProperties newProperties(String finalTexture) {
        return new IdentityProperties() {
            private String inventoryContents;
            private String enderchestContents;

            @Override public String getAlias() { return null; }
            @Override public int getLevel() { return 1; }
            @Override public int getExp() { return 0; }
            @Override public int getStatPoints() { return 0; }
            @Override public String getHeadText() { return finalTexture; }
            @Override public String getInventoryContents() { return inventoryContents; }
            @Override public void setInventoryContents(String inventoryContents) { this.inventoryContents = inventoryContents; }
            @Override public String getEnderchestContents() { return enderchestContents; }
            @Override public void setEnderchestContents(String enderchestContents) { this.enderchestContents = enderchestContents; }
        };
    }

    /**
     * Builds the profession's base stats with the rolled points added on.
     * @param baseStats The profession's configured base stats.
     * @param spread The six additions from {@link #spreadPoints(int)}.
     * @return The new stats.
     */
    private static IdentityStats newStats(Map<String, Integer> baseStats, int[] spread) {
        int energyVal = baseStats.getOrDefault("energy", 100);
        int healthVal = baseStats.getOrDefault("health", 20);
        int foodVal = baseStats.getOrDefault("food", 20);
        int strengthVal = baseStats.getOrDefault("strength", 0) + spread[0];
        int defenseVal = baseStats.getOrDefault("defense", 0) + spread[1];
        int wisdomVal = baseStats.getOrDefault("wisdom", 0) + spread[2];
        int luckVal = baseStats.getOrDefault("luck", 0) + spread[3];
        int agilityVal = baseStats.getOrDefault("agility", 0) + spread[4];
        int resilienceVal = baseStats.getOrDefault("resilience", 0) + spread[5];

        return new IdentityStats() {
            @Override public int getEnergy() { return energyVal; }
            @Override public int getHealth() { return healthVal; }
            @Override public int getFood() { return foodVal; }
            @Override public int getStrength() { return strengthVal; }
            @Override public int getDefense() { return defenseVal; }
            @Override public int getWisdom() { return wisdomVal; }
            @Override public int getLuck() { return luckVal; }
            @Override public int getAgility() { return agilityVal; }
            @Override public int getResilience() { return resilienceVal; }
        };
    }

    /**
     * Writes the new identity UUID, texture, name, and profession onto the item.
     * @param request The finalize attempt in progress.
     * @param identityUuid The identity the item now carries.
     * @param actualProfCode The profession's system code.
     * @param finalTexture The identity's head texture, or null.
     */
    private static void stampBoundItem(BindRequest request, UUID identityUuid, String actualProfCode, String finalTexture) {
        SkullMeta updatedMeta = (SkullMeta) request.itemToEquip().getItemMeta();
        if (updatedMeta == null) {
            return;
        }

        updatedMeta.getPersistentDataContainer().set(request.identityKey(), PersistentDataType.STRING, identityUuid.toString());

        if (finalTexture != null) {
            SkullTextures.apply(updatedMeta, finalTexture);
            updatedMeta.getPersistentDataContainer().set(request.textureKey(), PersistentDataType.STRING, finalTexture);
        } else {
            updatedMeta.getPersistentDataContainer().remove(request.textureKey());
        }

        if (updatedMeta.getPersistentDataContainer().has(request.unidentifiedKey(), PersistentDataType.BYTE)) {
            // Freshly bound, so it starts at level 1 with no alias
            // and is named after its profession until one is claimed.
            IdentityItemRenderer.apply(updatedMeta, identityUuid, null, actualProfCode, 1);
            updatedMeta.getPersistentDataContainer().remove(request.unidentifiedKey());
        }

        updatedMeta.getPersistentDataContainer().set(request.professionKey(), PersistentDataType.STRING, actualProfCode);
        request.itemToEquip().setItemMeta(updatedMeta);
    }

    /**
     * Finds a profession by its system code, ignoring case.
     * @param professions Every registered profession.
     * @param code The system code to match.
     * @return The matching profession, or empty.
     */
    private static Optional<Profession> findByCode(Collection<Profession> professions, String code) {
        return professions.stream()
                .filter(p -> p.getSystemCode().equalsIgnoreCase(code))
                .findFirst();
    }

    /**
     * Picks one element uniformly at random.
     * @param values The non-empty list to pick from.
     * @param <T> The element type.
     * @return The chosen element.
     */
    private static <T> T pickRandom(List<T> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }

    /**
     * Returns the item to the player with an explanation.
     * @param request The finalize attempt in progress.
     * @param message The message shown to the player.
     */
    private static void refund(BindRequest request, String message) {
        RefundItemSafely.execute(request.plugin(), request.player(), request.itemToEquip(), message);
    }
}
