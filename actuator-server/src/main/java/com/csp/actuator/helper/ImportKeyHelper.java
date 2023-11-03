package com.csp.actuator.helper;

import cn.hutool.core.collection.CollectionUtil;
import com.csp.actuator.api.constants.KeyInfoKeyConstant;
import com.csp.actuator.api.enums.DeviceOperationInterfaceEnum;
import com.csp.actuator.api.kms.ImportKeyTopicInfo;
import com.csp.actuator.constants.ErrorMessage;
import com.csp.actuator.device.FactoryBuilder;
import com.csp.actuator.device.bean.ImportKeyParamEntity;
import com.csp.actuator.device.factory.HSMFactory;
import com.csp.actuator.entity.ImportKeyInfo;
import com.csp.actuator.exception.ActuatorException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.csp.actuator.constants.ErrorMessage.ERROR_OPERATION_NOT_FOUND;

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
                importSM2Key(importKeyInfo);
                return true;
            default:
                return false;
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

    /**
     * 导入SM2非对称密钥到密码机
     */
    public static void importSM2Key(ImportKeyInfo importKeyInfo) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(importKeyInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return;
        }
        Map<String, Object> keyInfo = importKeyInfo.getKeyInfo();
        hsmImpl.importSM2Key(
                (Integer) keyInfo.get(KeyInfoKeyConstant.KEK_INDEX),
                (Integer) keyInfo.get(KeyInfoKeyConstant.KEY_TYPE),
                (String) keyInfo.get(KeyInfoKeyConstant.KEY_VALUE),
                (Integer) keyInfo.get(KeyInfoKeyConstant.KEY_INDEX),
                (Integer) keyInfo.get(KeyInfoKeyConstant.KEY_USAGE),
                (String) keyInfo.get(KeyInfoKeyConstant.KEY_LABEL),
                importKeyInfo.getKeyId(),
                importKeyInfo.getDeviceList());
    }

    public static Boolean batchImportSM2Key(ImportKeyTopicInfo importKeyTopicInfo) {
        log.info("ImportKeyTopicInfo :{}", importKeyTopicInfo);
        // 校验
        List<Map<String, Object>> keyInfoList = importKeyTopicInfo.getKeyInfo();
        if (CollectionUtil.isEmpty(keyInfoList)) {
            throw new ActuatorException(ErrorMessage.ERROR_KEY_INFO_NOT_FOUND);
        }
        // 处理一下导入信息
        List<ImportKeyParamEntity> importKeyParamEntityList = keyInfoList.stream().map(keyInfo -> {
            ImportKeyParamEntity importKeyParamEntity = new ImportKeyParamEntity();
            importKeyParamEntity.setKekIndex((Integer) keyInfo.get(KeyInfoKeyConstant.KEK_INDEX));
            importKeyParamEntity.setKeyIndex((Integer) keyInfo.get(KeyInfoKeyConstant.KEY_INDEX));
            importKeyParamEntity.setKeyAlgTypeCode((Integer) keyInfo.get(KeyInfoKeyConstant.KEY_ALG_TYPE));
            importKeyParamEntity.setKeyUsedType((Integer) keyInfo.get(KeyInfoKeyConstant.KEY_USAGE));
            importKeyParamEntity.setCipher((String) keyInfo.get(KeyInfoKeyConstant.KEY_VALUE));
            importKeyParamEntity.setKeyCV((String) keyInfo.getOrDefault(KeyInfoKeyConstant.KEY_CV, ""));
            importKeyParamEntity.setKeyLable((String) keyInfo.getOrDefault(KeyInfoKeyConstant.KEY_LABEL, ""));
            importKeyParamEntity.setKeyId((String) keyInfo.getOrDefault(KeyInfoKeyConstant.KEY_LABEL, ""));
            return importKeyParamEntity;
        }).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(importKeyParamEntityList)) {
            throw new ActuatorException(ErrorMessage.ERROR_KEY_INFO_NOT_FOUND);
        }
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(importKeyTopicInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return false;
        }
        // 执行
        hsmImpl.batchImportSM2Key(importKeyParamEntityList, importKeyTopicInfo.getDeviceList());
        return true;
    }

    public static Boolean batchImportSymmetricDek(ImportKeyTopicInfo importKeyTopicInfo) {
        log.info("ImportKeyTopicInfo :{}", importKeyTopicInfo);
        // 校验
        List<Map<String, Object>> keyInfoList = importKeyTopicInfo.getKeyInfo();
        if (CollectionUtil.isEmpty(keyInfoList)) {
            throw new ActuatorException(ErrorMessage.ERROR_KEY_INFO_NOT_FOUND);
        }
        // 处理一下导入信息
        List<ImportKeyParamEntity> importKeyParamEntityList = keyInfoList.stream().map(keyInfo -> {
            ImportKeyParamEntity importKeyParamEntity = new ImportKeyParamEntity();
            importKeyParamEntity.setKekIndex((Integer) keyInfo.get(KeyInfoKeyConstant.KEK_INDEX));
            importKeyParamEntity.setKeyIndex((Integer) keyInfo.get(KeyInfoKeyConstant.KEY_INDEX));
            importKeyParamEntity.setKeyAlgTypeCode((Integer) keyInfo.get(KeyInfoKeyConstant.KEY_ALG_TYPE));
            importKeyParamEntity.setKeyUsedType((Integer) keyInfo.get(KeyInfoKeyConstant.KEY_USAGE));
            importKeyParamEntity.setCipher((String) keyInfo.get(KeyInfoKeyConstant.KEY_VALUE));
            importKeyParamEntity.setKeyCV((String) keyInfo.getOrDefault(KeyInfoKeyConstant.KEY_CV, ""));
            importKeyParamEntity.setKeyLable((String) keyInfo.getOrDefault(KeyInfoKeyConstant.KEY_LABEL, ""));
            importKeyParamEntity.setKeyId((String) keyInfo.getOrDefault(KeyInfoKeyConstant.KEY_LABEL, ""));
            return importKeyParamEntity;
        }).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(importKeyParamEntityList)) {
            throw new ActuatorException(ErrorMessage.ERROR_KEY_INFO_NOT_FOUND);
        }
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(importKeyTopicInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return false;
        }
        // 执行
        return hsmImpl.batchImportSymmetricKey(importKeyParamEntityList, importKeyTopicInfo.getDeviceList());
    }
}
