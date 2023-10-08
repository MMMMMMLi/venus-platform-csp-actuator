package com.csp.actuator.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 回调状态
 *
 * @author Weijia Jiang
 * @version v1
 * @description 回调状态
 * @date Created in 2023-10-08 16:07
 */
@Getter
@AllArgsConstructor
public enum CallBackStatusEnum {
    SUCCESS,
    FAILED,
    LOSS,
    ;
}
