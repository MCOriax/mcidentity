package io.github.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.github.mcoriax.api.model.IdentityProperties;
import io.github.mcoriax.common.model.MutableIdentityProperties;
import org.bson.Document;

import java.util.Optional;
import java.util.UUID;

public class GetProperties {
    public static Optional<IdentityProperties> invoke(MongoDatabase database, UUID identityUuid) {
        Document doc = database.getCollection("identities").find(Filters.eq("_id", identityUuid.toString())).first();
        if (doc != null && doc.containsKey("properties")) {
            Document props = doc.get("properties", Document.class);
            return Optional.of(new MutableIdentityProperties(
                props.getInteger("level", 1),
                props.getInteger("exp", 0),
                props.getInteger("stat_points", 0),
                props.getString("head_text"),
                null,
                null,
                props.getString("inventory_contents"),
                props.getString("enderchest_contents")
            ));
        }
        return Optional.empty();
    }
}
