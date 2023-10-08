package com.csp.actuator.helper;

import com.csp.actuator.api.base.DataCenterInfo;
import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.config.DataCenterConfig;
import com.csp.actuator.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

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

    public static boolean checkMsgTime(String msg, Long MSG_EFFECTIVE_TIME) {
        log.info("checkMsgTime...");
        // 所有的消息都继承了DataCenterInfo，所以将当前msg转换成DataCenterInfo，校验一下它的数据中心ID
        DataCenterInfo dataCenterInfo = JsonUtils.readValue(msg, DataCenterInfo.class);
        log.info("DataCenterInfo :{}", dataCenterInfo);
        long nowDate = System.currentTimeMillis();
        log.info("NowDate: {}", nowDate);
        return nowDate - dataCenterInfo.getDate() <= MSG_EFFECTIVE_TIME;
    }

    public static boolean checkDataCenterIdAndMsgTime(String msg, Long MSG_EFFECTIVE_TIME) {
        log.info("CheckDataCenterIdAndMsgTime...");
        // 所有的消息都继承了DataCenterInfo，所以将当前msg转换成DataCenterInfo，校验一下它的数据中心ID
        DataCenterInfo dataCenterInfo = JsonUtils.readValue(msg, DataCenterInfo.class);
        log.info("DataCenterInfo :{}", dataCenterInfo);
        // 获取当前执行节点配置的数据中心信息
        DataCenterConfig dataCenterConfig = SpringUtils.getBean(DataCenterConfig.class);
        log.info("DataCenterConfig :{}", dataCenterConfig);
        if (!dataCenterConfig.getId().equals(dataCenterInfo.getDataCenterId())) {
            log.info("CheckDataCenterId failed, this msg does not belong to the current data center...");
            return false;
        }
        Long msgDate = dataCenterInfo.getDate();
        if (Objects.isNull(msgDate)) {
            log.info("CheckMsgTime failed, msgDate is not setting...");
            return false;
        }
        long nowDate = System.currentTimeMillis();
        log.info("NowDate: {}", nowDate);
        if (nowDate - msgDate > MSG_EFFECTIVE_TIME * 60 * 1000) {
            log.info("CheckMsgTime failed, Timeliness verification failed...");
            return false;
        }
        return true;
    }

}
