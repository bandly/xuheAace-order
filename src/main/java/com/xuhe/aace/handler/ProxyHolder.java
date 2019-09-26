package com.xuhe.aace.handler;

import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.Logger;
import com.xuhe.aace.common.RetCode;
import com.xuhe.protocol.client.AaceCenterClient;
import com.xuhe.protocol.model.ServerInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ProxyHolder implements TimerTask {



    public static final int HOLD_UNKNOWN = -1;
    public static final int HOLD_DIRECT = 1;
    public static final int HOLD_DOMAIN = 2;
    public static final int HOLD_CLIENT = 3;
    public static final int HOLD_LOCAL = 4; //本地
    public static final int HOLD_SERVER = 5;
    public static final int HOLD_FLUSH = 6;
    public static final String AACE_CENTER = "AaceCenter";

    private AaceMgr aaceMgr;

    private ReentrantLock proxyLock = new ReentrantLock();
    private HashMap<String,List<ProxyHolderNode>> proxyMap = new HashMap<String,List<ProxyHolderNode>>();


    private AaceCenterClient aaceCenterClient;


    public ProxyHolder(AaceMgr aaceMgr){
        this.aaceMgr = aaceMgr;
        aaceCenterClient = new AaceCenterClient(this.aaceMgr);
        //aaceCenterClient.init();

    }

    @Override
    public void process(Object obj) {
        checkAllProxy();
    }

    /**
     * 检测所有的代理
     */
    private void checkAllProxy() {
        HashMap<String, List<ProxyHolderNode>> proxies = new HashMap<>();
        proxyLock.lock();
        try{
            Iterator<Map.Entry<String,List<ProxyHolderNode>>> iter = proxyMap.entrySet().iterator();
            while(iter.hasNext()){
                Map.Entry<String,List<ProxyHolderNode>> entry = iter.next();
                List<ProxyHolderNode> nodes = new ArrayList<>(entry.getValue());
                proxies.put(entry.getKey(),nodes);
            }
        }finally {
            proxyLock.unlock();
        }
        Iterator<Map.Entry<String,List<ProxyHolderNode>>> iter = proxies.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry<String,List<ProxyHolderNode>> entry = iter.next();
            List<ProxyHolderNode> nodes = entry.getValue();
            for(ProxyHolderNode node : nodes){
                checkProxy(entry.getKey(),node);
            }
        }
    }

    private boolean checkProxy(String proxy, ProxyHolderNode node) {
        switch(node.getMode()){
            case HOLD_DIRECT:
                return checkDirectProxy(proxy,node);
            case HOLD_CLIENT:
                return checkClientProxy(proxy,node);
            case HOLD_SERVER:
                return checkServerProxy(proxy,node);
            case HOLD_DOMAIN:
                //return checkDomainProxy(proxy,node);
            case HOLD_LOCAL:
                return true;
            case HOLD_FLUSH:
                return checkFlushProxy(proxy,node);
        }
        return false;
    }

    private boolean checkServerProxy(String proxy, ProxyHolderNode node) {
        String interfaceName = node.getInterfaceName();
        ServerMgr serverMgr = aaceMgr.getServerMgr();
        //获取改interfaceName 到注册中心的所有socketChannel 的连接
        List<SocketChannel> centers = serverMgr.getProxyChannels(AACE_CENTER, interfaceName, ServerMgr.STS_AVAILABLE);
        if(null == centers || centers.isEmpty()){
            Logger.ErrLog("error: lost center connect");
            return false;
        }
        String srvHost = null;
        for(HostInfo hostInfo :node.getHostInfoList()){
            String host = hostInfo.getHost();
            if(host.isEmpty()){
                if(null == srvHost){
                    host = getLocalIp(interfaceName, host);
                    if(null == host) return false;
                    srvHost = host;
                    setHost(proxy, interfaceName, host);
                }
            }else{
                host = srvHost;
            }
            int status = serverMgr.getStatus();
            //重新向注册中心注册服务状态
            aaceCenterClient.registerProxyServer(host, String.valueOf(hostInfo.getPort()), proxy, interfaceName, status, centers);
        }
        return true;
    }

    /**
     * 设置代理 proxyHolderNode 中 hostInfo 的host
     * @param proxy
     * @param interfaceName
     * @param host
     */
    private void setHost(String proxy, String interfaceName, String host) {
        proxyLock.lock();
        try{
            List<ProxyHolderNode> nodes = proxyMap.get(proxy);
            for(ProxyHolderNode node : nodes){
                if(!isEqual(node.getInterfaceName(), interfaceName)
                        || (node.getMode() != HOLD_SERVER && node.getMode() != HOLD_LOCAL)){
                    continue;
                }
                for(HostInfo hostInfo : node.getHostInfoList()){
                    if(isEmpty(hostInfo.getHost())){
                        hostInfo.setHost(host);
                    }
                }
            }
        }finally {
            proxyLock.unlock();
        }

    }

    private boolean checkFlushProxy(String proxy, ProxyHolderNode node) {
        String interfaceName = node.getInterfaceName();
        ServerMgr serverMgr = aaceMgr.getServerMgr();
        SocketChannel channel = serverMgr.getProxyChannel(proxy, interfaceName, serverMgr.STS_AVAILABLE);
        if(null == channel){
            for(HostInfo hostInfo : node.getHostInfoList()){
                String host = hostInfo.getHost();

                int port = hostInfo.getPort();
                if(isServerProxy(proxy, host, port)){
                    continue;
                }
                int status = serverMgr.getServerStatus(proxy, interfaceName, host, port);
                //服务没有下线 就不管了
                if(status != ServerMgr.STS_DOWN){
                    continue;
                }
                channel = connect(proxy, interfaceName, host, port, node.isEncrypt(), ServerMgr.STS_STARTING, null, null);
                if(null != channel){
                    break;
                }
            }
        }
        if(null == channel) return false;
        return flushProxy(proxy, node, channel);

    }

    /**
     * 刷新远程注册中心的代理服务
     * @param proxy
     * @param node
     * @param centerChannel
     * @return
     */
    private boolean flushProxy(String proxy, ProxyHolderNode node, SocketChannel centerChannel) {
        ServerMgr serverMgr = aaceMgr.getServerMgr();
        ArrayList<ServerInfo> serverInfoList = new ArrayList<>();
        String interfaceName = node.getInterfaceName();
        if(proxy == AACE_CENTER){
            interfaceName = AACE_CENTER;
        }
        int retcode = aaceCenterClient.getProxy(proxy, interfaceName, serverInfoList, centerChannel, null);
        if(retcode >= 0 && retcode != 2){
            serverMgr.setServerStatus(centerChannel, ServerMgr.STS_WORKING);
        }
        if(retcode != RetCode.RET_SUCESS){
            return false;
        }

        boolean checkcode = false;
        interfaceName = node.getInterfaceName();
        for(ServerInfo serverInfo : serverInfoList){
            String srvProxy = serverInfo.getProxy();
            String host = serverInfo.getHostIp();
            int port = Integer.valueOf(serverInfo.getHostPort());
            if(isServerProxy(srvProxy, host, port)){
                continue;
            }
            int status = serverMgr.getServerStatus(srvProxy, interfaceName, host, port);
            SocketChannel channel = null;
            if(status != ServerMgr.STS_DOWN){
                channel = serverMgr.getHostChannel(srvProxy, interfaceName, host, port);
                if(status != serverInfo.getStatus()){
                    serverMgr.setServerStatus(channel, serverInfo.getStatus());
                }
                registLocalProxy(node.getLocalProxy(), node.getLocalInterfaceName(), channel);
                checkcode = true;
                continue;
            }
            channel = connect(srvProxy, node.getInterfaceName(), host, port, node.isEncrypt(), (int)serverInfo.getStatus(), node.getLocalProxy(), node.getLocalInterfaceName());
            if(null != channel){
                checkcode = true;
            }
        }
        return checkcode;
    }

    private boolean isServerProxy(String proxy, String host, int port) {
        proxyLock.lock();
        try{
            List<ProxyHolderNode> nodes = proxyMap.get(proxy);
            if(null == nodes){
                return false;
            }
            Iterator<ProxyHolderNode> iter = nodes.iterator();
            while(iter.hasNext()){
                ProxyHolderNode node = iter.next();
                if(node.getMode() != HOLD_SERVER && node.getMode() != HOLD_LOCAL) continue;
                for(HostInfo hostInfo : node.getHostInfoList()){
                    if((host.isEmpty() || hostInfo.getHost().equals(host))
                            && (port == 0 || hostInfo.getPort() == port)){
                        return true;
                    }
                }
            }
            return false;
        }finally {
            proxyLock.unlock();
        }
    }

    private boolean checkClientProxy(String proxy, ProxyHolderNode node) {
        return false;
    }

    private boolean checkDirectProxy(String proxy, ProxyHolderNode node) {
        SocketChannel channel = aaceMgr.getServerMgr().getProxyChannel(proxy,node.getInterfaceName(),ServerMgr.STS_AVAILABLE);
        if(null != channel){
            registLocalProxy(node.getLocalProxy(),node.getLocalInterfaceName(),channel);
            return true;
        }
        boolean result = false;
        for(HostInfo hostInfo : node.getHostInfoList()){
            channel = connect(proxy,node.getInterfaceName(),hostInfo.getHost(),hostInfo.getPort(),node.isEncrypt(),ServerMgr.STS_WORKING,node.getLocalProxy(),node.getLocalInterfaceName());
            if(null == channel)  continue;
            result = true;
        }
        return result;
    }

    /**
     *  建立SocketChannel 连接
     * @param proxy
     * @param interfaceName
     * @param host
     * @param port
     * @param encrypt
     * @param status
     * @param localProxy
     * @param localInterfaceName
     * @return
     */
    private SocketChannel connect(String proxy, String interfaceName, String host, int port, boolean encrypt, int status, String localProxy, String localInterfaceName) {
        SocketChannel channel = null;
        Logger.InfoLog("begin connect to " + host + ":" + port + ",proxy=" + proxy);
        try {
            channel = SocketChannel.open();
            channel.connect(new InetSocketAddress(host,port));
        } catch (IOException e) {
            Logger.LOGGER.error("error connect to "+ host +":" + port,e);
            return null;
        }
        aaceMgr.insertChannel(channel);
    /*    if(encrypt){
            if(!exchangeKey(channel, host, port)){
                aaceMgr.
            }
        }*/

        aaceMgr.getServerMgr().registerServer(channel, proxy, interfaceName, host, port, status);
        registLocalProxy(localProxy, localInterfaceName, channel);
        Logger.InfoLog("connect to "+ host + ":" + port + ", proxy=" + proxy);
        return channel;
    }



    private void registLocalProxy(String localProxy, String localInterfaceName, SocketChannel channel) {
        if(isEmpty(localProxy))  return ;
        List<SocketChannel> channels = new ArrayList<>();
        channels.add(channel);


    }

    private boolean isEmpty(String str) {
        return (null == str || str.isEmpty());
    }

    /**
     * 增加代理服务
     * @param proxy
     * @param interfaceName
     * @param mode
     * @param encrytp
     * @param hostInfoList
     * @param localProxy
     * @param localInterface
     * @return
     */
    public boolean addProxy(String proxy, String interfaceName, int mode, boolean encrytp, List<HostInfo> hostInfoList, String localProxy, String localInterface) {
        List<HostInfo> hostInfos = new ArrayList<>(hostInfoList.size());
        List<HostInfo> domainHosts = new ArrayList<>();
        for(HostInfo hostInfo : hostInfoList){
            String host = hostInfo.getHost();
            int port = hostInfo.getPort();
            HostInfo info = new HostInfo(host, port);
            switch (mode){
                case HOLD_SERVER:
                    if(host.equals("*")){
                        host = "0.0.0.0";
                    }else if(host.isEmpty()){
                        host = getLocalIp(interfaceName, host);
                    }
                case HOLD_LOCAL:
                    info = listen(proxy, interfaceName, host, port);
                    if(null == info) continue;
                    break;
                case HOLD_DIRECT:
                    if(host.equals("*")) continue;
                    if(!isValidIp(host)){
                        domainHosts.add(hostInfo);
                        continue;
                    }
                    break;
                default:
                    if(host.equals("*")) continue;
                    break;
            }
            hostInfos.add(info);
        }
        String centerInterface = null;
        if(mode == HOLD_CLIENT){
            if(hostInfos.isEmpty())  return true;
            //
        }

        if(!hostInfos.isEmpty()){
            ProxyHolderNode node = new ProxyHolderNode(interfaceName, mode, encrytp, hostInfos, localProxy, localInterface, centerInterface);
            if(addProxy(proxy, node)){
                checkProxy(proxy, node);
            }
        }
        if(!domainHosts.isEmpty()){
            ProxyHolderNode node = new ProxyHolderNode(interfaceName, HOLD_DOMAIN, encrytp, domainHosts, localProxy, localInterface, centerInterface);
            if(addProxy(proxy,node)){
                checkProxy(proxy, node);
            }
        }
        return true;
    }

    private HostInfo listen(String proxy, String interfaceName, String host, int port) {
        proxyLock.lock();
        try{
            List<ProxyHolderNode> nodes = proxyMap.get(proxy);
            if(null != nodes){
                for(ProxyHolderNode node : nodes){
                    if(node.getMode() != HOLD_SERVER && node.getMode() != HOLD_LOCAL) {
                        continue;
                    }
                    for(HostInfo hostInfo : node.getHostInfoList()){
                        if(hostInfo.getHost() == host){
                            if(port == 0){
                                port = hostInfo.getPort();
                            }
                            if(port == hostInfo.getPort()){
                                if(node.getInterfaceName() == interfaceName){
                                    return null;
                                }
                                return new HostInfo(host, port);
                            }
                        }
                    }
                }
            }
        }finally {
            proxyLock.unlock();
        }
        port = aaceMgr.addListener(host, port);
        if(port == 0){
            Logger.ErrLog("listen " + host + ":" + port + " error.");
            return null;
        }
        Logger.InfoLog(proxy + " listen at "+ host + ":" + port);
        return new HostInfo(host, port);
    }

    private boolean isValidIp(String ip) {
        String[] seg = ip.split("\\.");
        if(seg.length < 4) return false;
        for(int i = 0; i < 4; i++){
            int n = Integer.valueOf(seg[i]);
            if(n < 0 || n > 255)  return false;
        }
        return true;
    }

    /**
     *  把 node 加入到proxyMap 中 返回是否是新增
     * @param proxy
     * @param node
     * @return
     */
    private boolean addProxy(String proxy, ProxyHolderNode node) {
        proxyLock.lock();
        try{
            List<ProxyHolderNode> nodes = proxyMap.get(proxy);
            if(null == nodes){
                nodes = new ArrayList<>();
                nodes.add(node);
                proxyMap.put(proxy, nodes);
                return true;
            }
            ProxyHolderNode cacheNode = null;
            HashMap<String, Integer> hostMap = new HashMap<>();
            for(ProxyHolderNode phNode : nodes){
                if(!isEqual(phNode.getInterfaceName(), node.getInterfaceName())
                        || phNode.getMode() != node.getMode()
                        || !isEqual(phNode.getLocalProxy(), node.getLocalProxy())
                        || !isEqual(phNode.getLocalProxy(), node.getLocalInterfaceName())){
                    continue;
                }
                cacheNode = phNode;
                for(HostInfo hostInfo : phNode.getHostInfoList()){
                    hostMap.put(hostInfo.getHost(), hostInfo.getPort());
                }
                break;
            }
            if(null == cacheNode){
                nodes.add(node);
                return true;
            }
            boolean newRecord = false;
            for(HostInfo hostInfo : node.getHostInfoList()){
                Integer port = hostMap.get(hostInfo.getHost());
                if(null == port || !port.equals(hostInfo.getPort())){
                    cacheNode.getHostInfoList().add(hostInfo);
                    newRecord = true;
                    continue;
                }
            }
            return newRecord;
        }finally {
            proxyLock.unlock();
        }

    }

    private String getLocalIp(String interfaceName, String host) {
        if(!host.isEmpty() && !host.equals("0.0.0.0")){
            return host;
        }
        ServerMgr serverMgr = aaceMgr.getServerMgr();
        SocketChannel channel = serverMgr.getProxyChannel(AACE_CENTER, interfaceName, ServerMgr.STS_AVAILABLE);
        if(null == channel){
            return null;
        }
        try{
            InetSocketAddress address = (InetSocketAddress) channel.getLocalAddress();
            host = address.getHostString();
        } catch (IOException e) {
            Logger.ErrLog(e);
            return null;
        }
        return host;
    }

    private boolean isEqual(String src, String dest){
        if(src == dest) return true;
        if(null == src || null == dest) return false;
        return src.equals(dest);
    }


    public void testPrint(){
        System.out.println(proxyMap.size());
        for(Map.Entry<String,List<ProxyHolderNode>> entry : proxyMap.entrySet()){
            System.out.println("proxy: "+ entry.getKey());
            for(ProxyHolderNode node : entry.getValue()){
                System.out.println(node);
            }
        }
    }
}
