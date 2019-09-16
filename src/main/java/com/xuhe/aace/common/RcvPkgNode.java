package com.xuhe.aace.common;

import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class RcvPkgNode {

    private int action;
    private SocketChannel channel;
    private byte[] message;
    private long reqTime;

    public RcvPkgNode(int action,SocketChannel channel,byte[] message){
        this.action = action;
        this.channel = channel;
        this.message = message;
        this.reqTime = System.currentTimeMillis();
    }

    public int getAction() {
        return action;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public byte[] getMessage() {
        return message;
    }

    public long getReqTime() {
        return reqTime;
    }

    public void setMessage(byte[] message){
        this.message = message;
    }

    @Override
    public String toString() {
        return "RcvPkgNode{" +
                "action=" + action +
                ", channel=" + channel +
                ", message=" + Arrays.toString(message) +
                ", reqTime=" + reqTime +
                '}';
    }
}
