package com.csp.actuator.controller;

import com.csp.actuator.api.utils.JsonUtils;
import com.csp.actuator.cache.DataCenterKeyCache;
import com.csp.actuator.entity.ApiResult;
import com.csp.actuator.entity.SoftSignVerifyDTO;
import com.csp.actuator.message.consumer.DestroyKeyConsumer;
import com.csp.actuator.message.consumer.GenerateKeyConsumer;
import com.csp.actuator.message.consumer.ImportKeyConsumer;
import com.csp.actuator.message.consumer.SyncKeyConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static com.csp.actuator.constants.ErrorMessage.ERROR_DATA_CENTER_ID_NOT_FOUND;

/**
 * 接口Controller
 *
 * @author Weijia Jiang
 * @version v1
 * @description 接口Controller
 * @date Created in 2023-10-13 18:06
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    @Value("${kms.secret:0D9Qe30GIMm6oYTcDTuRQgRy7G7R6AMm}")
    private String kekSecret;

    @PostMapping("/getDataCenterKeyInfo")
    public ApiResult<String> getDataCenterKeyInfo(HttpServletRequest request, @Valid @RequestBody SoftSignVerifyDTO param) {
        log.info("GetDataCenterKeyInfo param: {}", param);
        String value = request.getHeader("secret");
        if (!kekSecret.equals(value)) {
            return ApiResult.fail("500", ERROR_DATA_CENTER_ID_NOT_FOUND);
        }
        try {
            return ApiResult.success(DataCenterKeyCache.getDataCenterKey4PublicKeyEncrypt(param.getPublicKeyHex()));
        } catch (Exception e) {
            return ApiResult.fail("500", e.getMessage());
        }
    }

    @PostMapping("/generateKey")
    public String generateKey(@RequestBody String msg) {
        return JsonUtils.writeValueAsString(GenerateKeyConsumer.generateKey(msg));
    }

    @PostMapping("/destroyKey")
    public String destroyKey(@RequestBody String msg) {
        return JsonUtils.writeValueAsString(DestroyKeyConsumer.destroyKey(msg));
    }

    @PostMapping("/syncKey")
    public String syncKey(@RequestBody String msg) {
        return JsonUtils.writeValueAsString(SyncKeyConsumer.syncKey(msg));
    }

    @PostMapping("/syncLMK")
    public String syncLMK(@RequestBody String msg) {
        return JsonUtils.writeValueAsString(SyncKeyConsumer.syncLMK(msg));
    }

    @PostMapping("/importSm2Dek")
    public String importSm2Dek(@RequestBody String msg) {
        return JsonUtils.writeValueAsString(ImportKeyConsumer.importSm2Dek(msg));
    }

    @PostMapping("/importSymmetricDek")
    public String importSymmetricDek(@RequestBody String msg) {
        return JsonUtils.writeValueAsString(ImportKeyConsumer.importSymmetricDek(msg));
    }
}
