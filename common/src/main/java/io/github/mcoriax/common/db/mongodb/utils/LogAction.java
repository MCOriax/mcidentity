package io.github.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import io.github.mcoriax.api.model.ActionType;
import org.bson.Document;

import java.util.Date;
import java.util.UUID;

public class LogAction {
    public static void invoke(MongoDatabase database, UUID playerUuid, UUID identityUuid, ActionType actionType) {
        Document log = new Document("player_uuid", playerUuid.toString())
                .append("identity_uuid", identityUuid.toString())
                .append("action_type", actionType.name())
                .append("action_time", new Date());
        database.getCollection("logs").insertOne(log);
    }
}
