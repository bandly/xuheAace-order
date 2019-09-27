package com.xuhe.aace.provider.server;

import com.xuhe.aace.handler.AaceHandler;


public class SaOrgAccountServer extends AaceHandler {


    public SaOrgAccountServer(String proxy, String interfaceName, String uri){
        this.proxy = proxy;
        this.interfaceName = interfaceName;
        this.uri = uri;
        this.init();
    }



    @Override
    protected boolean registerHandler() {
        this.aaceMgr.registerHandler("FaceTransfer", "faceReqTransfer", this, "faceReqTransfer", this.mode);
        return true;
    }


    public void faceReqTransfer(){
        System.out.println("klsdkjfsldkjfsldkjfl");
    }

}
