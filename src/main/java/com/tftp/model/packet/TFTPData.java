package com.tftp.model.packet;

public class TFTPData {
    private final short opcode = 3;
    private final short blockNumber;
    private final byte[] data;

    public TFTPData(short blockNumber, byte[] data) {
        if (data == null) {
            throw new NullPointerException("Data cannot be null");
        }
        this.blockNumber = blockNumber;
        this.data = data;
    }

    public byte[] toBytes() {
        byte[] packet = new byte[4 + data.length];

        packet[0] = (byte) ((opcode >> 8) & 0xFF);
        packet[1] = (byte) (opcode & 0xFF);
        packet[2] = (byte) ((blockNumber >> 8) & 0xFF);
        packet[3] = (byte) (blockNumber & 0xFF);
        System.arraycopy(data, 0, packet, 4, data.length);

        return packet;
    }

    public static TFTPData fromBytes(byte[] data, int length) {
        if (length < 4)
            throw new IllegalArgumentException("Data packet too short: " + length + " bytes");

        short blockNumber = (short) (((data[2] & 0xFF) << 8) | (data[3] & 0xFF));
        int dataLength = length - 4;
        byte[] fileData = new byte[dataLength];
        System.arraycopy(data, 4, fileData, 0, dataLength);

        return new TFTPData(blockNumber, fileData);
    }

    public short getOpcode() { return opcode; }
    public short getBlockNumber() { return blockNumber; }
    public byte[] getData() { return data; }
}