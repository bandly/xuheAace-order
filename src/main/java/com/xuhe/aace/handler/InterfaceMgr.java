package com.xuhe.aace.handler;

import com.xuhe.aace.common.RetCode;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InterfaceMgr {

    private static class MethodNode{
        private String func;
        private int mode;

        public MethodNode(String func, int mode){
            this.func = func;
            this.mode = mode;
        }
    }

    private static class InterfaceNode{
        private AaceHandler handler;
        HashMap<String, MethodNode> methodNodeHashMap;

        public InterfaceNode(AaceHandler handler){
            this.handler = handler;
            this.methodNodeHashMap = new HashMap<>();
        }

        public void addMethod(String name, String func, int mode){
            MethodNode node = new MethodNode(func, mode);
            methodNodeHashMap.put(name, node);
        }

        public MethodNode getMethod(String name){
            return methodNodeHashMap.get(name);
        }

        public HashMap<String, MethodNode> getMethodNodeHashMap() {
            return methodNodeHashMap;
        }

        public AaceHandler getHandler() {
            return handler;
        }

        public void setHandler(AaceHandler handler) {
            this.handler = handler;
        }
    }

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private HashMap<String, InterfaceNode> interfaceNodeHashMap = new HashMap<>();



    public boolean registerHandler(String interfaceName, String methodName, AaceHandler handler, String func, int mode){
        boolean retCode = true;
        lock.writeLock().lock();
        try{
            InterfaceNode node = interfaceNodeHashMap.get(interfaceName);
            if(null == node){
                node = new InterfaceNode(handler);
                node.addMethod(methodName, func, mode);
                interfaceNodeHashMap.put(interfaceName, node);
            }else{
                if(node.getMethodNodeHashMap().containsKey(methodName)){
                    retCode = false;
                }else{
                    node.addMethod(methodName, func, mode);
                }
            }
            return retCode;
        }finally {
            lock.writeLock().unlock();
        }
    }

    public int getHandler(String interfaceName, String methodName, MethodInfo methodInfo){
        lock.readLock().lock();
        try{
            InterfaceNode node = interfaceNodeHashMap.get(interfaceName);
            if(null == node){
                node = interfaceNodeHashMap.get("*");
                if(null == node){
                    return RetCode.RET_NOINTERFACE;
                }
            }
            MethodNode method = node.getMethod(methodName);
            if(null == method){
                method = node.getMethod("*");
                if(null == method){
                    return RetCode.RET_NOMETHOD;
                }
            }
            methodInfo.setHandler(node.getHandler());
            methodInfo.setMethodName(method.func);
            methodInfo.setMode(method.mode);
            return RetCode.RET_SUCESS;
        }finally {
            lock.readLock().unlock();
        }
    }

}
