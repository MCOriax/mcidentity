package io.gitlab.mcoriax.foliamc.commands.utils;

import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RenderIdentityInfo {
    /**
     * Render identity information to a player's chat.
     */
    public static void execute(Player player, UUID uuid, IdentityKey key, IdentityProperties props, IdentityStats stats) {
        player.sendMessage(Component.translatable("mcidentity.info.header", "--- Identity Data ---").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        
        player.sendMessage(infoLine("mcidentity.info.uuid", "UUID", uuid.toString(), NamedTextColor.WHITE));
        player.sendMessage(infoLine("mcidentity.info.profession", "Profession", key.getProfessionCode(), NamedTextColor.GREEN));

        player.sendMessage(infoLine("mcidentity.info.level", "Level", String.valueOf(props.getLevel()), NamedTextColor.YELLOW));
        player.sendMessage(infoLine("mcidentity.info.exp", "Exp", String.valueOf(props.getExp()), NamedTextColor.YELLOW));
        player.sendMessage(infoLine("mcidentity.info.stat_points", "Stat Points", String.valueOf(props.getStatPoints()), NamedTextColor.YELLOW));

        player.sendMessage(infoLine("mcidentity.info.health", "Health", String.valueOf(stats.getHealth()), NamedTextColor.RED));
        player.sendMessage(infoLine("mcidentity.info.food", "Food", String.valueOf(stats.getFood()), NamedTextColor.GOLD));
        player.sendMessage(infoLine("mcidentity.info.energy", "Energy", String.valueOf(stats.getEnergy()), NamedTextColor.BLUE));
        player.sendMessage(infoLine("mcidentity.info.strength", "Strength", String.valueOf(stats.getStrength()), NamedTextColor.DARK_RED));
        player.sendMessage(infoLine("mcidentity.info.defense", "Defense", String.valueOf(stats.getDefense()), NamedTextColor.GRAY));
        player.sendMessage(infoLine("mcidentity.info.wisdom", "Wisdom", String.valueOf(stats.getWisdom()), NamedTextColor.LIGHT_PURPLE));
        player.sendMessage(infoLine("mcidentity.info.luck", "Luck", String.valueOf(stats.getLuck()), NamedTextColor.GOLD));
        player.sendMessage(infoLine("mcidentity.info.agility", "Agility", String.valueOf(stats.getAgility()), NamedTextColor.YELLOW));
        player.sendMessage(infoLine("mcidentity.info.resilience", "Resilience", String.valueOf(stats.getResilience()), NamedTextColor.DARK_GREEN));
    }

    private static Component infoLine(String key, String label, String value, NamedTextColor valueColor) {
        return Component.translatable(key + ".label", label + ": ").color(NamedTextColor.GRAY)
                .append(Component.translatable(key + ".value", value, Component.text(value)).color(valueColor));
    }
}
