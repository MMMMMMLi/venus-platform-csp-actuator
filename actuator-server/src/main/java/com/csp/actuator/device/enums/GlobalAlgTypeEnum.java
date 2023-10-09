package com.csp.actuator.device.enums;

import com.csp.actuator.device.contants.VendorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static com.csp.actuator.device.contants.GlobalAlgTypeCodeConstant.*;
import static com.csp.actuator.device.contants.GlobalTypeCodeConstant.ERROR_KEY;


/**
 * 密钥算法枚举
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥算法枚举
 * @date Created in 2023-03-18 15:30
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum GlobalAlgTypeEnum {

    SYMMETRIC_KEY_ALG_AES128_TASS(SYMMETRIC_KEY_ALG_AES128, VendorConstant.TASS, 3, SYMMETRIC_KEY_ALG_AES128_NAME),
    SYMMETRIC_KEY_ALG_AES192_TASS(SYMMETRIC_KEY_ALG_AES192, VendorConstant.TASS, 4, SYMMETRIC_KEY_ALG_AES192_NAME),
    SYMMETRIC_KEY_ALG_AES256_TASS(SYMMETRIC_KEY_ALG_AES256, VendorConstant.TASS, 5, SYMMETRIC_KEY_ALG_AES256_NAME),
    SYMMETRIC_KEY_ALG_SM1_TASS(SYMMETRIC_KEY_ALG_SM1, VendorConstant.TASS, 6, SYMMETRIC_KEY_ALG_SM1_NAME),
    SYMMETRIC_KEY_ALG_SM4_TASS(SYMMETRIC_KEY_ALG_SM4, VendorConstant.TASS, 7, SYMMETRIC_KEY_ALG_SM4_NAME),
    SYMMETRIC_KEY_ALG_SSF33_TASS(SYMMETRIC_KEY_ALG_SSF33, VendorConstant.TASS, 8, SYMMETRIC_KEY_ALG_SSF33_NAME),
    SYMMETRIC_KEY_ALG_SM1_SANSEC(SYMMETRIC_KEY_ALG_SM1, VendorConstant.SANSEC, 0x00000102, SYMMETRIC_KEY_ALG_SM1_NAME),
    SYMMETRIC_KEY_ALG_SM4_SANSEC(SYMMETRIC_KEY_ALG_SM4, VendorConstant.SANSEC, 0x00000402, SYMMETRIC_KEY_ALG_SM4_NAME),
    SYMMETRIC_KEY_ALG_SM1_CBC_VENUS(SYMMETRIC_KEY_ALG_SM1, VendorConstant.VENUS_SELF, 0x00000102, SYMMETRIC_KEY_ALG_SM1_NAME),
    SYMMETRIC_KEY_ALG_SM4_CBC_VENUS(SYMMETRIC_KEY_ALG_SM4, VendorConstant.VENUS_SELF, 0x00000402, SYMMETRIC_KEY_ALG_SM4_NAME),
    SYMMETRIC_KEY_ALG_SM1_CBC_LEADSEC(SYMMETRIC_KEY_ALG_SM1, VendorConstant.LEADSEC_SELF, 0x00000102, SYMMETRIC_KEY_ALG_SM1_NAME),
    SYMMETRIC_KEY_ALG_SM4_CBC_LEADSEC(SYMMETRIC_KEY_ALG_SM4, VendorConstant.LEADSEC_SELF, 0x00000402, SYMMETRIC_KEY_ALG_SM4_NAME),
    ;

    /**
     * 密钥算法通用代码
     */
    private Integer globalTypeCode;

    /**
     * 厂商代码
     */
    private Integer vendorCode;

    /**
     * 厂商算法类型编码
     */
    private Integer vendorAlgTypeCode;

    /**
     * 厂商算法类型名称
     */
    private String vendorAlgTypeName;

    public static Integer getVendorAlgTypeCode(Integer globalTypeCode, Integer vendorCode) {
        for (GlobalAlgTypeEnum value : GlobalAlgTypeEnum.values()) {
            if (Objects.equals(value.globalTypeCode, globalTypeCode) && value.vendorCode.compareTo(vendorCode) == 0) {
                return value.vendorAlgTypeCode;
            }
        }
        return ERROR_KEY;
    }

    public static String getVendorAlgTypeName(Integer globalTypeCode, Integer vendorCode) {
        for (GlobalAlgTypeEnum value : GlobalAlgTypeEnum.values()) {
            if (Objects.equals(value.globalTypeCode, globalTypeCode) && value.vendorCode.compareTo(vendorCode) == 0) {
                return value.vendorAlgTypeName;
            }
        }
        return null;
    }
}
