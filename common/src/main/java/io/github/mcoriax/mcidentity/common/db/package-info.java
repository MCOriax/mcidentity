/**
 * Data access for MCIdentity. Hosts the shared stat key allow-list and the
 * selectable storage backends: {@code SQLiteDB} (the default),
 * {@code MySQLDB}, {@code PostgreSQLDB}, and {@code MongoDB}, plus the
 * {@code central} package for the MCIdentity central REST server. The
 * relational backends share their query logic through {@code AbstractSqlDB}.
 * Every row a player owns is keyed by the player UUID.
 */
package io.github.mcoriax.mcidentity.common.db;
