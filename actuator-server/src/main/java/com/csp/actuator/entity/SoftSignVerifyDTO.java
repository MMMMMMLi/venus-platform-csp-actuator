package com.csp.actuator.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Description 软算法外部密钥签名验证
 * @Author panlin.li
 * @Date 2023/8/17 18:46
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SoftSignVerifyDTO implements Serializable {
    private String algorithm;
    private String publicKeyHex;
    private String signValue;
    private String data;
}
