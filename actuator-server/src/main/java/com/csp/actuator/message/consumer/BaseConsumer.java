package com.csp.actuator.message.consumer;

import com.csp.actuator.helper.CheckHelper;
import com.csp.actuator.message.producer.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class BaseConsumer {

    private final MessageProducer messageProducer;

    public BaseConsumer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @Bean
    public Consumer<String> confirmDataCenter() {
        return msg -> {
            log.info("ConfirmDataCenter msg: {}", msg);
            // 确认一下数据中心信息
            boolean checkDataCenterId = CheckHelper.checkDataCenterId(msg);
            // 如果校验通过，则反馈回调信息
            if (checkDataCenterId) {
                // TODO: 增加回调 by. Weijia Jiang. 2023/10/8 14:28
                // messageProducer.producerMessage(TopicBingingName.CONFIRM_DATA_CENTER_CALL_BACK_BINGING_NAME, null);
            }
        };
    }
}
