package com.xuhe.aace.packer;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class PackData {

    public static String ENCODE = "utf-8";
    public static final int DEFAULT_TIME = 2000;
    public static final int MAX_RECORD_SIZE = (10 * 1024 * 1024);//10m
    public static final int COMPRESS_THRESHOLD = 10240;//压缩阀值
    public static final int PACK_RIGHT = 0;

    //错误码
    public static final int PACK_STARTER_ERROR = 1;
    public static final int PACK_VERSION_ERROR = 2;
    public static final int PACK_LENGTH_ERROR = 3;
    public static final int PACK_CHECKCODE_ERROR = 4;
    public static final int PACK_TYPEMATCH_ERROR = 5; //类型匹配错误
    public static final int PACK_INVALID = 6;
    public static final int PACK_SYSTEM_ERROR = 7;

    public static final byte FT_PACK = 0;
    public static final byte FT_CHAR = 1;
    public static final byte FT_NUMBER = 2;
    public static final byte FT_STRING = 3;
    public static final byte FT_VECTOR = 4;
    public static final byte FT_MAP = 5;
    public static final byte FT_STRUCT = 6;
    public static final byte FT_DOUBLE = 7;
    public static final byte FT_BINARY = 8; //二进制



    private byte[] inBuffer;
    private int inCursor = 0;
    private byte[] outBuffer;
    private int outCursor = 0;




    /**
     * 重置缓冲区 data 覆盖inBuffer 游标置为0
     * @param data
     */
    public void resetInBuff(byte[] data){
        inBuffer = data;
        inCursor = 0;
    }

    public void setInCursor(int cursor){
        inCursor = cursor;
    }

    public int getInCursor(){
        return inCursor;
    }

    public void resetOutBuff(byte[] data){
        outBuffer = data;
        outCursor = 0;
    }

    public void resetOutCursor(){
        outCursor = 0;
    }

    public void setOutCursor(int cursor){
        outCursor = cursor;
    }

    public int getOutCursor(){
        return outCursor;
    }

    /**
     * 判断类型匹配，
     * 匹配返回true
     * 不匹配 则number <-> double string <-> binary 这两个可以互转 返回true
     * @param src
     * @param dst
     * @return
     */
    public static boolean matchType(byte src,byte dst){
        if(src == dst) return true;
        switch(src){
            case FT_NUMBER:
                if(dst == FT_DOUBLE) return true;
                return false;
            case FT_DOUBLE:
                if(dst == FT_NUMBER) return true;
                return false;
            case FT_STRING:
                if(dst == FT_BINARY) return true;
                return false;
            case FT_BINARY:
                if(dst == FT_STRING) return true;
                return false;
            default:
                return false;
        }
    }

    public static int stringLen(String str){
        try{
            if(null == str){
                return 0;
            }
            return str.getBytes(ENCODE).length;
        }catch (Exception e){
            return 0;
        }
    }

    /**
     * 目前不知道干嘛的？
     * @param data
     * @param len
     * @return
     */
    public static byte calcLrc(byte[] data,int len){
        if(data.length < len) return 0;
        byte lrc = 0;
        for(int i=0; i < len; i++){
            //a^=b相当于：a=a^b； 异或就是两个数的二进制形式，按位对比，相同取0，不同取。
            lrc ^= data[i];
        }
        return lrc;
    }

    public static int getSize(byte ch){
        return 1;
    }

    /**
     *
     * @param val
     * @return
     */
    public static int getSize(long val){
        int s = 0;
        do{
            //无符号右移7 位
            val >>>= 7;
            s++;
        }while (val >0);
        return s;
    }

    public static int getSize(short val){
        return getSize((long)val);
    }

    public static int getSize(int val){
        return getSize((long)val);
    }

    public static int getSize(float val){
        double dval = val;
        return getSize(dval);
    }

    public static int getSize(double val){
        long longVal = Double.doubleToLongBits(val);
        return getSize(longVal);
    }

    public static int getSize(byte[] data){
        if(null == data) return 1;
        return getSize(data.length) + data.length;
    }

    public static int getSize(String data){
        int s = stringLen(data);
        return getSize(s) + s;
    }

    public static byte[] compressData(byte[] data, int offset) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DeflaterOutputStream zipOut = new DeflaterOutputStream(byteArrayOutputStream);
        zipOut.write(data,offset,data.length - offset);
        zipOut.finish();
        zipOut.flush();
        zipOut.close();
        int size = byteArrayOutputStream.size();
        int len = offset + 5 + size;
        byte[] result = new byte[len];
        System.arraycopy(data,0,result,0,offset);
        PackData packer = new PackData();
        packer.resetOutBuff(result);
        packer.setOutCursor(offset);
        packer.packBytes(byteArrayOutputStream.toByteArray());
        byteArrayOutputStream.close();
        return result;
    }


    public static byte[] decompressData(byte[] data,int offset) throws IOException {
        PackData packer = new PackData();
        packer.resetInBuff(data);
        packer.setInCursor(offset);
        int len = 0;
        try{
            len = packer.unpackInt();
        }catch (Exception e){
            return "".getBytes();
        }
        int pos = packer.getInCursor();
        ByteArrayInputStream in = new ByteArrayInputStream(data,pos,data.length - pos);
        InflaterInputStream zipIn = new InflaterInputStream(in);
        ByteArrayOutputStream  outputStream = new ByteArrayOutputStream();
        byte[] tmpbuf = new byte[len + 100];
        int size = -1;
        while((size = zipIn.read(tmpbuf)) > 0 ){
            outputStream.write(tmpbuf,0,size);
        }
        zipIn.close();
        byte[] rawdata = outputStream.toByteArray();
        byte[] result = new byte[rawdata.length + offset];
        System.arraycopy(data,0,result,0,offset);
        System.arraycopy(rawdata,0,result,offset,rawdata.length);
        outputStream.close();
        return result;
    }

    /**
     *  unsigned 无符号
     *  & 按位与
     *  0x0ff是十六进制表示法，即是十进制的255
     * @param c
     * @return
     */
    public static short toUnsigned(byte c){
        return (short)(c & 0x0ff);
    }

    public static int toUnsigned(short s){
        return s & 0x0ff;
    }

    public static short calcCheckCode(byte[] data,int offset){
        short checkcode = 0;
        for(int i = offset; i < data.length; i++){
            checkcode += toUnsigned(data[i]);
        }
        return checkcode;
    }

    public byte unpackByte() throws PackException {
        if(inBuffer.length < inCursor + 1){
            throw new PackException(PACK_LENGTH_ERROR,"PACK_LENGTH_ERROR");
        }
        return inBuffer[inCursor++];
    }

    public boolean unpackBool() throws PackException {
        byte val = unpackByte();
        return val != 0;
    }

    public long unpackLong() throws PackException {
        int exp = 0;
        long val = 0;
        for(; inCursor < inBuffer.length; exp +=7){
            byte ch = inBuffer[inCursor++];
            //0x80这是十六进制数，变成十进制数为-128，
            // 因为char型在C语言中范围为-128~127，并不是0乘以80，c语言中乘以用*,例如0*80，表示0乘以80。
            if((ch & 0x80) == 0){
                val += ((long)ch << exp);
                return val;
            }
            ch &= ~0x80;
            val += ((long)ch << exp);
        }
        throw new PackException(PACK_LENGTH_ERROR,"PACK_LENGTH_ERROR");
    }

    public short unpackShort() throws PackException {
        return (short)unpackLong();
    }

    public int unpackInt() throws PackException {
        return (int)unpackLong();
    }

    public float unpackFloat() throws PackException {
        double dval = unpackDouble();
        return (float)dval;
    }
    public double unpackDouble() throws PackException {
        long longval = unpackLong();
        return Double.longBitsToDouble(longval);
    }

    public byte[] unpackBytes() throws PackException {
        int len = unpackInt();
        if(len == 0) return null;
        if(inBuffer.length < inCursor + len) throw new PackException(PACK_LENGTH_ERROR,"PACK_LENGTH_ERROR");
        byte[] retval = new byte[len];
        System.arraycopy(inBuffer,inCursor,retval,0,len);
        inCursor += len;
        return retval;
    }

    /**
     * 解包字符串  按存储方式 第一个值存的字符串的长度，后面才是字符串内容
     * @return
     * @throws PackException
     */
    public String unpackString() throws PackException {
        int len = unpackInt();
        if(len == 0) return null;
        if(inBuffer.length < inCursor + len) throw new PackException(PACK_LENGTH_ERROR,"PACK_LENGTH_ERROR");
        String retVal = null;
        try{
            retVal = new String(inBuffer,inCursor,len,ENCODE);
        }catch (Exception e){

        }
        inCursor += len;
        return retVal;
    }

    public int peekField(FieldType field) throws PackException {
        int bakCur = inCursor;
        int len = 0, num = 0;
        switch (field.baseType){
            case FT_CHAR:
                len = 1;
                break;
            case FT_NUMBER:
            case FT_DOUBLE:
            {
                unpackLong();
                return inCursor - bakCur;
            }
            case FT_STRING:
            case FT_BINARY:
                len = unpackInt();
                break;
            case FT_VECTOR:
                num = unpackInt();
                {
                    FieldType fld = (FieldType) field.subType.get(0);
                    for(int i = 0; i < num; i++){
                        peekField(fld);
                    }
                }
                break;
            case FT_MAP:
                {
                    num = unpackInt();
                    FieldType fldKey = (FieldType)field.subType.get(0);
                    FieldType fldVal = (FieldType)field.subType.get(1);
                    for(int i = 0; i < num; i++){
                        peekField(fldKey);
                        peekField(fldVal);
                    }
                    break;
                }
            case FT_STRUCT:
                num = toUnsigned(unpackByte());
                for(int i = 0; i < num; i++){
                    peekField();
                }
                break;
            default:
                throw new PackException(PACK_INVALID,"PACK_INVALID");
        }
        inCursor += len;
        if(inBuffer.length < inCursor) throw new PackException(PACK_LENGTH_ERROR,"PACK_LENGTH_ERROR");
        return inCursor = bakCur;
    }

    public FieldType unpackFieldType() throws PackException {
        FieldType field = new FieldType();
        field.baseType = unpackByte();
        switch (field.baseType){
            case FT_VECTOR:
                field.subType = new ArrayList<FieldType>(1);
                field.subType.add(unpackFieldType());
                break;
            case FT_MAP:
                field.subType = new ArrayList<>(2);
                field.subType.add(unpackFieldType());
                field.subType.add(unpackFieldType());
                break;
            default:
                break;
        }
        return field;
    }

    public void peekField() throws PackException {
        FieldType field = unpackFieldType();
        peekField(field);
    }

    public void packByte(byte val){
        outBuffer[outCursor++] = val;
    }
    public void packPool(boolean val){
        byte ch = (byte)(val ? 1 : 0);
        packByte(ch);
    }

    public void packLong(long val){
        do{
            byte ch = (byte)(val & 0x7f);
            val >>>= 7;
            if(val > 0) ch |= 0x80;
            outBuffer[outCursor++] = ch;
        }while (val > 0);
    }

    public void packShort(short val){
        long v = (val & 0x0ffff);
        packLong(v);
    }

    public void packInt(int val){
        long v = (long)val;
        packLong(v);
    }

    public void packFloat(float val){
        double dval = val;
        packDouble(dval);
    }

    public void packDouble(double val){
        long longval = Double.doubleToLongBits(val);
        packLong(longval);
    }

    public void packBytes(byte[] val){
        if(val == null){
            packInt(0);
        }else{
            int len = val.length;
            packInt(len);
            System.arraycopy(val,0,outBuffer,outCursor,len);
            outCursor += len;
        }
    }

    /**
     * 封装包字符串  按存储方式 第一个值存的字符串的长度，后面才是字符串内容
     * @param val
     */
    public void packString(String val){
        try{
            if(null == val){
                packInt(0);
            }else{
                byte[] byteval = val.getBytes(ENCODE);
                packBytes(byteval);
            }
        }catch (Exception e){}
    }

    public void packFieldType(FieldType fieldType){
        packByte(fieldType.baseType);
        switch (fieldType.baseType){
            case FT_VECTOR:
                packFieldType((FieldType) fieldType.subType.get(0));
                break;
            case FT_MAP:
                packFieldType((FieldType) fieldType.subType.get(0));
                packFieldType((FieldType) fieldType.subType.get(1));
                break;
            default:
                break;
        }
    }

    public static byte[] PackInt(int val){
        int len = getSize(val);
        byte[] buff = new byte[len];
        PackData packer = new PackData();
        packer.resetOutBuff(buff);
        packer.packInt(val);
        return buff;
    }

    public static boolean string2Struct(byte[] src, PackStruct dst){
        PackData packer = new PackData();
        packer.resetInBuff(src);
        try{
            dst.unpackData(packer);
        }catch (PackException e){
            return false;
        }
        return true;
    }

    public static byte[] struct2String(PackStruct src){
        byte[] result = new byte[src.size()];
        PackData packer = new PackData();
        packer.resetOutBuff(result);
        src.packData(packer);
        return result;
    }
}
