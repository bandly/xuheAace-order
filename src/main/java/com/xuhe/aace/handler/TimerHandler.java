package com.xuhe.aace.handler;

import com.xuhe.aace.Logger;
import com.xuhe.aace.common.TimerQueue;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class TimerHandler extends Thread {


    private static class TimerHandlerNode{
        private TimerTask task;
        private Object param;
        private boolean loop;
        private int offTime;

        public TimerHandlerNode(TimerTask task, Object param, boolean loop, int offTime){
            this.task = task;
            this.param = param;
            this.loop = loop;
            this.offTime = offTime;
        }

        public int hashCode(){
            return task.getClass().getName().hashCode();
        }

        public boolean equals(Object obj){
            if(!obj.getClass().equals(this.getClass())){
                return false;
            }
            return this == obj;
        }
    }

    private TimerQueue<TimerHandlerNode> queue = new TimerQueue<>();


    public TimerHandler(){
        super("Aace-TimerHandler-T");
        this.setDaemon(true);
    }

    public void addTask(TimerTask task, Object param, boolean loop, int offTime){
        TimerHandlerNode node = new TimerHandlerNode(task, param, loop, offTime);
        queue.insert(node, offTime);
    }

    public void addTask(TimerTask task, int offTime){
        addTask(task, null, true, offTime);
    }

    public void addTask(TimerHandlerNode node){
        queue.insert(node, node.offTime);
    }


    public void run(){
        while(true){
            try {
                Thread.sleep(500);
            }catch (InterruptedException e){

            }
            List<TimerHandlerNode> tasks =  queue.get(-1);
            if(null == tasks || tasks.isEmpty()) continue;
            Iterator<TimerHandlerNode> iter = tasks.iterator();
            while(iter.hasNext()){
                TimerHandlerNode node = iter.next();
                if(null != node && null != node.task){
                    try{
                        node.task.process(node.param);
                    }catch (Throwable e){
                        Logger.LOGGER.error("TimerHandler.run ex", e);
                    }
                    if(node.loop){
                        addTask(node);
                    }
                }
            }
        }
    }


}
