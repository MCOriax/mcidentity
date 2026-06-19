package io.github.mcoriax.bukkit.util;

import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies and copies custom head textures across the whole supported server range
 * (1.8 -&gt; latest).
 *
 * <p>The way a base64 skin is attached to a skull changed repeatedly: pre-1.18
 * servers require reflecting a {@code com.mojang.authlib.GameProfile} onto the
 * skull meta, while 1.18.1+ exposes the platform-agnostic
 * {@code org.bukkit.profile.PlayerProfile} API, and the internal field/profile
 * wrapper shifted again around 1.20. To stay correct everywhere this class uses
 * the modern Bukkit {@link PlayerProfile} API when the server supports it, and
 * otherwise delegates to XSeries' {@link XSkull}, whose reflection layer safely
 * covers 1.8 -&gt; 1.17 (and newer).</p>
 *
 * <p>All modern-API calls are isolated in the nested {@link ModernProfile} type so
 * that a legacy (1.8) JVM never has to load {@code org.bukkit.profile} symbols when
 * verifying this class.</p>
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
        if (meta == null || base64Texture == null || base64Texture.isBlank()) {
            return;
        }
        // Modern path: native Bukkit PlayerProfile API (1.18.1+).
        if (ServerVersion.isAtLeast(1, 18, 1)) {
            try {
                ModernProfile.apply(meta, base64Texture);
                return;
            } catch (Throwable ignored) {
                // Any incompatibility falls through to the XSeries path below.
            }
        }
        // Legacy/fallback path: XSeries reflects the profile in (covers 1.8 -> 1.17).
        applyViaXSkull(meta, base64Texture);
    }

    /**
     * Copies the owner profile (and thus texture) from one skull meta to another.
     *
     * @param source the meta to read the profile from
     * @param target the meta to apply the profile to
     */
    public static void copyProfile(SkullMeta source, SkullMeta target) {
        if (source == null || target == null) {
            return;
        }
        if (ServerVersion.isAtLeast(1, 18, 1)) {
            try {
                if (ModernProfile.copy(source, target)) {
                    return;
                }
            } catch (Throwable ignored) {
                // Fall through to the XSeries copy below.
            }
        }
        try {
            XSkull.of(target).profile(Profileable.of(source)).apply();
        } catch (Throwable ignored) {
            // Leave the target untextured rather than break item creation.
        }
    }

    private static void applyViaXSkull(SkullMeta meta, String base64Texture) {
        try {
            XSkull.of(meta).profile(Profileable.detect(base64Texture)).apply();
        } catch (Throwable ignored) {
            // Leave the skull untextured rather than break item creation.
        }
    }

    /**
     * Modern (1.18.1+) Bukkit {@code PlayerProfile} implementation, kept in a separate
     * class so legacy servers never resolve {@code org.bukkit.profile} types.
     */
    private static final class ModernProfile {

        private ModernProfile() {
        }

        static void apply(SkullMeta meta, String base64Texture) {
            URL skinUrl = toSkinUrl(base64Texture);
            if (skinUrl == null) {
                return;
            }
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            profile.getTextures().setSkin(skinUrl);
            meta.setOwnerProfile(profile);
        }

        static boolean copy(SkullMeta source, SkullMeta target) {
            PlayerProfile profile = source.getOwnerProfile();
            if (profile != null) {
                target.setOwnerProfile(profile);
                return true;
            }
            return false;
        }

        private static URL toSkinUrl(String base64Texture) {
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
}
