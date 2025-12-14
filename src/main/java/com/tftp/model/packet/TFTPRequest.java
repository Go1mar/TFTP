package com.tftp.model.packet;

import java.nio.charset.StandardCharsets;

public class TFTPRequest {
    private final short opcode;
    private final String filename;

    public TFTPRequest(short opcode, String filename) {
        this.opcode = opcode;
        this.filename = filename;
    }

    public byte[] toBytes() {
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
        byte[] modeBytes = "octet".getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[4 + filenameBytes.length + modeBytes.length];

        data[0] = (byte) ((opcode >> 8) & 0xFF);
        data[1] = (byte) (opcode & 0xFF);

        System.arraycopy(filenameBytes, 0, data, 2, filenameBytes.length);
        data[2 + filenameBytes.length] = 0;

        System.arraycopy(modeBytes, 0, data, 3 + filenameBytes.length, modeBytes.length);
        data[3 + filenameBytes.length + modeBytes.length] = 0;

        return data;
    }

    public static TFTPRequest fromBytes(byte[] data, int length) {
        if (length < 4)
            throw new IllegalArgumentException("Request packet too short: " + length + " bytes");

        short opcode = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));

        int filenameEnd = 2;
        while (filenameEnd < length && data[filenameEnd] != 0)
            filenameEnd++;

        String filename = "";
        if (filenameEnd > 2)
            filename = new String(data, 2, filenameEnd - 2, StandardCharsets.UTF_8);

        return new TFTPRequest(opcode, filename);
    }

    public short getOpcode() { return opcode; }
    public String getFilename() { return filename; }
    public String getMode() { return "octet"; }
}