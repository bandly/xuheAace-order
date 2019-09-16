package com.xuhe.aace.context;

import java.nio.channels.SocketChannel;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;

public class AaceContext {

    private ArrayBlockingQueue<AaceContext> queue;
    private long seqId;
    private SocketChannel channel;
    private TreeMap<String,String> params = new TreeMap<>();
    private byte[] request;
    private int retcode;
    private byte[] result;
    private byte commFlag;
    private Long hashval;

    public AaceContext(){
        this.seqId = 0;
        this.commFlag = (byte)5;
    }

    public ArrayBlockingQueue<AaceContext> getQueue() {
        return queue;
    }

    public void setQueue(ArrayBlockingQueue<AaceContext> queue) {
        this.queue = queue;
    }

    public long getSeqId() {
        return seqId;
    }

    public void setSeqId(long seqId) {
        this.seqId = seqId;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public TreeMap<String, String> getParams() {
        return params;
    }

    public void setParams(TreeMap<String, String> params) {
        this.params = params;
    }

    public void set(String key,String value){
        if(null == this.params){
          this.params = new TreeMap<>();
        }
        this.params.put(key,value);
    }

    public String take(String key){
        if(null == this.params){
           return null;
        }
        return this.params.get(key);
    }

    public String get(String key){
        if(null == this.params){
            return null;
        }
        return this.params.get(key);
    }

    public byte[] getRequest() {
        return request;
    }

    public void setRequest(byte[] request) {
        this.request = request;
    }

    public int getRetcode() {
        return retcode;
    }

    public void setRetcode(int retcode) {
        this.retcode = retcode;
    }

    public byte[] getResult() {
        return result;
    }

    public void setResult(byte[] result) {
        this.result = result;
    }

    public byte getCommFlag() {
        return commFlag;
    }

    public void setCommFlag(byte commFlag) {
        this.commFlag = commFlag;
    }

    public Long getHashval(){
        if(null == hashval) return null;
        Long hashval_ = hashval;
        hashval = null;
        return hashval_;
    }

    public void setHashval(long hashval){
        this.hashval = hashval;
    }
}