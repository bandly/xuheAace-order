package com.xuhe.aace.consumer.client;

import com.xuhe.aace.handler.AaceCaller;

public class SaOrgAccountClient extends AaceCaller {

    public SaOrgAccountClient(String proxy, String interfaceName, String uri){
        this.proxy = proxy;
        this.interfaceName = interfaceName;
        this.uri = uri;
        this.init();
    }

    public void faceReqTransfer(){
     this.invoke(DEFAULT_TIMEOUT, null, "faceReqTransfer");
    }
}
