package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.gitlab.mcoriax.api.model.IdentityKey;
import org.bson.Document;

import java.util.Optional;
import java.util.UUID;

public class GetIdentityKey {
    public static Optional<IdentityKey> invoke(MongoDatabase database, UUID uuid) {
        Document doc = database.getCollection("identities").find(Filters.eq("_id", uuid.toString())).first();
        if (doc != null) {
            return Optional.of(new IdentityKey() {
                @Override public UUID getUniqueId() { return uuid; }
                @Override public String getProfessionCode() { return doc.getString("profession_code"); }
            });
        }
        return Optional.empty();
    }
}
