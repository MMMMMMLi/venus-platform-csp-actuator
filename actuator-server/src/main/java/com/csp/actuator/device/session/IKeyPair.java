package com.csp.actuator.device.session;

import com.csp.actuator.device.exception.DeviceException;

public interface IKeyPair {
    void decode(byte[] bArr) throws DeviceException;

    byte[] encode() throws DeviceException;

    int size();
}