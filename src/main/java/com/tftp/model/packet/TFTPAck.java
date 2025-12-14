package com.tftp.model.packet;

public class TFTPAck {
    private final short opcode = 4;
    private final short blockNumber;

    public TFTPAck(short blockNumber) {
        this.blockNumber = blockNumber;
    }

    public byte[] toBytes() {
        byte[] packet = new byte[4];

        packet[0] = (byte) ((opcode >> 8) & 0xFF);
        packet[1] = (byte) (opcode & 0xFF);
        packet[2] = (byte) ((blockNumber >> 8) & 0xFF);
        packet[3] = (byte) (blockNumber & 0xFF);

        return packet;
    }

    public static TFTPAck fromBytes(byte[] data, int length) {
        if (length < 4)
            throw new IllegalArgumentException("ACK packet too short: " + length + " bytes");
        short blockNumber = (short) (((data[2] & 0xFF) << 8) | (data[3] & 0xFF));
        return new TFTPAck(blockNumber);
    }

    public short getOpcode() { return opcode; }
    public short getBlockNumber() { return blockNumber; }
}