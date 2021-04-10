package com.tcl.json;

import com.tcl.entity.MethodInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MethodInfoJson {
    private MethodJson method;
    private String javaDoc;
    private List<String> paramNames = new ArrayList<>();

    public MethodInfoJson(MethodInfo info) {
        method = new MethodJson(info.signature);
        javaDoc = info.javaDoc;
        paramNames = info.paramNames;
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
}
