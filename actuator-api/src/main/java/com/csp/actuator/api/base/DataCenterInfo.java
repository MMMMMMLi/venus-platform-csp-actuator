package com.csp.actuator.api.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据中心实体类
 *
 * @author Weijia Jiang
 * @version v1
 * @description 数据中心ID实体类
 * @date Created in 2023-10-08 14:18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataCenterInfo {
    private String dataCenterId;

    private Long date;
}
