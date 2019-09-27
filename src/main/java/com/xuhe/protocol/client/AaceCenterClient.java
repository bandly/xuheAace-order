package com.xuhe.protocol.client;

import com.xuhe.aace.AaceMgr;
import com.xuhe.aace.Logger;
import com.xuhe.aace.common.RetCode;
import com.xuhe.aace.handler.AaceCaller;
import com.xuhe.aace.handler.ResponseNode;
import com.xuhe.aace.packer.FieldType;
import com.xuhe.aace.packer.PackData;
import com.xuhe.aace.packer.PackException;
import com.xuhe.protocol.model.ServerInfo;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class AaceCenterClient extends AaceCaller {


    public AaceCenterClient(AaceMgr aaceMgr){
        this.aaceMgr = aaceMgr;
        this.interfaceName = "AaceCenter";
        this.proxy = "AaceCenter";
        this.uri = "";
    }


    public int getProxy(String proxy, String interfaceName, ArrayList<ServerInfo> serverInfoList, SocketChannel centerChannel) {
        return getProxy(proxy, interfaceName, serverInfoList, centerChannel, DEFAULT_TIMEOUT);
    }

    public int getProxy(String proxy, String interName, ArrayList<ServerInfo> serverList, SocketChannel channel, int timeout) {
        byte[] reqData = packGetProxy(proxy, interName);
        ResponseNode responseNode = null;
        if(null == channel){
            responseNode = invoke("getProxy", reqData, timeout, null);
        }else{
            responseNode = invoke(channel, "getProxy", reqData, timeout, null);
        }
        return unpackGetProxy(responseNode, serverList);
    }

    protected byte[] packGetProxy(String proxy, String interName) {
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
        packData.resetOutBuff(reqData);
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



    private int unpackGetProxy(ResponseNode responseNode, ArrayList<ServerInfo> serverList) {
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

    public boolean registerProxyServer(String host, String port, String proxy, String interfaceName, int status, List<SocketChannel> channels) {
        byte[] reqData = packRegistProxyServer(host, port, proxy, interfaceName, status);
        return notify(channels, "registerProxy", reqData);
    }

    public boolean notify(List<SocketChannel> channels, String method, byte[] reqData){
        boolean retCode = false;
        Iterator<SocketChannel> iter = channels.iterator();
        while (iter.hasNext()){
            SocketChannel channel = iter.next();
            retCode = notify(channel, method, reqData) == RetCode.RET_SUCESS;
        }
        return retCode;
    }
    public int notify(SocketChannel channel, String method, byte[] reqData){
        int retCode = aaceMgr.notify(channel, interfaceName, method, reqData);
        if(retCode != RetCode.RET_SUCESS){
            Logger.ErrLog("call " + interfaceName + "." + method + " error.ret=" + retCode);
        }
        return RetCode.RET_SUCESS;
    }



    private byte[] packRegistProxyServer(String host, String port, String proxy, String interfaceName, int status) {
        PackData packData = new PackData();
        byte fieldNum = 5;
        do{
            if(Objects.equals("", interfaceName)){
                fieldNum--;
            }else{
                break;
            }
        }while (false);
        int packSize = 5;// 固定五个byte fieldNum, 3 FT_STRING 1 FT_NUMBER
        packSize += PackData.getSize(host);
        packSize += PackData.getSize(port);
        packSize += PackData.getSize(proxy);
        packSize += PackData.getSize(status);
        do{
            if(fieldNum == 4) break;
            packSize += 1;
            packSize += PackData.getSize(interfaceName);
        }while (false);
        byte[] reqData = new byte[packSize];
        packData.resetOutBuff(reqData);
        packData.packByte(fieldNum);
        do{
            packData.packByte(PackData.FT_STRING);
            packData.packString(host);
            packData.packByte(PackData.FT_STRING);
            packData.packString(port);
            packData.packByte(PackData.FT_STRING);
            packData.packString(proxy);
            packData.packByte(PackData.FT_NUMBER);
            packData.packInt(status);
            if(fieldNum == 4) break;
            packData.packByte(PackData.FT_STRING);
            packData.packString(interfaceName);
        }while (false);
        return reqData;
    }
}
