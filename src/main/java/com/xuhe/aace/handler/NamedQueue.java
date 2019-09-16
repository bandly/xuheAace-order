package com.xuhe.aace.handler;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * 自定义一个阻塞队列，为了可以表示队列名称
 * @param <E>
 */
public class NamedQueue<E> extends ArrayBlockingQueue<E> {

    private String name;

    public NamedQueue(String name,int capacity) {
        super(capacity);
    }

    public String getName(){
        return name;
    }


}
