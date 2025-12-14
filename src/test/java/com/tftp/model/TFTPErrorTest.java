package com.tftp.model;

import com.tftp.model.packet.TFTPError;
import com.tftp.model.protocol.TFTPProtocol;
import org.junit.Test;
import static org.junit.Assert.*;

public class TFTPErrorTest {

    @Test
    public void testCreateError() {
        TFTPError error = new TFTPError(TFTPProtocol.ERR_FILE_NOT_FOUND, "File not found");

        assertEquals(5, error.getOpcode());
        assertEquals(TFTPProtocol.ERR_FILE_NOT_FOUND, error.getErrorCode());
        assertEquals("File not found", error.getErrorMsg());
    }

    @Test
    public void testEmptyErrorMessage() {
        TFTPError error = new TFTPError(TFTPProtocol.ERR_NOT_DEFINED, "");
        assertEquals("", error.getErrorMsg());
    }

    @Test
    public void testToBytesAndFromBytes() {
        TFTPError original = new TFTPError(TFTPProtocol.ERR_ACCESS_VIOLATION, "Access denied");
        byte[] bytes = original.toBytes();

        TFTPError parsed = TFTPError.fromBytes(bytes, bytes.length);

        assertEquals(original.getErrorCode(), parsed.getErrorCode());
        assertEquals(original.getErrorMsg(), parsed.getErrorMsg());
    }

    @Test
    public void testAllStandardErrorCodes() {
        TFTPError error1 = new TFTPError(TFTPProtocol.ERR_FILE_NOT_FOUND, "Not found");
        TFTPError error2 = new TFTPError(TFTPProtocol.ERR_DISK_FULL, "Disk full");
        TFTPError error3 = new TFTPError(TFTPProtocol.ERR_ILLEGAL_OPERATION, "Illegal operation");

        assertEquals(1, error1.getErrorCode());
        assertEquals(3, error2.getErrorCode());
        assertEquals(4, error3.getErrorCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBytesTooShort() {
        byte[] shortData = new byte[3];
        TFTPError.fromBytes(shortData, shortData.length);
    }
}