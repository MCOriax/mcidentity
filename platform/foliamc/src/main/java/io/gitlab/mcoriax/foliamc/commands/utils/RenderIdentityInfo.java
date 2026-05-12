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
        player.sendMessage(Component.translatable("mcidentity.info.uuid", "UUID: {0}", Component.text(uuid.toString(), NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
        player.sendMessage(Component.translatable("mcidentity.info.profession", "Profession: {0}", Component.text(key.getProfessionCode(), NamedTextColor.GREEN)).color(NamedTextColor.GRAY));

        player.sendMessage(Component.translatable("mcidentity.info.level", "Level: {0}", Component.text(props.getLevel(), NamedTextColor.YELLOW)).color(NamedTextColor.GRAY));
        player.sendMessage(Component.translatable("mcidentity.info.exp", "Exp: {0}", Component.text(props.getExp(), NamedTextColor.YELLOW)).color(NamedTextColor.GRAY));
        player.sendMessage(Component.translatable("mcidentity.info.stat_points", "Stat Points: {0}", Component.text(props.getStatPoints(), NamedTextColor.YELLOW)).color(NamedTextColor.GRAY));

        player.sendMessage(Component.translatable("mcidentity.info.health", "Health: {0}", Component.text(stats.getHealth(), NamedTextColor.RED)).color(NamedTextColor.GRAY));
        player.sendMessage(Component.translatable("mcidentity.info.food", "Food: {0}", Component.text(stats.getFood(), NamedTextColor.GOLD)).color(NamedTextColor.GRAY));
        player.sendMessage(Component.translatable("mcidentity.info.energy", "Energy: {0}", Component.text(stats.getEnergy(), NamedTextColor.BLUE)).color(NamedTextColor.GRAY));
        player.sendMessage(Component.translatable("mcidentity.info.strength", "Strength: {0}", Component.text(stats.getStrength(), NamedTextColor.DARK_RED)).color(NamedTextColor.GRAY));
        player.sendMessage(Component.translatable("mcidentity.info.defense", "Defense: {0}", Component.text(stats.getDefense(), NamedTextColor.GRAY)).color(NamedTextColor.GRAY));
        player.sendMessage(Component.translatable("mcidentity.info.wisdom", "Wisdom: {0}", Component.text(stats.getWisdom(), NamedTextColor.LIGHT_PURPLE)).color(NamedTextColor.GRAY));
        player.sendMessage(Component.translatable("mcidentity.info.luck", "Luck: {0}", Component.text(stats.getLuck(), NamedTextColor.GOLD)).color(NamedTextColor.GRAY));
        player.sendMessage(Component.translatable("mcidentity.info.agility", "Agility: {0}", Component.text(stats.getAgility(), NamedTextColor.YELLOW)).color(NamedTextColor.GRAY));
        player.sendMessage(Component.translatable("mcidentity.info.resilience", "Resilience: {0}", Component.text(stats.getResilience(), NamedTextColor.DARK_GREEN)).color(NamedTextColor.GRAY));
    }
}
