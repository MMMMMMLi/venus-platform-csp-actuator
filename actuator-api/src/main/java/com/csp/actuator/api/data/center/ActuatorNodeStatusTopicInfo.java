package com.csp.actuator.api.data.center;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Weijia Jiang
 * @version v1
 * @description 执行节点上报Topic信息
 * @date Created in 2023-10-08 10:53
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActuatorNodeStatusTopicInfo {
    private String dataCenterId;
    private String dataCenterName;
    private String actuatorIp;
    private String actuatorPort;
    private Long date;
    private Integer status;
}
