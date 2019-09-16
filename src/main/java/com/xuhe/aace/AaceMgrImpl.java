package com.xuhe.aace;

import com.xuhe.aace.AacePacker.AaceHead;
import com.xuhe.aace.common.*;
import com.xuhe.aace.handler.NamedQueue;
import com.xuhe.aace.handler.ProxyHolder;
import com.xuhe.aace.handler.QueueMonitor;
import com.xuhe.aace.handler.ServerMgr;
import com.xuhe.aace.packer.PackData;

import java.nio.channels.SocketChannel;

/**
 * @author liq@shinemo.com
 * @version 1.0
 * @date 2019/8/30 11:38
 * @Description
 */
public abstract class AaceMgrImpl implements AaceMgr {


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
}
