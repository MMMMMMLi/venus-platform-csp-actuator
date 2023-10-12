package com.csp.actuator.constants;

/**
 * Topic名字
 *
 * @author Weijia Jiang
 * @version v1
 * @description Topic名字
 * @date Created in 2023-10-08 12:02
 */
public class TopicBingingName {

    /**
     * 数据节点上报Topic
     */
    public static final String NODE_REPORT_BINGING_NAME = "actuatorStatusReport-out-0";

    /**
     * 数据中心节点确认回调
     */
    public static final String CONFIRM_DATA_CENTER_CALL_BACK_BINGING_NAME = "confirmDataCenterCallBack-out-0";

    /**
     * 创建密钥回调
     */
    public static final String GENERATE_KEY_CALL_BACK_BINGING_NAME = "generateKeyCallBack-out-0";

    /**
     * 销毁密钥回调
     */
    public static final String DESTROY_KEY_CALL_BACK_BINGING_NAME = "destroyKeyCallBack-out-0";
}
