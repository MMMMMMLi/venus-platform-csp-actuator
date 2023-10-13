package com.csp.actuator.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 导入密钥信息
 *
 * @author Weijia Jiang
 * @version v1
 * @description 导入密钥信息
 * @date Created in 2023-10-13 13:13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportKeyInfo {

    /**
     * 执行的接口
     */
    private Integer operation;

    /**
     * 对应的KeyId
     */
    private String keyId;

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
