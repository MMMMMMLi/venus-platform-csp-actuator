package com.csp.actuator.device.session;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.csp.actuator.device.exception.DeviceException;
import com.csp.actuator.utils.EncodeConvertUtil;
import com.csp.actuator.utils.FileUtils;
import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.csp.actuator.device.contants.HSMConstant.SANSEC_CONFIG_FILE;

@Slf4j
public class SDFApi {
    /**
     * 对称密码算法标识
     */
    public static final int SGD_DES_CBC = 0x00004002;    // DES算法CBC加密模式
    public static final int SGD_3DES_CBC = 0x00001002;    // 3DES算法CBC加密模式
    public static final int SGD_AES_CBC = 0x00002002;    // AES算法CBC加密模式
    public static final int SGD_SM1_CBC = 0x00000102;        //SM1算法CBC加密模式
    public static final int SGD_SM4_ECB = 0x00000401;        //SM4算法ECB加密模式
    public static final int SGD_SM4_CBC = 0x00000402;        //SM4算法CBC加密模式
    public static final int SGD_SM7_CBC = 0x00008002;        //SM7算法CBC加密模式

    /**
     * 非对称密码算法标识
     */
    public static final int SGD_RSA = 0x00010000;        //RSA算法
    public static final int SGD_RSA_SIGN = 0x00010001;        //RSA签名验签算法
    public static final int SGD_SM2 = 0x00020100;        //SM2椭圆曲线密码算法
    public static final int SGD_SM2_1 = 0x00020200;        //SM2 签名验签算法
    public static final int SGD_SM2_3 = 0x00020800;        //SM2 加密算法

    /**
     * 密码杂凑算法标识
     */
    public static final int SGD_SM3 = 0X00000001;        //SM3杂凑算法
    public static final int SGD_SHA1 = 0x00000002;        //SHA_1杂凑算法
    public static final int SGD_SHA224 = 0x00000020;        //SHA_224杂凑算法
    public static final int SGD_SHA256 = 0x00000004;        //SHA_256杂凑算法
    public static final int SGD_SHA384 = 0x00000010;        //SHA_384杂凑算法
    public static final int SGD_SHA512 = 0x00000008;        //SHA_512杂凑算法
    public static final int SGD_MD5 = 0x00000080;        //MD5杂凑算法
    /**
     * 临时文件目录
     */
    public static String tempDirPath = System.getProperty("java.io.tmpdir");

    private static final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    private static final String[] configLocations = new String[]{"classpath*:/hsm/*.*"};

    public static Resource[] resolveConfigLocations() {
        return Stream.of(Optional.ofNullable(configLocations).orElse(new String[0]))
                .flatMap(location -> Stream.of(getResources(location))).toArray(Resource[]::new);
    }

    private static Resource[] getResources(String location) {
        try {
            return resourceResolver.getResources(location);
        } catch (IOException e) {
            log.error("获取资源文件出错!location:{}", location, e);
            return new Resource[0];
        }
    }

    static {
        try {
            Resource[] resources = resolveConfigLocations();
            log.info("获取到hsm目录下文件数:{}个", resources.length);
            for (Resource rs : resources) {
                InputStream inputStream = rs.getInputStream();
                //将文件写入临时目录
                File tempFile = new File(tempDirPath + File.separator + rs.getFilename());
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                FileUtils.copyInputStreamToFile(inputStream, tempFile);
                log.info("文件拷贝完成!path:{}", tempFile.getAbsolutePath());
            }
            // 本地加载第三方动态库路径
            System.setProperty("jna.library.path", tempDirPath);
        } catch (IOException e) {
            log.error("加载动态库路径path异常!", e);
        }
    }

    private CLibrary cLibrary;

    /**
     * 重写密码机配置文件
     *
     * @param ip              地址
     * @param port            端口
     * @param dllName         库文件
     * @param passwd          密码
     * @param isEnableSslFlag 是否开启SSL模式 0.否 1.是
     */
    public void overrideIniConfigFile(String ip, String port, String dllName, String passwd, Integer isEnableSslFlag) {
        FileInputStream input = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        OutputStream os = null;
        try {
            // 动态修改配置文件
            input = new FileInputStream(tempDirPath + File.separator + SANSEC_CONFIG_FILE);
            //将byte用utf_8编码转化成字符或字符串
            isr = new InputStreamReader(input, StandardCharsets.UTF_8);
            reader = new BufferedReader(isr);
            StringBuffer sbf = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().contains("=")) {
                    String[] pro = line.trim().split("=");
                    if ("ip".equals(pro[0].trim())) {
                        sbf.append(pro[0].trim()).append("=").append(ip).append("\n");
                    } else if ("port".equals(pro[0].trim())) {
                        sbf.append(pro[0].trim()).append("=").append(port).append("\n");
                    } else if ("passwd".equals(pro[0].trim())) {
                        sbf.append(pro[0].trim()).append("=").append(passwd).append("\n");
                    } else if ("Protocol".equals(pro[0].trim())) {
                        sbf.append(pro[0].trim()).append("=").append(isEnableSslFlag == 1 ? "ssl" : "tcp").append("\n");
                    } else {
                        sbf.append(line).append("\n");
                    }
                } else {
                    sbf.append(line).append("\n");
                }
            }
            os = Files.newOutputStream(Paths.get(SANSEC_CONFIG_FILE));
            os.write(sbf.toString().getBytes());
            os.flush();
            // 加载动态库
            cLibrary = Native.loadLibrary(dllName, CLibrary.class);
        } catch (Exception e) {
            log.error("init hsm is error", e);
        } catch (UnsatisfiedLinkError ue) {
            log.error("init hsm is error", ue);
            throw new DeviceException("加载动态库文件失败，请检查动态库文件是否正确！");
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (reader != null) {
                    reader.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                log.error("close is fail!");
            }
        }
    }

    /**
     * 打开设备
     *
     * @param phDeviceHandle
     * @return
     * @throws DeviceException
     */
    public int SDF_OpenDevice(PointerByReference phDeviceHandle) throws DeviceException {
        int rv = cLibrary.SDF_OpenDevice(phDeviceHandle);
        if (rv != 0) {
            log.error("SDF_OpenDevice errror，errorCode：", Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("打开设备失败，请确认设备是否连接正常！");
        } else {
            return rv;
        }
    }

    /**
     * 关闭设备
     *
     * @param hDeviceHandle
     * @return
     * @throws DeviceException
     */
    public int SDF_CloseDevice(Pointer hDeviceHandle) throws DeviceException {
        int rv = cLibrary.SDF_CloseDevice(hDeviceHandle);
        if (rv != 0) {
            log.error("SDF_CloseDevice errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("关闭设备失败，请确认设备是否连接正常！");
        } else {
            return rv;
        }
    }

    /**
     * 打开会话
     *
     * @param hDeviceHandle
     * @param phSessionHandle
     * @return
     * @throws DeviceException
     */
    public int SDF_OpenSession(Pointer hDeviceHandle, PointerByReference phSessionHandle) throws DeviceException {
        int rv = cLibrary.SDF_OpenSession(hDeviceHandle, phSessionHandle);
        if (rv != 0) {
            log.error("SDF_OpenSession errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("打开设备会话失败，请确认设备是否连接正常！");
        } else {
            return rv;
        }
    }

    /**
     * 关闭会话
     *
     * @param hSessionHandle
     * @return
     * @throws DeviceException
     */
    public int SDF_CloseSession(Pointer hSessionHandle) throws DeviceException {
        int rv = cLibrary.SDF_CloseSession(hSessionHandle);
        if (rv != 0) {
            log.error("SDF_CloseSession errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("关闭设备会话失败，请确认设备是否连接正常！");
        } else {
            return rv;
        }
    }

    /**
     * 获取密码设备信息
     *
     * @param hSessionHandle
     * @param pstDeviceInfo
     * @return
     */
    public int SDF_GetDeviceInfo(Pointer hSessionHandle, CLibrary.DEVICEINFO.ByReference pstDeviceInfo) {
        int rv = cLibrary.SDF_GetDeviceInfo(hSessionHandle, pstDeviceInfo);
        if (rv != 0) {
            log.error("SDF_GetDeviceInfo errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            return rv;
        } else {
            return rv;
        }
    }

    /**
     * 生成随机数
     *
     * @param hSessionHandle
     * @param uiLength
     * @param pucRandom
     * @return
     * @throws DeviceException
     */
    public int SDF_GenerateRandom(Pointer hSessionHandle, int uiLength, byte[] pucRandom) throws DeviceException {
        int rv = cLibrary.SDF_GenerateRandom(hSessionHandle, uiLength, pucRandom);
        if (rv != 0) {
            log.error("SDF_GenerateRandom errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("生成密钥失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 请求密码设备产生指定模长的RSA密钥对（明文）
     *
     * @param hSessionHandle
     * @param uiKeyBits
     * @param pucPublicKey
     * @param pucPrivateKey
     * @return
     * @throws DeviceException
     */
    public int SDF_GenerateKeyPair_RSA(Pointer hSessionHandle, int uiKeyBits, CLibrary.RSArefPublicKey.ByReference pucPublicKey, CLibrary.RSArefPrivateKey.ByReference pucPrivateKey) throws DeviceException {
        int rv = cLibrary.SDF_GenerateKeyPair_RSA(hSessionHandle, uiKeyBits, pucPublicKey, pucPrivateKey);
        if (rv != 0) {
            log.error("SDF_GenerateKeyPair_RSA errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("生成密钥失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 请求密码设备产生指定和模长的ECC密钥对
     *
     * @param hSessionHandle
     * @param uiAlgID
     * @param uiKeyBits
     * @param pucPublicKey
     * @param pucPrivateKey
     * @return
     * @throws DeviceException
     */
    public int SDF_GenerateKeyPair_ECC(Pointer hSessionHandle, int uiAlgID, int uiKeyBits, CLibrary.ECCrefPublicKey.ByReference pucPublicKey, CLibrary.ECCrefPrivateKey.ByReference pucPrivateKey) throws DeviceException {
        int rv = cLibrary.SDF_GenerateKeyPair_ECC(hSessionHandle, uiAlgID, uiKeyBits, pucPublicKey, pucPrivateKey);
        if (rv != 0) {
            log.error("SDF_GenerateKeyPair_ECC errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("生成密钥失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 导出密码设备内部存储的指定索引位置的签名公钥
     *
     * @param hSessionHandle
     * @param uiKeyIndex
     * @param pucPublicKey
     * @return
     * @throws DeviceException
     */
    public int SDF_ExportSignPublicKey_ECC(Pointer hSessionHandle, int uiKeyIndex, CLibrary.ECCrefPublicKey.ByReference pucPublicKey) throws DeviceException {
        int rv = cLibrary.SDF_ExportSignPublicKey_ECC(hSessionHandle, uiKeyIndex, pucPublicKey);
        if (rv != 0) {
            log.error("SDF_GenerateKeyPair_ECC errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("导出密码设备内部存储的指定索引位置的签名公钥失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 生成会话密钥并用密钥加密密钥加密输出
     *
     * @param hSessionHandle
     * @param uiKeyBits
     * @param uiAlgID
     * @param uiKEKIndex
     * @param pucKey
     * @param puiKeyLength
     * @param phKeyHandle
     * @return
     * @throws DeviceException
     */
    public int SDF_GenerateKeyWithKEK(Pointer hSessionHandle, int uiKeyBits, int uiAlgID, int uiKEKIndex, byte[] pucKey, IntByReference puiKeyLength, PointerByReference phKeyHandle) throws DeviceException {
        int rv = cLibrary.SDF_GenerateKeyWithKEK(hSessionHandle, uiKeyBits, uiAlgID, uiKEKIndex, pucKey, puiKeyLength, phKeyHandle);
        if (rv != 0) {
            log.error("SDF_GenerateKeyWithKEK errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("生成会话密钥并用密钥加密密钥加密输出失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 导入会话密钥并用密钥加密密钥解密
     *
     * @param hSessionHandle
     * @param pucKey
     * @param uiKeyLength
     * @param phKeyHandle
     * @return
     * @throws DeviceException
     */
    public int SDF_ImportKeyWithKEK(Pointer hSessionHandle, int uiAlgID, int uiKEKIndex, byte[] pucKey, int uiKeyLength, PointerByReference phKeyHandle) throws DeviceException {
        int rv = cLibrary.SDF_ImportKeyWithKEK(hSessionHandle, uiAlgID, uiKEKIndex, pucKey, uiKeyLength, phKeyHandle);
        if (rv != 0) {
            log.error("SDF_ImportKeyWithKEK errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("导入会话密钥并用密钥加密密钥解密失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 导入明文会话密钥，同时返回密钥句柄
     *
     * @param hSessionHandle
     * @param pucKey
     * @param uiKeyLength
     * @param phKeyHandle
     * @return
     * @throws DeviceException
     */
    public int SDF_ImportKey(Pointer hSessionHandle, byte[] pucKey, int uiKeyLength, PointerByReference phKeyHandle) throws DeviceException {
        int rv = cLibrary.SDF_ImportKey(hSessionHandle, pucKey, uiKeyLength, phKeyHandle);
        if (rv != 0) {
            log.error("SDF_ImportKey errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("导入会话密钥失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 获取内部对称密钥句柄
     *
     * @param hSessionHandle
     * @param index
     * @param phKeyHandle
     * @return
     * @throws DeviceException
     */
    public int SDF_GetSymmKeyHandle(Pointer hSessionHandle, int index, PointerByReference phKeyHandle) throws DeviceException {
        int rv = cLibrary.SDF_GetSymmKeyHandle(hSessionHandle, index, phKeyHandle);
        if (rv != 0) {
            log.error("SDF_GetSymmKeyHandle errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("获取内部对称密钥句柄失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 销毁会话密钥，并释放为密钥句柄分配的内存等资源
     *
     * @param hSessionHandle
     * @param hKeyHandle
     * @return
     * @throws DeviceException
     */
    public int SDF_DestroyKey(Pointer hSessionHandle, Pointer hKeyHandle) throws DeviceException {
        int rv = cLibrary.SDF_DestroyKey(hSessionHandle, hKeyHandle);
        if (rv != 0) {
            log.error("SDF_DestroyKey errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("销毁密钥失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     *以下非对称算法运算类函数
     */
    /**
     * 指定使用外部公钥对数据进行运算
     *
     * @param hSessionHandle
     * @param pucPublicKey
     * @param pucDataInput
     * @param uiInputLength
     * @param pucDataOutput
     * @param puiOutputLength
     * @return
     * @throws DeviceException
     */
    public int SDF_ExternalPublicKeyOperation_RSA(Pointer hSessionHandle, CLibrary.RSArefPublicKey.ByReference pucPublicKey, byte[] pucDataInput, int uiInputLength, byte[] pucDataOutput, IntByReference puiOutputLength) throws DeviceException {
        int rv = cLibrary.SDF_ExternalPublicKeyOperation_RSA(hSessionHandle, pucPublicKey, pucDataInput, uiInputLength, pucDataOutput, puiOutputLength);
        if (rv != 0) {
            log.error("SDF_ExternalPublicKeyOperation_RSA errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("加密失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 外部rsa密钥解密运算
     *
     * @param hSessionHandle
     * @param pucPrivateKey
     * @param pucDataInput
     * @param uiInputLength
     * @param pucDataOutput
     * @param puiOutputLength
     * @return
     * @throws DeviceException
     */
    public int SDF_ExternalPrivateKeyOperation_RSA(Pointer hSessionHandle, CLibrary.RSArefPrivateKey.ByReference pucPrivateKey, byte[] pucDataInput, int uiInputLength, byte[] pucDataOutput, IntByReference puiOutputLength) throws DeviceException {
        int rv = cLibrary.SDF_ExternalPrivateKeyOperation_RSA(hSessionHandle, pucPrivateKey, pucDataInput, uiInputLength, pucDataOutput, puiOutputLength);
        if (rv != 0) {
            log.error("SDF_ExternalPrivateKeyOperation_RSA errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("解密失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 使用外部ECC私钥对数据进行签名运算
     *
     * @param hSessionHandle
     * @param uiAlgID
     * @param pucPrivateKey
     * @param pucData
     * @param uiDataLength
     * @param pucSignature
     * @return
     * @throws DeviceException
     */
    int SDF_ExternalSign_ECC(Pointer hSessionHandle, int uiAlgID, CLibrary.ECCrefPrivateKey.ByReference pucPrivateKey, byte[] pucData, int uiDataLength, CLibrary.ECCSignature.ByReference pucSignature) throws DeviceException {
        int rv = cLibrary.SDF_ExternalSign_ECC(hSessionHandle, uiAlgID, pucPrivateKey, pucData, uiDataLength, pucSignature);
        if (rv != 0) {
            log.error("SDF_ExternalSign_ECC errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("签名失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 使用内部ECC私钥对数据进行签名运算
     *
     * @param hSessionHandle
     * @param uiISKIndex
     * @param pucData
     * @param uiDataLength
     * @param pucSignature
     * @return
     * @throws DeviceException
     */
    int SDF_InternalSign_ECC(Pointer hSessionHandle, int uiISKIndex, byte[] pucData, int uiDataLength, CLibrary.ECCSignature.ByReference pucSignature) throws DeviceException {
        int rv = cLibrary.SDF_InternalSign_ECC(hSessionHandle, uiISKIndex, pucData, uiDataLength, pucSignature);
        if (rv != 0) {
            log.error("SDF_ExternalSign_ECC errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("签名失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 使用外部ECC公钥对ECC签名值进行验证运算
     *
     * @param hSessionHandle
     * @param uiAlgID
     * @param pucPublicKey
     * @param pucData
     * @param uiDataLength
     * @param pucSignature
     * @return
     * @throws DeviceException
     */
    public int SDF_ExternalVerify_ECC(Pointer hSessionHandle, int uiAlgID, CLibrary.ECCrefPublicKey.ByReference pucPublicKey, byte[] pucData, int uiDataLength, CLibrary.ECCSignature.ByReference pucSignature) throws DeviceException {
        int rv = cLibrary.SDF_ExternalVerify_ECC(hSessionHandle, uiAlgID, pucPublicKey, pucData, uiDataLength, pucSignature);
        if (rv != 0) {
            log.error("SDF_ExternalVerify_ECC errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("验签失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 使用外部ECC公钥对ECC签名值进行验证运算
     *
     * @param hSessionHandle
     * @param uiISKIndex
     * @param pucData
     * @param uiDataLength
     * @param pucSignature
     * @return
     * @throws DeviceException
     */
    public int SDF_InternalVerify_ECC(Pointer hSessionHandle, int uiISKIndex, byte[] pucData, int uiDataLength, CLibrary.ECCSignature.ByReference pucSignature) throws DeviceException {
        int rv = cLibrary.SDF_InternalVerify_ECC(hSessionHandle, uiISKIndex, pucData, uiDataLength, pucSignature);
        if (rv != 0) {
            log.error("SDF_ExternalVerify_ECC errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("验签失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 外部ECC公钥加密
     *
     * @param hSessionHandle
     * @param uiAlgID
     * @param pucPublicKey
     * @param pucData
     * @param uiDataLength
     * @param pucEncData
     * @return
     * @throws DeviceException
     */
    public int SDF_ExternalEncrypt_ECC(Pointer hSessionHandle, int uiAlgID, CLibrary.ECCrefPublicKey.ByReference pucPublicKey, byte[] pucData, int uiDataLength, CLibrary.ECCCipher.ByReference pucEncData) throws DeviceException {
        int rv = cLibrary.SDF_ExternalEncrypt_ECC(hSessionHandle, uiAlgID, pucPublicKey, pucData, uiDataLength, pucEncData);
        if (rv != 0) {
            log.error("SDF_ExternalEncrypt_ECC errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("加密失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 外部ECC私钥解密
     *
     * @param hSessionHandle
     * @param uiAlgID
     * @param pucPrivateKey
     * @param pucEncData
     * @param pucData
     * @param puiDataLength
     * @return
     * @throws DeviceException
     */
    public int SDF_ExternalDecrypt_ECC(Pointer hSessionHandle, int uiAlgID, CLibrary.ECCrefPrivateKey.ByReference pucPrivateKey, CLibrary.ECCCipher.ByReference pucEncData, byte[] pucData, IntByReference puiDataLength) throws DeviceException {
        int rv = cLibrary.SDF_ExternalDecrypt_ECC(hSessionHandle, uiAlgID, pucPrivateKey, pucEncData, pucData, puiDataLength);
        if (rv != 0) {
            log.error("SDF_ExternalDecrypt_ECC errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("解密失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 请求密码设备产生ECC密钥对，存储到设备索引
     *
     * @param hSessionHandle
     * @param uiKeyNumber
     * @return
     * @throws DeviceException
     */
    public int SWCSM_GenerateECCKeyPair(Pointer hSessionHandle, int uiKeyNumber) throws DeviceException {
        int rv = cLibrary.SWCSM_GenerateECCKeyPair(hSessionHandle, uiKeyNumber);
        if (rv != 0) {
            log.error("SDF_GenerateKeyPair_ECC errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("生成密钥失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * HMAC运算
     *
     * @param hSessionHandle
     * @param hKeyHandle
     * @param uiAlgID
     * @param pucData
     * @param uiDataLength
     * @param pucHmac
     * @param puiEncDataLength
     * @return
     * @throws DeviceException
     */
    public int SDF_Hmac(Pointer hSessionHandle, Pointer hKeyHandle, int uiAlgID, byte[] pucData, int uiDataLength, byte[] pucHmac, IntByReference puiEncDataLength) throws DeviceException {
        int rv = cLibrary.SDF_HMAC(hSessionHandle, hKeyHandle, uiAlgID, pucData, uiDataLength, pucHmac, puiEncDataLength);
        if (rv != 0) {
            log.error("SDF_HashInit errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("HMAC运算失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     *对称算法运算类函数
     */
    /**
     * 使用指定的密钥句柄和IV对数据进行对称加密运算
     *
     * @param hSessionHandle
     * @param hKeyHandle
     * @param uiAlgID
     * @param pucIV
     * @param pucData
     * @param uiDataLength
     * @param pucEncData
     * @param puiEncDataLength
     * @return
     * @throws DeviceException
     */
    public int SDF_Encrypt(Pointer hSessionHandle, Object hKeyHandle, int uiAlgID, byte[] pucIV, byte[] pucData, int uiDataLength, byte[] pucEncData, IntByReference puiEncDataLength) throws DeviceException {
        Pointer pointer = null;
        if (hKeyHandle instanceof Pointer) {
            Pointer hKeyHandleValue = (Pointer) hKeyHandle;
            pointer = hKeyHandleValue;
        } else if (hKeyHandle instanceof IntByReference) {
            Integer hKeyHandleValue = ((IntByReference) hKeyHandle).getValue();
            pointer = new Memory(4);
            pointer.setInt(0, hKeyHandleValue);
        }
        int rv = cLibrary.SDF_Encrypt(hSessionHandle, pointer, uiAlgID, pucIV, pucData, uiDataLength, pucEncData, puiEncDataLength);
        if (rv != 0) {
            log.error("SDF_Encrypt errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("加密失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 使用指定的密钥句柄和IV对数据进行对称解密运算
     *
     * @param hSessionHandle
     * @param hKeyHandle
     * @param uiAlgID
     * @param pucIV
     * @param pucEncData
     * @param uiEncDataLength
     * @param pucData
     * @param puiDataLength
     * @return
     * @throws DeviceException
     */
    public int SDF_Decrypt(Pointer hSessionHandle, Object hKeyHandle, int uiAlgID, byte[] pucIV, byte[] pucEncData, int uiEncDataLength, byte[] pucData, IntByReference puiDataLength) throws DeviceException {
        Pointer pointer = null;
        if (hKeyHandle instanceof Pointer) {
            Pointer hKeyHandleValue = (Pointer) hKeyHandle;
            pointer = hKeyHandleValue;
        } else if (hKeyHandle instanceof IntByReference) {
            Integer hKeyHandleValue = ((IntByReference) hKeyHandle).getValue();
            pointer = new Memory(4);
            pointer.setInt(0, hKeyHandleValue);
        }
        int rv = cLibrary.SDF_Decrypt(hSessionHandle, pointer, uiAlgID, pucIV, pucEncData, uiEncDataLength, pucData, puiDataLength);
        if (rv != 0) {
            log.error("SDF_Decrypt errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("解密失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     *杂凑运算类函数
     */
    /**
     * 哈希运算初始化
     *
     * @param hSessionHandle
     * @param uiAlgID
     * @param pucPublicKey
     * @param pucID
     * @param uiIDLength
     * @return
     * @throws DeviceException
     */
    public int SDF_HashInit(Pointer hSessionHandle, int uiAlgID, CLibrary.ECCrefPublicKey.ByReference pucPublicKey, byte[] pucID, int uiIDLength) throws DeviceException {
        int rv = cLibrary.SDF_HashInit(hSessionHandle, uiAlgID, pucPublicKey, pucID, uiIDLength);
        if (rv != 0) {
            log.error("SDF_HashInit errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("摘要初始化失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 哈希运算多包运算
     *
     * @param hSessionHandle
     * @param pucData
     * @param uiDataLength
     * @return
     * @throws DeviceException
     */
    public int SDF_HashUpdate(Pointer hSessionHandle, byte[] pucData, int uiDataLength) throws DeviceException {
        int rv = cLibrary.SDF_HashUpdate(hSessionHandle, pucData, uiDataLength);
        if (rv != 0) {
            log.error("SDF_HashUpdate errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("摘要运算失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 哈希运算多包运算结束，返回hash值
     *
     * @param hSessionHandle
     * @param pucHash
     * @param puiHashLength
     * @return
     * @throws DeviceException
     */
    public int SDF_HashFinal(Pointer hSessionHandle, byte[] pucHash, IntByReference puiHashLength) throws DeviceException {
        int rv = cLibrary.SDF_HashFinal(hSessionHandle, pucHash, puiHashLength);
        if (rv != 0) {
            log.error("SDF_HashFinal errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("摘要运算失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    //---------------------扩展接口

    /**
     * 导入对称密钥到设备索引
     */
    public int SWMF_InputKEK(Pointer hSessionHandle, int index, byte[] pucKey, int uiKeyLength) throws DeviceException {
        int rv = cLibrary.SWMF_InputKEK(hSessionHandle, index, pucKey, uiKeyLength);
        if (rv != 0) {
            log.error("SDF_HashFinal errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("导入对称密钥到失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 生成对称密钥到设备索引
     */
    public int SWMF_GenerateKEK(Pointer hSessionHandle, int index, int uiKeyLength) throws DeviceException {
        int rv = cLibrary.SWMF_GenerateKEK(hSessionHandle, index, uiKeyLength);
        if (rv != 0) {
            log.error("SDF_HashFinal errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("生成对称密钥失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * 生成非对称密钥到设备索引
     */
    public int SWCSM_InputECCKeyPair(Pointer hSessionHandle, int index, CLibrary.ECCrefPublicKey.ByReference pucPublicKey, CLibrary.ECCrefPrivateKey.ByReference pucPrivateKey) throws DeviceException {
        int rv = cLibrary.SWCSM_InputECCKeyPair(hSessionHandle, index, pucPublicKey, pucPrivateKey);
        if (rv != 0) {
            log.error("SWCSM_InputECCKeyPair errror，errorCode：" + Integer.toString(rv, 16).toUpperCase());
            throw new DeviceException("生成非对称密钥失败，请检查参数或设备状态！");
        } else {
            return rv;
        }
    }

    /**
     * CLibrary类
     */
    public interface CLibrary extends Library {
        /**
         *以下设备管理类函数
         */
        /**
         * 打开密码设备
         */
        int SDF_OpenDevice(PointerByReference phDeviceHandle);

        /**
         * 关闭密码设备
         */
        int SDF_CloseDevice(Pointer hDeviceHandle);

        /**
         * 打开会话
         */
        int SDF_OpenSession(Pointer hDeviceHandle, PointerByReference phSessionHandle);

        /**
         * 关闭会话
         */
        int SDF_CloseSession(Pointer hSessionHandle);

        /**
         * 获取密码设备信息
         */
        int SDF_GetDeviceInfo(Pointer hSessionHandle, DEVICEINFO.ByReference pstDeviceInfo);

        /**
         * 生成随机数
         */
        int SDF_GenerateRandom(Pointer hSessionHandle, int uiLength, byte[] pucRandom);

        /**
         * 请求密码设备产生指定模长的RSA密钥对（明文）
         */
        int SDF_GenerateKeyPair_RSA(Pointer hSessionHandle, int uiKeyBits, RSArefPublicKey.ByReference pucPublicKey, RSArefPrivateKey.ByReference pucPrivateKey);

        /**
         * 导出密码设备内部存储的指定索引位置的签名公钥
         */
        int SDF_ExportSignPublicKey_ECC(Pointer hSessionHandle, int uiKeyIndex, ECCrefPublicKey.ByReference pucPublicKey);

        /**
         * 请求密码设备产生指定和模长的ECC密钥对
         */
        int SDF_GenerateKeyPair_ECC(Pointer hSessionHandle, int uiAlgID, int uiKeyBits, ECCrefPublicKey.ByReference pucPublicKey, ECCrefPrivateKey.ByReference pucPrivateKey);

        /**
         * 生成会话密钥并用密钥加密密钥加密输出
         */
        int SDF_GenerateKeyWithKEK(Pointer hSessionHandle, int uiKeyBits, int uiAlgID, int uiKEKIndex, byte[] pucKey, IntByReference puiKeyLength, PointerByReference phKeyHandle);

        /**
         * 导入会话密钥并用密钥加密密钥解密
         */
        int SDF_ImportKeyWithKEK(Pointer hSessionHandle, int uiAlgID, int uiKEKIndex, byte[] pucKey, int uiKeyLength, PointerByReference phKeyHandle);

        /**
         * 导入明文会话密钥，同时返回密钥句柄
         */
        int SDF_ImportKey(Pointer hSessionHandle, byte[] pucKey, int uiKeyLength, PointerByReference phKeyHandle);

        /**
         * 获取内部对称密钥句柄
         */
        int SDF_GetSymmKeyHandle(Pointer hSessionHandle, int index, PointerByReference phKeyHandle);

        /**
         * 销毁会话密钥，并释放为密钥句柄分配的内存等资源
         */
        int SDF_DestroyKey(Pointer hSessionHandle, Pointer hKeyHandle);


        /**
         *以下非对称算法运算类函数
         */
        /**
         * 指定使用外部公钥对数据进行运算
         */
        int SDF_ExternalPublicKeyOperation_RSA(Pointer hSessionHandle, RSArefPublicKey.ByReference pucPublicKey, byte[] pucDataInput, int uiInputLength, byte[] pucDataOutput, IntByReference puiOutputLength);

        /**
         * 指定使用外部私钥对数据进行运算
         */
        int SDF_ExternalPrivateKeyOperation_RSA(Pointer hSessionHandle, RSArefPrivateKey.ByReference pucPrivateKey, byte[] pucDataInput, int uiInputLength, byte[] pucDataOutput, IntByReference puiOutputLength);

        /**
         * 使用外部ECC私钥对数据进行签名运算
         */
        int SDF_ExternalSign_ECC(Pointer hSessionHandle, int uiAlgID, ECCrefPrivateKey.ByReference pucPrivateKey, byte[] pucData, int uiDataLength, ECCSignature.ByReference pucSignature);

        /**
         * 使用内部ECC私钥对数据进行签名运算
         */
        int SDF_InternalSign_ECC(Pointer hSessionHandle, int uiISKIndex, byte[] pucData, int uiDataLength, ECCSignature.ByReference pucSignature);

        /**
         * 使用外部ECC公钥对ECC签名值进行验证运算
         */
        int SDF_ExternalVerify_ECC(Pointer hSessionHandle, int uiAlgID, ECCrefPublicKey.ByReference pucPublicKey, byte[] pucData, int uiDataLength, ECCSignature.ByReference pucSignature);

        /**
         * 使用内部ECC公钥对ECC签名值进行验证运算
         */
        int SDF_InternalVerify_ECC(Pointer hSessionHandle, int uiISKIndex, byte[] pucData, int uiDataLength, ECCSignature.ByReference pucSignature);

        /**
         * 使用外部ECC公钥对数据进行加密运算
         */
        int SDF_ExternalEncrypt_ECC(Pointer hSessionHandle, int uiAlgID, ECCrefPublicKey.ByReference pucPublicKey, byte[] pucData, int uiDataLength, ECCCipher.ByReference pucEncData);

        /**
         * 使用外部 ECC 私钥进行解密运算
         */
        int SDF_ExternalDecrypt_ECC(Pointer hSessionHandle, int uiAlgID, ECCrefPrivateKey.ByReference pucPrivateKey, ECCCipher.ByReference pucEncData, byte[] pucData, IntByReference puiDataLength);

        /**
         * 请求密码设备产生ECC密钥对，存储到设备索引
         */
        int SWCSM_GenerateECCKeyPair(Pointer hSessionHandle, int uiKeyNumber);

        /**
         * HMAC运算
         */
        int SDF_HMAC(Pointer hSessionHandle, Pointer hKeyHandle, int uiAlgID, byte[] pucData, int uiDataLength, byte[] pucHmac, IntByReference puiEncDataLength);

        /**
         *对称算法运算类函数
         */
        /**
         * 使用指定的密钥句柄和IV对数据进行对称加密运算
         */
        int SDF_Encrypt(Pointer hSessionHandle, Pointer hKeyHandle, int uiAlgID, byte[] pucIV, byte[] pucData, int uiDataLength, byte[] pucEncData, IntByReference puiEncDataLength);

        /**
         * 使用指定的密钥句柄和IV对数据进行对称解密运算
         */
        int SDF_Decrypt(Pointer hSessionHandle, Pointer hKeyHandle, int uiAlgID, byte[] pucIV, byte[] pucEncData, int uiEncDataLength, byte[] pucData, IntByReference puiDataLength);

        /**
         *杂凑运算类函数
         */
        /**
         * 三步式数据杂凑运算第一步
         */
        int SDF_HashInit(Pointer hSessionHandle, int uiAlgID, ECCrefPublicKey.ByReference pucPublicKey, byte[] pucID, int uiIDLength);

        /**
         * 三步式数据杂凑运算第二步，对输入的明文进行杂凑运算
         */
        int SDF_HashUpdate(Pointer hSessionHandle, byte[] pucData, int uiDataLength);

        /**
         * 三步式数据杂凑运算第三步，杂凑运算结束返回杂凑数据并清除中间数据
         */
        int SDF_HashFinal(Pointer hSessionHandle, byte[] pucHash, IntByReference puiHashLength);

        //---------------------扩展接口

        /**
         * 导入对称密钥到设备索引
         */
        int SWMF_InputKEK(Pointer hSessionHandle, int index, byte[] pucKey, int uiKeyLength);

        /**
         * 生成对称密钥到设备索引
         */
        int SWMF_GenerateKEK(Pointer hSessionHandle, int index, int uiKeyLength);

        /**
         * 生成非对称密钥到设备索引
         */
        int SWCSM_InputECCKeyPair(Pointer hSessionHandle, int index, ECCrefPublicKey.ByReference pucPublicKey, ECCrefPrivateKey.ByReference pucPrivateKey);
        //------下面注释都是C结构体实体对象------

        /**
         * ECC公钥结构体
         */
        class ECCrefPublicKey extends Structure {
            public int bits;//256
            public byte[] x = new byte[64];
            public byte[] y = new byte[64];

            public int getBits() {
                return bits;
            }

            public void setBits(int bits) {
                this.bits = bits;
            }

            public byte[] getX() {
                return x;
            }

            public void setX(byte[] x) {
                System.arraycopy(x, 0, this.x, 0, x.length);
            }

            public byte[] getY() {
                return y;
            }

            public void setY(byte[] y) {
                System.arraycopy(y, 0, this.y, 0, y.length);
            }

            public static class ByReference extends ECCrefPublicKey implements Structure.ByReference {
            }

            public void stringToECCrefPublicKey(String eccRefPublicKey) {
                JSONObject jsonObjectDe = JSON.parseObject(eccRefPublicKey);
                byte[] x1 = Base64.getDecoder().decode(jsonObjectDe.get("x").toString());
                System.arraycopy(x1, 32, this.x, 32, 32);
                byte[] y1 = Base64.getDecoder().decode(jsonObjectDe.get("y").toString());
                System.arraycopy(y1, 32, this.y, 32, 32);
                this.bits = Integer.parseInt(jsonObjectDe.get("bits").toString());
            }

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList(new String[]{"bits", "x", "y"});
            }

            @Override
            public String toString() {
                return JSON.toJSONString(this);
            }
        }

        /**
         * ECC 私钥结构体
         */
        class ECCrefPrivateKey extends Structure {
            public int bits;
            public byte[] K = new byte[64];

            public ECCrefPrivateKey() {
            }

            public static class ByReference extends ECCrefPrivateKey implements Structure.ByReference {
            }

            public int getBits() {
                return bits;
            }

            public void setBits(int bits) {
                this.bits = bits;
            }

            public byte[] getK() {
                return K;
            }

            public void setK(byte[] k) {
                System.arraycopy(k, 0, this.K, 0, k.length);
            }

            public void stringToECCrefPrivateKey(String eccRefPrivateKey) {
                JSONObject jsonObjectDe = JSON.parseObject(eccRefPrivateKey);
                byte[] K1 = Base64.getDecoder().decode(jsonObjectDe.get("K").toString());
                System.arraycopy(K1, 32, this.K, 32, 32);
                this.bits = Integer.parseInt(jsonObjectDe.get("bits").toString());
            }

            @Override
            public String toString() {
                return JSON.toJSONString(this);
            }

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList(new String[]{"bits", "K"});
            }
        }

        /**
         * ECC 密文结构体
         */
        class ECCCipher extends Structure {
            public byte[] x = new byte[64];
            public byte[] y = new byte[64];
            //预留，用于支持带MAC输出的ECC算法
            public byte[] M = new byte[32];
            //密文数据长度
            public int L = 1024;
            //加密后的数据
            public byte[] C = new byte[1024];

            public ECCCipher() {
            }

            public static class ByReference extends ECCCipher implements Structure.ByReference {
            }

            public byte[] getX() {
                return x;
            }

            public void setX(byte[] x) {
                System.arraycopy(x, 0, this.x, 0, x.length);
            }

            public byte[] getY() {
                return y;
            }

            public void setY(byte[] y) {
                System.arraycopy(y, 0, this.y, 0, y.length);
            }

            public byte[] getM() {
                return M;
            }

            public void setM(byte[] m) {
                System.arraycopy(m, 0, this.M, 0, m.length);
            }

            public int getL() {
                return L;
            }

            public void setL(int l) {
                L = l;
            }

            public byte[] getC() {
                return C;
            }

            public void setC(byte[] c) {
                System.arraycopy(c, 0, this.C, 0, c.length);
            }

            public byte[] getCipher() {
                byte[] cipher = new byte[164 + L];
                System.arraycopy(x, 0, cipher, 0, 64);
                System.arraycopy(y, 0, cipher, 64, 64);
                System.arraycopy(M, 0, cipher, 128, 32);
                System.arraycopy(EncodeConvertUtil.int2bytes(L), 0, cipher, 160, 4);
                System.arraycopy(C, 0, cipher, 164, L);
                return cipher;
            }

            public void stringToECCCipher(String eccCipher) {
                JSONObject jsonObjectDe = JSON.parseObject(eccCipher);
                byte[] x1 = Base64.getDecoder().decode(jsonObjectDe.get("x").toString());
                System.arraycopy(x1, 32, this.x, 32, 32);
                byte[] y1 = Base64.getDecoder().decode(jsonObjectDe.get("y").toString());
                System.arraycopy(y1, 32, this.y, 32, 32);
                byte[] m = Base64.getDecoder().decode(jsonObjectDe.get("M").toString());
                System.arraycopy(m, 0, this.M, 0, m.length);
                byte[] c = Base64.getDecoder().decode(jsonObjectDe.get("C").toString());
                System.arraycopy(c, 0, this.C, 0, c.length);
                this.L = Integer.parseInt(jsonObjectDe.get("L").toString());
            }

            @Override
            public String toString() {
                return JSON.toJSONString(this);
            }

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList(new String[]{"x", "y", "M", "L", "C"});
            }
        }

        /**
         * RSA公钥结构体
         */
        class RSArefPublicKey extends Structure {
            public int bits = 0;//2048
            public byte[] m = new byte[512];
            public byte[] e = new byte[512];

            public static class ByReference extends RSArefPublicKey implements Structure.ByReference {
            }

            public int getBits() {
                return bits;
            }

            public void setBits(int bits) {
                this.bits = bits;
            }

            public byte[] getM() {
                return m;
            }

            public void setM(byte[] m) {
                this.m = m;
            }

            public byte[] getE() {
                return e;
            }

            public void setE(byte[] e) {
                this.e = e;
            }

            @Override
            public String toString() {
                return JSON.toJSONString(this);
            }

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList(new String[]{"bits", "m", "e"});
            }
        }

        /**
         * RSA私钥结构体
         */
        class RSArefPrivateKey extends Structure {
            public int bits = 0;
            public byte[] m = new byte[512];
            public byte[] e = new byte[512];
            public byte[] d = new byte[512];
            public byte[] prime = new byte[2 * 256];
            public byte[] pexp = new byte[2 * 256];
            public byte[] coef = new byte[256];

            public static class ByReference extends RSArefPrivateKey implements Structure.ByReference {
            }

            public RSArefPrivateKey() {
                super();
            }

            public int getBits() {
                return bits;
            }

            public void setBits(int bits) {
                this.bits = bits;
            }

            public byte[] getM() {
                return m;
            }

            public void setM(byte[] m) {
                this.m = m;
            }

            public byte[] getE() {
                return e;
            }

            public void setE(byte[] e) {
                this.e = e;
            }

            public byte[] getD() {
                return d;
            }

            public void setD(byte[] d) {
                this.d = d;
            }

            public byte[] getPrime() {
                return prime;
            }

            public void setPrime(byte[] prime) {
                this.prime = prime;
            }

            public byte[] getPexp() {
                return pexp;
            }

            public void setPexp(byte[] pexp) {
                this.pexp = pexp;
            }

            public byte[] getCoef() {
                return coef;
            }

            public void setCoef(byte[] coef) {
                this.coef = coef;
            }

            @Override
            public String toString() {
                return JSON.toJSONString(this);
            }

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList(new String[]{"bits", "m", "e", "d", "prime", "pexp", "coef"});
            }
        }

        /**
         * ECC签名结构体
         */
        class ECCSignature extends Structure {
            public byte[] r = new byte[64];
            public byte[] s = new byte[64];

            public byte[] getR() {
                return r;
            }

            public void setR(byte[] r) {
                System.arraycopy(r, 0, this.r, 0, r.length);
            }

            public byte[] getS() {
                return s;
            }

            public void setS(byte[] s) {
                System.arraycopy(s, 0, this.s, 0, s.length);
            }

            public static class ByReference extends ECCSignature implements Structure.ByReference {
            }

            public void stringToECCSignature(String eccSignature) {
                JSONObject jsonObjectDe = JSON.parseObject(eccSignature);
                byte[] r1 = Base64.getDecoder().decode(jsonObjectDe.get("r").toString());
                System.arraycopy(r1, 0, this.r, 0, r1.length);
                byte[] s1 = Base64.getDecoder().decode(jsonObjectDe.get("s").toString());
                System.arraycopy(s1, 0, this.s, 0, s1.length);
            }

            @Override
            public String toString() {
                return JSON.toJSONString(this);
            }

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList(new String[]{"r", "s"});
            }
        }

        @Data
        class DEVICEINFO extends Structure {
            public byte[] IssuerName = new byte[40];
            public byte[] DeviceName = new byte[16];
            public byte[] DeviceSerial = new byte[16];
            public int DeviceVersion = 0;
            public int StandardVersion = 0;
            public int[] AsymAlgAbility = new int[2];
            public int SymAlgAbility = 0;
            public int HashAlgAbility = 0;
            public int BufferSize = 0;

            public static class ByReference extends DEVICEINFO implements Structure.ByReference {
            }


            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList(new String[]{"IssuerName", "DeviceName", "DeviceSerial", "DeviceVersion", "StandardVersion", "AsymAlgAbility", "SymAlgAbility", "HashAlgAbility", "BufferSize"});
            }
        }
    }
}
