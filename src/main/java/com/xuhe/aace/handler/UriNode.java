package com.xuhe.aace.handler;

import java.util.HashMap;
import java.util.List;

public class UriNode {


    private List<HostInfo> hostInfoList;
    private int mode;
    private HashMap<String,String> params;
    private boolean encrytp;


    public UriNode( boolean encrytp,List<HostInfo> hostInfoList, int mode, HashMap<String, String> params){
        this.encrytp = encrytp;
        this.hostInfoList = hostInfoList;
        this.mode = mode;
        this.params = params;

    }

    public List<HostInfo> getHostInfoList() {
        return hostInfoList;
    }

    public void setHostInfoList(List<HostInfo> hostInfoList) {
        this.hostInfoList = hostInfoList;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public HashMap<String, String> getParams() {
        return params;
    }

    public void setParams(HashMap<String, String> params) {
        this.params = params;
    }

    public boolean isEncrytp() {
        return encrytp;
    }

    public void setEncrytp(boolean encrytp) {
        this.encrytp = encrytp;
    }
}
