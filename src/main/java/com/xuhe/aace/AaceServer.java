package com.xuhe.aace;

import com.xuhe.aace.common.ConnListener;
import com.xuhe.aace.common.SelectorStore;
import com.xuhe.aace.context.AaceContext;
import com.xuhe.aace.handler.Event;
import com.xuhe.aace.handler.ProxyHolder;
import com.xuhe.aace.handler.QueueMonitor;
import com.xuhe.aace.handler.ResponseNode;
import com.xuhe.aace.handler.ServerMgr;

import java.awt.*;
import java.nio.channels.SocketChannel;

/**
 * @author liq@shinemo.com
 * @version 1.0
 * @date 2019/8/30 11:35
 * @Description aace server端服务类
 */
public class AaceServer extends AaceMgrImpl {

    private AaceServer(){


        selector = new SelectorStore(TCP_READ_BUFF_SIZE, TCP_SEND_BUFF_SIZE);

        //监听socketChanel 链接
        listener = new ConnListener(this,TCP_READ_BUFF_SIZE,TCP_SEND_BUFF_SIZE);

        queueMonitor = new QueueMonitor();

        proxyHolder = new ProxyHolder(this);

        serverMgr = new ServerMgr();

        listener.start();


    }

    /**
     * 单例实例
     */
    private static volatile AaceServer instance;

    /**
     * 双重检查 获取单例对象
     * @return
     */
    public  static AaceMgr get() {
        if (null == instance) {
            synchronized (AaceServer.class){
                if(null == instance){
                    instance = new AaceServer();
                }
            }
        }
        return instance;
    }

    @Override
    public ProxyHolder getHolder() {
        return proxyHolder;
    }

    @Override
    public int addListener(String host, int port) {
        return selector.addListener(host, port);
    }

    @Override
    public ResponseNode syncRequest(SocketChannel channel, String proxy, String interfaceName, String methodName, byte[] reqData, int timeout, AaceContext ctx) {
        Event event = new Event();
        long currTime = System.currentTimeMillis();
        long seqId = getSeqId(currTime);


        return null;
    }

    private long getSeqId(long currTime) {
        int cnt =
    }
}
