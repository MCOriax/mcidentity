package io.github.mcoriax.mcidentity.bukkit.commands.utils;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Sends a player the MCIdentity wiki links as clickable chat lines, one URL per
 * line. Clicking a line opens the URL in the player's browser; hovering shows
 * the destination.
 */
public class SendWiki {

    /**
     * The wiki links, sent one per line.
     */
    private static final String[] WIKI_LINKS = {
        "https://mcoriax.github.io",
        "https://mcoriax.gitlab.io"
    };

    /**
     * Sends the clickable wiki links to a player.
     * @param player The player to send the links to.
     */
    public static void execute(Player player) {
        player.sendMessage("" + ChatColor.GOLD + ChatColor.BOLD + "--- MCIdentity Wiki ---");
        for (String url : WIKI_LINKS) {
            TextComponent line = new TextComponent(url);
            line.setColor(net.md_5.bungee.api.ChatColor.AQUA);
            line.setUnderlined(true);
            line.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
            line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to open " + url)));
            player.spigot().sendMessage(line);
        }
    }
}
