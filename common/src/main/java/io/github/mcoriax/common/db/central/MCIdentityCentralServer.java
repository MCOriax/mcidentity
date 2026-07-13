package io.github.mcoriax.common.db.central;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.mcoriax.api.db.IMCIdentityDB;
import io.github.mcoriax.api.model.*;
import io.github.mcoriax.common.MCIdentityManager;
import io.github.mcoriax.common.db.StatKeys;
import io.github.mcoriax.common.model.MutableIdentityProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central server implementation of {@link IMCIdentityDB}.
 *
 * <p>Instead of opening a database connection, every operation is a JSON call
 * to the MCIdentity central REST server, which owns the data for the whole
 * network. A single pooled {@link HttpClient} with keep-alive connections is
 * reused for all requests, and every call stays fully asynchronous on the
 * client's executor, so the game thread is never blocked.</p>
 *
 * <p>Failure semantics mirror the previous database backends: reads resolve
 * to their neutral fallback (empty optional, empty collection, or zero) and
 * writes complete normally after logging, while {@link #createIdentity} and
 * {@link #initializeSchema} complete exceptionally so callers can react.</p>
 */
public class MCIdentityCentralServer implements IMCIdentityDB {

    /**
     * Logger for transport and protocol failures.
     */
    private static final Logger LOGGER = Logger.getLogger(MCIdentityCentralServer.class.getName());

    /**
     * JSON codec configured to keep explicit nulls, which the API uses to
     * clear slots, cosmetics, and sessions.
     */
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    /**
     * Shared HTTP client holding the connection pool for all requests.
     */
    private final HttpClient client;

    /**
     * Base URI of the central server, without a trailing slash.
     */
    private final URI baseUri;

    /**
     * Value of the Authorization header sent with every request.
     */
    private final String authHeader;

    /**
     * Per-request timeout.
     */
    private final Duration timeout;

    /**
     * Creates a client for the given central server.
     * @param baseUrl The base URL of the central server, e.g. {@code https://server.com}.
     * @param token The bearer token matching the server's read-write credential.
     * @param timeoutSeconds The connect and request timeout in seconds.
     */
    public MCIdentityCentralServer(String baseUrl, String token, int timeoutSeconds) {
        this.baseUri = URI.create(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        this.authHeader = "Bearer " + token;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        // Redirects are never followed so the bearer token can only ever be
        // sent to the configured host.
        this.client = HttpClient.newBuilder()
            .connectTimeout(this.timeout)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> initializeSchema() {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(baseUri.getPath() + "/health"))
            .timeout(timeout)
            .GET()
            .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("Central server health check failed with status " + response.statusCode());
                }
            });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        JsonObject body = new JsonObject();
        body.addProperty("systemCode", systemCode);
        body.addProperty("displayName", displayName);
        return write("POST", "/v1/professions", body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return read("/v1/professions", Collections.emptyList(), json -> {
            List<Profession> professions = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray()) {
                professions.add(toProfession(element.getAsJsonObject()));
            }
            return professions;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createAbility(String abilityCode, String displayName, String abilitySkin) {
        JsonObject body = new JsonObject();
        body.addProperty("abilityCode", abilityCode);
        body.addProperty("displayName", displayName);
        body.addProperty("abilitySkin", abilitySkin);
        return write("POST", "/v1/abilities", body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Ability>> getAbilities() {
        return read("/v1/abilities", Collections.emptyList(), json -> {
            List<Ability> abilities = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray()) {
                abilities.add(toAbility(element.getAsJsonObject()));
            }
            return abilities;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Rune>> getRunes() {
        return read("/v1/runes", Collections.emptyList(), json -> {
            List<Rune> runes = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray()) {
                runes.add(toRune(element.getAsJsonObject()));
            }
            return runes;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        JsonObject props = new JsonObject();
        props.addProperty("level", properties.getLevel());
        props.addProperty("exp", properties.getExp());
        props.addProperty("statPoints", properties.getStatPoints());
        props.addProperty("headText", properties.getHeadText());

        JsonObject body = new JsonObject();
        body.addProperty("uuid", key.getUniqueId().toString());
        body.addProperty("professionCode", key.getProfessionCode());
        body.add("properties", props);
        body.add("stats", toStatsJson(stats));

        return send("POST", "/v1/identities", body).thenAccept(response -> {
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Failed to create identity on central server: status " + response.statusCode());
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return read("/v1/identities/" + uuid, Optional.empty(), json -> {
            String profession = json.getAsJsonObject().get("professionCode").getAsString();
            return Optional.of(new IdentityKey() {
                @Override public UUID getUniqueId() { return uuid; }
                @Override public String getProfessionCode() { return profession; }
            });
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid) {
        return read("/v1/identities/" + identityUuid + "/properties", Optional.empty(), json -> {
            JsonObject o = json.getAsJsonObject();
            return Optional.of(new MutableIdentityProperties(
                o.get("level").getAsInt(),
                o.get("exp").getAsInt(),
                o.get("statPoints").getAsInt(),
                asStringOrNull(o.get("headText")),
                null,
                null,
                asStringOrNull(o.get("inventoryContents")),
                asStringOrNull(o.get("enderchestContents"))
            ));
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return read("/v1/identities/" + identityUuid + "/stats", Optional.empty(),
            json -> Optional.of(toStats(json.getAsJsonObject())));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        if (!StatKeys.isValid(statKey)) {
            return CompletableFuture.completedFuture(0);
        }
        return read("/v1/identities/" + identityUuid + "/stats/" + statKey, 0,
            json -> json.getAsJsonObject().get("value").getAsInt());
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Map<Integer, EquippedAbility>> getEquippedAbilities(UUID identityUuid) {
        return read("/v1/identities/" + identityUuid + "/abilities", Collections.emptyMap(), json -> {
            Map<Integer, EquippedAbility> slots = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                JsonObject value = entry.getValue().getAsJsonObject();
                slots.put(Integer.valueOf(entry.getKey()),
                    new EquippedAbility(value.get("abilityCode").getAsString(), asStringOrNull(value.get("abilitySkin"))));
            }
            return slots;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode, String abilitySkin) {
        JsonObject body = new JsonObject();
        body.addProperty("abilityCode", abilityCode);
        body.addProperty("abilitySkin", abilitySkin);
        return write("PUT", "/v1/identities/" + identityUuid + "/abilities/" + slot, body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid) {
        return read("/v1/identities/" + identityUuid + "/runes", Collections.emptyMap(), json -> {
            Map<Integer, String> slots = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                slots.put(Integer.valueOf(entry.getKey()), entry.getValue().getAsString());
            }
            return slots;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode) {
        JsonObject body = new JsonObject();
        body.addProperty("runeCode", runeCode);
        return write("PUT", "/v1/identities/" + identityUuid + "/runes/" + slot, body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return read("/v1/players/" + playerUuid + "/session", Optional.empty(),
            json -> Optional.of(UUID.fromString(json.getAsJsonObject().get("identityUuid").getAsString())));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid) {
        return read("/v1/players/" + playerUuid + "/session", Optional.empty(), json -> {
            JsonObject o = json.getAsJsonObject();
            UUID identityUuid = UUID.fromString(o.get("identityUuid").getAsString());
            String contentKey = asStringOrNull(o.get("contentKey"));
            return Optional.of(new IdentitySession() {
                @Override public UUID getPlayerUniqueId() { return playerUuid; }
                @Override public UUID getIdentityUniqueId() { return identityUuid; }
                @Override public String getContentKey() { return contentKey; }
            });
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey) {
        JsonObject body = new JsonObject();
        body.addProperty("identityUuid", identityUuid != null ? identityUuid.toString() : null);
        body.addProperty("contentKey", contentKey);
        return write("PUT", "/v1/players/" + playerUuid + "/session", body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        JsonObject body = new JsonObject();
        body.addProperty("playerUuid", playerUuid.toString());
        body.addProperty("identityUuid", identityUuid.toString());
        body.addProperty("actionType", actionType.name());
        return write("POST", "/v1/logs", body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        JsonObject body = new JsonObject();
        body.addProperty("level", properties.getLevel());
        body.addProperty("exp", properties.getExp());
        body.addProperty("statPoints", properties.getStatPoints());
        body.addProperty("headText", properties.getHeadText());
        body.addProperty("inventoryContents", properties.getInventoryContents());
        body.addProperty("enderchestContents", properties.getEnderchestContents());
        return write("PUT", "/v1/identities/" + identityUuid + "/properties", body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return write("PUT", "/v1/identities/" + identityUuid + "/stats", toStatsJson(stats));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        if (!StatKeys.isValid(statKey)) {
            return CompletableFuture.completedFuture(null);
        }
        JsonObject body = new JsonObject();
        body.addProperty("amount", amount);
        return write("POST", "/v1/identities/" + identityUuid + "/stats/" + statKey + "/add", body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Map<String, String>> getCosmetics(UUID uuid) {
        return read("/v1/identities/" + uuid + "/cosmetics", Collections.emptyMap(), json -> {
            JsonObject o = json.getAsJsonObject();
            Map<String, String> cosmetics = new HashMap<>();
            cosmetics.put("skin", asStringOrNull(o.get("skin")));
            cosmetics.put("aura", asStringOrNull(o.get("aura")));
            return cosmetics;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> saveSkin(UUID uuid, String skin) {
        JsonObject body = new JsonObject();
        body.addProperty("skin", skin);
        return write("PUT", "/v1/identities/" + uuid + "/cosmetics", body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> saveAura(UUID uuid, String aura) {
        JsonObject body = new JsonObject();
        body.addProperty("aura", aura);
        return write("PUT", "/v1/identities/" + uuid + "/cosmetics", body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateSessionContentKey(UUID playerUuid, String contentKey) {
        JsonObject body = new JsonObject();
        body.addProperty("contentKey", contentKey);
        return write("PATCH", "/v1/players/" + playerUuid + "/session/content-key", body);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getBaseIdentitySlots(UUID playerUuid) {
        return read("/v1/players/" + playerUuid + "/base-slots", 0,
            json -> json.getAsJsonObject().get("slots").getAsInt());
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> addBaseIdentitySlots(UUID playerUuid, int amount) {
        JsonObject body = new JsonObject();
        body.addProperty("amount", amount);
        return send("POST", "/v1/players/" + playerUuid + "/base-slots/add", body)
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("Failed to update base identity slots on central server: status " + response.statusCode());
                }
                return JsonParser.parseString(response.body()).getAsJsonObject().get("slots").getAsInt();
            });
    }

    /**
     * Pings the central server's unversioned liveness probe. The request
     * itself also serves as a wake-up signal for cold-started deployments.
     * The returned future always completes: it resolves to {@code true} on a
     * 200 response and to {@code false} on any other status, a transport
     * failure, or a timeout.
     * @return A future resolving to whether the central server is online.
     */
    public CompletableFuture<Boolean> pingHealth() {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(baseUri.getPath() + "/health"))
            .timeout(timeout)
            .GET()
            .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> response.statusCode() == 200)
            .orTimeout(timeout.toSeconds() + 5, TimeUnit.SECONDS)
            .exceptionally(ex -> false);
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        client.close();
    }

    // --- Internal helpers ---

    /**
     * Sends one authenticated JSON request to the central server.
     * @param method The HTTP method.
     * @param path The API path relative to the base URL.
     * @param body The JSON body, or null for bodiless requests.
     * @return A future with the raw HTTP response.
     */
    private CompletableFuture<HttpResponse<String>> send(String method, String path, JsonObject body) {
        HttpRequest.BodyPublisher publisher = body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(GSON.toJson(body));
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(baseUri.getPath() + path))
            .timeout(timeout)
            .header("Authorization", authHeader)
            .header("Content-Type", "application/json")
            .method(method, publisher)
            .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Runs a write call, logging any transport or protocol failure without
     * failing the returned future, matching the old backends' behaviour.
     * @param method The HTTP method.
     * @param path The API path.
     * @param body The JSON body.
     * @return A future that completes when the write finishes.
     */
    private CompletableFuture<Void> write(String method, String path, JsonObject body) {
        return send(method, path, body)
            .thenAccept(response -> {
                if (response.statusCode() / 100 != 2) {
                    LOGGER.warning(() -> method + " " + path + " failed with status " + response.statusCode());
                }
            })
            .exceptionally(ex -> {
                LOGGER.log(Level.WARNING, ex, () -> method + " " + path + " failed");
                return null;
            });
    }

    /**
     * Runs a read call and parses the JSON payload, resolving to the given
     * fallback on 404, any other non-2xx status, or a transport failure.
     * @param path The API path.
     * @param fallback The neutral value used when the read fails.
     * @param parser Converts the response JSON into the result.
     * @param <T> The result type.
     * @return A future with the parsed result or the fallback.
     */
    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<T> read(String path, Object fallback, Function<JsonElement, T> parser) {
        return send("GET", path, null)
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    return parser.apply(JsonParser.parseString(response.body()));
                }
                if (response.statusCode() != 404) {
                    LOGGER.warning(() -> "GET " + path + " failed with status " + response.statusCode());
                }
                return (T) fallback;
            })
            .exceptionally(ex -> {
                LOGGER.log(Level.WARNING, ex, () -> "GET " + path + " failed");
                return (T) fallback;
            });
    }

    /**
     * Extracts a string from a JSON member, mapping JSON null to Java null.
     * @param element The JSON member, possibly null or JSON null.
     * @return The string value, or null.
     */
    private static String asStringOrNull(JsonElement element) {
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    /**
     * Serializes a stats document into its JSON form.
     * @param stats The stats to serialize.
     * @return The JSON object with all nine stat fields.
     */
    private static JsonObject toStatsJson(IdentityStats stats) {
        JsonObject body = new JsonObject();
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
     * Deserializes a stats document from its JSON form.
     * @param o The JSON object with all nine stat fields.
     * @return The immutable stats view.
     */
    private static IdentityStats toStats(JsonObject o) {
        int energy = o.get("energy").getAsInt();
        int health = o.get("health").getAsInt();
        int food = o.get("food").getAsInt();
        int strength = o.get("strength").getAsInt();
        int defense = o.get("defense").getAsInt();
        int wisdom = o.get("wisdom").getAsInt();
        int luck = o.get("luck").getAsInt();
        int agility = o.get("agility").getAsInt();
        int resilience = o.get("resilience").getAsInt();
        return new IdentityStats() {
            @Override public int getEnergy() { return energy; }
            @Override public int getHealth() { return health; }
            @Override public int getFood() { return food; }
            @Override public int getStrength() { return strength; }
            @Override public int getDefense() { return defense; }
            @Override public int getWisdom() { return wisdom; }
            @Override public int getLuck() { return luck; }
            @Override public int getAgility() { return agility; }
            @Override public int getResilience() { return resilience; }
        };
    }

    /**
     * Builds a profession view whose configuration accessors read the values
     * loaded into {@link MCIdentityManager} from the local profession files.
     * @param o The JSON object with the profession identity.
     * @return The profession view.
     */
    private static Profession toProfession(JsonObject o) {
        String code = o.get("systemCode").getAsString();
        String name = o.get("displayName").getAsString();
        return new Profession() {
            @Override public String getSystemCode() { return code; }
            @Override public String getDisplayName() { return name; }
            @Override public Map<String, Object> getExpConfig() { return MCIdentityManager.getProfessionExpConfig(code); }
            @Override public Map<String, Integer> getBaseStats() { return MCIdentityManager.getProfessionBaseStats(code); }
            @Override public String getStatsRandom() { return MCIdentityManager.getProfessionStatsRandom(code); }
            @Override public Map<Integer, String> getSkins() { return MCIdentityManager.getProfessionSkins(code); }
            @Override public void setSkins(Map<Integer, String> skins) { MCIdentityManager.setProfessionSkins(code, skins); }
        };
    }

    /**
     * Deserializes an ability from its JSON form.
     * @param o The JSON object with the ability fields.
     * @return The ability view.
     */
    private static Ability toAbility(JsonObject o) {
        String code = o.get("abilityCode").getAsString();
        String name = o.get("displayName").getAsString();
        String skin = asStringOrNull(o.get("abilitySkin"));
        return new Ability() {
            @Override public String getAbilityCode() { return code; }
            @Override public String getDisplayName() { return name; }
            @Override public String getAbilitySkin() { return skin; }
        };
    }

    /**
     * Deserializes a rune from its JSON form.
     * @param o The JSON object with the rune fields.
     * @return The rune view.
     */
    private static Rune toRune(JsonObject o) {
        String code = o.get("systemCode").getAsString();
        String name = o.get("displayName").getAsString();
        return new Rune() {
            @Override public String getSystemCode() { return code; }
            @Override public String getDisplayName() { return name; }
        };
    }
}
