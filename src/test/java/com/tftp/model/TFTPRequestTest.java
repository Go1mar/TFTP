package com.tftp.model;

import com.tftp.model.packet.TFTPRequest;
import com.tftp.model.protocol.TFTPProtocol;
import org.junit.Test;
import static org.junit.Assert.*;

public class TFTPRequestTest {

    @Test
    public void testCreateRRQ() {
        TFTPRequest request = new TFTPRequest(TFTPProtocol.RRQ, "test.txt");

        assertEquals(TFTPProtocol.RRQ, request.getOpcode());
        assertEquals("test.txt", request.getFilename());
        assertEquals("octet", request.getMode());
    }

    @Test
    public void testCreateWRQ() {
        TFTPRequest request = new TFTPRequest(TFTPProtocol.WRQ, "file.bin");

        assertEquals(TFTPProtocol.WRQ, request.getOpcode());
        assertEquals("file.bin", request.getFilename());
        assertEquals("octet", request.getMode());
    }

    @Test
    public void testToBytesAndFromBytes() {
        TFTPRequest original = new TFTPRequest(TFTPProtocol.RRQ, "document.pdf");
        byte[] bytes = original.toBytes();

        TFTPRequest parsed = TFTPRequest.fromBytes(bytes, bytes.length);

        assertEquals(original.getOpcode(), parsed.getOpcode());
        assertEquals(original.getFilename(), parsed.getFilename());
        assertEquals(original.getMode(), parsed.getMode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBytesTooShort() {
        byte[] shortData = new byte[3];
        TFTPRequest.fromBytes(shortData, shortData.length);
    }
}