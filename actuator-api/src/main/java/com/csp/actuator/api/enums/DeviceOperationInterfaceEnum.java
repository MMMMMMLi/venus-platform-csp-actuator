package com.csp.actuator.api.enums;

import java.util.ArrayList;
import java.util.List;

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

    public static DeviceOperationInterfaceEnum getGenerateKeyEnum(Integer operationIndex) {
        List<Integer> generateKeyEnumIndexList = new ArrayList<Integer>() {{
            add(generateSymmetricKey.ordinal());
            add(generateAndSaveSymmetricKey.ordinal());
            add(generateSymmetricKey4ProKeyIndex.ordinal());
            add(generateSymmetricKey4ProKeyInfo.ordinal());
            add(generateSM2Key.ordinal());
            add(generateSM2Key4ProKeyValue.ordinal());
            add(generateAndSaveSM2Key.ordinal());
            add(generateAndSaveSymmetricKey4ProKeyIndex.ordinal());

        }};
        if (generateKeyEnumIndexList.contains(operationIndex)) {
            return DeviceOperationInterfaceEnum.values()[operationIndex];
        }
        return null;
    }
}
