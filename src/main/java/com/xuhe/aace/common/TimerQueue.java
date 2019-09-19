package com.xuhe.aace.common;

import com.xuhe.aace.handler.Event;

import java.util.*;

public class TimerQueue<K> {

    private HashMap<K, Long> dataMap = new HashMap<>();
    //treeMap 可以实现存储元素的自动排序
    private TreeMap<Long, LinkedList<K>> queue =  new TreeMap<>();
    private Event event = new Event();

    public void add(K key, long ts){
        event.lock();
        try{
            dataMap.put(key, ts);
            LinkedList<K> list = queue.get(ts);
            if(null == list){
                list = new LinkedList<>();
                list.add(key);
                queue.put(ts, list);
            }else{
                list.add(key);
            }
            if(dataMap.size() == 1){
                event.signal();
            }
        }finally {
            event.unlock();
        }
    }


    public boolean remove(K key){
        event.lock();
        try{
            Long ts = dataMap.remove(key);
            if(null == ts){
                return false;
            }
            LinkedList<K> list = queue.get(ts);
            if(null == list) return true;
            if(list.remove(key)){
                if(list.isEmpty()){
                    queue.remove(ts);
                }
            }
        }finally {
            event.unlock();
        }
        return true;
    }

    public boolean insert(K key, int offTime){
        long ts = System.currentTimeMillis() + offTime;
        add(key, ts);
        return true;
    }


    public ArrayList<K> get(int count){
        long stop = System.currentTimeMillis();
        ArrayList<K> keys = null;
        if(count > 0){
            keys = new ArrayList<>(count);
        }else{
            keys = new ArrayList<>();
        }
        int i = 0;
        event.lock();
        try{
            Iterator<Map.Entry<Long, LinkedList<K>>> it = queue.entrySet().iterator();
            FINISH:
               while(it.hasNext()){
                   Map.Entry<Long, LinkedList<K>> entry = it.next();
                   long ts = entry.getKey();
                   if(ts >= stop) break;
                   LinkedList<K> valList = entry.getValue();
                   if(null != valList){
                       Iterator<K> itr = valList.iterator();
                       while(itr.hasNext()){
                           K key = itr.next();
                           keys.add(key);
                           i++;
                           if(count >0 && i >= count) break FINISH;
                       }
                   }
               }
        }finally {
            event.unlock();
        }
        if(keys.isEmpty()) return null;
        Iterator<K> itr = keys.iterator();
        while(itr.hasNext()){
            K key = itr.next();
            remove(key);
        }
        return keys;
    }


    public ArrayList<K> blockGet(int count){
        long stop = System.currentTimeMillis();
        event.lock();
        try{
            Long ts = queue.firstKey();
            if(ts.longValue() > stop){
                event.timeWait(ts.longValue() - stop);
            }
        }catch (NoSuchElementException e){
            event.timeWait(5000);
        }finally {
            event.unlock();
        }
        return get(count);
    }


}
