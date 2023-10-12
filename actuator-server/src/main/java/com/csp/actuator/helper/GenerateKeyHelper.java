package com.csp.actuator.helper;

import cn.hutool.core.collection.CollectionUtil;
import com.csp.actuator.api.enums.DeviceOperationInterfaceEnum;
import com.csp.actuator.api.kms.GenerateKeyTopicInfo;
import com.csp.actuator.device.FactoryBuilder;
import com.csp.actuator.api.entity.GenerateKeyResult;
import com.csp.actuator.device.contants.GlobalTypeCodeConstant;
import com.csp.actuator.device.contants.VendorConstant;
import com.csp.actuator.device.factory.HSMFactory;
import com.csp.actuator.exception.ActuatorException;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

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
                throw new ActuatorException(ERROR_GET_KEY_INDEX_FAILED);
            }
            keyIndex = getOneAvailableKeyIndexList(devModelCode, GlobalTypeCodeConstant.SYMMETRIC_KEY, 0, deviceList, maxKeyNums);
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
                throw new ActuatorException(ERROR_GET_KEY_INDEX_FAILED);
            }
            keyIndex = getOneAvailableKeyIndexList(devModelCode, GlobalTypeCodeConstant.ECC_KEY, keyUseType, deviceList, maxKeyNums);
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
                throw new ActuatorException(ERROR_GET_KEY_INDEX_FAILED);
            }
            keyIndex = getOneAvailableKeyIndexList(devModelCode, GlobalTypeCodeConstant.ECC_KEY, 0, deviceList, maxKeyNums);
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

    /**
     * 获取可用一个密钥值
     */
    public static Integer getOneAvailableKeyIndexList(Integer devModelCode, int globalKeyType, int globalUseType, List<String> devicePostList, int maxIndex) {
        List<Integer> availableKeyIndexList = getAvailableKeyIndexList(devModelCode, globalKeyType, globalUseType, devicePostList, maxIndex, 1);
        if (CollectionUtil.isEmpty(availableKeyIndexList) || CollectionUtil.isEmpty(availableKeyIndexList.stream().filter(Objects::nonNull).collect(Collectors.toList()))) {
            throw new ActuatorException(ERROR_KEY_NO_KEY_INDEX);
        }
        Integer integer = availableKeyIndexList.get(0);
        log.info("Available KeyIndex is: {}", integer);
        return integer;
    }

    /**
     * 获取指定数量的可用的密钥索引
     */
    public static List<Integer> getAvailableKeyIndexList(Integer devModelCode, int globalKeyType, int globalUseType, List<String> devicePostList, int maxIndex, int size) {
        // 获取密码机已经用的密钥索引
        List<Integer> indexList = Lists.newArrayList();
        if (devModelCode.equals(VendorConstant.TASS)) { // 江南天安密码机，一次返回1500个，所以最多两次就结束了
            // 第一次，从1号索引位往后取
            List<Integer> singleList = Optional.ofNullable(listKeysByDeviceGlobalKeyType(devModelCode, globalKeyType, globalUseType, 1, devicePostList)).orElse(Lists.newArrayList());
            if (CollectionUtil.isNotEmpty(singleList)) {
                indexList.addAll(singleList);
                // 取满了1500条，那么说明后面还有，还得继续取
                int nowStartIndex = indexList.get(indexList.size() - 1);
                while (singleList.size() == 1500 && indexList.size() < maxIndex && nowStartIndex < maxIndex) {
                    // 获取当前的indexList里的最大索引位置
                    nowStartIndex = indexList.remove(indexList.size() - 1);

                    singleList = Optional.ofNullable(listKeysByDeviceGlobalKeyType(devModelCode, globalKeyType, globalUseType,
                            nowStartIndex, devicePostList)).orElse(Lists.newArrayList());

                    indexList.addAll(singleList);
                }
            }
        }
        if (devModelCode.equals(VendorConstant.VENUS_SELF)) { // 启明自研密码机，对称密钥最多3000个，非对称密钥最多6000个，一次取1000个，
            // 第一次，从1号索引位往后取
            List<Integer> singleList = Optional.ofNullable(listKeysByDeviceGlobalKeyType(devModelCode, globalKeyType, globalUseType, 1, devicePostList)).orElse(Lists.newArrayList());
            if (CollectionUtil.isNotEmpty(singleList)) {
                indexList.addAll(singleList);
                // 取满了1000条，那么说明后面还有，还得继续取
                int nowStartIndex = indexList.get(indexList.size() - 1);
                while (singleList.size() == 1000 && indexList.size() < maxIndex && nowStartIndex < maxIndex) {
                    // 获取当前的indexList里的最大索引位置
                    nowStartIndex = indexList.remove(indexList.size() - 1);

                    singleList = Optional.ofNullable(listKeysByDeviceGlobalKeyType(devModelCode, globalKeyType, globalUseType,
                            nowStartIndex, devicePostList)).orElse(Lists.newArrayList());

                    indexList.addAll(singleList);
                }
            }
        }
        // 前10个和lmk的索引位置空出来，不允许使用----因为预留给平台密码机了
        // 这里有可能会浪费一个索引位置，因为有些密码机不需要LMK，等后面再说吧，浪费就浪费了
        for (int i = 0; i < GLOBAL_LMK_KEYINDEX; i++) {
            indexList.add(i + 1);
        }
        if (maxIndex == indexList.size()) {
            throw new ActuatorException(ERROR_KEY_NO_KEY_INDEX);
        }
        // 返回的密钥索引，通过随机生成，然后判断是否为密码机里已被占用的，从而找到仍未被使用的密钥索引
        List<Integer> result = Lists.newArrayList();
        Random rand = new Random();
        for (int i = 0; i < size; i++) {
            int num = rand.nextInt(maxIndex) + 1;
            while (CollectionUtil.contains(indexList, num)) {
                num = rand.nextInt(maxIndex) + 1;
            }
            result.add(num);
            indexList.add(num);
        }
        return result;
    }

    /**
     * 获取设备内的所有的密钥索引
     *
     * @param devModelCode   设备类型编码
     * @param devicePostList 设备信息
     * @return 结果集
     */
    public static List<Integer> listKeysByDeviceGlobalKeyType(Integer devModelCode, int globalKeyType, int globalKeyUsage, int startKey, List<String> devicePostList) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(devModelCode);
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        return hsmImpl.listKeys(globalKeyType, globalKeyUsage, startKey, devicePostList);
    }
}
