package com.csp.actuator.api.enums;

/**
 * 操作设备接口方法枚举
 *
 * @author Weijia Jiang
 * @version v1
 * @description 操作设备接口方法枚举
 * @date Created in 2023-10-09 16:16
 */
public enum DeviceOperationInterfaceEnum {
    removeKey,
    listKeys,
    generateSymmetricKey,
    generateAndSaveSymmetricKey,
    generateSymmetricKey4ProKeyIndex,
    generateSymmetricKey4ProKeyInfo,
    generateRandom,
    generateSM2Key,
    generateSM2Key4ProKeyValue,
    importSymmetricKey,
    importSymmetricKey4KekIndex,
    importSM2Key,
    generateAndSaveSM2Key,
    generateAndSaveSymmetricKey4ProKeyIndex,
    ;
}
