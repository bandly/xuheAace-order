package com.xuhe.aace.handler;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Event {

    private ReentrantLock lock;
    private Condition condition;

    public Event(){
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    public void lock(){
        lock.lock();
    }

    public void unlock(){
        lock.unlock();
    }
    public void signal(){
        condition.signal();
    }

    public void timeWait(long timeout){
        try {
            condition.awaitNanos(timeout * 1000000);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
