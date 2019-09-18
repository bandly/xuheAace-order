package com.xuhe.aace.handler;

import com.xuhe.aace.AaceCallback;
import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.Logger;
import com.xuhe.aace.common.RetCode;
import com.xuhe.aace.common.SocketInfo;
import com.xuhe.aace.context.AaceContext;

import java.nio.channels.SocketChannel;

public class RequestNode {

    public static final int RUNTIME_THRESHOLDER = 500;
    private String proxy;
    private String interfaceName;
    private String methodName;
    private int mode;
    private Event event;
    private ResponseNode response;
    private AaceCallback callback;
    private SocketChannel channel;
    private long reqTime;


    public RequestNode(String proxy, String interfaceName, String methodName, Event event, ResponseNode response, SocketChannel channel, long reqTime) {
        this.proxy = proxy;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.mode = AaceMgr.CALL_SYNC;
        this.event = event;
        this.response = response;
        this.callback = null;
        this.channel = channel;
        this.reqTime = reqTime;
    }
    public RequestNode(String proxy, String interfaceName, String methodName, AaceContext ctx, AaceCallback callback, SocketChannel channel, long reqTime) {
        this.proxy = proxy;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.mode = AaceMgr.CALL_ASYNC;
        this.event = null;
        this.response = new ResponseNode(RetCode.RET_TIMEOUT);
        this.response.setContext(ctx);
        this.callback = callback;
        this.channel = channel;
        this.reqTime = reqTime;
    }

    public int incTtl(){
        if(null == response.getContext()){
            return 0;
        }
        String val = response.getContext().get(AaceMgr.PARAM_TTL);
        if(null == null){
            val = "0";
        }
        int ttl = Integer.valueOf(val) + 1;
        if(ttl > 9) ttl = 9;
        response.getContext().set(AaceMgr.PARAM_TTL, "" + ttl);
        return ttl;
    }

    public void decTtl(){
        if(null == response.getContext()){
            return;
        }
        String val = response.getContext().get(AaceMgr.PARAM_TTL);
        if(null == val){
            return;
        }
        int ttl = Integer.valueOf(val) - 1;
        if(ttl <= 0){
            response.getContext().take(AaceMgr.PARAM_TTL);
        }else{
            response.getContext().set(AaceMgr.PARAM_TTL, "" + ttl);
        }
    }

    public void addRoute(){
        if(null == response.getContext()){
            return;
        }
        String route = response.getContext().get(AaceMgr.PARAM_ROUTE);
        int rt = 0;
        String status = null;
        switch(response.getRetCode()){
            case RetCode.RET_DISCONN:
                status = "disconnect";
                break;
            case RetCode.RET_TIMEOUT:
                rt = (int)(System.currentTimeMillis() - reqTime);
                status = "timeout(" + rt + ")";
                break;
            default:
                rt = (int)(System.currentTimeMillis() - reqTime);
                if(rt < RUNTIME_THRESHOLDER && null == route){
                    return;
                }
                status = "" + rt;
        }
        String hostInfo = SocketInfo.getRemoteConnect(channel);
        String info = proxy + "(" + hostInfo + ")[" + interfaceName + "." + methodName + "]," + status;
        if(null == route || route.length() == 0){
            route = info;
        }else{
            route = info + ";" + route;
        }
        Logger.WarnLog("ROUTE: " + route);
    }


    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
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

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public ResponseNode getResponse() {
        return response;
    }

    public void setResponse(ResponseNode response) {
        this.response = response;
    }

    public AaceCallback getCallback() {
        return callback;
    }

    public void setCallback(AaceCallback callback) {
        this.callback = callback;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public long getReqTime() {
        return reqTime;
    }

    public void setReqTime(long reqTime) {
        this.reqTime = reqTime;
    }
}
