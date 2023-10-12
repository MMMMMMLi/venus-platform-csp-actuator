package com.csp.actuator.api.kms;

import com.csp.actuator.api.entity.GenerateKeyResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 销毁密钥回调Topic实体信息
 *
 * @author Weijia Jiang
 * @version v1
 * @description 销毁密钥回调Topic实体信息
 * @date Created in 2023-10-07 16:33
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DestroyKeyCallBackTopicInfo {

    /**
     * 密钥ID
     */
    private String KeyId;

    /**
     * 操作状态
     */
    private Integer status;

    /**
     * 操作结果信息
     */
    private String message;

    /**
     * 操作时间
     */
    private Long date;
}
