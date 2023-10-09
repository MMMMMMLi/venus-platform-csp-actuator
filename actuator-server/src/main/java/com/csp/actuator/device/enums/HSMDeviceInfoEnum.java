package com.csp.actuator.device.enums;

import com.csp.actuator.device.factory.HSMFactory;
import com.csp.actuator.device.factory.impl.HSM4SansecImpl;
import com.csp.actuator.device.factory.impl.HSM4TassImpl;
import com.csp.actuator.device.factory.impl.HSM4VenusImpl;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 设备信息枚举
 *
 * @author Weijia Jiang
 * @version v1
 * @description 设备信息枚举
 * @date Created in 2023-03-17 14:10
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum HSMDeviceInfoEnum {


    SANSEC_HSM(1001, HSM4SansecImpl.class),
    VENUS_OEM(2001, HSM4SansecImpl.class),
    VENUS_SELF(2002, HSM4VenusImpl.class),
    LEADSEC_OEM(2001, HSM4SansecImpl.class),
    LEADSEC_SELF(2002, HSM4VenusImpl.class),
    TASS_HSM(4001, HSM4TassImpl.class);

    private Integer code;

    private Class<? extends HSMFactory> desc;

    public static Class<? extends HSMFactory> getDesc(Integer code) {
        HSMDeviceInfoEnum[] deviceInfoEnums = values();
        for (HSMDeviceInfoEnum deviceInfoEnum : deviceInfoEnums) {
            if (deviceInfoEnum.code.compareTo(code) == 0) {
                return deviceInfoEnum.desc;
            }
        }
        return null;
    }
}
