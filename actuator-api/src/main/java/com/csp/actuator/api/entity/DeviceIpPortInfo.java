package com.csp.actuator.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备端口信息
 *
 * @author Weijia Jiang
 * @version v1
 * @description 设备端口信息
 * @date Created in 2023-03-21 10:40
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceIpPortInfo {

    private String ipPort;

    private Long id;
}
