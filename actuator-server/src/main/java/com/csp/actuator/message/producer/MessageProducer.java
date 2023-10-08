package com.csp.actuator.message.producer;

import com.csp.actuator.api.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 消息生产者
 *
 * @author Weijia Jiang
 * @version v1
 * @description 消息生产者
 * @date Created in 2023-10-08 12:01
 */
@Slf4j
@Component
public class MessageProducer {
    @Resource
    private StreamBridge streamBridge;

    public boolean producerMessage(String bindingName, Object message) {
        log.info("producerMessage, bindingName: {}, message: {}", bindingName, message);
        return producerMessage(bindingName, JsonUtils.writeValueAsString(message));
    }

    public boolean producerMessage(String bindingName, String message) {
        log.info("producerMessage, bindingName: {}, message: {}", bindingName, message);
        if (StringUtils.isAnyBlank(bindingName, message)) {
            log.error("bindingName or message is blank. bindingName: {}, message: {}", bindingName, message);
            return false;
        }
        streamBridge.send(bindingName, message);
        return true;
    }
}
