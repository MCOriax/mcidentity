package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import io.gitlab.mcoriax.api.model.AbilityType;
import org.bson.Document;

public class CreateAbility {
    public static void invoke(MongoDatabase database, String systemCode, String displayName, AbilityType type, boolean isGlobal) {
        MongoCollection<Document> collection = database.getCollection("abilities");
        Document doc = new Document("system_code", systemCode)
                .append("display_name", displayName)
                .append("ability_type", type.name())
                .append("is_global", isGlobal);
        collection.updateOne(Filters.eq("system_code", systemCode), new Document("$set", doc), new UpdateOptions().upsert(true));
    }
}
