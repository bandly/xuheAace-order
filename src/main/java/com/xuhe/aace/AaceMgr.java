package com.xuhe.aace;

import com.xuhe.aace.AacePacker.AaceHead;
import com.xuhe.aace.common.SelectorStore;
import com.xuhe.aace.context.AaceContext;
import com.xuhe.aace.handler.*;

import java.nio.channels.SocketChannel;

/**
 * @author liq@shinemo.com
 * @version 1.0
 * @date 2019/8/30 11:37
 * @Description
 */
public interface AaceMgr {

    public static final int TCP_READ_BUFF_SIZE = 1048576;
    public static final int TCP_SEND_BUFF_SIZE = 1048576;
    public static final int MAX_QUEUE_SIZE = 1000;


    public static final int INTER_SERVER = 0;
    public static final int INTER_PROC = 1;
    public static final int INTER_AGENT = 2;

    //aaceHandlerNode  处理类型
    public static final byte CALL_REQUEST = 0;
    public static final byte CALL_RESPONSE = 1;
    public static final byte CALL_NOTIFY = 2;


    public static final String PARAM_MAIN = "_main";
    public static final String PARAM_SQL = "_sql";
    public static final String PARAM_DBERR = "_dberr";
    public static final String PARAM_ROUTE = "_route";
    public static final String PARAM_TTL = "_ttl";

    public SelectorStore getSelector();

    /**
     * 把接收到的socketchannel 插入到 readSelector 和 writeSelector 中
     * @param channel
     */
     void insertChannel(SocketChannel channel);

     ServerMgr getServerMgr();

     void response(SocketChannel channel, AaceHead aaceHead, int retcode, byte[] result, byte commFlag);

    /**
     * 向监视器中增加 队列
     * @param queue
     */
    void addQueueMonitor(NamedQueue<?> queue);

    /**
     * 获取proxyHolder
     * @return
     */
    ProxyHolder getHolder();

    /**
     * 增加监听 socketChannel
     * @param host
     * @param port
     * @return
     */
    int addListener(String host, int port);


    /**
     * 远程请求数据
     * @param channel
     * @param proxy
     * @param interfaceName
     * @param methodName
     * @param reqData
     * @param timeout
     * @param ctx
     * @return
     */
    ResponseNode syncRequest(SocketChannel channel, String proxy, String interfaceName, String methodName, byte[] reqData, int timeout, AaceContext ctx);
}
