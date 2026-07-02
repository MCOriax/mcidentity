package io.github.mcoriax.common.db.central;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import io.github.mcoriax.api.model.ActionType;
import io.github.mcoriax.api.model.EquippedAbility;
import io.github.mcoriax.api.model.IdentityKey;
import io.github.mcoriax.api.model.IdentityProperties;
import io.github.mcoriax.api.model.IdentitySession;
import io.github.mcoriax.api.model.IdentityStats;
import io.github.mcoriax.common.model.MutableIdentityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link MCIdentityCentralServer} against a stub HTTP server,
 * verifying the request lines, authentication header, JSON payloads, and
 * response parsing for the central server protocol.
 */
class MCIdentityCentralServerTest {

    /**
     * One request captured by the stub server.
     * @param method The HTTP method.
     * @param path The request path.
     * @param auth The Authorization header value.
     * @param body The request body.
     */
    record CapturedRequest(String method, String path, String auth, String body) {}

    /**
     * The stub HTTP server standing in for the central server.
     */
    private HttpServer server;

    /**
     * Requests received by the stub server, in arrival order.
     */
    private final ConcurrentLinkedQueue<CapturedRequest> requests = new ConcurrentLinkedQueue<>();

    /**
     * Client under test, pointed at the stub server.
     */
    private MCIdentityCentralServer client;

    /**
     * Status code the stub responds with.
     */
    private volatile int responseStatus = 200;

    /**
     * Body the stub responds with.
     */
    private volatile String responseBody = "{}";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            byte[] payload = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, payload.length == 0 ? -1 : payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            }
        });
        server.start();
        client = new MCIdentityCentralServer(
            "http://127.0.0.1:" + server.getAddress().getPort(), "secret-token", 5);
    }

    @AfterEach
    void stopServer() {
        client.shutdown();
        server.stop(0);
    }

    /**
     * Returns the single request the test expects the client to have sent.
     * @return The captured request.
     */
    private CapturedRequest onlyRequest() {
        assertEquals(1, requests.size());
        return requests.peek();
    }

    @Test
    void initializeSchemaChecksHealth() throws Exception {
        responseBody = "{\"status\":\"ok\"}";
        client.initializeSchema().get();
        assertEquals("/health", onlyRequest().path());
    }

    @Test
    void initializeSchemaFailsOnBadStatus() {
        responseStatus = 503;
        assertThrows(ExecutionException.class, () -> client.initializeSchema().get());
    }

    @Test
    void createProfessionSendsAuthorizedJson() throws Exception {
        responseStatus = 204;
        responseBody = "";
        client.createProfession("archer", "Archer").get();

        CapturedRequest request = onlyRequest();
        assertEquals("POST", request.method());
        assertEquals("/v1/professions", request.path());
        assertEquals("Bearer secret-token", request.auth());
        JsonObject body = JsonParser.parseString(request.body()).getAsJsonObject();
        assertEquals("archer", body.get("systemCode").getAsString());
        assertEquals("Archer", body.get("displayName").getAsString());
    }

    @Test
    void getIdentityKeyParsesResponse() throws Exception {
        UUID uuid = UUID.randomUUID();
        responseBody = "{\"uuid\":\"" + uuid + "\",\"professionCode\":\"miner\"}";

        Optional<IdentityKey> key = client.getIdentityKey(uuid).get();
        assertTrue(key.isPresent());
        assertEquals("miner", key.get().getProfessionCode());
        assertEquals(uuid, key.get().getUniqueId());
        assertEquals("/v1/identities/" + uuid, onlyRequest().path());
    }

    @Test
    void getIdentityKeyMapsNotFoundToEmpty() throws Exception {
        responseStatus = 404;
        responseBody = "{\"error\":\"Identity not found\"}";
        assertTrue(client.getIdentityKey(UUID.randomUUID()).get().isEmpty());
    }

    @Test
    void readsFallBackWhenServerUnreachable() throws Exception {
        MCIdentityCentralServer offline = new MCIdentityCentralServer("http://127.0.0.1:1", "secret-token", 1);
        assertTrue(offline.getProfessions().get().isEmpty());
        assertEquals(0, offline.getStat(UUID.randomUUID(), "energy").get());
        offline.shutdown();
    }

    @Test
    void createIdentitySendsFullDocumentAndFailsOnConflict() throws Exception {
        UUID uuid = UUID.randomUUID();
        IdentityKey key = new IdentityKey() {
            @Override public UUID getUniqueId() { return uuid; }
            @Override public String getProfessionCode() { return "miner"; }
        };
        IdentityProperties properties = new MutableIdentityProperties(2, 50, 1, "head", null, null, null, null);
        IdentityStats stats = statsOf(9);

        responseStatus = 201;
        responseBody = "{}";
        client.createIdentity(key, properties, stats).get();

        JsonObject body = JsonParser.parseString(onlyRequest().body()).getAsJsonObject();
        assertEquals(uuid.toString(), body.get("uuid").getAsString());
        assertEquals("miner", body.get("professionCode").getAsString());
        assertEquals(2, body.getAsJsonObject("properties").get("level").getAsInt());
        assertEquals(9, body.getAsJsonObject("stats").get("luck").getAsInt());

        responseStatus = 409;
        assertThrows(ExecutionException.class, () -> client.createIdentity(key, properties, stats).get());
    }

    @Test
    void getStatsParsesAllFields() throws Exception {
        responseBody = "{\"energy\":1,\"health\":2,\"food\":3,\"strength\":4,\"defense\":5,"
            + "\"wisdom\":6,\"luck\":7,\"agility\":8,\"resilience\":9}";
        Optional<IdentityStats> stats = client.getStats(UUID.randomUUID()).get();
        assertTrue(stats.isPresent());
        assertEquals(1, stats.get().getEnergy());
        assertEquals(9, stats.get().getResilience());
    }

    @Test
    void statOperationsRejectUnknownKeysWithoutHttp() throws Exception {
        UUID uuid = UUID.randomUUID();
        assertEquals(0, client.getStat(uuid, "updated_at").get());
        client.addStat(uuid, "drop table", 5).get();
        assertTrue(requests.isEmpty());
    }

    @Test
    void addStatPostsAmount() throws Exception {
        responseStatus = 204;
        responseBody = "";
        UUID uuid = UUID.randomUUID();
        client.addStat(uuid, "luck", 3).get();

        CapturedRequest request = onlyRequest();
        assertEquals("/v1/identities/" + uuid + "/stats/luck/add", request.path());
        assertEquals(3, JsonParser.parseString(request.body()).getAsJsonObject().get("amount").getAsInt());
    }

    @Test
    void equippedAbilitiesRoundTrip() throws Exception {
        UUID uuid = UUID.randomUUID();
        responseBody = "{\"1\":{\"systemCode\":\"dash\",\"modelId\":null}}";
        Map<Integer, EquippedAbility> slots = client.getEquippedAbilities(uuid).get();
        assertEquals(1, slots.size());
        assertEquals("dash", slots.get(1).systemCode());
        assertNull(slots.get(1).modelId());
        requests.clear();

        responseStatus = 204;
        responseBody = "";
        client.setEquippedAbility(uuid, 1, null, null).get();
        CapturedRequest request = onlyRequest();
        assertEquals("PUT", request.method());
        assertEquals("/v1/identities/" + uuid + "/abilities/1", request.path());
        assertTrue(JsonParser.parseString(request.body()).getAsJsonObject().get("abilityCode").isJsonNull());
    }

    @Test
    void sessionRoundTrip() throws Exception {
        UUID player = UUID.randomUUID();
        UUID identity = UUID.randomUUID();
        responseBody = "{\"identityUuid\":\"" + identity + "\",\"contentKey\":\"item64\"}";

        Optional<IdentitySession> session = client.getSession(player).get();
        assertTrue(session.isPresent());
        assertEquals(identity, session.get().getIdentityUniqueId());
        assertEquals("item64", session.get().getContentKey());
        assertEquals(identity, client.getActiveIdentity(player).get().orElseThrow());
        requests.clear();

        responseStatus = 204;
        responseBody = "";
        client.setActiveIdentity(player, null, null).get();
        CapturedRequest request = onlyRequest();
        assertEquals("PUT", request.method());
        assertEquals("/v1/players/" + player + "/session", request.path());
        assertTrue(JsonParser.parseString(request.body()).getAsJsonObject().get("identityUuid").isJsonNull());
    }

    @Test
    void cosmeticsUseSingleFieldUpdates() throws Exception {
        UUID uuid = UUID.randomUUID();
        responseStatus = 204;
        responseBody = "";
        client.saveSkin(uuid, "skin64").get();
        client.saveAura(uuid, null).get();

        assertEquals(2, requests.size());
        CapturedRequest skin = requests.poll();
        assertEquals("/v1/identities/" + uuid + "/cosmetics", skin.path());
        assertEquals("skin64", JsonParser.parseString(skin.body()).getAsJsonObject().get("skin").getAsString());
        CapturedRequest aura = requests.poll();
        JsonObject auraBody = JsonParser.parseString(aura.body()).getAsJsonObject();
        assertTrue(auraBody.get("aura").isJsonNull());
        assertTrue(!auraBody.has("skin"));
    }

    @Test
    void logActionPostsAuditEntry() throws Exception {
        responseStatus = 204;
        responseBody = "";
        UUID player = UUID.randomUUID();
        UUID identity = UUID.randomUUID();
        client.logAction(player, identity, ActionType.EQUIP).get();

        CapturedRequest request = onlyRequest();
        assertEquals("/v1/logs", request.path());
        JsonObject body = JsonParser.parseString(request.body()).getAsJsonObject();
        assertEquals("EQUIP", body.get("actionType").getAsString());
        assertEquals(player.toString(), body.get("playerUuid").getAsString());
    }

    /**
     * Builds a stats document where every field carries the given value.
     * @param value The value for all nine stats.
     * @return The stats view.
     */
    private static IdentityStats statsOf(int value) {
        return new IdentityStats() {
            @Override public int getEnergy() { return value; }
            @Override public int getHealth() { return value; }
            @Override public int getFood() { return value; }
            @Override public int getStrength() { return value; }
            @Override public int getDefense() { return value; }
            @Override public int getWisdom() { return value; }
            @Override public int getLuck() { return value; }
            @Override public int getAgility() { return value; }
            @Override public int getResilience() { return value; }
        };
    }
}
