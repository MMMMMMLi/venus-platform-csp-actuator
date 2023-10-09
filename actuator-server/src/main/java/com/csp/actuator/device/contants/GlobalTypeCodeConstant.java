package com.csp.actuator.device.contants;

/**
 * 公共密码类型实体类
 *
 * @author Weijia Jiang
 * @version v1
 * @description 公共密码类型实体类
 * @date Created in 2023-03-18 16:07
 */
public class GlobalTypeCodeConstant {

    /**
     * 对称密钥
     */
    public static final Integer SYMMETRIC_KEY = 0;

    /**
     * RSA密钥
     */
    public static final Integer RSA_KEY = 1;

    /**
     * ECC密钥
     */
    public static final Integer ECC_KEY = 2;
    /**
     * SM9系统主密钥
     */
    public static final Integer SM9_KEY = 5;
    /**
     * SM9KGC公钥
     */
    public static final Integer SM9KGC = 6;
    /**
     * SM9用户主密钥;
     */
    public static final Integer SM9_USER_KEY = 7;




    public static final Integer ERROR_KEY = 99999;
}
