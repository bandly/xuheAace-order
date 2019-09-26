package com.xuhe.aace;

import com.xuhe.aace.AacePacker.AaceHead;
import com.xuhe.aace.common.*;
import com.xuhe.aace.context.AaceContext;
import com.xuhe.aace.handler.*;
import com.xuhe.aace.packer.PackData;

import javax.crypto.spec.SecretKeySpec;
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
     * socket 秘钥存储器
     */
    protected SocketKeyStore socketKeyStore;

    /**
     * 远程请求管理服务
     */
    protected RequestMgr requestMgr;


    /**
     * 发送包管理
     */
    protected SndPkgMgr sndPkgMgr;

    /**
     * tcp消息写入 线程
     */
    protected TcpMsgWriter tcpMsgWriter;

    protected RcvPkgMgr recvMgr;

    protected TcpMsgReader tcpMsgReader;


    protected InterfaceMgr interfaceMgr;

    protected MsgDispatcher msgDispatcher;






    public SelectorStore getSelector() {
        return selector;
    }


    @Override
    public ServerMgr getServerMgr() {
        return serverMgr;
    }

    @Override
    public ProxyHolder getHolder() {
        return proxyHolder;
    }

    @Override
    public RequestMgr getRequestMgr() {
        return requestMgr;
    }

    @Override
    public SndPkgMgr getSendMgr() {
        return sndPkgMgr;
    }

    @Override
    public InterfaceMgr getInterfaceMgr(){
        return interfaceMgr;
    }


    @Override
    public int addListener(String host, int port) {
        return selector.addListener(host, port);
    }


    @Override
    public void insertChannel(SocketChannel channel) {
        selector.insert(channel,SelectorStore.RW_EVN);
    }

    @Override
    public void response(SocketChannel channel, AaceHead aaceHead, int retcode, byte[] result, byte commFlag) {
        aaceHead.setCallType(CALL_RESPONSE);
        int len = aaceHead.size() + PackData.getSize(retcode);
        if(null != result) len += result.length;
        byte[] msg = new byte[len];
        PackData packData = new PackData();
        packData.resetOutBuff(msg);
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
        while(!requestMgr.addRequest(seqId, proxy, interfaceName, methodName, event, responseNode, channel, currTime, timeout * 1000)){
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
        packData.resetOutBuff(msg);
        aaceHead.packData(packData);
        int len = packData.getOutCursor();
        System.arraycopy(reqData, 0, msg, len, reqData.length);
        SndPkgNode node = null;
        if(null != ctx){
            node = new  SndPkgNode(ActionType.MESSAGE_SEND, msg, ctx.getCommFlag());
        }else{
            node  = new SndPkgNode(ActionType.MESSAGE_SEND, msg);
        }
        if(!sndPkgMgr.putSndPkgNode(channel, node)){
            Logger.ErrLog("error send request to "+ SocketInfo.getRemoteConnect(channel) + ", methodName=" + interfaceName + ":" + methodName);
            return RetCode.RET_DISCONN;
        }
        return RetCode.RET_SUCESS;
    }

    public void onMessageRecv(SocketChannel channel, byte[] msg){
        RcvPkgNode node = new RcvPkgNode(ActionType.MESSAGE_RECV, channel, msg);
        System.out.println(new String(msg) + "------------" );
        recvMgr.put(node);
    }

    public boolean recvPackage(RcvPkgNode node) {
        if(node.getMessage() == null) {
            return false;
        }
        if(!msgDispatcher.put(node)) {
            Logger.WarnLog("DISPATCHER QUEUE FULL.");
            return false;
        }
        return true;
    }



    public void shutdown(SocketChannel channel, boolean callback){
        Logger.WarnLog("shutdown connect." + SocketInfo.getRemoteConnect(channel));
        selector.remove(channel, SelectorStore.WR_EVN);
        //recvMgr_.shutdown(channel);
        //sendMgr_.shutdown(channel);
        //serverMgr_.shutdown(channel, callback);
    }


    /**
     * 根据 socketChannel 获取 秘钥
     * @return
     */
    public SecretKeySpec getKeyStore(SocketChannel channel){
        return socketKeyStore.getKey(channel);
    }

    /**
     * 设置  socketChannel 秘钥
     * @return
     */
    public void setKeyStore(SocketChannel channel, SecretKeySpec secretKeySpec){
        socketKeyStore.setKey(channel, secretKeySpec);
    }


    public int notify(SocketChannel channel, String interfaceName, String method, byte[] reqData){
        return sendRequest(0, channel, CALL_NOTIFY, interfaceName, method,reqData, null);
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
