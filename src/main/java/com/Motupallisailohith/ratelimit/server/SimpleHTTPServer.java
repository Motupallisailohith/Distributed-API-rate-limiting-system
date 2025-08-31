package com.Motupallisailohith.ratelimit.server;

import com.Motupallisailohith.ratelimit.bucket.RateLimiter;
import com.Motupallisailohith.ratelimit.bucket.TokenBucketRateLimiter;
import com.Motupallisailohith.ratelimit.bucket.LeakyBucketRateLimiter;
import com.Motupallisailohith.ratelimit.bucket.SlidingWindowRateLimiter;
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
 * Simplified HTTP Server that works without JWT dependencies
 */
public class SimpleHTTPServer {
    private static final Logger logger = Logger.getLogger(SimpleHTTPServer.class.getName());

    private final HttpServer server;
    private volatile RateLimiter limiter;  // Made volatile for thread-safe switching
    private final ReliabilityModule reliability;

    public SimpleHTTPServer(int port, RateLimiter limiter, ReliabilityModule reliability) throws Exception {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.limiter = limiter;
        this.reliability = reliability;

        server.createContext("/api/data", new RateLimitHandler());
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/metrics", new MetricsHandler());
        server.createContext("/api/algorithm", new AlgorithmHandler());
        server.createContext("/", new WebInterfaceHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
    }

    public void start() {
        server.start();
        logger.info("Simple HTTP server started on port " + server.getAddress().getPort());
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

                // Simplified authentication - use API key from header or default
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                String apiKey = "demo-api-key";
                
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    // Extract API key from token (simplified for demo)
                    String token = authHeader.substring(7);
                    apiKey = "demo-" + token.substring(0, Math.min(8, token.length()));
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
                    "{\"algorithm\":\"%s\",\"status\":\"active\",\"port\":%d,\"timestamp\":%d,\"mode\":\"simple\"}",
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
                
                // Get UDP metrics from ReliabilityModule
                String udpMetrics = reliability.getUdpMetrics();
                
                // Generate sample historical data for charts (in a real system, you'd store this)
                StringBuilder historyJson = new StringBuilder("[");
                for (int i = 0; i < 10; i++) {
                    if (i > 0) historyJson.append(",");
                    int totalReqs = (int)(Math.random() * 100) + 50;
                    int allowedReqs = (int)(totalReqs * 0.7);
                    int blockedReqs = totalReqs - allowedReqs;
                    historyJson.append(String.format(
                        "{\"totalRequests\":%d,\"allowedRequests\":%d,\"blockedRequests\":%d}",
                        totalReqs, allowedReqs, blockedReqs
                    ));
                }
                historyJson.append("]");
                
                // Build comprehensive response with all data frontend expects
                String response = String.format(
                    "{\"uptime\":%d,\"algorithm\":\"%s\",\"port\":%d,\"status\":\"healthy\"," +
                    "\"totalRequests\":%d,\"allowedRequests\":%d,\"blockedRequests\":%d," +
                    "\"udpMetrics\":%s," +
                    "\"history\":%s}",
                    uptime, 
                    limiter.getClass().getSimpleName(), 
                    server.getAddress().getPort(),
                    (int)(Math.random() * 1000) + 100,  // Sample current totals
                    (int)(Math.random() * 700) + 70,    // Sample current allowed
                    (int)(Math.random() * 300) + 30,    // Sample current blocked
                    udpMetrics,
                    historyJson.toString()
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

    private class AlgorithmHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                String method = exchange.getRequestMethod();
                
                if ("GET".equals(method)) {
                    // Return current algorithm and available options
                    String response = String.format(
                        "{\"current\":\"%s\",\"available\":[\"tokenbucket\",\"leakybucket\",\"slidingwindow\"],\"timestamp\":%d}",
                        getCurrentAlgorithmName(), System.currentTimeMillis()
                    );
                    sendJsonResponse(exchange, 200, response);
                    
                } else if ("POST".equals(method)) {
                    // Switch algorithm
                    String body = readRequestBody(exchange);
                    String newAlgorithm = extractAlgorithmFromJson(body);
                    
                    if (newAlgorithm == null) {
                        sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Invalid algorithm specified\"}");
                        return;
                    }
                    
                    boolean success = switchAlgorithm(newAlgorithm);
                    if (success) {
                        String response = String.format(
                            "{\"status\":\"success\",\"algorithm\":\"%s\",\"message\":\"Algorithm switched successfully\",\"timestamp\":%d}",
                            getCurrentAlgorithmName(), System.currentTimeMillis()
                        );
                        sendJsonResponse(exchange, 200, response);
                        logger.info("Algorithm switched to: " + getCurrentAlgorithmName());
                    } else {
                        sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Unknown algorithm\"}");
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1); // Method not allowed
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error handling algorithm request", e);
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
            "<html><head><title>Distributed Rate Limiter - Simple Mode</title></head>" +
            "<body style='font-family:Arial,sans-serif;margin:40px;background:#f5f5f5;'>" +
            "<h1>🔥 Distributed Rate Limiter Dashboard (Simple Mode)</h1>" +
            "<p>Backend is running on port <strong>" + port + "</strong></p>" +
            "<p>Algorithm: <strong>" + limiter.getClass().getSimpleName() + "</strong></p>" +
            "<p>Mode: <strong>Simple (No JWT Dependencies)</strong></p>" +
            "<div style='background:#e8f5e8;padding:15px;border-radius:5px;margin:20px 0;'>" +
            "<h3>🎯 Quick Test</h3>" +
            "<p>API Endpoint: <code>POST /api/data</code></p>" +
            "<p>Status: <code>GET /api/status</code></p>" +
            "<p>Metrics: <code>GET /api/metrics</code></p>" +
            "<p><strong>Frontend Dashboard:</strong> <a href='http://localhost:3000' target='_blank'>http://localhost:3000</a></p>" +
            "</div>" +
            "<p><em>This simple version works without external JWT dependencies.</em></p>" +
            "<p><em>Authentication uses simplified Bearer tokens for demo purposes.</em></p>" +
            "</body></html>";
        }
    }

    // Algorithm switching helper methods
    private String getCurrentAlgorithmName() {
        String className = limiter.getClass().getSimpleName();
        if (className.equals("TokenBucketRateLimiter")) return "tokenbucket";
        if (className.equals("LeakyBucketRateLimiter")) return "leakybucket";
        if (className.equals("SlidingWindowRateLimiter")) return "slidingwindow";
        return "unknown";
    }
    
    private boolean switchAlgorithm(String algorithmName) {
        try {
            RateLimiter newLimiter;
            switch (algorithmName.toLowerCase()) {
                case "tokenbucket":
                    newLimiter = new TokenBucketRateLimiter(100, 60_000L);
                    break;
                case "leakybucket":
                    newLimiter = new LeakyBucketRateLimiter(100, 1_000L);
                    break;
                case "slidingwindow":
                    newLimiter = new SlidingWindowRateLimiter(100, 60_000L);
                    break;
                default:
                    return false;
            }
            
            // Update the reliability module with the new limiter
            reliability.updateRateLimiter(newLimiter);
            
            // Switch the limiter (thread-safe due to volatile)
            this.limiter = newLimiter;
            
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error switching algorithm", e);
            return false;
        }
    }
    
    private String readRequestBody(HttpExchange exchange) throws Exception {
        StringBuilder body = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }
    
    private String extractAlgorithmFromJson(String json) {
        try {
            // Simple JSON parsing for {"algorithm": "tokenbucket"}
            if (json.contains("\"algorithm\"")) {
                int start = json.indexOf("\"algorithm\"");
                int colonIndex = json.indexOf(":", start);
                int valueStart = json.indexOf("\"", colonIndex) + 1;
                int valueEnd = json.indexOf("\"", valueStart);
                if (valueStart > 0 && valueEnd > valueStart) {
                    return json.substring(valueStart, valueEnd);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error parsing algorithm JSON", e);
        }
        return null;
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
