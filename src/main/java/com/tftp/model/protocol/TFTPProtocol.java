package com.tftp.model.protocol;

public class TFTPProtocol {
    public static final short RRQ = 1;
    public static final short WRQ = 2;
    public static final short DATA = 3;
    public static final short ACK = 4;
    public static final short ERROR = 5;

    public static final int DEFAULT_PORT = 69;
    public static final int DATA_SIZE = 512;
    public static final int PACKET_SIZE = 516;

    public static final short ERR_NOT_DEFINED = 0;
    public static final short ERR_FILE_NOT_FOUND = 1;
    public static final short ERR_ACCESS_VIOLATION = 2;
    public static final short ERR_DISK_FULL = 3;
    public static final short ERR_ILLEGAL_OPERATION = 4;
    public static final short ERR_UNKNOWN_TID = 5;
    public static final short ERR_FILE_EXISTS = 6;
    public static final short ERR_NO_SUCH_USER = 7;
}