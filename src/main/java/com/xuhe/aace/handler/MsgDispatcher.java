package com.xuhe.aace.handler;

import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.AacePacker.AaceHead;
import com.xuhe.aace.Logger;
import com.xuhe.aace.common.*;
import com.xuhe.aace.packer.PackData;
import com.xuhe.aace.packer.PackException;

import javax.crypto.spec.SecretKeySpec;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;

public class MsgDispatcher {

    private AaceMgr aaceMgr;

    private int maxQueueSize;

    private ArrayBlockingQueue<RcvPkgNode> queue = new ArrayBlockingQueue<>(AaceMgr.MAX_QUEUE_SIZE);

    public MsgDispatcher(AaceMgr aaceMgr, int maxQueueSize){
        this.aaceMgr = aaceMgr;
        this.maxQueueSize = maxQueueSize;
        this.queue = new ArrayBlockingQueue<>(maxQueueSize);
    }

    public void start(int threadNum){
        for(int i = 0; i < threadNum; i++){
            MsgDispatcherTask task = new MsgDispatcherTask(this);
            task.start();
        }
    }

    public boolean put(RcvPkgNode rcvPkgNode){
        try{
            if(queue.size() >= maxQueueSize){
                return false;
            }
            queue.put(rcvPkgNode);
        }catch (InterruptedException e){
            return false;
        }
        return true;
    }

    public void recvPackage(RcvPkgNode rcvPkgNode){
        SocketChannel channel = rcvPkgNode.getChannel();
        if(!preProcess(rcvPkgNode)){
            SndPkgNode sendNode = new SndPkgNode(ActionType.SHUTDOWN);
            //aaceMgr.();
            return;
        }
        AaceHandlerNode handlerNode = new AaceHandlerNode();
        handlerNode.setRcvPkgNode(rcvPkgNode);
        AaceHead aaceHead = new AaceHead();
        if(rcvPkgNode.getAction() == ActionType.HEALTH_CHECK){
            aaceHead.setCallType(AaceMgr.CALL_NOTIFY);
            aaceHead.setInterfaceName("AaceInterface");
            aaceHead.setMethodName("check");
            aaceHead.setSeqId(0);
        }else{
            PackData packData = new PackData();
            packData.resetInBuff(rcvPkgNode.getMessage());
            try{
                aaceHead.unpackData(packData);
            }catch (PackException e){
                Logger.LOGGER.error("MsgDispatcher.receive an invalid package from {}", SocketInfo.getRemoteConnect(channel), e);
                aaceMgr.shutdown(rcvPkgNode.getChannel(), false);
                return;
            }
            int pos = packData.getInCursor();
            int len = rcvPkgNode.getMessage().length - pos;
            byte[] message = new byte[len];
            System.arraycopy(rcvPkgNode.getMessage(), pos, message, 0, len);
            rcvPkgNode.setMessage(message);
        }
        handlerNode.setAaceHead(aaceHead);
        if(aaceHead.getCallType() == AaceMgr.CALL_RESPONSE){
            RequestNode requestNode = aaceMgr.getRequestMgr().removeRequest(aaceHead.getSeqId(), aaceHead.getInterfaceName(), aaceHead.getMethodName(), RetCode.RET_SUCESS);
            if(null != requestNode){
                requestNode.getResponse().setRspData(rcvPkgNode.getMessage());
                requestNode.getResponse().addParams(aaceHead.getReserved());
                if(requestNode.getMode() == AaceMgr.CALL_SYNC){
                    requestNode.getEvent().lock();
                    requestNode.getEvent().signal();
                    requestNode.getEvent().unlock();
                }else{
                    if(null != requestNode.getCallback()){
                        //aaceMgr.getCllbackHandler().put(requestNode);
                    }
                }
                return;

            }
            Logger.WarnLog("response not match: "+ aaceHead.getSeqId());
        }
        putHandler(handlerNode);
    }

    private boolean putHandler(AaceHandlerNode handlerNode) {
        AaceHead aaceHead = handlerNode.getAaceHead();
        RcvPkgNode rcvPkgNode = handlerNode.getRcvPkgNode();
        MethodInfo methodInfo = new MethodInfo();

        int retCode = aaceMgr.getInterfaceMgr().getHandler(aaceHead.getInterfaceName(), aaceHead.getMethodName(), methodInfo);
        if(retCode != RetCode.RET_SUCESS){
            if(aaceHead.getCallType() == AaceMgr.CALL_RESPONSE) return false;
            SocketChannel channel = rcvPkgNode.getChannel();
            Logger.ErrLog("call failure, No method "+ aaceHead.getInterfaceName() + "." + aaceHead.getMethodName() +" found. from " + SocketInfo.getRemoteConnect(channel));
            if(aaceHead.getCallType() == AaceMgr.CALL_REQUEST){
                aaceMgr.response(channel, aaceHead, retCode, null, (byte)3);
            }
            return false;
        }
        if(methodInfo.getMode() != AaceMgr.INTER_AGENT && aaceHead.getCallType() == AaceMgr.CALL_RESPONSE){
            Logger.ErrLog(aaceHead.getInterfaceName() + "." + aaceHead.getMethodName() + " response too late. seqid=" + aaceHead.getSeqId());
            return false;
        }

        handlerNode.setMethodInfo(methodInfo);
        return methodInfo.getHandler().put(handlerNode);
    }

    private boolean preProcess(RcvPkgNode rcvPkgNode){
        byte[] message = rcvPkgNode.getMessage();
        SocketChannel channel = rcvPkgNode.getChannel();
        SecretKeySpec key = aaceMgr.getKeyStore(channel);
        if(null != key){
            byte[] buff = Security.getAESDecryptContent(key, message);
            if(null != buff){
                rcvPkgNode.setMessage(buff);
            }
        }
        return true;
    }




    public ArrayBlockingQueue<RcvPkgNode> getQueue() {
        return queue;
    }
}
