package com.csp.actuator.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

/**
 * @Auth yulang
 * @Date 2022/8/30
 * @Version 1.0
 */
@Slf4j
public class EncodeConvertUtil {
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final byte[] DECODING_TABLE = new byte[128];

    static {
        for ( byte i=0; i<HEX_DIGITS.length; i++ ) {
            DECODING_TABLE[HEX_DIGITS[i]] = i;
        }
    }

    /**
     *
     * 合并两个字节数组 </br>
     * @param data1 字节数组1
     * @param data2 字节数组2
     * @return </br>
     */
    public static byte[] mergeByteArray(byte[] data1, byte[] data2) {
        byte[] result = new byte[data1.length + data2.length];
        System.arraycopy(data1,  0, result, 0, data1.length);
        System.arraycopy(data2,  0, result, data1.length, data2.length);
        return result;
    }

    // byte数组长度为4, bytes[3]为高8位
    public static int bytes2Int(byte[] bytes){
        int value=0;
        value = ((bytes[3] & 0xff)<<24)|
                ((bytes[2] & 0xff)<<16)|
                ((bytes[1] & 0xff)<<8)|
                (bytes[0] & 0xff);
        return value;
    }

    public static byte[] int2bytes(int i) {
        byte[] rt = new byte[4];
        rt[0] = (byte) ((i >> 0) & 0xFF);
        rt[1] = (byte) ((i >> 8) & 0xFF);
        rt[2] = (byte) ((i >> 16) & 0xFF);
        rt[3] = (byte) ((i >> 24) & 0xFF);
        return rt;
    }

    /**
     *
     * 合并多个字节数组 </br>
     * @param datas
     * @return </br>
     */
    public static byte[] mergeByteArray(byte[]... datas) {
        if (datas == null) {
            return null;
        }
        int size = datas.length;
        byte[] result =datas[0];
        for (int i = 1; i < size; i++) {
            result =mergeByteArray(result, datas[i]);
        }
        return result;
    }

    /**
     *
     * 整数转字节数组 </br>
     * @param len 整数
     * @return </br>
     */
    public static byte[] intToByteArray(int len) {
        byte[] result = new byte[4];
        result[0] = (byte) (len >> 24 & 0xff);
        result[1] = (byte) (len >> 16 & 0xff);
        result[2] = (byte) (len >> 8 & 0xff);
        result[3] = (byte) (len & 0xff);
        return result;
    }

    /**
     *
     * 整型转字符数组 </br>
     * @param len
     * @return </br>
     */
    public static byte[] intToCharArray(int len) {
        return padRight(String.valueOf(len), 4, '0').getBytes();
    }

    /**
     *
     * 左侧补充指定字符 </br>
     * @param src 源字符串
     * @param len 想要的长度
     * @param ch 想要补充的字符
     * @return </br>
     */
    public static String padRight(String src, int len, char ch) {
        int diff = len - src.length();
        if (diff <= 0) {
            return src;
        }

        char[] charr = new char[len];
        System.arraycopy(src.toCharArray(), 0, charr, diff, src.length());
        for (int i = 0; i < diff; i++) {
            charr[i] = ch;
        }
        return new String(charr);
    }

    /**
     * 将字节数组转换为十六进制形式的字符串。
     * @param bin 字节数组。
     * @return 十六进制字符串。
     */
    public static String bin2hex(byte[] bin) {
        final StringBuilder sb = new StringBuilder(bin.length*2);
        for ( int i=0; i<bin.length; i++ ) {
            sb.append(HEX_DIGITS[(bin[i]>>4)&0x0f]);
            sb.append(HEX_DIGITS[bin[i]&0x0f]);
        }
        return sb.toString();
    }

    /**
     * 将十六进制形式的字符串转换为字节数组。
     * @param hex 十六进制字符串。
     * @return 字节数组。
     */
    public static byte[] hex2bin(String hex) {
        if(hex.length() % 2 == 1) {
            hex = "0"+hex;
        }
        hex = hex.toUpperCase();
        byte[] bytes  = new byte[hex.length()/2];
        for ( int i=0; i<hex.length(); i+=2 ) {
            byte b1 = DECODING_TABLE[hex.charAt(i)];
            byte b2 = DECODING_TABLE[hex.charAt(i+1)];
            bytes[i/2] = (byte)(b1<<4 | (b2 & 0xff));
        }
        return bytes;
    }

    /**
     * 字符串转化成为16进制字符串
     * @param s
     * @return
     */
    public static String strTo16(String s) {
        String str = "";
        for (int i = 0; i < s.length(); i++) {
            int ch = (int) s.charAt(i);
            String s4 = Integer.toHexString(ch);
            str = str + s4;
        }
        return str;
    }

    /**
     * 16进制转换成为string类型字符串
     * @param s
     * @return
     */
    public static String hexStringToString(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        s = s.replace(" ", "");
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                log.error("", e);
            }
        }
        try {
            s = new String(baKeyword, "UTF-8");
            new String();
        } catch (Exception e1) {
            log.error("", e1);
        }
        return s;
    }

    /**
     * 获取公钥长度 </br>
     * @param bytes
     * @param offset
     * @return </br>
     */
    public static int calcDerBuffLength(byte[] bytes, int offset) {
        int len = 0;
        if (bytes[offset] != 48) {
            len = 0;
        } else {
            len = bytes[(offset + 1)] & 0xFF;
            if (len <= 128)
                len += 2;
            else if (len - 128 > 3)
                len = 0;
            else
                len = bytes2int(bytes, offset + 2, len - 128) + 2 + (len - 128);
        }
        return len;
    }

    public static int bytes2int(byte[] bytes, int from, int to) {
        if (bytes == null)
            return 0;
        int i = 255;
        int j = 0;
        int k = 0;
        for (int m = 0; m < to; m++) {
            k <<= 8;
            j = bytes[(from + m)] & i;
            k |= j;
        }
        return k;
    }

    /**
     *
     * 将字节数组编码，转换成字符串 </br>
     * @param src
     * @return </br>
     */
    public static String byteToBase64(byte[] src) {
        return Base64.getEncoder().encodeToString(src);
    }

    /**
     *
     * 将字符串解码 </br>
     * @param src
     * @return </br>
     */
    public static byte[] base64ToByte(String src) {
        return Base64.getDecoder().decode(src);
    }
}
