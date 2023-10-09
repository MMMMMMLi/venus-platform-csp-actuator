package com.csp.actuator.device.session;


import com.csp.actuator.device.enums.SessionState;

public abstract class AbstractHsmSession implements HsmSession {
    // 会话状态：IDLE 空闲，BUSY 忙碌
    protected SessionState state = SessionState.IDLE;

    @Override
    public void open() {
        state = SessionState.BUSY;
    }

    @Override
    public void close() {
        state = SessionState.IDLE;
    }

    @Override
    public boolean isBusy() {
        return state == SessionState.BUSY;
    }

}
