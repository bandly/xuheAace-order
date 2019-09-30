package com.xuhe.aace.provider.server;

import com.xuhe.aace.common.RetCode;
import com.xuhe.aace.common.SocketInfo;
import com.xuhe.aace.consumer.client.CrmClient;
import com.xuhe.aace.context.AaceContext;
import com.xuhe.aace.handler.AaceHandler;
import com.xuhe.aace.handler.HostInfo;
import com.xuhe.aace.handler.ProxyHolder;
import com.xuhe.aace.handler.ServerMgr;
import com.xuhe.protocol.model.ServerInfo;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;


public class OrderServer extends AaceHandler {


    public OrderServer(String proxy, String interfaceName, String uri){
        this.proxy = proxy;
        this.interfaceName = interfaceName;
        this.uri = uri;
        this.init();
    }



    @Override
    protected boolean registerHandler() {
        this.aaceMgr.registerHandler("OrderServer", "createOrder", this, "createOrder", this.mode);
        return true;
    }

    private CrmClient crmClient = new CrmClient("CrmServer",
            "CrmServer",
            "aace://127.0.0.1:9990/center");

    /**
     * 创建订单
     */
    public void createOrder(String orderId, String productId, String crmId){
        //获取会员信息
        crmClient.getCrmInfo(crmId);
        System.out.println("orderId :" + orderId + " productId : "+ productId);
    }

}
