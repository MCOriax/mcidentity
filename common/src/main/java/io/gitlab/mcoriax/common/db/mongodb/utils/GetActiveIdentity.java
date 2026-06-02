package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.Optional;
import java.util.UUID;

public class GetActiveIdentity {
    public static Optional<UUID> invoke(MongoDatabase database, UUID playerUuid) {
        Document session = database.getCollection("identity_sessions").find(Filters.eq("_id", playerUuid.toString())).first();
        if (session != null && session.containsKey("identity_uuid")) {
            return Optional.of(UUID.fromString(session.getString("identity_uuid")));
        }
        return Optional.empty();
    }
}
