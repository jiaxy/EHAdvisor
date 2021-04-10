package com.tcl.utils;

import com.tcl.entity.*;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class JdtUtils {
    @Nonnull
    public static SimpleMethodSignature bindingToSimpleMethod(@Nonnull IMethodBinding binding) {
        Objects.requireNonNull(binding, "binding");
        var builder = new SimpleSignatureBuilder();
        if (!binding.isConstructor()) {
            builder.setReturnType(binding.getReturnType().getQualifiedName());
        }
        assert binding.getDeclaringClass() != null;
        builder.setQualifiedClassName(binding.getDeclaringClass().getErasure().getQualifiedName());
        //For a generic method, binding.getName() will get a name with angle brackets
        //So use binding.getMethodDeclaration().getName() instead
        builder.setMethodName(binding.getMethodDeclaration().getName());
        int pNum = binding.getParameterTypes().length;
        for (int i = 0; i < pNum; i++) {
            ITypeBinding p = binding.getParameterTypes()[i];
            assert p != null;
            String typeName = p.getErasure().getQualifiedName();
            if (binding.isVarargs() && i == pNum - 1) {
                typeName += "[]";
            }
            builder.addParamType(typeName);
        }
        return builder.build();
    }

    @Nonnull
    public static SimpleMethodSignature declarationToSimpleMethod(
            @Nonnull MethodDeclaration declaration,
            @Nonnull TypeDeclaration node,
            @Nonnull CompilationUnit ast) {
        var builder = new SimpleSignatureBuilder();
        //package name
        if (ast.getPackage() == null) {
            builder.setPackageName(null);
        } else {
            builder.setPackageName(ast.getPackage().getName().getFullyQualifiedName());
        }
        //class name
        String className = (node.resolveBinding() != null)
                ? node.resolveBinding().getName()
                : node.getName().getIdentifier();
        builder.setClassName(className);
        //method name
        builder.setMethodName(declaration.getName().getIdentifier());
        //return type
        if (!declaration.isConstructor()) {
            Type retType = declaration.getReturnType2();
            ITypeBinding retBinding = retType.resolveBinding();
            String retTypeName = (retBinding != null)
                    ? retBinding.getErasure().getQualifiedName()
                    : retType.toString();
            builder.setReturnType(retTypeName);
        }
        //param types
        int pNum = declaration.parameters().size();
        for (int i = 0; i < pNum; i++) {
            //param type
            var param = (SingleVariableDeclaration) declaration.parameters().get(i);
            ITypeBinding paramTypeBinding = param.getType().resolveBinding();
            String fullTypeName = (paramTypeBinding != null)
                    ? paramTypeBinding.getErasure().getQualifiedName()
                    : param.getName().getFullyQualifiedName();
            if (declaration.isVarargs() && i == pNum - 1) {
                fullTypeName += "[]";
            }
            builder.addParamType(fullTypeName);
        }
        return builder.build();
    }

    @Nonnull
    public static MethodSignature bindingToMethod(@Nonnull IMethodBinding binding) {
        Objects.requireNonNull(binding, "binding");
        SimpleMethodSignature signature = bindingToSimpleMethod(binding);
        int modifier = binding.getModifiers();
        boolean isStatic = staticOfModifier(modifier);
        AccessModifier access = accessOfModifier(modifier);
        var throwsDecl = new ArrayList<String>();
        for (ITypeBinding exBinding : binding.getExceptionTypes()) {
            assert exBinding != null;
            throwsDecl.add(exBinding.getErasure().getQualifiedName());
        }
        return new MethodSignature(
                access, isStatic, signature, throwsDecl
        );
    }

    @Nonnull
    public static MethodSignature declarationToMethod(
            @Nonnull MethodDeclaration declaration,
            @Nonnull TypeDeclaration node,
            @Nonnull CompilationUnit ast) {
        SimpleMethodSignature simple = declarationToSimpleMethod(declaration, node, ast);
        int modifier = declaration.getModifiers();
        boolean isStatic = staticOfModifier(modifier);
        AccessModifier access = accessOfModifier(modifier);
        var throwsDecl = new ArrayList<String>();
        for (Object obj : declaration.thrownExceptionTypes()) {
            var type = (Type) obj;
            ITypeBinding binding = type.resolveBinding();
            String typeName = (binding != null)
                    ? binding.getErasure().getQualifiedName()
                    : type.toString();
            throwsDecl.add(typeName);
        }
        return new MethodSignature(
                access, isStatic, simple, throwsDecl
        );
    }

    public static boolean staticOfModifier(int modifier) {
        return (modifier & Modifier.STATIC) != 0;
    }

    @Nonnull
    public static AccessModifier accessOfModifier(int modifier) {
        if ((modifier & Modifier.DEFAULT) != 0) {
            return AccessModifier.DEFAULT;
        } else if ((modifier & Modifier.PUBLIC) != 0) {
            return AccessModifier.PUBLIC;
        } else if ((modifier & Modifier.PROTECTED) != 0) {
            return AccessModifier.PROTECTED;
        } else if ((modifier & Modifier.PRIVATE) != 0) {
            return AccessModifier.PRIVATE;
        } else {
            return AccessModifier.DEFAULT;
        }
    }

    @Nonnull
    public static Map<String, ITypeBinding> typeBindingsFromMethodBinding(
            @Nonnull IMethodBinding methodBinding) {
        Objects.requireNonNull(methodBinding, "methodBinding");
        var dict = new HashMap<String, ITypeBinding>();
        ITypeBinding declClass = methodBinding.getDeclaringClass();
        assert declClass != null;
        dict.put(declClass.getErasure().getQualifiedName(), declClass.getErasure());
        ITypeBinding retType = methodBinding.getReturnType();
        assert retType != null;
        dict.put(retType.getErasure().getQualifiedName(), retType.getErasure());
        for (ITypeBinding p : methodBinding.getParameterTypes()) {
            assert p != null;
            dict.put(p.getErasure().getQualifiedName(), p.getErasure());
        }
        return dict;
    }

    /**
     * Extract qualified class name from ITypeBinding.
     */
    @Nonnull
    public static String toClassName(@Nonnull ITypeBinding binding) {
        Objects.requireNonNull(binding, "binding");
        return binding.getErasure().getQualifiedName();
    }

    /**
     * Extract qualified class name from ITypeBinding.
     * <p>
     * If binding is null, return null.
     */
    @Nullable
    public static String toClassNameOrNull(@Nullable ITypeBinding binding) {
        return (binding != null) ? binding.getErasure().getQualifiedName() : null;
    }

    @Nonnull
    public static ClassInfo bindingToClassInfo(@Nonnull ITypeBinding binding) {
        Objects.requireNonNull(binding, "binding");
        var interfaces = new ArrayList<String>();
        for (var ifBind : binding.getInterfaces()) {
            interfaces.add(toClassName(ifBind));
        }
        var methods = new ArrayList<MethodSignature>();
        for (var mBind : binding.getDeclaredMethods()) {
            methods.add(bindingToMethod(mBind));
        }
        return new ClassInfo(
                toClassName(binding),
                binding,
                toClassNameOrNull(binding.getSuperclass()),
                interfaces,
                methods
        );
    }

    public static boolean isCompatible(@Nonnull ITypeBinding subclass,
                                       @Nonnull ITypeBinding superclass) {
        Objects.requireNonNull(subclass, "subclass");
        Objects.requireNonNull(superclass, "superclass");
        if (toClassName(subclass).equals(toClassName(superclass))) {
            return true;
        }
        var supers = new ArrayList<ITypeBinding>();
        if (subclass.getSuperclass() != null) {
            supers.add(subclass.getSuperclass());
        }
        Collections.addAll(supers, subclass.getInterfaces());
        return supers.stream().anyMatch(cls -> isCompatible(cls, superclass));
    }

    private JdtUtils() {
    }
}
