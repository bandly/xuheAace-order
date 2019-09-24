package com.xuhe.aace.test;

import com.xuhe.aace.packer.PackData;

import java.awt.*;

public class Test {

    public static void main(String[] args) {
        System.out.println(1000&0x0ff);
        System.out.println(-56&0x0ff);

        System.out.println("aaa".length());
        int newLen = PackData.getSize("aaa");
        System.out.println(newLen);

        long val = System.currentTimeMillis() / 1000;
        System.out.println(val << 31);

        

    }
}
