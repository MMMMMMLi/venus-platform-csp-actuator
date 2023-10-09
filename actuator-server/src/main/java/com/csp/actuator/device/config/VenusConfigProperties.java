package com.csp.actuator.device.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @Description 启明星辰自研平台密码机配置
 * @Author panlin.li
 * @Date 2023/8/11 11:16
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "venus.config")
public class VenusConfigProperties {
    /**
     * 密码
     */
    private String password;
    /**
     * 设备动态库名
     */
    private List<String> libNameList;
    /**
     * 设备配置文件
     */
    private String configFile;

    /**
     * kek索引位
     */
    private int kekIndex = 1;
}
