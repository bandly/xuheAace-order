package com.xuhe.aace.handler;

import com.xuhe.aace.Logger;
import com.xuhe.aace.common.RcvPkgNode;

public class MsgDispatcherTask extends Thread{

    private MsgDispatcher dispatcher;

    public MsgDispatcherTask(MsgDispatcher dispatcher){
        super("Aace-MsgDispatcherTask-T");
        this.dispatcher = dispatcher;
        this.setDaemon(true);
    }

    public void run(){
        while (true){
            try{
                RcvPkgNode rcvPkgNode = null;
                try{
                    rcvPkgNode = dispatcher.getQueue().take();
                }catch (InterruptedException e){
                    continue;
                }
                if(null == rcvPkgNode) continue;
                dispatcher.recvPackage(rcvPkgNode);
            }catch (Throwable e){
                Logger.LOGGER.error("MsgDispatcherTask catch an unknown exception", e);
            }
        }
    }

}
