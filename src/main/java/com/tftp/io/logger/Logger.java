package com.tftp.io.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*;

public class Logger {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static PrintWriter fileWriter = null;

    static {
        try {
            File logDir = new File("logs");
            if (!logDir.exists())
                logDir.mkdir();

            fileWriter = new PrintWriter(new FileWriter("logs/server.log", true));
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
}