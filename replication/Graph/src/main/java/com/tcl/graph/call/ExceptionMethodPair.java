package com.tcl.graph.call;

import java.util.Objects;

public class ExceptionMethodPair {
    private final String exception;
    private final String simpleMethod;

    public ExceptionMethodPair(String exception, String simpleMethod) {
        this.exception = exception;
        this.simpleMethod = simpleMethod;
    }

    public String getException() {
        return exception;
    }

    public String getSimpleMethod() {
        return simpleMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExceptionMethodPair that = (ExceptionMethodPair) o;
        return Objects.equals(exception, that.exception) && Objects.equals(simpleMethod, that.simpleMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exception, simpleMethod);
    }

    @Override
    public String toString() {
        return "ExceptionMethodPair{" +
                "exception='" + exception + '\'' +
                ", simpleMethod='" + simpleMethod + '\'' +
                '}';
    }
}
