package com.csp.actuator.device.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Weijia Jiang
 * @version v1
 * @description
 * @date Created in 2023-10-09 11:18
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "hsm.config")
public class HsmConfigProperties {

    /**
     * 密码
     */
    private String password;
    /**
     * 设备动态库名
     */
    private String dllName;
    /**
     * 设备配置文件
     */
    private String configFile;

    /**
     * kek索引位
     */
    private int kekIndex = 1;

}
