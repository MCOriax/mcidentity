package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import org.bson.Document;

import java.util.Optional;
import java.util.UUID;

public class GetProperties {
    public static Optional<IdentityProperties> invoke(MongoDatabase database, UUID identityUuid) {
        Document doc = database.getCollection("identities").find(Filters.eq("_id", identityUuid.toString())).first();
        if (doc != null && doc.containsKey("properties")) {
            Document props = doc.get("properties", Document.class);
            return Optional.of(new IdentityProperties() {
                private String skin;
                private String aura;
                private String inventoryContents = props.getString("inventory_contents");
                private String enderchestContents = props.getString("enderchest_contents");
                
                @Override public int getLevel() { return props.getInteger("level", 1); }
                @Override public int getExp() { return props.getInteger("exp", 0); }
                @Override public int getStatPoints() { return props.getInteger("stat_points", 0); }
                @Override public String getHeadText() { return props.getString("head_text"); }
                @Override public String getSkin() { return skin; }
                @Override public void setSkin(String skin) { this.skin = skin; }
                @Override public String getAura() { return aura; }
                @Override public void setAura(String aura) { this.aura = aura; }
                @Override public String getInventoryContents() { return inventoryContents; }
                @Override public void setInventoryContents(String inventoryContents) { this.inventoryContents = inventoryContents; }
                @Override public String getEnderchestContents() { return enderchestContents; }
                @Override public void setEnderchestContents(String enderchestContents) { this.enderchestContents = enderchestContents; }
            });
        }
        return Optional.empty();
    }
}
