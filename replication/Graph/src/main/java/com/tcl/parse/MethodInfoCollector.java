package com.tcl.parse;

import com.tcl.entity.MethodInfo;
import com.tcl.entity.MethodSignature;
import com.tcl.utils.JdtUtils;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Collect program info in a method
 */
public class MethodInfoCollector {

    private final CompilationUnit ast;
    private final TypeDeclaration node;
    private final MethodDeclaration declaration;
    private final MethodInfo methodInfo = new MethodInfo();
    private final Map<MethodSignature, IMethodBinding> methodToBinding = new HashMap<>();
    private final Map<String, ITypeBinding> classToBinding = new HashMap<>();


    public MethodInfoCollector(@Nonnull CompilationUnit ast,
                               @Nonnull TypeDeclaration node,
                               @Nonnull MethodDeclaration declaration) {
        Objects.requireNonNull(ast, "ast");
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(declaration, "declaration");
        this.ast = ast;
        this.node = node;
        this.declaration = declaration;
    }

    @Nonnull
    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    @Nonnull
    public Map<MethodSignature, IMethodBinding> getMethodToBinding() {
        return methodToBinding;
    }

    @Nonnull
    public Map<String, ITypeBinding> getClassToBinding() {
        return classToBinding;
    }

    public void collect() {
        processDeclaration();
        if (declaration.getBody() == null) {
            return;
        }
        for (Object obj : declaration.getBody().statements()) {
            var statement = (Statement) obj;
            InfoEntry stmtInfo = infoInStatement(statement);
            updateMethodInfo(stmtInfo);
        }
    }

    private void updateMethodInfo(@Nonnull InfoEntry info) {
        methodInfo.callings.addAll(info.callings);
        methodInfo.throwsInBody.addAll(info.throwsInBody);
        for (var calling : info.callingToHandlers.keySet()) {
            Set<String> handlers = info.callingToHandlers.get(calling);
            methodInfo.putCallingHandlers(calling, handlers);
        }
        methodToBinding.putAll(info.methodToBinding);
        classToBinding.putAll(info.classToBinding);
    }

    /**
     * Method signature, param names
     */
    private void processDeclaration() {
        if (declaration.getJavadoc() != null) {
            methodInfo.javaDoc = declaration.getJavadoc().toString();
        }
        IMethodBinding mBind = declaration.resolveBinding();
        methodInfo.signature = (mBind != null)
                ? JdtUtils.bindingToMethod(mBind)
                : JdtUtils.declarationToMethod(declaration, node, ast);
        for (Object p : declaration.parameters()) {
            String pName = ((SingleVariableDeclaration) p).getName().getIdentifier();
            methodInfo.paramNames.add(pName);
        }
        if (mBind != null) {
            classToBinding.putAll(JdtUtils.typeBindingsFromMethodBinding(mBind));
        }
    }

    /**
     * Recursively extract BlockInfo in the statement
     */
    @Nonnull
    private InfoEntry infoInStatement(@Nullable Statement statement) {
        var stmtInfo = new InfoEntry();
        if (statement == null) {
            return stmtInfo;
        }
        if (statement instanceof Block) {
            var blockStmt = (Block) statement;
            for (Object obj : blockStmt.statements()) {
                var stmt = (Statement) obj;
                stmtInfo.update(infoInStatement(stmt));
            }
        } else if (statement instanceof ExpressionStatement) {
            var exprStmt = (ExpressionStatement) statement;
            stmtInfo.update(infoInExpression(exprStmt.getExpression()));
        } else if (statement instanceof DoStatement) {
            var doStmt = (DoStatement) statement;
            stmtInfo.update(infoInStatement(doStmt.getBody()));
            stmtInfo.update(infoInExpression(doStmt.getExpression()));
        } else if (statement instanceof ForStatement) {
            var forStmt = (ForStatement) statement;
            stmtInfo.update(infoInExpression(forStmt.getExpression()));
            stmtInfo.update(infoInStatement(forStmt.getBody()));
        } else if (statement instanceof IfStatement) {
            var ifStmt = (IfStatement) statement;
            stmtInfo.update(infoInExpression(ifStmt.getExpression()));
            stmtInfo.update(infoInStatement(ifStmt.getThenStatement()));
            stmtInfo.update(infoInStatement(ifStmt.getElseStatement()));
        } else if (statement instanceof SwitchStatement) {
            var swStmt = (SwitchStatement) statement;
            stmtInfo.update(infoInExpression(swStmt.getExpression()));
            for (Object obj : swStmt.statements()) {
                var stmt = (Statement) obj;
                stmtInfo.update(infoInStatement(stmt));
            }
        } else if (statement instanceof WhileStatement) {
            var whileStmt = (WhileStatement) statement;
            stmtInfo.update(infoInExpression(whileStmt.getExpression()));
            stmtInfo.update(infoInStatement(whileStmt.getBody()));
        } else if (statement instanceof EnhancedForStatement) {
            var ehForStmt = (EnhancedForStatement) statement;
            stmtInfo.update(infoInExpression(ehForStmt.getExpression()));
            stmtInfo.update(infoInStatement(ehForStmt.getBody()));
        } else if (statement instanceof VariableDeclarationStatement) {
            var varDeclStmt = (VariableDeclarationStatement) statement;
            for (Object obj : varDeclStmt.fragments()) {
                Expression expr = ((VariableDeclarationFragment) obj).getInitializer();
                stmtInfo.update(infoInExpression(expr));
            }
        } else if (statement instanceof ReturnStatement) {
            var retStmt = (ReturnStatement) statement;
            stmtInfo.update(infoInExpression(retStmt.getExpression()));
        } else if (statement instanceof SynchronizedStatement) {
            var syncStmt = (SynchronizedStatement) statement;
            stmtInfo.update(infoInExpression(syncStmt.getExpression()));
            stmtInfo.update(infoInStatement(syncStmt.getBody()));
        } else if (statement instanceof ThrowStatement) {
            var throwStmt = (ThrowStatement) statement;
            if (throwStmt.getExpression() instanceof ClassInstanceCreation) {
                var creationExpr = (ClassInstanceCreation) throwStmt.getExpression();
                stmtInfo.update(infoInExpression(creationExpr.getExpression()));
                ITypeBinding typeBinding = creationExpr.resolveTypeBinding();
                if (typeBinding != null && JdtUtils.isCompatible(
                        typeBinding, "java.lang.Throwable")) {
                    String exName = typeBinding.getErasure().getQualifiedName();
                    stmtInfo.throwsInBody.add(exName);
                    stmtInfo.classToBinding.put(exName, typeBinding);
                }
            }
        } else if (statement instanceof TryStatement) {
            var tryStmt = (TryStatement) statement;
            //try block
            InfoEntry blockInfoInTry = infoInStatement(tryStmt.getBody());
            stmtInfo.update(blockInfoInTry);
            //catch clauses
            var exceptionsInCatch = new ArrayList<String>();
            for (Object obj : tryStmt.catchClauses()) {
                var catchClause = (CatchClause) obj;
                //catch exception
                Type exceptionType = catchClause.getException().getType();
                if (exceptionType.isUnionType()) {
                    var unionType = (UnionType) exceptionType;
                    for (Object ot : unionType.types()) {
                        var eType = (Type) ot;
                        ITypeBinding typeBinding = eType.resolveBinding();
                        String exceptionTypeName = (typeBinding != null)
                                ? typeBinding.getErasure().getQualifiedName()
                                : catchClause.getException().getName().getFullyQualifiedName();
                        exceptionsInCatch.add(exceptionTypeName);
                        if (typeBinding != null) {
                            stmtInfo.classToBinding.put(exceptionTypeName, typeBinding);
                        }
                    }
                } else {
                    ITypeBinding typeBinding = exceptionType.resolveBinding();
                    String exceptionTypeName = (typeBinding != null)
                            ? typeBinding.getErasure().getQualifiedName()
                            : catchClause.getException().getName().getFullyQualifiedName();
                    exceptionsInCatch.add(exceptionTypeName);
                    if (typeBinding != null) {
                        stmtInfo.classToBinding.put(exceptionTypeName, typeBinding);
                    }
                }
                for (var calling : blockInfoInTry.callings) {
                    stmtInfo.putCallingHandlers(calling, exceptionsInCatch);
                }
                //stmts in catch body
                stmtInfo.update(infoInStatement(catchClause.getBody()));
            }
            //finally block
            blockInfoInTry.update(infoInStatement(tryStmt.getFinally()));
        } else if (statement instanceof ConstructorInvocation) {
            var ctorInvocation = (ConstructorInvocation) statement;
            IMethodBinding ctorBinding = ctorInvocation.resolveConstructorBinding();
            if (ctorBinding != null && ctorBinding.getDeclaringClass() != null) {
                MethodSignature ctor = JdtUtils.bindingToMethod(ctorBinding);
                stmtInfo.callings.add(ctor);
                stmtInfo.methodToBinding.put(ctor, ctorBinding);
                stmtInfo.classToBinding.putAll(
                        JdtUtils.typeBindingsFromMethodBinding(ctorBinding));
            }
        }
        return stmtInfo;
    }

    /**
     * Recursively extract BlockInfo in the expression
     */
    @Nonnull
    private InfoEntry infoInExpression(@Nullable Expression expression) {
        var exprInfo = new InfoEntry();
        if (expression == null) {
            return exprInfo;
        }
        if (expression instanceof ArrayAccess) {
            var arrAcc = (ArrayAccess) expression;
            exprInfo.update(infoInExpression(arrAcc.getIndex()));
        } else if (expression instanceof ArrayCreation) {
            var arrCreation = (ArrayCreation) expression;
            for (Object obj : arrCreation.dimensions()) {
                exprInfo.update(infoInExpression((Expression) obj));
            }
        } else if (expression instanceof Assignment) {
            var ass = (Assignment) expression;
            exprInfo.update(infoInExpression(ass.getLeftHandSide()));
            exprInfo.update(infoInExpression(ass.getRightHandSide()));
        } else if (expression instanceof CastExpression) {
            var cast = (CastExpression) expression;
            exprInfo.update(infoInExpression(cast.getExpression()));
        } else if (expression instanceof ConditionalExpression) {
            var cond = (ConditionalExpression) expression;
            exprInfo.update(infoInExpression(cond.getExpression()));
            exprInfo.update(infoInExpression(cond.getThenExpression()));
            exprInfo.update(infoInExpression(cond.getElseExpression()));
        } else if (expression instanceof FieldAccess) {
            var fieldAcc = (FieldAccess) expression;
            exprInfo.update(infoInExpression(fieldAcc.getExpression()));
        } else if (expression instanceof ParenthesizedExpression) {
            var parenthesized = (ParenthesizedExpression) expression;
            exprInfo.update(infoInExpression(parenthesized.getExpression()));
        } else if (expression instanceof VariableDeclarationExpression) {
            var varDecl = (VariableDeclarationExpression) expression;
            for (Object obj : varDecl.fragments()) {
                var fragment = (VariableDeclarationFragment) obj;
                exprInfo.update(infoInExpression(fragment.getInitializer()));
            }
        } else if (expression instanceof PrefixExpression) {
            var prefixExpr = (PrefixExpression) expression;
            exprInfo.update(infoInExpression(prefixExpr.getOperand()));
        } else if (expression instanceof PostfixExpression) {
            var postfixExpr = (PostfixExpression) expression;
            exprInfo.update(infoInExpression(postfixExpr.getOperand()));
        } else if (expression instanceof InfixExpression) {
            var infixExpr = (InfixExpression) expression;
            exprInfo.update(infoInExpression(infixExpr.getLeftOperand()));
            exprInfo.update(infoInExpression(infixExpr.getRightOperand()));
        } else if (expression instanceof ClassInstanceCreation) {
            var creationExpr = (ClassInstanceCreation) expression;
            exprInfo.update(infoInExpression(creationExpr.getExpression()));
            IMethodBinding ctorBinding = creationExpr.resolveConstructorBinding();
            if (ctorBinding != null) {
                if (ctorBinding.getName() == null || ctorBinding.getName().equals("")) {
                    //anonymous class
                    AnonymousClassDeclaration anonClsDecl = creationExpr.getAnonymousClassDeclaration();
                    for (Object obj : anonClsDecl.bodyDeclarations()) {
                        if (obj instanceof MethodDeclaration) {
                            var methodDecl = (MethodDeclaration) obj;
                            exprInfo.update(infoInStatement(methodDecl.getBody()));
                        }
                    }
                } else {
                    MethodSignature ctor = JdtUtils.bindingToMethod(ctorBinding);
                    exprInfo.callings.add(ctor);
                    exprInfo.methodToBinding.put(ctor, ctorBinding);
                    exprInfo.classToBinding.putAll(
                            JdtUtils.typeBindingsFromMethodBinding(ctorBinding));
                }
            }
        } else if (expression instanceof MethodInvocation) {
            var invocationExpr = (MethodInvocation) expression;
            for (Object expr : invocationExpr.arguments()) {
                exprInfo.update(infoInExpression((Expression) expr));
            }
            IMethodBinding methodBinding = invocationExpr.resolveMethodBinding();
            if (methodBinding != null) {
                MethodSignature method = JdtUtils.bindingToMethod(methodBinding);
                exprInfo.callings.add(method);
                exprInfo.methodToBinding.put(method, methodBinding);
                exprInfo.classToBinding.putAll(
                        JdtUtils.typeBindingsFromMethodBinding(methodBinding));
            }
        } else if (expression instanceof LambdaExpression) {
            var lambda = (LambdaExpression) expression;
            if (lambda.getBody() instanceof Expression) {
                exprInfo.update(infoInExpression((Expression) lambda.getBody()));
            } else if (lambda.getBody() instanceof Statement) {
                exprInfo.update(infoInStatement((Statement) lambda.getBody()));
            }
        } else if (expression instanceof MethodReference) {
            var methodRef = (MethodReference) expression;
            IMethodBinding methodBinding = methodRef.resolveMethodBinding();
            if (methodBinding != null) {
                MethodSignature method = JdtUtils.bindingToMethod(methodBinding);
                exprInfo.callings.add(method);
                exprInfo.methodToBinding.put(method, methodBinding);
                exprInfo.classToBinding.putAll(
                        JdtUtils.typeBindingsFromMethodBinding(methodBinding));
            }
        }
        //ignore super invoke
        return exprInfo;
    }

    /**
     * Contains information that we are interested in
     */
    private static class InfoEntry {
        final Set<MethodSignature> callings = new HashSet<>();
        final Set<String> throwsInBody = new HashSet<>();
        final Map<MethodSignature, IMethodBinding> methodToBinding = new HashMap<>();
        final Map<String, ITypeBinding> classToBinding = new HashMap<>();
        final Map<MethodSignature, Set<String>> callingToHandlers = new HashMap<>();

        /**
         * union with another BlockInfo and update itself
         */
        void update(@Nonnull InfoEntry other) {
            callings.addAll(other.callings);
            throwsInBody.addAll(other.throwsInBody);
            methodToBinding.putAll(other.methodToBinding);
            classToBinding.putAll(other.classToBinding);
            for (var calling : other.callingToHandlers.keySet()) {
                putCallingHandlers(calling, other.callingToHandlers.get(calling));
            }
        }

        void putCallingHandler(MethodSignature calling, String exception) {
            callingToHandlers.putIfAbsent(calling, new HashSet<>());
            callingToHandlers.get(calling).add(exception);
        }

        void putCallingHandlers(MethodSignature calling, Iterable<String> exceptions) {
            callingToHandlers.putIfAbsent(calling, new HashSet<>());
            exceptions.forEach(callingToHandlers.get(calling)::add);
        }

        @Override
        public String toString() {
            return "BlockInfo{" +
                    "callings=" + callings +
                    ", throwExceptions=" + throwsInBody +
                    ", callingToHandlers=" + callingToHandlers +
                    '}';
        }
    }

}
