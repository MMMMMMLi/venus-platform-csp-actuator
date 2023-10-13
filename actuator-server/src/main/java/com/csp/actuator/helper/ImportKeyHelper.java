package com.csp.actuator.helper;

import com.csp.actuator.api.constants.KeyInfoKeyConstant;
import com.csp.actuator.api.entity.GenerateKeyResult;
import com.csp.actuator.api.enums.DeviceOperationInterfaceEnum;
import com.csp.actuator.api.kms.GenerateKeyTopicInfo;
import com.csp.actuator.device.FactoryBuilder;
import com.csp.actuator.device.factory.HSMFactory;
import com.csp.actuator.entity.ImportKeyInfo;
import com.csp.actuator.exception.ActuatorException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.csp.actuator.constants.BaseConstant.ERROR_OPERATION_NOT_FOUND;

/**
 * 导入密钥Helper
 *
 * @author Weijia Jiang
 * @version v1
 * @description 导入密钥Helper
 * @date Created in 2023-10-13 13:09
 */

@Slf4j
public class ImportKeyHelper {

    /**
     * 导入密钥
     */
    public static boolean importKey(ImportKeyInfo importKeyInfo) {
        log.info("ImportKeyInfo :{}", importKeyInfo);
        // 获取操作码
        DeviceOperationInterfaceEnum operationInfo = DeviceOperationInterfaceEnum.getImportKeyEnum(importKeyInfo.getOperation());
        if (Objects.isNull(operationInfo)) {
            throw new ActuatorException(ERROR_OPERATION_NOT_FOUND);
        }
        // 校验密钥信息
        CheckHelper.checkKeyInfo(importKeyInfo.getKeyInfo());
        // 执行
        return executeGenerateKeyInterface(importKeyInfo, operationInfo);
    }

    public static boolean executeGenerateKeyInterface(ImportKeyInfo importKeyInfo, DeviceOperationInterfaceEnum operationInfo) {
        // 根据操作码执行创建密钥操作
        switch (operationInfo) {
            case importSymmetricKey:
                return importSymmetricKey(importKeyInfo);
            case importSymmetricKey4KekIndex:
                return importSymmetricKey4KekIndex(importKeyInfo);
            case importSM2Key:
                return importSM2Key(importKeyInfo);
            default:
                return null;
        }
    }

    /**
     * 导入对称密钥
     */
    public static boolean importSymmetricKey(ImportKeyInfo importKeyInfo) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(importKeyInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return Boolean.FALSE;
        }
        Map<String, Object> keyInfo = importKeyInfo.getKeyInfo();
        return hsmImpl.importSymmetricKey(
                (Integer) keyInfo.get(KeyInfoKeyConstant.KEY_INDEX),
                (Integer) keyInfo.get(KeyInfoKeyConstant.KEY_ALG_TYPE),
                (String) keyInfo.get(KeyInfoKeyConstant.KEY_VALUE),
                (String) keyInfo.get(KeyInfoKeyConstant.KEY_CV),
                importKeyInfo.getDeviceList());
    }


    /**
     * 导入对称密钥
     */
    public static boolean importSymmetricKey4KekIndex(ImportKeyInfo importKeyInfo) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(importKeyInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return Boolean.FALSE;
        }
        Map<String, Object> keyInfo = importKeyInfo.getKeyInfo();
        return hsmImpl.importSymmetricKey(
                (Integer) keyInfo.get(KeyInfoKeyConstant.KEK_INDEX),
                (Integer) keyInfo.get(KeyInfoKeyConstant.KEY_INDEX),
                (Integer) keyInfo.get(KeyInfoKeyConstant.KEY_ALG_TYPE),
                (String) keyInfo.get(KeyInfoKeyConstant.KEY_VALUE),
                (String) keyInfo.get(KeyInfoKeyConstant.KEY_CV),
                importKeyInfo.getDeviceList());
    }

}
