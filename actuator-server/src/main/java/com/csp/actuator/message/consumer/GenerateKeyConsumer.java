package com.csp.actuator.message.consumer;

import com.csp.actuator.api.entity.GenerateKeyResult;
import com.csp.actuator.api.enums.CallBackStatusEnum;
import com.csp.actuator.api.kms.GenerateKeyCallBackTopicInfo;
import com.csp.actuator.api.kms.GenerateKeyTopicInfo;
import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.constants.ErrorMessage;
import com.csp.actuator.helper.GenerateKeyHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static com.csp.actuator.constants.ErrorMessage.ERROR_DATA_FORMAT_FAILED;

/**
 * 创建密钥Consumer
 *
 * @author Weijia Jiang
 * @version v1
 * @description 创建密钥Consumer
 * @date Created in 2023-10-09 17:40
 */
@Slf4j
public class GenerateKeyConsumer {

    public static GenerateKeyCallBackTopicInfo generateKey(String msg) {
        log.info("GenerateKey msg: {}", msg);
        String message = ErrorMessage.DEFAULT_SUCCESS_MESSAGE;
        // 返回实体
        GenerateKeyCallBackTopicInfo callBackTopicInfo = new GenerateKeyCallBackTopicInfo();
        callBackTopicInfo.setDate(System.currentTimeMillis());
        // 格式化密钥
        GenerateKeyTopicInfo generateKeyTopicInfo = JsonUtils.readValue(msg, GenerateKeyTopicInfo.class);
        log.info("GenerateKeyTopicInfo :{}", generateKeyTopicInfo);
        if (Objects.isNull(generateKeyTopicInfo)) {
            return callBackTopicInfo.setMessage(ERROR_DATA_FORMAT_FAILED)
                    .setGenerateKeyResult(new GenerateKeyResult())
                    .setStatus(CallBackStatusEnum.FAILED.ordinal());
        }
        // 执行创建操作
        GenerateKeyResult generateKeyResult = null;
        try {
            generateKeyResult = GenerateKeyHelper.generateKey(generateKeyTopicInfo);
        } catch (Exception | Error e) {
            e.printStackTrace();
            log.error("GenerateKey failed, e: {}", message = e.getMessage());
        }
        if (Objects.isNull(generateKeyResult)) {
            callBackTopicInfo.setStatus(CallBackStatusEnum.FAILED.ordinal());
            callBackTopicInfo.setGenerateKeyResult(new GenerateKeyResult());
        } else {
            callBackTopicInfo.setStatus(CallBackStatusEnum.SUCCESS.ordinal());
            callBackTopicInfo.setGenerateKeyResult(generateKeyResult);
        }
        callBackTopicInfo.setMessage(message);
        callBackTopicInfo.setKeyId(generateKeyTopicInfo.getKeyId());
        log.info("GenerateKeyCallBackTopicInfo : {}", callBackTopicInfo);
        return callBackTopicInfo;
    }
}
