package com.csp.actuator.device.bean;

import com.sun.jna.Pointer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 密码机设备
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HsmDeviceDTO implements Serializable {
    /**
     * 设备唯一id
     */
    private String deviceId;
    /**
     * 设备名称
     */
    private String name;

    /**
     * 设备IP
     */
    private String ip;

    /**
     * 设备端口
     */
    private Integer port;

    /**
     * 设备状态
     */
    private String status;

    /**
     * 连接密码
     */
    private String password;

    /**
     * 签名证书base64
     */
    private String signCertBase64;
    /**
     * 加密证书base64
     */
    private String enCertBase64;
    /**
     * 根证书内容text
     */
    private String rootCert;

    /**
     * 设备句柄
     */
    public Pointer deviceHandle;

    /**
     * 密码机库文件类型 0.三未 1.启明自研
     */
    private Integer hsmSourceType;

    /**
     * 是否启用SSL 0.否 1.是
     */
    private Integer isEnableSslFlag;

    public String getDeviceId() {
        return this.ip + ":" + this.port;
    }

}
