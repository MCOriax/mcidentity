package io.github.mcoriax.mcidentity.common.db;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.COLLECTION_PROFESSIONS;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.COLLECTION_PROGRESS;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_PLAYER_UUID;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_PROFESSION_CODE;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_SYSTEM_CODE;

/**
 * Prepares a MongoDB database for the {@link MongoDB} backend on start.
 *
 * <p><strong>Internal.</strong> Package-private. Creates the unique indexes the
 * backend relies on.</p>
 */
final class MongoSchema {

    private MongoSchema() {
    }

    /**
     * Creates the unique indexes.
     * @param database The database.
     */
    static void initialize(MongoDatabase database) {
        IndexOptions unique = new IndexOptions().unique(true);
        database.getCollection(COLLECTION_PROFESSIONS).createIndex(new Document(FIELD_SYSTEM_CODE, 1), unique);
        database.getCollection(COLLECTION_PROGRESS).createIndex(
            new Document(FIELD_PLAYER_UUID, 1).append(FIELD_PROFESSION_CODE, 1), unique);
    }
}
