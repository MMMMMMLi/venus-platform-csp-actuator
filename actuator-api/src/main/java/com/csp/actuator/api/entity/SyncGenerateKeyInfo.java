package com.csp.actuator.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 密钥同步-创建密钥信息
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥同步-创建密钥信息
 * @date Created in 2023-10-13 11:39
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncGenerateKeyInfo {

    /**
     * 执行的接口
     */
    private Integer operation;

    /**
     * 对应的KeyId
     */
    private String keyId;

    /**
     * 创建密钥信息
     */
    private Map<String, Object> keyInfo;
}
