package com.csp.actuator.message.producer;

import com.csp.actuator.api.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
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

    public void producerMessage(String bindingName, Object message) {
        producerMessage(bindingName, JsonUtils.writeValueAsString(message));
    }

    public void producerMessage(String bindingName, String message) {
        streamBridge.send(bindingName, message);
    }
}
