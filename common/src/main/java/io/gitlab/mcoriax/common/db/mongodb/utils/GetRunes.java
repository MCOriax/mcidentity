package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.gitlab.mcoriax.api.model.Rune;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GetRunes {
    public static Collection<Rune> invoke(MongoDatabase database) {
        List<Rune> identity_runes = new ArrayList<>();
        for (Document doc : database.getCollection("identity_runes").find()) {
            identity_runes.add(new Rune() {
                @Override public String getSystemCode() { return doc.getString("system_code"); }
                @Override public String getDisplayName() { return doc.getString("display_name"); }
            });
        }
        return identity_runes;
    }

    public static Map<Integer, String> invoke(MongoDatabase database, UUID identityUuid) {
        Map<Integer, String> map = new HashMap<>();
        Document doc = database.getCollection("identities").find(Filters.eq("_id", identityUuid.toString())).first();
        if (doc != null && doc.containsKey("equipped_runes")) {
            Document equipped = doc.get("equipped_runes", Document.class);
            for (String key : equipped.keySet()) {
                map.put(Integer.parseInt(key), equipped.getString(key));
            }
        }
        return map;
    }
}
