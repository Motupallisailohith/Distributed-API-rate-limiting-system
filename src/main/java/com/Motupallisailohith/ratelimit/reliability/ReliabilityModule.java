package com.Motupallisailohith.ratelimit.reliability;

import com.Motupallisailohith.ratelimit.bucket.RateLimiter;
import com.Motupallisailohith.ratelimit.protocol.Packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ReliabilityModule handles incoming packets (ACKs and DELTA_UPDATEs),
 * maintains outgoing buffers, sequence generation, and reliable sends over UDP.
 * It also throttles retransmissions to avoid flooding.
 */
public class ReliabilityModule {
    private static final Logger logger = Logger.getLogger(ReliabilityModule.class.getName());

    // Outgoing buffers: bucketId -> seq -> Packet
    private final ConcurrentMap<Integer, ConcurrentMap<Integer, Packet>> outgoingBuffers = new ConcurrentHashMap<>();

    // Now using the generic RateLimiter interface
    private final RateLimiter limiter;
    private final DatagramSocket socket;
    private final InetAddress[] peers;
    private final int[] peerPorts;
    private final AtomicInteger seqGenerator = new AtomicInteger(0);

    // Retransmission throttle parameters
    private final int throttleCapacity = 50;
    private final long throttleRefillMs = 1_000L;
    private int throttleTokens = throttleCapacity;

    private final ScheduledExecutorService throttleScheduler    = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService retransmitScheduler = Executors.newSingleThreadScheduledExecutor();

    public ReliabilityModule(RateLimiter limiter,
                             DatagramSocket socket,
                             InetAddress[] peers,
                             int[] peerPorts) {
        this.limiter   = limiter;
        this.socket    = socket;
        this.peers     = peers;
        this.peerPorts = peerPorts;

        // Refill throttle tokens once per second
        throttleScheduler.scheduleAtFixedRate(() -> {
            synchronized (this) {
                throttleTokens = throttleCapacity;
            }
            logger.fine("Retransmission throttle refilled");
        }, throttleRefillMs, throttleRefillMs, TimeUnit.MILLISECONDS);

        // Retransmit any un-ACKed packets every 200ms
        retransmitScheduler.scheduleAtFixedRate(this::retransmitPending,
                                                200L, 200L, TimeUnit.MILLISECONDS);
    }

    /** Handle an incoming UDP packet. */
    public void handleIncoming(Packet pkt, InetAddress sender, int port, DatagramSocket sock) {
        try {
            switch (pkt.getType()) {
                case DELTA_UPDATE:
                    handleDelta(pkt, sender, port);
                    break;
                case ACK:
                    handleAck(pkt);
                    break;
                default:
                    logger.warning("Unknown packet type: " + pkt.getType());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing packet: " + pkt, e);
        }
    }

    private void handleDelta(Packet pkt, InetAddress sender, int port) throws Exception {
        // Apply the remote delta to whichever algorithm is in use
        limiter.applyRemoteDelta(pkt.getBucketId(), pkt.getDelta());

        // Debug: log remaining tokens if supported
       int remaining = limiter.getRemainingTokens(pkt.getBucketId());
       if (remaining >= 0) {
           logger.fine(String.format(
              "[UDP] handleDelta: bucket=%d applied delta=%d, remaining=%d",
              pkt.getBucketId(), pkt.getDelta(), remaining
           ));
       } else {
           logger.fine(String.format(
              "[UDP] handleDelta: bucket=%d applied delta=%d",
               pkt.getBucketId(), pkt.getDelta()
           ));
       }
 
      
        // Send back an ACK
        Packet ack = Packet.ack(pkt.getSequence());
        byte[] data = ack.toBytes();
        socket.send(new DatagramPacket(data, data.length, sender, port));
        logger.fine("Sent ACK seq=" + pkt.getSequence());
    }

    private void handleAck(Packet pkt) {
        int seq = pkt.getSequence();
        for (Map.Entry<Integer, ConcurrentMap<Integer, Packet>> entry : outgoingBuffers.entrySet()) {
            if (entry.getValue().remove(seq) != null) {
                logger.fine("ACK received, removed seq=" + seq + " from bucket=" + entry.getKey());
                break;
            }
        }
    }

    /**
     * Called by HTTPServer on each permitted request.
     * Enqueues and immediately broadcasts a DELTA_UPDATE.
     */
    public void sendDelta(int bucketId, int delta) {
        int seq = seqGenerator.incrementAndGet();
        Packet pkt = Packet.deltaUpdate(bucketId, delta, seq);

        outgoingBuffers
            .computeIfAbsent(bucketId, b -> new ConcurrentHashMap<>())
            .put(seq, pkt);

        sendToPeers(pkt);
    }

    /** Send a packet to all peers (excluding self), subject to throttle. */
    private void sendToPeers(Packet pkt) {
        byte[] data    = pkt.toBytes();
        int    selfPort = socket.getLocalPort();

        for (int i = 0; i < peers.length; i++) {
            if (peerPorts[i] == selfPort) {
                continue; // skip our own port
            }
            synchronized (this) {
                if (throttleTokens <= 0) {
                    logger.fine("Throttle empty—dropping seq=" + pkt.getSequence());
                    return;
                }
                throttleTokens--;
            }
            try {
                socket.send(new DatagramPacket(data, data.length, peers[i], peerPorts[i]));
            } catch (Exception e) {
                logger.log(Level.WARNING,
                           "Failed to send seq=" + pkt.getSequence() +
                           " to " + peers[i] + ":" + peerPorts[i], e);
            }
        }
    }

    /** Retransmit any still‐pending packets. */
    private void retransmitPending() {
        for (ConcurrentMap<Integer, Packet> buf : outgoingBuffers.values()) {
            for (Packet pkt : buf.values()) {
                sendToPeers(pkt);
            }
        }
    }

    /** Clean up schedulers and socket. */
    public void shutdown() {
        throttleScheduler.shutdownNow();
        retransmitScheduler.shutdownNow();
        socket.close();
    }
}
