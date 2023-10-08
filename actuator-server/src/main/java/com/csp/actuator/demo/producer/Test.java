package com.csp.actuator.demo.producer;

import com.csp.actuator.api.kms.CreateKeyTopicInfo;
import com.csp.actuator.api.utils.JsonUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.function.Consumer;

/**
 * @author Weijia Jiang
 * @version v1
 * @description
 * @date Created in 2023-09-22 14:49
 */
@Component
public class Test {
    @Resource
    private StreamBridge streamBridge;

    public void testProducer(String message) {
        streamBridge.send("testProducer-out-0", message);
    }

    @Bean
    public Consumer<String> testConsumer() {
        return msg -> {
            System.out.println("testConsumer:" + msg);
            // System.out.println("info:" + JsonUtils.convertValue(msg, CreateKeyTopicInfo.class));
        };
    }
}
