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
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/metrics", new MetricsHandler());
        server.createContext("/api/algorithm", new AlgorithmHandler());
        server.createContext("/", new WebInterfaceHandler());
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
                // Add CORS headers
                addCorsHeaders(exchange);
                
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                // 1) Verify JWT
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJsonResponse(exchange, 401, "{\"status\":\"error\",\"message\":\"JWT token required\"}");
                    return;
                }
                String token = authHeader.substring(7);
                String apiKey;
                try {
                    JwtClaims claims = jwtVerifier.verify(token);
                    apiKey = claims.getSubject();
                } catch (JwtException ex) {
                    sendJsonResponse(exchange, 401, "{\"status\":\"error\",\"message\":\"Invalid JWT token\"}");
                    return;
                }

                // 2) Rate-limit check
                int bucketId = apiKey.hashCode();
                boolean allowed = limiter.allow(bucketId, 1);
                int remaining = limiter.getRemainingTokens(bucketId);
                
                logger.fine(String.format(
                    "[HTTP] key=%s id=%d allowed=%s remaining=%d",
                    apiKey, bucketId, allowed, remaining));

                // 3) Broadcast delta & respond
                if (allowed) {
                    reliability.sendDelta(bucketId, 1);
                    String response = String.format(
                        "{\"status\":\"allowed\",\"algorithm\":\"%s\",\"remaining\":%d,\"apiKey\":\"%s\",\"timestamp\":%d}",
                        limiter.getClass().getSimpleName(), remaining, apiKey, System.currentTimeMillis()
                    );
                    sendJsonResponse(exchange, 200, response);
                } else {
                    String response = String.format(
                        "{\"status\":\"blocked\",\"reason\":\"rate_limit_exceeded\",\"algorithm\":\"%s\",\"remaining\":%d,\"timestamp\":%d}",
                        limiter.getClass().getSimpleName(), remaining, System.currentTimeMillis()
                    );
                    sendJsonResponse(exchange, 429, response);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error handling HTTP request", e);
                try { 
                    sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"Internal server error\"}");
                } catch (Exception ignored) {}
            } finally {
                exchange.close();
            }
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                String response = String.format(
                    "{\"algorithm\":\"%s\",\"status\":\"active\",\"port\":%d,\"timestamp\":%d}",
                    limiter.getClass().getSimpleName(),
                    server.getAddress().getPort(),
                    System.currentTimeMillis()
                );
                sendJsonResponse(exchange, 200, response);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error handling status request", e);
                try { sendJsonResponse(exchange, 500, "{\"status\":\"error\"}"); } catch (Exception ignored) {}
            } finally {
                exchange.close();
            }
        }
    }

    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                // Comprehensive metrics including UDP data that frontend expects
                long uptime = System.currentTimeMillis();
                
                // Get UDP metrics from reliability module if available
                String udpMetrics = "";
                if (reliability != null) {
                    try {
                        udpMetrics = reliability.getUdpMetrics();
                        // Remove the outer braces to embed in our response
                        if (udpMetrics.startsWith("{") && udpMetrics.endsWith("}")) {
                            udpMetrics = udpMetrics.substring(1, udpMetrics.length() - 1);
                        }
                    } catch (Exception e) {
                        logger.warning("Could not get UDP metrics: " + e.getMessage());
                        udpMetrics = "\"packetsSent\":0,\"packetsReceived\":0,\"acksReceived\":0,\"retransmissions\":0,\"deltasApplied\":0,\"pendingPackets\":0";
                    }
                } else {
                    // Fallback UDP metrics for demo
                    udpMetrics = "\"packetsSent\":0,\"packetsReceived\":0,\"acksReceived\":0,\"retransmissions\":0,\"deltasApplied\":0,\"pendingPackets\":0";
                }
                
                // Build comprehensive response with all metrics frontend expects
                String response = String.format(
                    "{\"uptime\":%d,\"algorithm\":\"%s\",\"port\":%d,\"status\":\"healthy\"," +
                    "\"totalRequests\":0,\"allowedRequests\":0,\"blockedRequests\":0," +
                    "\"udpMetrics\":{%s}," +
                    "\"history\":[]}",
                    uptime, 
                    limiter.getClass().getSimpleName(), 
                    server.getAddress().getPort(),
                    udpMetrics
                );
                sendJsonResponse(exchange, 200, response);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error handling metrics request", e);
                try { sendJsonResponse(exchange, 500, "{\"status\":\"error\"}"); } catch (Exception ignored) {}
            } finally {
                exchange.close();
            }
        }
    }

    private class WebInterfaceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                String path = exchange.getRequestURI().getPath();
                
                if (!"/".equals(path)) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                String html = generateWebInterface();
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                
                exchange.getResponseHeaders().add("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error serving web interface", e);
                try { exchange.sendResponseHeaders(500, 0); } catch (Exception ignored) {}
            } finally {
                exchange.close();
            }
        }

        private String generateWebInterface() {
            int port = server.getAddress().getPort();
            return "<!DOCTYPE html>" +
            "<html><head><title>Distributed Rate Limiter</title></head>" +
            "<body style='font-family:Arial,sans-serif;margin:40px;background:#f5f5f5;'>" +
            "<h1>🔥 Distributed Rate Limiter Dashboard</h1>" +
            "<p>Backend is running on port <strong>" + port + "</strong></p>" +
            "<p>Algorithm: <strong>" + limiter.getClass().getSimpleName() + "</strong></p>" +
            "<p>API Endpoint: <code>POST /api/data</code></p>" +
            "<p>Status: <code>GET /api/status</code></p>" +
            "<p>Metrics: <code>GET /api/metrics</code></p>" +
            "<p><em>Frontend dashboard coming soon...</em></p>" +
            "</body></html>";
        }
    }
//hope
    private class AlgorithmHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                if ("GET".equals(exchange.getRequestMethod())) {
                    // Return current algorithm info
                    String response = String.format(
                        "{\"current\":\"%s\",\"available\":[\"tokenbucket\",\"leakybucket\",\"slidingwindow\"]}",
                        limiter.getClass().getSimpleName().toLowerCase().replace("ratelimiter", "")
                    );
                    sendJsonResponse(exchange, 200, response);
                } else if ("POST".equals(exchange.getRequestMethod())) {
                    // Switch algorithm (for demo purposes, just return success)
                    // In a real system, you'd implement algorithm switching logic
                    String response = "{\"status\":\"success\",\"message\":\"Algorithm switch requested\"}";
                    sendJsonResponse(exchange, 200, response);
                } else {
                    exchange.sendResponseHeaders(405, -1); // Method not allowed
                }
            } catch (Exception e) {
                logger.severe("Error in AlgorithmHandler: " + e.getMessage());
                try {
                    sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"Internal server error\"}");
                } catch (Exception ex) {
                    logger.severe("Failed to send error response: " + ex.getMessage());
                }
            }
        }
    }

    // Helper methods
    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws Exception {
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
