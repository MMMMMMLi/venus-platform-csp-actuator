package com.csp.kms.actuator.demo;

import com.csp.kms.actuator.demo.producer.Test;
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

    @GetMapping(value = "/{message}")
    public String sendMessage(@PathVariable("message") String message) {
        test.testProducer(message);
        return "ok";
    }

}
