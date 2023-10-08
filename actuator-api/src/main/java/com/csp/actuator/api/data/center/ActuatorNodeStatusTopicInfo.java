package com.csp.actuator.api.data.center;

import com.csp.actuator.api.base.DataCenterInfo;
import lombok.*;

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
@EqualsAndHashCode(callSuper = true)
public class ActuatorNodeStatusTopicInfo extends DataCenterInfo {
    private String dataCenterName;
    private String actuatorIp;
    private String actuatorPort;
    private Integer status;
}
