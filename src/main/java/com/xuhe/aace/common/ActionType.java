package com.xuhe.aace.common;

public class ActionType {

    public static final int MESSAGE_RECV = 0;
    public static final int MESSAGE_SEND = 1;
    public static final int CLEAR_BUFFER = 2;
    public static final int STARTUP_CONN = 3;
    public static final int SET_ENCKEY = 4;
    public static final int SHUTDOWN = 5;
    public static final int HEALTH_CHECK = 6;

    public static final int UNUSE = -1;
}
