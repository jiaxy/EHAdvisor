package com.tcl.graph.call;

import com.tcl.entity.MethodSignature;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Call edges which considered dynamic dispatch
 */
public class CallEdgeDyn {
    @Nonnull
    public final MethodSignature callee;
    @Nonnull
    public final MethodSignature originalCallee;
    @Nonnull
    public final MethodSignature caller;

    public CallEdgeDyn(@Nonnull MethodSignature callee,
                       @Nonnull MethodSignature originalCallee,
                       @Nonnull MethodSignature caller) {
        this.callee = callee;
        this.originalCallee = originalCallee;
        this.caller = caller;
    }

    @Override
    public String toString() {
        return "(callee: " + callee +
                ", original callee: " + originalCallee +
                ", caller: " + caller + ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallEdgeDyn other = (CallEdgeDyn) o;
        return callee.equals(other.callee)
                && originalCallee.equals(other.originalCallee)
                && caller.equals(other.caller);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callee, originalCallee, caller);
    }
}
