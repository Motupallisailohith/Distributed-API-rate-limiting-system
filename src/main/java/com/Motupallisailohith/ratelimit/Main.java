package com.Motupallisailohith.ratelimit;

import com.Motupallisailohith.ratelimit.bucket.*;
import com.Motupallisailohith.ratelimit.reliability.ReliabilityModule;
import com.Motupallisailohith.ratelimit.server.UDPListener;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified Main class that automatically detects JWT availability and chooses appropriate server
 * Consolidates Main.java, DemoMain.java, and SimpleMain.java into one adaptive implementation
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        // ─── Configure logging ───
        Logger root = Logger.getLogger("");
        String logLevel = System.getenv().getOrDefault("LOG_LEVEL", "INFO");
        root.setLevel(Level.parse(logLevel));
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.parse(logLevel));
        }

        // 1. Parse arguments
        int    udpPort     = 7000;
        int    httpPort    = 8080;
        String configPath  = "config/nodes.yml";
        String algorithm   = "tokenbucket"; // default
        boolean demoMode   = false;
        
        for (String arg : args) {
            if (arg.startsWith("--udp.port="))      udpPort   = Integer.parseInt(arg.split("=")[1]);
            else if (arg.startsWith("--http.port=")) httpPort  = Integer.parseInt(arg.split("=")[1]);
            else if (arg.startsWith("--config="))    configPath = arg.split("=")[1];
            else if (arg.startsWith("--algorithm=")) algorithm  = arg.split("=")[1].toLowerCase();
            else if (arg.equals("--demo"))           demoMode  = true;
        }

        // Detect JWT availability
        boolean jwtAvailable = isJwtAvailable();
        if (demoMode || !jwtAvailable) {
            logger.info("=== DISTRIBUTED RATE LIMITER (DEMO MODE) ===");
            logger.info("JWT dependencies not available - using demo authentication");
        } else {
            logger.info("=== DISTRIBUTED RATE LIMITER (PRODUCTION MODE) ===");
            logger.info("JWT authentication enabled");
        }
        
        logger.info("Algorithm: " + algorithm);
        logger.info("HTTP Port: " + httpPort);
        logger.info("UDP Port: " + udpPort);

        // 2. Load and parse nodes.yml (host:port)
        List<String> rawLines;
        try {
            rawLines = Files.readAllLines(Paths.get(configPath));
        } catch (Exception e) {
            logger.warning("Cannot read config at " + configPath + ", using localhost:" + udpPort);
            rawLines = new ArrayList<>();
            rawLines.add("127.0.0.1:" + udpPort);
        }
        List<InetAddress> peerAddrs = new ArrayList<>();
        List<Integer>    peerPorts = new ArrayList<>();
        for (String line : rawLines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split(":");
            if (parts.length == 2) {
                peerAddrs.add(InetAddress.getByName(parts[0]));
                peerPorts.add(Integer.parseInt(parts[1]));
            }
        }
        InetAddress[] peers = peerAddrs.toArray(new InetAddress[0]);
        int[]         ports = peerPorts.stream().mapToInt(i -> i).toArray();

        // 3. Instantiate the chosen RateLimiter
        RateLimiter limiter;
        switch (algorithm) {
            case "leakybucket":
                limiter = new LeakyBucketRateLimiter(
                    /*capacity=*/100,
                    /*leakIntervalMs=*/1_000L
                );
                break;
            case "slidingwindow":
                limiter = new SlidingWindowRateLimiter(
                    /*maxRequests=*/100,
                    /*windowMs=*/60_000L
                );
                break;
            case "tokenbucket":
            default:
                limiter = new TokenBucketRateLimiter(
                    /*capacity=*/100,
                    /*refillIntervalMs=*/60_000L
                );
                break;
        }
        logger.info("Using rate-limiter algorithm: " + limiter.getClass().getSimpleName());

        // 4. Shared UDP socket
        DatagramSocket sendSocket = new DatagramSocket();

        // 5. ReliabilityModule wires UDP + limiter
        ReliabilityModule reliability = new ReliabilityModule(
        limiter,       // your RateLimiter implementation
        sendSocket,    // shared UDP socket for sending
        peers,         // peer addresses
        ports          // peer UDP ports
    );

        // 6. Start UDP listener
        UDPListener udpListener = new UDPListener(udpPort, reliability, /*threads=*/4);
        udpListener.start();
        logger.info("UDP listener running on port " + udpPort);

        // 7. Start appropriate HTTP server based on JWT availability
        if (demoMode || !jwtAvailable) {
            // Use demo server without JWT dependencies
            startDemoServer(httpPort, limiter, reliability);
        } else {
            // Use production server with JWT
            startProductionServer(httpPort, limiter, reliability);
        }

        // 8. Keep alive
        Thread.currentThread().join();
    }

    /**
     * Check if JWT dependencies are available at runtime
     */
    private static boolean isJwtAvailable() {
        try {
            Class.forName("com.Motupallisailohith.ratelimit.security.JwtVerifier");
            Class.forName("com.nimbusds.jose.JWSVerifier");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Start production server with JWT authentication
     */
    private static void startProductionServer(int httpPort, RateLimiter limiter, ReliabilityModule reliability) throws Exception {
        try {
            // Use reflection to avoid compile-time dependency on JWT classes
            Class<?> jwtVerifierClass = Class.forName("com.Motupallisailohith.ratelimit.security.JwtVerifier");
            Class<?> httpServerClass = Class.forName("com.Motupallisailohith.ratelimit.server.HTTPServer");
            
            URL jwkUrl = new URL("http://localhost:8000/jwks.json");
            Object jwtVerifier = jwtVerifierClass.getConstructor(URL.class).newInstance(jwkUrl);
            
            Object httpServer = httpServerClass.getConstructor(
                int.class, 
                RateLimiter.class, 
                ReliabilityModule.class, 
                jwtVerifierClass
            ).newInstance(httpPort, limiter, reliability, jwtVerifier);
            
            httpServerClass.getMethod("start").invoke(httpServer);
            logger.info("Production HTTP server with JWT running on port " + httpPort);
        } catch (Exception e) {
            logger.warning("Failed to start production server, falling back to demo mode: " + e.getMessage());
            startDemoServer(httpPort, limiter, reliability);
        }
    }

    /**
     * Start demo server without JWT dependencies
     */
    private static void startDemoServer(int httpPort, RateLimiter limiter, ReliabilityModule reliability) throws Exception {
        try {
            Class<?> demoServerClass = Class.forName("com.Motupallisailohith.ratelimit.server.DemoHTTPServer");
            Object demoServer = demoServerClass.getConstructor(
                int.class, 
                RateLimiter.class, 
                ReliabilityModule.class
            ).newInstance(httpPort, limiter, reliability);
            
            demoServerClass.getMethod("start").invoke(demoServer);
            logger.info("Demo HTTP server running on port " + httpPort);
        } catch (Exception e) {
            logger.severe("Failed to start demo server: " + e.getMessage());
            throw e;
        }
    }
}
