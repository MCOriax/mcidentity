package io.github.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import io.github.mcoriax.api.model.Profession;
import io.github.mcoriax.common.MCIdentityManager;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GetProfessions {
    public static Collection<Profession> invoke(MongoDatabase database) {
        List<Profession> identity_professions = new ArrayList<>();
        for (Document doc : database.getCollection("identity_professions").find()) {
            String code = doc.getString("system_code");
            identity_professions.add(new Profession() {
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
                @Override public Map<Integer, String> getSkins() {
                    return MCIdentityManager.getProfessionSkins(code);
                }
                @Override public void setSkins(Map<Integer, String> skins) {
                    MCIdentityManager.setProfessionSkins(code, skins);
                }
            });
        }
        return identity_professions;
    }
}
