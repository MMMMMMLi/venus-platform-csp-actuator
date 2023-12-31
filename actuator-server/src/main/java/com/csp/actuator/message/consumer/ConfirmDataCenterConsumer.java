package com.csp.actuator.message.consumer;

import com.csp.actuator.api.data.center.ConfirmDataCenterCallBackTopicInfo;
import com.csp.actuator.api.enums.CallBackStatusEnum;
import com.csp.actuator.config.DataCenterConfig;
import com.csp.actuator.constants.TopicBingingName;
import com.csp.actuator.helper.CheckHelper;
import com.csp.actuator.message.producer.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 基础信息的Consumer
 *
 * @author Weijia Jiang
 * @version v1
 * @description 基础信息的Consumer
 * @date Created in 2023-10-08 14:20
 */
@Slf4j
@Component
public class ConfirmDataCenterConsumer {

    @Value("${msg.effective.time}")
    private Long MSG_EFFECTIVE_TIME;

    private final MessageProducer messageProducer;

    private final DataCenterConfig dataCenterConfig;

    public ConfirmDataCenterConsumer(MessageProducer messageProducer, DataCenterConfig dataCenterConfig) {
        this.messageProducer = messageProducer;
        this.dataCenterConfig = dataCenterConfig;
    }

    @Bean
    public Consumer<String> confirmDataCenter() {
        return msg -> {
            if (StringUtils.isBlank(msg)) {
                return;
            }
            // 处理一下转义字符
            msg = StringUtils.replace(msg, "\\", "");
            if (msg.indexOf("\"") == 0) {
                msg = StringUtils.removeStart(msg, "\"");
            }
            if (msg.lastIndexOf("\"") == msg.length() - 1) {
                msg = StringUtils.removeEnd(msg, "\"");
            }
            log.info("ConfirmDataCenter msg: {}", msg);
            // 确认一下数据中心信息
            boolean checkDataCenterId = CheckHelper.checkDataCenterIdAndMsgTime(msg, MSG_EFFECTIVE_TIME);
            // 如果校验通过，则反馈回调信息
            if (checkDataCenterId) {
                ConfirmDataCenterCallBackTopicInfo callBackTopicInfo = new ConfirmDataCenterCallBackTopicInfo();
                callBackTopicInfo.setDataCenterId(dataCenterConfig.getId());
                callBackTopicInfo.setDate(System.currentTimeMillis());
                callBackTopicInfo.setStatus(CallBackStatusEnum.SUCCESS.ordinal());
                log.info("ConfirmDataCenterCallBackTopicInfo : {}", callBackTopicInfo);
                if (messageProducer.producerMessage(TopicBingingName.CONFIRM_DATA_CENTER_CALL_BACK_BINGING_NAME, callBackTopicInfo)) {
                    log.info("ConfirmDataCenter CallBack Success...");
                } else {
                    log.error("ConfirmDataCenter CallBack Failed...");
                }
            }
        };
    }
}
