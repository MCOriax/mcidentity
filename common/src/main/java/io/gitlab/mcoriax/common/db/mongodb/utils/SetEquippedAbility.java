package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.UUID;

public class SetEquippedAbility {
    public static void invoke(MongoDatabase database, UUID identityUuid, int slot, String abilityCode) {
        MongoCollection<Document> identities = database.getCollection("identities");
        if (abilityCode == null) {
            identities.updateOne(Filters.eq("_id", identityUuid.toString()), Updates.unset("equipped_abilities." + slot));
        } else {
            identities.updateOne(Filters.eq("_id", identityUuid.toString()), Updates.set("equipped_abilities." + slot, abilityCode));
        }
    }
}
