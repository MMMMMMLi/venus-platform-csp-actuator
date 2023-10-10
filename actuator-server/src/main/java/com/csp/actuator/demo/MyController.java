package com.csp.actuator.demo;

import com.csp.actuator.api.kms.GenerateKeyTopicInfo;
import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.demo.producer.Test;
import com.csp.actuator.message.producer.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Weijia Jiang
 * @version v1
 * @description
 * @date Created in 2023-09-22 14:56
 */
@RestController
public class MyController {

    @Autowired
    private Test test;

    @Autowired
    private MessageProducer messageProducer;

    @GetMapping(value = "/{message}")
    public String sendMessage(@PathVariable("message") String message) {
        System.out.println("message: " + message);
        GenerateKeyTopicInfo generateKeyTopicInfo = new GenerateKeyTopicInfo(message);
        test.testProducer(JsonUtils.writeValueAsString(generateKeyTopicInfo));
        System.out.println(">>>>>>>>");
        return "ok";
    }

    @GetMapping(value = "/confirmDataCenter/{message}")
    public String test1(@PathVariable("message") String message) {
        System.out.println("message: " + message);
        GenerateKeyTopicInfo generateKeyTopicInfo = new GenerateKeyTopicInfo(message);
        generateKeyTopicInfo.setDataCenterId(message);
        generateKeyTopicInfo.setDate(System.currentTimeMillis());
        messageProducer.producerMessage("testConfirmDataCenter-out-0", generateKeyTopicInfo);
        System.out.println(">>>>>>>>");
        return "ok";
    }

}
