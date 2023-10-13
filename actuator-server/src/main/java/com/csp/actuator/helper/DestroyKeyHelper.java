package com.csp.actuator.helper;

import com.csp.actuator.api.entity.RemoveKeyInfo;
import com.csp.actuator.api.kms.DestroyKeyTopicInfo;
import com.csp.actuator.device.FactoryBuilder;
import com.csp.actuator.device.factory.HSMFactory;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * 销毁密钥Helper
 *
 * @author Weijia Jiang
 * @version v1
 * @description 销毁密钥Helper
 * @date Created in 2023-10-11 17:25
 */
@Slf4j
public class DestroyKeyHelper {

    public static Boolean destroyKey(DestroyKeyTopicInfo destroyKeyTopicInfo) {
        // 设备编码
        Integer devModelCode = destroyKeyTopicInfo.getDevModelCode();
        // 校验设备编码
        CheckHelper.checkDevModelCode(devModelCode);
        // 校验设备信息
        CheckHelper.checkDeviceInfo(destroyKeyTopicInfo.getDeviceList());
        // 校验密钥信息
        CheckHelper.checkDestroyKeyInfo(destroyKeyTopicInfo.getRemoveKeyInfo());
        // 执行
        return removeKey(devModelCode, destroyKeyTopicInfo.getDeviceList(), Lists.newArrayList(destroyKeyTopicInfo.getRemoveKeyInfo()));
    }

    public static Boolean removeKey(Integer devModelCode, List<String> devicePostList, List<RemoveKeyInfo> removeKeyInfoList) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(devModelCode);
        if (Objects.isNull(hsmImpl)) {
            return false;
        }
        return hsmImpl.removeKey(devicePostList, removeKeyInfoList);
    }
}
