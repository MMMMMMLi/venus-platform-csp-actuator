package com.csp.actuator.exception;

/**
 * 执行节点异常
 *
 * @author Weijia Jiang
 * @version v1
 * @description 执行节点异常
 * @date Created in 2023-10-09 10:34
 */
public class ActuatorException extends RuntimeException {
    private final String code;
    private final String message;
    private final Object errT;

    public ActuatorException(String code, String message, Object errT) {
        super(message);
        this.code = code;
        this.message = message;
        this.errT = errT;
    }

    public ActuatorException(String message) {
        this("500", message, (Object) null);
    }

    public ActuatorException(String code, String message) {
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
