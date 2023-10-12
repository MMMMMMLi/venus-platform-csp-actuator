package com.csp.actuator.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 生成密钥返回结果集
 *
 * @author Weijia Jiang
 * @version v1
 * @description 生成密钥返回结果集
 * @date Created in 2023-03-23 20:19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class GenerateKeyResult {

    /**
     * 密钥密文
     */
    private String keyValue;

    /**
     * 密钥校验值
     */
    private String keyCV;

    /**
     * 密钥MAC值
     */
    private String keyMac;

    /**
     * 密钥IV
     */
    private String iv;

    /**
     * 密钥标签
     */
    private String keyLabel;

    /**
     * 密钥索引
     */
    private Integer keyIndex;
}
