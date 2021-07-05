package com.tcl.graph.call;

import com.tcl.entity.MethodSignature;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Original call edge, dynamic dispatch is not considered
 */
public class CallEdge {
    @Nonnull
    public final MethodSignature callee;
    @Nonnull
    public final MethodSignature caller;

    public CallEdge(@Nonnull MethodSignature callee,
                    @Nonnull MethodSignature caller) {
        this.callee = callee;
        this.caller = caller;
    }

    @Override
    public String toString() {
        return "(callee: " + callee + ", caller: " + caller + ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallEdge callEdge = (CallEdge) o;
        return callee.equals(callEdge.callee)
                && caller.equals(callEdge.caller);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callee, caller);
    }
}
