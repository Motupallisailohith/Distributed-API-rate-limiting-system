package com.Motupallisailohith.ratelimit.server;

import com.Motupallisailohith.ratelimit.bucket.RateLimiter;
import com.Motupallisailohith.ratelimit.reliability.ReliabilityModule;
import com.Motupallisailohith.ratelimit.security.JwtException;
import com.Motupallisailohith.ratelimit.security.JwtVerifier;
import com.Motupallisailohith.ratelimit.security.JwtVerifier.JwtClaims;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTPServer exposes a simple API endpoint for rate-limit testing,
 * with pluggable RateLimiter and JWT-based authentication.
 */
public class HTTPServer {
    private static final Logger logger = Logger.getLogger(HTTPServer.class.getName());

    private final HttpServer server;
    private final RateLimiter limiter;
    private final ReliabilityModule reliability;
    private final JwtVerifier jwtVerifier;

    /**
     * @param port        HTTP port to bind
     * @param limiter     pluggable RateLimiter (TokenBucket, LeakyBucket, SlidingWindow, etc.)
     * @param reliability UDP sync layer
     * @param jwtVerifier JWT verifier against your JWKS
     */
    public HTTPServer(int port,
                      RateLimiter limiter,
                      ReliabilityModule reliability,
                      JwtVerifier jwtVerifier) throws Exception {

        this.server       = HttpServer.create(new InetSocketAddress(port), 0);
        this.limiter      = limiter;
        this.reliability  = reliability;
        this.jwtVerifier  = jwtVerifier;

        server.createContext("/api/data", new RateLimitHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
    }

    public void start() {
        server.start();
        logger.info("HTTP server started on port " + server.getAddress().getPort());
    }

    private class RateLimitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                // 1) Verify JWT
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    exchange.sendResponseHeaders(401, -1);
                    return;
                }
                String token = authHeader.substring(7);
                String apiKey;
                try {
                    JwtClaims claims = jwtVerifier.verify(token);
                    apiKey = claims.getSubject();
                } catch (JwtException ex) {
                    // invalid or expired JWT
                    exchange.sendResponseHeaders(401, -1);
                    return;
                }

                // 2) Rate-limit check
                int bucketId = apiKey.hashCode();
                boolean allowed = limiter.allow(bucketId, 1);
                logger.fine(String.format(
                    "[HTTP] key=%s id=%d allowed=%s",
                    apiKey, bucketId, allowed));

                // 3) Broadcast delta & respond
                String response;
                int    status;
                if (allowed) {
                    reliability.sendDelta(bucketId, 1);
                    status   = 200;
                    response = "OK";
                } else {
                    status   = 429;
                    response = "Too Many Requests";
                }

                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error handling HTTP request", e);
                try { exchange.sendResponseHeaders(500, 0); } catch (Exception ignored) {}
            } finally {
                exchange.close();
            }
        }
    }
}
