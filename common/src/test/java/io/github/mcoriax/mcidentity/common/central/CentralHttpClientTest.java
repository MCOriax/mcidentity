package io.github.mcoriax.mcidentity.common.central;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link CentralHttpClient}, the transport every plugin shares,
 * against a stub HTTP server: the authentication header, the failure
 * semantics of reads and writes, and the health probes.
 */
class CentralHttpClientTest {

    /**
     * The stub server standing in for the central server.
     */
    private HttpServer server;

    /**
     * Authorization headers received, in arrival order.
     */
    private final ConcurrentLinkedQueue<String> auths = new ConcurrentLinkedQueue<>();

    /**
     * Client under test, pointed at the stub server.
     */
    private CentralHttpClient client;

    /**
     * Starts a stub that answers 200 with a small JSON object on {@code /ok},
     * 404 on {@code /missing}, 500 on {@code /broken} and 200 on {@code /health}.
     * @throws IOException If the stub cannot bind.
     */
    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String header = exchange.getRequestHeaders().getFirst("Authorization");
            if (header != null) {
                auths.add(header);
            }
            String path = exchange.getRequestURI().getPath();
            int status = switch (path) {
                case "/ok", "/health" -> 200;
                case "/missing" -> 404;
                default -> 500;
            };
            byte[] body = "{\"value\":7}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        client = new CentralHttpClient("http://127.0.0.1:" + server.getAddress().getPort() + "/", "secret", 5);
    }

    /**
     * Stops the stub and releases the client.
     */
    @AfterEach
    void tearDown() {
        client.close();
        server.stop(0);
    }

    /**
     * A successful read parses the payload and sends the bearer token.
     * @throws Exception On a transport failure.
     */
    @Test
    void readParsesPayloadAndAuthenticates() throws Exception {
        int value = client.<Integer>read("/ok", 0, json -> json.getAsJsonObject().get("value").getAsInt()).get();
        assertEquals(7, value);
        assertEquals("Bearer secret", auths.peek());
    }

    /**
     * Reads fall back on 404, on other failures, and on transport errors.
     * @throws Exception On a transport failure.
     */
    @Test
    void readFallsBackOnFailure() throws Exception {
        assertEquals(-1, client.<Integer>read("/missing", -1, json -> 1).get());
        assertEquals(-1, client.<Integer>read("/broken", -1, json -> 1).get());
        server.stop(0);
        assertEquals(-1, client.<Integer>read("/ok", -1, json -> 1).get());
    }

    /**
     * Writes complete normally even when the server rejects them.
     * @throws Exception On a transport failure.
     */
    @Test
    void writeNeverFailsTheFuture() throws Exception {
        JsonObject body = new JsonObject();
        assertNull(client.write("POST", "/broken", body).get());
        assertEquals(1, auths.size());
        server.stop(0);
        assertDoesNotThrow(() -> client.write("POST", "/ok", body).get());
    }

    /**
     * The raw send surfaces the status to the caller.
     * @throws Exception On a transport failure.
     */
    @Test
    void sendReturnsTheRawResponse() throws Exception {
        assertEquals(500, client.send("PUT", "/broken", new JsonObject()).get().statusCode());
    }

    /**
     * The strict probe fails on a bad status, the lenient one answers false.
     * @throws Exception On a transport failure.
     */
    @Test
    void healthProbes() throws Exception {
        client.checkHealth().get();
        assertTrue(client.pingHealth().get());
        server.stop(0);
        ExecutionException failure = assertThrows(ExecutionException.class, () -> client.checkHealth().get());
        assertNotNull(failure.getCause());
        assertFalse(client.pingHealth().get());
    }
}
