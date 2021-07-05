package com.tcl.json;

import com.tcl.entity.MethodSignature;

import java.util.Locale;
import java.util.Optional;

public class MethodJson {
    final private MethodSignature method;

    public MethodJson(MethodSignature method) {
        this.method = method;
    }

    public String getAccess() {
        return method.getAccess().toString().toLowerCase(Locale.ROOT);
    }

    public boolean isStatic() {
        return method.isStatic();
    }

    public String[] getThrowsDeclaration() {
        return method.getThrowsDeclaration();
    }

    public Optional<String> getReturnType() {
        return method.getReturnType();
    }

    public Optional<String> getPackageName() {
        return method.getPackageName();
    }

    public String getClassName() {
        return method.getClassName();
    }

    public String getMethodName() {
        return method.getMethodName();
    }

    public String[] getParamTypes() {
        return method.getParamTypes();
    }
}
