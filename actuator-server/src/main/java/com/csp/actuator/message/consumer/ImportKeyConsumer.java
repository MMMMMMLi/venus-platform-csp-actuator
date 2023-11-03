package com.csp.actuator.message.consumer;

import com.csp.actuator.api.entity.DeviceSyncKeyResult;
import com.csp.actuator.api.enums.CallBackStatusEnum;
import com.csp.actuator.api.kms.ImportKeyCallbackTopicInfo;
import com.csp.actuator.api.kms.ImportKeyTopicInfo;
import com.csp.actuator.api.kms.SyncLMKCallbackTopicInfo;
import com.csp.actuator.api.kms.SyncLMKTopicInfo;
import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.helper.ImportKeyHelper;
import com.csp.actuator.helper.SyncKeyHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.csp.actuator.constants.ErrorMessage.DEFAULT_SUCCESS_MESSAGE;
import static com.csp.actuator.constants.ErrorMessage.ERROR_DATA_FORMAT_FAILED;

/**
 * 密钥导入Consumer
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥导入Consumer
 * @date Created in 2023-10-12 18:02
 */
@Slf4j
public class ImportKeyConsumer {

    public static ImportKeyCallbackTopicInfo importSm2Dek(String msg) {
        log.info("importSm2Dek msg: {}", msg);
        String message = DEFAULT_SUCCESS_MESSAGE;
        // 返回实体
        ImportKeyCallbackTopicInfo callBackTopicInfo = new ImportKeyCallbackTopicInfo();
        callBackTopicInfo.setDate(System.currentTimeMillis());
        // 格式化密钥
        ImportKeyTopicInfo importKeyTopicInfo = JsonUtils.readValue(msg, ImportKeyTopicInfo.class);
        log.info("ImportKeyTopicInfo :{}", importKeyTopicInfo);
        if (Objects.isNull(importKeyTopicInfo)) {
            return callBackTopicInfo.setMessage(ERROR_DATA_FORMAT_FAILED)
                    .setStatus(CallBackStatusEnum.FAILED.ordinal());
        }
        // 执行创建操作
        Boolean flag = Boolean.FALSE;
        try {
            flag = ImportKeyHelper.batchImportSM2Key(importKeyTopicInfo);
        } catch (Exception e) {
            log.error("ImportKeyCallbackTopicInfo failed, e: {}", message = e.getMessage());
        }
        if (flag) {
            callBackTopicInfo.setStatus(CallBackStatusEnum.SUCCESS.ordinal());
        } else {
            callBackTopicInfo.setStatus(CallBackStatusEnum.FAILED.ordinal());
        }
        callBackTopicInfo.setMessage(message);
        log.info("ImportKeyCallbackTopicInfo : {}", callBackTopicInfo);
        return callBackTopicInfo;
    }

    public static ImportKeyCallbackTopicInfo importSymmetricDek(String msg) {
        log.info("importSymmetricDek msg: {}", msg);
        String message = DEFAULT_SUCCESS_MESSAGE;
        // 返回实体
        ImportKeyCallbackTopicInfo callBackTopicInfo = new ImportKeyCallbackTopicInfo();
        callBackTopicInfo.setDate(System.currentTimeMillis());
        // 格式化密钥
        ImportKeyTopicInfo importKeyTopicInfo = JsonUtils.readValue(msg, ImportKeyTopicInfo.class);
        log.info("ImportKeyTopicInfo :{}", importKeyTopicInfo);
        if (Objects.isNull(importKeyTopicInfo)) {
            return callBackTopicInfo.setMessage(ERROR_DATA_FORMAT_FAILED)
                    .setStatus(CallBackStatusEnum.FAILED.ordinal());
        }
        // 执行创建操作
        Boolean flag = Boolean.FALSE;
        try {
            flag = ImportKeyHelper.batchImportSymmetricDek(importKeyTopicInfo);
        } catch (Exception | Error e) {
            e.printStackTrace();
            log.error("ImportKeyCallbackTopicInfo failed, e: {}", message = e.getMessage());
        }
        if (flag) {
            callBackTopicInfo.setStatus(CallBackStatusEnum.SUCCESS.ordinal());
        } else {
            callBackTopicInfo.setStatus(CallBackStatusEnum.FAILED.ordinal());
        }
        callBackTopicInfo.setMessage(message);
        log.info("ImportKeyCallbackTopicInfo : {}", callBackTopicInfo);
        return callBackTopicInfo;
    }
}
