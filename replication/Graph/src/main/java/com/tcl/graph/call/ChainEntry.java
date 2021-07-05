package com.tcl.graph.call;

import com.tcl.entity.MethodSignature;

import javax.annotation.Nonnull;

public class ChainEntry {
    final private MethodSignature method;
    private boolean handled;

    public ChainEntry(@Nonnull MethodSignature method, boolean handled) {
        this.method = method;
        this.handled = handled;
    }

    public ChainEntry(@Nonnull ChainEntry other) {
        method = other.method;
        handled = other.handled;
    }

    @Override
    public String toString() {
        return "ChainEntry{" +
                "method=" + method +
                ", handled=" + handled +
                '}';
    }

    @Nonnull
    public MethodSignature getMethod() {
        return method;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

    public boolean isHandled() {
        return handled;
    }
}
