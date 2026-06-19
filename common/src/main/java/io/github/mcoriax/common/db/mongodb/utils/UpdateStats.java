package io.github.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.github.mcoriax.api.model.IdentityStats;
import org.bson.Document;

import java.util.UUID;

public class UpdateStats {
    public static void invoke(MongoDatabase database, UUID identityUuid, IdentityStats stats) {
        Document statsDoc = new Document("energy", stats.getEnergy())
                .append("health", stats.getHealth())
                .append("food", stats.getFood())
                .append("strength", stats.getStrength())
                .append("defense", stats.getDefense())
                .append("wisdom", stats.getWisdom())
                .append("luck", stats.getLuck())
                .append("agility", stats.getAgility())
                .append("resilience", stats.getResilience());

        database.getCollection("identities").updateOne(
                Filters.eq("_id", identityUuid.toString()),
                Updates.set("stats", statsDoc)
        );
    }
}
