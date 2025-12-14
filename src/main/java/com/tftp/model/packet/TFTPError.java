package com.tftp.model.packet;

import java.nio.charset.StandardCharsets;

public class TFTPError {
    private final short opcode = 5;
    private final short errorCode;
    private final String errorMsg;

    public TFTPError(short errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public byte[] toBytes() {
        byte[] errorMsgBytes = errorMsg.getBytes(StandardCharsets.UTF_8);
        byte[] packet = new byte[4 + errorMsgBytes.length + 1];

        packet[0] = (byte) ((opcode >> 8) & 0xFF);
        packet[1] = (byte) (opcode & 0xFF);
        packet[2] = (byte) ((errorCode >> 8) & 0xFF);
        packet[3] = (byte) (errorCode & 0xFF);

        System.arraycopy(errorMsgBytes, 0, packet, 4, errorMsgBytes.length);
        packet[packet.length - 1] = 0;

        return packet;
    }

    public static TFTPError fromBytes(byte[] data, int length) {
        if (length < 4)
            throw new IllegalArgumentException("Error packet too short: " + length + " bytes");

        short errorCode = (short) (((data[2] & 0xFF) << 8) | (data[3] & 0xFF));

        int msgEnd = 4;
        while (msgEnd < length && data[msgEnd] != 0)
            msgEnd++;

        String errorMsg = "";
        if (msgEnd > 4)
            errorMsg = new String(data, 4, msgEnd - 4, StandardCharsets.UTF_8);

        return new TFTPError(errorCode, errorMsg);
    }

    public short getOpcode() { return opcode; }
    public short getErrorCode() { return errorCode; }
    public String getErrorMsg() { return errorMsg; }
}