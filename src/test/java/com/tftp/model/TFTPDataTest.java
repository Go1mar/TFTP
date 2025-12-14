package com.tftp.model;

import com.tftp.model.packet.TFTPData;
import com.tftp.model.protocol.TFTPProtocol;
import org.junit.Test;
import static org.junit.Assert.*;

public class TFTPDataTest {

    @Test
    public void testCreateDataPacket() {
        byte[] data = {1, 2, 3, 4, 5};
        TFTPData packet = new TFTPData((short) 10, data);

        assertEquals(3, packet.getOpcode());
        assertEquals(10, packet.getBlockNumber());
        assertArrayEquals(data, packet.getData());
    }

    @Test
    public void testFullSizeData() {
        byte[] fullData = new byte[TFTPProtocol.DATA_SIZE];
        for (int i = 0; i < fullData.length; i++) {
            fullData[i] = (byte) i;
        }

        TFTPData packet = new TFTPData((short) 1, fullData);
        assertEquals(TFTPProtocol.DATA_SIZE, packet.getData().length);
    }

    @Test
    public void testToBytesAndFromBytes() {
        byte[] originalData = {10, 20, 30, 40, 50};
        TFTPData original = new TFTPData((short) 5, originalData);
        byte[] bytes = original.toBytes();

        TFTPData parsed = TFTPData.fromBytes(bytes, bytes.length);

        assertEquals(original.getBlockNumber(), parsed.getBlockNumber());
        assertArrayEquals(original.getData(), parsed.getData());
    }

    @Test(expected = NullPointerException.class)
    public void testNullData() {
        new TFTPData((short) 1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBytesTooShort() {
        byte[] shortData = new byte[3];
        TFTPData.fromBytes(shortData, shortData.length);
    }
}