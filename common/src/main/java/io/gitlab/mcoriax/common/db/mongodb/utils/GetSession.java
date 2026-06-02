package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.gitlab.mcoriax.api.model.IdentitySession;
import org.bson.Document;

import java.util.Optional;
import java.util.UUID;

public class GetSession {
    public static Optional<IdentitySession> invoke(MongoDatabase database, UUID playerUuid) {
        Document session = database.getCollection("identity_sessions").find(Filters.eq("_id", playerUuid.toString())).first();
        if (session != null && session.containsKey("identity_uuid")) {
            return Optional.of(new IdentitySession() {
                @Override public UUID getPlayerUniqueId() { return playerUuid; }
                @Override public UUID getIdentityUniqueId() { return UUID.fromString(session.getString("identity_uuid")); }
                @Override public String getContentKey() { return session.getString("content_key"); }
            });
        }
        return Optional.empty();
    }
}
