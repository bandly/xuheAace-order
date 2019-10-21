package com.xuhe.aace.consumer.client;

import com.xuhe.aace.AaceServer;
import com.xuhe.aace.handler.AaceCaller;

public class CrmClient extends AaceCaller {

    public CrmClient(String proxy, String interfaceName, String uri){
        super(AaceServer.get(), proxy, interfaceName, uri);
    }

    public void getCrmInfo(String crmId){
        this.invoke(DEFAULT_TIMEOUT, null, "getCrmInfo");
    }
}
