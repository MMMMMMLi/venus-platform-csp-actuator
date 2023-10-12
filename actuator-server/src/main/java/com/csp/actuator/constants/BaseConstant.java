package com.csp.actuator.constants;

/**
 * 基础常量
 *
 * @author Weijia Jiang
 * @version v1
 * @description 基础常量
 * @date Created in 2023-10-10 15:44
 */
public class BaseConstant {
    /**
     * 专属的LMK密钥索引位（前10个留给平台密码机使用）
     */
    public static Integer GLOBAL_LMK_KEYINDEX = 11;
    public static final String DEFAULT_SUCCESS_MESSAGE = "执行成功";
    public static final String DEFAULT_FAILED_MESSAGE = "执行失败";

    public static final String ERROR_OPERATION_NOT_FOUND = "操作设备接口类型未找到";
    public static final String ERROR_KEY_INFO_NOT_FOUND = "创建密钥属性为空，请检查重试";
    public static final String ERROR_DESTROY_KEY_INFO_NOT_FOUND = "销毁密钥属性为空，请检查重试";
    public static final String ERROR_KEY_DEV_MODEL_CODE_NOT_FOUND = "设备类型编码为空，请检查重试";
    public static final String ERROR_KEY_DEV_INFO_NOT_FOUND = "设备信息为空，请检查重试";
    public static final String ERROR_KEY_NO_KEY_INDEX = "创建密钥失败，暂无可用密钥";
    public static final String ERROR_GET_KEY_INDEX_FAILED = "创建密钥失败，获取可用密钥信息失败";
    public static final String ERROR_DATA_CENTER_ID_NOT_FOUND = "数据中心配置未找到";
}
