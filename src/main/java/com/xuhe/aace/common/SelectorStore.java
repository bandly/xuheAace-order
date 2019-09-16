package com.xuhe.aace.common;

import com.xuhe.aace.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Set;

/**
 * @author liq@shinemo.com
 * @version 1.0
 * @date 2019/8/30 16:36
 * @Description NIO Select 存储
 */
public class SelectorStore {

    public static final int RD_EVN = 1;
    public static final int WR_EVN = 2;
    public static final int RW_EVN = 3;


    protected Selector acceptSelector;
    protected Selector readSelector;
    protected Selector writeSelector;

    protected int readBuffSize;
    protected int writeBuffSize;
    protected boolean block = false;


    public SelectorStore(int readBuffSize, int writeBuffSize) {
        this.readBuffSize = readBuffSize;
        this.writeBuffSize = writeBuffSize;
        try {
            acceptSelector = Selector.open();
            readSelector = Selector.open();
            writeSelector = Selector.open();
        } catch (IOException e) {
            Logger.ErrLog(e);
        }
        block = false;
    }

    /**
     * jvm 回收对象 前调用该方法 关闭选择器
     */
    public void finalize() {
        try {
            acceptSelector.close();
            readSelector.close();
            writeSelector.close();
        } catch (IOException e) {
            Logger.ErrLog(e);
        }
    }


    public int getReadBuffSize() {
        return readBuffSize;
    }
    public int getWriteBuffSize() {
        return writeBuffSize;
    }

    public void setBlock(boolean block) {
        block = block;
    }


    public Set<SelectionKey> selectAccepter() {
        Set<SelectionKey> keys = null;
        try {
            while(block) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
            acceptSelector.select(1);
            keys = acceptSelector.selectedKeys();
        } catch (IOException e) {
            Logger.ErrLog(e);
        }
        return keys;
    }

    public Set<SelectionKey> selectReader() {
        Set<SelectionKey> keys = null;
        try {
            while(block) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
            readSelector.selectNow();
            keys = readSelector.selectedKeys();
        } catch (IOException e) {
            Logger.ErrLog(e);
        }
        return keys;
    }

    public Set<SelectionKey> selectWriter() {
        Set<SelectionKey> keys = null;
        try {
            while(block) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
            writeSelector.select(1);
            keys = writeSelector.selectedKeys();
        } catch (IOException e) {
            Logger.ErrLog(e);
        }
        return keys;
    }

    /**
     * 开启serverSocket 监听  并且注册到acceptSelector 上
     * @param host
     * @param port
     * @return
     */
    public int addListener(String host, int port) {
        try {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().setReuseAddress(true);
            server.socket().setReceiveBufferSize(readBuffSize);
            server.socket().setSoTimeout(3000);
            if (host == null || host.isEmpty()) {
                server.socket().bind(new InetSocketAddress(port));
            } else {
                server.socket().bind(new InetSocketAddress(host, port));
            }
            server.register(acceptSelector, SelectionKey.OP_ACCEPT);
            return port == 0 ? server.socket().getLocalPort() : port;
        } catch (IOException e) {
            Logger.ErrLog(e);
            return 0;
        }
    }


    /**
     * 重新注册到 acceptSelector 上
     * @param channel
     * @return
     */
    public boolean addListener(ServerSocketChannel channel) {
        try {
            channel.register(acceptSelector, SelectionKey.OP_ACCEPT);
        } catch (ClosedChannelException e) {
            Logger.ErrLog(e);
            return false;
        }
        return true;
    }


    /**
     *  把socketChannel 注册到 readSelector / writeSelector
     * @param channel
     * @param type
     * @return
     */
    public SelectionKey insert(SocketChannel channel, int type) {
        if(!channel.isConnected()) return null;
        SelectionKey key = null;
        try {
            if((type & RD_EVN) != 0) {
                key = channel.keyFor(readSelector);
                if(key == null) {
                    channel.configureBlocking(false);
                    key = channel.register(readSelector, SelectionKey.OP_READ);
                }
            }
            if((type & WR_EVN) != 0) {
                key = channel.keyFor(writeSelector);
                if(key == null || !key.isValid()) {
                    if(key != null) {
                        key.cancel();
                    }
                    channel.configureBlocking(false);
                    key = channel.register(writeSelector, SelectionKey.OP_WRITE);
                }
            }
        } catch (ClosedChannelException e) {
            Logger.ErrLog(e);
            key = null;
        } catch (IOException e) {
            Logger.ErrLog(e);
            key = null;
        }
        return key;
    }

    /**
     *
     * @param channel
     * @param buffer
     * @return
     */
    public boolean insert(SocketChannel channel, ByteBuffer buffer) {
        try {
            channel.register(writeSelector, SelectionKey.OP_WRITE, buffer);
        } catch (ClosedChannelException e) {
            return false;
        }
        return true;
    }

    /**
     * 关闭通道
     * @param channel
     * @param type
     */
    public void remove(SocketChannel channel, int type) {
        try {
            if((type & WR_EVN) != 0) {
                SelectionKey key = channel.keyFor(writeSelector);
                if(key != null) {
                    channel.register(writeSelector, SelectionKey.OP_WRITE);
                    key.cancel();
                }
            }
            if((type & RD_EVN) != 0) {
                SelectionKey key = channel.keyFor(readSelector);
                if(key != null) key.cancel();
                channel.close();
            }
        } catch (IOException e) {
            Logger.ErrLog(e);
        }
    }

    public SelectionKey getSelectKey(SocketChannel channel, int type) {
        if(type == WR_EVN) {
            return channel.keyFor(writeSelector);
        }
        if(type == RD_EVN) {
            return channel.keyFor(readSelector);
        }
        return null;
    }
    public SelectionKey getWriterKey(SocketChannel channel) {
        return getSelectKey(channel, WR_EVN);
    }
}
