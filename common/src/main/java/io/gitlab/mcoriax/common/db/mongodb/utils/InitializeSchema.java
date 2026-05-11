package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

public class InitializeSchema {
    public static void invoke(MongoDatabase database) {
        IndexOptions uniqueIndex = new IndexOptions().unique(true);
        database.getCollection("professions").createIndex(new Document("system_code", 1), uniqueIndex);
        database.getCollection("abilities").createIndex(new Document("system_code", 1), uniqueIndex);
        database.getCollection("runes").createIndex(new Document("system_code", 1), uniqueIndex);
    }
}
