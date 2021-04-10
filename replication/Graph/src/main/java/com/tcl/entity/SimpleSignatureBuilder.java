package com.tcl.entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class SimpleSignatureBuilder {

    private String returnType = null;
    private String packageName = null;
    private String className;
    private String methodName;
    private final List<String> paramTypes = new ArrayList<>();

    @Nonnull
    public SimpleSignatureBuilder setReturnType(@Nonnull String returnType) {
        this.returnType = returnType;
        return this;
    }

    @Nonnull
    public SimpleSignatureBuilder setPackageName(@Nullable String packageName) {
        this.packageName = packageName;
        return this;
    }

    @Nonnull
    public SimpleSignatureBuilder setClassName(@Nonnull String className) {
        this.className = className;
        return this;
    }

    @Nonnull
    public SimpleSignatureBuilder setQualifiedClassName(@Nonnull String qualifiedClassName) {
        String[] names = qualifiedClassName.split("\\.");
        if (names.length == 1) {
            this.packageName = null;
            this.className = qualifiedClassName;
        } else {
            StringJoiner sj = new StringJoiner(".");
            for (int i = 0; i < names.length - 1; i++) {
                sj.add(names[i]);
            }
            this.packageName = sj.toString();
            this.className = names[names.length - 1];
        }
        return this;
    }

    @Nonnull
    public SimpleSignatureBuilder setMethodName(@Nonnull String methodName) {
        this.methodName = methodName;
        return this;
    }

    @Nonnull
    public SimpleSignatureBuilder setParamTypes(@Nonnull Iterable<String> paramTypes) {
        this.paramTypes.clear();
        paramTypes.forEach(this::addParamType);
        return this;
    }

    @Nonnull
    public SimpleSignatureBuilder setParamTypes(String... paramTypes) {
        setParamTypes(Arrays.asList(paramTypes));
        return this;
    }

    @Nonnull
    public SimpleSignatureBuilder addParamType(@Nonnull String paramType) {
        Objects.requireNonNull(paramType, "paramType");
        paramTypes.add(paramType);
        return this;
    }

    @Nonnull
    public SimpleMethodSignature build() {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(methodName, "methodName");
        return new SimpleMethodSignature(
                returnType, packageName, className, methodName, paramTypes
        );
    }
}
