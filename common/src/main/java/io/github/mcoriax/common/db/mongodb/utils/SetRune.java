package io.github.mcoriax.common.db.mongodb.utils;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.UUID;

public class SetRune {
    public static void invoke(MongoDatabase database, UUID identityUuid, int slot, String runeCode) {
        MongoCollection<Document> identities = database.getCollection("identities");
        if (runeCode == null) {
            identities.updateOne(Filters.eq("_id", identityUuid.toString()), Updates.unset("equipped_runes." + slot));
        } else {
            identities.updateOne(Filters.eq("_id", identityUuid.toString()), Updates.set("equipped_runes." + slot, runeCode));
        }
    }
}
