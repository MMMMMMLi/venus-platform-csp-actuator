package com.csp.actuator.device.enums;

import com.csp.actuator.device.contants.VendorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static com.csp.actuator.device.contants.GlobalUsedTypeCodeConstant.*;
import static com.csp.actuator.device.contants.GlobalTypeCodeConstant.ERROR_KEY;

/**
 * 密钥用途通用枚举
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥用途通用枚举
 * @date Created in 2023-03-28 11:09
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum GlobalUsedTypeCodeEnum {

    TASS_KEY_USAGE_CODE_SIGN_ENCRYPTION(KEY_USAGE_CODE_SIGN_ENCRYPTION, VendorConstant.TASS, 2),
    TASS_KEY_USAGE_CODE_ENCRYPTION(KEY_USAGE_CODE_ENCRYPTION, VendorConstant.TASS, 1),
    TASS_KEY_USAGE_CODE_SIGN(KEY_USAGE_CODE_SIGN, VendorConstant.TASS, 0),
    VENUS_KEY_USAGE_CODE_ENCRYPTION(KEY_USAGE_CODE_ENCRYPTION, VendorConstant.VENUS_SELF, 1),
    VENUS_KEY_USAGE_CODE_SIGN(KEY_USAGE_CODE_SIGN, VendorConstant.VENUS_SELF, 0),
    LEADSEC_KEY_USAGE_CODE_ENCRYPTION(KEY_USAGE_CODE_ENCRYPTION, VendorConstant.LEADSEC_SELF, 1),
    LEADSEC_KEY_USAGE_CODE_SIGN(KEY_USAGE_CODE_SIGN, VendorConstant.LEADSEC_SELF, 0),
    ;

    /**
     * 通用密钥用途代码
     */
    private Integer globalUsedTypeCode;

    /**
     * 厂商编码
     */
    private Integer vendorCode;

    /**
     * 厂商自定义的用途编码
     * 比如对于启明密码机为：0-签名，1-加密
     */
    private Integer vendorUsedTypeCode;

    public static Integer getVendorUsedTypeCode(Integer globalTypeCode, Integer vendorCode) {
        for (GlobalUsedTypeCodeEnum value : GlobalUsedTypeCodeEnum.values()) {
            if (Objects.equals(value.globalUsedTypeCode, globalTypeCode) && value.vendorCode.compareTo(vendorCode) == 0) {
                return value.vendorUsedTypeCode;
            }
        }
        return ERROR_KEY;
    }
}
