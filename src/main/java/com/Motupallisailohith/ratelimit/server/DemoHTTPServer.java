package com.Motupallisailohith.ratelimit.server;

import com.Motupallisailohith.ratelimit.bucket.RateLimiter;
import com.Motupallisailohith.ratelimit.reliability.ReliabilityModule;
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
 * Demo HTTP Server that works without JWT dependencies
 * Uses simplified authentication for demonstration purposes
 */
public class DemoHTTPServer {
    private static final Logger logger = Logger.getLogger(DemoHTTPServer.class.getName());

    private final HttpServer server;
    private final RateLimiter limiter;
    private final ReliabilityModule reliability;

    public DemoHTTPServer(int port, RateLimiter limiter, ReliabilityModule reliability) throws Exception {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.limiter = limiter;
        this.reliability = reliability;

        server.createContext("/api/data", new RateLimitHandler());
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/metrics", new MetricsHandler());
        server.createContext("/", new WebInterfaceHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
    }

    public void start() {
        server.start();
        logger.info("Demo HTTP server started on port " + server.getAddress().getPort());
    }

    private class RateLimitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                // Simplified authentication for demo
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                String apiKey = "demo-api-key";
                
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    // Extract API key from simplified JWT (just for demo)
                    String token = authHeader.substring(7);
                    apiKey = extractApiKeyFromDemoToken(token);
                    if (apiKey == null) {
                        sendJsonResponse(exchange, 401, "{\"status\":\"error\",\"message\":\"Invalid demo token\"}");
                        return;
                    }
                } else {
                    sendJsonResponse(exchange, 401, "{\"status\":\"error\",\"message\":\"Bearer token required\"}");
                    return;
                }

                // Rate-limit check
                int bucketId = apiKey.hashCode();
                boolean allowed = limiter.allow(bucketId, 1);
                int remaining = limiter.getRemainingTokens(bucketId);
                
                logger.fine(String.format(
                    "[HTTP] key=%s id=%d allowed=%s remaining=%d",
                    apiKey, bucketId, allowed, remaining));

                // Broadcast delta & respond
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
                    "{\"algorithm\":\"%s\",\"status\":\"active\",\"port\":%d,\"timestamp\":%d,\"mode\":\"demo\"}",
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

                long uptime = System.currentTimeMillis();
                String response = String.format(
                    "{\"uptime\":%d,\"algorithm\":\"%s\",\"port\":%d,\"status\":\"healthy\",\"mode\":\"demo\"}",
                    uptime, limiter.getClass().getSimpleName(), server.getAddress().getPort()
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
            "<html><head><title>Distributed Rate Limiter - Demo</title></head>" +
            "<body style='font-family:Arial,sans-serif;margin:40px;background:#f5f5f5;'>" +
            "<h1>🔥 Distributed Rate Limiter Dashboard (Demo Mode)</h1>" +
            "<p>Backend is running on port <strong>" + port + "</strong></p>" +
            "<p>Algorithm: <strong>" + limiter.getClass().getSimpleName() + "</strong></p>" +
            "<p>Mode: <strong>Demo (Simplified JWT)</strong></p>" +
            "<div style='background:#e8f5e8;padding:15px;border-radius:5px;margin:20px 0;'>" +
            "<h3>🎯 Quick Test</h3>" +
            "<p>API Endpoint: <code>POST /api/data</code></p>" +
            "<p>Status: <code>GET /api/status</code></p>" +
            "<p>Metrics: <code>GET /api/metrics</code></p>" +
            "<p><strong>Frontend Dashboard:</strong> <a href='http://localhost:3000' target='_blank'>http://localhost:3000</a></p>" +
            "</div>" +
            "<p><em>This demo version works without external JWT dependencies.</em></p>" +
            "<p><em>For production JWT support, ensure nimbus-jose-jwt library is available.</em></p>" +
            "</body></html>";
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

    private String extractApiKeyFromDemoToken(String token) {
        try {
            // Simplified token parsing for demo
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                // For demo purposes, just extract a simple API key
                return "demo-api-key-" + parts[1].substring(0, Math.min(8, parts[1].length()));
            }
        } catch (Exception e) {
            // Invalid token format
        }
        return null;
    }
}
