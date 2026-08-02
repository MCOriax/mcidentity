package io.github.mcoriax.mcidentity.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the two facades against being bypassed.
 *
 * <p>{@link MCIdentityManager} (RAM state) and {@link MCIdentityProvider}
 * (persistence) are the only entry points the platform modules may use. Java
 * cannot express "public within this Gradle project", so the boundary is
 * enforced here instead: this test reads the {@code bukkit} and
 * {@code platforms:*} sources and fails the build when one of them names a
 * storage internal.</p>
 *
 * <p>When this test fails, the fix is to add what the caller needs to
 * {@link MCIdentityProvider} rather than to relax the rule.</p>
 */
class ArchitectureBoundaryTest {

    /**
     * Source roots that must go through the facades, relative to this module.
     */
    private static final String[] PLATFORM_SOURCE_ROOTS = {
        "../bukkit/src/main/java",
        "../platforms/spigotmc/src/main/java",
        "../platforms/papermc/src/main/java",
        "../platforms/foliamc/src/main/java",
        "../platforms/engine/src/main/java"
    };

    /**
     * Identifiers no platform source may mention: the persistence contract, the
     * concrete backends, and the packages they live in.
     */
    private static final Pattern FORBIDDEN = Pattern.compile(
        "\\b(IMCIdentityDB|AbstractSqlDB|SQLiteDB|MySQLDB|PostgreSQLDB|MongoDB|MCIdentityCentralServer|StatKeys)\\b"
            + "|io\\.github\\.mcoriax\\.mcidentity\\.common\\.(db|central)\\b");

    @Test
    @DisplayName("Platform modules reach storage only through the provider facade")
    void platformModulesDoNotTouchStorageInternals() throws IOException {
        List<String> violations = new ArrayList<>();

        for (String root : PLATFORM_SOURCE_ROOTS) {
            Path sourceRoot = Path.of(root);
            if (!Files.isDirectory(sourceRoot)) {
                // The module layout changed; that is a separate problem from
                // this rule, so skip rather than fail on a missing directory.
                continue;
            }
            try (Stream<Path> files = Files.walk(sourceRoot)) {
                files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                    List<String> lines;
                    try {
                        lines = Files.readAllLines(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    for (int index = 0; index < lines.size(); index++) {
                        String line = lines.get(index);
                        // A mention inside a comment is documentation, not a
                        // dependency, so only real code counts.
                        String code = stripComment(line);
                        if (FORBIDDEN.matcher(code).find()) {
                            violations.add(path + ":" + (index + 1) + "  " + line.trim());
                        }
                    }
                });
            }
        }

        assertTrue(violations.isEmpty(),
            "Platform code must go through MCIdentityProvider instead of naming storage internals:\n"
                + String.join("\n", violations));
    }

    /**
     * Removes the trailing line comment and whole-line block comment bodies, so
     * prose mentioning a backend by name does not count as a dependency.
     * @param line The source line.
     * @return The code portion of the line.
     */
    private static String stripComment(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
            return "";
        }
        int comment = line.indexOf("//");
        return comment >= 0 ? line.substring(0, comment) : line;
    }
}
