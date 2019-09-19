package com.xuhe.aace.common;

import com.xuhe.aace.handler.Event;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

public class TimerQueue<K> {

    private HashMap<K, Long> dataMap = new HashMap<>();
    private TreeMap<Long, LinkedList<K>> queue =  new TreeMap<>();
    private Event event = new Event();

    public void add(K key, long ts){
        event.lock();
        try{
            if(dataMap.containsKey(key)){

            }
        }finally {
            event.unlock();
        }
    }
}
