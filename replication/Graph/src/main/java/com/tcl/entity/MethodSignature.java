package com.tcl.entity;

import javax.annotation.Nonnull;
import java.util.*;

public class MethodSignature {

    private final AccessModifier access;
    private final boolean isStatic;
    private final SimpleMethodSignature simpleSignature;
    private final String[] throwsDeclaration;
    private final String signature;

    public MethodSignature(@Nonnull AccessModifier access,
                           boolean isStatic,
                           @Nonnull SimpleMethodSignature simpleSignature,
                           @Nonnull Iterable<String> throwsDeclaration) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(simpleSignature, "signature");
        Objects.requireNonNull(throwsDeclaration, "throwsDeclaration");
        this.access = access;
        this.isStatic = isStatic;
        this.simpleSignature = simpleSignature;
        var throwsList = new ArrayList<String>();
        for (String ex : throwsDeclaration) {
            Objects.requireNonNull(ex, "ex");
            throwsList.add(ex);
        }
        this.throwsDeclaration = throwsList.toArray(new String[0]);
        //construct signature string
        var sb = new StringBuilder();
        if (access != AccessModifier.DEFAULT) {
            sb.append(access.toString().toLowerCase(Locale.ROOT)).append(' ');
        }
        if (isStatic) {
            sb.append("static ");
        }
        sb.append(simpleSignature.toString());
        var sj = new StringJoiner(", ");
        throwsDeclaration.forEach(sj::add);
        if (sj.length() > 0) {
            sb.append(" throws ").append(sj.toString());
        }
        signature = sb.toString();
    }

    @Override
    public int hashCode() {
        return signature.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MethodSignature) {
            var other = (MethodSignature) obj;
            return signature.equals(other.signature);
        }
        return false;
    }

    @Override
    public String toString() {
        return signature;
    }

    @Nonnull
    public AccessModifier getAccess() {
        return access;
    }

    public boolean isStatic() {
        return isStatic;
    }

    @Nonnull
    public SimpleMethodSignature getSimpleSignature() {
        return simpleSignature;
    }

    @Nonnull
    public String[] getThrowsDeclaration() {
        return throwsDeclaration;
    }

    @Nonnull
    public String getSignature() {
        return signature;
    }

    //region proxy
    public Optional<String> getReturnType() {
        return simpleSignature.getReturnType();
    }

    public Optional<String> getPackageName() {
        return simpleSignature.getPackageName();
    }

    @Nonnull
    public String getClassName() {
        return simpleSignature.getClassName();
    }

    @Nonnull
    public String getMethodName() {
        return simpleSignature.getMethodName();
    }

    @Nonnull
    public String[] getParamTypes() {
        return simpleSignature.getParamTypes();
    }

    @Nonnull
    public String getQualifiedClassName() {
        return simpleSignature.getQualifiedClassName();
    }

    @Nonnull
    public String getQualifiedMethodName() {
        return simpleSignature.getQualifiedMethodName();
    }

    @Nonnull
    public String getNameAndParams() {
        return simpleSignature.getNameAndParams();
    }

    public boolean isConstructor() {
        return simpleSignature.isConstructor();
    }
    //endregion
}
