package com.csp.actuator.device.cache;

import cn.hutool.core.collection.CollectionUtil;
import cn.tass.exceptions.TAException;
import cn.tass.hsm.GHSMAPI;
import cn.tass.hsm.Host;
import cn.tass.hsm.LogConfig;
import com.csp.actuator.device.exception.DeviceException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.naming.ConfigurationException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.csp.actuator.device.contants.HSMConstant.*;


/**
 * 江南天安本地缓存
 *
 * @author Weijia Jiang
 * @version v1
 * @description 江南天安本地缓存
 * @date Created in 2023-04-26 9:51
 */
@Slf4j
public class TassInstanceLocalCache {
    // 定义缓存最大容量为64
    private final static int MAX_CACHE_SIZE = 64;

    // 用于存储缓存数据的 ConcurrentHashMap
    private static final ConcurrentHashMap<String, GHSMAPI> cacheMap = new ConcurrentHashMap<>(MAX_CACHE_SIZE);

    // 用于标记是否有数据存入
    private static volatile boolean hasData = false;

    private static LogConfig TASS_LOGCONFIG = null;

    static {
        TASS_LOGCONFIG = new LogConfig("error", "D:\\opt\\device");
    }

    private TassInstanceLocalCache() {
    }

    public static synchronized GHSMAPI get(List<String> serverHostList) {
        Set<String> ipSet = Sets.newHashSet();
        // 设备配置信息
        List<Host> hostList = serverHostList.stream().map(info -> {
            String[] serverInfo = StringUtils.split(info, ":");
            if (serverInfo.length != 2) {
                return null;
            }
            ipSet.add(serverInfo[0]);
            return new Host(TASS_HSM_MODEL, DEFAULT_LINK_NUM, serverInfo[0], Integer.valueOf(serverInfo[1]), DEFAULT_TINE_OUT);
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(hostList)) {
            log.error("Get Tass Device Instance error, Host Info: {}", serverHostList);
            throw new DeviceException("连接密码设备失败");
        }
        // 构建key
        List<String> keyList = Lists.newArrayList(ipSet);
        Collections.sort(keyList);
        String cacheKey = StringUtils.join(keyList, "-");
        // 构建实例链接
        GHSMAPI instance = null;
        try {
            if (hasData) {
                instance = cacheMap.get(cacheKey);
            }
            if (instance == null) {
                instance = GHSMAPI.getInstance(hostList, TASS_LOGCONFIG);
                cacheMap.put(cacheKey, instance);
                hasData = true;
            }
        } catch (ConfigurationException | TAException e) {
            log.error("Get Tass Device Instance error, Host Info: {}", serverHostList);
            log.error("Get Tass Device Instance error, Error Info: {}", e.getMessage());
            throw new DeviceException("连接密码设备失败");
        }
        return instance;
    }
}
