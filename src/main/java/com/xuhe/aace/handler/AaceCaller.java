package com.xuhe.aace.handler;

import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.AaceServer;
import com.xuhe.aace.Logger;
import com.xuhe.aace.common.RetCode;
import com.xuhe.aace.context.AaceContext;
import com.xuhe.aace.packer.PackData;

import java.nio.channels.SocketChannel;


public class AaceCaller {

    public final static int DEFAULT_TIMEOUT = 5000;
    protected AaceMgr aaceMgr;

    protected int mode;
    protected boolean distributed = false;

    protected String uri;
    protected String proxy;
    protected int port;
    protected String interfaceName;

    public boolean init(){
        if(uri == null || uri.isEmpty()
                || proxy == null || proxy.isEmpty()
                || interfaceName == null || interfaceName.isEmpty()){
            throw new RuntimeException("init aace caller error, uri or proxy or interface is null");
        }
        uri += "?proxy=" + proxy;
        if (port > 0) uri += "&port=" + port;
        this.aaceMgr = AaceServer.get();
        boolean retCode = true;
        UriNode node = UriParser.parse(uri);
        if(null == node){
            Logger.ErrLog("invalid uri:" + uri);
            return false;
        }
        if(node.getHostInfoList().isEmpty()){
            Logger.ErrLog("no valid host specified. uri=" + uri);
            return false;
        }
        proxy = node.getParams().get("proxy");
        if(null == proxy){
            Logger.ErrLog("no proxy specified in url "+ uri);
            return false;
        }

        if(node.getMode() == UriParser.PROXY_SERVER){
            mode = ProxyHolder.HOLD_DIRECT;
        } else{
            mode = ProxyHolder.HOLD_CLIENT;
        }
        retCode = aaceMgr.getHolder().addProxy(proxy, interfaceName, mode, node.isEncrytp(), node.getHostInfoList(), null, null);
        return retCode;
    }

    protected ResponseNode invoke(int timeout, AaceContext ctx, String methodName, Object... paramArr) {
        byte[] reqData = pack(paramArr);
        SocketChannel channel = getProxyServer(ctx);
        if(null == channel){
            Logger.ErrLog("no proxy" + proxy + " found. call " + interfaceName + "." + methodName + " error");
            return new ResponseNode(RetCode.RET_DISCONN);
        }
        return invoke(channel, methodName, reqData, timeout, ctx);
    }

    /**
     * 通用组装数据包的方法
     * @param paramArr
     * @return
     */
    private byte[] pack(Object[] paramArr) {
        byte fieldNum = (byte) paramArr.length;
        int packSize = 1;

        for(Object obj : paramArr){
            packSize += 1;
            packSize += PackData.getSize(interfaceName);
        }
        PackData packData = new PackData();
        byte[] reqData = new byte[packSize];
        packData.resetOutBuff(reqData);
        packData.packByte(fieldNum);
        for(Object obj : paramArr){
            packData.packByte(PackData.getPackType(obj));
            packData.packObject(obj);
        }
        return reqData;
    }




    /**
     * 方法调用
     * @param methodName
     * @param reqData
     * @param timeout
     * @param ctx
     * @return
     */
    protected ResponseNode invoke(String methodName, byte[] reqData, int timeout, AaceContext ctx) {
        SocketChannel channel = getProxyServer(ctx);
        if(null == channel){
            Logger.ErrLog("no proxy" + proxy + " found. call " + interfaceName + "." + methodName + " error");
            return new ResponseNode(RetCode.RET_DISCONN);
        }
        return invoke(channel, methodName, reqData, timeout, ctx);
    }





    protected ResponseNode invoke(SocketChannel channel, String methodName, byte[] reqData, int timeout, AaceContext ctx) {
        ResponseNode responseNode = aaceMgr.syncRequest(channel, proxy, interfaceName, methodName, reqData, timeout, ctx);
        if(responseNode.getRetCode() != RetCode.RET_SUCESS){
            Logger.ErrLog("call " + interfaceName + "." + methodName + " error.ret=" + responseNode.getRetCode());
        }
        return responseNode;
    }

    protected SocketChannel getProxyServer(AaceContext ctx) {
        SocketChannel channel = null;
        Long hashval = null;
        if(null != ctx){
            hashval = ctx.getHashval();
        }
        if(null == hashval || !distributed){
            channel = getProxyServer(proxy, ServerMgr.STS_WORKING);
            if(null != channel){
                distributed = false;
                return channel;
            }
        }
        if(null == hashval){
            return null;
        }
        channel = getDeployServer(hashval.longValue(), ServerMgr.STS_WORKING);
        distributed = (null != channel);
        return channel;
    }

    protected SocketChannel getDeployServer(long hashval, int status) {
        return aaceMgr.getServerMgr().getDistProxyChannel(proxy, interfaceName, hashval, status);
    }

    protected SocketChannel getProxyServer(String proxy, int status){
        return aaceMgr.getServerMgr().getProxyChannel(proxy, interfaceName, status);
    }




}
