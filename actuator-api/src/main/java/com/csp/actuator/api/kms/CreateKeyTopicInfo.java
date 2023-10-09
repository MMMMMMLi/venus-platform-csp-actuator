package com.csp.actuator.api.kms;

import com.csp.actuator.api.base.DataCenterInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 创建密钥Topic实体信息
 *
 * @author Weijia Jiang
 * @version v1
 * @description 创建密钥Topic实体信息
 * @date Created in 2023-10-07 16:31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateKeyTopicInfo extends DataCenterInfo {

    /**
     * 执行的接口
     */
    private String operation;

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
