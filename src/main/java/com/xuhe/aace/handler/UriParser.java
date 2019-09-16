package com.xuhe.aace.handler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UriParser {

    public static final int PROXY_CENTER = 0;
    public static final int PROXY_SERVER = 1;


    /**
     * 解析url aace://aace.shinemo.net:1699/center
     *    或者 aaces://aace.shinemo.net:1699/center
     * @param uri
     * @return
     */
    public static UriNode parse(String uri){
        String luri = uri.trim();
        boolean encrypt = false;
        int pos = 7; //下面的起始的索引
        if(luri.startsWith("aaces://")){
            encrypt = true;
            pos = 8;
        }else if(!luri.startsWith("aace://")){
            return null;
        }
        //找到 pos 之后的第一个"/" 位置
        int pos1 = luri.indexOf("/",pos);
        if(pos1 == -1) return null;
        // 字符 之前的 就是 host 信息
        String hostInfo = luri.substring(pos,pos1);
        String[] hosts = hostInfo.split(",");
        List<HostInfo> hostInfoList = new ArrayList<>(hosts.length);
        for(int i = 0; i < hosts.length; i++){
            String server = hosts[i];
            String host;
            int port;
            int pos2 = server.indexOf(":");
            if(pos2 == -1){
                host = server.trim();
                port = 16888;
            }else{
                host = server.substring(0,pos2).trim();
                try{
                    port = Integer.valueOf(server.substring(pos2 + 1));
                    if(port == 0) port = 16888;
                }catch (NumberFormatException e){
                    port = 16888;
                }
            }
            if(!isIP(host)){
                host = host2ip(host);
            }
            hostInfoList.add(new HostInfo(host,port));
        }
        pos = pos1 + 1;
        pos1 = luri.indexOf("?",pos);
        if(pos1 == -1) return null;
        String type = luri.substring(pos,pos1);
        int mode;
        if(type.equals("center")){
            mode = PROXY_CENTER;
        }else if(type.equals("server")){
            mode = PROXY_SERVER;
        }else {
            return null;
        }
        pos = pos1 + 1;
        HashMap<String,String> params = new HashMap<>();
        String[] kvs = luri.substring(pos).split("&");
        for(int i = 0; i < kvs.length; i++) {
            String[] kv = kvs[i].split("=");
            if(kv.length < 2) continue;
            params.put(kv[0],kv[1]);
        }
        UriNode node = new UriNode(encrypt,hostInfoList,mode,params);
        return node;
    }

    /**
     * 通过 InetAddress 对象 解析ip
     * @param host
     * @return
     */
    private static String host2ip(String host) {
        InetAddress address;
        try{
            address = InetAddress.getByName(host);
        }catch (UnknownHostException e){
            throw new RuntimeException("host=" + host + ", to ip error.",e);
        }
        String ip = address.getHostAddress();
        if(ip == null){
            throw new RuntimeException("host=" + host + ", to ip error.");
        }
        return ip;
    }

    /**
     * 判断 host 是否是ip
     *  这里 感觉是不很严谨， 应该用个正则表达式 ？
     * @param host
     * @return
     */
    private static boolean isIP(String host) {
        for(int i = 0; i < host.length(); i++){
            char c = host.charAt(i);
            if(c != '.' && !Character.isDigit(c)) return false;
        }
        return true;
    }
}
