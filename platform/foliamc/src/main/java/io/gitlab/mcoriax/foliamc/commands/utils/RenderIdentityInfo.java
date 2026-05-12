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
        player.sendMessage(Component.text("--- Identity Data ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("UUID: ", NamedTextColor.GRAY).append(Component.text(uuid.toString(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Profession: ", NamedTextColor.GRAY).append(Component.text(key.getProfessionCode(), NamedTextColor.GREEN)));

        player.sendMessage(Component.text("Level: ", NamedTextColor.GRAY).append(Component.text(props.getLevel(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Exp: ", NamedTextColor.GRAY).append(Component.text(props.getExp(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Stat Points: ", NamedTextColor.GRAY).append(Component.text(props.getStatPoints(), NamedTextColor.YELLOW)));

        player.sendMessage(Component.text("Health: ", NamedTextColor.GRAY).append(Component.text(stats.getHealth(), NamedTextColor.RED)));
        player.sendMessage(Component.text("Food: ", NamedTextColor.GRAY).append(Component.text(stats.getFood(), NamedTextColor.GOLD)));
        player.sendMessage(Component.text("Energy: ", NamedTextColor.GRAY).append(Component.text(stats.getEnergy(), NamedTextColor.BLUE)));
        player.sendMessage(Component.text("Strength: ", NamedTextColor.GRAY).append(Component.text(stats.getStrength(), NamedTextColor.DARK_RED)));
        player.sendMessage(Component.text("Defense: ", NamedTextColor.GRAY).append(Component.text(stats.getDefense(), NamedTextColor.GRAY)));
        player.sendMessage(Component.text("Wisdom: ", NamedTextColor.GRAY).append(Component.text(stats.getWisdom(), NamedTextColor.LIGHT_PURPLE)));
        player.sendMessage(Component.text("Luck: ", NamedTextColor.GRAY).append(Component.text(stats.getLuck(), NamedTextColor.GOLD)));
        player.sendMessage(Component.text("Agility: ", NamedTextColor.GRAY).append(Component.text(stats.getAgility(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Resilience: ", NamedTextColor.GRAY).append(Component.text(stats.getResilience(), NamedTextColor.DARK_GREEN)));
    }
}
