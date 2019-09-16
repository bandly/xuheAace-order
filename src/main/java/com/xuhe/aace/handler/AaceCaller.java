package com.xuhe.aace.handler;

import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.AaceServer;
import com.xuhe.aace.Logger;


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





}
