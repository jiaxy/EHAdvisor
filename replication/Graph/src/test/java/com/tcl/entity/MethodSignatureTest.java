package com.tcl.entity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class MethodSignatureTest {
    @Test
    public void test1() {
        SimpleMethodSignature signature = new SimpleSignatureBuilder()
                .setQualifiedClassName("Cls")
                .setMethodName("Cls")
                .setParamTypes("int", "int")
                .build();
        var mExt = new MethodSignature(
                AccessModifier.DEFAULT,
                false,
                signature,
                Collections.emptyList()
        );
        assertEquals("Cls.Cls(int, int)", mExt.toString());
    }

    @Test
    public void test2() {
        SimpleMethodSignature signature = new SimpleSignatureBuilder()
                .setQualifiedClassName("Cls")
                .setMethodName("Cls")
                .setParamTypes("int", "int")
                .build();
        var mExt = new MethodSignature(
                AccessModifier.DEFAULT,
                false,
                signature,
                Arrays.asList("E1", "E2")
        );
        assertEquals("Cls.Cls(int, int) throws E1, E2", mExt.toString());
    }

    @Test
    public void test3() {
        SimpleMethodSignature signature = new SimpleSignatureBuilder()
                .setReturnType("int")
                .setQualifiedClassName("com.pkg.Cls")
                .setMethodName("m")
                .setParamTypes("int", "int")
                .build();
        var mExt = new MethodSignature(
                AccessModifier.PUBLIC,
                true,
                signature,
                Collections.emptyList()
        );
        assertEquals("public static int com.pkg.Cls.m(int, int)", mExt.toString());
    }

    @Test
    public void test4() {
        SimpleMethodSignature signature = new SimpleSignatureBuilder()
                .setReturnType("int")
                .setQualifiedClassName("com.pkg.Cls")
                .setMethodName("m")
                .setParamTypes("int", "int")
                .build();
        var mExt = new MethodSignature(
                AccessModifier.PUBLIC,
                true,
                signature,
                Collections.singletonList("Exception")
        );
        assertEquals("public static int com.pkg.Cls.m(int, int) throws Exception", mExt.toString());
    }
}