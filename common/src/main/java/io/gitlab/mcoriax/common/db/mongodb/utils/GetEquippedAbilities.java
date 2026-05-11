package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GetEquippedAbilities {
    public static Map<Integer, String> invoke(MongoDatabase database, UUID identityUuid) {
        Map<Integer, String> map = new HashMap<>();
        Document doc = database.getCollection("identities").find(Filters.eq("_id", identityUuid.toString())).first();
        if (doc != null && doc.containsKey("equipped_abilities")) {
            Document equipped = doc.get("equipped_abilities", Document.class);
            for (String key : equipped.keySet()) {
                map.put(Integer.parseInt(key), equipped.getString(key));
            }
        }
        return map;
    }
}
