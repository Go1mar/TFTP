package com.tftp.model;

import com.tftp.model.packet.TFTPAck;
import org.junit.Test;
import static org.junit.Assert.*;

public class TFTPAckTest {

    @Test
    public void testCreateAck() {
        TFTPAck ack = new TFTPAck((short) 15);

        assertEquals(4, ack.getOpcode());
        assertEquals(15, ack.getBlockNumber());
    }

    @Test
    public void testAckZero() {
        TFTPAck ack = new TFTPAck((short) 0);
        assertEquals(0, ack.getBlockNumber());
    }

    @Test
    public void testToBytesAndFromBytes() {
        TFTPAck original = new TFTPAck((short) 25);
        byte[] bytes = original.toBytes();

        TFTPAck parsed = TFTPAck.fromBytes(bytes, bytes.length);

        assertEquals(original.getBlockNumber(), parsed.getBlockNumber());
        assertEquals(original.getOpcode(), parsed.getOpcode());
    }

    @Test
    public void testPacketSize() {
        TFTPAck ack = new TFTPAck((short) 1);
        byte[] bytes = ack.toBytes();
        assertEquals(4, bytes.length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBytesTooShort() {
        byte[] shortData = new byte[3];
        TFTPAck.fromBytes(shortData, shortData.length);
    }
}