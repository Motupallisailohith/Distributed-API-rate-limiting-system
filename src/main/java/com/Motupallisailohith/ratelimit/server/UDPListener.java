package com.Motupallisailohith.ratelimit.server;

import com.Motupallisailohith.ratelimit.protocol.Packet;
import com.Motupallisailohith.ratelimit.reliability.ReliabilityModule;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UDPListener binds to a port and dispatches incoming packets to the ReliabilityModule.
 */
public class UDPListener {
    private static final Logger logger = Logger.getLogger(UDPListener.class.getName());
    private final int port;
    private final ReliabilityModule reliabilityModule;
    private final ExecutorService workerPool;

    public UDPListener(int port, ReliabilityModule reliabilityModule, int workerThreads) {
        this.port = port;
        this.reliabilityModule = reliabilityModule;
        this.workerPool = Executors.newFixedThreadPool(workerThreads);
    }

    public void start() {
        Thread listenerThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(null)) {
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(port));
                logger.info("UDPListener started on port " + port);
                byte[] buffer = new byte[1024];
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                    socket.receive(dp);
                    // Hand off to worker pool
                    workerPool.submit(() -> handlePacket(dp, socket));
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in UDPListener", e);
            }
        }, "udp-listener-thread");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handlePacket(DatagramPacket dp, DatagramSocket socket) {
        try {
            byte[] data = new byte[dp.getLength()];
            System.arraycopy(dp.getData(), dp.getOffset(), data, 0, dp.getLength());

            Packet pkt = Packet.fromBytes(data);
            logger.fine("Received packet: " + pkt + " from " + dp.getSocketAddress());

            // Delegate to ReliabilityModule
            reliabilityModule.handleIncoming(pkt, dp.getAddress(), dp.getPort(), socket);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to process incoming packet", ex);
        }
    }
}
