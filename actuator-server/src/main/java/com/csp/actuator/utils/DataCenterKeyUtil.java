package com.csp.actuator.utils;

import com.csp.actuator.config.DataCenterConfig;
import com.csp.actuator.exception.ActuatorException;
import com.csp.actuator.report.NodeReport;

import java.nio.charset.StandardCharsets;

import static com.csp.actuator.constants.BaseConstant.ERROR_DATA_CENTER_ID_NOT_FOUND;

/**
 * 数据中心Key
 *
 * @author Weijia Jiang
 * @version v1
 * @description 数据中心Key
 * @date Created in 2023-10-11 15:13
 */
public class DataCenterKeyUtil {

    public static byte[] getDataCenterKey() {
        DataCenterConfig centerConfig = SpringUtils.getBean(DataCenterConfig.class);
        if (NodeReport.dataCenterInfoIsError(centerConfig.getId())) {
            throw new ActuatorException(ERROR_DATA_CENTER_ID_NOT_FOUND);
        }
        byte[] oldKey = confuseDataCenter(centerConfig.getId());
        byte[] centerKey = new byte[16];
        System.arraycopy(oldKey, 0, centerKey, 0, 16);
        return centerKey;
    }

    /**
     * 简单的混淆md5字符串
     * 遍历一个字符串，将其中的第一次遍历到的a字符改成b，第一次遍历到的e字符改成0
     *
     * @param dataCenterId 数据中心ID字符串，应该是一个不带-线的uuid，如 de050ae458816d792a9184a9eab0ef02
     * @return byte[]
     */
    public static byte[] confuseDataCenter(String dataCenterId) {
        char[] chars = dataCenterId.toCharArray();
        boolean firstA = true;
        boolean firstE = true;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == 'a' && firstA) {
                chars[i] = 'b';
                firstA = false;
            } else if (chars[i] == 'e' && firstE) {
                chars[i] = '0';
                firstE = false;
            }
        }
        String newString = new String(chars);
        newString = newString + "4X2X8;Qeb*d#_W$?&mz)8#*rgOb6zxDn";
        return SM3Util.hash(newString.getBytes(StandardCharsets.UTF_8));
    }
}
