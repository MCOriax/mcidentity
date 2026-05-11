package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import java.util.UUID;

public class SaveSkin {
    public static void invoke(MongoDatabase database, UUID uuid, String skin) {
        database.getCollection("identities").updateOne(
                Filters.eq("_id", uuid.toString()),
                Updates.set("cosmetics.skin", skin)
        );
    }
}
