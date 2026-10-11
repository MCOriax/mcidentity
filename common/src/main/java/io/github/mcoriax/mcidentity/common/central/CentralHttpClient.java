package io.github.mcoriax.mcidentity.common.central;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The transport every MCIdentity plugin uses to talk to the central REST server.
 *
 * <p>Owns one pooled {@link HttpClient}, the base URL, the bearer token and the
 * request timeout, and exposes the three call shapes the protocol needs:
 * {@link #send raw requests}, {@link #write fire-and-log writes} and
 * {@link #read reads with a neutral fallback}. The core plugin's
 * {@link MCIdentityCentralServer} and the add-on plugins' clients all build on
 * it, so the authentication header, the no-redirect rule and the failure
 * semantics exist in exactly one place.</p>
 *
 * <p>Failure semantics: writes complete normally after logging, and reads
 * resolve to their fallback on 404, any other non-2xx status or a transport
 * failure. Only {@link #send} and {@link #checkHealth()} surface failures to
 * the caller.</p>
 */
public class CentralHttpClient {

    /**
     * Logger for transport and protocol failures.
     */
    private static final Logger LOGGER = Logger.getLogger(CentralHttpClient.class.getName());

    /**
     * JSON codec configured to keep explicit nulls, which the API uses to
     * clear sessions and optional fields.
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
    public CentralHttpClient(String baseUrl, String token, int timeoutSeconds) {
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

    /**
     * Sends one authenticated JSON request to the central server.
     * @param method The HTTP method.
     * @param path The API path relative to the base URL.
     * @param body The JSON body, or null for bodiless requests.
     * @return A future with the raw HTTP response.
     */
    public CompletableFuture<HttpResponse<String>> send(String method, String path, JsonObject body) {
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
    public CompletableFuture<Void> write(String method, String path, JsonObject body) {
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
    public <T> CompletableFuture<T> read(String path, Object fallback, Function<JsonElement, T> parser) {
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
     * Checks the unversioned liveness probe. The returned future completes
     * exceptionally on any status other than 200 or on a transport failure.
     * @return A future that completes normally when the server is healthy.
     */
    public CompletableFuture<Void> checkHealth() {
        return client.sendAsync(healthRequest(), HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("Central server health check failed with status " + response.statusCode());
                }
            });
    }

    /**
     * Pings the unversioned liveness probe, which also serves as a wake-up
     * signal for cold-started deployments. The returned future always
     * completes: it resolves to {@code true} on a 200 response and to
     * {@code false} on any other status, a transport failure, or a timeout.
     * @return A future resolving to whether the central server is online.
     */
    public CompletableFuture<Boolean> pingHealth() {
        return client.sendAsync(healthRequest(), HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> response.statusCode() == 200)
            .orTimeout(timeout.toSeconds() + 5, TimeUnit.SECONDS)
            .exceptionally(ex -> false);
    }

    /**
     * Releases the connection pool.
     */
    public void close() {
        client.close();
    }

    /**
     * Builds the liveness probe request.
     * @return The unauthenticated GET request for {@code /health}.
     */
    private HttpRequest healthRequest() {
        return HttpRequest.newBuilder(baseUri.resolve(baseUri.getPath() + "/health"))
            .timeout(timeout)
            .GET()
            .build();
    }
}
