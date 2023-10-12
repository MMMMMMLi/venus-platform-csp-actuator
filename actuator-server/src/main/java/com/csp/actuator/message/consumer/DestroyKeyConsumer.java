package com.csp.actuator.message.consumer;

import com.csp.actuator.api.entity.GenerateKeyResult;
import com.csp.actuator.api.entity.RemoveKeyInfo;
import com.csp.actuator.api.enums.CallBackStatusEnum;
import com.csp.actuator.api.kms.DestroyKeyCallBackTopicInfo;
import com.csp.actuator.api.kms.DestroyKeyTopicInfo;
import com.csp.actuator.api.kms.GenerateKeyCallBackTopicInfo;
import com.csp.actuator.api.kms.GenerateKeyTopicInfo;
import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.constants.TopicBingingName;
import com.csp.actuator.helper.CheckHelper;
import com.csp.actuator.helper.DestroyKeyHelper;
import com.csp.actuator.helper.GenerateKeyHelper;
import com.csp.actuator.message.producer.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Consumer;

import static com.csp.actuator.constants.BaseConstant.DEFAULT_FAILED_MESSAGE;
import static com.csp.actuator.constants.BaseConstant.DEFAULT_SUCCESS_MESSAGE;

/**
 * 密钥销毁消费者
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥销毁消费者
 * @date Created in 2023-10-11 16:35
 */
@Slf4j
@Component
public class DestroyKeyConsumer {

    @Value("${msg.effective.time}")
    private Long MSG_EFFECTIVE_TIME;

    private final MessageProducer messageProducer;

    public DestroyKeyConsumer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @Bean
    public Consumer<String> destroyKey() {
        return msg -> {
            log.info("DestroyKey msg: {}", msg);
            // 确认一下数据中心信息，如果校验通过，则执行创建密钥操作
            if (!CheckHelper.checkDataCenterIdAndMsgTime(msg, MSG_EFFECTIVE_TIME)) {
                return;
            }
            // 格式化密钥
            DestroyKeyTopicInfo destroyKeyTopicInfo = JsonUtils.readValue(msg, DestroyKeyTopicInfo.class);
            log.info("DestroyKeyTopicInfo :{}", destroyKeyTopicInfo);
            String message = DEFAULT_SUCCESS_MESSAGE;
            // 执行销毁操作
            Boolean removeFlag = Boolean.FALSE;
            try {
                removeFlag = DestroyKeyHelper.destroyKey(destroyKeyTopicInfo);
            } catch (Exception e) {
                log.error("DestroyKey failed, e: {}", message = e.getMessage());
            }
            // 销毁密钥信息
            RemoveKeyInfo removeKeyInfo = destroyKeyTopicInfo.getRemoveKeyInfo();
            DestroyKeyCallBackTopicInfo callBackTopicInfo = new DestroyKeyCallBackTopicInfo();
            callBackTopicInfo.setDate(System.currentTimeMillis());
            callBackTopicInfo.setKeyId(removeKeyInfo.getKeyId());
            if (removeFlag) {
                callBackTopicInfo.setStatus(CallBackStatusEnum.FAILED.ordinal());
                callBackTopicInfo.setMessage(DEFAULT_FAILED_MESSAGE);
            } else {
                callBackTopicInfo.setStatus(CallBackStatusEnum.SUCCESS.ordinal());
                callBackTopicInfo.setMessage(message);
            }
            log.info("DestroyKeyCallBackTopicInfo : {}", callBackTopicInfo);
            if (messageProducer.producerMessage(TopicBingingName.DESTROY_KEY_CALL_BACK_BINGING_NAME, callBackTopicInfo)) {
                log.info("DestroyKey CallBack Success...");
            } else {
                log.error("DestroyKey CallBack Failed...");
            }
        };
    }
}
