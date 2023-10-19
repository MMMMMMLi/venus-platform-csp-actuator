package com.csp.actuator.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.security.Security;

public class SM4Util {
    // 算法
    public static final String ALGORITHM_NAME = "SM4";
    // 定义分组加密模式使用：PKCS5Padding
    public static final String ALGORITHM_NAME_ECB_PADDING = "SM4/ECB/PKCS5Padding";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 生成密钥，一般传入128位
     *
     * @param keySize 密钥位数
     * @return byte[]
     */
    public static byte[] generateKey(int keySize) {
        try {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            Security.addProvider(new BouncyCastleProvider());
            KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM_NAME, BouncyCastleProvider.PROVIDER_NAME);
            kg.init(keySize, new SecureRandom());
            return kg.generateKey().getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 生成128位密钥-base64格式的
     * SM4算法只支持128位的密钥（转换成byte[]后，长度应为16）
     *
     * @return String
     */
    public static String generateKey() {
        byte[] bytes = generateKey(128);
        return bytes == null ? null : Base64.toBase64String(bytes);
    }

    /**
     * sm4加密
     */
    public static byte[] encrypt(byte[] key, byte[] data) {
        return encrypt(key, Base64.toBase64String(data));
    }

    /**
     * sm4加密
     */
    public static byte[] encrypt(byte[] key, String data) {
        try {
            Cipher cipher = generateEcbCipher(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * sm4加密(base64)
     */
    public static String encrypt(String keyStr, String data) {
        try {
            byte[] key = Base64.decode(keyStr);
            Cipher cipher = generateEcbCipher(Cipher.ENCRYPT_MODE, key);
            return Base64.toBase64String(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * sm4解密
     */
    public static String decrypt(byte[] key, byte[] data) {
        try {
            Cipher cipher = generateEcbCipher(Cipher.DECRYPT_MODE, key);
            byte[] decrypt = cipher.doFinal(data);
            return new String(decrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * sm4解密(base64)
     */
    public static byte[] decrypt(byte[] key, String data) {
        try {
            Cipher cipher = generateEcbCipher(Cipher.DECRYPT_MODE, key);
            return Base64.decode(cipher.doFinal(Base64.decode(data)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * sm4解密(base64)
     */
    public static String decrypt(String keyStr, String data) {
        try {
            byte[] key = Base64.decode(keyStr);
            Cipher cipher = generateEcbCipher(Cipher.DECRYPT_MODE, key);
            byte[] decrypt = cipher.doFinal(Base64.decode(data));
            return new String(decrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 生成ECB暗号
     */
    private static Cipher generateEcbCipher(int mode, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance(SM4Util.ALGORITHM_NAME_ECB_PADDING, BouncyCastleProvider.PROVIDER_NAME);
        Key sm4Key = new SecretKeySpec(key, ALGORITHM_NAME);
        cipher.init(mode, sm4Key);
        return cipher;
    }
}
