package com.xuhe.aace.handler;

import java.util.List;

public class ProxyHolderNode {

    private String interfaceName;
    private int mode;
    private boolean encrypt;
    private List<HostInfo> hostInfoList;
    private String localProxy;
    private String localInterfaceName;
    private String centerInterfaceName;

    public ProxyHolderNode(String interfaceName, int mode, boolean encrypt, List<HostInfo> hostInfoList, String localProxy, String localInterfaceName, String centerInterfaceName) {
        this.interfaceName = interfaceName;
        this.mode = mode;
        this.encrypt = encrypt;
        this.hostInfoList = hostInfoList;
        this.localProxy = localProxy;
        this.localInterfaceName = localInterfaceName;
        this.centerInterfaceName = centerInterfaceName;
    }

    public ProxyHolderNode(String interfaceName, int mode, boolean encrypt, List<HostInfo> hostInfoList) {
        this.interfaceName = interfaceName;
        this.mode = mode;
        this.encrypt = encrypt;
        this.hostInfoList = hostInfoList;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public int getMode() {
        return mode;
    }

    public boolean isEncrypt() {
        return encrypt;
    }

    public List<HostInfo> getHostInfoList() {
        return hostInfoList;
    }

    public String getLocalProxy() {
        return localProxy;
    }

    public String getLocalInterfaceName() {
        return localInterfaceName;
    }

    public String getCenterInterfaceName() {
        return centerInterfaceName;
    }

    @Override
    public String toString() {
        return "ProxyHolderNode{" +
                "interfaceName='" + interfaceName + '\'' +
                ", mode=" + mode +
                ", encrypt=" + encrypt +
                ", hostInfoList=" + hostInfoList +
                ", localProxy='" + localProxy + '\'' +
                ", localInterfaceName='" + localInterfaceName + '\'' +
                ", centerInterfaceName='" + centerInterfaceName + '\'' +
                '}';
    }
}
