package com.tcl.json;

import com.tcl.graph.call.ChainEntry;

public class ChainEntryJson {
    private MethodJson method;
    private boolean handled;

    public ChainEntryJson(ChainEntry entry) {
        method = new MethodJson(entry.getMethod());
        handled = entry.isHandled();
    }

    public MethodJson getMethod() {
        return method;
    }

    public void setMethod(MethodJson method) {
        this.method = method;
    }

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }
}
