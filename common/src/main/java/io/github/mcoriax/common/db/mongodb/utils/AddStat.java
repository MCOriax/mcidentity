package io.github.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.github.mcoriax.common.db.StatKeys;

import java.util.UUID;

public class AddStat {
    public static void invoke(MongoDatabase database, UUID identityUuid, String statKey, int amount) {
        if (!StatKeys.isValid(statKey)) return;

        database.getCollection("identities").updateOne(
                Filters.eq("_id", identityUuid.toString()),
                Updates.inc("stats." + statKey, amount)
        );
    }
}
