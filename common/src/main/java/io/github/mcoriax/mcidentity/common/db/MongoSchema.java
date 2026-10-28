package io.github.mcoriax.mcidentity.common.db;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.ALIAS_COMPARABLE_FIELD;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.COLLECTION_IDENTITIES;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.COLLECTION_PROFESSIONS;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_SYSTEM_CODE;

/**
 * Prepares a MongoDB database for the {@link MongoDB} backend on start.
 *
 * <p><strong>Internal.</strong> Package-private. Removes the data this plugin
 * no longer owns, then creates the unique indexes the backend relies on.</p>
 */
final class MongoSchema {

    private MongoSchema() {
    }

    /**
     * Drops retired data and creates the indexes. Safe to run on every start.
     * @param database The database holding every MCIdentity collection.
     */
    static void initialize(MongoDatabase database) {
        dropRetiredAddonData(database);

        IndexOptions uniqueIndex = new IndexOptions().unique(true);
        database.getCollection(COLLECTION_PROFESSIONS).createIndex(new Document(FIELD_SYSTEM_CODE, 1), uniqueIndex);

        // Aliases are unique network-wide and compared without case, so the
        // index covers the folded mirror field rather than the stored
        // value. It is sparse because most identities have no alias, and
        // a unique index would otherwise reject every one of them past the
        // first.
        database.getCollection(COLLECTION_IDENTITIES).createIndex(
            new Document(ALIAS_COMPARABLE_FIELD, 1),
            new IndexOptions().unique(true).sparse(true));
    }

    /**
     * Removes the ability data this plugin no longer owns.
     *
     * <p>Three systems have moved out of the core plugin into standalone
     * add-ons: skills to MCIdentitySkill, and runes and cosmetics to
     * MCIdentityRune and MCIdentityCosmetic. Their collections and per-identity
     * fields are therefore removed on the first start after upgrading, and are
     * <strong>not</strong> recoverable afterwards.</p>
     * @param database The database holding every MCIdentity collection.
     */
    private static void dropRetiredAddonData(MongoDatabase database) {
        database.getCollection("identity_abilities").drop();
        database.getCollection("identity_runes").drop();
        for (String field : new String[] {"equipped_abilities", "equipped_runes", "cosmetics"}) {
            database.getCollection(COLLECTION_IDENTITIES).updateMany(
                Filters.exists(field), Updates.unset(field));
        }
    }
}
