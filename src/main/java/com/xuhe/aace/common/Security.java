package com.xuhe.aace.common;

import com.xuhe.aace.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;

public class Security {

    private static final String DEFAULT_PUBLICKEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDBRmrT/EiiBlfvuPxybg/Prfb6/wBBu46STyXstJyiDubzeXsKDzHKeI5hDmM9YalywOPDXDM4p+GWmDO9RDhxU5c7lcgHbEkzuf1atc9qbLYCeGkLQzxzb28TtQO9tiTlLfmIl/BqBNjWrpOXfzz2E1yjaKxbrbkV1j5P9ZJVTQIDAQAB";

    //rsa

    public static PublicKey getPublicKey(String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public static byte[] getRSAEncryptContent(String publicKey, byte[] content){
        try{
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            PublicKey key = getPublicKey(publicKey);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(content);
        }catch (Exception e){
            Logger.ErrLog(e);
            return null;
        }
    }

    public static byte[] getRSAEncryptContent(byte[] content){
        return getRSAEncryptContent(DEFAULT_PUBLICKEY, content);
    }

    public static PrivateKey getPrivateKey(String privateKey) throws Exception{
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static byte[] getRSADecryptContent(PrivateKey privateKey, byte[] content){
        try{
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(content);
        }catch (Exception e){
            Logger.ErrLog(e);
            return null;
        }
    }

    //aes

    public static SecretKeySpec getAESKey(byte[] srcKey){
        return new SecretKeySpec(srcKey, "AES");
    }

    public static byte[] getAESEncryptContent(byte[] key, byte[] content){
        return getAESEncryptContent(getAESKey(key), content);
    }
    public static byte[] getAESDecryptContent(byte[] key, byte[] content){
        return getAESDecryptContent(getAESKey(key), content);
    }

    public static byte[] getAESEncryptContent(SecretKeySpec key, byte[] content){
        try{
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(content);
        }catch (Exception e){
            Logger.ErrLog(e);
            return null;
        }
    }

    public static byte[] getAESDecryptContent(SecretKeySpec key, byte[] content){
        try{
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(content);
        }catch (Exception e){
            Logger.ErrLog(e);
            return null;
        }
    }

    /**
     * 随机生成一个aes key
     * @return
     */
    public static byte[] genAESKey() {
        Random rnd = new Random();
        byte[] rawKey = new byte[16];
        for(int i = 0; i < 16; i++) {
            rawKey[i] = (byte)rnd.nextInt();
        }
        return rawKey;
    }

}
