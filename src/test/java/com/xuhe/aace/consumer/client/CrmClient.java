package com.xuhe.aace.consumer.client;

import com.xuhe.aace.handler.AaceCaller;

public class CrmClient extends AaceCaller {

    public CrmClient(String proxy, String interfaceName, String uri){
        this.proxy = proxy;
        this.interfaceName = interfaceName;
        this.uri = uri;
        this.init();
    }

    public void getCrmInfo(String crmId){
        this.invoke(DEFAULT_TIMEOUT, null, "getCrmInfo");
    }
}
