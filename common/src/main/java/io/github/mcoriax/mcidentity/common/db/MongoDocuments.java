package io.github.mcoriax.mcidentity.common.db;

import com.mongodb.client.model.Updates;
import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.IdentityKey;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
import io.github.mcoriax.mcidentity.common.IdentityAlias;
import io.github.mcoriax.mcidentity.common.model.IdentityStatsRecord;
import io.github.mcoriax.mcidentity.common.model.MutableIdentityProperties;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Date;
import java.util.UUID;

/**
 * The document layout of the {@link MongoDB} backend: collection and field
 * names, and the mapping between documents and the identity model.
 *
 * <p><strong>Internal.</strong> Package-private. Kept apart from the backend
 * so the backend reads as the {@code IMCIdentityDB} operations alone.</p>
 */
final class MongoDocuments {

    /**
     * Path of the embedded field holding the alias exactly as the player typed it.
     */
    static final String ALIAS_FIELD = "properties.alias";

    /**
     * Path of the embedded field holding the folded alias. MongoDB compares
     * strings byte by byte under the default collation, so the folded mirror is
     * what makes uniqueness and lookups case-insensitive.
     */
    static final String ALIAS_COMPARABLE_FIELD = "properties.alias_comparable";

    /**
     * Collection holding one document per identity, properties and stats embedded.
     */
    static final String COLLECTION_IDENTITIES = "identities";

    /**
     * Collection holding the profession registry.
     */
    static final String COLLECTION_PROFESSIONS = "identity_professions";

    /**
     * Collection holding one document per player's equipped-identity session.
     */
    static final String COLLECTION_SESSIONS = "identity_sessions";

    /**
     * Field carrying a profession's stable system code.
     */
    static final String FIELD_SYSTEM_CODE = "system_code";

    /**
     * Field carrying a profession's human-readable name.
     */
    static final String FIELD_DISPLAY_NAME = "display_name";

    /**
     * Embedded document holding an identity's properties.
     */
    static final String FIELD_PROPERTIES = "properties";

    /**
     * Embedded document holding an identity's stats.
     */
    static final String FIELD_STATS = "stats";

    /**
     * Field carrying the identity UUID a session points at.
     */
    static final String FIELD_IDENTITY_UUID = "identity_uuid";

    /**
     * Field carrying a session's content key.
     */
    static final String FIELD_CONTENT_KEY = "content_key";

    /**
     * Field carrying a player's unlocked base-identity slot count.
     */
    static final String FIELD_SLOTS = "slots";

    private MongoDocuments() {
    }

    /**
     * Builds a new identity document with its properties and stats embedded.
     * @param key The identity key.
     * @param properties The starting properties.
     * @param stats The starting stats.
     * @return The identity document.
     */
    static Document newIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        Document propsDoc = new Document("level", properties.getLevel())
            .append("exp", properties.getExp())
            .append("stat_points", properties.getStatPoints())
            .append("head_text", properties.getHeadText());

        return new Document("_id", key.getUniqueId().toString())
            .append("profession_code", key.getProfessionCode())
            .append(FIELD_PROPERTIES, propsDoc)
            .append(FIELD_STATS, toStatsDocument(stats));
    }

    /**
     * Builds a profession registry entry.
     * @param systemCode The profession system code.
     * @param displayName The profession display name.
     * @return The entry document.
     */
    static Document professionEntry(String systemCode, String displayName) {
        return new Document(FIELD_SYSTEM_CODE, systemCode).append(FIELD_DISPLAY_NAME, displayName);
    }

    /**
     * Reads the embedded properties document.
     * @param props The embedded properties.
     * @return The properties.
     */
    static MutableIdentityProperties readProperties(Document props) {
        return new MutableIdentityProperties(
            props.getString("alias"),
            props.getInteger("level", 1),
            props.getInteger("exp", 0),
            props.getInteger("stat_points", 0),
            props.getString("head_text"),
            props.getString("inventory_contents"),
            props.getString("enderchest_contents"));
    }

    /**
     * Reads the embedded stats document; a missing stat reads as its default.
     * @param stats The embedded stats.
     * @return The stats.
     */
    static IdentityStats readStats(Document stats) {
        return new IdentityStatsRecord(
            stats.getInteger("energy", 0), stats.getInteger("health", 0), stats.getInteger("food", 20),
            stats.getInteger("strength", 0), stats.getInteger("defense", 0), stats.getInteger("wisdom", 0),
            stats.getInteger("luck", 0), stats.getInteger("agility", 0), stats.getInteger("resilience", 0));
    }

    /**
     * Serializes a stats view into its embedded document form.
     * @param stats The stats to serialize.
     * @return The document with all nine stat fields.
     */
    static Document toStatsDocument(IdentityStats stats) {
        return new Document("energy", stats.getEnergy())
            .append("health", stats.getHealth())
            .append("food", stats.getFood())
            .append("strength", stats.getStrength())
            .append("defense", stats.getDefense())
            .append("wisdom", stats.getWisdom())
            .append("luck", stats.getLuck())
            .append("agility", stats.getAgility())
            .append("resilience", stats.getResilience());
    }

    /**
     * Builds the update that writes every property except the alias.
     *
     * <p>Each field is set individually rather than replacing the whole
     * subdocument, so the alias — claimed through {@code setAlias}, which
     * checks uniqueness — survives a property write untouched.</p>
     * @param properties The properties to write.
     * @return The update.
     */
    static Bson propertiesUpdate(IdentityProperties properties) {
        return Updates.combine(
            Updates.set("properties.level", properties.getLevel()),
            Updates.set("properties.exp", properties.getExp()),
            Updates.set("properties.stat_points", properties.getStatPoints()),
            Updates.set("properties.head_text", properties.getHeadText()),
            Updates.set("properties.inventory_contents", properties.getInventoryContents()),
            Updates.set("properties.enderchest_contents", properties.getEnderchestContents()));
    }

    /**
     * Builds the update that claims or clears an alias, keeping the folded
     * mirror field in step.
     * @param alias The alias to claim, or null to clear it.
     * @return The update.
     */
    static Bson aliasUpdate(String alias) {
        return alias != null
            ? Updates.combine(
                Updates.set(ALIAS_FIELD, alias),
                Updates.set(ALIAS_COMPARABLE_FIELD, IdentityAlias.comparable(alias)))
            : Updates.combine(
                Updates.unset(ALIAS_FIELD),
                Updates.unset(ALIAS_COMPARABLE_FIELD));
    }

    /**
     * Builds a session document.
     * @param playerUuid The player.
     * @param identityUuid The identity equipped.
     * @param contentKey The serialized identity item.
     * @return The session document.
     */
    static Document session(UUID playerUuid, UUID identityUuid, String contentKey) {
        return new Document("_id", playerUuid.toString())
            .append(FIELD_IDENTITY_UUID, identityUuid.toString())
            .append(FIELD_CONTENT_KEY, contentKey)
            .append("equipped_at", new Date());
    }

    /**
     * Builds an audit log entry.
     * @param playerUuid The player.
     * @param identityUuid The identity acted on.
     * @param actionType The action.
     * @return The log document.
     */
    static Document logEntry(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return new Document("player_uuid", playerUuid.toString())
            .append(FIELD_IDENTITY_UUID, identityUuid.toString())
            .append("action_type", actionType.name())
            .append("action_time", new Date());
    }
}
