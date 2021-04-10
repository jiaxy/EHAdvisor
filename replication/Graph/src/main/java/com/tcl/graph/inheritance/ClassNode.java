package com.tcl.graph.inheritance;

import com.tcl.entity.MethodSignature;
import org.eclipse.jdt.core.dom.ITypeBinding;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClassNode {
    public String name;
    public ITypeBinding binding;
    @Nullable
    public String superclassName;
    public final Set<String> interfaceNames = new HashSet<>();
    public final Set<String> subclassNames = new HashSet<>();
    /**
     * "name and params" to method
     */
    public final Map<String, MethodSignature> npToMethod = new HashMap<>();

    @Override
    public String toString() {
        return "ClassNode{" +
                "name='" + name + '\'' +
                ", superclassName='" + superclassName + '\'' +
                ", interfaceNames=" + interfaceNames +
                ", subclassNames=" + subclassNames +
                ", npToMethod=" + npToMethod +
                '}';
    }
}
