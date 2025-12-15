package com.tftp.io.server;

import com.tftp.model.protocol.TFTPProtocol;
import com.tftp.io.logger.Logger;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TFTPServer {
    private final int port;
    private final String rootDirectory;
    private final ExecutorService threadPool;
    private volatile boolean running;
    private DatagramSocket serverSocket;

    public TFTPServer(int port, String rootDirectory) {
        this.port = port;
        this.rootDirectory = rootDirectory;
        this.threadPool = Executors.newFixedThreadPool(10);
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        File dir = new File(rootDirectory);
        if (!dir.exists())
            dir.mkdirs();
    }

    public void start() {
        try {
            serverSocket = new DatagramSocket(port);
            running = true;
            Logger.log("TFTP Server started on port " + port);
            Logger.log("Root directory: " + rootDirectory);

            while (running) {
                byte[] buffer = new byte[TFTPProtocol.PACKET_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);

                threadPool.execute(new ClientHandler(packet, rootDirectory));
            }
        } catch (IOException e) {
            if (running)
                Logger.error("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed())
            serverSocket.close();
        threadPool.shutdown();
        Logger.log("TFTP Server stopped");
    }

    public static void main(String[] args) {
        int port = TFTPProtocol.DEFAULT_PORT;
        String rootDir = "D:/TFTP/Server";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-p":
                case "--port":
                    if (i + 1 < args.length)
                        port = Integer.parseInt(args[++i]);
                    break;
                case "-d":
                case "--dir":
                    if (i + 1 < args.length)
                        rootDir = args[++i];
                    break;
            }
        }

        TFTPServer server = new TFTPServer(port, rootDir);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        server.start();
    }
}