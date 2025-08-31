package com.Motupallisailohith.ratelimit;

import com.Motupallisailohith.ratelimit.bucket.*;
import com.Motupallisailohith.ratelimit.reliability.ReliabilityModule;
import com.Motupallisailohith.ratelimit.security.JwtVerifier;
import com.Motupallisailohith.ratelimit.server.HTTPServer;
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

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        // ─── Configure logging for production ───
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
        for (String arg : args) {
            if (arg.startsWith("--udp.port="))      udpPort   = Integer.parseInt(arg.split("=")[1]);
            else if (arg.startsWith("--http.port=")) httpPort  = Integer.parseInt(arg.split("=")[1]);
            else if (arg.startsWith("--config="))    configPath = arg.split("=")[1];
            else if (arg.startsWith("--algorithm=")) algorithm  = arg.split("=")[1].toLowerCase();
        }

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

        // 7. JWT Verifier (point to your JWKs URL)
        URL jwkUrl         = new URL("http://localhost:8000/jwks.json");
        JwtVerifier jwtVer = new JwtVerifier(jwkUrl);

        // 8. HTTP server
        HTTPServer httpServer = new HTTPServer(
            httpPort,
            limiter,
            reliability,
            jwtVer
        );
        httpServer.start();
        logger.info("HTTP server running on port " + httpPort);

        // 9. Keep alive
        Thread.currentThread().join();
    }
}
