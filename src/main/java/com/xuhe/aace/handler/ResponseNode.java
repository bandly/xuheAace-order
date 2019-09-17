package com.xuhe.aace.handler;

import com.xuhe.aace.context.AaceContext;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ResponseNode {
    private int retCode;

    private byte[] rspData;

    private AaceContext context;

    public ResponseNode(int retCode, byte[] rspData, AaceContext context) {
        this.retCode = retCode;
        this.rspData = rspData;
        this.context = context;
    }

    public ResponseNode(int retCode) {
        this.retCode = retCode;
        this.rspData = null;
        this.context = null;
    }

    public int getRetCode() {
        return retCode;
    }

    public void setRetCode(int retCode) {
        this.retCode = retCode;
    }

    public byte[] getRspData() {
        return rspData;
    }

    public void setRspData(byte[] rspData) {
        this.rspData = rspData;
    }

    public AaceContext getContext() {
        return context;
    }

    public void setContext(AaceContext context) {
        this.context = context;
    }

    public void addParams(TreeMap<String, String> params){
        if(null == params){
            return ;
        }
        if(null == this.context){
            this.context = new AaceContext();
        }
        Iterator< Map.Entry< String,String > > itr = params.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry< String,String > entry = itr.next();
            this.context.set(entry.getKey(), entry.getValue());
        }
    }
}
