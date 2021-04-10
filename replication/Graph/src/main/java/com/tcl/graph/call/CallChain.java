package com.tcl.graph.call;

import com.tcl.entity.MethodSignature;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CallChain {
    private final MethodSignature throwFrom;
    private String exception;
    private final List<ChainEntry> chain;

    public CallChain(@Nonnull MethodSignature throwFrom,
                     @Nonnull List<ChainEntry> chain) {
        this.throwFrom = throwFrom;
        this.chain = chain;
    }

    public CallChain(@Nonnull CallChain other) {
        throwFrom = other.throwFrom;
        exception = other.exception;
        chain = new ArrayList<>();
        other.chain.forEach(entry -> chain.add(new ChainEntry(entry)));
    }

    @Override
    public String toString() {
        return "CallChain{" +
                "throwFrom=" + throwFrom +
                ", exception='" + exception + '\'' +
                ", chain=" + chain +
                '}';
    }

    @Nonnull
    public MethodSignature getThrowFrom() {
        return throwFrom;
    }

    public void setException(@Nonnull String exception) {
        this.exception = exception;
    }

    @Nonnull
    public String getException() {
        Objects.requireNonNull(exception, "exception");
        return exception;
    }

    @Nonnull
    public List<ChainEntry> getChain() {
        return chain;
    }
}
