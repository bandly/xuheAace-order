package com.xuhe.aace.packer;

public interface PackStruct {

    int size();
    void packData(PackData packData);
    void unpackData(PackData packData) throws PackException;
}
