package com.csp.actuator.api.kms;

import com.csp.actuator.api.entity.DeviceSyncKeyResult;
import com.csp.actuator.api.entity.GenerateKeyResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 密钥同步回调Topic实体
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥同步回调Topic实体
 * @date Created in 2023-10-13 10:34
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SyncKeyCallBackTopicInfo {

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

    /**
     * 同步信息
     */
    private DeviceSyncKeyResult deviceSyncKeyResult;
}
