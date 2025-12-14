package com.tftp.io.client;

import com.tftp.model.protocol.TFTPProtocol;
import com.tftp.model.client.ClientModel;
import com.tftp.model.packet.TFTPAck;
import com.tftp.model.packet.TFTPData;
import com.tftp.model.packet.TFTPError;
import com.tftp.model.packet.TFTPRequest;

import java.io.*;
import java.net.*;

public class TFTPClient {
    private InetAddress serverAddress;
    private int serverPort;
    private String localDirectory;
    private volatile boolean transferInProgress;
    private volatile boolean stopRequested;

    public TFTPClient(String serverHost, int port, String localDirectory) throws UnknownHostException {
        this.serverAddress = InetAddress.getByName(serverHost);
        this.serverPort = port;
        this.localDirectory = localDirectory;
        ensureLocalDirectoryExists();
    }

    private void ensureLocalDirectoryExists() {
        File dir = new File(localDirectory);
        if (!dir.exists())
            dir.mkdirs();
    }

    public boolean downloadFile(String filename, ClientModel model) {
        if (transferInProgress) {
            model.updateStatus("Another transfer is in progress");
            return false;
        }

        transferInProgress = true;
        stopRequested = false;

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);

            TFTPRequest rrq = new TFTPRequest(TFTPProtocol.RRQ, filename);
            sendPacket(socket, rrq.toBytes(), serverAddress, serverPort);

            File outputFile = new File(localDirectory, filename);
            if (outputFile.exists())
                outputFile.delete();

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                short expectedBlock = 1;
                long totalBytesReceived = 0;
                boolean lastPacket = false;

                model.updateStatus("Starting download: " + filename);
                model.updateLog("Sent RRQ for file: " + filename);

                while (!lastPacket && !stopRequested) {
                    DatagramPacket packet = receivePacket(socket);
                    byte[] data = packet.getData();
                    int length = packet.getLength();

                    if (length < 4) {
                        model.updateLog("Packet too short: " + length + " bytes");
                        continue;
                    }

                    short opcode = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));

                    if (opcode == TFTPProtocol.DATA) {
                        TFTPData dataPacket = TFTPData.fromBytes(data, length);
                        short receivedBlock = dataPacket.getBlockNumber();

                        if (receivedBlock == expectedBlock) {
                            byte[] fileData = dataPacket.getData();
                            fos.write(fileData);
                            totalBytesReceived += fileData.length;

                            model.updateProgress(totalBytesReceived, totalBytesReceived);
                            model.updateLog("Received DATA block " + expectedBlock + " (" + fileData.length + " bytes)");

                            TFTPAck ack = new TFTPAck(expectedBlock);
                            sendPacket(socket, ack.toBytes(), packet.getAddress(), packet.getPort());

                            if (fileData.length < TFTPProtocol.DATA_SIZE) {
                                lastPacket = true;
                                model.updateStatus("Download completed: " + filename);
                                model.updateLog("Download completed successfully. Total: " +
                                        totalBytesReceived + " bytes");
                            }

                            expectedBlock++;
                        } else if (receivedBlock == (short)(expectedBlock - 1)) {
                            model.updateLog("Received duplicate block " + receivedBlock + ", resending ACK");
                            TFTPAck ack = new TFTPAck(receivedBlock);
                            sendPacket(socket, ack.toBytes(), packet.getAddress(), packet.getPort());
                        } else {
                            model.updateLog("Unexpected block number: " + receivedBlock + ", expected: " + expectedBlock);
                        }
                    } else if (opcode == TFTPProtocol.ERROR) {
                        TFTPError error = TFTPError.fromBytes(data, length);
                        model.updateStatus("Error: " + error.getErrorMsg());
                        model.updateLog("Received ERROR: " + error.getErrorCode() + " - " + error.getErrorMsg());
                        return false;
                    } else if (opcode == TFTPProtocol.ACK) {
                        TFTPAck ack = TFTPAck.fromBytes(data, length);
                        model.updateLog("Received ACK during download (block " + ack.getBlockNumber() + "), ignoring");
                        continue;
                    } else {
                        model.updateStatus("Invalid TFTP operation: " + opcode);
                        model.updateLog("Invalid opcode received: " + opcode);
                        return false;
                    }
                }

                if (stopRequested) {
                    model.updateStatus("Download cancelled");
                    model.updateLog("Download cancelled by user");
                    return false;
                }

                return true;

            } catch (IOException e) {
                model.updateStatus("File error: " + e.getMessage());
                return false;
            }

        } catch (SocketTimeoutException e) {
            model.updateStatus("Timeout: Server not responding");
            return false;
        } catch (IOException e) {
            model.updateStatus("Network error: " + e.getMessage());
            return false;
        } finally {
            if (socket != null && !socket.isClosed())
                socket.close();
            transferInProgress = false;
        }
    }

    public boolean uploadFile(String filename, ClientModel model) {
        if (transferInProgress) {
            model.updateStatus("Another transfer is in progress");
            return false;
        }

        transferInProgress = true;
        stopRequested = false;

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);

            TFTPRequest wrq = new TFTPRequest(TFTPProtocol.WRQ, filename);
            sendPacket(socket, wrq.toBytes(), serverAddress, serverPort);

            File inputFile = new File(localDirectory, filename);
            if (!inputFile.exists() || !inputFile.isFile()) {
                model.updateStatus("File not found: " + filename);
                return false;
            }

            long fileSize = inputFile.length();

            try (FileInputStream fis = new FileInputStream(inputFile)) {
                DatagramPacket ackPacket = receivePacket(socket);
                byte[] ackData = ackPacket.getData();
                int length = ackPacket.getLength();

                if (length < 4) {
                    model.updateStatus("Invalid ACK packet");
                    return false;
                }

                short opcode = (short) (((ackData[0] & 0xFF) << 8) | (ackData[1] & 0xFF));

                if (opcode == TFTPProtocol.ERROR) {
                    TFTPError error = TFTPError.fromBytes(ackData, length);
                    model.updateStatus("Error: " + error.getErrorMsg());
                    return false;
                } else if (opcode != TFTPProtocol.ACK) {
                    model.updateStatus("Expected ACK, received: " + opcode);
                    return false;
                }

                TFTPAck ack = TFTPAck.fromBytes(ackData, length);
                if (ack.getBlockNumber() != 0) {
                    model.updateStatus("Expected ACK(0), received ACK(" + ack.getBlockNumber() + ")");
                    return false;
                }

                model.updateStatus("Starting upload: " + filename);
                model.updateLog("Sent WRQ for file: " + filename);
                model.updateLog("File size: " + fileSize + " bytes");
                model.updateLog("Received initial ACK(0)");

                short blockNumber = 1;
                long totalBytesSent = 0;
                byte[] buffer = new byte[TFTPProtocol.DATA_SIZE];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1 && !stopRequested) {
                    byte[] fileData;
                    if (bytesRead == TFTPProtocol.DATA_SIZE)
                        fileData = buffer;
                    else {
                        fileData = new byte[bytesRead];
                        System.arraycopy(buffer, 0, fileData, 0, bytesRead);
                    }

                    TFTPData dataPacket = new TFTPData(blockNumber, fileData);
                    sendPacket(socket, dataPacket.toBytes(), ackPacket.getAddress(), ackPacket.getPort());

                    totalBytesSent += bytesRead;
                    model.updateProgress(totalBytesSent, fileSize);
                    model.updateLog("Sent DATA block " + blockNumber + " (" + bytesRead + " bytes)");

                    boolean ackReceived = false;
                    int retries = 0;
                    while (!ackReceived && retries < 3) {
                        try {
                            DatagramPacket blockAckPacket = receivePacket(socket);
                            byte[] blockAckData = blockAckPacket.getData();
                            int blockAckLength = blockAckPacket.getLength();

                            if (blockAckLength < 4) {
                                retries++;
                                continue;
                            }

                            short blockOpcode = (short) (((blockAckData[0] & 0xFF) << 8) | (blockAckData[1] & 0xFF));

                            if (blockOpcode == TFTPProtocol.ACK) {
                                TFTPAck blockAck = TFTPAck.fromBytes(blockAckData, blockAckLength);
                                if (blockAck.getBlockNumber() == blockNumber) {
                                    model.updateLog("Received ACK for block " + blockNumber);
                                    ackReceived = true;
                                } else if (blockAck.getBlockNumber() < blockNumber) {
                                    model.updateLog("Received duplicate ACK for block " + blockAck.getBlockNumber());
                                } else {
                                    model.updateLog("Unexpected ACK block: " + blockAck.getBlockNumber());
                                    retries++;
                                }
                            } else if (blockOpcode == TFTPProtocol.ERROR) {
                                TFTPError error = TFTPError.fromBytes(blockAckData, blockAckLength);
                                model.updateStatus("Error: " + error.getErrorMsg());
                                return false;
                            } else {
                                model.updateLog("Unexpected opcode: " + blockOpcode);
                                retries++;
                            }
                        } catch (SocketTimeoutException e) {
                            retries++;
                            model.updateLog("Timeout waiting for ACK for block " + blockNumber +
                                    " (attempt " + retries + "/3)");
                            if (retries < 3) {
                                sendPacket(socket, dataPacket.toBytes(), ackPacket.getAddress(), ackPacket.getPort());
                                model.updateLog("Resent DATA block " + blockNumber);
                            }
                        }
                    }

                    if (!ackReceived) {
                        model.updateStatus("Upload failed: No ACK for block " + blockNumber);
                        return false;
                    }

                    blockNumber++;
                }

                if (stopRequested) {
                    model.updateStatus("Upload cancelled");
                    model.updateLog("Upload cancelled by user");
                    return false;
                }

                model.updateStatus("Upload completed: " + filename);
                model.updateLog("Upload completed successfully. Total: " + totalBytesSent + " bytes");
                return true;

            } catch (IOException e) {
                model.updateStatus("File error: " + e.getMessage());
                return false;
            }

        } catch (SocketTimeoutException e) {
            model.updateStatus("Timeout: Server not responding");
            return false;
        } catch (IOException e) {
            model.updateStatus("Network error: " + e.getMessage());
            return false;
        } finally {
            if (socket != null && !socket.isClosed())
                socket.close();
            transferInProgress = false;
        }
    }

    private void sendPacket(DatagramSocket socket, byte[] data, InetAddress address, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }

    private DatagramPacket receivePacket(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[TFTPProtocol.PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    public void stopTransfer() {
        stopRequested = true;
    }

    public boolean isTransferInProgress() {
        return transferInProgress;
    }
}