package com.xuhe.aace.test;


import com.xuhe.aace.AaceServer;
import com.xuhe.aace.provider.server.OrderServer;

public class Main {


    public static void main(String[] args) {

        //开启服务
        OrderServer aaceCenterServer = new OrderServer("OrderServer",
                "OrderServer",
                "aace://127.0.0.1:9990/center");

/*
        AaceCheckServer aaceCheckServer = new AaceCheckServer("AaceCheck",
                "AaceCheck",
                "aace://127.0.0.1:9990/center");
*/


        while (true){
            try {
                Thread.sleep(1000* 10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            AaceServer.get().getHolder().testPrint();
            AaceServer.get().getServerMgr().testPrint();
        }






        //LockSupport.park();
    }

}
