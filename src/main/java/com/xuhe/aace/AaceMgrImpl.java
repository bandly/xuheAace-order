package com.xuhe.aace;

import com.xuhe.aace.AacePacker.AaceHead;
import com.xuhe.aace.common.*;
import com.xuhe.aace.context.AaceContext;
import com.xuhe.aace.handler.*;
import com.xuhe.aace.packer.PackData;

import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liq@shinemo.com
 * @version 1.0
 * @date 2019/8/30 11:38
 * @Description
 */
public abstract class AaceMgrImpl implements AaceMgr {

    private static AtomicInteger counter = new AtomicInteger(0);

    /**
     * nio select 选择器存储
     */
    protected SelectorStore selector;

    /**
     * socketChannel 连接监听器
     */
    protected ConnListener listener;

    /**
     *  处理队列监听器
     */
    protected QueueMonitor queueMonitor;

    /**
     * 代理holder
     */
    protected ProxyHolder proxyHolder;

    /**
     * sever 管理者
     */
    protected ServerMgr serverMgr;

    /**
     * 远程请求管理服务
     */
    protected RequestMgr requestMgr;

    /**
     * socket 秘钥存储器
     */
    protected SocketKeyStore socketKeyStore;


    public SelectorStore getSelector() {
        return selector;
    }

    @Override
    public void insertChannel(SocketChannel channel) {
        selector.insert(channel,SelectorStore.RW_EVN);
    }

    @Override
    public ServerMgr getServerMgr() {
        return serverMgr;
    }

    @Override
    public void response(SocketChannel channel, AaceHead aaceHead, int retcode, byte[] result, byte commFlag) {
        aaceHead.setCallType(CALL_RESPONSE);
        int len = aaceHead.size() + PackData.getSize(retcode);
        if(null != result) len += result.length;
        byte[] msg = new byte[len];
        PackData packData = new PackData();
        packData.resetOutBUff(msg);
        aaceHead.packData(packData);
        if(retcode != RetCode.RET_UNUSE){
            packData.packInt(retcode);
        }
        if(null != result){
            System.arraycopy(result,0,msg,packData.getOutCursor(),result.length);
        }
        SndPkgNode node = new SndPkgNode(ActionType.MESSAGE_SEND,msg,commFlag);
        //



    }

    @Override
    public void addQueueMonitor(NamedQueue<?> queue) {
        queueMonitor.registerQueue(queue);
    }

    @Override
    public ResponseNode syncRequest(SocketChannel channel, String proxy, String interfaceName, String methodName, byte[] reqData, int timeout, AaceContext ctx) {
        Event event = new Event();
        long currTime = System.currentTimeMillis();
        long seqId = genSeqId(currTime);
        ResponseNode responseNode = new ResponseNode(RetCode.RET_TIMEOUT);
        responseNode.setContext(ctx);
        while(!requestMgr.addRequest(seqId, proxy, interfaceName, methodName, event, responseNode, channel, currTime, timeout)){
            seqId = genSeqId(currTime);
        }
        event.lock();
        try{
            int retCode = sendRequest(seqId, channel, CALL_REQUEST, interfaceName, methodName, reqData, ctx);
            if(retCode != RetCode.RET_SUCESS){


                requestMgr.removeRequest(seqId, interfaceName, methodName, retCode);
                return responseNode;
            }

            event.timeWait(timeout + 100);
        }finally {
            event.unlock();
        }

        if(null != ctx){
            ctx.getParams().putAll(responseNode.getContext().getParams());
        }

        return responseNode;
    }

    private int sendRequest(long seqId, SocketChannel channel, byte callRequestType, String interfaceName, String methodName, byte[] reqData, AaceContext ctx) {
        AaceHead aaceHead = new AaceHead();
        aaceHead.setCallType((byte)callRequestType);
        aaceHead.setSeqId(seqId);
        aaceHead.setInterfaceName(interfaceName);
        aaceHead.setMethodName(methodName);
        if(null != ctx) aaceHead.setReserved((ctx.getParams()));

        byte[] msg = new byte[aaceHead.size() + reqData.length];
        PackData packData = new PackData();
        packData.resetOutBUff(msg);
        aaceHead.packData(packData);
        int len = packData.getOutCursor();
        System.arraycopy(reqData, 0, msg, len, reqData.length);
        SndPkgNode node = null;
        if(null != ctx){
            node = new  SndPkgNode(ActionType.MESSAGE_SEND, msg, ctx.getCommFlag());
        }else{
            node  = new SndPkgNode(ActionType.MESSAGE_SEND, msg);
        }
        if(!put(channel, node)){
            Logger.ErrLog("error send request to "+ SocketInfo.getRemoteConnect(channel) + ", methodName=" + interfaceName + ":" + methodName);
            return RetCode.RET_DISCONN;
        }
        return RetCode.RET_SUCESS;
    }

    private boolean put(SocketChannel channel, SndPkgNode node) {
        return false;
    }

    public void shutdown(SocketChannel channel, boolean callback){
        Logger.WarnLog("shutdown connect." + SocketInfo.getRemoteConnect(channel));
        selector.remove(channel, SelectorStore.WR_EVN);
        //recvMgr_.shutdown(channel);
        //sendMgr_.shutdown(channel);
        //serverMgr_.shutdown(channel, callback);
    }



    private long genSeqId(long currTime) {
        int cnt = counter.incrementAndGet();
        if(cnt < 0){
            counter.set(0);
            cnt = 0;
        }
        long val = currTime / 1000;
        return (val << 31) + cnt;
    }




}
