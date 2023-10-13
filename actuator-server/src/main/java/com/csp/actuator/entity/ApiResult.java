package com.csp.actuator.entity;

import cn.hutool.core.util.ObjectUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API接口统一返回
 *
 * @author lipanlin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    public static final String SUCCESS_CODE = "0";

    private String code;
    private T data;
    private String msg;
    private String traceId;
    private long time;

    public ApiResult(String code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
        this.time = System.currentTimeMillis();
    }

    public static <T> ApiResult<T> success() {
        return new ApiResult<>();
    }


    public static <T> ApiResult<T> success(T data) {
        return success(data, "操作成功");
    }

    public static <T> ApiResult<T> success(T data, String msg) {
        return new ApiResult<>(SUCCESS_CODE, data, msg);
    }

    public static <T> ApiResult<T> fail(String code, String msg) {
        return new ApiResult<>(code, null, msg);
    }
}