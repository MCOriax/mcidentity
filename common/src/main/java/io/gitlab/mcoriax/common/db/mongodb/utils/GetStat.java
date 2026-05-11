package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.UUID;

public class GetStat {
    public static int invoke(MongoDatabase database, UUID identityUuid, String statKey) {
        Document doc = database.getCollection("identities").find(Filters.eq("_id", identityUuid.toString())).first();
        if (doc != null && doc.containsKey("stats")) {
            Document stats = doc.get("stats", Document.class);
            return stats.getInteger(statKey, 0);
        }
        return 0;
    }
}
