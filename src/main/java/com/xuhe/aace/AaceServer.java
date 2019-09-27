package com.xuhe.aace;

import com.xuhe.aace.common.*;
import com.xuhe.aace.handler.*;


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

        serverMgr = new ServerMgr(this);

        interfaceMgr = new InterfaceMgr();

        socketKeyStore = new SocketKeyStore();

        requestMgr = new RequestMgr(this);

        sndPkgMgr = new SndPkgMgr(this, TCP_READ_BUFF_SIZE, MAX_QUEUE_SIZE);

        tcpMsgWriter = new TcpMsgWriter(this);

        recvMgr = new RcvPkgMgr(this, MAX_QUEUE_SIZE);

        tcpMsgReader = new TcpMsgReader(this);

        msgDispatcher = new MsgDispatcher(this, MAX_QUEUE_SIZE);

        timerHandler = new TimerHandler();

        listener.start();

        tcpMsgWriter.start();

        recvMgr.start();

        tcpMsgReader.start();

        msgDispatcher.start(1);

        timerHandler.addTask(proxyHolder, 5000);
        timerHandler.start();


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



}
