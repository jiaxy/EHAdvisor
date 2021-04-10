package com.tcl.graph.inheritance;

import com.tcl.entity.AccessModifier;
import com.tcl.entity.ClassInfo;
import com.tcl.entity.MethodSignature;
import com.tcl.utils.JdtUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class InheritanceGraph {
    private final Map<String, ClassNode> nameToClass = new HashMap<>();

    public InheritanceGraph(@Nonnull Collection<ClassInfo> infos) {
        addNodesFromInfos(infos);
        infos.forEach(this::connectEdgesAround);
    }

    private void addNodesFromInfos(@Nonnull Iterable<ClassInfo> infos) {
        for (var info : infos) {
            assert !nameToClass.containsKey(info.className);
            var node = new ClassNode();
            node.name = info.className;
            node.binding = info.binding;
            node.superclassName = info.superclassName;
            node.interfaceNames.addAll(Arrays.asList(info.interfaceNames));
            for (var m : info.declaredMethods) {
                node.npToMethod.put(m.getNameAndParams(), m);
            }
            nameToClass.put(node.name, node);
        }
    }

    private void connectEdgesAround(@Nonnull ClassInfo info) {
        ClassNode node = nameToClass.get(info.className);
        assert node != null;
        if (info.superclassName != null
                && nameToClass.containsKey(info.superclassName)) {
            ClassNode superNode = nameToClass.get(info.superclassName);
            superNode.subclassNames.add(info.className);
            node.superclassName = info.superclassName;
        }
        for (String ifName : info.interfaceNames) {
            if (nameToClass.containsKey(ifName)) {
                ClassNode ifNode = nameToClass.get(ifName);
                ifNode.subclassNames.add(info.className);
                node.interfaceNames.add(ifName);
            }
        }
    }

    public boolean isCompatible(@Nonnull String subName, @Nonnull String superName) {
        if (subName.equals(superName)) {
            return true;
        }
        assert nameToClass.containsKey(subName) && nameToClass.containsKey(superName);
        return JdtUtils.isCompatible(
                nameToClass.get(subName).binding,
                nameToClass.get(superName).binding
        );
    }


    /**
     * Get the overridden method in this class.
     * <p>
     * If none, return empty.
     *
     * @param className in which class
     * @param method    method in base or this
     * @return overridden method in this class
     */
    public Optional<MethodSignature> overriddenInClass(
            @Nonnull String className, @Nonnull MethodSignature method) {
        ClassNode node = nameToClass.get(className);
        assert node != null;
//        if (node == null) { // no such class in graph
//            return Optional.empty();
//        }
        MethodSignature override = node.npToMethod.get(method.getNameAndParams());
        if (override == null || !isVirtualNotPrivate(override)) {
            //only virtual and non-private method can override another method
            return Optional.empty();
        }
        return Optional.of(override);
    }

    /**
     * Get all overridden methods in subclasses recursively.
     */
    @Nonnull
    public Set<MethodSignature> allOverriddenMethods(
            @Nonnull String className, @Nonnull MethodSignature method) {
        //There may be multiple paths to a subclass, so use a set to contain methods.
        var overriddenMethods = new HashSet<MethodSignature>();
        if (isVirtualNotPrivate(method)) {
            //only virtual and non-private method can be overridden
            overriddenInClass(className, method).ifPresent(overriddenMethods::add);
            for (var subName : nameToClass.get(className).subclassNames) {
                overriddenMethods.addAll(allOverriddenMethods(subName, method));
            }
        }
        return overriddenMethods;
    }

    @Nullable
    public String nodesToString() {
        var sb = new StringBuilder();
        for (var node : nameToClass.values()) {
            sb.append(node.toString()).append('\n');
        }
        return sb.toString();
    }

    private static boolean isVirtualNotPrivate(@Nonnull MethodSignature method) {
        return !method.isStatic()
                && !method.isConstructor()
                && method.getAccess() != AccessModifier.PRIVATE;
    }

}
