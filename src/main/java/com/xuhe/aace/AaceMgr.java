package com.xuhe.aace;

import com.xuhe.aace.AacePacker.AaceHead;
import com.xuhe.aace.common.*;
import com.xuhe.aace.context.AaceContext;
import com.xuhe.aace.handler.*;

import javax.crypto.spec.SecretKeySpec;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Set;

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

    /**
     * 请求类型，同步还是异步
     */
    public static final int CALL_SYNC = 0;
    public static final int CALL_ASYNC = 1;



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

     SndPkgMgr getSendMgr();




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
     * 远程响应数据
     * @param channel
     * @param aaceHead
     * @param retcode
     * @param result
     * @param commFlag
     */
    void response(SocketChannel channel, AaceHead aaceHead, int retcode, byte[] result, byte commFlag);

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


    /**
     * 根据 socketChannel 获取 秘钥
     * @return
     */
    SecretKeySpec getKeyStore(SocketChannel channel);

    /**
     * 设置  socketChannel 秘钥
     * @return
     */
    void setKeyStore(SocketChannel channel, SecretKeySpec secretKeySpec);

    /**
     * 关闭通道
     * @param channel
     * @param callback
     */
    void shutdown(SocketChannel channel, boolean callback);


    /**
     * 消息接收
     * @param channel
     * @param msg
     */
    void onMessageRecv(SocketChannel channel, byte[] msg);


    boolean recvPackage(RcvPkgNode reqPack);
}
