package com.xuhe.aace.handler;

public class HostInfo {

    private String host;
    private int port;

    public HostInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "HostInfo{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
