package com.tftp.model;

import com.tftp.model.client.ClientModel;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClientModelTest {

    @Test
    public void testInitialState() {
        ClientModel model = new ClientModel();

        assertEquals("Ready", model.getStatus());
        assertEquals(0, model.getLogMessages().size());
        assertEquals(0, model.getCurrentProgress());
        assertEquals(0, model.getTotalSize());
    }

    @Test
    public void testUpdateStatus() {
        ClientModel model = new ClientModel();
        TestListener listener = new TestListener();
        model.setListener(listener);

        model.updateStatus("Downloading...");

        assertEquals("Downloading...", model.getStatus());
        assertEquals("Downloading...", listener.lastStatus);
    }

    @Test
    public void testUpdateLog() {
        ClientModel model = new ClientModel();
        TestListener listener = new TestListener();
        model.setListener(listener);

        model.updateLog("File transfer started");

        assertEquals(1, model.getLogMessages().size());
        assertEquals("File transfer started", model.getLogMessages().get(0));
        assertEquals("File transfer started", listener.lastLog);
    }

    @Test
    public void testUpdateProgress() {
        ClientModel model = new ClientModel();
        TestListener listener = new TestListener();
        model.setListener(listener);

        model.updateProgress(500, 1000);

        assertEquals(500, model.getCurrentProgress());
        assertEquals(1000, model.getTotalSize());
        assertEquals(500, listener.lastCurrent);
        assertEquals(1000, listener.lastTotal);
    }

    @Test
    public void testCompleteTransfer() {
        ClientModel model = new ClientModel();

        model.updateStatus("Starting transfer");
        model.updateLog("Connected to server");
        model.updateProgress(0, 1024);
        model.updateProgress(512, 1024);
        model.updateProgress(1024, 1024);
        model.updateStatus("Transfer complete");
        model.updateLog("File saved successfully");

        assertEquals("Transfer complete", model.getStatus());
        assertEquals(2, model.getLogMessages().size());
        assertEquals(1024, model.getCurrentProgress());
        assertEquals(1024, model.getTotalSize());
    }

    class TestListener implements ClientModel.ClientModelListener {
        public String lastStatus;
        public String lastLog;
        public long lastCurrent;
        public long lastTotal;

        @Override
        public void onStatusUpdated(String status) {
            lastStatus = status;
        }

        @Override
        public void onLogUpdated(String logMessage) {
            lastLog = logMessage;
        }

        @Override
        public void onProgressUpdated(long current, long total) {
            lastCurrent = current;
            lastTotal = total;
        }
    }
}