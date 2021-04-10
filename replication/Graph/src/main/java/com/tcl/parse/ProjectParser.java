package com.tcl.parse;

import com.tcl.utils.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

public class ProjectParser {

    private final String projectFolder;
    private final String[] jarPaths;
    private final ASTParser parser = ASTParser.newParser(AST.JLS11);
    private ProjectDatabase database = null;

    public ProjectParser(@Nonnull String projectFolder) {
        this(projectFolder, new String[0]);
    }

    public ProjectParser(@Nonnull String projectFolder, @Nonnull String[] jarPaths) {
        Objects.requireNonNull(projectFolder, "projectFolder");
        Objects.requireNonNull(jarPaths, "jarPaths");
        this.projectFolder = projectFolder;
        this.jarPaths = jarPaths;
    }

    /**
     * Get the {@link ProjectDatabase} of this project
     *
     * @return A {@link ProjectDatabase} containing all program info we need
     */
    @Nonnull
    public ProjectDatabase getDatabase() {
        if (database == null) {
            parse();
        }
        return database;
    }

    /**
     * Parse the whole project and build the project database
     */
    public void parse() {
        clear();
        List<String> classPaths = FileUtils.getClassPaths(projectFolder);
        String property = System.getProperty("java.class.path", ".");
        classPaths.addAll(Arrays.asList(property.split(File.pathSeparator)));
        classPaths.add(System.getProperty("java.home"));
        classPaths.addAll(Arrays.asList(jarPaths));
        List<String> allJavaFiles = FileUtils.getAllJavaFiles(projectFolder);
        setParserEnvironment(classPaths.toArray(new String[0]), new String[]{projectFolder});
        var requestor = new FileASTRequestorImpl();
        parser.createASTs(
                allJavaFiles.toArray(new String[0]), // sourceFilePaths
                null, // encodings
                new String[0], // bindingKeys
                requestor, // requestor
                null // monitor
        );
        database = requestor.getProjectDatabase();
        database.build();
    }

    private void setParserEnvironment(@Nonnull String[] classpathEntries,
                                      @Nonnull String[] sourcePathEntries) {
        parser.setResolveBindings(true);
        Hashtable<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_11);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_11);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_11);
        parser.setCompilerOptions(options);
        parser.setEnvironment(classpathEntries, sourcePathEntries, null, true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);
    }

    public void clear() {
        database = null;
    }

}
