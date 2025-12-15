package com.tftp.model.client;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class ClientModel {
    private String status;
    private List<String> logMessages;
    private long currentProgress;
    private long totalSize;
    private ClientModelListener listener;
    private PrintWriter clientLogWriter;
    private final String logDirectory = "D:/TFTP/Client/logs";
    private final String logFile = "tftp_client.log";

    public interface ClientModelListener {
        void onStatusUpdated(String status);
        void onLogUpdated(String logMessage);
        void onProgressUpdated(long current, long total);
    }

    public ClientModel() {
        this.logMessages = new ArrayList<>();
        this.status = "Ready";
        this.currentProgress = 0;
        this.totalSize = 0;
        initializeClientLog();
    }

    private void initializeClientLog() {
        try {
            File logDir = new File(logDirectory);
            if (!logDir.exists())
                logDir.mkdirs();

            File logFile = new File(logDir, this.logFile);
            clientLogWriter = new PrintWriter(new FileWriter(logFile, true));

            if (logFile.length() == 0) {
                clientLogWriter.println("TFTP Client Log");
                clientLogWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize client log: " + e.getMessage());
            clientLogWriter = null;
        }
    }

    public void setListener(ClientModelListener listener) {
        this.listener = listener;
    }

    public void updateStatus(String status) {
        this.status = status;
        if (listener != null)
            listener.onStatusUpdated(status);

        writeToClientLog("STATUS: " + status);
    }

    public void updateLog(String logMessage) {
        this.logMessages.add(logMessage);
        if (listener != null)
            listener.onLogUpdated(logMessage);

        writeToClientLog("LOG: " + logMessage);
    }

    public void updateProgress(long current, long total) {
        this.currentProgress = current;
        this.totalSize = total;

        if (listener != null)
            listener.onProgressUpdated(current, total);

        if (current % 5120 == 0 || current == total)
            writeToClientLog("PROGRESS: " + current + "/" + total + " bytes (" + (int)((double)current/total*100) + "%)");
    }

    private void writeToClientLog(String message) {
        if (clientLogWriter != null) {
            try {
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timestamp = dateFormat.format(new java.util.Date());
                clientLogWriter.println("[" + timestamp + "] " + message);
                clientLogWriter.flush();
            } catch (Exception e) {}
        }
    }

    public String getStatus() {
        return status;
    }

    public List<String> getLogMessages() {
        return logMessages;
    }

    public long getCurrentProgress() {
        return currentProgress;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void close() {
        if (clientLogWriter != null) {
            clientLogWriter.close();
            clientLogWriter = null;
        }
    }
}