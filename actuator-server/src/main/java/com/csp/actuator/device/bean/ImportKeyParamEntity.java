package com.csp.actuator.device.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入接口参数实体
 *
 * @author Weijia Jiang
 * @version v1
 * @description 导入接口参数实体
 * @date Created in 2023-11-02 14:25
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportKeyParamEntity {
    Integer kekIndex;
    Integer keyIndex;
    Integer keyAlgTypeCode;
    Integer keyUsedType;
    String cipher;
    String keyCV;
    String keyLable;
    String keyId;
}
