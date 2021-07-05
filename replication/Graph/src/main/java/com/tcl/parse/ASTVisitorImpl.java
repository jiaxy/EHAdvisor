package com.tcl.parse;

import com.tcl.entity.MethodInfo;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import javax.annotation.Nonnull;
import java.util.Objects;

public class ASTVisitorImpl extends ASTVisitor {

    private final CompilationUnit ast;
    private final ProjectDatabase dbToFill;

    public ASTVisitorImpl(@Nonnull CompilationUnit ast,
                          @Nonnull ProjectDatabase dbToFill) {
        Objects.requireNonNull(ast, "ast");
        Objects.requireNonNull(dbToFill, "dbToFill");
        this.ast = ast;
        this.dbToFill = dbToFill;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        MethodDeclaration[] declarations = node.getMethods();
        for (var declaration : declarations) {
            var collector = new MethodInfoCollector(ast, node, declaration);
            collector.collect();
            MethodInfo methodInfo = collector.getMethodInfo();
//            if (methodInfo.throwsInBody.isEmpty()) {
//                continue;
//            }
//            System.out.println(methodInfo);//print method info
            dbToFill.methodToInfo.put(methodInfo.signature, methodInfo);
            dbToFill.methodToBinding.putAll(collector.getMethodToBinding());
            dbToFill.classToBinding.putAll(collector.getClassToBinding());
        }
        return super.visit(node);
    }
}
