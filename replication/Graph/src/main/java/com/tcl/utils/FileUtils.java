package com.tcl.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.tcl.entity.MethodSignature;
import com.tcl.graph.call.CallEdge;
import com.tcl.graph.call.CallEdgeDyn;
import com.tcl.graph.call.ExceptionMethodPair;
import com.tcl.json.CallChainJson;
import com.tcl.json.InheritJson;
import com.tcl.json.MethodInfoJson;
import com.tcl.json.PredictUnitJson;
import com.tcl.parse.ProjectDatabase;
import org.eclipse.jdt.core.dom.ITypeBinding;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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
        outputInheritance(db, folder + "\\exception-inherit.json");
        outputPredictUnitsCsv(db, folder + "\\predict-units.csv");
    }

    @Deprecated
    public static void outputProjectToFolder1(
            @Nonnull ProjectDatabase db, @Nonnull String folder) throws IOException {
        Files.createDirectories(Paths.get(folder));
        outputMethodInfos(db, folder + "\\method-infos.json");
        outputInheritance(db, folder + "\\exception-inherit.json");
        outputCallChains(db, folder + "\\call-chains.json");
    }

    @Deprecated
    public static void outputProjectToFolder2(
            @Nonnull ProjectDatabase db, @Nonnull String folder) throws IOException {
        Files.createDirectories(Paths.get(folder));
        outputMethodInfos(db, folder + "\\method-infos.json");
        outputInheritance(db, folder + "\\exception-inherit.json");
//        outputPredictUnits(db, folder + "\\predict-units.json");
        outputPredictUnitsCsv1(db, folder + "\\predict-units.csv");
    }

    public static void outputMethodInfos(
            @Nonnull ProjectDatabase db, @Nonnull String filepath) throws IOException {
        var methodInfos = new ArrayList<MethodInfoJson>();
        for (var method : db.methodToInfo.keySet()) {
            var info = db.methodToInfo.get(method);
            methodInfos.add(new MethodInfoJson(info, db.isExceptionSource(method)));
        }
        JSON.writeJSONString(createWriter(filepath), methodInfos,
                SerializerFeature.PrettyFormat);
    }

    public static void outputInheritance(
            @Nonnull ProjectDatabase db, @Nonnull String filepath) throws IOException {
        var clsList = new ArrayList<InheritJson>();
        for (var clsName : db.classToBinding.keySet()) {
            if (!db.isThrowableType(clsName)) {
                continue;
            }
            var json = new InheritJson();
            json.setName(clsName);
            for (ITypeBinding x = db.classToBinding.get(clsName);
                 x != null; x = x.getSuperclass()) {
                json.getSuperClasses().add(JdtUtils.toClassName(x));
            }
            clsList.add(json);
        }
        JSON.writeJSONString(createWriter(filepath), clsList,
                SerializerFeature.PrettyFormat);
    }

    public static void outputPredictUnitsCsv(
            @Nonnull ProjectDatabase db, @Nonnull String filepath) throws IOException {
        try (FileWriter writer = createWriter(filepath)) {
            //(exception, simpleMethod) will be unique in csv
            var pairSet = new HashSet<ExceptionMethodPair>();
            String header =
                    "throwFrom," +
                            "exception," +
                            "simpleMethod," +
                            "methodTop," +
                            "methodBottom," +
                            "classTop," +
                            "classBottom," +
                            "packageTop," +
                            "packageBottom," +
                            "chainTop," +
                            "chainBottom," +
                            "handled";
            writer.write(header + '\n');
            for (var method : db.methodToInfo.keySet()) {
                if (!db.isExceptionSource(method)) {
                    continue;
                }
                for (var unit : db.predUnitsFromSource(method)) {
                    var exMethod = new ExceptionMethodPair(unit.exception, unit.simpleMethod);
                    if (pairSet.contains(exMethod)) {
                        continue;
                    }
                    pairSet.add(exMethod);
//                    if (pairSet.size() % 100 == 1) {
//                        System.out.println("pairSet.size = " + pairSet.size());
//                    }
                    var sj = new StringJoiner(",");
                    sj.add('"' + unit.throwFrom + '"')
                            .add(unit.exception)
                            .add('"' + unit.simpleMethod + '"')
                            .add(String.valueOf(unit.position.methodTop))
                            .add(String.valueOf(unit.position.methodBottom))
                            .add(String.valueOf(unit.position.classTop))
                            .add(String.valueOf(unit.position.classBottom))
                            .add(String.valueOf(unit.position.packageTop))
                            .add(String.valueOf(unit.position.packageBottom))
                            .add(String.valueOf(unit.position.chainTop))
                            .add(String.valueOf(unit.position.chainBottom))
                            .add(unit.handled ? "1" : "0");
                    writer.write(sj.toString() + '\n');
                }
            }
        }
    }

    @Deprecated
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
        JSON.writeJSONString(createWriter(filepath), chainList,
                SerializerFeature.PrettyFormat);
    }

    @Deprecated
    public static void outputPredictUnitsJson(
            @Nonnull ProjectDatabase db, @Nonnull String filepath) throws IOException {
        var units = new ArrayList<PredictUnitJson>();
        for (var method : db.methodToInfo.keySet()) {
            if (!db.isExceptionSource(method)) {
                continue;
            }
            var chainsFromSrc = db.chainsFromSource(method);
            chainsFromSrc.forEach(
                    chain -> units.addAll(
                            PredictUnitJson.predictUnitsFromChain(chain)));
        }
        JSON.writeJSONString(createWriter(filepath), units,
                SerializerFeature.PrettyFormat);
    }

    @Deprecated
    public static void outputPredictUnitsCsv1(
            @Nonnull ProjectDatabase db, @Nonnull String filepath) throws IOException {
        try (FileWriter writer = createWriter(filepath)) {
            String header = "chainId, " +
                    "throwFrom, " +
                    "exception, " +
                    "simpleMethod, " +
                    "methodBottom, " +
                    "methodTop, " +
                    "classBottom, " +
                    "classTop, " +
                    "packageBottom, " +
                    "packageTop, " +
                    "posInChain, " +
                    "handled";
            writer.write(header + '\n');
            int chainNum = 0;
            for (var method : db.methodToInfo.keySet()) {
                if (!db.isExceptionSource(method)) {
                    continue;
                }
                for (var chain : db.chainsFromSource(method)) {
                    List<PredictUnitJson> units = PredictUnitJson.predictUnitsFromChain(chain);
                    for (var unit : units) {
                        var sj = new StringJoiner(", ");
                        sj.add(String.valueOf(chainNum))
                                .add('"' + unit.getSource().getThrowFrom() + '"')
                                .add(unit.getSource().getException())
                                .add('"' + unit.getChainEntry().getSimpleMethod() + '"')
                                .add(String.valueOf(unit.getChainEntry().getMethodBottom()))
                                .add(String.valueOf(unit.getChainEntry().getMethodTop()))
                                .add(String.valueOf(unit.getChainEntry().getClassBottom()))
                                .add(String.valueOf(unit.getChainEntry().getClassTop()))
                                .add(String.valueOf(unit.getChainEntry().getPackageBottom()))
                                .add(String.valueOf(unit.getChainEntry().getPackageTop()))
                                .add(String.valueOf(unit.getChainEntry().getPosInChain()))
                                .add(unit.getChainEntry().isHandled() ? "1" : "0");
                        writer.write(sj.toString() + '\n');
                    }
                    chainNum++;
                }
            }
        }
    }

    @Nonnull
    public static FileWriter createWriter(@Nonnull String filepath)
            throws IOException {
        var file = new File(filepath);
        if (!file.exists()) {
            boolean b = file.createNewFile();
            assert b;
        }
        return new FileWriter(file);
    }

    public static void writeToFile(
            @Nonnull String filepath, String content) throws IOException {
        try (FileWriter writer = createWriter(filepath)) {
            writer.write(content);
        }
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

    public static void mkDirsIfNotExist(String dirPath) {
        var dir = new File(dirPath);
        if (!dir.exists()) {
            boolean b = dir.mkdirs();
            assert b;
        }
    }

    private FileUtils() {
    }
}
