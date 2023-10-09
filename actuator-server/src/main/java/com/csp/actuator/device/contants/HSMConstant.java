package com.csp.actuator.device.contants;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * 密码机常量类
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密码机常量类
 * @date Created in 2023-03-22 10:57
 */
public class HSMConstant {

    public static final Integer DEFAULT_LINK_NUM = 1;

    public static final Integer DEFAULT_TINE_OUT = 60;

    /**
     * 江南天安服务器密码机的设备类型
     */
    public static final String TASS_HSM_MODEL = "GHSM";
    /**
     * 三未密码机加载的配置文件
     */
    public static final String SANSEC_CONFIG_FILE = "swsds.ini";
    /**
     * 启明自研密码机加载的配置文件
     */
    public static final String VENUS_CONFIG_FILE = "hsmConfig.ini";


    public static List<Integer> CANNOT_DELETE_KEYINDEX = Lists.newArrayList(1);


    /**
     * 专属的LMK密钥索引位（前10个留给平台密码机使用）
     */
    public static Integer GLOBAL_LMK_KEYINDEX = 11;

    /**
     * 共享时，存到数据库的默认的密钥索引
     */
    public static Integer DEFAULT_DATABASE_KEYINDEX = 0;
}