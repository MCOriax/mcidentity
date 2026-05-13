package io.gitlab.mcoriax.foliamc.commands.utils;

import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RenderIdentityInfo {
    /**
     * Render identity information to a player's chat.
     */
    public static void execute(Player player, UUID uuid, IdentityKey key, IdentityProperties props, IdentityStats stats) {
        player.sendMessage(Component.text("--- Identity Data ---", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>UUID: <white><value></white></gray>", Placeholder.unparsed("value", uuid.toString())));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Profession: <green><value></green></gray>", Placeholder.unparsed("value", key.getProfessionCode())));

        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Level: <yellow><value></yellow></gray>", Placeholder.unparsed("value", String.valueOf(props.getLevel()))));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Exp: <yellow><value></yellow></gray>", Placeholder.unparsed("value", String.valueOf(props.getExp()))));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Stat Points: <yellow><value></yellow></gray>", Placeholder.unparsed("value", String.valueOf(props.getStatPoints()))));

        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Health: <red><value></red></gray>", Placeholder.unparsed("value", String.valueOf(stats.getHealth()))));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Food: <gold><value></gold></gray>", Placeholder.unparsed("value", String.valueOf(stats.getFood()))));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Energy: <blue><value></blue></gray>", Placeholder.unparsed("value", String.valueOf(stats.getEnergy()))));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Strength: <dark_red><value></dark_red></gray>", Placeholder.unparsed("value", String.valueOf(stats.getStrength()))));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Defense: <gray><value></gray></gray>", Placeholder.unparsed("value", String.valueOf(stats.getDefense()))));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Wisdom: <light_purple><value></light_purple></gray>", Placeholder.unparsed("value", String.valueOf(stats.getWisdom()))));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Luck: <gold><value></gold></gray>", Placeholder.unparsed("value", String.valueOf(stats.getLuck()))));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Agility: <yellow><value></yellow></gray>", Placeholder.unparsed("value", String.valueOf(stats.getAgility()))));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Resilience: <dark_green><value></dark_green></gray>", Placeholder.unparsed("value", String.valueOf(stats.getResilience()))));
    }
}
