package com.csp.actuator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 数据中心信息
 *
 * @author Weijia Jiang
 * @version v1
 * @description 数据中心信息
 * @date Created in 2023-10-08 10:17
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "data.center")
public class DataCenterInfo {
    private String id;
    private String name;
}
