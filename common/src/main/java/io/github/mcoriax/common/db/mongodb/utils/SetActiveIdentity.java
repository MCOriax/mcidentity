package io.github.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.util.Date;
import java.util.UUID;

public class SetActiveIdentity {
    public static void invoke(MongoDatabase database, UUID playerUuid, UUID identityUuid, String contentKey) {
        MongoCollection<Document> sessions = database.getCollection("identity_sessions");
        if (identityUuid == null) {
            sessions.deleteOne(Filters.eq("_id", playerUuid.toString()));
        } else {
            Document doc = new Document("_id", playerUuid.toString())
                    .append("identity_uuid", identityUuid.toString())
                    .append("content_key", contentKey)
                    .append("equipped_at", new Date());
            sessions.updateOne(Filters.eq("_id", playerUuid.toString()), new Document("$set", doc), new UpdateOptions().upsert(true));
        }
    }
}
