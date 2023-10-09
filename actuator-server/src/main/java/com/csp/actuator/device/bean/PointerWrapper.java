package com.csp.actuator.device.bean;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

public class PointerWrapper implements AutoCloseable {
    private Pointer pointer;

    public PointerWrapper(int size) {
        this.pointer = new Memory(size);
    }

    public Pointer getPointer() {
        return this.pointer;
    }

    public void close() {
        if (this.pointer != null) {
            this.pointer = null;
        }

    }
}