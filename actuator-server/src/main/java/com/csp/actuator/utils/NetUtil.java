package com.csp.actuator.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * 网络
 *
 * @author Weijia Jiang
 * @version v1
 * @description 网络
 * @date Created in 2023-10-20 10:28
 */
@Slf4j
public class NetUtil {

    /**
     * 获取本机ip
     *
     * @return String
     */
    public static String getLocalHostAddr() {
        Enumeration allNetInterfaces;
        Vector<String> ipAddr = new Vector<>();
        String ipLocalAddr = "127.0.0.1";
        InetAddress ip = null;
        try {
            allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    ip = (InetAddress) addresses.nextElement();
                    ipAddr.add(ip.toString());
                    // IP是ipv4，ipv6换成Inet6Address
                    if (ip != null && ip instanceof Inet4Address) {
                        String hostAddress = ip.getHostAddress();
                        if (!hostAddress.equals("127.0.0.1") && !hostAddress.equals("/127.0.0.1")) {
                            // 得到本地IP
                            ipLocalAddr = ip.toString().split("[/]")[1];
                        }
                    }
                }
            }
        } catch (SocketException e) {
            log.error("SocketException：", e);
        }
        log.info("最终结果ipLocalAddr = {}", ipLocalAddr);
        return ipLocalAddr;
    }
}
