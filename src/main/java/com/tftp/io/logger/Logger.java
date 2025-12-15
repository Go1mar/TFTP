package com.tftp.io.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*;

public class Logger {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static PrintWriter fileWriter = null;
    private static String logFileName = "server.log";

    public static void initialize(String rootDirectory) {
        try {
            File logDir = new File(rootDirectory, "logs");
            if (!logDir.exists())
                logDir.mkdirs();

            File logFile = new File(logDir, logFileName);
            fileWriter = new PrintWriter(new FileWriter(logFile, true));

            if (logFile.length() == 0) {
                fileWriter.println("TFTP Server Log - Started: " + dateFormat.format(new Date()));
                fileWriter.flush();
            }

            System.out.println("Log file created at: " + logFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Could not open log file: " + e.getMessage());
        }
    }

    public static void log(String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = "[" + timestamp + "] " + message;

        System.out.println(logMessage);
        writeToFile(logMessage);
    }

    public static void error(String message) {
        String timestamp = dateFormat.format(new Date());
        String errorMessage = "[" + timestamp + "] ERROR: " + message;

        System.err.println(errorMessage);
        writeToFile(errorMessage);
    }

    private static void writeToFile(String message) {
        if (fileWriter != null) {
            fileWriter.println(message);
            fileWriter.flush();
        }
    }

    public static void close() {
        if (fileWriter != null) {
            String timestamp = dateFormat.format(new Date());
            fileWriter.println("TFTP Server Log - Stopped: " + timestamp);
            fileWriter.flush();
            fileWriter.close();
            fileWriter = null;
        }
    }
}