package com.csp.actuator.device.session;


import com.csp.actuator.device.bean.HsmDeviceDTO;
import com.csp.actuator.device.bean.PointerWrapper;
import com.csp.actuator.device.enums.GlobalUsedTypeCodeEnum;
import com.csp.actuator.device.exception.DeviceException;
import com.csp.actuator.utils.EncodeConvertUtil;
import com.csp.actuator.utils.Padding;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.*;

import static com.csp.actuator.device.contants.HSMConstant.VENUS_DEVICE_PASSWORD;
import static com.csp.actuator.device.contants.HSMConstant.VENUS_DLL_NAME_LIST;
import static com.csp.actuator.device.contants.HsmFunctionConstant.*;


/**
 * @Description 启明自研平台密码机会话实现
 * @Author panlin.li
 * @Date 2023/8/11 10:46
 */
@Slf4j
@Component
public class VenusHsmSession extends AbstractHsmSession {

    public PointerByReference phDeviceHandle = new PointerByReference();

    public PointerByReference phSessionHandle = new PointerByReference();

    private SDFV2Api sdfV2Api;

    public Pointer deviceHandle;

    public Pointer sessionHandle;

    @Override
    public HsmSession instance(HsmDeviceDTO hsm) {
        sdfV2Api = new SDFV2Api();
        int result = 1;
        if (hsm.getDeviceHandle() == null) {
            sdfV2Api.overrideIniConfigFile(hsm.getIp(), hsm.getPort().toString(), VENUS_DLL_NAME_LIST, VENUS_DEVICE_PASSWORD, hsm.getIsEnableSslFlag());
            // 初始化会话池
            result = sdfV2Api.SDF_OpenDevice(phDeviceHandle);
            if (result != 0) {
                log.error("open device is fail! result: {}", result);
                throw new DeviceException("连接设备失败，请检查设备是否正常！");
            }
            deviceHandle = phDeviceHandle.getValue();
            hsm.setDeviceHandle(deviceHandle);
        } else {
            sdfV2Api.overrideIniConfigFile(hsm.getIp(), hsm.getPort().toString(), VENUS_DLL_NAME_LIST, VENUS_DEVICE_PASSWORD, hsm.getIsEnableSslFlag());
            this.deviceHandle = hsm.getDeviceHandle();
        }
        // 创建会话
        result = sdfV2Api.SDF_OpenSession(deviceHandle, phSessionHandle);
        if (result != 0) {
            log.error("open device session is fail!");
            throw new DeviceException("建立设备会话失败，请检查设备是否正常！");
        }
        sessionHandle = phSessionHandle.getValue();
        return this;
    }

    @Override
    public Object execute(Map<String, Object> param) throws DeviceException {
        String operation = param.get("operation").toString();
        switch (operation) {
            case generateKeyIndex:
                return generateKeyIndex(param);
            case SDF_GenerateKeyWithKEK:
                return generateKekWithKEK(param);
            case SDIF_ImportKeyAndSave:
                return SDIF_ImportKeyAndSave(param);
            case SDIF_ImportKeyPair_ECC:
                return SDIF_ImportKeyPair_ECC(param);
            case SWCSM_GenerateECCKeyPair:
                return swcsmGenerateECCKeyPair(param);
            case GenerateRandom:
                return generateRandom(param);
            case SDF_ExportSignPublicKey_ECC:
                return exportSignPublicKeyECC(param);
            case GenerateKeyPair_ECC:
                return generateKeyPairECC(param);
            case SDF_Encrypt:
                return sdfEncrypt(param);
            case SDF_Decrypt:
                return sdfDecrypt(param);
            case SDF_DeDEK:
                return sdfDeDEK(param);
            case SDF_InternalEncrypt_SM4:
                return internalEncryptSM4(param);
            case SDF_InternalDecrypt_SM4:
                return internalDecryptSM4(param);
            case ExternalEncrypt_ECC:
                return externalEncryptECC(param);
            case ExternalDecrypt_ECC:
                return externalDecryptECC(param);
            case SDF_ExternalSign_ECC:
                return sdfExternalSignECC(param);
            case SDF_InternalSign_ECC:
                return sdfInternalSignECC(param);
            case SDF_ExternalVerify_ECC:
                return sdfExternalVerifyECC(param);
            case SDF_InternalVerify_ECC:
                return sdfInternalVerifyECC(param);
            case SDF_Hash:
                return sdfHash(param);
            case SDIF_DeleteKey:
                return deleteKey(param);
            case SDIF_ListKey:
                return listKey(param);
            default:
                break;
        }
        return null;
    }

    /**
     * 往密码机某个索引位生成对称密钥
     *
     * @param param
     * @return
     */
    private Object generateKeyIndex(Map<String, Object> param) {
        Integer keyIndex = (Integer) param.get(PARAM_KEY_INDEX);
        Integer strength = (Integer) param.get(PARAM_STRENGTH);
        byte[] randomBytes = new byte[strength / 8];
        new SecureRandom().nextBytes(randomBytes);
        //1.删除索引位密钥
        param.put(PARAM_KEY_TYPE, 1);
        param.put(PARAM_KEY_USAGE, 0);
        this.deleteKey(param);
        //2.在索引位导入对称密钥
        Map<String, Object> inputParam = new HashMap<>();
        inputParam.put(PARAM_KEY_INDEX, keyIndex);
        inputParam.put("key", randomBytes);
        inputParam.put(PARAM_STRENGTH, strength);
        inputParam.put("algType", SDFV2Api.SGD_SM4_ECB);
        return this.SDIF_ImportKeyAndSave(inputParam);
    }

    private Object sdfDeDEK(Map<String, Object> param) {
        Integer keyIndex = (Integer) param.get("kekIndex");
        byte[] dek = (byte[]) param.get("key");
        byte[] decryptData = new byte[dek.length];
        IntByReference puiDecDataLength = new IntByReference(dek.length);
        // SM4-ECB模式
        int algrithmId = SDFV2Api.SGD_SM4_ECB;
        if (param.containsKey("algorithm")) {
            algrithmId = (Integer) param.get("algorithm");
        }
        byte[] iv = new byte[16];
        sdfV2Api.SDF_InternalDecrypt(sessionHandle, keyIndex, algrithmId, iv, dek, dek.length, decryptData, puiDecDataLength);
        return decryptData;
    }

    /**
     * 内部密钥SM4加密
     *
     * @param param
     * @return
     */
    private Object internalEncryptSM4(Map<String, Object> param) {
        byte[] data = (byte[]) param.get("data");
        // 此函数不对数据进行填充处理，输入数据必须是指定算法分组长度的整数倍
        byte[] dataByte = Padding.PKCS5Padding(data, 16);
        // 平台密码机的kekIndex固定是1，所以不传这个参数的时候，那就默认是1
        int index = Integer.parseInt(param.get("kekIndex") == null ? "1" : param.get("kekIndex").toString());

        PointerByReference phKeyHandle = new PointerByReference();
        int rest = sdfV2Api.SDF_GetSymmKeyHandle(sessionHandle, index, phKeyHandle);
        if (rest != 0) {
            throw new DeviceException("导入内部密钥对称密钥失败，请确认密码机索引1位置是否生成SM4算法密钥！");
        }
        Pointer keyHandle = phKeyHandle.getValue();
        int algrithmId = SDFV2Api.SGD_SM4_CBC;
        byte[] iv = new byte[16];
        byte[] encData = new byte[dataByte.length];
        IntByReference puiEncDataLength = new IntByReference(encData.length);
        int encrypt = sdfV2Api.SDF_Encrypt(sessionHandle, keyHandle, algrithmId, iv, dataByte, dataByte.length, encData, puiEncDataLength);
        if (encrypt != 0) {
            log.error("encrypt result: {}", encrypt);
            throw new DeviceException("加密密钥失败，请检查加密机是否连接正确！");
        }
        // 释放密钥句柄
        sdfV2Api.SDF_DestroyKey(sessionHandle, keyHandle);
        return encData;
    }

    /**
     * 内部密钥SM4解密
     *
     * @param param
     * @return
     */
    private Object internalDecryptSM4(Map<String, Object> param) {
        byte[] data = (byte[]) param.get("data");
        // 平台密码机的kekIndex固定是1，所以不传这个参数的时候，那就默认是1
        int index = Integer.parseInt(param.get("kekIndex") == null ? "1" : param.get("kekIndex").toString());

        PointerByReference phKeyHandle = new PointerByReference();
        int rest = sdfV2Api.SDF_GetSymmKeyHandle(sessionHandle, index, phKeyHandle);
        if (rest != 0) {
            throw new DeviceException("导入内部密钥对称密钥失败，请确认密码机索引1位置是否生成SM4算法密钥！");
        }
        Pointer keyHandle = phKeyHandle.getValue();
        byte[] decryptData = new byte[data.length];
        IntByReference puiDecDataLength = new IntByReference(decryptData.length);
        int algrithmId = SDFV2Api.SGD_SM4_CBC;
        byte[] iv = new byte[16];
        int decrypt = sdfV2Api.SDF_Decrypt(sessionHandle, keyHandle, algrithmId, iv, data, data.length, decryptData, puiDecDataLength);
        if (decrypt != 0) {
            throw new DeviceException("解密失败，请检查加密机是否连接正确！");
        }
        // 释放密钥句柄
        sdfV2Api.SDF_DestroyKey(sessionHandle, keyHandle);
        return Padding.PKCS5UnPadding(decryptData, 16);
    }

    /**
     * 生成kek密钥信息
     *
     * @return
     */
    private Object generateKekWithKEK(Map<String, Object> param) throws DeviceException {
        log.info("generateKekWithKEK param= {}", param);

        int lgrithmId = SDFV2Api.SGD_SM4_ECB;
        Integer kekIndex = (Integer) param.get("kekIndex");
        int strength = Integer.parseInt(param.get("strength").toString());
        PointerWrapper pucKey = new PointerWrapper(32);
        IntByReference encKeyDataLength = new IntByReference(32);
        PointerByReference phKeyHandle = new PointerByReference(Pointer.NULL);
        int result = sdfV2Api.SDF_GenerateKeyWithKEK(sessionHandle, strength, lgrithmId, kekIndex, pucKey.getPointer(), encKeyDataLength, phKeyHandle);
        if (result != 0) {
            throw new DeviceException("生成kek密钥失败，请检查服务器连接是否正常！");
        }
        return pucKey.getPointer().getByteArray(0L, encKeyDataLength.getValue());
    }


    /**
     * 输入对称密钥
     *
     * @return
     */
    private Object SDIF_ImportKeyAndSave(Map<String, Object> param) throws DeviceException {
        log.info("Import key and save param= {}", param);

        byte[] key = (byte[]) param.get("key");
        int keyIndex = Integer.parseInt(param.get("keyIndex").toString());
        int algType = Integer.parseInt(param.get("algType").toString());
        int strength = Integer.parseInt(param.get("strength").toString());
        int result = sdfV2Api.SDIF_ImportKeyAndSave(sessionHandle, algType, key, strength / 8, keyIndex);
        if (result != 0) {
            throw new DeviceException("生成并导入密钥失败，请检查服务器连接是否正常！");
        }
        return true;
    }

    /**
     * 输入非对称密钥
     *
     * @return
     */
    private Object SDIF_ImportKeyPair_ECC(Map<String, Object> param) throws DeviceException {
        log.info("SDIF Import key pair param = {}", param);

        Integer keyIndex = (Integer) param.get("keyIndex");
        Integer keyUsedType = (Integer) param.get("keyUsedType");
        byte[] pubKeyByte = (byte[]) param.get("publicKey");
        byte[] priKeyByte = (byte[]) param.get("privateKey");

        int uiAlgId;
        if (keyUsedType.equals(GlobalUsedTypeCodeEnum.VENUS_KEY_USAGE_CODE_ENCRYPTION.getVendorUsedTypeCode())) {
            uiAlgId = SDFV2Api.SGD_SM2; // 加密
        } else {
            uiAlgId = SDFV2Api.SGD_SM2_1; // 签名
        }
        log.info("SDIF Import key pair uiAlgId = {}", uiAlgId);

        // 公钥对象
        byte[] X = new byte[pubKeyByte.length / 2];
        byte[] Y = new byte[pubKeyByte.length / 2];
        System.arraycopy(pubKeyByte, 0, X, 0, X.length);
        System.arraycopy(pubKeyByte, X.length, Y, 0, Y.length);
        SDFV2Api.CLibrary.ECCrefPublicKey.ByReference pucPublicKey = new SDFV2Api.CLibrary.ECCrefPublicKey.ByReference();
        pucPublicKey.setBits(256);
        pucPublicKey.setX(X);
        pucPublicKey.setY(Y);

        SDFV2Api.CLibrary.ECCrefPrivateKey.ByReference pucPrivateKey = new SDFV2Api.CLibrary.ECCrefPrivateKey.ByReference();
        pucPrivateKey.setBits(256);
        pucPrivateKey.setK(priKeyByte);

        int gensult = sdfV2Api.SDIF_ImportKeyPair_ECC(sessionHandle, keyIndex, uiAlgId, pucPublicKey, pucPrivateKey);
        if (gensult != 0) {
            throw new DeviceException("导入ECC密钥失败，请检查服务器连接是否正常！");
        }
        return true;
    }

    /**
     * 生成非对称密钥并保存到索引
     *
     * @return
     */
    private Object swcsmGenerateECCKeyPair(Map<String, Object> param) throws DeviceException {
        Integer keyIndex = (Integer) param.get("keyIndex");
        int gensult = sdfV2Api.SWCSM_GenerateECCKeyPair(sessionHandle, keyIndex * 2);
        if (gensult != 0) {
            throw new DeviceException("生成ECC密钥失败，请检查服务器连接是否正常！");
        }
        return true;
    }

    /**
     * 生成随机数
     *
     * @return
     */
    private Object generateRandom(Map<String, Object> param) throws DeviceException {
        int strength = Integer.parseInt(param.get("strength").toString());
        byte[] data = new byte[strength];
        int result = sdfV2Api.SDF_GenerateRandom(sessionHandle, data.length, data);
        if (result != 0) {
            throw new DeviceException("生成随机数失败，请检查服务器连接是否正常！");
        }
        return data;
    }

    /**
     * 导入内部ECC签名公钥
     *
     * @param param
     * @return
     */
    private Object exportSignPublicKeyECC(Map<String, Object> param) {
        int indexNumber = (int) param.get("indexNumber");
        SDFV2Api.CLibrary.SM2refPublicKey.ByReference pucPublicKeyEcc = new SDFV2Api.CLibrary.SM2refPublicKey.ByReference();
        sdfV2Api.SDF_ExportSignPublicKey_ECC(sessionHandle, indexNumber, pucPublicKeyEcc);
        return pucPublicKeyEcc.encode();
    }

    /**
     * 生成非对称密钥 </br>
     *
     * @param param
     * @return
     * @throws DeviceException </br>
     */
    private Object generateKeyPairECC(Map<String, Object> param) throws DeviceException {
        String algorithm = param.get("algorithm").toString();
        int bit = Integer.parseInt(param.get("strength").toString());
        int genAsym = 1;
        byte[] priKey;
        byte[] pubKey;

        SDFV2Api.CLibrary.ECCrefPublicKey.ByReference pucPublicKeyEcc = new SDFV2Api.CLibrary.ECCrefPublicKey.ByReference();
        SDFV2Api.CLibrary.ECCrefPrivateKey.ByReference pucPrivateKeyEcc = new SDFV2Api.CLibrary.ECCrefPrivateKey.ByReference();

        SDFV2Api.CLibrary.RSArefPublicKey.ByReference pucPublicKeyRsa = new SDFV2Api.CLibrary.RSArefPublicKey.ByReference();
        SDFV2Api.CLibrary.RSArefPrivateKey.ByReference pucPrivateKeyRsa = new SDFV2Api.CLibrary.RSArefPrivateKey.ByReference();
        if ("RSA".equals(algorithm)) {
            genAsym = sdfV2Api.SDF_GenerateKeyPair_RSA(sessionHandle, bit, pucPublicKeyRsa, pucPrivateKeyRsa);

            pubKey = EncodeConvertUtil.mergeByteArray(pucPublicKeyRsa.getM(), pucPublicKeyRsa.getE());

            priKey = EncodeConvertUtil.mergeByteArray(pucPrivateKeyRsa.getM(), pucPrivateKeyRsa.getE(),
                    pucPrivateKeyRsa.getD(), pucPrivateKeyRsa.getPrime(), pucPrivateKeyRsa.getPexp(),
                    pucPrivateKeyRsa.coef);
        } else if ("SM2".equals(algorithm) || "SM9".equals(algorithm)) {
            genAsym = sdfV2Api.SDF_GenerateKeyPair_ECC(sessionHandle, SDFV2Api.SGD_SM2_1, bit, pucPublicKeyEcc, pucPrivateKeyEcc);

            pubKey = EncodeConvertUtil.mergeByteArray(pucPublicKeyEcc.getX(), pucPublicKeyEcc.getY());

            priKey = pucPrivateKeyEcc.getK();
        } else {
            throw new DeviceException("不支持该算法！");
        }
        if (genAsym != 0) {
            throw new DeviceException("生成非对称密钥失败，请检查服务器连接是否正常！");
        }
        List<byte[]> list = new ArrayList<>();
        list.add(priKey);
        list.add(pubKey);
        return list;
    }

    /**
     * 对称密钥加密 </br>
     *
     * @param param
     * @return
     * @throws DeviceException </br>
     */
    private Object sdfEncrypt(Map<String, Object> param) throws DeviceException {
        Integer keyIndex = (Integer) param.get("kekIndex");
        byte[] data = (byte[]) param.get("data");
        String algorithm = param.get("algorithm").toString();
        byte[] dataByte = Padding.PKCS5Padding(data, 16);
        byte[] encData = new byte[dataByte.length];
        IntByReference puiEncDataLength = new IntByReference(encData.length);
        byte[] key = (byte[]) param.get("key");
        byte[] iv = new byte[key.length];
        int algrithmId = 1;
        PointerByReference phKeyHandle = new PointerByReference();
        switch (algorithm) {
            case "DES":
                algrithmId = SDFV2Api.SGD_DES_CBC;
                break;
            case "3DES":
                algrithmId = SDFV2Api.SGD_3DES_CBC;
                break;
            case "AES":
                algrithmId = SDFV2Api.SGD_AES_CBC;
                break;
            case "SM1":
                algrithmId = SDFV2Api.SGD_SM1_CBC;
                break;
            case "SM4":
                algrithmId = SDFV2Api.SGD_SM4_ECB;
                //kek解密
                int val = sdfV2Api.SDF_ImportKeyWithKEK(sessionHandle, algrithmId, keyIndex, key, 32, phKeyHandle);
                if (val != 0) {
                    throw new DeviceException("解密kek失败，请检查加密机是否连接正确！");
                }
                break;
            case "ZUC":
                algrithmId = SDFV2Api.SGD_SM4_CBC;
                break;
            case "SM7":
                algrithmId = SDFV2Api.SGD_SM7_CBC;
                break;
            default:
                break;
        }
        // 密钥句柄
        Pointer keyHandle = phKeyHandle.getValue();
        int encrypt = sdfV2Api.SDF_Encrypt(sessionHandle, keyHandle, algrithmId, iv, dataByte, dataByte.length, encData, puiEncDataLength);
        if (encrypt != 0) {
            log.error("encrypt result: {}", encrypt);
            throw new DeviceException("加密密钥失败，请检查加密机是否连接正确！");
        }
        // 释放密钥句柄
        sdfV2Api.SDF_DestroyKey(sessionHandle, keyHandle);
        return encData;
    }

    /**
     * 对称密钥解密 </br>
     *
     * @param param
     * @return
     * @throws DeviceException </br>
     */
    private Object sdfDecrypt(Map<String, Object> param) throws DeviceException {
        Integer keyIndex = (Integer) param.get("kekIndex");
        byte[] data = (byte[]) param.get("data");
        String algorithm = param.get("algorithm").toString();
        byte[] decryptData = new byte[data.length];
        IntByReference puiDecDataLength = new IntByReference(decryptData.length);
        byte[] key = (byte[]) param.get("key");
        byte[] iv = new byte[key.length];
        int algrithmId = 1;
        PointerByReference phKeyHandle = new PointerByReference();
        switch (algorithm) {
            case "DES":
                algrithmId = SDFV2Api.SGD_DES_CBC;
                break;
            case "3DES":
                algrithmId = SDFV2Api.SGD_3DES_CBC;
                break;
            case "AES":
                algrithmId = SDFV2Api.SGD_AES_CBC;
                break;
            case "SM1":
                algrithmId = SDFV2Api.SGD_SM1_CBC;
                break;
            case "SM4":
                algrithmId = SDFV2Api.SGD_SM4_ECB;
                //kek解密
                int val = sdfV2Api.SDF_ImportKeyWithKEK(sessionHandle, algrithmId, keyIndex, key, 32, phKeyHandle);
                if (val != 0) {
                    throw new DeviceException("解密kek失败，请检查加密机是否连接正确！");
                }
                break;
            case "ZUC":
                algrithmId = SDFV2Api.SGD_SM4_CBC;
                break;
            case "SM7":
                algrithmId = SDFV2Api.SGD_SM7_CBC;
                break;
            default:
                break;
        }
        // 密钥句柄
        Pointer keyHandle = phKeyHandle.getValue();
        int decrypt = sdfV2Api.SDF_Decrypt(sessionHandle, keyHandle, algrithmId, iv, data, data.length, decryptData, puiDecDataLength);
        if (decrypt != 0) {
            throw new DeviceException("解密失败，请检查加密机是否连接正确！");
        }
        // 释放密钥句柄
        sdfV2Api.SDF_DestroyKey(sessionHandle, keyHandle);
        return Padding.PKCS5UnPadding(decryptData, 16);
    }

    /**
     * 外部ECC密钥加密 </br>
     *
     * @param param
     * @return
     * @throws DeviceException </br>
     */
    private Object externalEncryptECC(Map<String, Object> param) throws DeviceException {
        byte[] key = (byte[]) param.get("key");
        byte[] data = (byte[]) param.get("data");
        int strength = (int) param.get("strength");
        String algorithm = param.get("algorithm").toString();
        int encrypt = 1;
        SDFV2Api.CLibrary.ECCCipher.ByReference pucEncDataEcc = new SDFV2Api.CLibrary.ECCCipher.ByReference();
        byte[] M = new byte[key.length / 2];
        byte[] E = new byte[key.length / 2];
        System.arraycopy(key, 0, M, 0, M.length);
        System.arraycopy(key, M.length, E, 0, E.length);
        if ("RSA".equals(algorithm)) {
            // RSA算法，加密字符串必须是2048/8=256字节，不够长度需要填充，填充“空格”
            byte[] dataByte = new byte[strength / 8];
            if (data.length > strength / 8) {
                throw new DeviceException("加密数据过长，请从新输入！");
            }
            System.arraycopy(data, 0, dataByte, 0, data.length);
            for (int i = data.length; i < dataByte.length; i++) {
                dataByte[i] = 32;
            }
            byte[] pucDataOutput = new byte[strength / 8];
            SDFV2Api.CLibrary.RSArefPublicKey.ByReference pucPublicKeyRsa = new SDFV2Api.CLibrary.RSArefPublicKey.ByReference();
            pucPublicKeyRsa.setBits(strength);
            pucPublicKeyRsa.setM(M);
            pucPublicKeyRsa.setE(E);
            IntByReference puiOutputLength = new IntByReference();
            encrypt = sdfV2Api.SDF_ExternalPublicKeyOperation_RSA(sessionHandle, pucPublicKeyRsa, dataByte, dataByte.length, pucDataOutput, puiOutputLength);
            if (encrypt != 0) {
                throw new DeviceException("非对称密钥加密失败，请检查服务器连接是否正常！");
            }
            return pucDataOutput;
        } else if ("SM2".equals(algorithm) || "SM9".equals(algorithm)) {
            SDFV2Api.CLibrary.ECCrefPublicKey.ByReference pucPublicKeyEcc = new SDFV2Api.CLibrary.ECCrefPublicKey.ByReference();
            pucPublicKeyEcc.setBits(strength);
            pucPublicKeyEcc.setX(M);
            pucPublicKeyEcc.setY(E);
            encrypt = sdfV2Api.SDF_ExternalEncrypt_ECC(sessionHandle, SDFV2Api.SGD_SM2_3, pucPublicKeyEcc, data, data.length, pucEncDataEcc);
        } else {
            throw new DeviceException("不支持该算法！");
        }
        if (encrypt != 0) {
            throw new DeviceException("非对称密钥加密失败，请检查服务器连接是否正常！");
        }
        return pucEncDataEcc.getCipher();
    }

    /**
     * 外部ECC密钥解密 </br>
     *
     * @param param
     * @return
     * @throws DeviceException </br>
     */
    private Object externalDecryptECC(Map<String, Object> param) throws DeviceException {
        byte[] key = (byte[]) param.get("key");
        byte[] data = (byte[]) param.get("data");
        int strength = (int) param.get("strength");
        String algorithm = param.get("algorithm").toString();
        byte[] pucData = new byte[data.length];
        int decrypt = 1;
        byte[] pucDecData = new byte[pucData.length];
        IntByReference puiDecDataLength = new IntByReference(pucDecData.length);
        if ("RSA".equals(algorithm)) {
            SDFV2Api.CLibrary.RSArefPrivateKey.ByReference pucPrivateKeyRsa = new SDFV2Api.CLibrary.RSArefPrivateKey.ByReference();
            pucPrivateKeyRsa.setBits(strength);
            byte[] m = new byte[512];
            byte[] e = new byte[512];
            byte[] d = new byte[512];
            byte[] prime = new byte[2 * 256];
            byte[] pexp = new byte[2 * 256];
            byte[] coef = new byte[256];
            System.arraycopy(key, 0, m, 0, m.length);
            System.arraycopy(key, 0 + m.length, e, 0, e.length);
            System.arraycopy(key, 0 + m.length + e.length, d, 0, d.length);
            System.arraycopy(key, 0 + m.length + e.length + d.length, prime, 0, prime.length);
            System.arraycopy(key, 0 + m.length + e.length + d.length + prime.length, pexp, 0, pexp.length);
            System.arraycopy(key, 0 + m.length + e.length + d.length + prime.length + pexp.length, coef, 0, coef.length);
            pucPrivateKeyRsa.setM(m);
            pucPrivateKeyRsa.setE(e);
            pucPrivateKeyRsa.setD(d);
            pucPrivateKeyRsa.setPrime(prime);
            pucPrivateKeyRsa.setPexp(pexp);
            pucPrivateKeyRsa.setCoef(coef);
            decrypt = sdfV2Api.SDF_ExternalPrivateKeyOperation_RSA(sessionHandle, pucPrivateKeyRsa, data, data.length, pucDecData, puiDecDataLength);
        } else if ("SM2".equals(algorithm) || "SM9".equals(algorithm)) {
            SDFV2Api.CLibrary.ECCCipher.ByReference pucEncDataEcc = new SDFV2Api.CLibrary.ECCCipher.ByReference();
            byte[] x = new byte[64];
            byte[] y = new byte[64];
            byte[] M = new byte[32];
            byte[] l = new byte[32];
            byte[] C = new byte[data.length - 64 - 64 - 32 - 4];
            System.arraycopy(data, 0, x, 0, 64);
            System.arraycopy(data, 64, y, 0, 64);
            System.arraycopy(data, 64 + 64, M, 0, 32);
            System.arraycopy(data, 64 + 64 + 32, l, 0, 4);
            System.arraycopy(data, 64 + 64 + 32 + 4, C, 0, C.length);
            int L = EncodeConvertUtil.bytes2Int(l);
            pucEncDataEcc.setX(x);
            pucEncDataEcc.setY(y);
            pucEncDataEcc.setM(M);
            pucEncDataEcc.setL(L);
            pucEncDataEcc.setC(C);
            SDFV2Api.CLibrary.ECCrefPrivateKey.ByReference pucPrivateKeyEcc = new SDFV2Api.CLibrary.ECCrefPrivateKey.ByReference();
            pucPrivateKeyEcc.setBits(strength);
            pucPrivateKeyEcc.setK(key);
            decrypt = sdfV2Api.SDF_ExternalDecrypt_ECC(sessionHandle, SDFV2Api.SGD_SM2_3, pucPrivateKeyEcc, pucEncDataEcc, pucDecData, puiDecDataLength);
        } else {
            throw new DeviceException("不支持该算法！");
        }
        if (decrypt != 0) {
            throw new DeviceException("非对称密钥解密失败，请检查服务器连接是否正常！");
        }
        return pucDecData;
    }

    /**
     * 外部密钥数字签名
     *
     * @param param
     * @return
     */
    private Object sdfExternalSignECC(Map<String, Object> param) {
        byte[] key = (byte[]) param.get("key");
        byte[] data = (byte[]) param.get("data");
        String algorithm = param.get("algorithm").toString();
        if ("RSA".equals(algorithm)) {
            return this.externalEncryptECC(param);
        }
        int bits = (int) param.get("strength");
        // 先对数据做hash运算，再做签名
        Map<String, Object> map = new HashMap<>();
        map.put("data", data);
        map.put("algorithm", "SM3");
        data = (byte[]) sdfHash(map);
        // 签名算法，默认SM2
        int uiAlgID = SDFV2Api.SGD_SM2_1;
        byte[] K = new byte[bits / 4];
        System.arraycopy(key, 0, K, 0, K.length);
        SDFV2Api.CLibrary.ECCrefPrivateKey.ByReference pucPrivateKey = new SDFV2Api.CLibrary.ECCrefPrivateKey.ByReference();
        pucPrivateKey.setBits(bits);
        pucPrivateKey.setK(K);
        SDFV2Api.CLibrary.ECCSignature.ByReference pucSignature = new SDFV2Api.CLibrary.ECCSignature.ByReference();
        int sign = sdfV2Api.SDF_ExternalSign_ECC(sessionHandle, uiAlgID, pucPrivateKey, data, data.length, pucSignature);
        if (sign != 0) {
            throw new DeviceException("数字签名失败，请检查服务器连接是否正常！");
        }
        return EncodeConvertUtil.mergeByteArray(pucSignature.getR(), pucSignature.getS());
    }

    /**
     * 内部密钥数字签名
     *
     * @param param
     * @return
     */
    private Object sdfInternalSignECC(Map<String, Object> param) {
        int indexNumber = (int) param.get("indexNumber");
        // 先对数据做hash运算，再做签名
        param.put("algorithm", "SM3");
        byte[] data = (byte[]) sdfHash(param);
        SDFV2Api.CLibrary.SM2refSignature.ByReference pucSignature = new SDFV2Api.CLibrary.SM2refSignature.ByReference();
        sdfV2Api.SDF_InternalSign_ECC(sessionHandle, indexNumber, data, data.length, pucSignature);
        return pucSignature.encode();
    }

    /**
     * 外部密钥数字验签
     *
     * @param param
     * @return
     */
    private Object sdfExternalVerifyECC(Map<String, Object> param) {
        byte[] key = (byte[]) param.get("key");
        byte[] data = (byte[]) param.get("data");
        byte[] sign = (byte[]) param.get("sign");
        int bits = (int) param.get("strength");
        String algorithm = param.get("algorithm").toString();
        if ("RSA".equals(algorithm)) {
            String rsaData = (new String(data)).trim();
            param.put("data", param.get("sign"));
            String signVerifyData = (new String((byte[]) externalDecryptECC(param))).trim();
            return rsaData.equals(signVerifyData);
        }
        // 先对数据做hash运算，再做验签
        Map<String, Object> map = new HashMap<>();
        map.put("data", data);
        map.put("algorithm", "SM3");
        data = (byte[]) sdfHash(map);
        // 签名算法，默认SM2
        int uiAlgID = SDFV2Api.SGD_SM2_1;
        // 公钥对象
        byte[] X = new byte[key.length / 2];
        byte[] Y = new byte[key.length / 2];
        System.arraycopy(key, 0, X, 0, X.length);
        System.arraycopy(key, X.length, Y, 0, Y.length);
        SDFV2Api.CLibrary.ECCrefPublicKey.ByReference pucPublicKey = new SDFV2Api.CLibrary.ECCrefPublicKey.ByReference();
        pucPublicKey.setBits(bits);
        pucPublicKey.setX(X);
        pucPublicKey.setY(Y);

        // 签名值对象
        byte[] R = new byte[sign.length / 2];
        byte[] S = new byte[sign.length / 2];
        System.arraycopy(sign, 0, R, 0, R.length);
        System.arraycopy(sign, R.length, S, 0, S.length);
        SDFV2Api.CLibrary.ECCSignature.ByReference pucSignature = new SDFV2Api.CLibrary.ECCSignature.ByReference();
        pucSignature.setR(R);
        pucSignature.setS(S);
        // 验签
        int rc = sdfV2Api.SDF_ExternalVerify_ECC(sessionHandle, uiAlgID, pucPublicKey, data, data.length, pucSignature);
        return (rc == 0 ? true : false);
    }

    /**
     * 内部密钥数字验签
     *
     * @param param
     * @return
     */
    private Object sdfInternalVerifyECC(Map<String, Object> param) {
        int indexNumber = (int) param.get("indexNumber");
        byte[] data = (byte[]) param.get("data");
        byte[] sign = (byte[]) param.get("sign");
        // 先对数据做hash运算，再做验签
//        Map<String, Object> map = new HashMap<>();
//        map.put("data", data);
        param.put("algorithm", "SM3");
        data = (byte[]) sdfHash(param);
        SDFV2Api.CLibrary.SM2refSignature.ByReference pucSignature = new SDFV2Api.CLibrary.SM2refSignature.ByReference();
        pucSignature.decode(sign);
        // 验签
        int code = sdfV2Api.SDF_InternalVerify_ECC(sessionHandle, indexNumber, data, data.length, pucSignature);
        return code == 0;
    }

    /**
     * 摘要运算
     *
     * @return
     */
    private Object sdfSm3Hash(Map<String, Object> param) {
        byte[] data = (byte[]) param.get("data");
        int indexNumber = (int) param.get("indexNumber");
        int uiAlgID = SDFV2Api.SGD_SM3;
        // 获取公钥
        byte[] pucHash = new byte[32];
        SDFV2Api.CLibrary.SM2refPublicKey.ByReference pucPublicKeyEcc = new SDFV2Api.CLibrary.SM2refPublicKey.ByReference();
        sdfV2Api.SDF_ExportSignPublicKey_ECC(sessionHandle, indexNumber, pucPublicKeyEcc);
        byte[] userId = "1234567812345678".getBytes();
        IntByReference puiHashLength = new IntByReference(pucHash.length);
        sdfV2Api.SDF_HashInit(sessionHandle, uiAlgID, pucPublicKeyEcc, userId, userId.length);
        sdfV2Api.SDF_HashUpdate(sessionHandle, data, data.length);
        int hashLength = 64;
        PointerWrapper wrapper = new PointerWrapper(hashLength);
        IntByReference pucDataOutputLength = new IntByReference(hashLength);
        sdfV2Api.SDF_HashFinal(sessionHandle, wrapper.getPointer(), pucDataOutputLength);
        return wrapper.getPointer().getByteArray(0L, pucDataOutputLength.getValue());
    }


    /**
     * 摘要运算
     *
     * @param param
     * @return
     */
    private Object sdfHash(Map<String, Object> param) {
        byte[] data = (byte[]) param.get("data");
        String algorithm = param.get("algorithm").toString();
        // 算法标识，默认SM3
        int uiAlgID = SDFV2Api.SGD_SM3;
        byte[] pucHash = new byte[32];
        if ("SHA1".equals(algorithm)) {
            uiAlgID = SDFV2Api.SGD_SHA1;
        } else if ("SHA224".equals(algorithm)) {
            uiAlgID = SDFV2Api.SGD_SHA224;
        } else if ("SHA256".equals(algorithm)) {
            uiAlgID = SDFV2Api.SGD_SHA256;
        } else if ("SHA384".equals(algorithm)) {
            uiAlgID = SDFV2Api.SGD_SHA384;
            pucHash = new byte[64];
        } else if ("SHA512".equals(algorithm)) {
            uiAlgID = SDFV2Api.SGD_SHA512;
            pucHash = new byte[64];
        } else if ("MD5".equals(algorithm)) {
            uiAlgID = SDFV2Api.SGD_MD5;
        } else if ("SM3".equals(algorithm)) {
            return sdfSm3Hash(param);
        }
        IntByReference puiHashLength = new IntByReference(pucHash.length);
        sdfV2Api.SDF_HashInit(sessionHandle, uiAlgID, null, null, 0);
        sdfV2Api.SDF_HashUpdate(sessionHandle, data, data.length);
        PointerWrapper wrapper = new PointerWrapper(pucHash.length);
        sdfV2Api.SDF_HashFinal(sessionHandle, wrapper.getPointer(), puiHashLength);
        return pucHash;
    }

    private Object deleteKey(Map<String, Object> param) {
        log.info("deleteKey param= {}", param);
        Integer keyIndex = (Integer) param.get(PARAM_KEY_INDEX);
        Integer keyType = (Integer) param.get(PARAM_KEY_TYPE);
        Integer keyUsage = (Integer) param.get(PARAM_KEY_USAGE);
        int val = sdfV2Api.SDIF_DeleteKey(sessionHandle, keyIndex, keyType, keyUsage);
        if (val != 0) {
            throw new DeviceException("删除设备密钥失败，请检查加密机是否连接正确！");
        }
        return val;
    }

    private Object listKey(Map<String, Object> param) {
        log.info("listKey param= {}", param);
        List<Integer> result = new ArrayList<>();
        Integer keyIndex = (Integer) param.get(PARAM_KEY_INDEX);
        Integer keyType = (Integer) param.get(PARAM_KEY_TYPE);
        Integer keyUsage = (Integer) param.get(PARAM_KEY_USAGE);

        PointerWrapper pw = new PointerWrapper(4000);
        IntByReference keyNums = new IntByReference(1000);
        int val = sdfV2Api.SDIF_ListKey(sessionHandle, keyIndex, keyType, keyUsage, keyNums, pw.getPointer());
        if (val != 0) {
            throw new DeviceException("获取设备密钥列表失败，请检查加密机是否连接正确！");
        }
        int length = keyNums.getValue();
        byte[] kids = pw.getPointer().getByteArray(0L, length * 4);

        int numGroups = kids.length / 4;
        for (int i = 0; i < numGroups; i++) {
            byte[] group = Arrays.copyOfRange(kids, i * 4, (i + 1) * 4);
            int num = EncodeConvertUtil.bytes2Int(group);
            result.add(num);
        }
        log.info("listKey result = {}", result);

        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public void destroySession() {
        sdfV2Api.SDF_CloseSession(sessionHandle);
    }

    @Override
    public void destroyHsm() {
        sdfV2Api.SDF_CloseSession(sessionHandle);
        sdfV2Api.SDF_CloseDevice(deviceHandle);
    }

    @Override
    public boolean test() {
        byte[] data = new byte[16];
        int gensult = sdfV2Api.SDF_GenerateRandom(sessionHandle, data.length, data);
        if (gensult != 0) {
            log.warn("会话测试连接失败!");
            return false;
        }
        return true;
    }

    @Override
    public Integer getHsmSessionType() {
        return 0;
    }
}
