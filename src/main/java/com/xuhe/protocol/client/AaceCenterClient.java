package com.xuhe.protocol.client;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import com.xuhe.aace.Logger;
import com.xuhe.aace.common.RetCode;
import com.xuhe.aace.context.AaceContext;
import com.xuhe.aace.handler.AaceCaller;
import com.xuhe.aace.handler.ResponseNode;
import com.xuhe.aace.handler.ServerMgr;
import com.xuhe.aace.packer.FieldType;
import com.xuhe.aace.packer.PackData;
import com.xuhe.aace.packer.PackException;
import com.xuhe.protocol.model.ServerInfo;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class AaceCenterClient extends AaceCaller {


    public int getProxy(String proxy, String interfaceName, List<ServerInfo> serverInfoList, SocketChannel centerChannel, AaceContext ctx) {
        return getProxy(proxy, interfaceName, serverInfoList, centerChannel, DEFAULT_TIMEOUT, ctx);
    }

    public int getProxy(String proxy, String interName, List<ServerInfo> serverList, SocketChannel channel, int timeout, AaceContext ctx) {
        byte[] reqData = packGetProxy(proxy, interName);
        ResponseNode responseNode = null;
        if(null == channel){
            responseNode = invoke("getProxy", reqData, timeout, ctx);
        }else{
            responseNode = invoke(channel, "getProxy", reqData, timeout, ctx);
        }
        return unpackGetProxy(responseNode, serverList);
    }

    private ResponseNode invoke(String methodName, byte[] reqData, int timeout, AaceContext ctx) {
        SocketChannel channel = getProxyServer(ctx);
        if(null == channel){
            Logger.ErrLog("no proxy" + proxy + " found. call " + interfaceName + "." + methodName + " error");
            return new ResponseNode(RetCode.RET_DISCONN);
        }
        return invoke(channel, methodName, reqData, timeout, ctx);
    }



    private ResponseNode invoke(SocketChannel channel, String methodName, byte[] reqData, int timeout, AaceContext ctx) {
        ResponseNode responseNode = aaceMgr.syncRequest(channel, proxy, interfaceName, methodName, reqData, timeout, ctx);
        if(responseNode.getRetCode() != RetCode.RET_SUCESS){
           Logger.ErrLog("call " + interfaceName + "." + methodName + " error.ret=" + responseNode.getRetCode());
        }
        return responseNode;
    }
    private SocketChannel getProxyServer(AaceContext ctx) {
        SocketChannel channel = null;
        Long hashval = null;
        if(null != null){
            hashval = ctx.getHashval();
        }
        if(null == hashval || !distributed){
            channel = getProxyServer(proxy, ServerMgr.STS_WORKING);
            if(null != channel){
                distributed = false;
                return channel;
            }
        }
        if(null == hashval){
            return null;
        }
        channel = getDeployServer(hashval.longValue(), ServerMgr.STS_WORKING);
        distributed = (null != channel);
        return channel;
    }

    private SocketChannel getDeployServer(long hashval, int status) {
        return aaceMgr.getServerMgr().getDistProxyChannel(proxy, interfaceName, hashval, status);
    }

    private SocketChannel getProxyServer(String proxy, int status){
        return aaceMgr.getServerMgr().getProxyChannel(proxy, interfaceName, status);
    }



    private byte[] packGetProxy(String proxy, String interName) {
        PackData packData = new PackData();
        byte fieldNum = 2;
        do{
            if("".equals(interName)){
                fieldNum--;
            }else break;
        }while (false);
        int packSize = 2; //为什么是2 是因为 第一个存储fieldNum,第一个存储proxy 的类型
        packSize += PackData.getSize(proxy);
        do{
            if(fieldNum == 1) break;
            packSize += 1;
            packSize += PackData.getSize(interfaceName);
        }while(false);

        byte[] reqData = new byte[packSize];
        packData.resetOutBUff(reqData);
        packData.packByte(fieldNum);

        do{
            packData.packByte(PackData.FT_STRING);
            packData.packString(proxy);
            if(fieldNum == 1) break;
            packData.packByte(PackData.FT_STRING);
            packData.packString(interfaceName);
        }while (false);
        return reqData;
    }
    private int unpackGetProxy(ResponseNode responseNode, List<ServerInfo> serverList) {
        int retCode = responseNode.getRetCode();
        if(retCode != RetCode.RET_SUCESS) return retCode;
        PackData packData = new PackData();
        byte[] respData = responseNode.getRspData();
        packData.resetInBuff(respData);
        try{
            retCode = packData.unpackInt();
            try{
                byte num = packData.unpackByte();
                FieldType fieldType;
                if(num < 1) throw new PackException(PackData.PACK_LENGTH_ERROR, "PACK_LENGTH_ERROR");
                fieldType = packData.unpackFieldType();
                if(!PackData.matchType(fieldType.baseType, PackData.FT_VECTOR))  throw new PackException(PackData.PACK_TYPEMATCH_ERROR, "PACK_TYPEMATCH_ERROR");
                {
                    int size = packData.unpackInt();
                    if(size > PackData.MAX_RECORD_SIZE || size < 0) throw new PackException(PackData.PACK_LENGTH_ERROR, "PACK_LENGTH_ERROR");
                    serverList.ensureCapacity(size);
                    for(int i = 0; i < size; i++){
                        ServerInfo tmpVal = new ServerInfo();
                        tmpVal.unpackData(packData);
                        serverList.add(tmpVal);
                    }
                }
            }catch (PackException e){
                return retCode != RetCode.RET_SUCESS ? retCode : RetCode.RET_INVALID;
            }



        }catch (PackException e){
            return RetCode.RET_INVALID;
        }
        return retCode;
    }

}
