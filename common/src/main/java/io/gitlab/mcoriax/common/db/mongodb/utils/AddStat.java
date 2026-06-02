package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.UUID;

public class AddStat {
    public static void invoke(MongoDatabase database, UUID identityUuid, String statKey, int amount) {
        if (!isValidStatKey(statKey)) return;

        database.getCollection("identities").updateOne(
                Filters.eq("_id", identityUuid.toString()),
                Updates.inc("stats." + statKey, amount)
        );
    }

    private static boolean isValidStatKey(String key) {
        return key != null && (key.equals("energy") || key.equals("health") || key.equals("food") || key.equals("strength") ||
                key.equals("defense") || key.equals("wisdom") || key.equals("luck") ||
                key.equals("agility") || key.equals("resilience"));
    }
}
