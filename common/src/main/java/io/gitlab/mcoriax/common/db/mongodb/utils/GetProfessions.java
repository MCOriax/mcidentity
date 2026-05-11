package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import io.gitlab.mcoriax.api.model.Profession;
import io.gitlab.mcoriax.common.MCIdentityManager;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GetProfessions {
    public static Collection<Profession> invoke(MongoDatabase database) {
        List<Profession> professions = new ArrayList<>();
        for (Document doc : database.getCollection("professions").find()) {
            String code = doc.getString("system_code");
            professions.add(new Profession() {
                @Override public String getSystemCode() { return code; }
                @Override public String getDisplayName() { return doc.getString("display_name"); }
                @Override public Map<String, Object> getExpConfig() {
                    return MCIdentityManager.getProfessionExpConfig(code);
                }
                @Override public Map<String, Integer> getBaseStats() {
                    return MCIdentityManager.getProfessionBaseStats(code);
                }
                @Override public String getStatsRandom() {
                    return MCIdentityManager.getProfessionStatsRandom(code);
                }
            });
        }
        return professions;
    }
}
