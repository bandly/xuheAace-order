package com.xuhe.aace.packer;

public class PackException extends Exception{

    private int errorCode = 0;

    public PackException(int errorCode,String reason){
        super(reason);
        errorCode = errorCode;
    }

    public PackException(int errorCode){
        this(errorCode,"unknown");
    }

    public int getErrorCode(){
        return errorCode;
    }

    public void setErrorCode(int errorCode){
        errorCode = errorCode;
    }
}
