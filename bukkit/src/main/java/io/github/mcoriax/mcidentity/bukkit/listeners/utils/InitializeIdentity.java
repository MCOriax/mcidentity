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
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class InitializeIdentity {

    /**
     * Profession code asking for a random non-base class.
     */
    private static final String RANDOM_PROFESSION = "random";

    /**
     * Message shown when no profession can be chosen.
     */
    private static final String NO_PROFESSIONS = "No professions available!";

    /**
     * Everything one equip attempt needs, so the steps below can be read
     * without threading nine arguments through each of them.
     *
     * @param plugin The plugin instance.
     * @param player The player equipping the item.
     * @param identityUuid The identity UUID carried by the item.
     * @param professionCode The requested profession code, null for random.
     * @param itemToEquip The item stack being equipped.
     * @param reopenGui Whether the GUI reopens afterwards.
     * @param textureKey The NamespacedKey for texture.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     */
    private record EquipRequest(JavaPlugin plugin, Player player, UUID identityUuid, String professionCode,
                                ItemStack itemToEquip, boolean reopenGui, NamespacedKey textureKey,
                                NamespacedKey professionKey, NamespacedKey unidentifiedKey) {
    }

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
        EquipRequest request = new EquipRequest(plugin, player, identityUuid, professionCode, itemToEquip,
                reopenGui, textureKey, professionKey, unidentifiedKey);
        MCIdentityProvider.instance.getIdentityKey(identityUuid)
                .thenAccept(optKey -> optKey.ifPresentOrElse(
                        key -> equipStoredIdentity(request, key),
                        () -> registerNewIdentity(request)))
                .exceptionally(ex -> {
                    refund(request, ChatColor.RED + MessageComponents.value(
                            "mcidentity.gui.error.fetch_identity_failed.prefix",
                            "Failed to fetch identity data: ",
                            messageOf(ex),
                            "mcidentity.gui.error.fetch_identity_failed.suffix",
                            ""
                    ));
                    return null;
                });
    }

    /**
     * Creates the identity the item claims, which the database does not hold yet.
     * @param request The equip attempt in progress.
     */
    private static void registerNewIdentity(EquipRequest request) {
        // The base profession only exists as the permanent identity
        // bound to a player's UUID; it can never come from an item.
        if (request.professionCode() != null && BaseProfession.isBaseProfession(request.professionCode())) {
            refund(request, ChatColor.RED + "The base Player profession cannot be equipped from an item!");
            return;
        }
        MCIdentityProvider.instance.getProfessions().thenAccept(professions ->
                resolveProfession(request, professions)
                        .ifPresent(profession -> stampAndPersist(request, profession)));
    }

    /**
     * Chooses the profession the new identity is created with, refunding the
     * item and resolving empty when none can be chosen.
     * @param request The equip attempt in progress.
     * @param professions Every registered profession.
     * @return The chosen profession, or empty when the item was refunded.
     */
    private static Optional<Profession> resolveProfession(EquipRequest request, Collection<Profession> professions) {
        if (professions.isEmpty()) {
            refund(request, ChatColor.RED + NO_PROFESSIONS);
            return Optional.empty();
        }
        String code = request.professionCode();
        if (code == null || code.equalsIgnoreCase(RANDOM_PROFESSION)) {
            return resolveRandomProfession(request, professions);
        }
        Optional<Profession> named = findByCode(professions, code);
        if (named.isEmpty()) {
            refund(request, ChatColor.RED + MessageComponents.value(
                    "mcidentity.gui.error.profession_not_found.prefix",
                    "Profession '",
                    code,
                    "mcidentity.gui.error.profession_not_found.suffix",
                    "' not found!"
            ));
        }
        return named;
    }

    /**
     * Picks a random non-base profession, preferring one that is both loaded in
     * memory and registered in storage.
     * @param request The equip attempt in progress.
     * @param professions Every registered profession.
     * @return The chosen profession, or empty when the item was refunded.
     */
    private static Optional<Profession> resolveRandomProfession(EquipRequest request, Collection<Profession> professions) {
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
            refund(request, ChatColor.RED + NO_PROFESSIONS);
            return Optional.empty();
        }
        return Optional.of(pickRandom(selectable));
    }

    /**
     * Stamps the item on the player's thread, then writes the identity away.
     * @param request The equip attempt in progress.
     * @param profession The profession the identity is created with.
     */
    private static void stampAndPersist(EquipRequest request, Profession profession) {
        String actualProfCode = profession.getSystemCode();
        Schedulers.forEntity(request.player()).run(request.plugin(), st1 -> {
            String finalTexture = stampNewIdentity(request, profession, actualProfCode);
            persistNewIdentity(request, profession, actualProfCode, finalTexture);
        }, null);
    }

    /**
     * Writes the profession, texture, and display name onto an unidentified item.
     * @param request The equip attempt in progress.
     * @param profession The profession the identity is created with.
     * @param actualProfCode The profession's system code.
     * @return The texture the identity ends up with, or null when it has none.
     */
    private static String stampNewIdentity(EquipRequest request, Profession profession, String actualProfCode) {
        SkullMeta skullMeta = (SkullMeta) request.itemToEquip().getItemMeta();
        if (skullMeta == null) {
            return null;
        }

        boolean isUnidentified = skullMeta.getPersistentDataContainer()
                .has(request.unidentifiedKey(), PersistentDataType.BYTE);
        String pdcTexture = skullMeta.getPersistentDataContainer()
                .get(request.textureKey(), PersistentDataType.STRING);
        boolean isUnidentifiedTexture = pdcTexture != null
                && pdcTexture.equals(HeadTexture.getUnidentified(request.plugin()));
        if (!isUnidentified && !isUnidentifiedTexture) {
            return pdcTexture;
        }

        String finalTexture = applyRandomSkin(request, skullMeta, profession, pdcTexture);

        // A brand new identity has no alias yet, so it is
        // named after its profession until one is claimed.
        IdentityItemRenderer.apply(skullMeta, request.identityUuid(), null, actualProfCode, 1);
        skullMeta.getPersistentDataContainer().set(request.professionKey(), PersistentDataType.STRING, actualProfCode);
        skullMeta.getPersistentDataContainer().remove(request.unidentifiedKey());
        request.itemToEquip().setItemMeta(skullMeta);
        return finalTexture;
    }

    /**
     * Gives an unskinned item one of its profession's skins.
     * @param request The equip attempt in progress.
     * @param skullMeta The item's meta, mutated in place.
     * @param profession The profession supplying the skins.
     * @param existingTexture The texture already on the item, if any.
     * @return The texture now on the item, or null when the profession has none.
     */
    private static String applyRandomSkin(EquipRequest request, SkullMeta skullMeta, Profession profession, String existingTexture) {
        if (existingTexture != null && !existingTexture.equals(HeadTexture.getUnidentified(request.plugin()))) {
            return existingTexture;
        }
        List<String> textures = new ArrayList<>(profession.getSkins().values());
        if (textures.isEmpty()) {
            return null;
        }
        String activeTexture = pickRandom(textures);
        SkullTextures.apply(skullMeta, activeTexture);
        skullMeta.getPersistentDataContainer().set(request.textureKey(), PersistentDataType.STRING, activeTexture);
        return activeTexture;
    }

    /**
     * Serializes the stamped item and creates its identity in storage.
     * @param request The equip attempt in progress.
     * @param profession The profession the identity is created with.
     * @param actualProfCode The profession's system code.
     * @param finalTexture The texture the identity ends up with.
     */
    private static void persistNewIdentity(EquipRequest request, Profession profession, String actualProfCode, String finalTexture) {
        CompletableFuture.runAsync(() -> {
            String updatedBase64 = ItemStackSerializer.serialize(request.itemToEquip());
            CreateIdentityInDB.execute(request.plugin(), request.identityUuid(), profession, finalTexture)
                    .thenAccept(v -> {
                        fireCreationEvent(request, actualProfCode);
                        ProcessEquip.execute(request.plugin(), request.player(), request.identityUuid(),
                                request.itemToEquip(), updatedBase64, request.reopenGui());
                    })
                    .exceptionally(ex -> {
                        refund(request, ChatColor.RED + MessageComponents.value(
                                "mcidentity.gui.error.registration_failed.prefix",
                                "Failed to register identity: ",
                                messageOf(ex),
                                "mcidentity.gui.error.registration_failed.suffix",
                                ""
                        ));
                        return null;
                    });
        }, task -> Schedulers.async().runNow(request.plugin(), st -> task.run()));
    }

    /**
     * Announces the freshly created identity once its stats and properties are readable.
     * @param request The equip attempt in progress.
     * @param actualProfCode The profession's system code.
     */
    private static void fireCreationEvent(EquipRequest request, String actualProfCode) {
        Schedulers.forEntity(request.player()).run(request.plugin(), st2 ->
                MCIdentityProvider.instance.getStats(request.identityUuid()).thenAccept(optStats ->
                        MCIdentityProvider.instance.getProperties(request.identityUuid()).thenAccept(optProps -> {
                            if (optStats.isEmpty() || optProps.isEmpty()) {
                                return;
                            }
                            Schedulers.forEntity(request.player()).run(request.plugin(), st3 -> {
                                IdentityKey key = new IdentityKey() {
                                    @Override public UUID getUniqueId() { return request.identityUuid(); }
                                    @Override public String getProfessionCode() { return actualProfCode; }
                                };
                                MCIdentityCreationEvent event = new MCIdentityCreationEvent(key, optProps.get(), optStats.get());
                                Bukkit.getPluginManager().callEvent(event);
                            }, null);
                        })), null);
    }

    /**
     * Equips an identity storage already knows about.
     * @param request The equip attempt in progress.
     * @param key The stored identity key.
     */
    private static void equipStoredIdentity(EquipRequest request, IdentityKey key) {
        String dbProfCode = key.getProfessionCode();

        // A stored base identity is permanently bound and never equipable
        // through an item: the primary player profession, and every
        // alternative (now of any profession) detected by its
        // deterministic UUID, so a bound identity can never be traded in.
        if (BaseProfession.isBaseProfession(dbProfCode)
                || BaseProfession.identityIndex(request.player().getUniqueId(), request.identityUuid(),
                    MCIdentityManager.getCachedBaseIdentitySlots(request.player().getUniqueId())) >= 0) {
            refund(request, ChatColor.RED + "Base identities cannot be equipped from an item!");
            return;
        }

        Schedulers.forEntity(request.player()).run(request.plugin(), st4 -> {
            SkullMeta skullMeta = (SkullMeta) request.itemToEquip().getItemMeta();
            if (skullMeta == null || !needsRestamp(request, skullMeta)) {
                equipSerialized(request);
                return;
            }
            restampStoredIdentity(request, skullMeta, dbProfCode);
        }, null);
    }

    /**
     * Reports whether the item still shows as unidentified or random.
     * @param request The equip attempt in progress.
     * @param skullMeta The item's meta.
     * @return true when the item must be re-stamped before it is equipped.
     */
    private static boolean needsRestamp(EquipRequest request, SkullMeta skullMeta) {
        boolean isUnidentified = skullMeta.getPersistentDataContainer()
                .has(request.unidentifiedKey(), PersistentDataType.BYTE);
        String currentProf = skullMeta.getPersistentDataContainer()
                .get(request.professionKey(), PersistentDataType.STRING);
        return isUnidentified || RANDOM_PROFESSION.equalsIgnoreCase(currentProf);
    }

    /**
     * Re-stamps an unidentified item from the identity storage already holds.
     * @param request The equip attempt in progress.
     * @param skullMeta The item's meta, mutated in place.
     * @param dbProfCode The stored profession code.
     */
    private static void restampStoredIdentity(EquipRequest request, SkullMeta skullMeta, String dbProfCode) {
        MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
            Optional<Profession> optProf = findByCode(professions, dbProfCode);
            if (optProf.isEmpty()) {
                equipSerialized(request);
                return;
            }
            Profession profession = optProf.get();
            // This identity is already stored, so it may
            // carry an alias and a level of its own; read
            // them rather than assuming a fresh level 1.
            MCIdentityProvider.instance.getProperties(request.identityUuid()).thenAccept(optProps ->
                    Schedulers.forEntity(request.player()).run(request.plugin(), st5 -> {
                        applyStoredIdentity(request, skullMeta, profession, optProps, dbProfCode);
                        equipSerialized(request);
                    }, null));
        });
    }

    /**
     * Writes the stored alias, level, profession, and a skin onto the item.
     * @param request The equip attempt in progress.
     * @param skullMeta The item's meta, mutated in place.
     * @param profession The stored profession.
     * @param optProps The stored properties, if any.
     * @param dbProfCode The stored profession code.
     */
    private static void applyStoredIdentity(EquipRequest request, SkullMeta skullMeta, Profession profession,
                                            Optional<IdentityProperties> optProps, String dbProfCode) {
        String storedAlias = optProps.map(IdentityProperties::getAlias).orElse(null);
        int storedLevel = optProps.map(IdentityProperties::getLevel).orElse(1);
        IdentityItemRenderer.apply(skullMeta, request.identityUuid(), storedAlias, dbProfCode, storedLevel);

        List<String> textures = new ArrayList<>(profession.getSkins().values());
        if (!textures.isEmpty()) {
            SkullTextures.apply(skullMeta, pickRandom(textures));
        }

        skullMeta.getPersistentDataContainer().set(request.professionKey(), PersistentDataType.STRING, dbProfCode);
        skullMeta.getPersistentDataContainer().remove(request.unidentifiedKey());
        request.itemToEquip().setItemMeta(skullMeta);
    }

    /**
     * Serializes the item off the game thread and hands it to the equip pipeline.
     * @param request The equip attempt in progress.
     */
    private static void equipSerialized(EquipRequest request) {
        Schedulers.async().runNow(request.plugin(), st -> {
            String base64 = ItemStackSerializer.serialize(request.itemToEquip());
            ProcessEquip.execute(request.plugin(), request.player(), request.identityUuid(),
                    request.itemToEquip(), base64, request.reopenGui());
        });
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
     * @param request The equip attempt in progress.
     * @param message The message shown to the player.
     */
    private static void refund(EquipRequest request, String message) {
        RefundItemSafely.execute(request.plugin(), request.player(), request.itemToEquip(), message);
    }

    /**
     * Reads a failure's message, falling back when it carries none.
     * @param ex The failure.
     * @return The message to show.
     */
    private static String messageOf(Throwable ex) {
        return ex.getMessage() != null ? ex.getMessage() : "Unknown error";
    }
}
