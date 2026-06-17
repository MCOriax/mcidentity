package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

public class CreateProfession {
    public static void invoke(MongoDatabase database, String systemCode, String displayName) {
        MongoCollection<Document> collection = database.getCollection("identity_professions");
        Document doc = new Document("system_code", systemCode).append("display_name", displayName);
        collection.updateOne(Filters.eq("system_code", systemCode), new Document("$set", doc), new UpdateOptions().upsert(true));
    }
}
