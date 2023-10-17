package com.csp.actuator.message.consumer;

import com.csp.actuator.api.enums.CallBackStatusEnum;
import com.csp.actuator.api.kms.DestroyKeyCallBackTopicInfo;
import com.csp.actuator.api.kms.DestroyKeyTopicInfo;
import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.helper.DestroyKeyHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static com.csp.actuator.constants.ErrorMessage.DEFAULT_SUCCESS_MESSAGE;
import static com.csp.actuator.constants.ErrorMessage.ERROR_DATA_FORMAT_FAILED;

/**
 * 密钥销毁消费者
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥销毁消费者
 * @date Created in 2023-10-11 16:35
 */
@Slf4j
public class DestroyKeyConsumer {

    public static DestroyKeyCallBackTopicInfo destroyKey(String msg) {
        log.info("DestroyKey msg: {}", msg);
        // 返回实体信息
        String message = DEFAULT_SUCCESS_MESSAGE;
        DestroyKeyCallBackTopicInfo callBackTopicInfo = new DestroyKeyCallBackTopicInfo();
        callBackTopicInfo.setDate(System.currentTimeMillis());
        // 格式化密钥
        DestroyKeyTopicInfo destroyKeyTopicInfo = JsonUtils.readValue(msg, DestroyKeyTopicInfo.class);
        log.info("DestroyKeyTopicInfo :{}", destroyKeyTopicInfo);
        if (Objects.isNull(destroyKeyTopicInfo)) {
            return callBackTopicInfo.setMessage(ERROR_DATA_FORMAT_FAILED)
                    .setStatus(CallBackStatusEnum.FAILED.ordinal());
        }
        // 执行销毁操作
        Boolean removeFlag = Boolean.FALSE;
        try {
            removeFlag = DestroyKeyHelper.destroyKey(destroyKeyTopicInfo);
        } catch (Exception e) {
            log.error("DestroyKey failed, e: {}", message = e.getMessage());
        }
        // 销毁密钥信息
        if (removeFlag) {
            callBackTopicInfo.setStatus(CallBackStatusEnum.SUCCESS.ordinal());
        } else {
            callBackTopicInfo.setStatus(CallBackStatusEnum.FAILED.ordinal());
        }
        callBackTopicInfo.setKeyId(destroyKeyTopicInfo.getRemoveKeyInfo().getKeyId());
        callBackTopicInfo.setMessage(message);
        log.info("DestroyKeyCallBackTopicInfo : {}", callBackTopicInfo);
        return callBackTopicInfo;
    }
}
