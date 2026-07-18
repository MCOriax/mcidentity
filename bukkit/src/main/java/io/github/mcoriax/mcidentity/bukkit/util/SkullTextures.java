package io.github.mcoriax.mcidentity.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies and reads custom head textures using the platform-agnostic
 * {@code org.bukkit.profile} API (available in spigot-api), instead of Paper's
 * {@code com.destroystokyo.paper.profile} types.
 * <p>
 * Textures are supplied as the base64-encoded profile-property value that wraps a
 * JSON document of the form {@code {"textures":{"SKIN":{"url":"..."}}}}; the skin
 * URL is extracted and applied via {@link PlayerTextures#setSkin(URL)}.
 * </p>
 */
public final class SkullTextures {

    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private SkullTextures() {
    }

    /**
     * Applies a base64-encoded skin texture to the given skull meta.
     *
     * @param meta          the skull meta to modify
     * @param base64Texture the base64 profile-property texture value
     */
    public static void apply(SkullMeta meta, String base64Texture) {
        URL skinUrl = toSkinUrl(base64Texture);
        if (skinUrl == null) {
            return;
        }
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        profile.getTextures().setSkin(skinUrl);
        meta.setOwnerProfile(profile);
    }

    /**
     * Copies the owner profile (and thus texture) from one skull meta to another.
     *
     * @param source the meta to read the profile from
     * @param target the meta to apply the profile to
     */
    public static void copyProfile(SkullMeta source, SkullMeta target) {
        PlayerProfile profile = source.getOwnerProfile();
        if (profile != null) {
            target.setOwnerProfile(profile);
        }
    }

    /**
     * Decodes a base64 profile-property texture value into its skin URL.
     *
     * @param base64Texture the base64 texture value
     * @return the skin URL, or {@code null} if it could not be parsed
     */
    private static URL toSkinUrl(String base64Texture) {
        if (base64Texture == null || base64Texture.isBlank()) {
            return null;
        }
        try {
            String json = new String(Base64.getDecoder().decode(base64Texture), StandardCharsets.UTF_8);
            Matcher matcher = URL_PATTERN.matcher(json);
            if (!matcher.find()) {
                return null;
            }
            return URI.create(matcher.group(1)).toURL();
        } catch (Exception e) {
            return null;
        }
    }
}
