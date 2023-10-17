package com.csp.actuator.api.kms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

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
@Accessors(chain = true)
public class SyncLMKCallbackTopicInfo {
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
