package com.xuhe.aace.handler;

/**
 * @author liq@shinemo.com
 * @version 1.0
 * @date 2019/9/11 15:01
 * @Description
 */
public class ServerNode {

    private String proxy;

    private String interfaceName;

    private String host;

    private int port;

    private int status;

    public ServerNode(String proxy, String interfaceName, String host, int port, int status) {
        this.proxy = proxy;
        this.interfaceName = interfaceName;
        this.host = host;
        this.port = port;
        this.status = status;
    }


    public String getProxy() {
        return proxy;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getStatus() {
        return status;
    }


    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
