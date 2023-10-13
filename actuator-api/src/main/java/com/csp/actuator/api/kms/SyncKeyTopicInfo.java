package com.csp.actuator.api.kms;

import com.csp.actuator.api.base.DataCenterInfo;
import com.csp.actuator.api.entity.RemoveKeyInfo;
import com.csp.actuator.api.entity.SyncGenerateKeyInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 密钥同步Topic实体
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥同步Topic实体
 * @date Created in 2023-10-13 10:33
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SyncKeyTopicInfo extends DataCenterInfo {

    /**
     * 设备类型编码
     */
    private Integer devModelCode;

    /**
     * 需要创建密钥的设备列表
     */
    private List<String> syncDeviceList;

    /**
     * 需要销毁密钥的设备列表
     */
    private List<String> freedDeviceList;

    /**
     * 设备内最大的非对称密钥数量
     */
    private Integer maxASymmKeyCount;

    /**
     * 设备内最大的对称密钥数量
     */
    private Integer maxSymmKeyCount;

    /**
     * 需要创建KEK的密钥信息
     */
    private List<SyncGenerateKeyInfo> generateKeKInfoList;

    /**
     * 需要创建的密钥信息
     */
    private List<SyncGenerateKeyInfo> generateKeyInfoList;

    /**
     * 需要销毁的密钥信息
     */
    private List<RemoveKeyInfo> removeKeyInfoList;
}
