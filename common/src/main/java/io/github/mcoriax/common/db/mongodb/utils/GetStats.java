package io.github.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.github.mcoriax.api.model.IdentityStats;
import org.bson.Document;

import java.util.Optional;
import java.util.UUID;

public class GetStats {
    public static Optional<IdentityStats> invoke(MongoDatabase database, UUID identityUuid) {
        Document doc = database.getCollection("identities").find(Filters.eq("_id", identityUuid.toString())).first();
        if (doc != null && doc.containsKey("stats")) {
            Document stats = doc.get("stats", Document.class);
            return Optional.of(new IdentityStats() {
                @Override public int getEnergy() { return stats.getInteger("energy", 0); }
                @Override public int getHealth() { return stats.getInteger("health", 0); }
                @Override public int getFood() { return stats.getInteger("food", 20); }
                @Override public int getStrength() { return stats.getInteger("strength", 0); }
                @Override public int getDefense() { return stats.getInteger("defense", 0); }
                @Override public int getWisdom() { return stats.getInteger("wisdom", 0); }
                @Override public int getLuck() { return stats.getInteger("luck", 0); }
                @Override public int getAgility() { return stats.getInteger("agility", 0); }
                @Override public int getResilience() { return stats.getInteger("resilience", 0); }
            });
        }
        return Optional.empty();
    }
}
