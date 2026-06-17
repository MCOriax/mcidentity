package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import io.gitlab.mcoriax.api.model.Ability;
import io.gitlab.mcoriax.api.model.AbilityType;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GetAbilities {
    public static Collection<Ability> invoke(MongoDatabase database) {
        List<Ability> identity_abilities = new ArrayList<>();
        for (Document doc : database.getCollection("identity_abilities").find()) {
            identity_abilities.add(new Ability() {
                @Override public String getSystemCode() { return doc.getString("system_code"); }
                @Override public String getDisplayName() { return doc.getString("display_name"); }
                @Override public AbilityType getAbilityType() { return AbilityType.valueOf(doc.getString("ability_type")); }
                @Override public boolean isGlobal() { return doc.getBoolean("is_global", false); }
            });
        }
        return identity_abilities;
    }
}
