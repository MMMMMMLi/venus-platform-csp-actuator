package com.csp.actuator.helper;

import cn.hutool.core.collection.CollectionUtil;
import com.csp.actuator.device.FactoryBuilder;
import com.csp.actuator.device.contants.VendorConstant;
import com.csp.actuator.device.factory.HSMFactory;
import com.csp.actuator.exception.ActuatorException;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static com.csp.actuator.constants.BaseConstant.ERROR_KEY_NO_KEY_INDEX;
import static com.csp.actuator.constants.BaseConstant.GLOBAL_LMK_KEYINDEX;

/**
 * 设备接口
 *
 * @author Weijia Jiang
 * @version v1
 * @description 设备接口
 * @date Created in 2023-10-13 12:01
 */
@Slf4j
public class DeviceHelper {

    /**
     * 获取可用一个密钥值
     */
    public static Integer getOneAvailableKeyIndexList(Integer devModelCode, int globalKeyType, int globalUseType, List<String> devicePostList, int maxIndex) {
        List<Integer> availableKeyIndexList = getAvailableKeyIndexList(devModelCode, globalKeyType, globalUseType, devicePostList, maxIndex, 1);
        if (CollectionUtil.isEmpty(availableKeyIndexList) || CollectionUtil.isEmpty(availableKeyIndexList.stream().filter(Objects::nonNull).collect(Collectors.toList()))) {
            throw new ActuatorException(ERROR_KEY_NO_KEY_INDEX);
        }
        Integer integer = availableKeyIndexList.get(0);
        log.info("Available KeyIndex is: {}", integer);
        return integer;
    }

    /**
     * 获取指定数量的可用的密钥索引
     */
    public static List<Integer> getAvailableKeyIndexList(Integer devModelCode, int globalKeyType, int globalUseType, List<String> devicePostList, int maxIndex, int size) {
        // 获取密码机已经用的密钥索引
        List<Integer> indexList = getDeviceKeyIndexByKeyTypeAndKeyUsage(devModelCode, globalKeyType, globalUseType, devicePostList, maxIndex);
        // 前10个和lmk的索引位置空出来，不允许使用----因为预留给平台密码机了
        // 这里有可能会浪费一个索引位置，因为有些密码机不需要LMK，等后面再说吧，浪费就浪费了
        for (int i = 0; i < GLOBAL_LMK_KEYINDEX; i++) {
            indexList.add(i + 1);
        }
        if (maxIndex == indexList.size()) {
            throw new ActuatorException(ERROR_KEY_NO_KEY_INDEX);
        }
        // 返回的密钥索引，通过随机生成，然后判断是否为密码机里已被占用的，从而找到仍未被使用的密钥索引
        List<Integer> result = Lists.newArrayList();
        Random rand = new Random();
        for (int i = 0; i < size; i++) {
            int num = rand.nextInt(maxIndex) + 1;
            while (CollectionUtil.contains(indexList, num)) {
                num = rand.nextInt(maxIndex) + 1;
            }
            result.add(num);
            indexList.add(num);
        }
        return result;
    }

    public static List<Integer> getDeviceKeyIndexByKeyTypeAndKeyUsage(Integer devModelCode, int globalKeyType, int globalUseType, List<String> devicePostList, int maxIndex) {
        if (devModelCode.equals(VendorConstant.TASS)) {
            // 江南天安密码机，一次返回1500个，所以最多两次就结束了
            return getDeviceKeyIndexWithBatchSize(devModelCode, globalKeyType, globalUseType, devicePostList, 1500, maxIndex);
        }
        if (devModelCode.equals(VendorConstant.VENUS_SELF)) {
            // 启明自研密码机，对称密钥最多3000个，非对称密钥最多6000个，一次取1000个，
            return getDeviceKeyIndexWithBatchSize(devModelCode, globalKeyType, globalUseType, devicePostList, 1000, maxIndex);
        }
        return Lists.newArrayList();
    }

    /**
     * 获取设备里面的所有已用密钥
     */
    public static List<Integer> getDeviceKeyIndexWithBatchSize(Integer devModelCode, int globalKeyType, int globalUseType, List<String> devicePostList, int batchSize, int maxIndex) {
        List<Integer> indexList = Lists.newArrayList();
        // 第一次，从1号索引位往后取
        List<Integer> singleList = Optional.ofNullable(listKeysByDeviceGlobalKeyType(devModelCode, globalKeyType, globalUseType, 1, devicePostList)).orElse(Lists.newArrayList());
        if (CollectionUtil.isNotEmpty(singleList)) {
            indexList.addAll(singleList);
            // 取满了1000条，那么说明后面还有，还得继续取
            int nowStartIndex = indexList.get(indexList.size() - 1);
            while (singleList.size() == batchSize && indexList.size() < maxIndex && nowStartIndex < maxIndex) {
                // 获取当前的indexList里的最大索引位置
                nowStartIndex = indexList.remove(indexList.size() - 1);

                singleList = Optional.ofNullable(listKeysByDeviceGlobalKeyType(devModelCode, globalKeyType, globalUseType,
                        nowStartIndex, devicePostList)).orElse(Lists.newArrayList());

                indexList.addAll(singleList);
            }
        }
        return indexList;
    }

    /**
     * 获取设备内的所有的密钥索引
     *
     * @param devModelCode   设备类型编码
     * @param devicePostList 设备信息
     * @return 结果集
     */
    public static List<Integer> listKeysByDeviceGlobalKeyType(Integer devModelCode, int globalKeyType, int globalKeyUsage, int startKey, List<String> devicePostList) {
        // 获取出指定厂商服务器密码机实现类
        HSMFactory hsmImpl = FactoryBuilder.getHsmImpl(devModelCode);
        if (Objects.isNull(hsmImpl)) {
            return null;
        }
        return hsmImpl.listKeys(globalKeyType, globalKeyUsage, startKey, devicePostList);
    }
}
