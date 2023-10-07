package com.csp.actuator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class KmsActuatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(KmsActuatorApplication.class, args);
    }

}
