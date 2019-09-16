package com.xuhe.aace.context;

public abstract class AsyncServer {

    public void start(int num, String proxy){
        for(int i = 0; i< num; i++){
            AsyncServerTask task = new AsyncServerTask(this,proxy);
            task.start();
        }
    }

    protected abstract void process(AaceContext ctx);
}
