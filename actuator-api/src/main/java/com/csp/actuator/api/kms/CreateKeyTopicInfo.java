package com.csp.actuator.api.kms;

import com.csp.actuator.api.base.DataCenterInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
    private String keyInfo;
}
