package com.csp.actuator.message.consumer;

import com.csp.actuator.api.entity.DeviceSyncKeyResult;
import com.csp.actuator.api.entity.GenerateKeyResult;
import com.csp.actuator.api.enums.CallBackStatusEnum;
import com.csp.actuator.api.kms.GenerateKeyCallBackTopicInfo;
import com.csp.actuator.api.kms.GenerateKeyTopicInfo;
import com.csp.actuator.api.kms.SyncKeyCallBackTopicInfo;
import com.csp.actuator.api.kms.SyncKeyTopicInfo;
import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.constants.TopicBingingName;
import com.csp.actuator.helper.CheckHelper;
import com.csp.actuator.helper.GenerateKeyHelper;
import com.csp.actuator.helper.SyncKeyHelper;
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
 * 密钥同步Consumer
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥同步Consumer
 * @date Created in 2023-10-12 18:02
 */
@Slf4j
@Component
public class SyncKeyConsumer {

    @Value("${msg.effective.time}")
    private Long MSG_EFFECTIVE_TIME;

    private final MessageProducer messageProducer;

    public SyncKeyConsumer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @Bean
    public Consumer<String> syncKey() {
        return msg -> {
            log.info("SyncKey msg: {}", msg);
            // 确认一下数据中心信息，如果校验通过，则执行创建密钥操作
            if (!CheckHelper.checkDataCenterIdAndMsgTime(msg, MSG_EFFECTIVE_TIME)) {
                return;
            }
            // 格式化密钥
            SyncKeyTopicInfo syncKeyTopicInfo = JsonUtils.readValue(msg, SyncKeyTopicInfo.class);
            log.info("SyncKeyTopicInfo :{}", syncKeyTopicInfo);
            // 执行创建操作
            DeviceSyncKeyResult generateKeyResult = null;
            String message = DEFAULT_SUCCESS_MESSAGE;
            try {
                generateKeyResult = SyncKeyHelper.syncKey(syncKeyTopicInfo);
            } catch (Exception e) {
                log.error("GenerateKey failed, e: {}", message = e.getMessage());
            }
            SyncKeyCallBackTopicInfo callBackTopicInfo = new SyncKeyCallBackTopicInfo();
            callBackTopicInfo.setDate(System.currentTimeMillis());
            callBackTopicInfo.setDeviceSyncKeyResult(generateKeyResult);
            if (Objects.isNull(generateKeyResult)) {
                callBackTopicInfo.setStatus(CallBackStatusEnum.FAILED.ordinal());
                callBackTopicInfo.setMessage(DEFAULT_FAILED_MESSAGE);
            } else {
                callBackTopicInfo.setStatus(CallBackStatusEnum.SUCCESS.ordinal());
                callBackTopicInfo.setMessage(message);
            }
            log.info("SyncKeyCallBackTopicInfo : {}", callBackTopicInfo);
            if (messageProducer.producerMessage(TopicBingingName.SYNC_KEY_CALL_BACK_BINGING_NAME, callBackTopicInfo)) {
                log.info("SyncKey CallBack Success...");
            } else {
                log.error("SyncKey CallBack Failed...");
            }
        };
    }
}
