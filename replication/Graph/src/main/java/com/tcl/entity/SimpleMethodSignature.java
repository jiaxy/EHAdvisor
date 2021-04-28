package com.tcl.entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public class SimpleMethodSignature {
    @Nullable
    private final String returnType;
    @Nullable
    private final String packageName;
    private final String className;
    private final String methodName;
    private final String[] paramTypes;
    private final String nameAndParams;
    private final String hashSignature;

    public SimpleMethodSignature(@Nullable String returnType,
                                 @Nullable String packageName,
                                 @Nonnull String className,
                                 @Nonnull String methodName,
                                 @Nonnull Iterable<String> paramTypes) {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(paramTypes, "paramTypes");
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
//        if (returnType == null && !className.equals(methodName)) {
//            throw new IllegalArgumentException("Not a constructor and returnType is null");
//        }
        this.returnType = returnType;
        var params = new ArrayList<String>();
        for (String param : paramTypes) {
            Objects.requireNonNull(param, "param");
            params.add(param);
        }
        this.paramTypes = params.toArray(new String[0]);
        //construct signature
        var sj = new StringJoiner(", ");
        paramTypes.forEach(sj::add);
        nameAndParams = methodName + '(' + sj + ')';
        hashSignature = getQualifiedMethodName() + '(' + sj + ')';
    }

    @Override
    public String toString() {
        return (returnType != null)
                ? returnType + ' ' + hashSignature
                : hashSignature;
    }

    @Override
    public int hashCode() {
        return hashSignature.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SimpleMethodSignature) {
            var other = (SimpleMethodSignature) obj;
            return hashSignature.equals(other.hashSignature);
        }
        return false;
    }

    public Optional<String> getReturnType() {
        return Optional.ofNullable(returnType);
    }

    public Optional<String> getPackageName() {
        return Optional.ofNullable(packageName);
    }

    @Nonnull
    public String getClassName() {
        return className;
    }

    @Nonnull
    public String getMethodName() {
        return methodName;
    }

    @Nonnull
    public String[] getParamTypes() {
        return paramTypes;
    }

    @Nonnull
    public String getQualifiedClassName() {
        return (packageName != null) ? packageName + "." + className : className;
    }

    @Nonnull
    public String getQualifiedMethodName() {
        return getQualifiedClassName() + "." + methodName;
    }

    @Nonnull
    public String getNameAndParams() {
        return nameAndParams;
    }

    public boolean isConstructor() {
        return className.equals(methodName);
    }

}
