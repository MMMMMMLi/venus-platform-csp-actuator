package com.csp.actuator.message.consumer;

import com.csp.actuator.api.enums.CallBackStatusEnum;
import com.csp.actuator.api.kms.GenerateKeyCallBackTopicInfo;
import com.csp.actuator.api.kms.GenerateKeyTopicInfo;
import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.constants.ErrorMessage;
import com.csp.actuator.constants.TopicBingingName;
import com.csp.actuator.api.entity.GenerateKeyResult;
import com.csp.actuator.helper.CheckHelper;
import com.csp.actuator.helper.GenerateKeyHelper;
import com.csp.actuator.message.producer.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 创建密钥Consumer
 *
 * @author Weijia Jiang
 * @version v1
 * @description 创建密钥Consumer
 * @date Created in 2023-10-09 17:40
 */
@Slf4j
@Component
public class GenerateKeyConsumer {

    @Value("${msg.effective.time}")
    private Long MSG_EFFECTIVE_TIME;

    private final MessageProducer messageProducer;

    public GenerateKeyConsumer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @Bean
    public Consumer<String> generateKey() {
        return msg -> {
            log.info("GenerateKey msg: {}", msg);
            // 确认一下数据中心信息，如果校验通过，则执行创建密钥操作
            if (!CheckHelper.checkDataCenterIdAndMsgTime(msg, MSG_EFFECTIVE_TIME)) {
                return;
            }
            // 格式化密钥
            GenerateKeyTopicInfo generateKeyTopicInfo = JsonUtils.readValue(msg, GenerateKeyTopicInfo.class);
            log.info("GenerateKeyTopicInfo :{}", generateKeyTopicInfo);
            // 执行创建操作
            GenerateKeyResult generateKeyResult = null;
            String message = ErrorMessage.DEFAULT_SUCCESS_MESSAGE;
            try {
                generateKeyResult = GenerateKeyHelper.generateKey(generateKeyTopicInfo);
            } catch (Exception e) {
                log.error("GenerateKey failed, e: {}", message = e.getMessage());
            }
            GenerateKeyCallBackTopicInfo callBackTopicInfo = new GenerateKeyCallBackTopicInfo();
            callBackTopicInfo.setDate(System.currentTimeMillis());
            callBackTopicInfo.setKeyId(generateKeyTopicInfo.getKeyId());
            callBackTopicInfo.setMessage(message);
            if (Objects.isNull(generateKeyResult)) {
                callBackTopicInfo.setStatus(CallBackStatusEnum.FAILED.ordinal());
                callBackTopicInfo.setGenerateKeyResult(new GenerateKeyResult());
            } else {
                callBackTopicInfo.setStatus(CallBackStatusEnum.SUCCESS.ordinal());
                callBackTopicInfo.setGenerateKeyResult(generateKeyResult);
            }
            log.info("GenerateKeyCallBackTopicInfo : {}", callBackTopicInfo);
            if (messageProducer.producerMessage(TopicBingingName.GENERATE_KEY_CALL_BACK_BINGING_NAME, callBackTopicInfo)) {
                log.info("GenerateKey CallBack Success...");
            } else {
                log.error("GenerateKey CallBack Failed...");
            }
        };
    }
}
