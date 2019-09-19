package com.xuhe.aace.handler;

import com.xuhe.aace.AaceMgr;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 服务请求管理
 */
public class RequestMgr extends Thread{

    private AaceMgr aaceMgr;

    private final ReentrantReadWriteLock reqLock = new ReentrantReadWriteLock();

    private final Lock writeLock = reqLock.writeLock();

    private HashMap<Long, RequestNode> requestMap = new HashMap<>();

    public RequestMgr(AaceMgr aaceMgr){
        this.aaceMgr = aaceMgr;
        this.setDaemon(true);
    }

    public boolean addRequest(long seqId, String proxy, String interfaceName, String methodName, Event event, ResponseNode response, SocketChannel channel, long reqTime, int timeout){
        RequestNode requestNode = new RequestNode(proxy, interfaceName, methodName, event, response, channel, reqTime);
        if(!putRequestMap(seqId, requestNode)){
            return false;
        }
        int ttl = requestNode.incTtl();
        timeout += 100 - ttl * 10;
        return true;
    }

    public RequestNode removeRequest(long seqId, String interfaceName, String methodName, int retCode){
        RequestNode requestNode = removeRequestMap(seqId, interfaceName, methodName);
        if(null == requestNode) return null;
        requestNode.getResponse().setRetCode(retCode);
        if(Objects.equals(interfaceName, "AaceCheck")){
            requestNode.addRoute();
        }
        requestNode.decTtl();
        return requestNode;
    }

    private RequestNode removeRequestMap(long seqId, String interfaceName, String methodName) {
        writeLock.lock();
        try{
            RequestNode requestNode = requestMap.get(seqId);
            if(null == requestNode){
                return null;
            }
            if((null != interfaceName && !Objects.equals(interfaceName, requestNode.getInterfaceName()))
                    || (null != methodName && Objects.equals(methodName, requestNode.getMethodName()))){
                return null;
            }
            requestMap.remove(seqId);
            return requestNode;
        }finally {
            writeLock.unlock();
        }
    }


    private boolean putRequestMap(long seqId, RequestNode requestNode) {
        writeLock.lock();
        try{
            if(requestMap.containsKey(seqId)){
                return false;
            }
            requestMap.put(seqId, requestNode);
            return true;
        }finally {
            writeLock.unlock();
        }
    }

    public void run(){
        while (true){
            //List<Long> seqIds =
        }
    }


}
