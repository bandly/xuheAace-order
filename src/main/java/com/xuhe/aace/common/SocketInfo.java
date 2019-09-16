package com.xuhe.aace.common;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author liq@shinemo.com
 * @version 1.0
 * @date 2019/9/11 14:15
 * @Description
 */
public class SocketInfo {

    public static String getRemoteAddress(SocketChannel channel){
        if(null == channel || null == channel.socket() || null == channel.socket().getInetAddress()){
            return "";
        }
        return channel.socket().getInetAddress().getHostAddress();
    }

    public static int getRemotePort(SocketChannel channel){
        if(null == channel || null == channel.socket()){
            return 0;
        }
        return channel.socket().getPort();
    }

    public static String getRemoteConnect(SocketChannel channel){
        String info =  null;
        try {
            info = channel.getRemoteAddress().toString();
            if(info.charAt(0) == '/'){
                info = info.substring(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            info = "disconnected";
        }
        return info;
    }




}
