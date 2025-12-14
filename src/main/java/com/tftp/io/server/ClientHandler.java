package com.tftp.io.server;

import com.tftp.model.packet.*;
import com.tftp.model.protocol.TFTPProtocol;
import com.tftp.io.logger.Logger;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private final DatagramPacket initialPacket;
    private final String rootDirectory;
    private InetAddress clientAddress;
    private int clientPort;

    public ClientHandler(DatagramPacket packet, String rootDirectory) {
        this.initialPacket = packet;
        this.rootDirectory = rootDirectory;
        this.clientAddress = packet.getAddress();
        this.clientPort = packet.getPort();
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(10000);

            byte[] data = initialPacket.getData();
            int length = initialPacket.getLength();

            if (length < 4) {
                sendError(socket, TFTPProtocol.ERR_ILLEGAL_OPERATION, "Packet too short");
                return;
            }

            short opcode = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));

            switch (opcode) {
                case TFTPProtocol.RRQ:
                    handleReadRequest(socket, data, length);
                    break;
                case TFTPProtocol.WRQ:
                    handleWriteRequest(socket, data, length);
                    break;
                default:
                    sendError(socket, TFTPProtocol.ERR_ILLEGAL_OPERATION, "Illegal TFTP operation: " + opcode);
            }
        } catch (IOException e) {
            Logger.error("Client handler error: " + e.getMessage());
        }
    }

    private void handleReadRequest(DatagramSocket socket, byte[] data, int length) throws IOException {
        TFTPRequest request = TFTPRequest.fromBytes(data, length);
        String filename = request.getFilename();
        File file = new File(rootDirectory, filename);

        Logger.log("RRQ from " + clientAddress + ":" + clientPort + " for file: " + filename);

        if (!file.exists() || !file.isFile()) {
            sendError(socket, TFTPProtocol.ERR_FILE_NOT_FOUND, "File not found: " + filename);
            return;
        }

        if (!isInRootDirectory(file)) {
            sendError(socket, TFTPProtocol.ERR_ACCESS_VIOLATION, "Access violation");
            return;
        }

        long fileSize = file.length();
        Logger.log("Sending file: " + filename + " (" + fileSize + " bytes)");

        try (FileInputStream fis = new FileInputStream(file)) {
            short blockNumber = 1;
            byte[] buffer = new byte[TFTPProtocol.DATA_SIZE];
            int bytesRead;
            long totalBytesSent = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] fileData;
                if (bytesRead == TFTPProtocol.DATA_SIZE)
                    fileData = buffer;
                else {
                    fileData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, fileData, 0, bytesRead);
                }

                TFTPData dataPacket = new TFTPData(blockNumber, fileData);
                sendPacket(socket, dataPacket.toBytes());
                totalBytesSent += bytesRead;

                Logger.log("Sent DATA block " + blockNumber + " (" + bytesRead + " bytes)");

                boolean ackReceived = false;
                int retries = 0;
                while (!ackReceived && retries < 3) {
                    try {
                        DatagramPacket ackPacket = receivePacket(socket);
                        byte[] ackData = ackPacket.getData();
                        int ackLength = ackPacket.getLength();

                        if (ackLength < 4) {
                            retries++;
                            continue;
                        }

                        short ackOpcode = (short) (((ackData[0] & 0xFF) << 8) | (ackData[1] & 0xFF));

                        if (ackOpcode == TFTPProtocol.ACK) {
                            TFTPAck ack = TFTPAck.fromBytes(ackData, ackLength);
                            if (ack.getBlockNumber() == blockNumber) {
                                Logger.log("Received ACK for block " + blockNumber);
                                ackReceived = true;
                            }
                            else if (ack.getBlockNumber() < blockNumber)
                                Logger.log("Received duplicate ACK for block " + ack.getBlockNumber());
                            else {
                                Logger.error("Unexpected ACK block: " + ack.getBlockNumber());
                                retries++;
                            }
                        } else if (ackOpcode == TFTPProtocol.ERROR) {
                            TFTPError error = TFTPError.fromBytes(ackData, ackLength);
                            Logger.error("Client sent error: " + error.getErrorMsg());
                            return;
                        } else {
                            Logger.error("Unexpected opcode: " + ackOpcode);
                            retries++;
                        }
                    } catch (SocketTimeoutException e) {
                        retries++;
                        Logger.error("Timeout waiting for ACK for block " + blockNumber +
                                " (attempt " + retries + "/3)");
                        if (retries < 3) {
                            sendPacket(socket, dataPacket.toBytes());
                            Logger.log("Resent DATA block " + blockNumber);
                        }
                    }
                }

                if (!ackReceived) {
                    Logger.error("Failed to send block " + blockNumber + ", stopping transfer");
                    return;
                }

                blockNumber++;

                if (blockNumber == 0) {
                    Logger.error("Block number overflow, file too large");
                    return;
                }
            }

            Logger.log("File sent successfully: " + totalBytesSent + " bytes");

        } catch (IOException e) {
            Logger.error("Error reading file: " + e.getMessage());
            sendError(socket, TFTPProtocol.ERR_NOT_DEFINED, "File read error");
        }
    }

    private void handleWriteRequest(DatagramSocket socket, byte[] data, int length) throws IOException {
        TFTPRequest request = TFTPRequest.fromBytes(data, length);
        String filename = request.getFilename();
        File file = new File(rootDirectory, filename);

        Logger.log("WRQ from " + clientAddress + ":" + clientPort + " for file: " + filename);

        if (file.exists()) {
            sendError(socket, TFTPProtocol.ERR_FILE_EXISTS, "File already exists");
            return;
        }

        if (!isInRootDirectory(file)) {
            sendError(socket, TFTPProtocol.ERR_ACCESS_VIOLATION, "Access violation");
            return;
        }

        TFTPAck ack = new TFTPAck((short) 0);
        sendPacket(socket, ack.toBytes());
        Logger.log("Sent initial ACK(0)");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            short expectedBlock = 1;
            boolean lastPacket = false;
            long totalBytesReceived = 0;

            while (!lastPacket) {
                try {
                    DatagramPacket packet = receivePacket(socket);
                    byte[] packetData = packet.getData();
                    int packetLength = packet.getLength();

                    if (packetLength < 4) {
                        sendError(socket, TFTPProtocol.ERR_ILLEGAL_OPERATION, "Packet too short");
                        file.delete();
                        return;
                    }

                    short opcode = (short) (((packetData[0] & 0xFF) << 8) | (packetData[1] & 0xFF));

                    if (opcode == TFTPProtocol.DATA) {
                        TFTPData dataPacket = TFTPData.fromBytes(packetData, packetLength);
                        short blockNumber = dataPacket.getBlockNumber();

                        if (blockNumber == expectedBlock) {
                            byte[] receivedData = dataPacket.getData();
                            fos.write(receivedData);
                            totalBytesReceived += receivedData.length;

                            Logger.log("Received DATA block " + blockNumber + " (" + receivedData.length + " bytes)");

                            TFTPAck dataAck = new TFTPAck(blockNumber);
                            sendPacket(socket, dataAck.toBytes());

                            if (receivedData.length < TFTPProtocol.DATA_SIZE) {
                                lastPacket = true;
                                Logger.log("File received successfully: " + totalBytesReceived + " bytes");
                            }

                            expectedBlock++;

                            if (expectedBlock == 0) {
                                sendError(socket, TFTPProtocol.ERR_ILLEGAL_OPERATION, "File too large");
                                file.delete();
                                return;
                            }
                        } else if (blockNumber < expectedBlock) {
                            Logger.log("Received duplicate block " + blockNumber);
                            TFTPAck duplicateAck = new TFTPAck(blockNumber);
                            sendPacket(socket, duplicateAck.toBytes());
                        } else {
                            Logger.error("Unexpected block: " + blockNumber + ", expected: " + expectedBlock);
                            sendError(socket, TFTPProtocol.ERR_UNKNOWN_TID, "Unexpected block number");
                            file.delete();
                            return;
                        }
                    } else if (opcode == TFTPProtocol.ERROR) {
                        TFTPError error = TFTPError.fromBytes(packetData, packetLength);
                        Logger.error("Client sent error: " + error.getErrorMsg());
                        file.delete();
                        return;
                    } else {
                        sendError(socket, TFTPProtocol.ERR_ILLEGAL_OPERATION, "Unexpected opcode: " + opcode);
                        file.delete();
                        return;
                    }
                } catch (SocketTimeoutException e) {
                    Logger.error("Timeout waiting for DATA packet");
                    sendError(socket, TFTPProtocol.ERR_NOT_DEFINED, "Transfer timeout");
                    file.delete();
                    return;
                }
            }
        } catch (IOException e) {
            Logger.error("Error writing file: " + e.getMessage());
            sendError(socket, TFTPProtocol.ERR_DISK_FULL, "File write error");
            file.delete();
        }
    }

    private void sendError(DatagramSocket socket, int errorCode, String errorMsg) throws IOException {
        TFTPError error = new TFTPError((short) errorCode, errorMsg);
        sendPacket(socket, error.toBytes());
        Logger.error("Sent error: " + errorCode + " - " + errorMsg);
    }

    private void sendPacket(DatagramSocket socket, byte[] data) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, clientPort);
        socket.send(packet);
    }

    private DatagramPacket receivePacket(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[TFTPProtocol.PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        clientAddress = packet.getAddress();
        clientPort = packet.getPort();

        return packet;
    }

    private boolean isInRootDirectory(File file) {
        try {
            String filePath = file.getCanonicalPath();
            String rootPath = new File(rootDirectory).getCanonicalPath();
            return filePath.startsWith(rootPath);
        } catch (IOException e) {
            return false;
        }
    }
}