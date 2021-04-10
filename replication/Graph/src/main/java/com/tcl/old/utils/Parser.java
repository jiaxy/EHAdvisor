package com.tcl.old.utils;

import com.tcl.old.entity.MethodEntity;
import lombok.Getter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Parser {

    private ASTParser parser = ASTParser.newParser(AST.JLS11);

    @Getter
    private LinkedList<MethodEntity> list = new LinkedList<>();

    @Getter
    private HashMap<String, MethodEntity> nameToEntity = new HashMap<>();

    @Getter
    private HashMap<String, HashSet<String>> classToSubClass = new HashMap<>();


    /**
     * @param classpath representing the project path
     * @param src       could be same to the classpath
     */
    private void setEnvironment(String[] classpath, String[] src) {
        parser.setResolveBindings(true);
        Hashtable<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_11);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_11);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_11);
        parser.setCompilerOptions(options);
        parser.setEnvironment(classpath, src, null, true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);
    }

    /**
     * @param path path of a single java file
     * @throws IOException path not exists
     */
    public void parseJavaFile(String path) throws IOException {
        parser.setUnitName(path);
        parser.setSource(OldFileUtils.fileToCharArray(path));
        CompilationUnit cu = null;
        try {
            cu = (CompilationUnit) parser.createAST(null);
        } catch (Exception e) {
            return;
        }
//        if (cu == null) return;
        MethodVisitor visitor = new MethodVisitor(list, cu, classToSubClass);
//        visitor.parser = this;
        cu.accept(visitor);
        cu.recordModifications();
    }

    /**
     * @param path the project path
     * @throws IOException file path not exist
     */
    public void parseProject(String path, String jarPath) throws IOException {
        List<String> classPath = OldFileUtils.getClassPaths(path);
        String[] src = {path};
        String property = System.getProperty("java.class.path", ".");
        String[] split = property.split(File.pathSeparator);
        classPath.addAll(Arrays.asList(split));
        classPath.add(System.getProperty("java.home"));
        File dir = new File(jarPath);
        for (File f : dir.listFiles()) {
            classPath.add(f.getAbsolutePath());
        }

        String[] cp = classPath.toArray(new String[0]);
//        String[] cp = new String[split.length + 2];
//        System.arraycopy(split, 0, cp, 0, split.length);
//        cp[cp.length - 2] = path;
//        cp[cp.length - 1] = System.getProperty("java.home");

        List<String> allJavaFiles = OldFileUtils.getAllJavaFiles(src[0]);

//        for (String s : allJavaFiles) {
//            setEnvironment(cp, src);
//            try {
//                parseJavaFile(s);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }

        OldFileASTRequestor requestor = new OldFileASTRequestor();
        requestor.list = list;
        requestor.classToSubClass = classToSubClass;
        setEnvironment(cp, src);
        String[] arr2 = {};
        parser.createASTs(allJavaFiles.toArray(new String[0]), null, arr2, requestor, null);

        for (MethodEntity entity : list) {
            nameToEntity.put(entity.getFullName(), entity);
        }
    }
}

