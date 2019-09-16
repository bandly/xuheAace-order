package com.xuhe.aace.handler;


import com.xuhe.aace.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class QueueMonitor implements TimerTask {

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private HashMap<String,NamedQueue<?>> queues = new HashMap<>();


    public boolean registerQueue(NamedQueue<?> queue){
        String name = queue.getName();
        lock.writeLock().lock();
        try{
            if(queues.containsKey(name)) return false;
            queues.put(name,queue);
        }finally {
            lock.writeLock().unlock();
        }
        return true;
    }



    @Override
    public void process(Object obj) {
        lock.readLock().lock();
        try{
            for(Map.Entry<String,NamedQueue<?>> entry : queues.entrySet()){
                int size = entry.getValue().size();
                if(size > 0){
                    Logger.WarnLog("Queue " + entry.getKey() + " size=" + size);
                }
            }
        }finally {
            lock.readLock().unlock();
        }
    }
}
