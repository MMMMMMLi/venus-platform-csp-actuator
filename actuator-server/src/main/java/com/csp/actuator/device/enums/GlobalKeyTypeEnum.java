package com.csp.actuator.device.enums;

import com.csp.actuator.device.contants.GlobalTypeCodeConstant;
import com.csp.actuator.device.contants.VendorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;


/**
 * 密钥类型枚举
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥类型枚举
 * @date Created in 2023-03-18 15:30
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum GlobalKeyTypeEnum {

    TASS_SYMMETRIC_KEY(GlobalTypeCodeConstant.SYMMETRIC_KEY, VendorConstant.TASS, 0),
    TASS_ECC_KEY(GlobalTypeCodeConstant.ECC_KEY, VendorConstant.TASS, 2),
    VENUS_SYMMETRIC(GlobalTypeCodeConstant.SYMMETRIC_KEY, VendorConstant.VENUS_SELF, 1),
    VENUS_ECC_KEY(GlobalTypeCodeConstant.ECC_KEY, VendorConstant.VENUS_SELF, 3),
    LEADSEC_SYMMETRIC(GlobalTypeCodeConstant.SYMMETRIC_KEY, VendorConstant.LEADSEC_SELF, 1),
    LEADSEC_ECC_KEY(GlobalTypeCodeConstant.ECC_KEY, VendorConstant.LEADSEC_SELF, 3),
    ;

    /**
     * 密钥算法通用代码
     */
    private Integer globalTypeCode;

    /**
     * 厂商编码
     */
    private Integer vendorCode;

    /**
     * 厂商自定义的密钥类型编码
     * 比如对于启明密码机为：1-SYMM,2-RSA,3-SM2,4-ECC,5-DSA,6-EDDSA,7-SM9M,8-SM9U
     */
    private Integer vendorTypeCode;

    public static Integer getVendorTypeCode(Integer globalTypeCode, Integer vendorCode) {
        for (GlobalKeyTypeEnum value : GlobalKeyTypeEnum.values()) {
            if (Objects.equals(value.globalTypeCode, globalTypeCode) && value.vendorCode.compareTo(vendorCode) == 0) {
                return value.vendorTypeCode;
            }
        }
        return GlobalTypeCodeConstant.ERROR_KEY;
    }
}

