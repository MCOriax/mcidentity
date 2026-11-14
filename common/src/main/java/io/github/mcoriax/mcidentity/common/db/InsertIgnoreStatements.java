package io.github.mcoriax.mcidentity.common.db;

/**
 * The insert-if-absent statements of the three SQL dialects.
 *
 * <p><strong>Internal.</strong> Package-private, used by {@link AbstractSqlDB}.</p>
 *
 * <p>Every statement is a compile-time constant: the shared column lists are
 * constants too, so no SQL text is ever assembled at runtime, and each dialect
 * names only the keyword that differs.</p>
 */
enum InsertIgnoreStatements {

    /** SQLite: {@code INSERT OR IGNORE}. */
    SQLITE("INSERT OR IGNORE INTO " + Columns.PROFESSION, "INSERT OR IGNORE INTO " + Columns.PLAYER),

    /** MySQL: {@code INSERT IGNORE}. */
    MYSQL("INSERT IGNORE INTO " + Columns.PROFESSION, "INSERT IGNORE INTO " + Columns.PLAYER),

    /** PostgreSQL: {@code ON CONFLICT DO NOTHING}. */
    POSTGRESQL("INSERT INTO " + Columns.PROFESSION + Columns.ON_CONFLICT,
        "INSERT INTO " + Columns.PLAYER + Columns.ON_CONFLICT);

    /** Inserts one profession with (system_code, display_name) parameters. */
    private final String profession;

    /**
     * Inserts a player's stats with (player_uuid, stat_points, energy, health,
     * food, strength, defense, wisdom, luck, agility, resilience) parameters.
     */
    private final String player;

    InsertIgnoreStatements(String profession, String player) {
        this.profession = profession;
        this.player = player;
    }

    /**
     * @return The profession insert-if-absent.
     */
    String profession() {
        return profession;
    }

    /**
     * @return The player stats insert-if-absent.
     */
    String player() {
        return player;
    }

    /**
     * The table and value lists every dialect shares.
     */
    private static final class Columns {
        static final String PROFESSION = "identity_professions (system_code, display_name) VALUES (?, ?)";
        static final String PLAYER = "player_stats (player_uuid, stat_points, energy, health, food, strength, "
            + "defense, wisdom, luck, agility, resilience) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        static final String ON_CONFLICT = " ON CONFLICT DO NOTHING";

        private Columns() {
        }
    }
}
