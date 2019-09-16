package com.xuhe.aace.common;

import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.Logger;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author liq@shinemo.com
 * @version 1.0
 * @date 2019/8/30 17:40
 * @Description
 */
public class ConnListener extends Thread {

    protected AaceMgr aaceMgr;
    protected int readBuffSize;
    protected int writeBuffSize;

    public ConnListener(AaceMgr aaceMgr, int readBuffSize, int writeBuffSize) {
        super("Aace-ConnListener-T");
        this.aaceMgr = aaceMgr;
        this.readBuffSize = readBuffSize;
        this.writeBuffSize = writeBuffSize;
        this.setDaemon(true);
    }

    /**
     * 线程循环遍历serverSocketChannel 接收新的连接
     */
    public void run() {
        Logger.InfoLog("ConnLister run....");
        SelectorStore selector = aaceMgr.getSelector();
        while (true) {
            try {
                Set<SelectionKey> keys = selector.selectAccepter();
                if (keys == null || keys.isEmpty()) continue;
                Iterator<SelectionKey> itr = keys.iterator();
                while (itr.hasNext()) {
                    SelectionKey key = itr.next();
                    itr.remove();
                    if (!key.isValid()) {
                        key.cancel();
                        continue;
                    }
                    if (!key.isAcceptable()) continue;
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    try {
                        SocketChannel channel = server.accept();
                        channel.socket().setReuseAddress(false);
                        channel.socket().setReceiveBufferSize(readBuffSize);
                        channel.socket().setSendBufferSize(writeBuffSize);
                        channel.socket().setKeepAlive(true);
                        //把接收到socketChannel 插入到readSelector 和 writeSelector 中
                        aaceMgr.insertChannel(channel);
                        aaceMgr.getServerMgr().addConnChannel(channel);
                        Logger.InfoLog("accept a new connect:" + channel.socket().getInetAddress());
                    } catch (IOException e) {
                        Logger.LOGGER.error("ConnectListener catch an exception:", e);
                        continue;
                    }
                }
            } catch (Throwable e) {
                Logger.LOGGER.error("ConnectListener catch an exception:", e);
            }
        }
    }
}
