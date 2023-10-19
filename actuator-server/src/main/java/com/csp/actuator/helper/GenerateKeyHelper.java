package com.csp.actuator.helper;

import com.csp.actuator.api.enums.DeviceOperationInterfaceEnum;
import com.csp.actuator.api.kms.GenerateKeyTopicInfo;
import com.csp.actuator.constants.ErrorMessage;
import com.csp.actuator.device.FactoryBuilder;
import com.csp.actuator.api.entity.GenerateKeyResult;
import com.csp.actuator.device.contants.GlobalTypeCodeConstant;
import com.csp.actuator.device.factory.HSMFactory;
import com.csp.actuator.exception.ActuatorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.csp.actuator.api.constants.KeyInfoKeyConstant.*;
import static com.csp.actuator.device.contants.GlobalExportKeyAlgTypeCodeConstant.EXPORT_KEY_ALG_TYPE_CBC;

/**
 * 创建密钥
 *
 * @author Weijia Jiang
 * @version v1
 * @description 创建密钥
 * @date Created in 2023-10-10 15:28
 */

@Slf4j
public class GenerateKeyHelper {

    /**
     * 创建Key
     *
     * @param generateKeyTopicInfo topic信息
     * @return 结果集
     */
    public static GenerateKeyResult generateKey(GenerateKeyTopicInfo generateKeyTopicInfo) {
        // 获取操作码
        DeviceOperationInterfaceEnum operationInfo = DeviceOperationInterfaceEnum.getGenerateKeyEnum(generateKeyTopicInfo.getOperation());
        if (Objects.isNull(operationInfo)) {
            throw new ActuatorException(ErrorMessage.ERROR_OPERATION_NOT_FOUND);
        }
        // 校验设备编码
        CheckHelper.checkDevModelCode(generateKeyTopicInfo.getDevModelCode());
        // 校验设备信息
        CheckHelper.checkDeviceInfo(generateKeyTopicInfo.getDeviceList());
        // 校验密钥信息
        CheckHelper.checkKeyInfo(generateKeyTopicInfo.getKeyInfo());
        // 执行
        return executeGenerateKeyInterface(generateKeyTopicInfo, operationInfo);
    }

    public static GenerateKeyResult executeGenerateKeyInterface(GenerateKeyTopicInfo generateKeyTopicInfo, DeviceOperationInterfaceEnum operationInfo) {
        // 根据操作码执行创建密钥操作
        switch (operationInfo) {
            case generateSymmetricKey:
                return generateSymmetricKey(generateKeyTopicInfo);
            case generateAndSaveSymmetricKey:
                return generateAndSaveSymmetricKey(generateKeyTopicInfo);
            case generateSymmetricKey4ProKeyIndex:
                return generateSymmetricKey4ProKeyIndex(generateKeyTopicInfo);
            case generateSymmetricKey4ProKeyInfo:
                return generateSymmetricKey4ProKeyValue(generateKeyTopicInfo);
            case generateSM2Key:
                return generateSM2Key(generateKeyTopicInfo);
            case generateSM2Key4ProKeyValue:
                return generateSM2Key4ProKeyValue(generateKeyTopicInfo);
            case generateAndSaveSM2Key:
                return generateAndSaveSM2Key(generateKeyTopicInfo);
            case generateAndSaveSymmetricKey4ProKeyIndex:
                return generateAndSaveSymmetricKey4ProKeyIndex(generateKeyTopicInfo);
            default:
                return null;
        }
    }

    /**
     * 获取密钥的IV
     *
     * @param length 长度
     */
    public static String getKeyIv(int length) {
        return StringUtils.substring(UUID.randomUUID().toString().replace("-", ""), 0, length);
    }

    /**
     * 创建对称密钥
     */
    public static GenerateKeyResult generateSymmetricKey(GenerateKeyTopicInfo generateKeyTopicInfo) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(generateKeyTopicInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        GenerateKeyResult generateKeyResult = hsmImpl.generateSymmetricKey(
                (Integer) generateKeyTopicInfo.getKeyInfo().get(KEY_ALG_TYPE),
                generateKeyTopicInfo.getDeviceList());
        return Objects.isNull(generateKeyResult) ? null : generateKeyResult.setKeyIndex(0);
    }

    /**
     * 创建并将对称密钥保存到密码机指定索引位置
     */
    public static GenerateKeyResult generateAndSaveSymmetricKey(GenerateKeyTopicInfo generateKeyTopicInfo) {
        Integer devModelCode = generateKeyTopicInfo.getDevModelCode();
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(devModelCode);
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        Map<String, Object> keyInfo = generateKeyTopicInfo.getKeyInfo();
        List<String> deviceList = generateKeyTopicInfo.getDeviceList();
        // 校验一下索引值
        Integer keyIndex = (Integer) keyInfo.get(KEY_INDEX);
        Integer maxKeyNums = (Integer) keyInfo.get(KEY_INDEX_MAX_NUMS);
        if (Objects.isNull(keyIndex) || keyIndex == 0) {
            if (Objects.isNull(maxKeyNums) || maxKeyNums == 0) {
                throw new ActuatorException(ErrorMessage.ERROR_GET_KEY_INDEX_FAILED);
            }
            keyIndex = DeviceHelper.getOneAvailableKeyIndexList(devModelCode, GlobalTypeCodeConstant.SYMMETRIC_KEY, 0, deviceList, maxKeyNums);
        }
        GenerateKeyResult generateKeyResult = hsmImpl.generateAndSaveSymmetricKey(
                (Integer) generateKeyTopicInfo.getKeyInfo().get(KEY_ALG_TYPE),
                keyIndex, deviceList);
        return Objects.isNull(generateKeyResult) ? null : generateKeyResult.setKeyIndex(keyIndex);
    }

    /**
     * 产生对称密钥,保护密钥保护输出
     */
    public static GenerateKeyResult generateSymmetricKey4ProKeyIndex(GenerateKeyTopicInfo generateKeyTopicInfo) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(generateKeyTopicInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        Map<String, Object> keyInfo = generateKeyTopicInfo.getKeyInfo();
        // 获取一个keyIv
        String destKeyIv = getKeyIv(16);
        GenerateKeyResult generateKeyResult = hsmImpl.generateSymmetricKey4ProKeyIndex(
                (Integer) keyInfo.get(KEY_ALG_TYPE),
                (Integer) keyInfo.get(KEK_INDEX),
                (Integer) keyInfo.get(KEK_KEY_ALG_TYPE),
                destKeyIv,
                EXPORT_KEY_ALG_TYPE_CBC,
                generateKeyTopicInfo.getDeviceList());
        return Objects.isNull(generateKeyResult) ? null : generateKeyResult.setIv(destKeyIv).setKeyIndex(0);
    }

    /**
     * 产生对称密钥,保护密钥保护输出
     */
    public static GenerateKeyResult generateSymmetricKey4ProKeyValue(GenerateKeyTopicInfo generateKeyTopicInfo) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(generateKeyTopicInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        Map<String, Object> keyInfo = generateKeyTopicInfo.getKeyInfo();
        // 获取一个keyIv
        String destKeyIv = getKeyIv(16);
        GenerateKeyResult generateKeyResult = hsmImpl.generateSymmetricKey4ProKeyInfo(
                (Integer) keyInfo.get(KEY_ALG_TYPE),
                (String) keyInfo.get(KEK_VALUE),
                (Integer) keyInfo.get(KEK_KEY_ALG_TYPE),
                (String) keyInfo.get(KEK_KEY_CV),
                destKeyIv, EXPORT_KEY_ALG_TYPE_CBC, generateKeyTopicInfo.getDeviceList());
        return Objects.isNull(generateKeyResult) ? null : generateKeyResult.setIv(destKeyIv).setKeyIndex(0);
    }

    /**
     * 创建SM2非对称密钥
     */
    public static GenerateKeyResult generateSM2Key(GenerateKeyTopicInfo generateKeyTopicInfo) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(generateKeyTopicInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        GenerateKeyResult generateKeyResult = hsmImpl.generateSM2Key(
                (Integer) generateKeyTopicInfo.getKeyInfo().get(KEK_INDEX),
                generateKeyTopicInfo.getDeviceList());
        return Objects.isNull(generateKeyResult) ? null : generateKeyResult.setKeyIndex(0);
    }

    /**
     * 创建SM2非对称密钥
     */
    public static GenerateKeyResult generateSM2Key4ProKeyValue(GenerateKeyTopicInfo generateKeyTopicInfo) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(generateKeyTopicInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        GenerateKeyResult generateKeyResult = hsmImpl.generateSM2Key4ProKeyValue(
                (Integer) generateKeyTopicInfo.getKeyInfo().get(KEK_KEY_ALG_TYPE),
                (String) generateKeyTopicInfo.getKeyInfo().get(KEK_VALUE),
                generateKeyTopicInfo.getDeviceList());
        return Objects.isNull(generateKeyResult) ? null : generateKeyResult.setKeyIndex(0);
    }

    /**
     * 生成并导入SM2非对称密钥到密码机
     */
    public static GenerateKeyResult generateAndSaveSM2Key(GenerateKeyTopicInfo generateKeyTopicInfo) {
        Integer devModelCode = generateKeyTopicInfo.getDevModelCode();
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(devModelCode);
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        // 获取密钥信息
        Map<String, Object> keyInfo = generateKeyTopicInfo.getKeyInfo();
        List<String> deviceList = generateKeyTopicInfo.getDeviceList();
        // 校验一下索引值
        Integer keyIndex = (Integer) keyInfo.get(KEY_INDEX);
        Integer maxKeyNums = (Integer) keyInfo.get(KEY_INDEX_MAX_NUMS);
        Integer keyUseType = (Integer) keyInfo.get(KEY_USAGE);
        if (Objects.isNull(keyIndex) || keyIndex == 0) {
            if (Objects.isNull(maxKeyNums) || maxKeyNums == 0) {
                throw new ActuatorException(ErrorMessage.ERROR_GET_KEY_INDEX_FAILED);
            }
            keyIndex = DeviceHelper.getOneAvailableKeyIndexList(devModelCode, GlobalTypeCodeConstant.ECC_KEY, keyUseType, deviceList, maxKeyNums);
        }
        // 获取一个keyLable
        String keyLable = getKeyIv(16);
        GenerateKeyResult generateKeyResult = hsmImpl.generateAndSaveSM2Key(
                (Integer) keyInfo.get(KEK_INDEX),
                (Integer) keyInfo.get(KEY_ALG_TYPE),
                keyIndex, keyUseType, keyLable, deviceList);
        return Objects.isNull(generateKeyResult) ? null : generateKeyResult.setKeyLabel(keyLable).setKeyIndex(keyIndex);
    }

    public static GenerateKeyResult generateAndSaveSymmetricKey4ProKeyIndex(GenerateKeyTopicInfo generateKeyTopicInfo) {
        Integer devModelCode = generateKeyTopicInfo.getDevModelCode();
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(generateKeyTopicInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        // 获取密钥信息
        Map<String, Object> keyInfo = generateKeyTopicInfo.getKeyInfo();
        List<String> deviceList = generateKeyTopicInfo.getDeviceList();
        // 校验一下索引值
        Integer keyIndex = (Integer) keyInfo.get(KEY_INDEX);
        Integer maxKeyNums = (Integer) keyInfo.get(KEY_INDEX_MAX_NUMS);
        if (Objects.isNull(keyIndex) || keyIndex == 0) {
            if (Objects.isNull(maxKeyNums) || maxKeyNums == 0) {
                throw new ActuatorException(ErrorMessage.ERROR_GET_KEY_INDEX_FAILED);
            }
            keyIndex = DeviceHelper.getOneAvailableKeyIndexList(devModelCode, GlobalTypeCodeConstant.ECC_KEY, 0, deviceList, maxKeyNums);
        }
        // 获取一个keyIv
        String destKeyIv = getKeyIv(16);
        String keyLable = getKeyIv(16);
        GenerateKeyResult generateKeyResult = hsmImpl.generateAndSaveSymmetricKey4ProKeyIndex(
                (Integer) keyInfo.get(KEY_ALG_TYPE),
                keyIndex,
                (Integer) keyInfo.get(KEK_KEY_ALG_TYPE),
                (Integer) keyInfo.get(KEY_INDEX),
                keyLable, destKeyIv, EXPORT_KEY_ALG_TYPE_CBC, deviceList);
        return Objects.isNull(generateKeyResult) ? null : generateKeyResult.setIv(destKeyIv).setKeyLabel(keyLable).setKeyIndex(keyIndex);
    }

}
