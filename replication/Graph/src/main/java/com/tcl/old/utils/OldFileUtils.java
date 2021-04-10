package com.tcl.old.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.tcl.old.entity.MethodEntity;
import com.tcl.old.utils.Features;
import com.tcl.old.utils.Parser;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class OldFileUtils {

    private OldFileUtils() {
    }

    /**
     * Get all java files recursively.
     *
     * @param path dir's path
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


    public static List<String> getClassPaths(String rootPath) {
        List<String> re = new ArrayList<>();
        File file = new File(rootPath);
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
     * Converting the original java file to char array.
     *
     * @param path file's path
     * @return char[]
     * @throws IOException file cannot be read in
     */
    public static char[] fileToCharArray(String path) throws IOException {
        File file = new File(path);
        BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
        byte[] bytes = input.readAllBytes();
        char[] re = new char[bytes.length];
        for (int i = 0; i < bytes.length; ++i) {
            re[i] = (char) bytes[i];
        }
        input.close();
        return re;
    }

    /**
     * Converting the parser to the json file.
     *
     * @param parser       specified parser of a project
     * @param jsonFileName json file's name
     * @throws IOException cannot create the json file
     */
    public static void toJsonFiles(Parser parser, String jsonFileName) throws IOException {
        File f = new File("D:\\IdeaProjects\\test\\tmp_re\\" + jsonFileName);
        if (!f.exists()) {
            f.createNewFile();
        }
        FileWriter w = new FileWriter(f);
        w.write(JSON.toJSONString(parser.getList(), SerializerFeature.PrettyFormat));
        w.close();
    }

    /**
     * Converting the parser to a dot(graphviz) file to draw a method calling graph.
     *
     * @param parser the specified parser of one project.
     * @param path   dot file's path
     * @throws IOException cannot create the target file
     */
    public static void toGraphFile(Parser parser, String path) throws IOException {

        File file = new File(path);
        if (!file.createNewFile()) {
            return;
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("strict digraph demo {\n");
        LinkedList<MethodEntity> list = parser.getList();
        HashSet<String> edges = new HashSet<>();
        for (MethodEntity entity : list) {
            HashSet<String> callingSets = entity.getCallingSets();
            Set<String> catchName = entity.getCatchName();
            Set<String> throwsName = entity.getThrowsName();
            if (catchName != null && catchName.size() > 0 &&
                    throwsName != null && throwsName.size() > 0) {
                writer.write("\"" + entity.getMethodName() + "\"[color=green];\n");
            } else if (throwsName != null && throwsName.size() > 0) {
                writer.write("\"" + entity.getMethodName() + "\"[color=blue];\n");
            } else if (catchName != null && catchName.size() > 0) {
                writer.write("\"" + entity.getMethodName() + "\"[color=red];\n");
            }
            if (catchName != null) {
                for (var c : catchName) {
                    writer.write("\"" + c + "\"[shape=box];\n");
                    writer.write("\"" + entity.getMethodName() + "\"->\"" + c + "\";\n");
                }
            }
            if (throwsName != null) {
                for (var c : throwsName) {
                    writer.write("\"" + c + "\"[shape=box];\n");
                    String str = "\"" + entity.getMethodName() + "\"->\"" + c + "\";\n";
                    writer.write(str);
//                    System.out.println(str);
                }
            }
            if (callingSets != null && !callingSets.isEmpty()) {
                for (var j : callingSets) {
                    if (j.startsWith("java.")) {
                        continue;
                    }
                    String[] split = j.split("\\$");
                    StringBuilder tmp = new StringBuilder();
                    tmp.append("  \"").append(entity.getMethodName()).append("\" -> \"").append(split[split.length - 1]).append("\";\n");
                    if (!edges.contains(tmp.toString())) {
                        writer.write(tmp.toString());
                        edges.add(tmp.toString());
                    }
                }
            }
        }
        writer.write("}");
        writer.close();
    }

    /**
     * TODO
     * for class calling graph
     * classMapping represents if this class has exceptions to catch or throw.
     */
    public static void toGraphFile(Iterable<DefaultEdge> edges, String path, Map<String, Integer> classMapping) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            file.createNewFile();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("strict digraph demo {\n");

        for (var tmp : edges) {
            String s1 = tmp.toString();
            String[] split = s1.split(":");
            String a = split[0].substring(1).trim();
            String b = split[1].substring(0, split[1].length() - 1).trim();
            Integer integer = classMapping.get(a);
            switch (integer) {
                case 0:
                    writer.write("  \"" + a + "\" -> \"" + b + "\"\n");
                    break;
                case 1:
                    writer.write("  \"" + a + "\" -> \"" + b + "\"[color=blue]\n");
                    break;
                case 2:
                    writer.write("  \"" + a + "\" -> \"" + b + "\"[color=red]\n");
                    break;
                case 3:
                    writer.write("  \"" + a + "\" -> \"" + b + "\"[color=green]\n");
                    break;
            }
            writer.write("  \"" + a + "\" -> \"" + b + "\"\n");
        }
        writer.write("}");
        writer.close();
    }

    // for method graph
    // full name - key
    public static void toGraphFile(Iterable<DefaultEdge> edges, String path, Parser p) throws IOException {
        HashMap<String, MethodEntity> mapping;
        mapping = p.getNameToEntity();
        File file = new File(path);
        if (!file.exists()) {
            file.createNewFile();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("strict digraph demo {\n");
        for (var edge : edges) {
            String link = edge.toString();
            String[] split = link.split(":");
            String from = split[0].substring(1).trim(), to = split[1].substring(0, split[1].length() - 1).trim();
            if (to.equals("1-catch") || to.equals("2-throw") || to.equals("3-all")) {
                if (to.equals("1-catch")) {
                    writer.write("  \"" + from + "\"[color=blue];\n");
                } else if (to.equals("2-throw")) {
                    writer.write("  \"" + from + "\"[color=red];\n");
                } else {
                    writer.write("  \"" + from + "\"[color=green];\n");
                }
                continue;
            }
            MethodEntity entity = mapping.get(from);
            if (entity.getCatchName() != null && entity.getCatchName().size() > 0
                    && entity.getThrowsName() != null && entity.getThrowsName().size() > 0) {
                writer.write("  \"" + from + "\"[color=green];\n");
                writer.write("  \"" + from + "\" -> \"" + to + "\";\n");
            } else if (entity.getCatchName() != null && entity.getCatchName().size() > 0) {
                writer.write("  \"" + from + "\"[color=blue];\n");
                writer.write("  \"" + from + "\" -> \"" + to + "\";\n");
            } else if (entity.getThrowsName() != null && entity.getThrowsName().size() > 0) {
                writer.write("  \"" + from + "\"[color=red];\n");
                writer.write("  \"" + from + "\" -> \"" + to + "\";\n");
            } else writer.write("  \"" + from + "\" -> \"" + to + "\";\n");
        }
        writer.write("}");
        writer.close();
    }


    // for method graph
    // full name - key
    public static void toGraphFile2(Iterable<DefaultEdge> edges, String path, Parser p) throws IOException {
        HashMap<String, MethodEntity> mapping;
        mapping = p.getNameToEntity();
        File file = new File(path);
        if (!file.exists()) {
            file.createNewFile();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("strict digraph demo {\n");
        writer.write("node[shape=\"rect\"]\n");
        for (var edge : edges) {
            String link = edge.toString();
            String[] split = link.split(":");
            String from = split[0].substring(1).trim(), to = split[1].substring(0, split[1].length() - 1).trim();

//            if (to.equals("1-catch") || to.equals("2-throw") || to.equals("3-all")) {
//                if (to.equals("1-catch")) {
//                    writer.write("  \"" + from + "\"[color=blue];\n");
//                } else if (to.equals("2-throw")) {
//                    writer.write("  \"" + from + "\"[color=red];\n");
//                } else {
//                    writer.write("  \"" + from + "\"[color=green];\n");
//                }
//                continue;
//            }

            MethodEntity entity = mapping.get(from);
            if (entity.getCatchName() != null && entity.getCatchName().size() > 0
                    && entity.getThrowsName() != null && entity.getThrowsName().size() > 0) {
                writer.write("  \"" + from + "\"[color=green];\n");
                writer.write("  \"" + to + "\" -> \"" + from + "\";\n");
            } else if (entity.getCatchName() != null && entity.getCatchName().size() > 0) {
                writer.write("  \"" + from + "\"[color=blue];\n");
                writer.write("  \"" + to + "\" -> \"" + from + "\";\n");

            } else if (entity.getThrowsName() != null && entity.getThrowsName().size() > 0) {
                writer.write("  \"" + from + "\"[color=red];\n");
                writer.write("  \"" + to + "\" -> \"" + from + "\";\n");
            } else {
                writer.write("  \"" + to + "\" -> \"" + from + "\";\n");
            }
        }
        writer.write("}");
        writer.close();
    }


    public static void toFeaturesFile(Features features, String name) throws IOException {
        File file = new File("D:\\IdeaProjects\\test\\tmp_re\\" + name);
        if (!file.createNewFile()) {
            return;
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("MethodName,Exception,Top,Bottom,Throw,Catch\n");
//        writer.write("MethodName,Top,Bottom,Type\n");
        var parser = features.getParser();
        var innerClassDist = features.getInnerClassDist();
        for (var entry : parser.getNameToEntity().entrySet()) {
            MethodEntity entity = entry.getValue();
            String className = entity.getClassName();
            var innerInfo = innerClassDist.get(className);
            if (innerInfo != null) {
                Integer dist = innerInfo.dist.get(entity.getFullName());
                if (dist == null) {
                    continue;
                }
//                switch (Features.getEType(entity)) {
//                    case THROW:
//                        writer.write(entity.getFullName() + "," + dist + "," + (innerInfo.maxDist - dist) + ",throw\n");
//                        break;
//                    case CATCH:
//                        writer.write(entity.getFullName() + "," + dist + "," + (innerInfo.maxDist - dist) + ",catch\n");
//                        break;
//                    case ALL:
//                        writer.write(entity.getFullName() + "," + dist + "," + (innerInfo.maxDist - dist) + ",all\n");
//                    default:
//                        break;
//                }
                Set<String> throwsName = entity.getThrowsName();
                if (throwsName != null && throwsName.size() > 0) {
                    for (var t : throwsName) {
                        writer.write(entity.getFullName() + "," + t + "," + dist + "," + (innerInfo.maxDist - dist) + ",1,0\n");
                    }
                }
                Set<String> catchName = entity.getCatchName();
                if (catchName != null && catchName.size() > 0) {
                    for (var c : catchName) {
                        writer.write(entity.getFullName() + "," + c + "," + dist + "," + (innerInfo.maxDist - dist) + ",0,1\n");
                    }
                }
            }
        }
        writer.close();
    }

    public static void toFullFeaturesFile(Features features, String name, int projectId, String projectName) throws IOException {
        File file = new File(name);
        if (!file.exists()) {
            file.createNewFile();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("ProjectId,ProjectName,Method,Exception,MethodTop,MethodBottom,ClassTop,ClassBottom,PacTop,PacBottom,Throw,Catch,Calling\n");
        var methodDist = features.getInnerClassDist();
        var classDist = features.getClassDist();
        var pacDist = features.getPacDist();
        var parser = features.getParser();
        for (var entry : parser.getNameToEntity().entrySet()) {
            MethodEntity entity = entry.getValue();
            String className = entity.getClassName();
            var innerInfo = methodDist.get(className);
            StringBuilder line = new StringBuilder();
            if (innerInfo != null) {
                Integer dist = innerInfo.dist.get(entity.getFullName());
                if (dist == null) {
                    continue;
                }
                int curClassDist = 0, boCurClassDist = 0, curPacDist = 0, boCurPacDist = 0;
                if (classDist.get(entity.getClassName()) != null) {
                    curClassDist = classDist.get(entity.getClassName()).curDist;
                    boCurClassDist = classDist.get(entity.getClassName()).maxDist - curClassDist;
                }
                if (pacDist.get(entity.getPackageName()) != null) {
                    curPacDist = pacDist.get(entity.getPackageName()).curDist;
                    boCurPacDist = pacDist.get(entity.getPackageName()).maxDist - curPacDist;
                }
                Set<String> throwsName = entity.getThrowsName();


                if (throwsName != null && throwsName.size() > 0) {
                    for (var t : throwsName) {
                        writer.write(projectId + "," + projectName + "," + entity.getFullName() + "," + t + "," + dist + "," + (innerInfo.maxDist - dist)
                                + "," + curClassDist + "," + boCurClassDist + "," + curPacDist + "," + boCurPacDist + ",1,0,");
                        var callingSets = entity.getCallingSets();
                        if (!(callingSets == null || callingSets.size() == 0)) {
                            StringBuilder sb = new StringBuilder();

                            for (var i : callingSets) {
                                sb.append(i).append("|");
                            }
                            writer.write(sb.toString());
                        }
                        writer.write("\n");
                    }
                }


                Set<String> catchName = entity.getCatchName();
                if (catchName != null && catchName.size() > 0) {
                    for (var c : catchName) {
//                        writer.write(entity.getFullName() + "," + c + "," + dist + "," + (innerInfo.maxDist - dist) + ",0,1\n");
                        writer.write(projectId + "," + projectName + "," + entity.getFullName() + "," + c + "," + dist + "," + (innerInfo.maxDist - dist)
                                + "," + curClassDist + "," + boCurClassDist + "," + curPacDist + "," + boCurPacDist + ",0,1,");
                        var callingSets = entity.getCallingSets();
                        if (!(callingSets == null || callingSets.size() == 0)) {
                            StringBuilder sb = new StringBuilder();

                            for (var i : callingSets) {
                                sb.append(i).append("|");
                            }
                            writer.write(sb.toString());
                        }
                        writer.write("\n");
                    }
                }


            }
        }
        writer.close();
    }

    public static void callingLink(DefaultDirectedGraph<String, DefaultEdge> methodGraph, String fileName) throws IOException {
        List<String> collect = methodGraph.vertexSet().stream()
                .filter(v -> methodGraph.inDegreeOf(v) == 0)
                .collect(Collectors.toList());

        File file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        int single = 0, to = 0, trave = 0;
        HashSet<String> vis = new HashSet<>();
        for (var start : collect) {
            DepthFirstIterator<String, DefaultEdge> it = new DepthFirstIterator<>(methodGraph, start);
            StringBuilder sb = new StringBuilder();
            int flag = 0;
            vis.add(start);
            while (it.hasNext()) {
                ++trave;
                String tmp = it.next();
                if (tmp.startsWith("unknown")) {
                    continue;
                }
                sb.append(tmp).append("->");
                ++flag;
            }
            if (flag == 1) single++;
            ++to;
            sb.append('\n');
            writer.write(sb.toString());
        }
        writer.close();
    }


}
