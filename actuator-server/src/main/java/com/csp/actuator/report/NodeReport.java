package com.csp.actuator.report;

import com.csp.actuator.api.data.center.ActuatorNodeStatusTopicInfo;
import com.csp.actuator.api.enums.ActuatorStatusEnum;
import com.csp.actuator.config.DataCenterConfig;
import com.csp.actuator.constants.TopicBingingName;
import com.csp.actuator.message.producer.MessageProducer;
import com.csp.actuator.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * 节点上报接口
 *
 * @author Weijia Jiang
 * @version v1
 * @description 节点上报接口
 * @date Created in 2023-10-08 11:54
 */
@Slf4j
@Component
@EnableScheduling
public class NodeReport {

    @Autowired
    private DataCenterConfig dataCenterInfo;

    @Value("${server.port}")
    private String serverPort;

    public static final String DEFAULT_CENTER_INFO_VALUE = "default";

    /**
     * 定时上报为启动之后的5min为第一次，后续每10min上报一次
     */
    @Scheduled(initialDelayString = "${node.report.initialDelay}", fixedDelayString = "${node.report.fixedDelay}", timeUnit = TimeUnit.MINUTES)
    public void update() {
        // 校验是否符合不符合不上报
        if (dataCenterInfoIsError(dataCenterInfo.getId()) || dataCenterInfoIsError(dataCenterInfo.getName())) {
            return;
        }
        log.info("定时上报节点信息...");
        producerMessage(getActuatorNodeStatusTopicInfo(dataCenterInfo.getId(), dataCenterInfo.getName(), serverPort, ActuatorStatusEnum.UPDATE));
    }

    /**
     * 项目关闭之后上报节点信息
     */
    @PreDestroy
    private void close() {
        // 校验是否符合不符合不上报
        if (dataCenterInfoIsError(dataCenterInfo.getId()) || dataCenterInfoIsError(dataCenterInfo.getName())) {
            return;
        }
        log.info("执行节点正在关闭，开始上报节点信息...");
        producerMessage(getActuatorNodeStatusTopicInfo(dataCenterInfo.getId(), dataCenterInfo.getName(), serverPort, ActuatorStatusEnum.CLOSE));
    }

    public static boolean dataCenterInfoIsError(String dataCenterInfo) {
        // 检查属性是否存在
        return StringUtils.isBlank(dataCenterInfo) || DEFAULT_CENTER_INFO_VALUE.equals(dataCenterInfo);
    }

    public static ActuatorNodeStatusTopicInfo getActuatorNodeStatusTopicInfo(String dataCenterId, String dataCenterName, String serverPort, ActuatorStatusEnum actuatorStatusEnum) {
        String ip = "127.0.0.1";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("获取当前设备IP失败...");
        }
        ActuatorNodeStatusTopicInfo actuatorNodeStatusTopicInfo =
                ActuatorNodeStatusTopicInfo.builder()
                        .dataCenterName(dataCenterName)
                        .actuatorIp(ip)
                        .actuatorPort(serverPort)
                        .status(actuatorStatusEnum.ordinal())
                        .build();
        actuatorNodeStatusTopicInfo.setDataCenterId(dataCenterId);
        actuatorNodeStatusTopicInfo.setDate(System.currentTimeMillis());
        return actuatorNodeStatusTopicInfo;
    }

    public static void producerMessage(ActuatorNodeStatusTopicInfo actuatorNodeStatusTopicInfo) {

        log.info("ActuatorNodeStatusTopicInfo : {}", actuatorNodeStatusTopicInfo);
        if (SpringUtils.getBean(MessageProducer.class).producerMessage(TopicBingingName.NODE_REPORT_BINGING_NAME, actuatorNodeStatusTopicInfo)) {
            log.info("上报节点信息完成...");
        } else {
            log.error("上报节点信息失败...");
        }
    }
}
