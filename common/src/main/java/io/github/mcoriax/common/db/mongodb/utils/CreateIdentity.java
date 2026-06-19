package io.github.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.mcoriax.api.model.IdentityKey;
import io.github.mcoriax.api.model.IdentityProperties;
import io.github.mcoriax.api.model.IdentityStats;
import org.bson.Document;

public class CreateIdentity {
    public static void invoke(MongoDatabase database, IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        String professionCode = key.getProfessionCode();
        if (professionCode == null || professionCode.trim().isEmpty() || professionCode.equalsIgnoreCase("random")) {
            throw new IllegalArgumentException("Invalid profession code: " + professionCode);
        }

        try {
            // Ensure profession exists
            CreateProfession.invoke(database, key.getProfessionCode(), key.getProfessionCode());

            MongoCollection<Document> identities = database.getCollection("identities");
            
            Document propsDoc = new Document("level", properties.getLevel())
                    .append("exp", properties.getExp())
                    .append("stat_points", properties.getStatPoints())
                    .append("head_text", properties.getHeadText());

            Document statsDoc = new Document("energy", stats.getEnergy())
                    .append("health", stats.getHealth())
                    .append("food", stats.getFood())
                    .append("strength", stats.getStrength())
                    .append("defense", stats.getDefense())
                    .append("wisdom", stats.getWisdom())
                    .append("luck", stats.getLuck())
                    .append("agility", stats.getAgility())
                    .append("resilience", stats.getResilience());

            Document identityDoc = new Document("_id", key.getUniqueId().toString())
                    .append("profession_code", key.getProfessionCode())
                    .append("properties", propsDoc)
                    .append("stats", statsDoc)
                    .append("cosmetics", new Document("skin", null).append("aura", null))
                    .append("equipped_abilities", new Document())
                    .append("equipped_runes", new Document());

            identities.insertOne(identityDoc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create identity in MongoDB", e);
        }
    }
}
