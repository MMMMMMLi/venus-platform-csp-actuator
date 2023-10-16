package com.csp.actuator.demo.producer;

import com.csp.actuator.api.constants.KeyInfoKeyConstant;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.csp.actuator.api.kms.GenerateKeyTopicInfo;
import com.csp.actuator.message.producer.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.UUID;

/**
 * @author Weijia Jiang
 * @version v1
 * @description
 * @date Created in 2023-09-22 14:56
 */
@RestController
public class MyController {

    @Autowired
    private MessageProducer messageProducer;

    @GetMapping(value = "/{msg}")
    public String demo(@PathVariable String msg) {
        System.out.println(msg);
        return "ok";
    }

    @GetMapping(value = "/createKey/{msg}")
    public String createKey(@PathVariable String msg) {
        String[] split = msg.split(":");
        GenerateKeyTopicInfo generateKeyTopicInfo = new GenerateKeyTopicInfo();
        generateKeyTopicInfo.setOperation(Integer.valueOf(split[1]));
        generateKeyTopicInfo.setKeyId(UUID.randomUUID().toString());
        generateKeyTopicInfo.setDevModelCode(2001);
        generateKeyTopicInfo.setDeviceList(Lists.newArrayList("172.20.88.48:8008"));
        HashMap<String, Object> objectObjectHashMap = Maps.newHashMap();
        objectObjectHashMap.put(KeyInfoKeyConstant.KEY_ALG_TYPE, 7);
        generateKeyTopicInfo.setKeyInfo(objectObjectHashMap);
        generateKeyTopicInfo.setDataCenterId(split[0]);
        generateKeyTopicInfo.setDate(System.currentTimeMillis());
        messageProducer.producerMessage("testGenerateKey-out-0", generateKeyTopicInfo);
        return "ok";
    }

}
