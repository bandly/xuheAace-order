package com.xuhe.aace.test;


import com.xuhe.aace.AaceServer;
import com.xuhe.aace.provider.server.OrderServer;

public class Main {


    public static void main(String[] args) {

        //开启服务
        OrderServer aaceCenterServer = new OrderServer("OrderServer",
                "OrderServer",
                "aace://127.0.0.1:9990/center");



        aaceCenterServer.createOrder("ksldjflsd", "lksdfdfd", "ksldkfjsd" );



        //LockSupport.park();
    }

}
