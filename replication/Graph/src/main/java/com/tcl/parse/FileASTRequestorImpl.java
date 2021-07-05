package com.tcl.parse;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import javax.annotation.Nonnull;

public class FileASTRequestorImpl extends FileASTRequestor {

    private final ProjectDatabase database = new ProjectDatabase();

    @Nonnull
    public ProjectDatabase getProjectDatabase() {
        return database;
    }

    @Override
    public void acceptAST(String sourceFilePath, CompilationUnit ast) {
        var visitor = new ASTVisitorImpl(ast, database);
        ast.accept(visitor);
        ast.recordModifications();
    }
}
