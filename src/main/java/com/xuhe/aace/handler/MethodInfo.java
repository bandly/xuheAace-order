package com.xuhe.aace.handler;

import com.xuhe.aace.AaceMgr;

public class MethodInfo {

    private AaceHandler handler;

    private String methodName;

    private int mode;

    public MethodInfo(){
        this.mode = AaceMgr.INTER_SERVER;
    }

    public MethodInfo(AaceHandler obj,int mode){
        this.handler = obj;
        this.mode = mode;
    }

    public AaceHandler getHandler() {
        return handler;
    }

    public void setHandler(AaceHandler handler) {
        this.handler = handler;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "MethodInfo{" +
                "handler=" + handler +
                ", methodName='" + methodName + '\'' +
                ", mode=" + mode +
                '}';
    }
}
