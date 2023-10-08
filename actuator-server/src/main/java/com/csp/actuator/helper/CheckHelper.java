package com.csp.actuator.helper;

import com.csp.actuator.api.base.DataCenterInfo;
import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.config.DataCenterConfig;
import com.csp.actuator.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 校验Helper
 *
 * @author Weijia Jiang
 * @version v1
 * @description 校验Helper
 * @date Created in 2023-10-08 14:21
 */
@Slf4j
public class CheckHelper {

    public static boolean checkDataCenterId(String msg) {
        log.info("CheckDataCenterId...");
        // 所有的消息都继承了DataCenterInfo，所以将当前msg转换成DataCenterInfo，校验一下它的数据中心ID
        DataCenterInfo dataCenterInfo = JsonUtils.readValue(msg, DataCenterInfo.class);
        log.info("DataCenterInfo :{}", dataCenterInfo);
        // 获取当前执行节点配置的数据中心信息
        DataCenterConfig dataCenterConfig = SpringUtils.getBean(DataCenterConfig.class);
        log.info("DataCenterConfig :{}", dataCenterConfig);
        return dataCenterConfig.getId().equals(dataCenterInfo.getDataCenterId());
    }

}
