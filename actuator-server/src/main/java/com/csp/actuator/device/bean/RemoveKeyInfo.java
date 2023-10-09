package com.csp.actuator.device.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 密钥删除实体
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥删除实体
 * @date Created in 2023-03-22 17:20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveKeyInfo {

    /**
     * 密钥类型
     */
    private Integer globalKeyType;

    /**
     * 密钥索引
     */
    private Integer keyIndex;

    /**
     * 密钥用途
     */
    private Integer globalKeyUsage;
}
