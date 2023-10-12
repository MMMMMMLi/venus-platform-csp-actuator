package com.csp.actuator.helper;

import com.csp.actuator.api.enums.DeviceOperationInterfaceEnum;
import com.csp.actuator.api.kms.GenerateKeyTopicInfo;
import com.csp.actuator.device.FactoryBuilder;
import com.csp.actuator.api.entity.GenerateKeyResult;
import com.csp.actuator.device.factory.HSMFactory;
import com.csp.actuator.exception.ActuatorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.csp.actuator.api.constants.KeyInfoKeyConstant.*;
import static com.csp.actuator.constants.BaseConstant.*;
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
            throw new ActuatorException(ERROR_OPERATION_NOT_FOUND);
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
        return hsmImpl.generateSymmetricKey(
                (Integer) generateKeyTopicInfo.getKeyInfo().get(KEY_ALG_TYPE),
                generateKeyTopicInfo.getDeviceList());
    }

    /**
     * 创建并将对称密钥保存到密码机指定索引位置
     */
    public static GenerateKeyResult generateAndSaveSymmetricKey(GenerateKeyTopicInfo generateKeyTopicInfo) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(generateKeyTopicInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        Map<String, Object> keyInfo = generateKeyTopicInfo.getKeyInfo();
        // 校验一下索引值
        Integer keyIndex = (Integer) keyInfo.get(KEY_INDEX);
        return hsmImpl.generateAndSaveSymmetricKey(
                (Integer) generateKeyTopicInfo.getKeyInfo().get(KEY_ALG_TYPE),
                (Integer) keyInfo.get(KEY_INDEX),
                generateKeyTopicInfo.getDeviceList());
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
        return Objects.isNull(generateKeyResult) ? null : generateKeyResult.setIv(destKeyIv);
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
        return Objects.isNull(generateKeyResult) ? null : generateKeyResult.setIv(destKeyIv);
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
        return hsmImpl.generateSM2Key(
                (Integer) generateKeyTopicInfo.getKeyInfo().get(KEK_INDEX),
                generateKeyTopicInfo.getDeviceList());
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
        return hsmImpl.generateSM2Key4ProKeyValue(
                (Integer) generateKeyTopicInfo.getKeyInfo().get(KEY_ALG_TYPE),
                (String) generateKeyTopicInfo.getKeyInfo().get(KEK_VALUE),
                generateKeyTopicInfo.getDeviceList());
    }

    /**
     * 生成并导入SM2非对称密钥到密码机
     */
    public static GenerateKeyResult generateAndSaveSM2Key(GenerateKeyTopicInfo generateKeyTopicInfo) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(generateKeyTopicInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        Map<String, Object> keyInfo = generateKeyTopicInfo.getKeyInfo();
        // 获取一个keyLable
        String keyLable = getKeyIv(16);
        GenerateKeyResult generateKeyResult = hsmImpl.generateAndSaveSM2Key(
                (Integer) keyInfo.get(KEK_INDEX),
                (Integer) keyInfo.get(KEY_ALG_TYPE),
                (Integer) keyInfo.get(KEY_INDEX),
                (Integer) keyInfo.get(KEY_USAGE),
                keyLable,
                generateKeyTopicInfo.getDeviceList());
        return Objects.isNull(generateKeyResult) ? null : generateKeyResult.setKeyLabel(keyLable);
    }

    public static GenerateKeyResult generateAndSaveSymmetricKey4ProKeyIndex(GenerateKeyTopicInfo generateKeyTopicInfo) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(generateKeyTopicInfo.getDevModelCode());
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        Map<String, Object> keyInfo = generateKeyTopicInfo.getKeyInfo();
        // 获取一个keyIv
        String destKeyIv = getKeyIv(16);
        String keyLable = getKeyIv(16);
        GenerateKeyResult generateKeyResult = hsmImpl.generateAndSaveSymmetricKey4ProKeyIndex(
                (Integer) keyInfo.get(KEY_ALG_TYPE),
                (Integer) keyInfo.get(KEK_INDEX),
                (Integer) keyInfo.get(KEK_KEY_ALG_TYPE),
                (Integer) keyInfo.get(KEY_INDEX),
                keyLable, destKeyIv, EXPORT_KEY_ALG_TYPE_CBC, generateKeyTopicInfo.getDeviceList());
        return Objects.isNull(generateKeyResult) ? null : generateKeyResult.setIv(destKeyIv).setKeyLabel(keyLable);
    }
}
