/**
 * Data access for the Identity system. Hosts the shared stat key allow-list
 * and the selectable storage backends: {@code SQLiteDB} (the default),
 * {@code MySQLDB}, {@code PostgreSQLDB}, and {@code MongoDB}, plus the
 * {@code central} package for the MCIdentity central REST server. The
 * relational backends share their query logic through {@code AbstractSqlDB}.
 */
package io.github.mcoriax.common.db;
