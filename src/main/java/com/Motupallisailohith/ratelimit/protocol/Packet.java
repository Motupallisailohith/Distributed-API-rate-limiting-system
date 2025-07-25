package com.Motupallisailohith.ratelimit.protocol;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * Packet represents the custom UDP-based reliable protocol frame
 * carrying rate-limit delta or acknowledgement messages.
 */
public class Packet {
    // --- Protocol Constants ---
    public static final short MAGIC = (short) 0xDA7A; // 2 bytes
    public static final byte VERSION = 0x01;          // 1 byte

    /**
     * Types of protocol messages
     */
    public enum Type {
        DELTA_UPDATE((byte) 0x01),
        ACK((byte) 0x02);

        private final byte code;
        Type(byte code) { this.code = code; }
        public byte getCode() { return code; }
        public static Type fromByte(byte b) {
            for (Type t : values()) {
                if (t.code == b) return t;
            }
            throw new IllegalArgumentException("Unknown Packet Type: " + b);
        }
    }

    // --- Packet Fields ---
    private final short magic;
    private final byte version;
    private final Type type;
    private final int sequence;
    private final short length;      // payload length in bytes
    private final int bucketId;      // only for DELTA_UPDATE
    private final int delta;         // only for DELTA_UPDATE
    private final long checksum;     // CRC32 over header+payload

    // Private constructor used by factory methods and fromBytes
    private Packet(short magic, byte version, Type type,
                   int sequence, short length,
                   int bucketId, int delta, long checksum) {
        this.magic = magic;
        this.version = version;
        this.type = type;
        this.sequence = sequence;
        this.length = length;
        this.bucketId = bucketId;
        this.delta = delta;
        this.checksum = checksum;
    }

    /**
     * Factory for a DELTA_UPDATE packet
     */
    public static Packet deltaUpdate(int bucketId, int delta, int sequence) {
        short payloadLen = (short) (Integer.BYTES + Integer.BYTES);
        ByteBuffer buf = ByteBuffer.allocate(Short.BYTES + Byte.BYTES + Byte.BYTES
            + Integer.BYTES + Short.BYTES + payloadLen);
        buf.putShort(MAGIC);
        buf.put(VERSION);
        buf.put(Type.DELTA_UPDATE.getCode());
        buf.putInt(sequence);
        buf.putShort(payloadLen);
        buf.putInt(bucketId);
        buf.putInt(delta);
        buf.flip();

        CRC32 crc = new CRC32();
        crc.update(buf.array(), 0, buf.limit());
        long cs = crc.getValue();
        return new Packet(MAGIC, VERSION, Type.DELTA_UPDATE,
                          sequence, payloadLen,
                          bucketId, delta, cs);
    }

    /**
     * Factory for an ACK packet
     */
    public static Packet ack(int sequence) {
        short payloadLen = 0;
        ByteBuffer buf = ByteBuffer.allocate(Short.BYTES + Byte.BYTES + Byte.BYTES
            + Integer.BYTES + Short.BYTES);
        buf.putShort(MAGIC);
        buf.put(VERSION);
        buf.put(Type.ACK.getCode());
        buf.putInt(sequence);
        buf.putShort(payloadLen);
        buf.flip();

        CRC32 crc = new CRC32();
        crc.update(buf.array(), 0, buf.limit());
        long cs = crc.getValue();
        return new Packet(MAGIC, VERSION, Type.ACK,
                          sequence, payloadLen,
                          0, 0, cs);
    }

    /**
     * Serialize this packet into bytes
     */
    public byte[] toBytes() {
        int headerLen = Short.BYTES + Byte.BYTES + Byte.BYTES
                      + Integer.BYTES + Short.BYTES;
        int totalLen = headerLen + length + Integer.BYTES;
        ByteBuffer buf = ByteBuffer.allocate(totalLen);

        buf.putShort(magic);
        buf.put(version);
        buf.put(type.getCode());
        buf.putInt(sequence);
        buf.putShort(length);
        if (type == Type.DELTA_UPDATE) {
            buf.putInt(bucketId);
            buf.putInt(delta);
        }

        // Compute checksum over header+payload
        CRC32 crc = new CRC32();
        crc.update(buf.array(), 0, headerLen + length);
        buf.putInt((int) crc.getValue());

        return buf.array();
    }

    /**
     * Deserialize a Packet from bytes, validating magic, version, length, and checksum.
     */
    public static Packet fromBytes(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        if (data.length < Short.BYTES + Byte.BYTES + Byte.BYTES
            + Integer.BYTES + Short.BYTES + Integer.BYTES) {
            throw new IllegalArgumentException("Packet too short: " + data.length);
        }

        short magic = buf.getShort();
        if (magic != MAGIC) {
            throw new IllegalArgumentException("Invalid MAGIC: " + String.format("0x%04X", magic));
        }
        byte version = buf.get();
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported VERSION: " + version);
        }
        Type type = Type.fromByte(buf.get());
        int seq = buf.getInt();
        short length = buf.getShort();

        int expectedLen = Short.BYTES + Byte.BYTES + Byte.BYTES
                        + Integer.BYTES + Short.BYTES + length + Integer.BYTES;
        if (data.length != expectedLen) {
            throw new IllegalArgumentException(
                "Expected " + expectedLen + " bytes, got " + data.length);
        }

        int bucketId = 0, delta = 0;
        if (type == Type.DELTA_UPDATE) {
            if (length != Integer.BYTES * 2) {
                throw new IllegalArgumentException("Invalid payload length for DELTA_UPDATE: " + length);
            }
            bucketId = buf.getInt();
            delta = buf.getInt();
        }

        long receivedCrc = Integer.toUnsignedLong(buf.getInt());
        CRC32 crc = new CRC32();
        crc.update(data, 0, data.length - Integer.BYTES);
        if (crc.getValue() != receivedCrc) {
            throw new IllegalArgumentException("Checksum mismatch: calc=" + crc.getValue()
                                             + ", recv=" + receivedCrc);
        }

        return new Packet(magic, version, type, seq, length, bucketId, delta, receivedCrc);
    }

    // --- Getters ---
    public Type getType()       { return type; }
    public int getSequence()    { return sequence; }
    public int getBucketId()    { return bucketId; }
    public int getDelta()       { return delta; }
    public long getChecksum()   { return checksum; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Packet)) return false;
        Packet p = (Packet) o;
        return magic == p.magic && version == p.version
            && type == p.type && sequence == p.sequence
            && length == p.length && bucketId == p.bucketId
            && delta == p.delta && checksum == p.checksum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(magic, version, type, sequence, length, bucketId, delta, checksum);
    }

    @Override
    public String toString() {
        return "Packet{" +
               "magic=0x" + Integer.toHexString(magic & 0xFFFF) +
               ", version=" + version +
               ", type=" + type +
               ", sequence=" + sequence +
               ", length=" + length +
               ", bucketId=" + bucketId +
               ", delta=" + delta +
               ", checksum=0x" + Long.toHexString(checksum) +
               '}';
    }
}
