package com.csp.actuator.device;

import cn.hutool.core.collection.CollectionUtil;
import cn.tass.hsm.GHSMAPI;

import com.csp.actuator.device.bean.HsmDeviceDTO;
import com.csp.actuator.device.cache.TassInstanceLocalCache;
import com.csp.actuator.device.exception.DeviceException;
import com.csp.actuator.device.session.GMT0018SDFSession;
import com.csp.actuator.device.session.VenusHsmSession;
import com.csp.actuator.utils.SourceUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 获取设备链接帮助类
 *
 * @author Weijia Jiang
 * @version v1
 * @description 获取设备链接帮助类
 * @date Created in 2023-03-18 11:39
 */
@Slf4j
public class DeviceInstanceHelper {

    /**
     * 获取江南天安的链接对象
     *
     * @param serverHostList 配置信息
     * @return {@link GHSMAPI}
     */
    public static GHSMAPI getTassHSMInstance(List<String> serverHostList) {
        if (CollectionUtil.isEmpty(serverHostList)) {
            return null;
        }
        return TassInstanceLocalCache.get(serverHostList);
    }

    /**
     * 从多个三未的密码机中获取一个链接对象
     *
     * @param serverHostList 链接列表
     * @return {@link GMT0018SDFSession}
     */
    public static GMT0018SDFSession getOneSansecHSMInstance(List<String> serverHostList) {
        return getSansecHSSMInstance(Lists.newArrayList(serverHostList.get(0))).get(0);
    }

    /**
     * 获取三未的链接对象
     *
     * @param serverHostList 配置信息
     * @return {@link GMT0018SDFSession}
     */
    public static List<GMT0018SDFSession> getSansecHSSMInstance(List<String> serverHostList) {
        List<String[]> serverHostInfo = serverHostList.stream().map(info -> {
            String[] serverInfo = StringUtils.split(info, ":");
            if (serverInfo.length != 2) {
                return null;
            }
            return serverInfo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(serverHostInfo)) {
            throw new DeviceException("连接密码设备失败");
        }
        List<GMT0018SDFSession> instanceList = Lists.newArrayList();
        try {
            instanceList =
                    serverHostInfo.stream().map(serverHost -> {
                        GMT0018SDFSession session = new GMT0018SDFSession();
                        HsmDeviceDTO hsm = new HsmDeviceDTO();
                        String ip = serverHost[0];
                        String port = serverHost[1];
                        hsm.setDeviceId(ip + ":" + port);
                        hsm.setIp(ip);
                        hsm.setPort(Integer.valueOf(port));
                        hsm.setIsEnableSslFlag(0);
                        session.instance(hsm);
                        return session;
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Get Sansec Device Instance error, Host Info: {}", serverHostList);
            log.error("Get Sansec Device Instance error, Error Info: {}", e.getMessage());
            throw new DeviceException("连接密码设备失败");
        } finally {
            SourceUtil.removeLocalCertFile();
        }
        return instanceList;
    }

    /**
     * 从多个自研的密码机中获取一个链接对象
     *
     * @param serverHostList 链接列表
     * @return {@link VenusHsmSession}
     */
    public static VenusHsmSession getOneVenusHSMInstance(List<String> serverHostList) {
        return getVenusHSSMInstance(Lists.newArrayList(serverHostList.get(0))).get(0);
    }

    /**
     * 获取自研的链接对象
     *
     * @param serverHostList 配置信息
     * @return {@link VenusHsmSession}
     */
    public static List<VenusHsmSession> getVenusHSSMInstance(List<String> serverHostList) {
        List<String[]> serverHostInfo = serverHostList.stream().map(info -> {
            String[] serverInfo = StringUtils.split(info, ":");
            if (serverInfo.length != 2) {
                return null;
            }
            return serverInfo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(serverHostInfo)) {
            throw new DeviceException("连接密码设备失败");
        }
        List<VenusHsmSession> instanceList = Lists.newArrayList();
        try {
            instanceList =
                    serverHostInfo.stream().map(serverHost -> {
                        VenusHsmSession session = new VenusHsmSession();
                        HsmDeviceDTO hsm = new HsmDeviceDTO();
                        String ip = serverHost[0];
                        String port = serverHost[1];
                        hsm.setDeviceId(ip + ":" + port);
                        hsm.setIp(ip);
                        hsm.setPort(Integer.valueOf(port));
                        hsm.setIsEnableSslFlag(0);
                        session.instance(hsm);
                        return session;
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Get Venus Device Instance error, Host Info: {}", serverHostList);
            log.error("Get Venus Device Instance error, Error Info: {}", e.getMessage());
            throw new DeviceException("连接密码设备失败");
        } finally {
            SourceUtil.removeLocalCertFile();
        }
        return instanceList;
    }
}
