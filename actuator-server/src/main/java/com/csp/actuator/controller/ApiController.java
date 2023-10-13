package com.csp.actuator.controller;

import com.csp.actuator.cache.DataCenterKeyCache;
import com.csp.actuator.constants.BaseConstant;
import com.csp.actuator.entity.ApiResult;
import com.csp.actuator.entity.SoftSignVerifyDTO;
import com.csp.actuator.exception.ActuatorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

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
@RestController
@RequestMapping("/api")
public class ApiController {

    @Value("${kms.secret:0D9Qe30GIMm6oYTcDTuRQgRy7G7R6AMm}")
    private String kekSecret;

    @PostMapping("/getDataCenterKeyInfo")
    public ApiResult<String> getDataCenterKeyInfo(HttpServletRequest request, @Valid @RequestBody SoftSignVerifyDTO param) {
        String value = request.getHeader("secret");
        if (!kekSecret.equals(value)) {
            throw new ActuatorException(ERROR_DATA_CENTER_ID_NOT_FOUND);
        }
        try {
            return ApiResult.success(DataCenterKeyCache.getDataCenterKey4PublicKeyEncrypt(param.getPublicKeyHex()));
        } catch (Exception e) {
            return ApiResult.fail("500", e.getMessage());
        }
    }
}
