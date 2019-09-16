package com.xuhe.protocol.model;

import com.xuhe.aace.packer.PackData;
import com.xuhe.aace.packer.PackException;
import com.xuhe.aace.packer.PackStruct;

public class ServerInfo implements PackStruct {

    private String proxy;
    private String hostIp;
    private String hostPort;
    private byte status;


    @Override
    public int size() {
        return 0;
    }

    @Override
    public void packData(PackData packData) {

    }

    @Override
    public void unpackData(PackData packData) throws PackException {

    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }
}
