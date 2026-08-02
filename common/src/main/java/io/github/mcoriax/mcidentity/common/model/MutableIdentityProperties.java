package io.github.mcoriax.mcidentity.common.model;

import io.github.mcoriax.mcidentity.api.model.IdentityProperties;

/**
 * Mutable, field-backed implementation of {@link IdentityProperties}.
 *
 * <p>Centralises the property carrier that was previously duplicated as
 * anonymous classes across the database read paths and the provider's storage
 * update logic.</p>
 */
public final class MutableIdentityProperties implements IdentityProperties {

    private final String alias;
    private final int level;
    private final int exp;
    private final int statPoints;
    private final String headText;
    private String inventoryContents;
    private String enderchestContents;

    /**
     * Creates a new mutable properties holder.
     * @param alias The unique alias, or null when the identity has none.
     * @param level The current level.
     * @param exp The current experience points.
     * @param statPoints The unspent stat points.
     * @param headText The head texture string.
     * @param inventoryContents The serialized inventory contents.
     * @param enderchestContents The serialized enderchest contents.
     */
    public MutableIdentityProperties(String alias, int level, int exp, int statPoints, String headText,
                                     String inventoryContents, String enderchestContents) {
        this.alias = alias;
        this.level = level;
        this.exp = exp;
        this.statPoints = statPoints;
        this.headText = headText;
        this.inventoryContents = inventoryContents;
        this.enderchestContents = enderchestContents;
    }

    /**
     * Creates a mutable copy of the given properties.
     * @param source The properties to copy.
     * @return A new mutable copy of {@code source}.
     */
    public static MutableIdentityProperties copyOf(IdentityProperties source) {
        return new MutableIdentityProperties(
            source.getAlias(), source.getLevel(), source.getExp(), source.getStatPoints(), source.getHeadText(),
            source.getInventoryContents(), source.getEnderchestContents()
        );
    }

    /**
     * Creates a copy of the given properties carrying a different alias, used
     * to refresh the RAM session after an alias has been claimed.
     * @param source The properties to copy.
     * @param alias The alias the copy should carry.
     * @return A new mutable copy of {@code source} with {@code alias} applied.
     */
    public static MutableIdentityProperties withAlias(IdentityProperties source, String alias) {
        return new MutableIdentityProperties(
            alias, source.getLevel(), source.getExp(), source.getStatPoints(), source.getHeadText(),
            source.getInventoryContents(), source.getEnderchestContents()
        );
    }

    @Override public String getAlias() { return alias; }
    @Override public int getLevel() { return level; }
    @Override public int getExp() { return exp; }
    @Override public int getStatPoints() { return statPoints; }
    @Override public String getHeadText() { return headText; }
    @Override public String getInventoryContents() { return inventoryContents; }
    @Override public void setInventoryContents(String inventoryContents) { this.inventoryContents = inventoryContents; }
    @Override public String getEnderchestContents() { return enderchestContents; }
    @Override public void setEnderchestContents(String enderchestContents) { this.enderchestContents = enderchestContents; }
}
