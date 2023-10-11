package com.csp.actuator.api.constants;

/**
 * Topic信息常量
 *
 * @author Weijia Jiang
 * @version v1
 * @description Topic信息常量
 * @date Created in 2023-10-09 10:04
 */
public class TopicNameConstant {

    /**
     * 执行节点上报
     */
    public static final String ACTUATOR_STATUS_REPORT = "actuator_status_report";

    /**
     * 确认数据中心是否存在
     */
    public static final String CONFIRM_DATA_CENTER = "confirm_data_center";

    /**
     * 确认数据中心是否存在回调
     */
    public static final String CONFIRM_DATA_CENTER_CALL_BACK = "confirm_data_center_call_back";

    /**
     * 创建密钥
     */
    public static final String GENERATE_KEY = "generate_key";

    /**
     * 创建密钥回调
     */
    public static final String GENERATE_KEY_CALL_BACK = "generate_key_call_back";

    /**
     * 销毁密钥
     */
    public static final String DESTROY_KEY = "destroy_key";

    /**
     * 销毁密钥回调
     */
    public static final String DESTROY_KEY_CALL_BACK = "destroy_key_call_back";

    /**
     * 同步密钥
     */
    public static final String SYNC_KEY = "sync_key";

    /**
     * 同步密钥回调
     */
    public static final String SYNC_KEY_CALL_BACK = "sync_key_call_back";
}
