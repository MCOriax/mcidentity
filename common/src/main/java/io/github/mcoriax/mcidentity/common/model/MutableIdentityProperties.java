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

    private final int level;
    private final int exp;
    private final int statPoints;
    private final String headText;
    private String skin;
    private String aura;
    private String inventoryContents;
    private String enderchestContents;

    /**
     * Creates a new mutable properties holder.
     * @param level The current level.
     * @param exp The current experience points.
     * @param statPoints The unspent stat points.
     * @param headText The head texture string.
     * @param skin The skin cosmetic string.
     * @param aura The aura cosmetic code.
     * @param inventoryContents The serialized inventory contents.
     * @param enderchestContents The serialized enderchest contents.
     */
    public MutableIdentityProperties(int level, int exp, int statPoints, String headText,
                                     String skin, String aura,
                                     String inventoryContents, String enderchestContents) {
        this.level = level;
        this.exp = exp;
        this.statPoints = statPoints;
        this.headText = headText;
        this.skin = skin;
        this.aura = aura;
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
            source.getLevel(), source.getExp(), source.getStatPoints(), source.getHeadText(),
            source.getSkin(), source.getAura(),
            source.getInventoryContents(), source.getEnderchestContents()
        );
    }

    @Override public int getLevel() { return level; }
    @Override public int getExp() { return exp; }
    @Override public int getStatPoints() { return statPoints; }
    @Override public String getHeadText() { return headText; }
    @Override public String getSkin() { return skin; }
    @Override public void setSkin(String skin) { this.skin = skin; }
    @Override public String getAura() { return aura; }
    @Override public void setAura(String aura) { this.aura = aura; }
    @Override public String getInventoryContents() { return inventoryContents; }
    @Override public void setInventoryContents(String inventoryContents) { this.inventoryContents = inventoryContents; }
    @Override public String getEnderchestContents() { return enderchestContents; }
    @Override public void setEnderchestContents(String enderchestContents) { this.enderchestContents = enderchestContents; }
}
