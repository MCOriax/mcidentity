package io.github.mcoriax.mcidentity.common.db;

import com.mongodb.client.model.Filters;
import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;
import io.github.mcoriax.mcidentity.common.model.PlayerStatsRecord;
import io.github.mcoriax.mcidentity.common.model.ProfessionProgressRecord;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Date;
import java.util.UUID;

/**
 * The document layout of the {@link MongoDB} backend: collection and field
 * names, and the mapping between documents and the player model.
 *
 * <p><strong>Internal.</strong> Package-private. Kept apart from the backend
 * so the backend reads as the {@code IMCIdentityDB} operations alone. The
 * collections mirror the SQL tables of the same names.</p>
 */
final class MongoDocuments {

    static final String COLLECTION_PROFESSIONS = "identity_professions";

    static final String COLLECTION_STATS = "player_stats";

    static final String COLLECTION_ACTIVE = "player_professions";

    static final String COLLECTION_PROGRESS = "player_profession_progress";

    static final String COLLECTION_LOGS = "profession_logs";

    static final String FIELD_ID = "_id";

    static final String FIELD_SYSTEM_CODE = "system_code";

    static final String FIELD_DISPLAY_NAME = "display_name";

    static final String FIELD_PLAYER_UUID = "player_uuid";

    static final String FIELD_PROFESSION_CODE = "profession_code";

    static final String FIELD_ACTIVE_PROFESSION = "active_profession";

    static final String FIELD_LEVEL = "level";

    static final String FIELD_EXP = "exp";

    static final String FIELD_STAT_POINTS = "stat_points";

    private MongoDocuments() {
    }

    static Document professionEntry(String systemCode, String displayName) {
        return new Document(FIELD_SYSTEM_CODE, systemCode).append(FIELD_DISPLAY_NAME, displayName);
    }

    static Bson byPlayer(UUID playerUuid) {
        return Filters.eq(FIELD_ID, playerUuid.toString());
    }

    static Bson byProgress(UUID playerUuid, String professionCode) {
        return Filters.and(
            Filters.eq(FIELD_PLAYER_UUID, playerUuid.toString()),
            Filters.eq(FIELD_PROFESSION_CODE, professionCode));
    }

    static Document toStatsDocument(PlayerStats stats) {
        return new Document(FIELD_STAT_POINTS, stats.getStatPoints())
            .append("energy", stats.getEnergy())
            .append("health", stats.getHealth())
            .append("food", stats.getFood())
            .append("strength", stats.getStrength())
            .append("defense", stats.getDefense())
            .append("wisdom", stats.getWisdom())
            .append("luck", stats.getLuck())
            .append("agility", stats.getAgility())
            .append("resilience", stats.getResilience());
    }

    static PlayerStats readStats(Document doc) {
        return new PlayerStatsRecord(doc.getInteger(FIELD_STAT_POINTS, 0),
            doc.getInteger("energy", 0), doc.getInteger("health", 0), doc.getInteger("food", 20),
            doc.getInteger("strength", 0), doc.getInteger("defense", 0), doc.getInteger("wisdom", 0),
            doc.getInteger("luck", 0), doc.getInteger("agility", 0), doc.getInteger("resilience", 0));
    }

    static Document toProgressDocument(ProfessionProgress progress) {
        return new Document(FIELD_LEVEL, progress.getLevel()).append(FIELD_EXP, progress.getExp());
    }

    static ProfessionProgress readProgress(Document doc) {
        return new ProfessionProgressRecord(doc.getInteger(FIELD_LEVEL, 1), doc.getInteger(FIELD_EXP, 0));
    }

    static Document activeProfession(String professionCode) {
        return new Document(FIELD_ACTIVE_PROFESSION, professionCode).append("equipped_at", new Date());
    }

    static Document logEntry(UUID playerUuid, String professionCode, ActionType actionType) {
        return new Document(FIELD_PLAYER_UUID, playerUuid.toString())
            .append(FIELD_PROFESSION_CODE, professionCode)
            .append("action_type", actionType.name())
            .append("action_time", new Date());
    }
}
