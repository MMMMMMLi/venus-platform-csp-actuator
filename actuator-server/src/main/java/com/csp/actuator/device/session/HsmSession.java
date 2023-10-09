package com.csp.actuator.device.session;


import com.csp.actuator.device.bean.HsmDeviceDTO;

import java.util.Map;


public interface HsmSession {
    /**
     * 初始化设备信息 </br>
     *
     * @param hsm 设备</br>
     */
    HsmSession instance(HsmDeviceDTO hsm);

    /**
     * 执行设备指令 </br>
     *
     * @param data 发送数据
     * @return </br>
     */
    Object execute(Map<String, Object> data);

    /**
     * 忙碌状态 </br>
     *
     * @return </br>
     */
    boolean isBusy();

    /**
     * 置为BUSY状态 </br>  </br>
     */
    void open();

    /**
     * 置为IDLE状态 </br>  </br>
     */
    void close();

    /**
     * 销毁会话 </br>
     */
    void destroySession();

    /**
     * 销毁设备 </br>
     */
    void destroyHsm();

    /**
     * 测试 </br>
     */
    boolean test();

    /**
     * 密码机厂商类型
     * @return
     */
    Integer getHsmSessionType();
}
