package com.csp.actuator.device.factory.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.csp.actuator.api.entity.GenerateKeyResult;
import com.csp.actuator.api.entity.RemoveKeyInfo;
import com.csp.actuator.cache.DataCenterKeyCache;
import com.csp.actuator.device.DeviceInstanceHelper;
import com.csp.actuator.device.contants.GlobalTypeCodeConstant;
import com.csp.actuator.device.contants.VendorConstant;
import com.csp.actuator.device.enums.GlobalAlgLengthEnum;
import com.csp.actuator.device.enums.GlobalAlgTypeEnum;
import com.csp.actuator.device.enums.GlobalKeyTypeEnum;
import com.csp.actuator.device.enums.GlobalUsedTypeCodeEnum;
import com.csp.actuator.device.exception.DeviceException;
import com.csp.actuator.device.factory.HSMFactory;
import com.csp.actuator.device.session.VenusHsmSession;
import com.csp.actuator.utils.SM4Util;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.csp.actuator.device.contants.GlobalTypeCodeConstant.ERROR_KEY;
import static com.csp.actuator.device.contants.HsmFunctionConstant.*;


/**
 * @author Weijia Jiang
 * @version v1
 * @description 启明星辰自研密码机实现
 * @date Created in 2023-08-09 10:24
 */
@Slf4j
public class HSM4VenusImpl implements HSMFactory {

    /**
     * 删除密钥
     *
     * @param deviceInfoPort    设备信息
     * @param removeKeyInfoList 需要移除的密钥信息
     * @return true成功，false失败
     */
    @Override
    public Boolean removeKey(List<String> deviceInfoPort, List<RemoveKeyInfo> removeKeyInfoList) {
        log.info("HSM4VenusImpl removeKey removeKeyInfoList: {}", removeKeyInfoList);
        if (CollectionUtil.isEmpty(deviceInfoPort) || CollectionUtil.isEmpty(removeKeyInfoList)) {
            return Boolean.FALSE;
        }
        // 获取密码机设备连接实例
        List<VenusHsmSession> venusHsmSessionList = DeviceInstanceHelper.getVenusHSSMInstance(deviceInfoPort);
        if (CollectionUtil.isEmpty(venusHsmSessionList)) {
            return Boolean.FALSE;
        }
        boolean deleteFlag = Boolean.TRUE;
        try {
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put(PARAM_OPERATION, SDIF_DeleteKey);
            // 循环删除指定索引位置的密钥
            for (RemoveKeyInfo removeKeyInfo : removeKeyInfoList) {
                Integer globalKeyType = removeKeyInfo.getGlobalKeyType();
                // 获取对应厂商的密钥类型编码
                Integer vendorKeyTypeCode = GlobalKeyTypeEnum.getVendorTypeCode(globalKeyType, VendorConstant.VENUS_SELF);
                if (ERROR_KEY.equals(vendorKeyTypeCode)) {
                    continue;
                }
                int keyUsage = 0;
                if (GlobalTypeCodeConstant.ECC_KEY.equals(globalKeyType)) {
                    // 如果是非对称的需要处理一下密钥用途参数
                    keyUsage = GlobalUsedTypeCodeEnum.getVendorUsedTypeCode(removeKeyInfo.getGlobalKeyUsage(), VendorConstant.VENUS_SELF);
                }
                inputParam.put(PARAM_KEY_INDEX, removeKeyInfo.getKeyIndex());
                inputParam.put(PARAM_KEY_TYPE, vendorKeyTypeCode);
                inputParam.put(PARAM_KEY_USAGE, keyUsage);
                venusHsmSessionList.forEach(venusHsmSession -> venusHsmSession.execute(inputParam));
            }
        } catch (Exception e) {
            deleteFlag = Boolean.FALSE;
            log.error("HSM4VenusImpl removeKey Exception: ", e);
        } finally {
            closeVenusHsm(venusHsmSessionList);
        }
        return deleteFlag;
    }

    /**
     * 获取已用的密钥索引列表
     *
     * @param globalKeyType  密钥类型:{@link GlobalTypeCodeConstant}
     * @param globalKeyUsage 密钥用途:{@link GlobalUsedTypeCodeEnum}
     * @param startKeyIndex  起始索引
     * @param devicePostList 操作设备列表
     * @return 已用的密钥列表
     */
    @Override
    public List<Integer> listKeys(int globalKeyType, int globalKeyUsage, int startKeyIndex, List<String> devicePostList) {
        log.info("HSM4VenusImpl listKeys globalKeyType:{}, globalKeyUsage:{}, startKeyIndex:{}", globalKeyType, globalKeyUsage, startKeyIndex);
        //  获取设备链接实例
        VenusHsmSession venusHsmSession = DeviceInstanceHelper.getOneVenusHSMInstance(devicePostList);
        if (Objects.isNull(venusHsmSession)) {
            return null;
        }
        try {
            // 获取一下对应的密钥类型
            Integer vendorKeyTypeCode = GlobalKeyTypeEnum.getVendorTypeCode(globalKeyType, VendorConstant.VENUS_SELF);
            int keyUsage = 0;
            if (GlobalTypeCodeConstant.ECC_KEY.equals(globalKeyType)) {
                // 如果是非对称的需要处理一下密钥用途参数
                keyUsage = GlobalUsedTypeCodeEnum.getVendorUsedTypeCode(globalKeyUsage, VendorConstant.VENUS_SELF);
            }
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put(PARAM_OPERATION, SDIF_ListKey);
            inputParam.put(PARAM_KEY_INDEX, startKeyIndex);
            inputParam.put(PARAM_KEY_TYPE, vendorKeyTypeCode);
            inputParam.put(PARAM_KEY_USAGE, keyUsage);
            // 获取所有密钥列表
            int[] keyIds = (int[]) venusHsmSession.execute(inputParam);
            if (ArrayUtils.isEmpty(keyIds)) {
                return Lists.newArrayList();
            }
            return Arrays.stream(keyIds).boxed().collect(Collectors.toList());
        } catch (Exception e) {
            log.error("HSM4VenusImpl listKeys Exception: ", e);
        } finally {
            closeVenusHsm(venusHsmSession);
        }
        return Lists.newArrayList();
    }

    /**
     * 生成一个对称密钥，注意这个对称密钥是用平台密码机1号位KEK进行加密的
     * 这个方法常用于生成“应用KEK”，当不需要保存到密码机的时候使用（密码机共享）
     *
     * @param globalAlgTypeCode 算法类型
     * @param devicePostList    操作的设备列表
     * @return GenerateKeyResult
     */
    @Override
    public GenerateKeyResult generateSymmetricKey(int globalAlgTypeCode, List<String> devicePostList) {
        VenusHsmSession session = DeviceInstanceHelper.getOneVenusHSMInstance(devicePostList);
        try {
            // 生成一个密钥
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "GenerateRandom");
            param.put("strength", 16);
            byte[] data = (byte[]) session.execute(param);
            // 基于平台密码机Kek生成一个密钥，这个密钥是加密的
            if (ArrayUtils.isEmpty(data)) {
                throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
            }
            log.info("HSM4VenusImpl generateSymmetricKey success, key length = {}", data.length);
            // 使用软密钥加密保存
            data = SM4Util.encrypt(DataCenterKeyCache.getDataCenterKey(), data);
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(data))
                    .build();
        } catch (Exception e) {
            log.error("HSM4VenusImpl generateSymmetricKey failed, error: ", e);
            throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
        } finally {
            if (Objects.nonNull(session)) {
                session.close();
            }
        }
    }

    /**
     * 生成一个对称密钥，并且保存它到业务密码机里面去，注意这个对称密钥是用平台密码机1号位KEK进行加密的
     * 这个方法常用于生成“应用KEK”
     *
     * @param globalAlgTypeCode 算法类型
     * @param keyIndex          保存到业务密码机的密钥索引位置
     * @param devicePostList    操作的设备列表
     * @return GenerateKeyResult
     */
    @Override
    public GenerateKeyResult generateAndSaveSymmetricKey(int globalAlgTypeCode, Integer keyIndex, List<String> devicePostList) {
        List<VenusHsmSession> sessionList = DeviceInstanceHelper.getVenusHSSMInstance(devicePostList);
        if (CollectionUtil.isEmpty(sessionList)) {
            return null;
        }
        try {
            // 根据keyAlgType获取密钥长度
            Integer strength = GlobalAlgLengthEnum.getAlgLength(globalAlgTypeCode);
            // 生成一个密钥
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "GenerateRandom");
            param.put("strength", 16);
            byte[] data = (byte[]) sessionList.get(0).execute(param);
            if (ArrayUtils.isEmpty(data)) {
                throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
            }
            log.info("SDF_GenerateKeyWithKEK success, next execute SWMF_InputKEK...");

            // 再将解密完的明文，导入并保存到密码机里去
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put(PARAM_OPERATION, "SDIF_ImportKeyAndSave");
            inputParam.put(PARAM_KEY_INDEX, keyIndex);
            inputParam.put("key", data);
            inputParam.put("strength", strength);
            inputParam.put("algType", GlobalAlgTypeEnum.getVendorAlgTypeCode(globalAlgTypeCode, VendorConstant.VENUS_SELF));

            sessionList.forEach(session -> session.execute(inputParam));
            // 使用软密钥加密保存
            data = SM4Util.encrypt(DataCenterKeyCache.getDataCenterKey(), data);
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(data))
                    .build();
        } catch (Exception e) {
            log.error("HSM4VenusImpl generateAndSaveSymmetricKey failed, error: ", e);
            throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
        } finally {
            closeVenusHsm(sessionList);
        }
    }

    /**
     * 生成一个对称密钥，注意这个对称密钥是用业务密码机的KEK进行加密的，具体使用的哪一个，需要看proKeyIndex参数
     * 这个方法常用于根据“应用KEK”--->生成“应用DEK”
     *
     * @param globalAlgTypeCode       算法类型
     * @param proKeyIndex             父级KEK密钥索引
     * @param proKeyGlobalAlgTypeCode 父级KEK算法类型
     * @param destKeyIV               偏移量IV
     * @param encDerivedAlg           暂不知
     * @param devicePostList          操作的设备列表
     * @return GenerateKeyResult
     */
    @Override
    public GenerateKeyResult generateSymmetricKey4ProKeyIndex(Integer globalAlgTypeCode, Integer proKeyIndex, Integer proKeyGlobalAlgTypeCode, String destKeyIV, int encDerivedAlg, List<String> devicePostList) {
        log.info("HSM4VenusImpl generateSymmetricKey4ProKeyIndex globalAlgTypeCode:{} ,proKeyIndex:{} ,proKeyGlobalAlgTypeCode:{} ,destKeyIV: {}, encDerivedAlg: {}",
                globalAlgTypeCode, proKeyIndex, proKeyGlobalAlgTypeCode, destKeyIV, encDerivedAlg);
        //  获取设备链接实例
        VenusHsmSession venusHsmSession = DeviceInstanceHelper.getOneVenusHSMInstance(devicePostList);
        if (Objects.isNull(venusHsmSession)) {
            return null;
        }
        try {
            // 长度
            Integer strength = GlobalAlgLengthEnum.getAlgLength(globalAlgTypeCode);
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "SDF_GenerateKeyWithKEK");
            param.put("kekIndex", proKeyIndex);
            param.put("strength", strength);
            byte[] data = (byte[]) venusHsmSession.execute(param);
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(data))
                    .build();
        } catch (Exception e) {
            log.error("HSM4VenusImpl generateSymmetricKey4ProKeyIndex failed, error: ", e);
            throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
        } finally {
            closeVenusHsm(venusHsmSession);
        }
    }

    /**
     * 生成一个对称密钥，注意这个对称密钥是用业务密码机的KEK进行加密的，具体如何加密，需要看proKeyInfo参数
     * 这个方法常用于根据“应用KEK”的内容，而不是索引--->生成“应用DEK”
     *
     * @param globalAlgTypeCode       算法类型
     * @param proKeyInfo              父级KEK密钥内容（被平台密码机1号位索引加密过的）
     * @param proKeyGlobalAlgTypeCode 父级KEK算法类型
     * @param proKeyCv                父级KEK
     * @param destKeyIV               偏移量IV
     * @param encDerivedAlg           暂时用不着
     * @param devicePostList          操作的设备列表
     * @return GenerateKeyResult
     */
    @Override
    public GenerateKeyResult generateSymmetricKey4ProKeyInfo(Integer globalAlgTypeCode, String proKeyInfo, Integer proKeyGlobalAlgTypeCode, String proKeyCv, String destKeyIV, int encDerivedAlg, List<String> devicePostList) {
        log.info("HSM4VenusImpl generateSymmetricKey4ProKeyInfo globalAlgTypeCode:{} ,proKeyInfo:{} ,proKeyGlobalAlgTypeCode:{} ,destKeyIV: {}, encDerivedAlg: {}",
                globalAlgTypeCode, proKeyInfo, proKeyGlobalAlgTypeCode, destKeyIV, encDerivedAlg);
        VenusHsmSession venusHsmSession = DeviceInstanceHelper.getOneVenusHSMInstance(devicePostList);
        if (Objects.isNull(venusHsmSession)) {
            return null;
        }
        try {
            // 第一步，通过软密钥，解密应用kek
            byte[] decryptProKeyInfoBytes = SM4Util.decrypt(DataCenterKeyCache.getDataCenterKey(), proKeyInfo);

            // 第二步、将解密完的明文，导入并保存到业务密码机里去，索引固定用10号位
            Integer proKeyIndex = 10;
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put(PARAM_OPERATION, "SDIF_ImportKeyAndSave");
            inputParam.put(PARAM_KEY_INDEX, proKeyIndex);
            inputParam.put("key", decryptProKeyInfoBytes);
            inputParam.put("strength", GlobalAlgLengthEnum.getAlgLength(proKeyGlobalAlgTypeCode));
            inputParam.put("algType", GlobalAlgTypeEnum.getVendorAlgTypeCode(proKeyGlobalAlgTypeCode, VendorConstant.VENUS_SELF));
            venusHsmSession.execute(inputParam);

            // 第三步、使用刚刚导入的密钥，生成一个key，生成的即是密文的
            Map<String, Object> param3 = new HashMap<>();
            param3.put("operation", "SDF_GenerateKeyWithKEK");
            param3.put("kekIndex", proKeyIndex);
            param3.put("strength", GlobalAlgLengthEnum.getAlgLength(globalAlgTypeCode));
            byte[] data = (byte[]) venusHsmSession.execute(param3);

            // 第四步、删除掉刚刚导入的kek
            Integer vendorKeyTypeCode = GlobalKeyTypeEnum.getVendorTypeCode(GlobalTypeCodeConstant.SYMMETRIC_KEY, VendorConstant.VENUS_SELF);
            Map<String, Object> removeParam = new HashMap<>();
            removeParam.put(PARAM_OPERATION, SDIF_DeleteKey);
            removeParam.put(PARAM_KEY_INDEX, proKeyIndex);
            removeParam.put(PARAM_KEY_TYPE, vendorKeyTypeCode);
            removeParam.put(PARAM_KEY_USAGE, 0);
            venusHsmSession.execute(removeParam);

            // 第五步，返回生成的key
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(data))
                    .build();
        } catch (Exception e) {
            log.error("HSM4VenusImpl generateSymmetricKey4ProKeyInfo failed, error: ", e);
            throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
        } finally {
            closeVenusHsm(venusHsmSession);
        }
    }

    /**
     * 生成随机数
     *
     * @param devicePostList 操作的设备列表
     * @param length         随机数长度
     * @return String
     */
    @Override
    public String generateRandom(List<String> devicePostList, int length) {
        log.info("HSM4VenusImpl generateRandom devicePostList:{}, length:{}", devicePostList, length);
        //  获取设备链接实例
        VenusHsmSession venusHsmSession = DeviceInstanceHelper.getOneVenusHSMInstance(devicePostList);
        if (Objects.isNull(venusHsmSession)) {
            return null;
        }
        try {
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put(PARAM_OPERATION, GenerateRandom);
            inputParam.put(PARAM_STRENGTH, length);
            // 获取所有密钥列表
            byte[] data = (byte[]) venusHsmSession.execute(inputParam);
            if (ArrayUtils.isEmpty(data)) {
                return null;
            }
            return Arrays.toString(data);
        } catch (Exception e) {
            log.error("HSM4VenusImpl generateRandom Exception: ", e);
            return null;
        } finally {
            closeVenusHsm(venusHsmSession);
        }
    }

    @Override
    public Boolean importSymmetricKey(int keyIndex, Integer globalAlgTypeCode, String cipherByLMK, String keyCV, List<String> devicePostList) {
        List<VenusHsmSession> sessionList = DeviceInstanceHelper.getVenusHSSMInstance(devicePostList);
        if (CollectionUtil.isEmpty(sessionList)) {
            return Boolean.FALSE;
        }
        try {
            // 导入的是KEK密钥，KEK密钥由软算法加密，需要解密。
            byte[] decryptData = SM4Util.decrypt(DataCenterKeyCache.getDataCenterKey(), cipherByLMK);
            log.info("SDF_DeDEK success, next execute SWMF_InputKEK...");

            // 导入sm4对称密钥到`业务密码机`
            Integer strength = GlobalAlgLengthEnum.getAlgLength(globalAlgTypeCode);
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put("operation", "SDIF_ImportKeyAndSave");
            inputParam.put("keyIndex", keyIndex);
            inputParam.put("key", decryptData);
            inputParam.put("strength", strength);
            inputParam.put("algType", GlobalAlgTypeEnum.getVendorAlgTypeCode(globalAlgTypeCode, VendorConstant.VENUS_SELF));

            sessionList.forEach(session -> session.execute(inputParam));
            return true;
        } catch (Exception e) {
            log.error("HSM4VenusImpl importSymmetricKey failed, error:", e);
            throw new DeviceException("导入密钥失败，请检查服务器连接是否正常！");
        } finally {
            closeVenusHsm(sessionList);
        }
    }


    @Override
    public Boolean importSymmetricKey(int kekIndex, int keyIndex, Integer globalAlgTypeCode, String cipherByLMK, String keyCV, List<String> devicePostList) {
        List<VenusHsmSession> sessionList = DeviceInstanceHelper.getVenusHSSMInstance(devicePostList);
        if (CollectionUtil.isEmpty(sessionList)) {
            return Boolean.FALSE;
        }
        try {
            Map<String, Object> param = Maps.newHashMap();
            // 生成的密钥是密文的，导入需要的是明文，所以此处需要先使用`业务密码机`解密
            param.put("operation", "SDF_DeDEK");
            param.put("kekIndex", kekIndex);
            param.put("key", Base64.getDecoder().decode(cipherByLMK));
            byte[] decryptData = (byte[]) sessionList.get(0).execute(param);

            // 导入sm4对称密钥到`业务密码机`
            Integer strength = GlobalAlgLengthEnum.getAlgLength(globalAlgTypeCode);
            Map<String, Object> inputParam = Maps.newHashMap();
            inputParam.put("operation", "SDIF_ImportKeyAndSave");
            inputParam.put("keyIndex", keyIndex);
            inputParam.put("key", decryptData);
            inputParam.put("strength", strength);
            inputParam.put("algType", GlobalAlgTypeEnum.getVendorAlgTypeCode(globalAlgTypeCode, VendorConstant.VENUS_SELF));

            sessionList.forEach(session -> session.execute(inputParam));
            return true;
        } catch (Exception e) {
            log.error("HSM4VenusImpl importSymmetricKey failed, error:", e);
            throw new DeviceException("导入密钥失败，请检查服务器连接是否正常！！");
        } finally {
            closeVenusHsm(sessionList);
        }
    }

    @Override
    public void importSM2Key(Integer kekIndex, Integer globalAlgTypeCode, String cipherByLMK, int keyIndex, int keyUsedType, String keyLable, String keyId, List<String> devicePostList) {
        List<VenusHsmSession> sessionList = DeviceInstanceHelper.getVenusHSSMInstance(devicePostList);
        if (CollectionUtil.isEmpty(sessionList)) {
            return;
        }
        try {
            // 处理密钥，将密钥分开成公钥私钥
            String[] keyInfo = StringUtils.split(cipherByLMK, "&");
            byte[] priKey = Base64.getDecoder().decode(keyInfo[1]);
            byte[] pubKey = Base64.getDecoder().decode(keyInfo[0]);
            // 使用平台密码机给私钥解密一下子
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "SDF_InternalDecrypt_SM4");
            param.put("data", priKey);
            param.put("kekIndex", kekIndex);
            byte[] decryPriKey = (byte[]) sessionList.get(0).execute(param);
            // 补一下0
            byte[] newPubKey = new byte[128];
            System.arraycopy(pubKey, 0, newPubKey, 32, 32);
            System.arraycopy(pubKey, 32, newPubKey, 96, 32);

            // 导入sm2密钥到业务密码机
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put("operation", "SDIF_ImportKeyPair_ECC");
            inputParam.put("privateKey", decryPriKey);
            inputParam.put("publicKey", newPubKey);
            inputParam.put("keyIndex", keyIndex);
            inputParam.put("keyUsedType", GlobalUsedTypeCodeEnum.getVendorUsedTypeCode(keyUsedType, VendorConstant.VENUS_SELF));
            sessionList.forEach(session -> session.execute(inputParam));
        } catch (Exception e) {
            log.error("HSM4VenusImpl importSM2Key failed, error: ", e);
            throw new DeviceException("导入密钥失败，请检查服务器连接是否正常！");
        } finally {
            closeVenusHsm(sessionList);
        }
    }

    /**
     * 使用”业务密码机“的kekIndex生成加密一个SM2密钥
     *
     * @param proKekIndex    父级KEK密钥索引
     * @param devicePostList 操作的设备列表
     * @return GenerateKeyResult
     */
    @Override
    public GenerateKeyResult generateSM2Key(Integer proKekIndex, List<String> devicePostList) {
        VenusHsmSession venusHsmSession = DeviceInstanceHelper.getOneVenusHSMInstance(devicePostList);
        if (Objects.isNull(venusHsmSession)) {
            return null;
        }
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "GenerateKeyPair_ECC");
            param.put("algorithm", "SM2");
            param.put("strength", 256);
            List<byte[]> result = (List<byte[]>) venusHsmSession.execute(param);
            byte[] priKey = null, pubKey = null;
            if (result != null && result.size() > 1) {
                priKey = result.get(0);
                pubKey = result.get(1);
            } else {
                throw new DeviceException("使用平台密码机生成ECC密钥对失败！");
            }
            byte[] newPubKey = new byte[64];
            System.arraycopy(pubKey, 32, newPubKey, 0, 32);
            System.arraycopy(pubKey, 96, newPubKey, 32, 32);

            // 使用”业务密码机“给私钥加密一下子再返回
            param = new HashMap<>();
            param.put("operation", "SDF_InternalEncrypt_SM4");
            param.put("kekIndex", proKekIndex);
            param.put("data", priKey);
            byte[] encryptPriKey = (byte[]) venusHsmSession.execute(param);

            // 转换组装返回结果
            return GenerateKeyResult.builder()
                    .keyValue(String.join("&", Base64.getEncoder().encodeToString(newPubKey), Base64.getEncoder().encodeToString(encryptPriKey)))
                    .build();
        } catch (Exception e) {
            log.error("HSM4VenusImpl generateSM2Key failed, error: ", e);
            throw new DeviceException("生成非对称密钥失败，请检查服务器连接是否正常！");
        } finally {
            closeVenusHsm(venusHsmSession);
        }
    }

    @Override
    public GenerateKeyResult generateSM2Key4ProKeyValue(Integer proKeyGlobalAlgTypeCode, String proKekInfo, List<String> devicePostList) {
        VenusHsmSession venusHsmSession = DeviceInstanceHelper.getOneVenusHSMInstance(devicePostList);
        if (Objects.isNull(venusHsmSession)) {
            return null;
        }
        try {
            // 首先使用平台密码机生成一个ECC密钥对
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "GenerateKeyPair_ECC");
            param.put("algorithm", "SM2");
            param.put("strength", 256);
            List<byte[]> result = (List<byte[]>) venusHsmSession.execute(param);
            byte[] priKey = null, pubKey = null;
            if (result != null && result.size() > 1) {
                priKey = result.get(0);
                pubKey = result.get(1);
            } else {
                throw new DeviceException("使用平台密码机生成ECC密钥对失败！");
            }
            byte[] newPubKey = new byte[64];
            System.arraycopy(pubKey, 32, newPubKey, 0, 32);
            System.arraycopy(pubKey, 96, newPubKey, 32, 32);

            // 第一步，通过平台密码机，解密应用kek
            byte[] decryptProKeyInfoBytes = SM4Util.decrypt(DataCenterKeyCache.getDataCenterKey(), proKekInfo);

            // 第二步、将解密完的明文，导入并保存到业务密码机里去，索引固定用10号位
            Integer proKekIndex = 10;
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put(PARAM_OPERATION, "SDIF_ImportKeyAndSave");
            inputParam.put(PARAM_KEY_INDEX, proKekIndex);
            inputParam.put("key", decryptProKeyInfoBytes);
            inputParam.put("strength", GlobalAlgLengthEnum.getAlgLength(proKeyGlobalAlgTypeCode));
            inputParam.put("algType", GlobalAlgTypeEnum.getVendorAlgTypeCode(proKeyGlobalAlgTypeCode, VendorConstant.VENUS_SELF));
            venusHsmSession.execute(inputParam);

            // 第三步、使用刚刚导入的密钥， 使用”业务密码机“给私钥加密一下子再返回
            param = new HashMap<>();
            param.put("operation", "SDF_InternalEncrypt_SM4");
            param.put("kekIndex", proKekIndex);
            param.put("data", priKey);
            byte[] encryptPriKey = (byte[]) venusHsmSession.execute(param);

            // 第四步、删除掉刚刚导入的kek
            Integer vendorKeyTypeCode = GlobalKeyTypeEnum.getVendorTypeCode(GlobalTypeCodeConstant.SYMMETRIC_KEY, VendorConstant.VENUS_SELF);
            Map<String, Object> removeParam = new HashMap<>();
            removeParam.put(PARAM_OPERATION, SDIF_DeleteKey);
            removeParam.put(PARAM_KEY_INDEX, proKekIndex);
            removeParam.put(PARAM_KEY_TYPE, vendorKeyTypeCode);
            removeParam.put(PARAM_KEY_USAGE, 0);
            venusHsmSession.execute(removeParam);

            // 第五步，返回生成的密钥
            return GenerateKeyResult.builder()
                    .keyValue(String.join("&", Base64.getEncoder().encodeToString(newPubKey), Base64.getEncoder().encodeToString(encryptPriKey)))
                    .build();
        } catch (Exception e) {
            log.error("HSM4VenusImpl generateSM2Key failed, error: ", e);
            throw new DeviceException("生成非对称密钥失败，请检查服务器连接是否正常！");
        } finally {
            closeVenusHsm(venusHsmSession);
        }
    }

    @Override
    public GenerateKeyResult generateAndSaveSM2Key(Integer kekIndex, Integer globalAlgTypeCode, int keyIndex, Integer keyUsedType, String keyLabel, List<String> devicePostList) {
        List<VenusHsmSession> sessionList = DeviceInstanceHelper.getVenusHSSMInstance(devicePostList);
        if (CollectionUtil.isEmpty(sessionList)) {
            return null;
        }
        try {
            // 使用业务密码机生成ECC密钥对
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "GenerateKeyPair_ECC");
            param.put("algorithm", "SM2");
            param.put("strength", 256);
            List<byte[]> result = (List<byte[]>) sessionList.get(0).execute(param);
            byte[] priKey = null, pubKey = null;
            if (result != null && result.size() > 1) {
                priKey = result.get(0);
                pubKey = result.get(1);
            } else {
                throw new DeviceException("使用平台密码机生成ECC密钥对失败！");
            }

            byte[] newPubKey = new byte[64];
            System.arraycopy(pubKey, 32, newPubKey, 0, 32);
            System.arraycopy(pubKey, 96, newPubKey, 32, 32);

            // 导入业务密码机
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put("operation", "SDIF_ImportKeyPair_ECC");
            inputParam.put("privateKey", priKey);
            inputParam.put("publicKey", pubKey);
            inputParam.put("keyIndex", keyIndex);
            inputParam.put("keyUsedType", GlobalUsedTypeCodeEnum.getVendorUsedTypeCode(keyUsedType, VendorConstant.VENUS_SELF));
            sessionList.forEach(session -> session.execute(inputParam));

            // 使用"业务密码机"给私钥加密一下子再返回
            param = new HashMap<>();
            param.put("operation", "SDF_InternalEncrypt_SM4");
            param.put("kekIndex", kekIndex);
            param.put("data", priKey);
            byte[] encryptPriKey = (byte[]) sessionList.get(0).execute(param);

            // 转换组装返回结果
            return GenerateKeyResult.builder()
                    .keyValue(String.join("&", Base64.getEncoder().encodeToString(newPubKey),
                            Base64.getEncoder().encodeToString(encryptPriKey)))
                    .build();
        } catch (Exception e) {
            log.error("HSM4VenusImpl generateAndSaveSM2Key failed, error: ", e);
            throw new DeviceException("生成非对称密钥失败，请检查服务器连接是否正常！");
        } finally {

            closeVenusHsm(sessionList);
        }
    }

    /**
     * 生成一个对称密钥，并且保存它到业务密码机里面去，注意这个对称密钥是用业务密码机的KEK加密的
     * 这个方法常用于通过“应用KEK”--------> 生成”应用DEk“
     *
     * @param globalAlgTypeCode   算法类型
     * @param proKeyIndex         父级KEK密钥索引
     * @param proKeyGlobalKeyType 父级KEK算法类型
     * @param keyIndex            待导入的密钥索引
     * @param keyLabel            待导入的密钥Label
     * @param destKeyIV           偏移量IV
     * @param encDerivedAlg       暂时用不着
     * @param devicePostList      操作的设备列表
     * @return GenerateKeyResult
     */
    @Override
    public GenerateKeyResult generateAndSaveSymmetricKey4ProKeyIndex(Integer globalAlgTypeCode, Integer proKeyIndex, Integer proKeyGlobalKeyType, Integer keyIndex, String keyLabel, String destKeyIV, int encDerivedAlg, List<String> devicePostList) {
        List<VenusHsmSession> sessionList = DeviceInstanceHelper.getVenusHSSMInstance(devicePostList);
        if (CollectionUtil.isEmpty(sessionList)) {
            return null;
        }
        try {
            // 转换长度
            Integer strength = GlobalAlgLengthEnum.getAlgLength(globalAlgTypeCode);
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "SDF_GenerateKeyWithKEK");
            param.put("kekIndex", proKeyIndex);
            param.put("strength", strength);
            byte[] data = (byte[]) sessionList.get(0).execute(param);
            // 基于Kek生成一个密钥，然后再导入进密码机
            if (ArrayUtils.isEmpty(data)) {
                throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
            }

            // 生成的密钥是密文的，导入需要的是明文，所以此处需要先解密。
            param = new HashMap<>();
            param.put("operation", "SDF_DeDEK");
            param.put("kekIndex", proKeyIndex);
            param.put("key", data);
            byte[] decryptData = (byte[]) sessionList.get(0).execute(param);

            // 长度
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put("operation", "SDIF_ImportKeyAndSave");
            inputParam.put("keyIndex", keyIndex);
            inputParam.put("key", decryptData);
            inputParam.put("strength", strength);
            inputParam.put("algType", GlobalAlgTypeEnum.getVendorAlgTypeCode(globalAlgTypeCode, VendorConstant.VENUS_SELF));

            sessionList.forEach(session -> session.execute(inputParam));
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(data))
                    .build();
        } catch (Exception e) {
            log.error("HSM4VenusImpl generateAndSaveSymmetricKey4ProKeyIndex failed, error: ", e);
            throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
        } finally {
            closeVenusHsm(sessionList);
        }
    }

    /**
     * 异步关闭密码机的连接
     *
     * @param venusHsmSession VenusHsmSession实例
     */
    private void closeVenusHsm(VenusHsmSession venusHsmSession) {
        List<VenusHsmSession> venusHsmSessions = Lists.newArrayList(venusHsmSession);
        this.closeVenusHsm(venusHsmSessions);
    }

    /**
     * 异步关闭密码机的连接
     *
     * @param venusHsmSessions VenusHsmSession列表
     */
    private void closeVenusHsm(List<VenusHsmSession> venusHsmSessions) {
        new Thread(() -> {
            venusHsmSessions.forEach(VenusHsmSession::destroyHsm);
        }).start();
    }
}
