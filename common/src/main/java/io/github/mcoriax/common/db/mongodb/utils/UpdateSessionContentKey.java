package io.github.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.UUID;

public class UpdateSessionContentKey {
    public static void invoke(MongoDatabase db, UUID playerUuid, String contentKey) {
        MongoCollection<Document> collection = db.getCollection("identity_sessions");
        collection.updateOne(Filters.eq("player_uuid", playerUuid.toString()), Updates.set("content_key", contentKey));
    }
}
