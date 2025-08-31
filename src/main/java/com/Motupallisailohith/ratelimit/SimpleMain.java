package com.Motupallisailohith.ratelimit;

import com.Motupallisailohith.ratelimit.bucket.*;
import com.Motupallisailohith.ratelimit.reliability.ReliabilityModule;
import com.Motupallisailohith.ratelimit.server.UDPListener;
import com.Motupallisailohith.ratelimit.server.SimpleHTTPServer;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple Main class that works without JWT dependencies
 */
public class SimpleMain {
    private static final Logger logger = Logger.getLogger(SimpleMain.class.getName());

    public static void main(String[] args) throws Exception {
        // Enable INFO logging
        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.INFO);
        }

        // 1. Parse arguments
        int    udpPort     = 7000;
        int    httpPort    = 8090;  // Use 8090 by default to avoid conflicts
        String configPath  = "config/nodes.yml";
        String algorithm   = "tokenbucket"; // default
        for (String arg : args) {
            if (arg.startsWith("--udp.port="))      udpPort   = Integer.parseInt(arg.split("=")[1]);
            else if (arg.startsWith("--http.port=")) httpPort  = Integer.parseInt(arg.split("=")[1]);
            else if (arg.startsWith("--config="))    configPath = arg.split("=")[1];
            else if (arg.startsWith("--algorithm=")) algorithm  = arg.split("=")[1].toLowerCase();
        }

        logger.info("=== DISTRIBUTED RATE LIMITER (SIMPLE MODE) ===");
        logger.info("Algorithm: " + algorithm);
        logger.info("HTTP Port: " + httpPort);
        logger.info("UDP Port: " + udpPort);

        // 2. Load and parse nodes.yml (host:port)
        List<String> rawLines;
        try {
            rawLines = Files.readAllLines(Paths.get(configPath));
        } catch (Exception e) {
            logger.warning("Cannot read config at " + configPath + ", using localhost:" + udpPort);
            rawLines = new ArrayList<String>();
            rawLines.add("127.0.0.1:" + udpPort);
        }
        List<InetAddress> peerAddrs = new ArrayList<InetAddress>();
        List<Integer>    peerPorts = new ArrayList<Integer>();
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
        int[]         ports = new int[peerPorts.size()];
        for (int i = 0; i < peerPorts.size(); i++) {
            ports[i] = peerPorts.get(i);
        }

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

        // 7. Simple HTTP server (without JWT dependencies)
        SimpleHTTPServer httpServer = new SimpleHTTPServer(
            httpPort,
            limiter,
            reliability
        );
        httpServer.start();
        logger.info("Simple HTTP server running on port " + httpPort);
        logger.info("Web interface: http://localhost:" + httpPort);
        logger.info("API endpoint: http://localhost:" + httpPort + "/api/data");

        // 8. Keep alive
        Thread.currentThread().join();
    }
}

