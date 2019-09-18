package com.xuhe.aace.common;

import com.xuhe.aace.AaceMgr;

import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.HashMap;

public class MsgQueue {


    //deque 支持同时从两端添加 或移除元素 ArrayDeque 没有容量限制 ArrayDeque不支持值为 null 的元素
    private HashMap<SocketChannel, ArrayDeque<SndPkgNode>> queue = new HashMap<>();
    private int maxSize = AaceMgr.MAX_QUEUE_SIZE;


    public MsgQueue(int maxQueueSize) {
        this.maxSize = maxQueueSize;
    }

    public void removeChannel(SocketChannel channel){
        queue.remove(channel);
    }

    public boolean putSndPkgNode(SocketChannel channel, SndPkgNode node){
        ArrayDeque<SndPkgNode> deque = queue.get(channel);
        if(null == deque){
            deque = new ArrayDeque<>();
            deque.add(node);
            queue.put(channel, deque);
            return true;
        }
        if(deque.size() >= maxSize){
            return false;
        }
        deque.add(node);
        return true;
    }

    /**
     * 取出队列头部的元素，并从队列中移除
     * @param channel
     * @return
     */
    public SndPkgNode pollSndPkgNode(SocketChannel channel){
        ArrayDeque<SndPkgNode> deque = queue.get(channel);
        if(null == deque) return null;
        if(deque.isEmpty())  return null;
        //取出队列头部的元素，并从队列中移除
        //队列为空，返回null
        return deque.poll();
    }

    /**
     *  //取出队列头部的元素，但并不移除
     *     //队列为空，返回null
     * @param channel
     * @return
     */
    public SndPkgNode peekSndPkgNode(SocketChannel channel){
        ArrayDeque<SndPkgNode> deque = queue.get(channel);
        if(null == deque) return null;
        if(deque.isEmpty())  return null;
        //取出队列头部的元素，但并不移除
        //队列为空，返回null
        return deque.peek();
    }

    /**
     *   //取出队列头部的元素，并从队列中移除
     *    //队列为空，抛出NoSuchElementException异常
     * @param channel
     */
    public void removeSndPkgNode(SocketChannel channel){
        ArrayDeque<SndPkgNode> deque = queue.get(channel);
        if(null == deque) return ;
        if(deque.isEmpty())  return ;
        // //取出队列头部的元素，并从队列中移除
        //    //队列为空，抛出NoSuchElementException异常
        deque.remove();
    }


    public int size(SocketChannel channel){
        ArrayDeque<SndPkgNode> deque = queue.get(channel);
        if(null == queue) return 0;
        if(deque.isEmpty()) return 0;
        return deque.size();
    }



}
