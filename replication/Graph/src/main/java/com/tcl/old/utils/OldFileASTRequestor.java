package com.tcl.old.utils;

import com.tcl.old.entity.MethodEntity;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class OldFileASTRequestor extends FileASTRequestor {

    public LinkedList<MethodEntity> list;
    public HashMap<String, HashSet<String>> classToSubClass;

    @Override
    public void acceptAST(String sourceFilePath, CompilationUnit ast) {
        MethodVisitor visitor = new MethodVisitor(list, ast, classToSubClass);
        ast.accept(visitor);
        ast.recordModifications();
    }
}
