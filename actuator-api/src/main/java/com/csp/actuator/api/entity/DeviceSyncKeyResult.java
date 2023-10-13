package com.csp.actuator.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 设备密钥通过结果集
 *
 * @author Weijia Jiang
 * @version v1
 * @description 设备密钥通过结果集
 * @date Created in 2023-03-21 9:56
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSyncKeyResult {

    /**
     * 释放成功的
     */
    private List<Long> succFreedDeviceIdList;

    /**
     * 释放失败的
     */
    private List<Long> failFreedDeviceIdList;

    /**
     * 同步成功的
     */
    private List<Long> succSyncDeviceIdList;

    /**
     * 同步失败的
     */
    private List<Long> failSyncDeviceIdList;
}
