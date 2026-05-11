package io.gitlab.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import org.bson.Document;

import java.util.UUID;

public class UpdateProperties {
    public static void invoke(MongoDatabase database, UUID identityUuid, IdentityProperties properties) {
        Document propsDoc = new Document("level", properties.getLevel())
                .append("exp", properties.getExp())
                .append("stat_points", properties.getStatPoints())
                .append("head_text", properties.getHeadText());

        database.getCollection("identities").updateOne(
                Filters.eq("_id", identityUuid.toString()),
                Updates.set("properties", propsDoc)
        );
    }
}
