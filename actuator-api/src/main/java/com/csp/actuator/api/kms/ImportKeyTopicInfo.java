package com.csp.actuator.api.kms;

import com.csp.actuator.api.base.DataCenterInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 导入实体
 *
 * @author Weijia Jiang
 * @version v1
 * @description 导入实体
 * @date Created in 2023-11-02 10:27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ImportKeyTopicInfo extends DataCenterInfo {
    /**
     * 设备类型编码
     */
    private Integer devModelCode;

    /**
     * 需要操作的设备列表
     */
    private List<String> deviceList;

    /**
     * 导入密钥信息
     */
    private List<Map<String, Object>> keyInfo;
}
