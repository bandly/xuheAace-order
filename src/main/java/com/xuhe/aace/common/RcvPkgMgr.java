package com.xuhe.aace.common;

import com.sun.beans.editors.ByteEditor;
import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.Logger;
import com.xuhe.protocol.client.AaceCenterClient;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public class RcvPkgMgr extends Thread{

    public static final int PACK_FALSE = -1;
    public static final int PACK_CONT = 0;
    public static final int PACK_OK = 1;
    public static final int PACK_CHK = 2;



    private static class MsgBuffNode{
        transient boolean checked;
        transient int length;
        transient ByteBuffer headBuff;
        transient ByteBuffer buffer;

        public MsgBuffNode(){
            checked = false;
            length = 0;
        }

        public void clear(){
            checked = false;
            length = 0;
            headBuff = null;
            buffer = null;
        }
    }

    private AaceMgr aaceMgr;
    private int maxQueueSize;
    private ArrayBlockingQueue<RcvPkgNode> queue;
    private HashMap<SocketChannel, MsgBuffNode> msgBuffer = new HashMap<>();

    public RcvPkgMgr(AaceMgr aaceMgr, int maxQueueSize){
        super("Aace-RcvPkgMgr-T");
        this.aaceMgr = aaceMgr;
        this.maxQueueSize = maxQueueSize;
        this.queue = new ArrayBlockingQueue<>(maxQueueSize);
        this.setDaemon(true);
    }

    public boolean put(RcvPkgNode node){
        try{
            if(queue.size() >= maxQueueSize){
                return false;
            }
            queue.put(node);
        }catch (InterruptedException e){
            return false;
        }
        return true;
    }

    public void shutdown(SocketChannel channel){
        if(aaceMgr.getClass().equals(AaceCenterClient.class)){
            clearBuff(channel);
            return;
        }
        RcvPkgNode node = new RcvPkgNode(ActionType.SHUTDOWN, channel, null);
        put(node);
    }

    public int checkLen(MsgBuffNode cacheNode, ByteBuffer pkgBuff){
        int len = 0, exp = 0, count = 0;
        if(null == cacheNode.buffer){
            cacheNode.buffer = ByteBuffer.allocate(16);
        }
        ByteBuffer headBuff = cacheNode.buffer;
        headBuff.flip();
        while(headBuff.hasRemaining()){
            byte ch = headBuff.get();
            if((ch & 0x80) == 0){
                len += (ch << exp);
                return len;
            }
            ch &= ~0x80;
            len += (ch << exp);
            exp += 7;
            if(++count >= 8) return -2;
        }
        headBuff.limit(headBuff.capacity());
        while(pkgBuff.hasRemaining()){
            byte ch = pkgBuff.get();
            headBuff.put(ch);
            if((ch & 0x80) == 0){
                len += (ch << exp);
                return len;
            }
            ch &= ~0x80;
            len += (ch << exp);
            exp += 7;
            if(++count >= 8) return -2;
        }
        return -1;
    }

    private int checkHead(MsgBuffNode node){
        node.checked = true;
        return PACK_OK;
    }

    private int getPackage(MsgBuffNode cacheNode, ByteBuffer pkgBuff){
        if(cacheNode.length == 0){
            int len = checkLen(cacheNode, pkgBuff);
            if(len == -2) return PACK_FALSE;
            if(len < 0) return PACK_CONT;
            cacheNode.length = len;
            cacheNode.buffer = null;
            if(len == 0){
                cacheNode.clear();
                return PACK_CHK;
            }
        }
        int left = pkgBuff.remaining();
        int currLen = cacheNode.length;
        if(currLen > 1024*1024){
            currLen = 1024*1024;
            if(currLen < left) currLen = left;
        }
        int cacheLen = 0;
        if(null == cacheNode.buffer){
            cacheNode.buffer = ByteBuffer.allocate(currLen);
        }else{
            cacheLen = cacheNode.buffer.position();
            int minLen = cacheNode.length < cacheLen + left ? cacheNode.length : cacheLen + left;
            if(cacheNode.buffer.capacity() < minLen) {
                currLen = 2 * cacheNode.buffer.capacity();
                if(currLen < minLen) currLen = minLen;
                ByteBuffer newCache = ByteBuffer.allocate(currLen);
                cacheNode.buffer.flip();
                newCache.put(cacheNode.buffer);
                cacheNode.buffer = newCache;
            }
        }
        if(cacheNode.length <= cacheLen + left) {
            int appendLen = cacheNode.length - cacheLen;
            for(int i = 0; i < appendLen; i++) {
                cacheNode.buffer.put(pkgBuff.get());
            }
            cacheNode.buffer.flip();
            return PACK_OK;
        }
        cacheNode.buffer.put(pkgBuff);
        if(!cacheNode.checked) {
            if(checkHead(cacheNode) == PACK_FALSE) return PACK_FALSE;
        }
        return PACK_CONT;
    }

    protected boolean procPackage(RcvPkgNode pkgNode) {
        SocketChannel channel = pkgNode.getChannel();
        MsgBuffNode node = msgBuffer.get(channel);
        if(node == null) {
            node = new MsgBuffNode();
            msgBuffer.put(channel, node);
        }
        ByteBuffer pkgBuff = ByteBuffer.wrap(pkgNode.getMessage());
        while(pkgBuff.hasRemaining()) {
            int ret = getPackage(node, pkgBuff);
            switch(ret) {
                case PACK_CONT:
                    Logger.InfoLog("PkgHandler message length not enough, insert in MsgBuffer! rcvlen=" + pkgNode.getMessage().length + ",currlen=" + node.buffer.position() + ", totallen=" + node.length);
                    return true;
                case PACK_OK:
                {
                    RcvPkgNode reqPack = new RcvPkgNode(pkgNode.getAction(), channel, node.buffer.array());
                    aaceMgr.recvPackage(reqPack);
                    node.clear();
                    break;
                }
                case PACK_CHK:
                    //aaceMgr.onHealthCheck(channel);
                    break;
                default:
                    Logger.ErrLog("PkgHandler find one message head error! peer = "+ SocketInfo.getRemoteConnect(channel));
                    msgBuffer.remove(channel);
                    aaceMgr.shutdown(channel, true);
                    return false;

            }
        }
        return true;
    }
    public void clearBuff(SocketChannel channel) {
        msgBuffer.remove(channel);
    }
    public void run() {
        while(true) {
            try {
                RcvPkgNode node = null;
                try {
                    node = queue.take();
                } catch (InterruptedException e) {
                    continue;
                }
                if(node == null) continue;
                SocketChannel channel = node.getChannel();
                switch(node.getAction()) {
                    case ActionType.STARTUP_CONN:
                        break;
                    case ActionType.CLEAR_BUFFER:
                        clearBuff(channel);
                        break;
                    case ActionType.MESSAGE_RECV:
                        procPackage(node);
                        break;
                    default:
                        aaceMgr.recvPackage(node);
                        break;
                }
            } catch (Throwable e ) {
                Logger.LOGGER.error("RcvPkgMgr catch an unknown exception:", e);
            }

        }
    }

}
