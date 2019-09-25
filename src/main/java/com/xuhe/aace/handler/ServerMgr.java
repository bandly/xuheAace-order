package com.xuhe.aace.handler;

import com.sun.security.ntlm.Server;
import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.common.SocketInfo;

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author liq@shinemo.com
 * @version 1.0
 * @date 2019/9/11 15:20
 * @Description 管理服务的类
 */
public class ServerMgr {

    public static final int STS_DOWN = 0;
    public static final int STS_STARTING = 1;
    public static final int STS_WORKING = 2;
    public static final int STS_AVAILABLE = 3;
    //compatible  可共用的; 兼容的; 可共存的
    public static final int STS_COMPATIBLE = 4;
    //冲突
    public static final int STS_CONFLICT = 5;




    private static class ProxyInterfacNode{
        private String interfaceName;
        private int curr;
        private ArrayList<SocketChannel> serverList = new ArrayList<>();

        public ProxyInterfacNode(String interfaceName){
            this.interfaceName = interfaceName;
            this.curr = 0;
        }

        public boolean equals(Object obj){
            if(null == obj) return false;
            if(obj == this) return true;
            if(obj.getClass().equals(getClass())){
                ProxyInterfacNode node = (ProxyInterfacNode)obj;
                return Objects.equals(this.interfaceName,node.interfaceName);
            }
            return false;
        }
    }

    //代理map proxy -> ProxyNode(interfaceName)
    private ConcurrentHashMap<String, List<ProxyInterfacNode>> proxyMap = new ConcurrentHashMap<>();

    //确保安全，因为有可能多个线程同时put
    private ConcurrentHashMap<SocketChannel,ServerNode> socketServerNodeMap = new ConcurrentHashMap<>();

    //代理父级
    private HashMap<String,TreeSet<Integer>> subProxyMap = new HashMap<>();

    //用来确保 subProxyMap 的读写安全
    private ReentrantReadWriteLock subLock = new ReentrantReadWriteLock();

    //用来确保 proxyMap 的读写安全
    private ReentrantReadWriteLock proxyLock = new ReentrantReadWriteLock();

    //用来确保 socketServerMap 的读写安全
    private ReentrantReadWriteLock socketLock = new ReentrantReadWriteLock();


    private int status;


    public ServerMgr(AaceMgr aaceMgr){
        this.status = STS_STARTING;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void addConnChannel(SocketChannel channel){
        String host = SocketInfo.getRemoteAddress(channel);
        int port = SocketInfo.getRemotePort(channel);
        ServerNode serverNode = new ServerNode("","",host,port,STS_DOWN);
        ServerNode oldServerNode = AddConnChannel(channel, serverNode);
        //之前加入过该channel 则删 proxyNode
        if(null != oldServerNode){
            removeProxyServer(oldServerNode.getProxy(),oldServerNode.getInterfaceName(),channel);
        }
        updateActiveTime(channel);
    }

    private void updateActiveTime(SocketChannel channel) {

    }

    private void removeProxyServer(String proxy, String interfaceName, SocketChannel channel) {
        if(proxy.isEmpty()){
            //没有代理名称 则直接返回 说明之前也没有加入过
            return;
        }
        ProxyInterfacNode proxyNode = new ProxyInterfacNode(interfaceName);

        proxyLock.writeLock().lock();

        try{
            List<ProxyInterfacNode> proxyInterfacNodeList = proxyMap.get(proxy);
            if(null == proxyInterfacNodeList || proxyInterfacNodeList.size() == 0){
                return;
            }
            //因为 ProxyNode 重写的equals 方法 所以可以这样查找
            int index = proxyInterfacNodeList.indexOf(proxyNode);
            if(index < 0){
                return;
            }
            ProxyInterfacNode oldProxyNode = proxyInterfacNodeList.get(index);
            //删除代理 对应的 serverlist 中的channel
            oldProxyNode.serverList.remove(channel);
            //如果当前proxy 对应的interface 中 serverList 空了，这删除该interface
            if(oldProxyNode.serverList.isEmpty()){
                proxyInterfacNodeList.remove(index);
                if(proxyInterfacNodeList.isEmpty()){
                    proxyMap.remove(proxy);
                    //可能还有父级需要删除
                    int pos = proxy.indexOf(".");
                    if(pos >= 0){
                        int id = Integer.parseInt(proxy.substring(pos+1));
                        delSubProxy(proxy.substring(0,pos),id);
                    }
                }
            }
        }finally {
            proxyLock.writeLock().unlock();
        }
    }

    private void delSubProxy(String proxy, int id) {
        subLock.writeLock().lock();
        try{
            TreeSet<Integer> subProxySet = subProxyMap.get(proxy);
            if(null == subProxySet){
                return;
            }
            if(subProxySet.remove(id)){
                if(subProxySet.isEmpty()){
                    subProxyMap.remove(proxy);
                }
            }
        }finally {
            subLock.writeLock().unlock();
        }
    }

    /**
     * 把channel 添加到 集合中，并且返回 老的
     * @param channel
     * @param serverNode
     * @return
     */
    private ServerNode AddConnChannel(SocketChannel channel, ServerNode serverNode) {
        ServerNode oldNode = socketServerNodeMap.put(channel,serverNode);
        return oldNode;
    }

    /**
     * 根据proxy interfaceName 状态 获取 SocketChannel
     * @param proxy
     * @param interfaceName
     * @param status
     * @return
     */
    public SocketChannel getProxyChannel(String proxy, String interfaceName, int status){
        if(proxy == null || proxy.isEmpty()) return null;
        ProxyInterfacNode node = new ProxyInterfacNode(interfaceName);
        ProxyInterfacNode any = new ProxyInterfacNode("*");
        proxyLock.writeLock().lock();
        try{
            List<ProxyInterfacNode> proxyInterfacNodeList = proxyMap.get(proxy);
            if(proxyInterfacNodeList == null){
                return null;
            }
            int index = 0;
            index = proxyInterfacNodeList.indexOf(node);
            if(index < 0){
                index = proxyInterfacNodeList.indexOf(any);
            }
            if(index < 0){
                return null;
            }
            ProxyInterfacNode proxyInterfacNode = proxyInterfacNodeList.get(index);
            if(proxyInterfacNode.serverList.isEmpty()){
                return null;
            }
            if(proxyInterfacNode.serverList.size() == 1){
                return proxyInterfacNode.serverList.get(0);
            }
            int currId = proxyInterfacNode.curr;
            int stop = currId;
            if(currId >= proxyInterfacNode.serverList.size()){
                stop = 0;
            }

            socketLock.readLock().lock();
            try{
                do{
                    currId++;
                    if(currId >= proxyInterfacNode.serverList.size()){
                        currId = 0;
                    }
                    SocketChannel channel = proxyInterfacNode.serverList.get(currId);
                    ServerNode serverNode = socketServerNodeMap.get(channel);
                    if(null != serverNode){
                        if((status & serverNode.getStatus()) != 0){
                            //只要状态 不是0
                            proxyInterfacNode.curr = currId;
                            return channel;
                        }
                    }
                }while (currId != stop);
                return null;
            }finally {
                socketLock.readLock().unlock();
            }

        }finally {
            proxyLock.writeLock().unlock();
        }
    }


    public void registerServer(SocketChannel channel, String proxy, String interfaceName, String host, int port, int status) {
        addConnChannel(channel);
        setConnChannel(channel, proxy, interfaceName, host, port, status);
        addProxyServer(proxy, interfaceName, channel);
        updateActiveTime(channel);
        int pos = proxy.indexOf(".");
        if(pos < 0) return ;
        int id =  Integer.valueOf(proxy.substring(pos + 1));
        addSubProxy(proxy.substring(0, pos), id);

    }

    private void addSubProxy(String proxy, int id) {
        subLock.writeLock().lock();
        try{
            TreeSet<Integer> subProxySet = subProxyMap.get(proxy);
            if(null == subProxySet){
                subProxySet = new TreeSet<>();
                subProxyMap.put(proxy, subProxySet);
            }
            subProxySet.add(id);
        }finally {
            subLock.writeLock().unlock();
        }
    }

    private void addProxyServer(String proxy, String interfaceName, SocketChannel channel) {
        if(proxy.isEmpty()) return ;
        ProxyInterfacNode proxyInterfacNode = new ProxyInterfacNode(interfaceName);
        proxyInterfacNode.serverList.add(channel);
        proxyLock.writeLock().lock();
        try{
            List<ProxyInterfacNode> proxyInterfacNodeList = proxyMap.get(proxy);
            if(null == proxyInterfacNodeList){
                proxyInterfacNodeList = new ArrayList<>();
                proxyInterfacNodeList.add(proxyInterfacNode);
                proxyMap.put(proxy,proxyInterfacNodeList);
                return;
            }
            int index = proxyInterfacNodeList.indexOf(proxyInterfacNode);
            if(index >= 0){
                ProxyInterfacNode pin = proxyInterfacNodeList.get(index);
                if(pin.serverList.contains(channel)) return;
                proxyInterfacNode.serverList.add(channel);
            }else{
                proxyInterfacNodeList.add(proxyInterfacNode);
            }
        }finally {
            proxyLock.writeLock().unlock();
        }
    }

    private void setConnChannel(SocketChannel channel, String proxy, String interfaceName, String host, int port, int status) {
        socketLock.writeLock().lock();
        try {
            ServerNode node = socketServerNodeMap.get(channel);
            if (null == node) {
                node = new ServerNode(proxy, interfaceName, host, port, status);
                socketServerNodeMap.put(channel, node);
            } else {
                node.setProxy(proxy);
                node.setInterfaceName(interfaceName);
                if (!host.isEmpty()) {
                    node.setHost(host);
                }
                if (port > 0) {
                    node.setPort(port);
                }
                node.setStatus(status);
            }
        } finally {
            socketLock.writeLock().unlock();
        }
    }

    public int getServerStatus(String proxy, String interfaceName, String host, int port) {
        //7???
        List<ServerNode> serverNodeList = getProxyServers(proxy, interfaceName, 7);
        if(null == serverNodeList || serverNodeList.isEmpty()){
            return STS_DOWN;
        }

        Iterator<ServerNode> iter = serverNodeList.iterator();
        while(iter.hasNext()){
            ServerNode node = iter.next();
            if(node.getHost().equals(host) && node.getPort() == port){
                return node.getStatus();
            }
        }
        return STS_DOWN;
    }

    /**
     * 根据 proxyMap 查找出socketChannel 在根据 socketServerNodeMap 查找出serverNode
     * @param proxy
     * @param interfaceName
     * @param status
     * @return
     */
    private List<ServerNode> getProxyServers(String proxy, String interfaceName, int status) {
        if(proxy.isEmpty()) return null;
        ProxyInterfacNode proxyInterfacNode = new ProxyInterfacNode(interfaceName);
        proxyLock.readLock().lock();
        try{
            List<ProxyInterfacNode> proxyInterfacNodeList = proxyMap.get(proxy);
            if(null == proxyInterfacNodeList){
                return null;
            }
            int index = proxyInterfacNodeList.indexOf(proxyInterfacNode);
            if(index < 0){
                return null;
            }
            ProxyInterfacNode pin = proxyInterfacNodeList.get(index);
            if(pin.serverList.isEmpty()){
                return null;
            }
            List<ServerNode> servers = new ArrayList<>(pin.serverList.size());
            socketLock.readLock().lock();
            try{
                Iterator<SocketChannel> iter = pin.serverList.iterator();
                while (iter.hasNext()){
                    SocketChannel channel = iter.next();
                    ServerNode serverNode = socketServerNodeMap.get(channel);
                    if(null != serverNode){
                        if((status & serverNode.getStatus()) != 0){
                            servers.add(serverNode);
                        }
                    }
                }
                return servers;
            }finally {
                socketLock.readLock().unlock();
            }


        }finally {
            proxyLock.readLock().unlock();
        }
    }


    public void setServerStatus(SocketChannel channel, int status) {
        socketLock.writeLock().lock();
        try{
            ServerNode node = socketServerNodeMap.get(channel);
            if(null != node){
                node.setStatus(status);
            }else{
                String host = SocketInfo.getRemoteAddress(channel);
                int port = SocketInfo.getRemotePort(channel);
                node = new ServerNode("", "", host, port, status);
                socketServerNodeMap.put(channel, node);
            }

        }finally {
            socketLock.writeLock().unlock();
        }
    }


    public SocketChannel getHostChannel(String proxy, String interfaceName, String host, int port) {
        if(proxy.isEmpty()){
            return null;
        }
        ProxyInterfacNode proxyInterfacNode = new ProxyInterfacNode(interfaceName);
        proxyLock.readLock().lock();
        try{
            List<ProxyInterfacNode> proxyInterfacNodeList = proxyMap.get(proxy);
            if(null == proxyInterfacNodeList){
                return null;
            }
            int index = proxyInterfacNodeList.indexOf(proxyInterfacNode);
            if(index < 0){
                return null;
            }
            ProxyInterfacNode pin = proxyInterfacNodeList.get(index);
            if(pin.serverList.isEmpty()){
                return null;
            }
            socketLock.readLock().lock();
            try{
                for(SocketChannel channel : pin.serverList){
                    ServerNode serverNode = socketServerNodeMap.get(channel);
                    if(null == serverNode){
                        continue;
                    }
                    if(serverNode.getHost().equals(host) && serverNode.getPort() == port){
                        return channel;
                    }
                }
                return null;
            }finally {
                socketLock.readLock().unlock();
            }
        }finally {
            proxyLock.readLock().unlock();
        }
    }

    public SocketChannel getDistProxyChannel(String proxy, String interfaceName, long hashval, int status) {
        return null;
    }

    /**
     * 根据状态一次性获取 指定proxy 指定接口 指定状态的所有SocketChannel
     * @param proxy
     * @param interfaceName
     * @param status
     * @return
     */
    public List<SocketChannel> getProxyChannels(String proxy, String interfaceName, int status) {
        List<String> subProxies = getSubProxies(proxy);
        if(null == subProxies){
            return getProxyChannelsBySubProxy(proxy, interfaceName, status);
        }
        List<SocketChannel> channels = new ArrayList<>();
        Iterator<String> iterator = subProxies.iterator();
        while(iterator.hasNext()){
            String subProxy = iterator.next();
            List<SocketChannel> subChannels = getProxyChannelsBySubProxy(subProxy, interfaceName, status);
            if(!subChannels.isEmpty()){
                channels.addAll(subChannels);
            }
        }
        return channels;
    }

    /**
     * 获取代理的通道
     * @param proxy
     * @param interfaceName
     * @param status
     * @return
     */
    private List<SocketChannel> getProxyChannelsBySubProxy(String proxy, String interfaceName, int status) {
        if(proxy.isEmpty()) return null;
        ProxyInterfacNode node = new  ProxyInterfacNode(interfaceName);
        proxyLock.readLock().lock();
        try{
            List<ProxyInterfacNode> proxyNodeList = proxyMap.get(proxy);
            if(null == proxyNodeList){
                return null;
            }
            int index = proxyNodeList.indexOf(node);
            if(index < 0){
                return null;
            }
            ProxyInterfacNode proxyInterfacNode = proxyNodeList.get(index);
            if(proxyInterfacNode.serverList.isEmpty()){
                return null;
            }
            List<SocketChannel> servers = new ArrayList<>(proxyInterfacNode.serverList.size());
            socketLock.readLock().lock();
            try{
                Iterator<SocketChannel> itr = servers.iterator();
                while(itr.hasNext()){
                    SocketChannel channel = itr.next();
                    ServerNode serverNode = socketServerNodeMap.get(channel);
                    if(null != serverNode
                            && (status &serverNode.getStatus()) != 0){
                        servers.add(channel);
                    }
                }
            }finally {
                socketLock.readLock().unlock();
            }
            return servers;
        }finally {
            proxyLock.readLock().unlock();
        }
    }

    /**
     * 获取该proxy 的子代理 或 附属代理
     * @param proxy
     * @return
     */
    private List<String> getSubProxies(String proxy) {
        List<String> subProxys =  null;
        subLock.readLock().lock();
        try{
            TreeSet<Integer> subIds = subProxyMap.get(proxy);
            if(null != subIds){
                subProxys = new ArrayList<String>(subIds.size());
                Iterator<Integer> iter = subIds.iterator();
                while(iter.hasNext()){
                    Integer id = iter.next();
                    String subProxy = proxy + "." + id;
                    subProxys.add(subProxy);
                }
            }
        }finally {
            subLock.readLock().unlock();
        }
        return subProxys;
    }


    public void testPrint(){
        System.out.println("-------------- serverMgr:   proxyMap: " + proxyMap.size());
        for(Map.Entry<String,List<ProxyInterfacNode>> entry : proxyMap.entrySet()){
            System.out.println("proxy: "+ entry.getKey());
            for(ProxyInterfacNode node : entry.getValue()){
                System.out.println(node);
            }
        }

        System.out.println("-------------- serverMgr:   socketMap: " + socketServerNodeMap.size());
        for(Map.Entry<SocketChannel,ServerNode> entry : socketServerNodeMap.entrySet()){
            System.out.println("socketChannel: "+ entry.getKey());
            System.out.println("serverNode : "+ entry.getValue());
        }
    }
}

