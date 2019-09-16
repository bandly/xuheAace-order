package com.xuhe.aace.common;

public class SndPkgNode {

    private int action;
    private byte[] message;
    private byte commFlag;

    public SndPkgNode(){
        this.action =ActionType.MESSAGE_SEND;
        this.commFlag = 3;
    }

    public SndPkgNode(int action){
        this.action = action;
        this.commFlag = 3;
    }

    public SndPkgNode(int action,byte[] message){
        this.action = action;
        this.message = message;
        this.commFlag = 3;
    }

    public SndPkgNode(int action,byte[] message,byte commFlag){
        this.action = action;
        this.message = message;
        this.commFlag = commFlag;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public byte getCommFlag() {
        return commFlag;
    }

    public void setCommFlag(byte commFlag) {
        this.commFlag = commFlag;
    }
}
