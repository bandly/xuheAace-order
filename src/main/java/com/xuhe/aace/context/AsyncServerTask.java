package com.xuhe.aace.context;

import com.xuhe.aace.Logger;

public class AsyncServerTask extends Thread {

    private AsyncServer server;

    public AsyncServerTask(AsyncServer server,String proxy){
        super("Aace-AsyncServerTask-"+proxy);
        this.server = server;
        this.setDaemon(true);
    }

    public void run(){
        while (true){
            AaceContext ctx =  new AaceContext();
            try{
                server.process(ctx);
            }catch (Exception e){
                Logger.LOGGER.error("AsyncServerTask.run server={} process ex",server,e);
            }
        }
    }
}
