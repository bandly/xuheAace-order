package com.xuhe.aace.common;

import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class TcpMsgWriter extends Thread{

    private AaceMgr aaceMgr;

    public TcpMsgWriter(AaceMgr aaceMgr){
        super("Aace-TcpMsgWriter-T");
        this.aaceMgr = aaceMgr;
        this.setDaemon(true);
    }

    public void run(){
        while (true){
            try{
                Set<SelectionKey> keys = aaceMgr.getSendMgr().getWriteChannels();
                if(keys.isEmpty()) continue;
                for(SelectionKey key : keys){
                    if(!key.isValid()){
                        key.cancel();
                        continue;
                    }
                    SocketChannel channel = (SocketChannel) key.channel();
                    ByteBuffer buff = (ByteBuffer) key.attachment();
                    if(null == buff || !buff.hasRemaining()){
                        buff = aaceMgr.getSendMgr().getChannelByteBuffer(channel);
                        if(null == buff) continue;
                        buff.flip();
                    }
                    try{
                        channel.write(buff);
                    }catch (IOException e){
                        aaceMgr.shutdown(channel, true);
                    }catch (Throwable e){
                        Logger.LOGGER.error("TcpMsgWriter catch an throwable exception: ", e);
                    }
                    if(buff.hasRemaining()){
                        aaceMgr.getSelector().insert(channel, buff);
                    }else{
                        aaceMgr.getSelector().insert(channel, buff);
                    }
                }
            }catch (Throwable e){
                Logger.LOGGER.error("TcpMsgWriter catch an unknown exception: {}", e);
            }
        }
    }
}
