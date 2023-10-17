package com.csp.actuator.api.kms;

import com.csp.actuator.api.base.DataCenterInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2023-10-17 14:47
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SyncLMKTopicInfo extends DataCenterInfo {

    /**
     * 设备类型编码
     */
    private Integer devModelCode;

    /**
     * 需要操作的设备列表
     */
    private List<String> deviceList;

    /**
     * 创建密钥信息
     */
    private Map<String, Object> keyInfo;
}
