package com.csp.actuator.device.exception;

/**
 * 设备异常
 *
 * @author Weijia Jiang
 * @version v1
 * @description 设备异常
 * @date Created in 2023-10-09 10:34
 */
public class DeviceException extends RuntimeException {
    private final String code;
    private final String message;
    private final Object errT;

    public DeviceException(String code, String message, Object errT) {
        super(message);
        this.code = code;
        this.message = message;
        this.errT = errT;
    }

    public DeviceException(String message) {
        this("500", message, (Object) null);
    }

    public DeviceException(String code, String message) {
        this(code, message, (Object) null);
    }

    public String getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public Object getErrT() {
        return this.errT;
    }
}
