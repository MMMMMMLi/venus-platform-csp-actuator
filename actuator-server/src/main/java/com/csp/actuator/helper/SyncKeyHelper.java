package com.csp.actuator.helper;

import cn.hutool.core.collection.CollectionUtil;
import com.csp.actuator.api.entity.GenerateKeyResult;
import com.csp.actuator.api.entity.RemoveKeyInfo;
import com.csp.actuator.api.entity.SyncGenerateKeyInfo;
import com.csp.actuator.api.kms.SyncKeyTopicInfo;
import com.csp.actuator.device.contants.GlobalTypeCodeConstant;
import com.csp.actuator.device.contants.GlobalUsedTypeCodeConstant;
import com.csp.actuator.entity.ImportKeyInfo;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

import static com.csp.actuator.constants.BaseConstant.GLOBAL_LMK_KEYINDEX;

/**
 * 密钥同步接口
 *
 * @author Weijia Jiang
 * @version v1
 * @description 密钥同步接口
 * @date Created in 2023-10-13 11:52
 */

@Slf4j
public class SyncKeyHelper {
    public static GenerateKeyResult syncKey(SyncKeyTopicInfo syncKeyTopicInfo) {
        // 校验设备编码
        CheckHelper.checkDevModelCode(syncKeyTopicInfo.getDevModelCode());
        // 校验两个列表是不是都是空的
        if (CollectionUtil.isEmpty(syncKeyTopicInfo.getSyncDeviceList())
                && CollectionUtil.isEmpty(syncKeyTopicInfo.getFreedDeviceList())) {
            // 都是空的，不执行
            return null;
        }
        // 需要操作的密钥列表是不是都是空的
        if (CollectionUtil.isEmpty(syncKeyTopicInfo.getGenerateKeKInfoList())
                && CollectionUtil.isEmpty(syncKeyTopicInfo.getGenerateKeyInfoList())
                && CollectionUtil.isEmpty(syncKeyTopicInfo.getRemoveKeyInfoList())) {
            // 都是空的，不执行
            return null;
        }
        // 创建Key
        handleSyncDevice(syncKeyTopicInfo);
        // 销毁Key
        handleFreedDevice(syncKeyTopicInfo);
        return null;
    }

    private static GenerateKeyResult handleSyncDevice(SyncKeyTopicInfo syncKeyTopicInfo) {
        // 获取需要操作的设备列表
        List<String> syncDeviceList = syncKeyTopicInfo.getSyncDeviceList();
        if (CollectionUtil.isEmpty(syncDeviceList)) {
            return null;
        }
        // 设备类型编码
        Integer devModelCode = syncKeyTopicInfo.getDevModelCode();
        // 先将需要扩容的机器清空一下密钥
        syncDeviceList.forEach(device -> {
            // 处理一下旧的对称密钥
            removeDeviceOldKey(devModelCode, device, GlobalTypeCodeConstant.SYMMETRIC_KEY, 0, syncKeyTopicInfo.getMaxSymmKeyCount());
            // 处理一下旧的非对称密钥 - 加密
            removeDeviceOldKey(devModelCode, device, GlobalTypeCodeConstant.ECC_KEY, GlobalUsedTypeCodeConstant.KEY_USAGE_CODE_ENCRYPTION, syncKeyTopicInfo.getMaxASymmKeyCount());
            // 处理一下旧的非对称密钥 - 签名
            removeDeviceOldKey(devModelCode, device, GlobalTypeCodeConstant.ECC_KEY, GlobalUsedTypeCodeConstant.KEY_USAGE_CODE_SIGN, syncKeyTopicInfo.getMaxASymmKeyCount());
        });
        // 然后同步一下密钥，先同步KEK，再同步DEK
        List<SyncGenerateKeyInfo> generateKeKInfoList = syncKeyTopicInfo.getGenerateKeKInfoList();
        generateKeKInfoList.forEach(kekInfo -> {
            ImportKeyHelper.importKey(ImportKeyInfo.builder()
                    .keyId(kekInfo.getKeyId()).operation(kekInfo.getOperation())
                    .deviceList(syncDeviceList).devModelCode(devModelCode)
                    .build());
        });
        List<SyncGenerateKeyInfo> generateKeyInfoList = syncKeyTopicInfo.getGenerateKeyInfoList();
        return null;
    }

    private static void removeDeviceOldKey(Integer devModelCode, String device, Integer keyType, Integer keyUsage, Integer keyIndexMaxNums) {
        // 获取密码机内所有的密钥信息
        List<Integer> indexKeyList = DeviceHelper.getDeviceKeyIndexByKeyTypeAndKeyUsage(devModelCode, keyType, keyUsage, Lists.newArrayList(device), keyIndexMaxNums);
        // 日志友好输出
        String logType = GlobalTypeCodeConstant.SYMMETRIC_KEY.equals(keyType) ? "symmetry" :
                GlobalUsedTypeCodeConstant.KEY_USAGE_CODE_ENCRYPTION.equals(keyUsage) ? "asymmetry-enc" : "asymmetry-sign";
        if (CollectionUtil.isNotEmpty(indexKeyList)) {
            // 这里去除掉索引位置小于等于11的对称密钥，这几个不能删除
            indexKeyList.removeIf(n -> n <= GLOBAL_LMK_KEYINDEX);
            // 再次校验
            if (CollectionUtil.isNotEmpty(indexKeyList)) {
                log.info("device [{}] has old {} key, try del: {}", device, logType, indexKeyList);
                // 删除
                DestroyKeyHelper.removeKey(devModelCode, Lists.newArrayList(device),
                        indexKeyList.stream().map(item ->
                                RemoveKeyInfo.builder()
                                        .keyIndex(item)
                                        .globalKeyType(keyType)
                                        .globalKeyUsage(keyUsage).build()
                        ).collect(Collectors.toList())
                );
            } else {
                log.info("device [{}] has old {} key, but it belongs to a preset key and does not need to be deleted...", logType, device);
            }
        } else {
            log.info("device [{}] no has old {} key...", device, logType);
        }
    }

    private static Boolean handleFreedDevice(SyncKeyTopicInfo syncKeyTopicInfo) {
        return DestroyKeyHelper.removeKey(syncKeyTopicInfo.getDevModelCode(), syncKeyTopicInfo.getFreedDeviceList(), syncKeyTopicInfo.getRemoveKeyInfoList());
    }
}
