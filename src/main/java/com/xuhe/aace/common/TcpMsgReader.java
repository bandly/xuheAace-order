package com.xuhe.aace.common;

import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class TcpMsgReader extends Thread {

    private AaceMgr aaceMgr;

    public TcpMsgReader(AaceMgr aaceMgr){
        super("Aace-TcpMsgReader-T");
        this.aaceMgr = aaceMgr;
        this.setDaemon(true);
    }


    public void run(){
        SelectorStore selectorStore = aaceMgr.getSelector();
        ByteBuffer buff = ByteBuffer.allocate(selectorStore.getReadBuffSize());
        while(true){
            try{
                Set<SelectionKey> keys = selectorStore.selectReader();
                if(null == keys || keys.isEmpty()){
                    sleep(0, 10);
                    continue;
                }
                Iterator<SelectionKey> itr = keys.iterator();
                while(itr.hasNext()){
                    SelectionKey key = itr.next();
                    itr.remove();
                    if(!key.isValid()){
                        key.cancel();
                        continue;
                    }
                    if(!key.isReadable()) continue;
                    SocketChannel channel = (SocketChannel) key.channel();
                    buff.clear();
                    int readRet = 0;
                    try{
                        readRet = channel.read(buff);
                    }catch (IOException e){
                        readRet = -1;
                    }
                    if(readRet < 0){
                        Logger.InfoLog("TcpMsgReader readRet < 0 ," + readRet);
                        aaceMgr.shutdown(channel, true);
                        continue;
                    }
                    if(readRet > 0){
                        byte[] msg = new byte[readRet];
                        System.arraycopy(buff.array(), 0, msg, 0, readRet);
                        aaceMgr.onMessageRecv(channel, msg);
                        continue;
                    }
                }
            }catch (Throwable e){
                Logger.LOGGER.error("TcpMsgReader catch an unknown exception:", e);
            }
        }
    }

}
