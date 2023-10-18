package com.csp.actuator.cache;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.constants.BaseConstant;
import com.csp.actuator.entity.ApiResult;
import com.csp.actuator.entity.SoftSignVerifyDTO;
import com.csp.actuator.exception.ActuatorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.security.KeyPair;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.csp.actuator.constants.ErrorMessage.ERROR_DATA_PARAM_NOT_FOUND;

/**
 * 数据中心Key
 *
 * @author Weijia Jiang
 * @version v1
 * @description 数据中心Key
 * @date Created in 2023-10-11 15:13
 */
@Slf4j
public class DataCenterKeyCache {

    private static byte[] DATA_CENTER_KEY = null;

    private static KeyPair keyPair = null;

    public static byte[] getDataCenterKey() {
        return DATA_CENTER_KEY;
    }

    public static boolean initDataCenterKey(String kmsAddress, String kmsSecret) {
        if (keyPair == null) {
            keyPair = SecureUtil.generateKeyPair("SM2");
        }
        try {
            SoftSignVerifyDTO softSignVerifyDTO = new SoftSignVerifyDTO("", HexUtil.encodeHexStr(keyPair.getPublic().getEncoded()), "", "");
            // 构建请求
            String result = HttpRequest.post(kmsAddress + "/csp/kms/api/hsmDevice/digitalEnvelopeEncryptKek")
                    .header(BaseConstant.REQUEST_HEADER_SECRET, kmsSecret)
                    .header(Header.CONTENT_TYPE, "application/json")
                    .body(JSONObject.toJSONString(softSignVerifyDTO))
                    .timeout(20000)
                    .execute().body();
            log.info("Get key result :{}", result);
            // 校验
            if (StringUtils.isBlank(result)) {
                return false;
            }
            ApiResult apiResult = JSON.parseObject(result, ApiResult.class);
            // 校验一下返回码
            String code = apiResult.getCode();
            if (!ApiResult.SUCCESS_CODE.equals(code)) {
                return false;
            }
            // 密钥信息
            String keyInfo = (String) apiResult.getData();
            if (StringUtils.isBlank(keyInfo)) {
                return false;
            }
            // 设置
            setDataCenterKey(keyInfo);
            // 校验
            if (DATA_CENTER_KEY == null) {
                return false;
            }
        } catch (Exception e) {
            log.error("initDataCenterKey error :{}", e.getMessage());
            return false;
        }
        return true;
    }

    public static String getDataCenterKey4PublicKeyEncrypt(String publicKeyStr) {
        if (StringUtils.isBlank(publicKeyStr)) {
            throw new ActuatorException(ERROR_DATA_PARAM_NOT_FOUND);
        }
        SM2 sm2 = SmUtil.sm2(null, publicKeyStr);
        // 对明文KEK进行SM2加密
        return sm2.encryptBase64(DATA_CENTER_KEY, KeyType.PublicKey);
    }

    public static void setDataCenterKey(String keyInfo) {
        // 获取SM2
        SM2 sm2 = SmUtil.sm2(HexUtil.encodeHexStr(keyPair.getPrivate().getEncoded()), HexUtil.encodeHexStr(keyPair.getPublic().getEncoded()));
        // 解码
        byte[] keyInfoByte = Base64.getDecoder().decode(keyInfo);
        // 解密赋值
        DATA_CENTER_KEY = sm2.decrypt(keyInfoByte, KeyType.PrivateKey);
    }
}
