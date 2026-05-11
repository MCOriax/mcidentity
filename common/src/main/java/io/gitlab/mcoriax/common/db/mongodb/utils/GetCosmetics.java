package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GetCosmetics {
    public static Map<String, String> invoke(MongoDatabase database, UUID uuid) {
        Map<String, String> map = new HashMap<>();
        Document doc = database.getCollection("identities").find(Filters.eq("_id", uuid.toString())).first();
        if (doc != null && doc.containsKey("cosmetics")) {
            Document cosmetics = doc.get("cosmetics", Document.class);
            map.put("skin", cosmetics.getString("skin"));
            map.put("aura", cosmetics.getString("aura"));
        }
        return map;
    }
}
