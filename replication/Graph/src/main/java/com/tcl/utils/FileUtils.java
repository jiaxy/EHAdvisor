package com.tcl.utils;

import com.alibaba.fastjson.JSON;
import com.tcl.entity.MethodSignature;
import com.tcl.graph.call.CallEdge;
import com.tcl.graph.call.CallEdgeDyn;
import com.tcl.json.CallChainJson;
import com.tcl.json.InheritJson;
import com.tcl.json.MethodInfoJson;
import com.tcl.parse.ProjectDatabase;
import org.eclipse.jdt.core.dom.ITypeBinding;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FileUtils {
    @Nonnull
    public static String callEdgesToGraphViz(@Nonnull Iterable<CallEdge> edges,
                                             @Nonnull Set<MethodSignature> exceptionSources) {
        var sb = new StringBuilder();
        sb.append("digraph call {\n");
        sb.append("node[shape=\"rect\"]\n");
        for (var s : exceptionSources) {
            String m = s.getSimpleSignature().toString();
            sb.append('"').append(m).append('"').append("[color=red]").append('\n');
        }
        for (var edge : edges) {
            String from = edge.callee.getSimpleSignature().toString();
            String to = edge.caller.getSimpleSignature().toString();
            sb.append('"').append(from).append('"')
                    .append(" -> ")
                    .append('"').append(to).append('"')
                    .append('\n');
        }
        sb.append("}\n");
        return sb.toString();
    }

    @Nonnull
    public static String dynCallEdgesToGraphViz(@Nonnull Iterable<CallEdgeDyn> edges,
                                                @Nonnull Set<MethodSignature> exceptionSources) {
        var list = new ArrayList<CallEdge>();
        edges.forEach(e -> list.add(new CallEdge(e.callee, e.caller)));
        return callEdgesToGraphViz(list, exceptionSources);
    }

    public static void outputProjectToFolder(
            @Nonnull ProjectDatabase db, @Nonnull String folder) throws IOException {
        Files.createDirectories(Paths.get(folder));
        outputMethodInfos(db, folder + "\\method-infos.json");
        outputInheritance(db, folder + "\\class-inherit.json");
        outputCallChains(db, folder + "\\call-chains.json");
    }

    public static void outputMethodInfos(
            @Nonnull ProjectDatabase db, @Nonnull String filepath) throws IOException {
        var methodInfos = db.methodToInfo.values().stream()
                .map(MethodInfoJson::new).collect(Collectors.toList());
        String s = JSON.toJSONString(methodInfos);
        writeToFile(filepath, s);
    }

    public static void outputInheritance(
            @Nonnull ProjectDatabase db, @Nonnull String filepath) throws IOException {
        var clsList = new ArrayList<InheritJson>();
        for (var clsName : db.classToBinding.keySet()) {
            var json = new InheritJson();
            json.setName(clsName);
            for (ITypeBinding x = db.classToBinding.get(clsName);
                 x != null; x = x.getSuperclass()) {
                json.getSuperClasses().add(JdtUtils.toClassName(x));
            }
            clsList.add(json);
        }
        String s = JSON.toJSONString(clsList);
        writeToFile(filepath, s);
    }

    public static void outputCallChains(
            @Nonnull ProjectDatabase db, @Nonnull String filepath) throws IOException {
        var chainList = new ArrayList<CallChainJson>();
        for (var method : db.methodToInfo.keySet()) {
            if (!db.isExceptionSource(method)) {
                continue;
            }
            var chainsFromSrc = db.chainsFromSource(method);
            chainsFromSrc.forEach(chain -> chainList.add(new CallChainJson(chain)));
        }
        String s = JSON.toJSONString(chainList);
        writeToFile(filepath, s);
    }

    public static void writeToFile(
            @Nonnull String filepath, String content) throws IOException {
        var file = new File(filepath);
        if (!file.exists()) {
            boolean b = file.createNewFile();
            assert b;
        }
        var writer = new FileWriter(file);
        writer.write(content);
        writer.close();
    }

    @Nonnull
    public static List<String> getClassPaths(@Nonnull String path) {
        List<String> re = new ArrayList<>();
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            throw new IllegalArgumentException();
        }
        for (File tmp : files) {
            if (tmp.isDirectory() && tmp.getName().endsWith("src")) {
                re.add(tmp.getAbsolutePath());
            } else if (tmp.isDirectory() && !tmp.getName().endsWith("test")) {
                re.addAll(getClassPaths(tmp.getAbsolutePath()));
            }
        }
        return re;
    }

    /**
     * Get all java files recursively.
     *
     * @param path directory path
     * @return LinkedList<String>
     */
    public static List<String> getAllJavaFiles(String path) {
        List<String> re = new ArrayList<>();
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            throw new IllegalArgumentException("The path not exists in your disk.");
        }
        for (File tmp : files) {
            if (tmp.isFile() && tmp.getName().endsWith(".java")) {
                re.add(tmp.getAbsolutePath());
            } else if (tmp.isDirectory() && !tmp.getName().endsWith("test")) {
                re.addAll(getAllJavaFiles(tmp.getAbsolutePath()));
            }
        }
        return re;
    }

    private FileUtils() {
    }
}
