package com.csp.actuator.utils;

/**
 * @author Weijia Jiang
 * @version v1
 * @description
 * @date Created in 2023-05-08 17:51
 */
public class Padding {

    public Padding() {
    }

    public static byte[] PBOCEncPadding(byte[] message, int blockSize) {
        if (message.length % blockSize == 0) {
            return message;
        } else {
            int newLeng = (message.length / blockSize + 1) * blockSize;
            byte[] ret = new byte[newLeng];
            System.arraycopy(message, 0, ret, 0, message.length);
            ret[message.length] = -128;
            return ret;
        }
    }

    public static byte[] PBOCEncUnPadding(byte[] message, int blockSize) {
        if (message.length % blockSize != 0) {
            return null;
        } else {
            int i;
            for (i = message.length - 1; message[i] == 0; --i) {
            }

            if (message[i] == -128) {
                --i;
            }

            if (message.length - i + 1 > blockSize) {
                return null;
            } else {
                byte[] outb = new byte[i + 1];
                System.arraycopy(message, 0, outb, 0, i + 1);
                return outb;
            }
        }
    }

    public static byte[] PBOCMacPadding(byte[] message, int blockSize) {
        return ISO9797PaddingMethod2(message, blockSize);
    }

    public static byte[] PBOCMacUnPadding(byte[] message, int blockSize) {
        return ISO9797UnPaddingMethod2(message, blockSize);
    }

    public static byte[] ISO9797PaddingMethod2(byte[] message, int blockSize) {
        int newLeng = (message.length / blockSize + 1) * blockSize;
        byte[] ret = new byte[newLeng];
        System.arraycopy(message, 0, ret, 0, message.length);
        ret[message.length] = -128;
        return ret;
    }

    public static byte[] ISO9797UnPaddingMethod2(byte[] message, int blockSize) {
        if (message.length % blockSize != 0) {
            return null;
        } else {
            int i;
            for (i = message.length - 1; message[i] == 0; --i) {
            }

            if (message[i] != -128) {
                return null;
            } else {
                --i;
                if (message.length - i + 1 >= blockSize) {
                    return null;
                } else {
                    byte[] outb = new byte[i + 1];
                    System.arraycopy(message, 0, outb, 0, i + 1);
                    return outb;
                }
            }
        }
    }

    public static byte[] ISO9797PaddingMethod1(byte[] message, int blockSize) {
        if (message.length % blockSize == 0) {
            return message;
        } else {
            int newLeng = (message.length / blockSize + 1) * blockSize;
            byte[] ret = new byte[newLeng];
            System.arraycopy(message, 0, ret, 0, message.length);
            return ret;
        }
    }

    public static byte[] ISO9797UnPaddingMethod1(byte[] message, int blockSize) {
        if (message.length % blockSize != 0) {
            return null;
        } else {
            int i;
            for (i = message.length - 1; message[i] == 0; --i) {
            }

            if (message.length - i + 1 >= blockSize) {
                return null;
            } else {
                byte[] outb = new byte[i + 1];
                System.arraycopy(message, 0, outb, 0, i + 1);
                return outb;
            }
        }
    }

    public static byte[] ANSIX923Padding(byte[] message, int blockSize) {
        if (blockSize > 255) {
            return null;
        } else {
            int newLeng = (message.length / blockSize + 1) * blockSize;
            byte[] ret = new byte[newLeng];
            System.arraycopy(message, 0, ret, 0, message.length);
            ret[ret.length - 1] = (byte) (ret.length - message.length);
            return ret;
        }
    }

    public static byte[] ANSIX923UnPadding(byte[] message, int blockSize) {
        if (blockSize <= 255 && blockSize >= message[message.length - 1]) {
            int i;
            for (i = 2; i < message[message.length - 1]; ++i) {
                if (message[message.length - i] != 0) {
                    return null;
                }
            }

            i = message.length - (message[message.length - 1] & 255);
            byte[] ret = new byte[i];
            System.arraycopy(message, 0, ret, 0, ret.length);
            return ret;
        } else {
            return null;
        }
    }

    public static byte[] PKCS5Padding(byte[] message, int blockSize) {
        if (blockSize > 255) {
            return null;
        } else {
            int newLeng = (message.length / blockSize + 1) * blockSize;
            byte[] ret = new byte[newLeng];
            System.arraycopy(message, 0, ret, 0, message.length);

            for (int i = message.length; i < ret.length; ++i) {
                ret[i] = (byte) (ret.length - message.length);
            }
            return ret;
        }
    }

    public static byte[] PKCS5UnPadding(byte[] message, int blockSize) {
        if (blockSize > 255) {
            return null;
        } else if (message.length % blockSize == 0 && message[message.length - 1] <= blockSize) {
            int newLeng = message.length - message[message.length - 1];
            byte[] ret = new byte[newLeng];

            for (int i = message.length - 1; i >= ret.length; --i) {
                if (message[i] != (byte) (message.length - newLeng & 255)) {
                    return null;
                }
            }

            System.arraycopy(message, 0, ret, 0, ret.length);
            return ret;
        } else {
            return message;
        }
    }
}
