package com.xuhe.aace.handler;

import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.AacePacker.AaceHead;
import com.xuhe.aace.AaceServer;
import com.xuhe.aace.Logger;
import com.xuhe.aace.common.RcvPkgNode;
import com.xuhe.aace.common.RetCode;
import com.xuhe.aace.context.AaceContext;
import com.xuhe.aace.context.AsyncServer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class AaceHandler extends AsyncServer {

    //处理地址
    protected String uri;
    //代理名称
    protected String proxy;
    //端口号
    protected int port;
    //处理线程数
    protected int threadNum;
    //处理的队列
    protected NamedQueue<AaceHandlerNode> queue;



    protected int mode = AaceMgr.INTER_SERVER;
    protected AaceMgr aaceMgr;

    protected String interfaceName;
    protected String name;



    public AaceHandler(){

    }

    public AaceHandler(AaceHandler obj){
        this.aaceMgr = obj.aaceMgr;
        this.queue = obj.queue;
        this.proxy = obj.proxy;
        this.mode = obj.mode;
        this.name = obj.name;
    }


    public void setQueue(NamedQueue<AaceHandlerNode> queue){
        queue = queue;
        aaceMgr.addQueueMonitor(queue);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public NamedQueue<AaceHandlerNode> getQueue() {
        return queue;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean init(){
        if(uri == null || uri.isEmpty() || proxy == null || proxy.isEmpty() || interfaceName == null || interfaceName.isEmpty()){
            throw new RuntimeException("init aace caller error, uri or proxy or interface is null");
        }
        uri += "?proxy=" + proxy;
        if(port > 0) uri += "&port=" + port;
        this.aaceMgr = AaceServer.get();
        if(!registerHandler()){
            Logger.ErrLog("Register Handler error.");
            return false;
        }
        boolean retcode = true;
        UriNode node = UriParser.parse(uri);
        if(node == null){
            Logger.ErrLog("invalid uri:"+uri);
            return false;
        }
        proxy = node.getParams().get("proxy");
        if(name == null){
            name = proxy;
        }

        if(node.getMode() == UriParser.PROXY_SERVER){
            retcode = aaceMgr.getHolder().addProxy(proxy,interfaceName,ProxyHolder.HOLD_LOCAL, node.isEncrytp(), node.getHostInfoList(),null, null);
            if(!retcode){
                return false;
            }
        }else{
            retcode = aaceMgr.getHolder().addProxy(ProxyHolder.AACE_CENTER, interfaceName, ProxyHolder.HOLD_FLUSH,false,node.getHostInfoList(),null, null);
            if(!retcode){
                return false;
            }
            String hostIp = node.getParams().get("host");
            if(null == hostIp){
                hostIp = "";
            }
            int hostPort = 0;
            try{
                String port = node.getParams().get("port");
                if(null != port){
                    hostPort = Integer.valueOf(port);
                }
            }catch (NumberFormatException e){
                Logger.WarnLog("port invalid, uri format error:" + uri);
                return false;
            }
            List<HostInfo> hostInfoList = new ArrayList<>(1);
            hostInfoList.add(new HostInfo(hostIp,hostPort));
            if(!aaceMgr.getHolder().addProxy(proxy, interfaceName, ProxyHolder.HOLD_SERVER, node.isEncrytp(), hostInfoList, null, null)){
                Logger.ErrLog("add proxy error, uri:" + uri);
                return false;
            }
        }
        if(null == queue){
            this.setQueue(new NamedQueue<AaceHandlerNode>(name, AaceMgr.MAX_QUEUE_SIZE));
        }
        if(threadNum > 0){
            start(threadNum, name);
        }
        return true;
    }

    /**
     * 注册要开启的服务接口
     * @return
     */
    protected abstract boolean registerHandler();

    public boolean put(AaceHandlerNode node){
        try{
            if(queue.remainingCapacity() < 4){
                Logger.ErrLog("QUEUE "+ name + " is FULL! size= " + queue.size() + ", item[x]=" + queue.take());
                return false;
            }
            queue.put(node);
        }catch (InterruptedException e){
            return false;
        }
        return true;
    }


    @Override
    protected void process(AaceContext ctx) {
        AaceHandlerNode handlerNode = null;
        try {
            queue.take();
        } catch (InterruptedException e) {
            return;
        }
        MethodInfo methodInfo = handlerNode.getMethodInfo();
        if(null == methodInfo) return;

        RcvPkgNode rcvPkgNode =  handlerNode.getRcvPkgNode();
        ctx.setChannel(rcvPkgNode.getChannel());
        AaceHead aaceHead = handlerNode.getAaceHead();

        //如果该aaceHandlerNode 是 响应类型
        if(aaceHead.getCallType() == AaceMgr.CALL_RESPONSE){
            //响应
            methodInfo.setMode(AaceMgr.INTER_AGENT);
        }

        ctx.setSeqId(aaceHead.getSeqId());
        ctx.setCommFlag((byte)3);

        switch(methodInfo.getMode()){
            case AaceMgr.INTER_PROC:
                ctx.setRequest(rcvPkgNode.getMessage());
            case AaceMgr.INTER_SERVER:
                ctx.setParams(aaceHead.getReserved());
                ctx.setRetcode(RetCode.RET_INVALID);
                try{
                    Method method = methodInfo.getHandler().getClass().getMethod(methodInfo.getMethodName(),RcvPkgNode.class,AaceContext.class);
                    method.invoke(methodInfo.getHandler(),rcvPkgNode,ctx);
                }catch (Exception e){
                    Logger.LOGGER.error("AaceHandler.process call method={} ex",methodInfo,e);
                    ctx.setRetcode(RetCode.RET_FAILURE);
                }
                break;
            case AaceMgr.INTER_AGENT:
                try{
                    Method method = methodInfo.getHandler().getClass().getMethod(methodInfo.getMethodName(),RcvPkgNode.class,AaceHead.class,AaceContext.class);
                    method.invoke(methodInfo.getHandler(),rcvPkgNode,aaceHead,ctx);
                }catch (Exception e){
                    Logger.LOGGER.error("AaceHandler.process call method={} ex",methodInfo,e);
                    ctx.setRetcode(RetCode.RET_FAILURE);
                }
                break;
        }

        //如果该aaceHandlerNode 是 请求类型
        if(aaceHead.getCallType() == AaceMgr.CALL_REQUEST){
            int retcode = ctx.getRetcode();
            if(retcode == RetCode.RET_INVALID){
                String route = ctx.get(AaceMgr.PARAM_ROUTE);
                String hostInfo = "";
                try{
                    hostInfo = ctx.getChannel().getLocalAddress().toString();
                }catch (IOException e){}
                String info = hostInfo + ":" + aaceHead.getInterfaceName() + "." + aaceHead.getMethodName() + ",invalid";
                if(null == route){
                    route = info;
                }else{
                    route = info + ":" + route;
                }
                ctx.set(AaceMgr.PARAM_ROUTE,route);
            }
            aaceHead.setReserved(ctx.getParams());
            aaceMgr.response(rcvPkgNode.getChannel(),aaceHead,retcode,ctx.getResult(),ctx.getCommFlag());
        }
    }
}
