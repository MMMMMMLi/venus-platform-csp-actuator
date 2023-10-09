package com.csp.actuator.device.enums;

import com.csp.actuator.device.contants.VendorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static com.csp.actuator.device.contants.GlobalExportKeyAlgTypeCodeConstant.*;
import static com.csp.actuator.device.contants.GlobalTypeCodeConstant.ERROR_KEY;


/**
 * 保护密钥加密导出时的算法类型枚举
 *
 * @author Weijia Jiang
 * @version v1
 * @description 保护密钥加密导出时的算法类型枚举
 * @date Created in 2023-03-23 20:53
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum GlobalExportKeyAlgTypeEnum {

    EXPORT_KEY_ALG_TYPE_ECB_TASS(EXPORT_KEY_ALG_TYPE_ECB, VendorConstant.TASS, 0),
    EXPORT_KEY_ALG_TYPE_CBC_TASS(EXPORT_KEY_ALG_TYPE_CBC, VendorConstant.TASS, 1),
    EXPORT_KEY_ALG_TYPE_GCM_TASS(EXPORT_KEY_ALG_TYPE_GCM, VendorConstant.TASS, 6);


    private Integer globalTypeCode;

    private Integer vendorCode;

    private Integer exportKeyAlgTypeCode;

    public static Integer getExportKeyAlgTypeCodee(Integer globalTypeCode, Integer vendorCode) {
        for (GlobalExportKeyAlgTypeEnum value : GlobalExportKeyAlgTypeEnum.values()) {
            if (Objects.equals(value.globalTypeCode, globalTypeCode) && value.vendorCode.compareTo(vendorCode) == 0) {
                return value.exportKeyAlgTypeCode;
            }
        }
        return ERROR_KEY;
    }
}
