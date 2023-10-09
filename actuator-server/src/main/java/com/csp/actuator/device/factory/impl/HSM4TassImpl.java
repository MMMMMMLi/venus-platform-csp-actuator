package com.csp.actuator.device.factory.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.tass.hsm.GHSMAPI;

import com.csp.actuator.device.DeviceInstanceHelper;
import com.csp.actuator.device.bean.GenerateKeyResult;
import com.csp.actuator.device.bean.RemoveKeyInfo;
import com.csp.actuator.device.contants.GlobalTypeCodeConstant;
import com.csp.actuator.device.contants.VendorConstant;
import com.csp.actuator.device.enums.GlobalAlgTypeEnum;
import com.csp.actuator.device.enums.GlobalKeyTypeEnum;
import com.csp.actuator.device.enums.GlobalUsedTypeCodeEnum;
import com.csp.actuator.device.factory.HSMFactory;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static com.csp.actuator.device.contants.GlobalTypeCodeConstant.ERROR_KEY;


/**
 * 江南天安HSM实现类
 *
 * @author Weijia Jiang
 * @version v1
 * @description 江南天安HSM实现类
 * @date Created in 2023-03-17 14:11
 */
@Slf4j
public class HSM4TassImpl implements HSMFactory {

    private static final byte[] NULL_BYTE = new byte[0];

    @Override
    public Boolean removeKey(List<String> deviceInfoPort, List<RemoveKeyInfo> keyIndexList) {
        log.info("HSM4TassImpl removeKey keyIndexList:{}", keyIndexList);
        if (CollectionUtil.isEmpty(deviceInfoPort) || CollectionUtil.isEmpty(keyIndexList)) {
            return Boolean.FALSE;
        }
        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(deviceInfoPort);
        if (Objects.isNull(tassHSMInstance)) {
            return Boolean.FALSE;
        }
        // 循环删除索引
        boolean deleteFlag = Boolean.TRUE;
        for (RemoveKeyInfo removeKeyInfo : keyIndexList) {
            try {
                Integer vendorTypeCode = GlobalKeyTypeEnum.getVendorTypeCode(removeKeyInfo.getGlobalKeyType(), VendorConstant.TASS);
                if (ERROR_KEY.equals(vendorTypeCode)) {
                    continue;
                }
                if (!tassHSMInstance.deleteKey(vendorTypeCode, removeKeyInfo.getKeyIndex())) {
                    deleteFlag = Boolean.FALSE;
                }
            } catch (Exception e) {
                deleteFlag = Boolean.FALSE;
                log.error(e.getMessage());
            }
        }
        return deleteFlag;
    }

    @Override
    public List<Integer> listKeys(int globalKeyType, int globalKeyUsage, int startKeyIndex, List<String> devicePostList) {
        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(devicePostList);
        if (Objects.isNull(tassHSMInstance)) {
            return null;
        }
        try {
            // 获取一下对应的密钥类型
            Integer vendorKeyTypeCode = GlobalKeyTypeEnum.getVendorTypeCode(globalKeyType, VendorConstant.TASS);
            // 获取所有密钥列表
            return tassHSMInstance.listKeys(vendorKeyTypeCode, startKeyIndex);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public GenerateKeyResult generateSymmetricKey(int keyAlgType, List<String> devicePostList) {
        log.info("HSM4TassImpl generateSymmetricKey keyAlgType:{}", keyAlgType);
        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(devicePostList);
        if (Objects.isNull(tassHSMInstance)) {
            return null;
        }
        try {
            // 获取一下对应的密钥算法类型
            Integer vendorAlgTypeCode = GlobalAlgTypeEnum.getVendorAlgTypeCode(keyAlgType, VendorConstant.TASS);
            // 生成密钥。
            // 0 索引 - 对称密钥密文;1 索引 – 对称密钥校验值
            List<byte[]> result = tassHSMInstance.genSymmKey(vendorAlgTypeCode);
            if (CollectionUtil.isEmpty(result) || result.size() != 2) {
                return null;
            }
            // 转换组装
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(result.get(0)))
                    .keyCV(Base64.getEncoder().encodeToString(result.get(1)))
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public GenerateKeyResult generateAndSaveSymmetricKey(int keyAlgType, Integer keyIndex, List<String> devicePostList) {
        log.info("HSM4TassImpl generateSymmetricKey keyAlgType: {} ,keyIndex: {}", keyAlgType, keyIndex);
        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(devicePostList);
        if (Objects.isNull(tassHSMInstance)) {
            return null;
        }
        try {
            // 获取一下对应的密钥算法类型
            Integer vendorAlgTypeCode = GlobalAlgTypeEnum.getVendorAlgTypeCode(keyAlgType, VendorConstant.TASS);
            // 生成密钥。
            // 0 索引 - 对称密钥密文;1 索引 – 对称密钥校验值
            List<byte[]> result = tassHSMInstance.generateSymmKeySyncAll(vendorAlgTypeCode, keyIndex);
            if (CollectionUtil.isEmpty(result) || result.size() != 2) {
                return null;
            }
            // 转换组装
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(result.get(0)))
                    .keyCV(Base64.getEncoder().encodeToString(result.get(1)))
                    .build();
        } catch (Exception e) {
            // 需要批量回滚一下
            removeKey(devicePostList, Lists.newArrayList(RemoveKeyInfo.builder().keyIndex(keyIndex).globalKeyType(GlobalTypeCodeConstant.SYMMETRIC_KEY).build()));
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public GenerateKeyResult generateSymmetricKey4ProKeyIndex(Integer globalKeyType, Integer proKey, Integer proKeyGlobalKeyType, String destKeyIV, int encDerivedAlg, List<String> devicePostList) {
        log.info("HSM4TassImpl generateSymmetricKey globalKeyType: {} ,proKey: {} ,proKeyGlobalKeyType: {} ,destKeyIV: {} ,encDerivedAlg: {}",
                globalKeyType, proKey, proKeyGlobalKeyType, destKeyIV, encDerivedAlg);
        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(devicePostList);
        if (Objects.isNull(tassHSMInstance)) {
            return null;
        }
        // 处理一下向量值
        byte[] destKeyIVByte = destKeyIV.getBytes();
        // 获取一下对应的密钥算法类型
        Integer vendorAlgTypeCode = GlobalAlgTypeEnum.getVendorAlgTypeCode(globalKeyType, VendorConstant.TASS);
        Integer proKeyVendorAlgTypeCode = GlobalAlgTypeEnum.getVendorAlgTypeCode(proKeyGlobalKeyType, VendorConstant.TASS);
        try {
            // 生成密钥。
            List<byte[]> result = tassHSMInstance.proGenSymmKey(proKey, proKeyVendorAlgTypeCode, NULL_BYTE, vendorAlgTypeCode, destKeyIVByte, encDerivedAlg, destKeyIVByte, NULL_BYTE);
            //  0 索引： LMK 加密的会话密钥密文；
            //  1 索引： 会话密钥的校验值；
            //  2 索引： 保护密钥加密的会话密钥密文；
            //  3 索引： 会话密钥的MAC值；
            //  4 索引： 认证数据计算后的认证值， 当导出时的算法模式为 6(GCM) 时存在。
            if (CollectionUtil.isEmpty(result) || result.size() < 4) {
                return null;
            }
            // 转换组装
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(result.get(2)))
                    .keyCV(Base64.getEncoder().encodeToString(result.get(1)))
                    .keyMac(Base64.getEncoder().encodeToString(result.get(3)))
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public GenerateKeyResult generateSymmetricKey4ProKeyInfo(Integer globalKeyType, String proKeyInfo, Integer proKeyGlobalKeyType, String proKeyCv, String destKeyIV, int encDerivedAlg, List<String> devicePostList) {
        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(devicePostList);
        if (Objects.isNull(tassHSMInstance)) {
            return null;
        }
        // 处理一下向量值
        byte[] destKeyIVByte = Base64.getDecoder().decode(destKeyIV);
        // 转换一下byte
        byte[] proKeyInfoByte = Base64.getDecoder().decode(proKeyInfo);
        byte[] proKeyCvByte = Base64.getDecoder().decode(proKeyCv);
        // 获取一下对应的密钥算法类型
        Integer vendorAlgTypeCode = GlobalAlgTypeEnum.getVendorAlgTypeCode(globalKeyType, VendorConstant.TASS);
        Integer proKeyVendorAlgTypeCode = GlobalAlgTypeEnum.getVendorAlgTypeCode(proKeyGlobalKeyType, VendorConstant.TASS);
        try {
            // 生成密钥。
            List<byte[]> result = tassHSMInstance.proGenSymmKey(proKeyInfoByte, proKeyVendorAlgTypeCode, proKeyCvByte, vendorAlgTypeCode, destKeyIVByte, encDerivedAlg, NULL_BYTE, NULL_BYTE);
            //  0 索引： LMK 加密的会话密钥密文；
            //  1 索引： 会话密钥的校验值；
            //  2 索引： 保护密钥加密的会话密钥密文；
            //  3 索引： 会话密钥的MAC值；
            //  4 索引： 认证数据计算后的认证值， 当导出时的算法模式为 6(GCM) 时存在。
            if (CollectionUtil.isEmpty(result) || result.size() < 4) {
                return null;
            }
            // 转换组装
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(result.get(2)))
                    .keyCV(Base64.getEncoder().encodeToString(result.get(1)))
                    .keyMac(Base64.getEncoder().encodeToString(result.get(3)))
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public String generateRandom(List<String> devicePostList, int length) {
        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(devicePostList);
        if (Objects.isNull(tassHSMInstance)) {
            return null;
        }
        try {
            return tassHSMInstance.genRandom(length);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public GenerateKeyResult generateSM2Key(Integer proKekIndex, List<String> devicePostList) {
        log.info("HSM4TassImpl generateSM2Key");
        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(devicePostList);
        if (Objects.isNull(tassHSMInstance)) {
            return null;
        }
        try {
            // 生成密钥
            // 0 索引 - DER 编码的公钥;1 索引 – LMK 加密的私钥密文
            List<byte[]> result = tassHSMInstance.genSM2Key();
            if (CollectionUtil.isEmpty(result) || result.size() != 2) {
                return null;
            }
            // 处理一下公钥和私钥信息
            String pubKey = Base64.getEncoder().encodeToString(result.get(0));
            String priKey = Base64.getEncoder().encodeToString(result.get(1));
            // 转换组装
            return GenerateKeyResult.builder()
                    .keyValue(String.join("&", pubKey, priKey))
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public Boolean importSymmetricKey(int keyIndex, Integer globalAlgTypeCode, String cipherByLMK, String keyCV, List<String> devicePostList) {
        log.info("HSM4TassImpl importSymmetricKey keyIndex: {} ,globalAlgTypeCode :{} ,cipherByLMK: {} ,keyCV: {}", keyIndex, globalAlgTypeCode, cipherByLMK, keyCV);
        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(devicePostList);
        if (Objects.isNull(tassHSMInstance)) {
            return null;
        }
        try {
            // 获取一下对应的密钥算法类型
            String vendorAlgTypeName = GlobalAlgTypeEnum.getVendorAlgTypeName(globalAlgTypeCode, VendorConstant.TASS);
            // 处理一下私钥和CV值
            byte[] cipherByLMKInfo = Base64.getDecoder().decode(cipherByLMK);
            byte[] keyCVInfo = Base64.getDecoder().decode(keyCV);
            // 备份密钥。
            return tassHSMInstance.importSymmKey(keyIndex, vendorAlgTypeName, cipherByLMKInfo, keyCVInfo);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public Boolean importSymmetricKey(int kekIndex, int keyIndex, Integer globalAlgTypeCode, String cipherByLMK, String keyCV, List<String> devicePostList) {
        log.info("HSM4TassImpl importSymmetricKey kekIndex: {} ,keyIndex: {} ,globalAlgTypeCode :{} ,cipherByLMK: {} ,keyCV: {}", kekIndex, keyIndex, globalAlgTypeCode, cipherByLMK, keyCV);
        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(devicePostList);
        if (Objects.isNull(tassHSMInstance)) {
            return null;
        }
        try {
            // 获取一下对应的密钥算法类型
            String vendorAlgTypeName = GlobalAlgTypeEnum.getVendorAlgTypeName(globalAlgTypeCode, VendorConstant.TASS);
            // 处理一下私钥和CV值
            byte[] cipherByLMKInfo = Base64.getDecoder().decode(cipherByLMK);
            byte[] keyCVInfo = Base64.getDecoder().decode(keyCV);
            // 备份密钥。
            return tassHSMInstance.importSymmKey(keyIndex, vendorAlgTypeName, cipherByLMKInfo, keyCVInfo);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public void importSM2Key(Integer kekIndex, Integer globalKeyType, String cipherByLMK, int keyIndex, int keyUsedType, String keyLable, String keyId, List<String> devicePostList) {
        log.info("HSM4TassImpl importSymmetricKey globalKeyType: {} ,cipherByLMK: {} ,keyIndex :{} ,keyUsedType: {} ,keyLable: {} ,keyId: {}", globalKeyType, cipherByLMK, keyIndex, keyUsedType, keyLable, keyId);
        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(devicePostList);
        if (Objects.isNull(tassHSMInstance)) {
            return;
        }
        // 获取一下对应的密钥使用用途类型
        Integer vendorKeyUsedType = GlobalUsedTypeCodeEnum.getVendorUsedTypeCode(keyUsedType, VendorConstant.TASS);
        // 获取一下对应的密钥算法类型
        Integer vendorKeyTypeCode = GlobalKeyTypeEnum.getVendorTypeCode(globalKeyType, VendorConstant.TASS);
        try {
            String[] split = StringUtils.split(cipherByLMK, "&");
            if (split.length != 2) {
                return;
            }
            // 处理一下私钥和CV值
            byte[] cipherByLMKInfo = Base64.getDecoder().decode(split[1]);
            byte[] keyLableInfo = keyLable.getBytes();
            // 导入密钥
            tassHSMInstance.importASymmKeySyncAll(vendorKeyTypeCode, 0x0007, cipherByLMKInfo, keyIndex, vendorKeyUsedType, keyLableInfo, NULL_BYTE);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public GenerateKeyResult generateAndSaveSM2Key(Integer kekIndex, Integer globalKeyType, int keyIndex, Integer keyUsedType, String keyLable, List<String> devicePostList) {
        log.info("HSM4TassImpl importSymmetricKey globalKeyType: {} ,keyIndex :{} ,keyUsedType: {} ,keyLable: {} ", globalKeyType, keyIndex, keyUsedType, keyLable);
        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(devicePostList);
        if (Objects.isNull(tassHSMInstance)) {
            return null;
        }
        // 获取一下对应的密钥算法类型
        Integer vendorKeyTypeCode = GlobalKeyTypeEnum.getVendorTypeCode(globalKeyType, VendorConstant.TASS);
        // 获取一下对应的密钥使用用途类型
        Integer vendorKeyUsedType = GlobalUsedTypeCodeEnum.getVendorUsedTypeCode(keyUsedType, VendorConstant.TASS);
        try {
            // 0 索引 - DER 编码的公钥;1 索引 – LMK 加密的私钥密文
            List<byte[]> result = tassHSMInstance.genSM2Key();
            if (CollectionUtil.isEmpty(result) || result.size() != 2) {
                return null;
            }
            byte[] priKeyByteInfo = result.get(1);
            // 处理一下私钥和CV值
            byte[] keyLableInfo = keyLable.getBytes();
            // 导入密钥
            tassHSMInstance.importASymmKeySyncAll(vendorKeyTypeCode, 0x0007, priKeyByteInfo, keyIndex, vendorKeyUsedType, keyLableInfo, NULL_BYTE);
            // 转换组装
            return GenerateKeyResult.builder()
                    .keyValue(String.join("&", Base64.getEncoder().encodeToString(result.get(0)),
                            Base64.getEncoder().encodeToString(priKeyByteInfo)))
                    .build();
        } catch (Exception e) {
            // 需要批量回滚一下
            removeKey(devicePostList, Lists.newArrayList(RemoveKeyInfo.builder().keyIndex(keyIndex).globalKeyType(GlobalTypeCodeConstant.ECC_KEY).build()));
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public GenerateKeyResult generateAndSaveSymmetricKey4ProKeyIndex(Integer globalKeyType, Integer proKeyIndex, Integer proKeyGlobalKeyType, Integer keyIndex,
                                                                     String keyLabel, String destKeyIV, int encDerivedAlg, List<String> devicePostList) {
        log.info("HSM4TassImpl generateAndSaveSymmetricKey4ProKeyIndex globalKeyType: {} ,proKeyIndex: {} ,proKeyGlobalKeyType: {} ,destKeyIV: {} ,encDerivedAlg: {} ,keyIndex: {}",
                globalKeyType, proKeyIndex, proKeyGlobalKeyType, destKeyIV, encDerivedAlg, keyIndex);

        //  获取设备链接实例
        GHSMAPI tassHSMInstance = DeviceInstanceHelper.getTassHSMInstance(devicePostList);
        if (Objects.isNull(tassHSMInstance)) {
            return null;
        }
        // 处理一下向量值
        byte[] destKeyIVByte = destKeyIV.getBytes();
        // 获取一下对应的密钥算法类型
        Integer vendorAlgTypeCode = GlobalAlgTypeEnum.getVendorAlgTypeCode(globalKeyType, VendorConstant.TASS);
        Integer proKeyVendorAlgTypeCode = GlobalAlgTypeEnum.getVendorAlgTypeCode(proKeyGlobalKeyType, VendorConstant.TASS);
        try {
            // 生成密钥。
            List<byte[]> result = tassHSMInstance.proGenSymmKey(proKeyIndex, proKeyVendorAlgTypeCode, NULL_BYTE,
                    vendorAlgTypeCode, destKeyIVByte, encDerivedAlg, destKeyIVByte, NULL_BYTE);
            if (CollectionUtil.isEmpty(result) || result.size() < 4) {
                return null;
            }
            byte[] ciphertextData = result.get(2);
            // 转换组装
            GenerateKeyResult info = GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(ciphertextData))
                    .keyCV(Base64.getEncoder().encodeToString(result.get(1)))
                    .build();
            // 将密钥导入到密码机
            result = tassHSMInstance.proImportSymmetricKeySyncAll(encDerivedAlg, proKeyIndex, proKeyVendorAlgTypeCode, NULL_BYTE,
                    "", vendorAlgTypeCode, keyIndex, keyLabel, destKeyIVByte, ciphertextData);
            if (CollectionUtil.isEmpty(result) || result.size() < 2) {
                return null;
            }
            return info.setKeyMac(Base64.getEncoder().encodeToString(result.get(1)));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public GenerateKeyResult generateSM2Key4ProKeyValue(Integer globalAlgTypeCode, String proKekInfo, List<String> devicePostList) {
        return new GenerateKeyResult();
    }
}
