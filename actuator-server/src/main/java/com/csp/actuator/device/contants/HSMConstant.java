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

    /**
     * 启明密码机动态库名称
     */
    public static final List<String> VENUS_DLL_NAME_LIST = Lists.newArrayList("libcsp_session","libhsm");

    /**
     * 三未动态库名称
     */
    public static final String SANSEC_DLL_NAME = "swsds";

    /**
     * 密码机密码
     */
    public static final String DEVICE_PASSWORD = "88888888";
}