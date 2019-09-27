package com.xuhe.aace.test;


import com.xuhe.aace.AaceServer;
import com.xuhe.aace.consumer.client.SaOrgAccountClient;
import com.xuhe.aace.provider.server.SaOrgAccountServer;
import com.xuhe.protocol.server.AaceCheckServer;

import java.util.concurrent.locks.LockSupport;

public class Main {


    public static void main(String[] args) {

        //开启服务
        SaOrgAccountServer saOrgAccountServer = new SaOrgAccountServer("saOrgAccount11",
                "saOrgAccount11",
                "aace://aace.shinemo.net:16999/center");


        AaceCheckServer aaceCheckServer = new AaceCheckServer("AaceCheck",
                "AaceCheck",
                "aace://aace.shinemo.net:16999/center");

        AaceServer.get().getHolder().testPrint();

        AaceServer.get().getServerMgr().testPrint();


        //服务调用
        SaOrgAccountClient saOrgAccountClient = new SaOrgAccountClient("saOrgAccount11",
                "saOrgAccount11",
                "aace://aace.shinemo.net:16999/center");
        saOrgAccountClient.faceReqTransfer();


        LockSupport.park();
    }

}
