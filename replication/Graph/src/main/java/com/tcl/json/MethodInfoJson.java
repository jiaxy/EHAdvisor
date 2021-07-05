package com.tcl.json;

import com.tcl.entity.MethodInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MethodInfoJson {
    private MethodJson method;
    private String javaDoc;
    private List<String> paramNames;
    private boolean exceptionSource;

    public MethodInfoJson(MethodInfo info, boolean exceptionSource) {
        method = new MethodJson(info.signature);
        javaDoc = info.javaDoc;
        paramNames = info.paramNames;
        this.exceptionSource = exceptionSource;
    }

    public MethodJson getMethod() {
        return method;
    }

    public void setMethod(MethodJson method) {
        this.method = method;
    }

    public Optional<String> getJavaDoc() {
        return Optional.ofNullable(javaDoc);
    }

    public void setJavaDoc(String javaDoc) {
        this.javaDoc = javaDoc;
    }

    public List<String> getParamNames() {
        return paramNames;
    }

    public void setParamNames(List<String> paramNames) {
        this.paramNames = paramNames;
    }

    public boolean isExceptionSource() {
        return exceptionSource;
    }

    public void setExceptionSource(boolean exceptionSource) {
        this.exceptionSource = exceptionSource;
    }
}
