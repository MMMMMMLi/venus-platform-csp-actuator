package com.csp.actuator.device.factory.impl;

import com.csp.actuator.api.entity.GenerateKeyResult;
import com.csp.actuator.api.entity.RemoveKeyInfo;
import com.csp.actuator.device.DeviceInstanceHelper;
import com.csp.actuator.device.contants.GlobalUsedTypeCodeConstant;
import com.csp.actuator.device.enums.GlobalAlgLengthEnum;
import com.csp.actuator.device.exception.DeviceException;
import com.csp.actuator.device.factory.HSMFactory;
import com.csp.actuator.device.session.GMT0018SDFSession;
import com.csp.actuator.cache.DataCenterKeyCache;
import com.csp.actuator.utils.SM4Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.csp.actuator.device.contants.HsmFunctionConstant.PARAM_KEY_INDEX;
import static com.csp.actuator.device.contants.HsmFunctionConstant.PARAM_OPERATION;


/**
 * 启明星辰服务器密码机接口实现
 *
 * @author Weijia Jiang
 * @version v1
 * @description 启明星辰服务器密码机接口实现
 * @date Created in 2023-04-25 11:42
 */
@Slf4j
public class HSM4SansecImpl implements HSMFactory {

    @Override
    public Boolean removeKey(List<String> deviceInfoPort, List<RemoveKeyInfo> removeKeyInfoList) {
        log.info("Venus Device RemoveKey Interface not implemented...");
        return Boolean.TRUE;
    }

    @Override
    public List<Integer> listKeys(int globalKeyType, int globalKeyUsage, int startKeyIndex, List<String> devicePostList) {
        return null;
    }

    @Override
    public GenerateKeyResult generateSymmetricKey(int keyAlgType, List<String> devicePostList) {
        GMT0018SDFSession session = DeviceInstanceHelper.getOneSansecHSMInstance(devicePostList);
        try {
            // 生成一个密钥
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "GenerateRandom");
            param.put("strength", 16);
            byte[] data = (byte[]) session.execute(param);
            if (ArrayUtils.isEmpty(data)) {
                throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
            }
            log.info("HSM4SansecImpl generateSymmetricKey success, key length = {}", data.length);
            // 使用软密钥加密保存
            data = SM4Util.encrypt(DataCenterKeyCache.getDataCenterKey(), data);
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(data))
                    .build();
        } catch (Exception e) {
            log.error("HSM4SansecImpl generateSymmetricKey failed, error: ", e);
            throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
        } finally {
            if (Objects.nonNull(session)) {
                session.close();
            }
        }
    }

    @Override
    public GenerateKeyResult generateAndSaveSymmetricKey(int keyAlgType, Integer keyIndex, List<String> devicePostList) {
        List<GMT0018SDFSession> sessionList = DeviceInstanceHelper.getSansecHSSMInstance(devicePostList);
        try {
            // 长度
            Integer strength = GlobalAlgLengthEnum.getAlgLength(keyAlgType);
            // 生成一个密钥
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "GenerateRandom");
            param.put("strength", 16);
            byte[] data = (byte[]) sessionList.get(0).execute(param);
            if (ArrayUtils.isEmpty(data)) {
                throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
            }
            log.info("SDF_GenerateKeyWithKEK success, next execute SWMF_InputKEK...");

            // 长度
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put("operation", "SWMF_InputKEK");
            inputParam.put("keyIndex", keyIndex);
            inputParam.put("key", data);
            inputParam.put("strength", strength);
            sessionList.forEach(session -> session.execute(inputParam));
            log.info("SWMF_InputKEK success...");
            // 使用软密钥加密保存
            data = SM4Util.encrypt(DataCenterKeyCache.getDataCenterKey(), data);
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(data))
                    .build();
        } catch (Exception e) {
            log.error("generateAndSaveSymmetricKey failed, error: {}", e.getMessage());
            e.printStackTrace();
            throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
        } finally {
            sessionList.forEach(GMT0018SDFSession::destroyHsm);
        }
    }

    @Override
    public GenerateKeyResult generateSymmetricKey4ProKeyIndex(Integer globalKeyType, Integer proKeyIndex, Integer proKeyGlobalKeyType, String destKeyIV, int encDerivedAlg, List<String> devicePostList) {
        GMT0018SDFSession session = DeviceInstanceHelper.getOneSansecHSMInstance(devicePostList);
        try {
            // 长度
            Integer strength = GlobalAlgLengthEnum.getAlgLength(globalKeyType);
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "SDF_GenerateKeyWithKEK");
            param.put("kekIndex", proKeyIndex);
            param.put("strength", strength);
            byte[] data = (byte[]) session.execute(param);
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(data))
                    .build();
        } catch (Exception e) {
            log.error("generateSymmetricKey4ProKeyIndex failed, error:{}", e.getMessage());
            e.printStackTrace();
            throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
        } finally {
            session.destroyHsm();
        }
    }

    @Override
    public GenerateKeyResult generateSymmetricKey4ProKeyInfo(Integer globalAlgTypeCode, String proKeyInfo, Integer proKeyGlobalKeyType, String proKeyCv, String destKeyIV, int encDerivedAlg, List<String> devicePostList) {
        log.info("HSM4VenusImpl generateSymmetricKey4ProKeyInfo globalAlgTypeCode:{} ,proKeyInfo:{} ,proKeyGlobalKeyType:{} ,destKeyIV: {}, encDerivedAlg: {}",
                globalAlgTypeCode, proKeyInfo, proKeyGlobalKeyType, destKeyIV, encDerivedAlg);
        GMT0018SDFSession snasecSession = DeviceInstanceHelper.getOneSansecHSMInstance(devicePostList);
        try {
            // 第一步，通过软密钥，解密应用kek
            byte[] decryptProKeyInfoBytes = SM4Util.decrypt(DataCenterKeyCache.getDataCenterKey(), proKeyInfo);

            // 第二步、将解密完的明文，导入并保存到业务密码机里去，索引固定用10
            Integer proKeyIndex = 10;
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put(PARAM_OPERATION, "SWMF_InputKEK");
            inputParam.put(PARAM_KEY_INDEX, proKeyIndex);
            inputParam.put("key", decryptProKeyInfoBytes);
            inputParam.put("strength", GlobalAlgLengthEnum.getAlgLength(proKeyGlobalKeyType));
            snasecSession.execute(inputParam);

            // 第三步、使用刚刚导入的密钥，生成一个key，生成的即是密文的
            Map<String, Object> param3 = new HashMap<>();
            param3.put("operation", "SDF_GenerateKeyWithKEK");
            param3.put("kekIndex", proKeyIndex);
            param3.put("strength", GlobalAlgLengthEnum.getAlgLength(globalAlgTypeCode));
            byte[] data = (byte[]) snasecSession.execute(param3);

            // 第五步，直接返回生成的key，三未的不需要删除密钥索引
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(data))
                    .build();
        } catch (Exception e) {
            log.error("HSM4VenusImpl generateSymmetricKey4ProKeyInfo failed, error: ", e);
            throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
        } finally {
            if (Objects.nonNull(snasecSession)) {
                snasecSession.destroyHsm();
            }
        }
    }

    @Override
    public String generateRandom(List<String> devicePostList, int length) {
        GMT0018SDFSession session = DeviceInstanceHelper.getOneSansecHSMInstance(devicePostList);
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "GenerateRandom");
            param.put("strength", length);
            byte[] random = (byte[]) session.execute(param);
            return Arrays.toString(random);
        } catch (Exception e) {
            log.error("generateRandom failed, error:{}", e.getMessage());
            e.printStackTrace();
            throw new DeviceException("生成随机字符串失败，请检查服务器连接是否正常！");
        } finally {
            session.destroyHsm();
        }
    }

    @Override
    public Boolean importSymmetricKey(int keyIndex, Integer globalAlgTypeCode, String cipherByLMK, String keyCV, List<String> devicePostList) {
        List<GMT0018SDFSession> sessionList = DeviceInstanceHelper.getSansecHSSMInstance(devicePostList);
        try {
            // 导入的是KEK密钥，KEK密钥由软算法加密，需要解密。
            byte[] decryptData = SM4Util.decrypt(DataCenterKeyCache.getDataCenterKey(), cipherByLMK);
            log.info("SDF_DeDEK success, next execute SWMF_InputKEK...");

            // 长度
            Integer strength = GlobalAlgLengthEnum.getAlgLength(globalAlgTypeCode);
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put("operation", "SWMF_InputKEK");
            inputParam.put("keyIndex", keyIndex);
            inputParam.put("key", decryptData);
            inputParam.put("strength", strength);
            sessionList.forEach(session -> session.execute(inputParam));
            log.info("SWMF_InputKEK success...");
            return true;
        } catch (Exception e) {
            log.error("importSymmetricKey failed, error:{}", e.getMessage());
            e.printStackTrace();
            throw new DeviceException("导入密钥失败，请检查服务器连接是否正常！");
        } finally {
            sessionList.forEach(GMT0018SDFSession::destroyHsm);
        }
    }

    @Override
    public Boolean importSymmetricKey(int kekIndex, int keyIndex, Integer globalAlgTypeCode, String cipherByLMK, String keyCV, List<String> devicePostList) {
        List<GMT0018SDFSession> sessionList = DeviceInstanceHelper.getSansecHSSMInstance(devicePostList);
        try {
            Map<String, Object> param = new HashMap<>();
            // 生成的密钥是密文的，导入需要的是明文，所以此处需要先解密。
            param = new HashMap<>();
            param.put("operation", "SDF_DeDEK");
            param.put("kekIndex", kekIndex);
            param.put("key", Base64.getDecoder().decode(cipherByLMK));
            byte[] encryData = (byte[]) sessionList.get(0).execute(param);
            log.info("SDF_DeDEK success, next execute SWMF_InputKEK...");

            // 长度
            Integer strength = GlobalAlgLengthEnum.getAlgLength(globalAlgTypeCode);
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put("operation", "SWMF_InputKEK");
            inputParam.put("keyIndex", keyIndex);
            inputParam.put("key", encryData);
            inputParam.put("strength", strength);
            sessionList.forEach(session -> session.execute(inputParam));
            log.info("SWMF_InputKEK success...");
            return true;
        } catch (Exception e) {
            log.error("importSymmetricKey failed, error:{}", e.getMessage());
            e.printStackTrace();
            throw new DeviceException("导入密钥失败，请检查服务器连接是否正常！");
        } finally {
            sessionList.forEach(GMT0018SDFSession::destroyHsm);
        }
    }

    @Override
    public void importSM2Key(Integer kekIndex, Integer globalKeyType, String cipherByLMK, int keyIndex, int keyUsedType, String keyLabel, String keyId, List<String> devicePostList) {
        List<GMT0018SDFSession> sessionList = DeviceInstanceHelper.getSansecHSSMInstance(devicePostList);
        try {
            // 处理密钥
            String[] keyInfo = StringUtils.split(cipherByLMK, "&");
            byte[] priKey = Base64.getDecoder().decode(keyInfo[1]);
            byte[] pubKey = Base64.getDecoder().decode(keyInfo[0]);
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "SDF_InternalDecrypt_SM4");
            param.put("data", priKey);
            param.put("kekIndex", kekIndex);
            byte[] decryPriKey = (byte[]) sessionList.get(0).execute(param);
            log.info("SDF_InternalDecrypt_SM4 success, next execute SWCSM_InputECCKeyPair...");
            // 补一下0
            byte[] newPubKey = new byte[128];
            System.arraycopy(pubKey, 0, newPubKey, 32, 32);
            System.arraycopy(pubKey, 32, newPubKey, 96, 32);
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put("operation", "SWCSM_InputECCKeyPair");
            inputParam.put("privateKey", decryPriKey);
            inputParam.put("publicKey", newPubKey);
            inputParam.put("keyIndex", keyIndex);
            if (GlobalUsedTypeCodeConstant.KEY_USAGE_CODE_SIGN.equals(keyUsedType)) {
                inputParam.put("keyIndexType", 1);
            }
            sessionList.forEach(session -> session.execute(inputParam));
            log.info("SWCSM_InputECCKeyPair success...");
        } catch (Exception e) {
            log.error("importSM2Key failed, error:{}", e.getMessage());
            e.printStackTrace();
            throw new DeviceException("导入密钥失败，请检查服务器连接是否正常！");
        } finally {
            sessionList.forEach(GMT0018SDFSession::destroyHsm);
        }
    }


    @Override
    public GenerateKeyResult generateSM2Key(Integer proKekIndex, List<String> devicePostList) {
        GMT0018SDFSession snasecSession = DeviceInstanceHelper.getOneSansecHSMInstance(devicePostList);
        try {
            // 调用业务密码机，生成一个ECC密钥对
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "GenerateKeyPair_ECC");
            param.put("algorithm", "SM2");
            param.put("strength", 256);
            List<byte[]> result = (List<byte[]>) snasecSession.execute(param);
            log.info("GenerateKeyPair_ECC success, next execute SDF_InternalEncrypt_SM4...");
            byte[] priKey = null, pubKey = null;
            if (result != null && result.size() > 1) {
                priKey = result.get(0);
                pubKey = result.get(1);
            } else {
                throw new DeviceException("生成非对称密钥失败！");
            }
            byte[] newPubKey = new byte[64];
            System.arraycopy(pubKey, 32, newPubKey, 0, 32);
            System.arraycopy(pubKey, 96, newPubKey, 32, 32);
            // 调用”业务密码机“，给私钥加密一下子，才能保存到数据库
            param = new HashMap<>();
            param.put("operation", "SDF_InternalEncrypt_SM4");
            param.put("kekIndex", proKekIndex);
            param.put("data", priKey);
            byte[] encryptPriKey = (byte[]) snasecSession.execute(param);

            // 转换组装
            return GenerateKeyResult.builder()
                    .keyValue(String.join("&", Base64.getEncoder().encodeToString(newPubKey), Base64.getEncoder().encodeToString(encryptPriKey)))
                    .build();
        } catch (Exception e) {
            log.error("generateSM2Key failed, error: ", e);
            throw new DeviceException("生成sm2密钥失败，请检查服务器连接是否正常！");
        } finally {
            if (Objects.nonNull(snasecSession)) {
                snasecSession.destroyHsm();
            }
        }
    }


    @Override
    public GenerateKeyResult generateSM2Key4ProKeyValue(Integer proKeyGlobalAlgTypeCode, String proKeyInfo, List<String> devicePostList) {
        GMT0018SDFSession snasecSession = DeviceInstanceHelper.getOneSansecHSMInstance(devicePostList);
        try {
            // 调用业务密码机，生成一个ECC密钥对
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "GenerateKeyPair_ECC");
            param.put("algorithm", "SM2");
            param.put("strength", 256);
            List<byte[]> result = (List<byte[]>) snasecSession.execute(param);
            log.info("GenerateKeyPair_ECC success, next execute SDF_InternalEncrypt_SM4...");
            byte[] priKey = null, pubKey = null;
            if (result != null && result.size() > 1) {
                priKey = result.get(0);
                pubKey = result.get(1);
            } else {
                throw new DeviceException("生成非对称密钥失败！");
            }
            byte[] newPubKey = new byte[64];
            System.arraycopy(pubKey, 32, newPubKey, 0, 32);
            System.arraycopy(pubKey, 96, newPubKey, 32, 32);

            // 第一步，通过软密钥解密应用kek
            byte[] decryptProKeyInfoBytes = SM4Util.decrypt(DataCenterKeyCache.getDataCenterKey(), proKeyInfo);

            // 第二步、将解密完的明文，导入并保存到业务密码机里去，索引固定用10
            Integer proKeyIndex = 10;
            Integer strength = GlobalAlgLengthEnum.getAlgLength(proKeyGlobalAlgTypeCode);
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put(PARAM_OPERATION, "SWMF_InputKEK");
            inputParam.put(PARAM_KEY_INDEX, proKeyIndex);
            inputParam.put("key", decryptProKeyInfoBytes);
            inputParam.put("strength", strength);
            snasecSession.execute(inputParam);

            // 第三步、使用刚刚导入的应用kek密钥(这样才符合业务逻辑)，调用”业务密码机“，给‘私钥’加密一下子，才能保存到数据库
            param = new HashMap<>();
            param.put("operation", "SDF_InternalEncrypt_SM4");
            param.put("kekIndex", proKeyIndex);
            param.put("data", priKey);
            byte[] encryptPriKey = (byte[]) snasecSession.execute(param);

            // 第四步，直接返回生成的key，三未的不需要删除密钥索引
            return GenerateKeyResult.builder()
                    .keyValue(String.join("&", Base64.getEncoder().encodeToString(newPubKey), Base64.getEncoder().encodeToString(encryptPriKey)))
                    .build();
        } catch (Exception e) {
            log.error("generateSM2Key failed, error: ", e);
            throw new DeviceException("生成sm2密钥失败，请检查服务器连接是否正常！");
        } finally {
            if (Objects.nonNull(snasecSession)) {
                snasecSession.destroyHsm();
            }
        }
    }


    @Override
    public GenerateKeyResult generateAndSaveSM2Key(Integer kekIndex, Integer globalKeyType, int keyIndex, Integer keyUsedType, String keyLabel, List<String> devicePostList) {
        List<GMT0018SDFSession> sessionList = DeviceInstanceHelper.getSansecHSSMInstance(devicePostList);
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "GenerateKeyPair_ECC");
            param.put("algorithm", "SM2");
            param.put("strength", 256);
            List<byte[]> result = (List<byte[]>) sessionList.get(0).execute(param);
            log.info("GenerateKeyPair_ECC success, next execute SDF_InternalEncrypt_SM4...");
            byte[] priKey = null, pubKey = null;
            if (result != null && result.size() > 1) {
                priKey = result.get(0);
                pubKey = result.get(1);
            } else {
                throw new DeviceException("生成非对称密钥失败！");
            }
            byte[] newPubKey = new byte[64];
            System.arraycopy(pubKey, 32, newPubKey, 0, 32);
            System.arraycopy(pubKey, 96, newPubKey, 32, 32);

            // 导入
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put("operation", "SWCSM_InputECCKeyPair");
            inputParam.put("privateKey", priKey);
            inputParam.put("publicKey", pubKey);
            inputParam.put("keyIndex", keyIndex);
            if (GlobalUsedTypeCodeConstant.KEY_USAGE_CODE_SIGN.equals(keyUsedType)) {
                inputParam.put("keyIndexType", 1);
            }
            sessionList.forEach(session -> session.execute(inputParam));
            log.info("SWCSM_InputECCKeyPair success...");

            // 给私钥用”业务密码机“加密一下再返回保存到数据库
            param = new HashMap<>();
            param.put("operation", "SDF_InternalEncrypt_SM4");
            param.put("kekIndex", kekIndex);
            param.put("data", priKey);
            byte[] encryPriKey = (byte[]) sessionList.get(0).execute(param);
            log.info("SDF_InternalEncrypt_SM4 success...");

            // 转换组装
            return GenerateKeyResult.builder()
                    .keyValue(String.join("&", Base64.getEncoder().encodeToString(newPubKey),
                            Base64.getEncoder().encodeToString(encryPriKey)))
                    .build();
        } catch (Exception e) {
            log.error("generateAndSaveSM2Key failed, error:{}", e.getMessage());
            e.printStackTrace();
            throw new DeviceException("生成非对称密钥失败，请检查服务器连接是否正常！");
        } finally {
            sessionList.forEach(GMT0018SDFSession::destroyHsm);
        }
    }

    @Override
    public GenerateKeyResult generateAndSaveSymmetricKey4ProKeyIndex(Integer globalKeyType, Integer proKeyIndex, Integer proKeyGlobalKeyType, Integer keyIndex, String keyLabel, String destKeyIV, int encDerivedAlg, List<String> devicePostList) {
        List<GMT0018SDFSession> sessionList = DeviceInstanceHelper.getSansecHSSMInstance(devicePostList);
        try {
            // 长度
            Integer strength = GlobalAlgLengthEnum.getAlgLength(globalKeyType);
            Map<String, Object> param = new HashMap<>();
            param.put("operation", "SDF_GenerateKeyWithKEK");
            param.put("kekIndex", proKeyIndex);
            param.put("strength", strength);
            byte[] data = (byte[]) sessionList.get(0).execute(param);
            log.info("SDF_GenerateKeyWithKEK success, next execute SDF_DeDEK...");
            // 基于Kek生成一个密钥，然后再导入进密码机
            if (ArrayUtils.isEmpty(data)) {
                throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
            }
            // 生成的密钥是密文的，导入需要的是明文，所以此处需要先解密。
            param = new HashMap<>();
            param.put("operation", "SDF_DeDEK");
            param.put("kekIndex", proKeyIndex);
            param.put("key", data);
            byte[] encryData = (byte[]) sessionList.get(0).execute(param);
            log.info("SDF_DeDEK success, next execute SWMF_InputKEK...");
            // 长度
            Map<String, Object> inputParam = new HashMap<>();
            inputParam.put("operation", "SWMF_InputKEK");
            inputParam.put("keyIndex", keyIndex);
            inputParam.put("key", encryData);
            inputParam.put("strength", strength);
            sessionList.forEach(session -> session.execute(inputParam));
            log.info("SWMF_InputKEK success...");
            return GenerateKeyResult.builder()
                    .keyValue(Base64.getEncoder().encodeToString(data))
                    .build();
        } catch (Exception e) {
            log.error("generateAndSaveSymmetricKey4ProKeyIndex failed, error: {}", e.getMessage());
            e.printStackTrace();
            throw new DeviceException("生成密钥失败，请检查服务器连接是否正常！");
        } finally {
            sessionList.forEach(GMT0018SDFSession::destroyHsm);
        }
    }

}
