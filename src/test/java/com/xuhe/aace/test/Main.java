package com.xuhe.aace.test;


import com.xuhe.aace.AaceServer;
import com.xuhe.aace.provider.server.SaOrgAccountServer;

import java.util.concurrent.locks.LockSupport;

public class Main {


    public static void main(String[] args) {
        SaOrgAccountServer saOrgAccountServer = new SaOrgAccountServer();
        saOrgAccountServer.setUri("aace://aace.shinemo.net:1699/center");
        saOrgAccountServer.setProxy("saOrgAccount11");
        saOrgAccountServer.setInterfaceName("saOrgAccount11");
        saOrgAccountServer.init();


        AaceServer.get().getHolder().testPrint();

        AaceServer.get().getServerMgr().testPrint();


        LockSupport.park();
    }

}
