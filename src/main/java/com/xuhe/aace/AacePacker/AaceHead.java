package com.xuhe.aace.AacePacker;

import com.xuhe.aace.packer.FieldType;
import com.xuhe.aace.packer.PackData;
import com.xuhe.aace.packer.PackException;
import com.xuhe.aace.packer.PackStruct;

import java.util.*;

public class AaceHead implements PackStruct {

    private String interfaceName;
    private String methodName = "";
    private byte callType = 2;
    private long seqId = 0;
    private TreeMap<String,String> reserved;

    public static List<String> names(){
        List<String> result = new ArrayList<>(5);
        result.add("interfaceName");
        result.add("methodName");
        result.add("callType");
        result.add("seqId");
        result.add("reserved");
        return result;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public byte getCallType() {
        return callType;
    }

    public void setCallType(byte callType) {
        this.callType = callType;
    }

    public long getSeqId() {
        return seqId;
    }

    public void setSeqId(long seqId) {
        this.seqId = seqId;
    }

    public TreeMap<String, String> getReserved() {
        return reserved;
    }

    public void setReserved(TreeMap<String, String> reserved) {
        this.reserved = reserved;
    }

    @Override
    public int size() {
        byte fieldNum = 5;
        do{
            //这里似乎不太对？ 不应该直接都 break
            if(null == reserved)
                fieldNum--;
            else break;
            if(seqId == 0)
                fieldNum--;
            else break;
            if(callType == 2)
                fieldNum--;
            else break;
            if(null == methodName)
                fieldNum--;
            else break;
        }while(false);
        int size = 2;
        size += PackData.getSize(interfaceName);
        do{
            if(fieldNum == 1) break;
            size += 1;
            size += PackData.getSize(methodName);
            if(fieldNum == 2) break;
            size += 1;
            size++;
            if(fieldNum == 3) break;
            size += 1;
            size += PackData.getSize(seqId);
            if(fieldNum == 4) break;
            size += 3;
            if(null == reserved){
                size++;
            }else{
                size += PackData.getSize(reserved.size());
                Iterator<Map.Entry<String,String>> itr = reserved.entrySet().iterator();
                while(itr.hasNext()){
                    Map.Entry<String,String> entry = itr.next();
                    size += PackData.getSize(entry.getKey());
                    size += PackData.getSize(entry.getValue());
                }
            }
        }while(false);
        return size;
    }

    @Override
    public void packData(PackData packData) {
        byte fieldNum = 5;
        do{
            if(null == reserved)
                fieldNum--;
            else break;
            if(seqId == 0)
                fieldNum--;
            else break;
            if(callType == 2)
                fieldNum--;
            else break;
            if(methodName == "")
                fieldNum--;
            else break;
        }while (false);
        packData.packInt(fieldNum);
        do{
            packData.packByte(PackData.FT_STRING);
            packData.packString(interfaceName);
            if(fieldNum == 1) break;
            packData.packByte(PackData.FT_STRING);
            packData.packString(methodName);
            if(fieldNum == 2) break;
            packData.packByte(PackData.FT_CHAR);
            packData.packByte(callType);
            if(fieldNum == 3) break;
            packData.packByte(PackData.FT_NUMBER);
            packData.packLong(seqId);
            if(fieldNum == 4) break;
            packData.packByte(PackData.FT_MAP);
            packData.packByte(PackData.FT_STRING);
            packData.packByte(PackData.FT_STRING);
            if(null == reserved){
                packData.packByte((byte)0);
            }else{
                int len = reserved.size();
                packData.packInt(len);
                Iterator<Map.Entry<String,String>> itr = reserved.entrySet().iterator();
                while(itr.hasNext()){
                    Map.Entry<String,String> entry = itr.next();
                    packData.packString(entry.getKey());
                    packData.packString(entry.getValue());
                }
            }
        }while (false);
    }

    @Override
    public void unpackData(PackData packData) throws PackException {
        byte num = packData.unpackByte();
        FieldType field;
        if(num < 1) throw new PackException(PackData.PACK_LENGTH_ERROR,"PACK_LENGTH_ERROR");
        field = packData.unpackFieldType();
        if(!PackData.matchType(field.baseType,PackData.FT_STRING)) throw new PackException(PackData.PACK_TYPEMATCH_ERROR,"PACK_TYPEMATCH_ERROR");
        interfaceName = packData.unpackString();
        do{
            if(num < 2) break;
            field = packData.unpackFieldType();
            if(!PackData.matchType(field.baseType,PackData.FT_STRING)) throw new PackException(PackData.PACK_TYPEMATCH_ERROR,"PACK_TYPEMATCH_ERROR");
            methodName = packData.unpackString();
            if(num < 3) break;
            field = packData.unpackFieldType();
            if(!PackData.matchType(field.baseType,PackData.FT_CHAR)) throw new PackException(PackData.PACK_TYPEMATCH_ERROR,"PACK_TYPEMATCH_ERROR");
            callType = packData.unpackByte();
            if(num < 4) break;
            field = packData.unpackFieldType();
            if(!PackData.matchType(field.baseType,PackData.FT_NUMBER)) throw new PackException(PackData.PACK_TYPEMATCH_ERROR,"PACK_TYPEMATCH_ERROR");
            seqId = packData.unpackLong();
            if(num < 5) break;
            field = packData.unpackFieldType();
            if(!PackData.matchType(field.baseType,PackData.FT_MAP)) throw new PackException(PackData.PACK_TYPEMATCH_ERROR,"PACK_TYPEMATCH_ERROR");
            {
                int size = packData.unpackInt();
                if(size > packData.MAX_RECORD_SIZE || size < 0 )  throw new PackException(PackData.PACK_LENGTH_ERROR,"PACK_LENGTH_ERROR");
                reserved = new TreeMap<String,String>();
                for(int i = 0; i < size; i++){
                    String key = null;
                    String value = null;
                    key = packData.unpackString();
                    value = packData.unpackString();
                    reserved.put(key,value);
                }
            }
        }while (false);

        //后面不太懂为什么还要在循环一遍
        for(int i = 5; i < num; i++){
            packData.peekField();
        }
    }

    @Override
    public String toString() {
        return "AaceHead{" +
                "interfaceName='" + interfaceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", callType=" + callType +
                ", seqId=" + seqId +
                ", reserved=" + reserved +
                '}';
    }
}
