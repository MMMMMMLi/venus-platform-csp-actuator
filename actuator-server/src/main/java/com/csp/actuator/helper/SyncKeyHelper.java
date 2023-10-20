package com.csp.actuator.helper;

import cn.hutool.core.collection.CollectionUtil;
import com.csp.actuator.api.constants.KeyInfoKeyConstant;
import com.csp.actuator.api.entity.DeviceIpPortInfo;
import com.csp.actuator.api.entity.DeviceSyncKeyResult;
import com.csp.actuator.api.entity.RemoveKeyInfo;
import com.csp.actuator.api.entity.SyncGenerateKeyInfo;
import com.csp.actuator.api.kms.SyncKeyTopicInfo;
import com.csp.actuator.api.kms.SyncLMKTopicInfo;
import com.csp.actuator.device.contants.GlobalTypeCodeConstant;
import com.csp.actuator.device.contants.GlobalUsedTypeCodeConstant;
import com.csp.actuator.entity.ImportKeyInfo;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
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

    public static DeviceSyncKeyResult syncKey(SyncKeyTopicInfo syncKeyTopicInfo) {
        // 结果集
        DeviceSyncKeyResult.DeviceSyncKeyResultBuilder builder = DeviceSyncKeyResult.builder();
        // 校验设备编码
        CheckHelper.checkDevModelCode(syncKeyTopicInfo.getDevModelCode());
        // 校验两个列表是不是都是空的
        if (CollectionUtil.isEmpty(syncKeyTopicInfo.getSyncDeviceList())
                && CollectionUtil.isEmpty(syncKeyTopicInfo.getFreedDeviceList())) {
            log.info("SyncKey DeviceList is all empty, break...");
            // 都是空的，不执行
            return builder.build();
        }
        // 需要操作的密钥列表是不是都是空的
        if (CollectionUtil.isEmpty(syncKeyTopicInfo.getGenerateKeKInfoList())
                && CollectionUtil.isEmpty(syncKeyTopicInfo.getGenerateKeyInfoList())
                && CollectionUtil.isEmpty(syncKeyTopicInfo.getRemoveKeyInfoList())) {
            log.info("SyncKey KeyList is all empty, break...");
            // 都是空的，不执行
            return builder.build();
        }
        // 创建Key
        DeviceSyncKeyResult syncResult = handleSyncDevice(syncKeyTopicInfo);
        // 销毁Key
        DeviceSyncKeyResult destroyResult = handleFreedDevice(syncKeyTopicInfo);
        return builder
                .succSyncDeviceIdList(syncResult.getSuccSyncDeviceIdList())
                .failSyncDeviceIdList(syncResult.getFailSyncDeviceIdList())
                .succFreedDeviceIdList(destroyResult.getSuccFreedDeviceIdList())
                .failFreedDeviceIdList(destroyResult.getFailFreedDeviceIdList())
                .build();
    }

    /**
     * 处理一下同步的密钥信息
     */
    private static DeviceSyncKeyResult handleSyncDevice(SyncKeyTopicInfo syncKeyTopicInfo) {
        log.info("Start HandleSyncDevice function...");
        // 获取需要操作的设备列表
        List<DeviceIpPortInfo> syncDeviceInfoList = syncKeyTopicInfo.getSyncDeviceList();
        if (CollectionUtil.isEmpty(syncDeviceInfoList)) {
            log.info("HandleSyncDevice DeviceList is empty, break...");
            return new DeviceSyncKeyResult();
        }
        // 设备类型编码
        Integer devModelCode = syncKeyTopicInfo.getDevModelCode();
        // 密钥信息
        List<SyncGenerateKeyInfo> generateKeKInfoList = syncKeyTopicInfo.getGenerateKeKInfoList();
        if (CollectionUtil.isEmpty(generateKeKInfoList)) {
            log.info("HandleSyncDevice generateKeKInfoList is empty, break...");
            return new DeviceSyncKeyResult();
        }
        List<SyncGenerateKeyInfo> generateKeyInfoList = syncKeyTopicInfo.getGenerateKeyInfoList();
        // 同步失败的密码设备
        List<Long> failSyncDeviceIdList = Lists.newArrayList();
        List<Long> succSyncDeviceIdList = Lists.newArrayList();
        // 先将需要扩容的机器清空一下密钥
        syncDeviceInfoList.forEach(device -> {
            String ipPort = device.getIpPort();
            log.info("start handle device :[{}]", ipPort);
            // 获取设备ip端口
            List<String> devicePostList = Lists.newArrayList(ipPort);
            try {
                // 处理一下旧的对称密钥
                removeDeviceAppointOldKey(devModelCode, devicePostList, GlobalTypeCodeConstant.SYMMETRIC_KEY, 0, syncKeyTopicInfo.getMaxSymmKeyCount());
                // 处理一下旧的非对称密钥 - 加密
                removeDeviceAppointOldKey(devModelCode, devicePostList, GlobalTypeCodeConstant.ECC_KEY, GlobalUsedTypeCodeConstant.KEY_USAGE_CODE_ENCRYPTION, syncKeyTopicInfo.getMaxASymmKeyCount());
                // 处理一下旧的非对称密钥 - 签名
                removeDeviceAppointOldKey(devModelCode, devicePostList, GlobalTypeCodeConstant.ECC_KEY, GlobalUsedTypeCodeConstant.KEY_USAGE_CODE_SIGN, syncKeyTopicInfo.getMaxASymmKeyCount());
                log.info("remove old key success...");
                // 然后同步一下密钥，先同步KEK，再同步DEK
                if (CollectionUtil.isNotEmpty(generateKeKInfoList)) {
                    generateKeKInfoList.forEach(kekInfo -> {
                        ImportKeyHelper.importKey(ImportKeyInfo.builder()
                                .keyId(kekInfo.getKeyId()).operation(kekInfo.getOperation())
                                .deviceList(devicePostList).devModelCode(devModelCode)
                                .keyInfo(kekInfo.getKeyInfo())
                                .build());
                    });
                }
                log.info("handle kek success...");
                if (CollectionUtil.isNotEmpty(generateKeyInfoList)) {
                    generateKeyInfoList.forEach(keyInfo -> {
                        ImportKeyHelper.importKey(ImportKeyInfo.builder()
                                .keyId(keyInfo.getKeyId()).operation(keyInfo.getOperation())
                                .deviceList(devicePostList).devModelCode(devModelCode)
                                .keyInfo(keyInfo.getKeyInfo())
                                .build());
                    });
                }
                log.info("handle dek success...");
            } catch (Exception e) {
                log.error("handle device :[{}] , failed :{}", ipPort, e.getMessage());
                failSyncDeviceIdList.add(device.getId());
            }
            succSyncDeviceIdList.add(device.getId());
            log.info("handle device :[{}] , success...", ipPort);
        });
        return DeviceSyncKeyResult.builder().succSyncDeviceIdList(succSyncDeviceIdList).failSyncDeviceIdList(failSyncDeviceIdList).build();
    }

    /**
     * 处理一下销毁密钥信息
     */
    private static DeviceSyncKeyResult handleFreedDevice(SyncKeyTopicInfo syncKeyTopicInfo) {
        DeviceSyncKeyResult.DeviceSyncKeyResultBuilder builder = DeviceSyncKeyResult.builder();
        // 销毁信息
        List<DeviceIpPortInfo> freedDeviceList = syncKeyTopicInfo.getFreedDeviceList();
        if (CollectionUtil.isEmpty(freedDeviceList)) {
            log.info("HandleFreedDevice DeviceList is empty, break...");
            return builder.build();
        }
        // 端口列表
        List<String> ipPortList = freedDeviceList.stream().map(DeviceIpPortInfo::getIpPort).collect(Collectors.toList());
        // ID端口
        List<Long> idList = freedDeviceList.stream().map(DeviceIpPortInfo::getId).collect(Collectors.toList());
        try {
            DestroyKeyHelper.removeKey(syncKeyTopicInfo.getDevModelCode(), ipPortList, syncKeyTopicInfo.getRemoveKeyInfoList());
            builder.succFreedDeviceIdList(idList).failFreedDeviceIdList(Lists.newArrayList());
        } catch (Exception e) {
            log.error("handleFreedDevice error, info :{}", e.getMessage());
            builder.failFreedDeviceIdList(idList).succFreedDeviceIdList(Lists.newArrayList());
        }
        return builder.build();
    }

    /**
     * 删除指定设备内的指定密钥
     */
    public static void removeDeviceAppointOldKey(Integer devModelCode, List<String> devicePostList, Integer keyType, Integer keyUsage, Integer keyIndexMaxNums) {
        // 获取密码机内所有的密钥信息
        List<Integer> indexKeyList = DeviceHelper.getDeviceKeyIndexByKeyTypeAndKeyUsage(devModelCode, keyType, keyUsage, devicePostList, keyIndexMaxNums);
        // 日志友好输出
        String logType = GlobalTypeCodeConstant.SYMMETRIC_KEY.equals(keyType) ? "symmetry" :
                GlobalUsedTypeCodeConstant.KEY_USAGE_CODE_ENCRYPTION.equals(keyUsage) ? "asymmetry-enc" : "asymmetry-sign";
        if (CollectionUtil.isNotEmpty(indexKeyList)) {
            // 这里去除掉索引位置小于等于11的对称密钥，这几个不能删除
            indexKeyList.removeIf(n -> n <= GLOBAL_LMK_KEYINDEX);
            // 再次校验
            if (CollectionUtil.isNotEmpty(indexKeyList)) {
                log.info("device [{}] has old {} key, try del: {}", devicePostList, logType, indexKeyList);
                // 删除
                DestroyKeyHelper.removeKey(devModelCode, devicePostList,
                        indexKeyList.stream().map(item ->
                                RemoveKeyInfo.builder()
                                        .keyIndex(item)
                                        .globalKeyType(keyType)
                                        .globalKeyUsage(keyUsage).build()
                        ).collect(Collectors.toList())
                );
            } else {
                log.info("device [{}] has old {} key, but it belongs to a preset key and does not need to be deleted...", logType, devicePostList);
            }
        } else {
            log.info("device [{}] no has old {} key...", devicePostList, logType);
        }
    }

    public static Boolean syncLMK(SyncLMKTopicInfo lmkTopicInfo) {
        // 校验设备编码
        CheckHelper.checkDevModelCode(lmkTopicInfo.getDevModelCode());
        // 校验设备信息
        CheckHelper.checkDeviceInfo(lmkTopicInfo.getDeviceList());
        // 校验密钥信息
        CheckHelper.checkKeyInfo(lmkTopicInfo.getKeyInfo());
        // 给个默认值
        Map<String, Object> keyInfo = lmkTopicInfo.getKeyInfo();
        keyInfo.put(KeyInfoKeyConstant.KEY_CV, "");
        return ImportKeyHelper.importSymmetricKey(ImportKeyInfo.builder()
                .keyId("")
                .devModelCode(lmkTopicInfo.getDevModelCode())
                .deviceList(lmkTopicInfo.getDeviceList())
                .keyInfo(keyInfo)
                .build());
    }
}
