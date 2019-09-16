package com.xuhe.aace.handler;


import com.xuhe.aace.AacePacker.AaceHead;
import com.xuhe.aace.common.RcvPkgNode;

public class AaceHandlerNode {


    private MethodInfo methodInfo;

    private AaceHead aaceHead;

    private RcvPkgNode rcvPkgNode;


    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public void setMethodInfo(MethodInfo methodInfo) {
        this.methodInfo = methodInfo;
    }

    public AaceHead getAaceHead() {
        return aaceHead;
    }

    public void setAaceHead(AaceHead aaceHead) {
        this.aaceHead = aaceHead;
    }

    public RcvPkgNode getRcvPkgNode() {
        return rcvPkgNode;
    }

    public void setRcvPkgNode(RcvPkgNode rcvPkgNode) {
        this.rcvPkgNode = rcvPkgNode;
    }

    @Override
    public String toString() {
        return "AaceHandlerNode{" +
                "methodInfo=" + methodInfo +
                ", aaceHead=" + aaceHead +
                ", rcvPkgNode=" + rcvPkgNode +
                '}';
    }
}
