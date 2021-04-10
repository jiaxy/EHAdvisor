package com.tcl.entity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SimpleSignatureBuilderTest {

    @Test
    public void testBuild1() {
        var builder = new SimpleSignatureBuilder();
        builder.setReturnType("void")
                .setPackageName(null)
                .setClassName("Cls")
                .setMethodName("m")
                .setParamTypes(Collections.emptyList());
        assert builder.build().toString().equals("void Cls.m()");
    }

    @Test
    public void testBuild2() {
        var builder = new SimpleSignatureBuilder();
        builder.setReturnType("void")
                .setPackageName("com.p")
                .setClassName("Cls")
                .setMethodName("m")
                .setParamTypes(Arrays.asList("int", "int"));
        assert builder.build().toString().equals("void com.p.Cls.m(int, int)");
    }

    @Test
    public void testBuild3() {
        var builder = new SimpleSignatureBuilder();
        builder.setReturnType("void")
                .setPackageName(null)
                .setClassName("Cls")
                .setMethodName("m");
        assert builder.build().toString().equals("void Cls.m()");
    }

    @Test
    public void testBuild4() {
        var builder = new SimpleSignatureBuilder();
        builder.setReturnType("void")
                .setPackageName(null)
                .setClassName("Cls")
                .setMethodName("m");
        builder.addParamType("int");
        builder.addParamType("String");
        assert builder.build().toString().equals("void Cls.m(int, String)");
    }

    @Test
    public void testBuild5(){
        var builder = new SimpleSignatureBuilder();
        builder.setPackageName(null)
                .setClassName("Cls")
                .setMethodName("Cls")
                .setParamTypes("int", "int");
        assert builder.build().toString().equals("Cls.Cls(int, int)");
    }
}