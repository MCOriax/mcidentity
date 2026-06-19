package io.github.mcoriax.bukkit.util;

import org.bukkit.Bukkit;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects the running Minecraft server version and exposes it as comparable
 * major/minor/patch integers plus a handful of high-level capability checks.
 *
 * <p>The shared {@code bukkit} module compiles against a modern spigot-api, so any
 * code path that touches API added after 1.8 (for example
 * {@code PlayerInteractEvent#getHand()}, introduced in 1.9) must be gated at
 * runtime to avoid {@code NoSuchMethodError}/{@code NoClassDefFoundError} on legacy
 * servers. This class is that gate, without hard-coding a single server version.</p>
 *
 * <p>Parsing is resilient across both the historical {@code 1.x.y} scheme and the
 * newer {@code <major>.<minor>.<patch>} scheme (e.g. {@code 26.1.2}), and copes with
 * major versions well beyond the current release. Detection runs once and is cached
 * in static fields.</p>
 */
public final class ServerVersion {

    /** Matches the leading numeric components of e.g. {@code 1.20.4-R0.1-SNAPSHOT} or {@code 26.1.2-R0.1-SNAPSHOT}. */
    private static final Pattern BUKKIT_VERSION = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    /** Matches the legacy CraftBukkit relocation package, e.g. {@code v1_20_R3}. */
    private static final Pattern NMS_PACKAGE = Pattern.compile("v(\\d+)_(\\d+)_R(\\d+)");

    private static final int MAJOR;
    private static final int MINOR;
    private static final int PATCH;
    private static final String RAW;

    static {
        int[] parsed = detect();
        MAJOR = parsed[0];
        MINOR = parsed[1];
        PATCH = parsed[2];
        RAW = safeRaw();
    }

    private ServerVersion() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static String safeRaw() {
        try {
            String version = Bukkit.getBukkitVersion();
            return version != null ? version : "unknown";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static int[] detect() {
        // Primary source: Bukkit.getBukkitVersion() -> "1.20.4-R0.1-SNAPSHOT" / "26.1.2-R0.1-SNAPSHOT".
        try {
            String version = Bukkit.getBukkitVersion();
            if (version != null) {
                Matcher matcher = BUKKIT_VERSION.matcher(version);
                if (matcher.find()) {
                    return new int[] {
                            parse(matcher.group(1)),
                            parse(matcher.group(2)),
                            parse(matcher.group(3))
                    };
                }
            }
        } catch (Throwable ignored) {
            // Fall through to the NMS package heuristic below.
        }

        // Fallback: legacy CraftBukkit package name -> "...craftbukkit.v1_20_R3".
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            Matcher matcher = NMS_PACKAGE.matcher(pkg);
            if (matcher.find()) {
                return new int[] { parse(matcher.group(1)), parse(matcher.group(2)), 0 };
            }
        } catch (Throwable ignored) {
            // Detection failed entirely; fall back to a modern assumption below.
        }

        // Unknown environment (e.g. a unit test with no live server): assume a modern,
        // offhand-capable server so capability checks do not wrongly disable features.
        Logger.getLogger("MCIdentity").warning(
                "Could not detect the server version; assuming a modern (offhand-capable) server.");
        return new int[] { 1, 21, 0 };
    }

    private static int parse(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** @return the major component (1 for the {@code 1.x} scheme, otherwise the leading number). */
    public static int getMajor() {
        return MAJOR;
    }

    /** @return the minor component (the familiar "Minecraft version" on the {@code 1.x} scheme). */
    public static int getMinor() {
        return MINOR;
    }

    /** @return the patch component, or {@code 0} when the version string omits it. */
    public static int getPatch() {
        return PATCH;
    }

    /** @return the raw {@code Bukkit.getBukkitVersion()} string, or {@code "unknown"}. */
    public static String getRawVersion() {
        return RAW;
    }

    /** @return a compact {@code major.minor.patch} string. */
    public static String getShortVersion() {
        return MAJOR + "." + MINOR + "." + PATCH;
    }

    /**
     * @param reqMajor required major version
     * @param reqMinor required minor version
     * @return {@code true} if the server is at least {@code reqMajor.reqMinor}
     */
    public static boolean isAtLeast(int reqMajor, int reqMinor) {
        return isAtLeast(reqMajor, reqMinor, 0);
    }

    /**
     * @param reqMajor required major version
     * @param reqMinor required minor version
     * @param reqPatch required patch version
     * @return {@code true} if the server is at least {@code reqMajor.reqMinor.reqPatch}
     */
    public static boolean isAtLeast(int reqMajor, int reqMinor, int reqPatch) {
        if (MAJOR != reqMajor) {
            return MAJOR > reqMajor;
        }
        if (MINOR != reqMinor) {
            return MINOR > reqMinor;
        }
        return PATCH >= reqPatch;
    }

    /**
     * @return {@code true} on pre-1.13 "legacy" servers, before the 1.13 flattening
     *         changed Material/Sound/Particle naming
     */
    public static boolean isLegacy() {
        return !isAtLeast(1, 13);
    }

    /**
     * @return {@code true} when the server supports the off-hand slot and the dual-wield
     *         API ({@code EquipmentSlot.OFF_HAND}, {@code event.getHand()}), added in 1.9
     */
    public static boolean hasOffhand() {
        return isAtLeast(1, 9);
    }
}
