package com.xuhe.aace.common;

import javax.crypto.spec.SecretKeySpec;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

/**
 * socket 秘钥缓存存储
 */
public class SocketKeyStore {

    private ConcurrentHashMap<SocketChannel, SecretKeySpec> socketKey = new ConcurrentHashMap<>();

    public boolean setKey(SocketChannel channel, SecretKeySpec key){
        socketKey.put(channel, key);
        return true;
    }

    public SecretKeySpec getKey(SocketChannel channel){
        return socketKey.get(channel);
    }

}
