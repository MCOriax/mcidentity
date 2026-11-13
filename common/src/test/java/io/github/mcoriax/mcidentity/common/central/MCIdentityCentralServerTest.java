package io.github.mcoriax.mcidentity.common.central;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;
import io.github.mcoriax.mcidentity.common.model.PlayerStatsRecord;
import io.github.mcoriax.mcidentity.common.model.ProfessionProgressRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void readsFallBackWhenServerUnreachable() throws Exception {
        MCIdentityCentralServer offline = new MCIdentityCentralServer("http://127.0.0.1:1", "secret-token", 1);
        assertTrue(offline.getProfessions().get().isEmpty());
        assertEquals(0, offline.getStat(UUID.randomUUID(), "energy").get());
        offline.shutdown();
    }

    @Test
    void createPlayerPostsStatsAndFailsOnBadStatus() throws Exception {
        UUID player = UUID.randomUUID();
        responseStatus = 201;
        client.createPlayer(player, statsOf(7)).get();

        CapturedRequest request = onlyRequest();
        assertEquals("POST", request.method());
        assertEquals("/v1/players", request.path());
        JsonObject body = JsonParser.parseString(request.body()).getAsJsonObject();
        assertEquals(player.toString(), body.get("playerUuid").getAsString());
        assertEquals(7, body.getAsJsonObject("stats").get("statPoints").getAsInt());
        assertEquals(7, body.getAsJsonObject("stats").get("resilience").getAsInt());

        responseStatus = 500;
        assertThrows(ExecutionException.class, () -> client.createPlayer(player, statsOf(1)).get());
    }

    @Test
    void getStatsParsesAllFields() throws Exception {
        UUID player = UUID.randomUUID();
        responseBody = "{\"statPoints\":4,\"energy\":1,\"health\":2,\"food\":3,\"strength\":4,\"defense\":5,"
            + "\"wisdom\":6,\"luck\":7,\"agility\":8,\"resilience\":9}";

        PlayerStats stats = client.getStats(player).get().orElseThrow();
        assertEquals("/v1/players/" + player + "/stats", onlyRequest().path());
        assertEquals(4, stats.getStatPoints());
        assertEquals(1, stats.getEnergy());
        assertEquals(9, stats.getResilience());
    }

    @Test
    void statOperationsRejectUnknownKeysWithoutHttp() throws Exception {
        assertEquals(0, client.getStat(UUID.randomUUID(), "energy; drop").get());
        client.addStat(UUID.randomUUID(), "1=1", 3).get();
        assertTrue(requests.isEmpty());
    }

    @Test
    void addStatPostsAmount() throws Exception {
        UUID player = UUID.randomUUID();
        responseStatus = 204;
        responseBody = "";
        client.addStat(player, "luck", 3).get();

        CapturedRequest request = onlyRequest();
        assertEquals("POST", request.method());
        assertEquals("/v1/players/" + player + "/stats/luck/add", request.path());
        assertEquals(3, JsonParser.parseString(request.body()).getAsJsonObject().get("amount").getAsInt());
    }

    @Test
    void activeProfessionRoundTrip() throws Exception {
        UUID player = UUID.randomUUID();
        responseBody = "{\"professionCode\":\"miner\"}";
        assertEquals(Optional.of("miner"), client.getActiveProfession(player).get());
        assertEquals("/v1/players/" + player + "/profession", onlyRequest().path());

        requests.clear();
        responseStatus = 204;
        responseBody = "";
        client.setActiveProfession(player, "fighter").get();
        CapturedRequest request = onlyRequest();
        assertEquals("PUT", request.method());
        assertEquals("fighter", JsonParser.parseString(request.body()).getAsJsonObject().get("professionCode").getAsString());
    }

    @Test
    void activeProfessionMissingIsEmpty() throws Exception {
        responseStatus = 404;
        responseBody = "{\"error\":\"Not found\"}";
        assertTrue(client.getActiveProfession(UUID.randomUUID()).get().isEmpty());
    }

    @Test
    void progressRoundTrip() throws Exception {
        UUID player = UUID.randomUUID();
        responseBody = "{\"level\":5,\"exp\":12}";
        ProfessionProgress progress = client.getProgress(player, "miner").get().orElseThrow();
        assertEquals(5, progress.getLevel());
        assertEquals(12, progress.getExp());
        assertEquals("/v1/players/" + player + "/professions/miner/progress", onlyRequest().path());

        requests.clear();
        responseStatus = 204;
        responseBody = "";
        client.updateProgress(player, "miner", new ProfessionProgressRecord(6, 0)).get();
        CapturedRequest request = onlyRequest();
        assertEquals("PUT", request.method());
        assertEquals(6, JsonParser.parseString(request.body()).getAsJsonObject().get("level").getAsInt());
    }

    @Test
    void logActionPostsAuditEntry() throws Exception {
        UUID player = UUID.randomUUID();
        responseStatus = 201;
        client.logAction(player, "miner", ActionType.EQUIP).get();

        CapturedRequest request = onlyRequest();
        assertEquals("/v1/profession-logs", request.path());
        JsonObject body = JsonParser.parseString(request.body()).getAsJsonObject();
        assertEquals(player.toString(), body.get("playerUuid").getAsString());
        assertEquals("miner", body.get("professionCode").getAsString());
        assertEquals("EQUIP", body.get("actionType").getAsString());
    }

    @Test
    void pingHealthReportsServerState() throws Exception {
        responseBody = "{\"status\":\"ok\"}";
        assertTrue(client.pingHealth().get());
        assertEquals("/health", onlyRequest().path());

        responseStatus = 503;
        assertEquals(false, client.pingHealth().get());
    }

    @Test
    void pingHealthResolvesFalseWhenServerUnreachable() throws Exception {
        MCIdentityCentralServer offline = new MCIdentityCentralServer("http://127.0.0.1:1", "secret-token", 1);
        assertEquals(false, offline.pingHealth().get());
        offline.shutdown();
    }

    /**
     * Builds stats where every field carries the given value.
     * @param value The value for the stat points and all nine stats.
     * @return The stats.
     */
    private static PlayerStatsRecord statsOf(int value) {
        return new PlayerStatsRecord(value, value, value, value, value, value, value, value, value, value);
    }
}
