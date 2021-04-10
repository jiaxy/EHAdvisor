package com.tcl.entity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SimpleMethodSignatureTest {

    @Test
    public void testCtor1() {
        var method = new SimpleMethodSignature(
                "void",
                null,
                "Cls",
                "m",
                Collections.emptyList()
        );
        assert method.toString().equals("void Cls.m()");
    }

    @Test
    public void testCtor2() {
        var method = new SimpleMethodSignature(
                "int",
                "com.pkg",
                "Cls",
                "m",
                Collections.singletonList("int")
        );
        assert method.toString().equals("int com.pkg.Cls.m(int)");
    }

    @Test
    public void testCtor3() {
        var method = new SimpleMethodSignature(
                "int",
                "com.pkg",
                "Cls",
                "m",
                Arrays.asList("int", "int")
        );
        assert method.toString().equals("int com.pkg.Cls.m(int, int)");
    }

    @Test
    public void testFullClsName1() {
        var method = new SimpleMethodSignature(
                "void",
                "com.pkg",
                "Cls",
                "m",
                Arrays.asList("int", "int")
        );
        assert method.getQualifiedClassName().equals("com.pkg.Cls");
    }

    @Test
    public void testFullClsName2() {
        var method = new SimpleMethodSignature(
                "void",
                null,
                "Cls",
                "m",
                Arrays.asList("int", "int")
        );
        assert method.getQualifiedClassName().equals("Cls");
    }

    @Test
    public void testFullMethodName() {
        var method = new SimpleMethodSignature(
                "void",
                "com.pkg",
                "Cls",
                "m",
                Arrays.asList("int", "int")
        );
        assert method.getQualifiedMethodName().equals("com.pkg.Cls.m");
    }

    @Test
    public void equalTest(){
        var m1 = new SimpleMethodSignature(
                "void",
                null,
                "Cls",
                "m",
                Arrays.asList("int", "int")
        );
        var m2 = new SimpleMethodSignature(
                "void",
                null,
                "Cls",
                "m",
                Arrays.asList("int", "int")
        );
        assert m1.equals(m2);
    }

    @Test
    public void nameAndParamsTest1(){
        var m = new SimpleMethodSignature(
                "void",
                null,
                "cls",
                "m",
                Arrays.asList("int", "int")
        );
        assert m.getNameAndParams().equals("m(int, int)");
    }

    @Test
    public void nameAndParamsTest2(){
        var m = new SimpleMethodSignature(
                "void",
                "com.pkg",
                "cls",
                "m",
                Collections.emptyList()
        );
        assert m.getNameAndParams().equals("m()");
    }
}