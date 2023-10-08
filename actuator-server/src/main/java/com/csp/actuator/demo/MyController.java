package com.csp.actuator.demo;

import com.csp.actuator.api.kms.CreateKeyTopicInfo;
import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.config.DataCenterInfo;
import com.csp.actuator.demo.producer.Test;
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
    private DataCenterInfo dataCenterInfo;

    @GetMapping(value = "/{message}")
    public String sendMessage(@PathVariable("message") String message) {
        System.out.println(dataCenterInfo);
        System.out.println("message: " + message);
        CreateKeyTopicInfo createKeyTopicInfo = new CreateKeyTopicInfo(message);
        test.testProducer(JsonUtils.writeValueAsString(createKeyTopicInfo));
        System.out.println(">>>>>>>>");
        return "ok";
    }

}
