package com.tcl.old.utils;

import com.tcl.old.entity.MethodEntity;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class MethodVisitor extends ASTVisitor {

    HashMap<String, HashSet<String>> classToSubClass;
    private final LinkedList<MethodEntity> list;
    private final CompilationUnit cu;

    public MethodVisitor(LinkedList<MethodEntity> linkedList, CompilationUnit cu) {
        list = linkedList;
        this.cu = cu;

//        for (Object o : cu.imports()) {
//            imports.add(((ImportDeclaration) o).getName().getFullyQualifiedName());
//        }
    }

    public MethodVisitor(LinkedList<MethodEntity> linkedList, CompilationUnit cu, HashMap<String, HashSet<String>> m) {
        this(linkedList, cu);
        classToSubClass = m;
    }


    @Override
    public boolean visit(EnumDeclaration node) {
        return super.visit(node);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        MethodDeclaration[] methods = node.getMethods();
        for (MethodDeclaration method : methods) {
            getMethodInfo(node, method);
        }
        return super.visit(node);
    }

    //method decl: signature, throw decl, para name
    private void getMethodInfo(TypeDeclaration node, MethodDeclaration method) {
        // 没有body，即只是一个方法声明
        if (method.getBody() == null) {
            return;
        }
        // for package-info.java file
        if (cu.getPackage() == null) return;
        MethodEntity entity = new MethodEntity();
        entity.setPackageName(cu.getPackage().getName().getFullyQualifiedName());

        //params
        StringBuilder paraBuilder = new StringBuilder();
        List<String> para = new LinkedList<>();
        int size = method.parameters().size();
        for (int i = 0; i < size; ++i) {
            Object o = method.parameters().get(i);
            String typeName = ((SingleVariableDeclaration) o).getType().resolveBinding().getErasure().getName();
            String fullTypeName = ((SingleVariableDeclaration) o).getType().resolveBinding().getErasure().getQualifiedName();
            if (i == size - 1 && method.isVarargs()) {
                typeName = typeName + "[]";
            }
            String varName = ((SingleVariableDeclaration) o).getName().getFullyQualifiedName();
            paraBuilder.append("@").append(typeName);//TODO fullTypeName
            para.add(fullTypeName);
        }
        entity.setParameters(para);

        //throws decl
        Set<String> throwsType = new HashSet<>();
        for (Object o : method.thrownExceptionTypes()) {
            ITypeBinding binding = ((Type) o).resolveBinding();
            if (binding != null) {
                throwsType.add(binding.getQualifiedName());
            } else {
                throwsType.add(((Type) o).toString());
            }
        }
        entity.setThrowsName(throwsType);

//        System.out.println("class name: " + node.resolveBinding().getName());
//        System.out.println("method name: " + method.getName().getIdentifier());
        entity.setClassName(node.resolveBinding().getQualifiedName());
        entity.setMethodName(method.getName().getFullyQualifiedName());
        entity.setFullName(entity.getClassName()
                + "$" + entity.getMethodName() + paraBuilder.toString());

        entity.setCallingSets(new HashSet<>());
        entity.setCatchName(new HashSet<>());
        Block block = method.getBody();
        getInfoFromBlock(block, entity);

        list.add(entity);
    }

    //method body
    private void getInfoFromBlock(Block block, MethodEntity entity) {
        List statements = block.statements();
        for (Object s : statements) {
            statementHandler((Statement) s, entity);
        }
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        return true;
    }

    //stmt in method body, may recursively
    private void statementHandler(Statement statement, MethodEntity entity) {
        if (statement == null) return;

        if (statement instanceof Block) {
            List statements = ((Block) statement).statements();
            for (Object s : statements) {
                statementHandler((Statement) s, entity);
            }
        } else if (statement instanceof ExpressionStatement) {
            expressionHandler(((ExpressionStatement) statement).getExpression(), entity);
        } else if (statement instanceof ConstructorInvocation) {
            //TODO call ctor
            IMethodBinding binding = ((ConstructorInvocation) statement).resolveConstructorBinding();
            if (binding != null) {
                StringBuilder tmp = new StringBuilder();
                if (binding.getDeclaringClass() != null) {
                    var types = binding.getParameterTypes();
                    StringBuilder para = new StringBuilder();
                    for (var t : types) {
                        para.append("@").append(t.getErasure().getName());
                    }
                    tmp.append(binding.getDeclaringClass().getErasure().getQualifiedName()).append("$").append(binding.getName()).append(para);
//                    tmp.append(binding.getDeclaringClass().getErasure().getQualifiedName()).append("$").append(binding.getName());
                    entity.getCallingSets().add(tmp.toString());
                }
//                    tmp.append(binding.getDeclaringClass().getQualifiedName()).append("$").append(binding.getName()).append("$");
//                for (var i : binding.getParameterTypes()) {
//                    if (i != null) {
//                        if (i.getDeclaringClass() != null) {
//                            tmp.append(i.getDeclaringClass().getQualifiedName());
//                        }
//                    }
//                }
            }
        } else if (statement instanceof DoStatement) {
            statementHandler(((DoStatement) statement).getBody(), entity);
            expressionHandler(((DoStatement) statement).getExpression(), entity);
        } else if (statement instanceof ForStatement) {
            statementHandler(((ForStatement) statement).getBody(), entity);
            expressionHandler(((ForStatement) statement).getExpression(), entity);
        } else if (statement instanceof IfStatement) {
            expressionHandler(((IfStatement) statement).getExpression(), entity);
            statementHandler(((IfStatement) statement).getElseStatement(), entity);
            statementHandler(((IfStatement) statement).getThenStatement(), entity);
        } else if (statement instanceof SwitchStatement) {
            expressionHandler(((SwitchStatement) statement).getExpression(), entity);
            //TODO stmts
        } else if (statement instanceof ThrowStatement) {
            // TODO 抛出的异常 有可能是RuntimeException
            var expression = ((ThrowStatement) statement).getExpression();
            if (expression instanceof ClassInstanceCreation) {
                var conBinding = ((ClassInstanceCreation) expression).resolveConstructorBinding();
                if (conBinding != null) {
                    entity.getThrowsName().add(conBinding.getDeclaringClass().getErasure().getQualifiedName());
                }
            } else if (expression instanceof SimpleName) {
                var typeBinding = expression.resolveTypeBinding();
                if (typeBinding != null) {
                    entity.getThrowsName().add(typeBinding.getErasure().getQualifiedName());
                }
            } else if (expression instanceof MethodInvocation) {
                var mBinding = ((MethodInvocation) expression).resolveMethodBinding();
                if (mBinding != null) {
                    entity.getThrowsName().add(mBinding.getReturnType().getErasure().getQualifiedName());
                }
            }
        } else if (statement instanceof TryStatement) {
            // for resources
            for (Object e : ((TryStatement) statement).resources()) {
                expressionHandler((VariableDeclarationExpression) e, entity);
            }
            statementHandler(((TryStatement) statement).getBody(), entity);
            statementHandler(((TryStatement) statement).getFinally(), entity);
            List list = ((TryStatement) statement).catchClauses();
            for (Object o : list) {
                statementHandler(((CatchClause) o).getBody(), entity);
                if (((CatchClause) o).getException().getType().isUnionType()) {
                    UnionType unionType = (UnionType) ((CatchClause) o).getException().getType();
                    var types = unionType.types();
                    for (var t : types) {
                        entity.getCatchName().add(((Type) t).resolveBinding().getErasure().getQualifiedName());
                    }
                } else {
                    ITypeBinding binding = ((CatchClause) o).getException().getType().resolveBinding();
                    if (binding != null) {
                        entity.getCatchName().add(binding.getErasure().getQualifiedName());
                    } else {
                        entity.getCatchName().add(((CatchClause) o).getException().getName().getFullyQualifiedName());
                    }
                }
            }
        } else if (statement instanceof WhileStatement) {
            expressionHandler(((WhileStatement) statement).getExpression(), entity);
            statementHandler(((WhileStatement) statement).getBody(), entity);
        } else if (statement instanceof EnhancedForStatement) {
            expressionHandler(((EnhancedForStatement) statement).getExpression(), entity);
            statementHandler(((EnhancedForStatement) statement).getBody(), entity);
        } else if (statement instanceof VariableDeclarationStatement) {
            for (Object o : ((VariableDeclarationStatement) statement).fragments()) {
                expressionHandler(((VariableDeclarationFragment) o).getInitializer(), entity);
            }
        } else if (statement instanceof ReturnStatement) {
            expressionHandler(((ReturnStatement) statement).getExpression(), entity);
        }
    }

    private void expressionHandler(Expression expression, MethodEntity entity) {
        if (expression == null) return;

        if (expression instanceof ArrayAccess) {
            Expression index = ((ArrayAccess) expression).getIndex();
            expressionHandler(index, entity);
        } else if (expression instanceof ArrayCreation) {
            List dimensions = ((ArrayCreation) expression).dimensions();
            for (Object o : dimensions) {
                expressionHandler((Expression) o, entity);
            }
        } else if (expression instanceof Assignment) {
            Expression rightHandSide = ((Assignment) expression).getRightHandSide();
            expressionHandler(rightHandSide, entity);
            expressionHandler(((Assignment) expression).getLeftHandSide(), entity);
        } else if (expression instanceof CastExpression) {
            expressionHandler(((CastExpression) expression).getExpression(), entity);
        } else if (expression instanceof ClassInstanceCreation) {
            //new obj
            expressionHandler(((ClassInstanceCreation) expression).getExpression(), entity);
            IMethodBinding binding = ((ClassInstanceCreation) expression).resolveConstructorBinding();
            if (binding != null) {
                ITypeBinding declaringClass = binding.getDeclaringClass();
                if (binding.getName() == null || binding.getName().equals("")) {
                    var anonymousClassDeclaration = ((ClassInstanceCreation) expression).getAnonymousClassDeclaration();
                    var list = anonymousClassDeclaration.bodyDeclarations();
                    for (Object o : list) {
                        if (o instanceof MethodDeclaration) {
                            statementHandler(((MethodDeclaration) o).getBody(), entity);
                        }
                    }
                } else {
                    //TODO new?
                    StringBuilder output = new StringBuilder();
                    output.append(declaringClass.getErasure().getQualifiedName()).append("$").append(binding.getDeclaringClass().getErasure().getName());
                    var parameters = binding.getParameterTypes();
                    for (var p : parameters) {
                        output.append("@").append(p.getErasure().getName());
                    }
                    entity.getCallingSets().add(output.toString());
                }
            }
        } else if (expression instanceof ConditionalExpression) {
            expressionHandler(((ConditionalExpression) expression).getExpression(), entity);
            expressionHandler(((ConditionalExpression) expression).getElseExpression(), entity);
            expressionHandler(((ConditionalExpression) expression).getThenExpression(), entity);
        } else if (expression instanceof FieldAccess) {
            expressionHandler(((FieldAccess) expression).getExpression(), entity);
        } else if (expression instanceof ParenthesizedExpression) {
            expressionHandler(((ParenthesizedExpression) expression).getExpression(), entity);
        } else if (expression instanceof InfixExpression) {
            expressionHandler(((InfixExpression) expression).getLeftOperand(), entity);
            expressionHandler(((InfixExpression) expression).getRightOperand(), entity);
        }
        // most important part
        else if (expression instanceof MethodInvocation) {
            List arguments = ((MethodInvocation) expression).arguments();
            for (Object o : arguments) {
                expressionHandler(((Expression) o), entity);
            }
            expressionHandler(((MethodInvocation) expression).getExpression(), entity);

            IMethodBinding binding = ((MethodInvocation) expression).resolveMethodBinding();
            if (binding != null) {
                StringBuilder type = new StringBuilder();
                for (var i : binding.getParameterTypes()) {
                    type.append("@").append(i.getErasure().getName());
                }
                var name = binding.getDeclaringClass().getErasure().getQualifiedName();
                entity.getCallingSets().add(name + "$" + binding.getName() + type.toString());
                ITypeBinding parent = binding.getDeclaringClass().getSuperclass();
                if (parent != null) {
                    String pName = parent.getErasure().getQualifiedName();
                    if (!classToSubClass.containsKey(pName)) {
                        classToSubClass.put(pName, new HashSet<>());
                    }
                    classToSubClass.get(pName).add(binding.getDeclaringClass().getErasure().getQualifiedName());
                }
                if (binding.getDeclaringClass().getInterfaces() != null) {
                    for (ITypeBinding t : binding.getDeclaringClass().getInterfaces()) {
                        if (!classToSubClass.containsKey(t.getQualifiedName()) && (!binding.getDeclaringClass().getQualifiedName().startsWith("java") || true)) {
                            classToSubClass.put(t.getErasure().getQualifiedName(), new HashSet<>());
                        }
                        if (!binding.getDeclaringClass().getQualifiedName().startsWith("java") || true)
                            classToSubClass.get(t.getErasure().getQualifiedName()).add(binding.getDeclaringClass().getErasure().getQualifiedName());
                    }
                }
            } else {
                SimpleName name = ((MethodInvocation) expression).getName();
                entity.getCallingSets().add("unknown$" + name);
            }
        } else if (expression instanceof VariableDeclarationExpression) {
            List fragments = ((VariableDeclarationExpression) expression).fragments();
            for (var i : fragments) {
                expressionHandler(((VariableDeclarationFragment) i).getInitializer(), entity);
            }
        } else if (expression instanceof PrefixExpression) {
            expressionHandler(((PrefixExpression) expression).getOperand(), entity);
        }
    }
}
