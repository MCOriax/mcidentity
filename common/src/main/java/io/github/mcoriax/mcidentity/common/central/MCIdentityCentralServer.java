package io.github.mcoriax.mcidentity.common.central;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.api.model.Profession;
import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;
import io.github.mcoriax.mcidentity.common.db.StatKeys;
import io.github.mcoriax.mcidentity.common.model.ConfiguredProfession;
import io.github.mcoriax.mcidentity.common.model.PlayerStatsRecord;
import io.github.mcoriax.mcidentity.common.model.ProfessionProgressRecord;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Central server implementation of {@link IMCIdentityDB}.
 *
 * <p>Instead of opening a database connection, every operation is a JSON call
 * to the MCIdentity central REST server, which owns the data for the whole
 * network. A single pooled {@link CentralHttpClient} with keep-alive connections is
 * reused for all requests, and every call stays fully asynchronous on the
 * client's executor, so the game thread is never blocked. Every player
 * resource lives under {@code /v1/players/{playerUuid}}.</p>
 *
 * <p>Failure semantics mirror the database backends: reads resolve to their
 * neutral fallback (empty optional, empty collection, or zero) and writes
 * complete normally after logging, while {@link #createPlayer} and
 * {@link #initializeSchema} complete exceptionally so callers can react.</p>
 */
public class MCIdentityCentralServer implements IMCIdentityDB {

    /**
     * Base path of every player resource.
     */
    private static final String PATH_PLAYERS = "/v1/players/";

    /**
     * The stats resource below a player.
     */
    private static final String PATH_STATS = "/stats";

    /**
     * The pooled HTTP client carrying the bearer token.
     */
    private final CentralHttpClient http;

    /**
     * Creates the client for a central server.
     * @param baseUrl The server's base URL.
     * @param token The bearer token.
     * @param timeoutSeconds The request timeout.
     */
    public MCIdentityCentralServer(String baseUrl, String token, int timeoutSeconds) {
        this.http = new CentralHttpClient(baseUrl, token, timeoutSeconds);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> initializeSchema() {
        return http.checkHealth();
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        JsonObject body = new JsonObject();
        body.addProperty("systemCode", systemCode);
        body.addProperty("displayName", displayName);
        return http.write("POST", "/v1/professions", body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return http.read("/v1/professions", Collections.emptyList(), json -> {
            List<Profession> professions = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray()) {
                JsonObject o = element.getAsJsonObject();
                professions.add(new ConfiguredProfession(o.get("systemCode").getAsString(), o.get("displayName").getAsString()));
            }
            return professions;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createPlayer(UUID playerUuid, PlayerStats stats) {
        JsonObject body = new JsonObject();
        body.addProperty("playerUuid", playerUuid.toString());
        body.add("stats", toStatsJson(stats));
        return http.send("POST", "/v1/players", body).thenAccept(response -> {
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Failed to create the player on the central server: status " + response.statusCode());
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<ProfessionProgress>> getProgress(UUID playerUuid, String professionCode) {
        return http.read(progressPath(playerUuid, professionCode), Optional.empty(), json -> {
            JsonObject o = json.getAsJsonObject();
            return Optional.of(new ProfessionProgressRecord(o.get("level").getAsInt(), o.get("exp").getAsInt()));
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateProgress(UUID playerUuid, String professionCode, ProfessionProgress progress) {
        JsonObject body = new JsonObject();
        body.addProperty("level", progress.getLevel());
        body.addProperty("exp", progress.getExp());
        return http.write("PUT", progressPath(playerUuid, professionCode), body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<PlayerStats>> getStats(UUID playerUuid) {
        return http.read(PATH_PLAYERS + playerUuid + PATH_STATS, Optional.empty(),
            json -> Optional.of(toStats(json.getAsJsonObject())));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getStat(UUID playerUuid, String statKey) {
        if (!StatKeys.isValid(statKey)) {
            return CompletableFuture.completedFuture(0);
        }
        return http.read(PATH_PLAYERS + playerUuid + PATH_STATS + "/" + statKey, 0,
            json -> json.getAsJsonObject().get("value").getAsInt());
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<String>> getActiveProfession(UUID playerUuid) {
        return http.read(PATH_PLAYERS + playerUuid + "/profession", Optional.empty(),
            json -> Optional.of(json.getAsJsonObject().get("professionCode").getAsString()));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setActiveProfession(UUID playerUuid, String professionCode) {
        JsonObject body = new JsonObject();
        body.addProperty("professionCode", professionCode);
        return http.write("PUT", PATH_PLAYERS + playerUuid + "/profession", body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, String professionCode, ActionType actionType) {
        JsonObject body = new JsonObject();
        body.addProperty("playerUuid", playerUuid.toString());
        body.addProperty("professionCode", professionCode);
        body.addProperty("actionType", actionType.name());
        return http.write("POST", "/v1/profession-logs", body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateStats(UUID playerUuid, PlayerStats stats) {
        return http.write("PUT", PATH_PLAYERS + playerUuid + PATH_STATS, toStatsJson(stats));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> addStat(UUID playerUuid, String statKey, int amount) {
        if (!StatKeys.isValid(statKey)) {
            return CompletableFuture.completedFuture(null);
        }
        JsonObject body = new JsonObject();
        body.addProperty("amount", amount);
        return http.write("POST", PATH_PLAYERS + playerUuid + PATH_STATS + "/" + statKey + "/add", body);
    }

    /**
     * Pings the server's health endpoint, which also wakes a cold server.
     * @return A future with true when the server answers.
     */
    public CompletableFuture<Boolean> pingHealth() {
        return http.pingHealth();
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        http.close();
    }

    // --- Internal helpers ---

    /**
     * Builds the path of one profession's progress.
     * @param playerUuid The player's UUID.
     * @param professionCode The profession's system code.
     * @return The path.
     */
    private static String progressPath(UUID playerUuid, String professionCode) {
        return PATH_PLAYERS + playerUuid + "/professions/"
            + URLEncoder.encode(professionCode, StandardCharsets.UTF_8) + "/progress";
    }

    /**
     * Writes stats as the JSON body the server expects.
     * @param stats The stats.
     * @return The JSON object.
     */
    private static JsonObject toStatsJson(PlayerStats stats) {
        JsonObject body = new JsonObject();
        body.addProperty("statPoints", stats.getStatPoints());
        body.addProperty("energy", stats.getEnergy());
        body.addProperty("health", stats.getHealth());
        body.addProperty("food", stats.getFood());
        body.addProperty("strength", stats.getStrength());
        body.addProperty("defense", stats.getDefense());
        body.addProperty("wisdom", stats.getWisdom());
        body.addProperty("luck", stats.getLuck());
        body.addProperty("agility", stats.getAgility());
        body.addProperty("resilience", stats.getResilience());
        return body;
    }

    /**
     * Reads stats from the server's JSON.
     * @param o The JSON object.
     * @return The stats.
     */
    private static PlayerStats toStats(JsonObject o) {
        return new PlayerStatsRecord(o.get("statPoints").getAsInt(),
            o.get("energy").getAsInt(), o.get("health").getAsInt(), o.get("food").getAsInt(),
            o.get("strength").getAsInt(), o.get("defense").getAsInt(), o.get("wisdom").getAsInt(),
            o.get("luck").getAsInt(), o.get("agility").getAsInt(), o.get("resilience").getAsInt());
    }
}
