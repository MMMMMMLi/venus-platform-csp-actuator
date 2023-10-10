package com.csp.actuator.device;

import com.csp.actuator.device.enums.HSMDeviceInfoEnum;
import com.csp.actuator.device.factory.HSMFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * 实体类生成器
 *
 * @author Weijia Jiang
 * @version v1
 * @description 实体类生成器
 * @date Created in 2023-03-17 17:26
 */
@Slf4j
public class FactoryBuilder {

    /**
     * 获取服务器密码机实体类
     *
     * @param devModelCode 模板编码
     * @return 具体实现
     */
    public static HSMFactory getHsmImpl(Integer devModelCode) {
        // 根据厂商名字获取具体的实现类
        Class<? extends HSMFactory> deviceClass = HSMDeviceInfoEnum.getDesc(devModelCode);
        // 校验
        if (Objects.isNull(deviceClass)) {
            log.error("getDeviceClass error, devModelCode is {}", devModelCode);
            return null;
        }
        try {
            log.info("HSMFactory getHsmImpl success, vendorName:{}, deviceClass:{}", devModelCode, deviceClass.getName());
            return deviceClass.newInstance();
        } catch (Exception e) {
            log.error("newInstance error, vendorName is {}", devModelCode);
            return null;
        }
    }
}
