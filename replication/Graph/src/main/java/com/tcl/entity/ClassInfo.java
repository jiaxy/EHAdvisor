package com.tcl.entity;

import org.eclipse.jdt.core.dom.ITypeBinding;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

import static com.tcl.utils.CollectionUtils.toArray;

/**
 * Class Info for building inheritance hierarchy graph
 */
public class ClassInfo {
    public final String className;
    public final ITypeBinding binding;
    @Nullable
    public final String superclassName;
    public final String[] interfaceNames;
    public final MethodSignature[] declaredMethods;

    public ClassInfo(@Nonnull String className,
                     @Nonnull ITypeBinding binding,
                     @Nullable String superclassName,
                     @Nonnull Iterable<String> interfaceNames,
                     @Nonnull Iterable<MethodSignature> declaredMethods) {
        this.className = className;
        this.binding = binding;
        this.superclassName = superclassName;
        this.interfaceNames = toArray(interfaceNames, String.class);
        this.declaredMethods = toArray(declaredMethods, MethodSignature.class);
    }

    @Override
    public String toString() {
        return "ClassInfo{" +
                "className=" + className  +
                ", superclass=" + superclassName +
                ", interfaces=" + Arrays.toString(interfaceNames) +
                ", declaredMethods=" + Arrays.toString(declaredMethods) +
                '}';
    }
}
