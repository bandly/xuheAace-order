package com.xuhe.protocol.model;

import com.xuhe.aace.packer.FieldType;
import com.xuhe.aace.packer.PackData;
import com.xuhe.aace.packer.PackException;
import com.xuhe.aace.packer.PackStruct;

import java.util.ArrayList;

public class ServerInfo implements PackStruct {

    private String proxy;
    private String hostIp;
    private String hostPort;
    private byte status;


    public static ArrayList<String> names(){
        ArrayList<String> result = new ArrayList<>();
        result.add("mProxy");
        result.add("mHostIp");
        result.add("mHostProxy");
        result.add("mStatus");
        return result;
    }


    @Override
    public int size() {
        int size = 6;//为什么是6？
        size += PackData.getSize(proxy);
        size += PackData.getSize(hostIp);
        size += PackData.getSize(hostPort);
        return size;
    }

    @Override
    public void packData(PackData packData) {
        byte fieldNum = 4;
        packData.packByte(fieldNum);
        packData.packByte(PackData.FT_STRING);
        packData.packString(proxy);
        packData.packByte(PackData.FT_STRING);
        packData.packString(hostIp);
        packData.packByte(PackData.FT_STRING);
        packData.packString(hostPort);
        packData.packByte(PackData.FT_CHAR);
        packData.packByte(status);
    }

    @Override
    public void unpackData(PackData packData) throws PackException {
        byte num = packData.unpackByte();
        FieldType fieldType;
        if(num < 4) throw new PackException(PackData.PACK_LENGTH_ERROR, "PACK_LENGTH_ERROR");
        fieldType = packData.unpackFieldType();
        if(!PackData.matchType(fieldType.baseType, PackData.FT_STRING)){
            throw new PackException(PackData.PACK_TYPEMATCH_ERROR, "PACK_TYPEMATCH_ERROR");
        }
        proxy = packData.unpackString();
        fieldType = packData.unpackFieldType();
        if(!PackData.matchType(fieldType.baseType, PackData.FT_STRING)){
            throw new PackException(PackData.PACK_TYPEMATCH_ERROR, "PACK_TYPEMATCH_ERROR");
        }
        hostIp = packData.unpackString();
        fieldType = packData.unpackFieldType();
        if(!PackData.matchType(fieldType.baseType, PackData.FT_STRING)){
            throw new PackException(PackData.PACK_TYPEMATCH_ERROR, "PACK_TYPEMATCH_ERROR");
        }
        hostPort = packData.unpackString();
        fieldType = packData.unpackFieldType();
        if(!PackData.matchType(fieldType.baseType, PackData.FT_CHAR)){
            throw new PackException(PackData.PACK_TYPEMATCH_ERROR, "PACK_TYPEMATCH_ERROR");
        }
        status = packData.unpackByte();
        for(int i = 4; i < num; i++){
            packData.peekField();
        }
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }
}
