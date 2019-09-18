package com.xuhe.aace.common;

import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.Logger;
import com.xuhe.aace.handler.Event;
import com.xuhe.aace.packer.PackData;

import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashSet;

/**
 * 发送包管理
 */
public class SndPkgMgr {

    private AaceMgr aaceMgr;
    private int maxBuffSize;
    private MsgQueue queue;
    private Event lock = new Event();
    private HashSet<SocketChannel> writeChannels = new HashSet<>();

    public void SndPkgNode(AaceMgr aaceMgr, int maxBuffSize, int maxQueueSize){
        this.aaceMgr = aaceMgr;
        this.maxBuffSize = maxBuffSize;
        this.queue = new MsgQueue(maxQueueSize);
    }
    public void preProcess(SocketChannel channel, SndPkgNode node){
        if(null == node.getMessage()
                || node.getMessage().length == 0
                || node.getAction() != ActionType.MESSAGE_SEND
                || (node.getCommFlag() & 1) == 0){
            return ;
        }
        SecretKeySpec key = aaceMgr.getKeyStore(channel);
        if(null != key){
            byte[] message = Security.getAESEncryptContent(key, node.getMessage());
            if(null != message){
                node.setMessage(message);
            }
        }
    }

    public boolean putSndPkgNode(SocketChannel channel, SndPkgNode node){
        switch (node.getAction()){
            case ActionType.SET_ENCKEY:
            {
                SecretKeySpec key = Security.getAESKey(node.getMessage());
                if(null == key){
                    return false;
                }
                aaceMgr.setKeyStore(channel, key);
                return true;
            }
            case ActionType.SHUTDOWN:
            {
                aaceMgr.shutdown(channel, true);
                return true;
            }
            default:
                break;
        }
        preProcess(channel, node);
        lock.lock();

        try{
            if(!queue.putSndPkgNode(channel, node)){
                Logger.ErrLog("Message Queue FULL "+ SocketInfo.getRemoteConnect(channel));
                return false;
            }
            writeChannels.add(channel);
            ///唤醒一个在 await()等待队列中的线程。与Object.notify()相似
            lock.signal();
            return true;
        }finally {
            lock.unlock();
        }
    }

    public void join(ByteBuffer buffer, SndPkgNode node){
        byte[] message = node.getMessage();
        if(null == message){
            buffer.put((byte)0);
        }else{
            byte[] lenBuff = PackData.PackInt(message.length);
            buffer.put(lenBuff);
            buffer.put(message);
        }
    }








}
