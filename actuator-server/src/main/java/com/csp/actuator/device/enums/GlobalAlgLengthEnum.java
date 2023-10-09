package com.csp.actuator.device.enums;

import java.util.Objects;

import static com.csp.actuator.device.contants.GlobalAlgTypeCodeConstant.*;
import static com.csp.actuator.device.contants.GlobalTypeCodeConstant.ERROR_KEY;


/**
 * 算法长度
 *
 * @author Weijia Jiang
 * @version v1
 * @description 算法长度
 * @date Created in 2023-04-26 15:26
 */
public enum GlobalAlgLengthEnum {
    SM1(SYMMETRIC_KEY_ALG_SM1, 128),
    SM4(SYMMETRIC_KEY_ALG_SM4, 128),

    SM2(ASYMMETRIC_KEY_SM2, 256);

    /**
     * 密钥算法通用代码
     */
    private Integer globalTypeCode;

    /**
     * 算法长度
     */
    private Integer algLength;

    GlobalAlgLengthEnum(Integer globalTypeCode, Integer algLength) {
        this.globalTypeCode = globalTypeCode;
        this.algLength = algLength;
    }

    public Integer getGlobalTypeCode() {
        return globalTypeCode;
    }

    public Integer getAlgLength() {
        return algLength;
    }

    public static Integer getAlgLength(Integer globalTypeCode) {
        for (GlobalAlgLengthEnum value : GlobalAlgLengthEnum.values()) {
            if (Objects.equals(value.globalTypeCode, globalTypeCode)) {
                return value.algLength;
            }
        }
        return ERROR_KEY;
    }
}
