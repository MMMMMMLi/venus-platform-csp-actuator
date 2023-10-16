package com.csp.actuator.message.consumer;

import com.csp.actuator.api.entity.DeviceSyncKeyResult;
import com.csp.actuator.api.enums.CallBackStatusEnum;
import com.csp.actuator.api.kms.SyncKeyCallBackTopicInfo;
import com.csp.actuator.api.kms.SyncKeyTopicInfo;
import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.helper.SyncKeyHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static com.csp.actuator.constants.ErrorMessage.DEFAULT_SUCCESS_MESSAGE;
import static com.csp.actuator.constants.ErrorMessage.ERROR_DATA_FORMAT_FAILED;

/**
 * 密钥同步Consumer
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥同步Consumer
 * @date Created in 2023-10-12 18:02
 */
@Slf4j
public class SyncKeyConsumer {

    public static SyncKeyCallBackTopicInfo syncKey(String msg) {
        log.info("SyncKey msg: {}", msg);
        String message = DEFAULT_SUCCESS_MESSAGE;
        // 返回实体
        SyncKeyCallBackTopicInfo callBackTopicInfo = new SyncKeyCallBackTopicInfo();
        callBackTopicInfo.setDate(System.currentTimeMillis());
        // 格式化密钥
        SyncKeyTopicInfo syncKeyTopicInfo = JsonUtils.readValue(msg, SyncKeyTopicInfo.class);
        log.info("SyncKeyTopicInfo :{}", syncKeyTopicInfo);
        if (Objects.isNull(syncKeyTopicInfo)) {
            return callBackTopicInfo.setMessage(ERROR_DATA_FORMAT_FAILED)
                    .setDeviceSyncKeyResult(new DeviceSyncKeyResult())
                    .setStatus(CallBackStatusEnum.FAILED.ordinal());
        }
        // 执行创建操作
        DeviceSyncKeyResult deviceSyncKeyResult = null;
        try {
            deviceSyncKeyResult = SyncKeyHelper.syncKey(syncKeyTopicInfo);
        } catch (Exception e) {
            log.error("GenerateKey failed, e: {}", message = e.getMessage());
        }
        if (Objects.isNull(deviceSyncKeyResult)) {
            callBackTopicInfo.setStatus(CallBackStatusEnum.FAILED.ordinal());
            callBackTopicInfo.setDeviceSyncKeyResult(new DeviceSyncKeyResult());
        } else {
            callBackTopicInfo.setStatus(CallBackStatusEnum.SUCCESS.ordinal());
            callBackTopicInfo.setDeviceSyncKeyResult(deviceSyncKeyResult);
        }
        callBackTopicInfo.setMessage(message);
        log.info("SyncKeyCallBackTopicInfo : {}", callBackTopicInfo);
        return callBackTopicInfo;
    }
}
