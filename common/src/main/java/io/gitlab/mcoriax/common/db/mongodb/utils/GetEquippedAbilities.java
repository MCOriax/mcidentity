package io.gitlab.mcoriax.common.db.mongodb.utils;

import io.gitlab.mcoriax.api.model.EquippedAbility;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GetEquippedAbilities {
    public static Map<Integer, EquippedAbility> invoke(MongoDatabase database, UUID identityUuid) {
        Map<Integer, EquippedAbility> map = new HashMap<>();
        Document doc = database.getCollection("identities").find(Filters.eq("_id", identityUuid.toString())).first();
        if (doc != null && doc.containsKey("equipped_abilities")) {
            Document equipped = doc.get("equipped_abilities", Document.class);
            for (String key : equipped.keySet()) {
                Object val = equipped.get(key);
                if (val instanceof Document abilityDoc) {
                    map.put(Integer.parseInt(key), new EquippedAbility(
                        abilityDoc.getString("system_code"),
                        abilityDoc.getString("model_id")
                    ));
                } else if (val instanceof String systemCode) {
                    map.put(Integer.parseInt(key), new EquippedAbility(systemCode, null));
                }
            }
        }
        return map;
    }
}
