package io.github.mcoriax.mcidentity.bukkit.util;

import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.ChatColor;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Builds the name and lore shown on an identity — the item a player carries and
 * the heads in the Player Identity GUI alike.
 *
 * <p>An identity is named by its alias, with its profession and level below in
 * the lore. Both are rendered from data, never read back out of the item, so
 * the presentation is rebuilt from scratch every time it changes rather than
 * edited in place. That is what lets a player rename an identity item or clear
 * its lore without breaking anything: the plugin identifies an identity solely
 * by its persistent data container, and this class overwrites whatever text is
 * there the next time the identity is rendered.</p>
 *
 * <p>The alias colour is derived from the identity UUID rather than picked
 * fresh each time, so an identity keeps one stable colour across relogs and
 * re-renders without anything extra having to be stored for it.</p>
 */
public final class IdentityItemRenderer {

    /**
     * The colours an identity's name may take, chosen by its UUID. Dark greys
     * and black are left out so the name stays legible against the tooltip.
     */
    private static final ChatColor[] NAME_COLORS = {
        ChatColor.AQUA, ChatColor.BLUE, ChatColor.DARK_AQUA,
        ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_PURPLE,
        ChatColor.DARK_RED, ChatColor.GOLD, ChatColor.GREEN,
        ChatColor.LIGHT_PURPLE, ChatColor.RED, ChatColor.WHITE,
        ChatColor.YELLOW
    };

    /**
     * Prevents instantiation of this utility holder.
     */
    private IdentityItemRenderer() {
    }

    /**
     * Writes the name and lore of an identity onto its item meta, replacing
     * whatever was there.
     * @param meta The item meta to write onto.
     * @param identityUuid The identity being rendered, used to pick its colour.
     * @param alias The identity's alias, or null when it has none.
     * @param professionCode The identity's profession system code.
     * @param level The identity's current level.
     */
    public static void apply(ItemMeta meta, UUID identityUuid, String alias, String professionCode, int level) {
        if (meta == null) {
            return;
        }
        meta.setDisplayName(name(identityUuid, alias, professionCode));
        meta.setLore(lore(professionCode, level));
    }

    /**
     * Builds the display name of an identity: its alias, or the profession's
     * display name when no alias has been claimed, so an identity is never
     * left nameless.
     * @param identityUuid The identity being rendered, used to pick its colour.
     * @param alias The identity's alias, or null when it has none.
     * @param professionCode The identity's profession system code.
     * @return The display name.
     */
    public static String name(UUID identityUuid, String alias, String professionCode) {
        String shown = alias != null && !alias.isBlank()
            ? alias
            : MCIdentityManager.getProfessionDisplayName(professionCode);
        return color(identityUuid) + shown;
    }

    /**
     * Builds the lore of an identity: its profession and its level.
     * @param professionCode The identity's profession system code.
     * @param level The identity's current level.
     * @return The lore lines.
     */
    public static List<String> lore(String professionCode, int level) {
        String professionName = MCIdentityManager.getProfessionDisplayName(professionCode);
        int maxLevel = MCIdentityManager.getProfessionLevelConfig(
            professionCode == null ? "" : professionCode.toLowerCase()).maxLevel();
        String levelShown = level >= maxLevel ? "MAX" : String.valueOf(level);
        return List.of(
            ChatColor.GRAY + "Profession: " + ChatColor.WHITE + professionName,
            ChatColor.GRAY + "Level: " + ChatColor.WHITE + levelShown
        );
    }

    /**
     * Picks the identity's stable name colour from its UUID.
     * @param identityUuid The identity being rendered, or null for a default.
     * @return The colour its name is written in.
     */
    private static ChatColor color(UUID identityUuid) {
        if (identityUuid == null) {
            return ChatColor.WHITE;
        }
        long seed = identityUuid.getMostSignificantBits() ^ identityUuid.getLeastSignificantBits();
        return NAME_COLORS[new Random(seed).nextInt(NAME_COLORS.length)];
    }
}
