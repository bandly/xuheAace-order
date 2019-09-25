package com.xuhe.aace.handler;

import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.common.RetCode;
import com.xuhe.aace.common.TimerQueue;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    private TimerQueue<Long> timerQueue = new TimerQueue<>();


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
        timerQueue.insert(seqId, timeout);
        return true;
    }

    public RequestNode removeRequest(long seqId, String interfaceName, String methodName, int retCode){
        RequestNode requestNode = removeRequestMap(seqId, interfaceName, methodName);
        if(null == requestNode) return null;
        timerQueue.remove(seqId);
        requestNode.getResponse().setRetCode(retCode);
        if(Objects.equals(interfaceName, "AaceCheck")){
            requestNode.addRoute();
        }
        requestNode.decTtl();
        return requestNode;
    }


    public RequestNode removeRequest(long seqId){
        return removeRequestMap(seqId, null, null);
    }


    private RequestNode removeRequestMap(long seqId, String interfaceName, String methodName) {
        writeLock.lock();
        try{
            RequestNode requestNode = requestMap.get(seqId);
            if(null == requestNode){
                return null;
            }
            if((null != interfaceName && !Objects.equals(interfaceName, requestNode.getInterfaceName()))
                    || (null != methodName && !Objects.equals(methodName, requestNode.getMethodName()))){
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
            List<Long> seqIds = timerQueue.blockGet(1000);
            if(null == seqIds){
                continue;
            }
            Iterator<Long> iter = seqIds.iterator();
            while(iter.hasNext()){
                long seqId = iter.next();
                RequestNode node = removeRequest(seqId);
                node.getResponse().setRetCode(RetCode.RET_TIMEOUT);
                node.addRoute();
                node.decTtl();
                if(node.getMode() == AaceMgr.CALL_SYNC){
                    node.getEvent().lock();
                    node.getEvent().signal();
                    node.getEvent().lock();
                }else{
                    if(null != node.getCallback()){
                        //aaceMgr.getc
                    }
                }
            }
        }
    }


}
