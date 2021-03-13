package com.tcl.utils;

import com.tcl.entity.MethodEntity;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.util.*;
import java.util.stream.Collectors;

public class Features {

    @Getter
    private Parser parser;

    @Getter
    private HashMap<String, InnerInfo> innerClassDist = new HashMap<>();

    static class InnerInfo {
        InnerInfo() {}
        InnerInfo(String name) {
            this.name = name;
        }
        String name;
        LinkedHashMap<String, Integer> dist = new LinkedHashMap<>();
        int maxDist = 0;
        int curDist;
        String lastInsert;
    }

    /**
     * String : name of the class
     * ClassDistInfo : class distance information
     */
    @Getter
    private HashMap<String, ClassDistInfo> classDist = new HashMap<>();

    @Getter
    private HashMap<String, ClassDistInfo> pacDist = new HashMap<>();

    public static class ClassDistInfo {
        ClassDistInfo() {}
        public String className;
        ClassDistInfo(String name) { className = name; }
        public int curDist = Integer.MAX_VALUE;
        public int maxDist = 0;
    }



    public Features() {}

    public Features(Parser parser) {
        this.parser = parser;
        for (MethodEntity entity : parser.getList()) {
            if (!innerClassDist.containsKey(entity.getClassName())) {
                innerClassDist.put(entity.getClassName(), new InnerInfo(entity.getClassName()));
            }
        }
    }

    public static EType getEType(MethodEntity entity) {
        if (entity.getThrowsName() != null && entity.getThrowsName().size() > 0
                && entity.getCatchName() != null && entity.getCatchName().size() > 0) {
            return EType.ALL;
        } else if (entity.getThrowsName() != null && entity.getThrowsName().size() > 0) {
            return EType.THROW;
        } else if (entity.getCatchName() != null && entity.getCatchName().size() > 0) {
            return EType.CATCH;
        } else return EType.NONE;
    }

    public void extractFromMethod(DefaultDirectedGraph<String, DefaultEdge> methodGraph) {
        HashMap<String, MethodEntity> nameToEntity = parser.getNameToEntity();
        List<String> collect = methodGraph.vertexSet().stream()
                .filter(v -> methodGraph.inDegreeOf(v) == 0)
                .collect(Collectors.toList());
        ConnectivityInspector<String, DefaultEdge> conn = new ConnectivityInspector<>(methodGraph);

        HashMap<String, Boolean> vis = new HashMap<>();

        HashSet<String> ed = new HashSet<>();

        for (var start : collect) {
            BreadthFirstIterator<String, DefaultEdge> it = new BreadthFirstIterator<>(methodGraph, start);
            while (it.hasNext()) {
                String tmp = it.next();
                vis.put(tmp, true);
                var entity = nameToEntity.get(tmp);
                if (entity != null) {
                    String className = entity.getClassName();
                    InnerInfo innerInfo = innerClassDist.get(className);
                    if (!innerInfo.dist.containsKey(tmp)) {
                        if (innerInfo.dist.size() > 0) {
                            // todo 暂时为netty关闭
                            if (ed.contains(innerInfo.lastInsert + ":" + tmp) || conn.pathExists(innerInfo.lastInsert, tmp)) {
                                ed.add(innerInfo.lastInsert + ":" +tmp);
                                innerInfo.curDist++;
                            } else {
                                innerInfo.curDist = 0;
                            }
                            innerInfo.dist.put(tmp, innerInfo.curDist);
                        } else {
                            innerInfo.dist.put(tmp, 0);
                            innerInfo.curDist = 0;
                        }
                    }
                    else {
                        innerInfo.curDist = innerInfo.dist.get(tmp);
                    }
                    innerInfo.lastInsert = tmp;
                    innerInfo.maxDist = Math.max(innerInfo.maxDist, innerInfo.curDist);
                }
            }
        }

        for (String v : methodGraph.vertexSet()) {
            if (!vis.containsKey(v)) {
                BreadthFirstIterator<String, DefaultEdge> it = new BreadthFirstIterator<>(methodGraph);
                while (it.hasNext()) {
                    String tmp = it.next();
                    if (vis.containsKey(tmp)) continue;
                    vis.put(tmp, true);
                    var entity = nameToEntity.get(tmp);
                    if (entity != null) {
                        String className = entity.getClassName();
                        InnerInfo innerInfo = innerClassDist.get(className);
                        if (!innerInfo.dist.containsKey(tmp)) {
                            if (innerInfo.dist.size() > 0) {
                                // todo 暂时为netty关闭
                                if (conn.pathExists(innerInfo.lastInsert, tmp)) {
                                    innerInfo.curDist++;
                                } else {
                                    innerInfo.curDist = 0;
                                }

                                innerInfo.dist.put(tmp, innerInfo.curDist);
                            } else {
                                innerInfo.dist.put(tmp, 0);
                                innerInfo.curDist = 0;
                            }
                        }
                        else {
                            innerInfo.curDist = innerInfo.dist.get(tmp);
                        }
                        innerInfo.lastInsert = tmp;
                        innerInfo.maxDist = Math.max(innerInfo.maxDist, innerInfo.curDist);
                    }
                }
            }
        }
    }

    public void extractFromClass(DefaultDirectedGraph<String, DefaultEdge> classGraph) {

        List<String> starts = classGraph.vertexSet()
                                        .stream()
                                        .filter(v -> classGraph.inDegreeOf(v) == 0)
                                        .collect(Collectors.toList());

        HashMap<String, Boolean> vis = new HashMap<>();

//        System.out.println("Current Class Graph vertices size : " + classGraph.vertexSet().size());
//        System.out.println("Current start points size : " + starts.size());


        for (String start : starts) {
            BreadthFirstIterator<String, DefaultEdge> it = new BreadthFirstIterator<>(classGraph, start);
            while (it.hasNext()) {
                String tmp = it.next();
                var depth = it.getDepth(tmp);
                if (!classDist.containsKey(tmp)) {
                    classDist.put(tmp, new ClassDistInfo(tmp));
                }
                classDist.get(tmp).curDist = Math.min(classDist.get(tmp).curDist, depth);
                classDist.get(tmp).maxDist = Math.max(classDist.get(tmp).maxDist, depth);
                vis.put(tmp, true);
            }
        }

        for (String v : classGraph.vertexSet()) {
            if (!vis.containsKey(v)) {
                BreadthFirstIterator<String, DefaultEdge> it = new BreadthFirstIterator<>(classGraph);
                while (it.hasNext()) {
                    String tmp = it.next();
                    if (!vis.containsKey(tmp)) {
                        vis.put(tmp, true);
                        classDist.put(tmp, new ClassDistInfo(tmp));
                        int depth = it.getDepth(tmp);
                        classDist.get(tmp).curDist = Math.min(classDist.get(tmp).curDist, depth);
                        classDist.get(tmp).maxDist = Math.max(classDist.get(tmp).maxDist, depth);
                    }
                }
            }
        }

//        System.out.println("ClassDist size : " + classDist.size());
    }

    public void extractFromPackage(DefaultDirectedGraph<String, DefaultEdge> pacGraph) {
        HashMap<String, Boolean> vis = new HashMap<>();
        List<String> starts = pacGraph.vertexSet().stream().filter(v -> pacGraph.inDegreeOf(v) == 0).collect(Collectors.toList());
        for (String start : starts) {
            BreadthFirstIterator<String, DefaultEdge> it = new BreadthFirstIterator<>(pacGraph, start);
            while (it.hasNext()) {
                String tmp = it.next();
                var depth = it.getDepth(tmp);
                if (!pacDist.containsKey(tmp)) {
                    pacDist.put(tmp, new ClassDistInfo(tmp));
                }
                pacDist.get(tmp).curDist = Math.min(pacDist.get(tmp).curDist, depth);
                pacDist.get(tmp).maxDist = Math.max(pacDist.get(tmp).maxDist, depth);
                vis.put(tmp, true);
            }
        }


        for (String v : pacGraph.vertexSet()) {
            if (!vis.containsKey(v)) {
                BreadthFirstIterator<String, DefaultEdge> it = new BreadthFirstIterator<>(pacGraph);
                while (it.hasNext()) {
                    String tmp = it.next();
                    if (!vis.containsKey(tmp)) {
                        vis.put(tmp, true);
                        pacDist.put(tmp, new ClassDistInfo(tmp));
                        int depth = it.getDepth(tmp);
                        pacDist.get(tmp).curDist = Math.min(pacDist.get(tmp).curDist, depth);
                        pacDist.get(tmp).maxDist = Math.max(pacDist.get(tmp).maxDist, depth);
                    }
                }
            }
        }
    }

}

class MethodInfo implements Comparable<MethodInfo> {

    public int depth;

    public String name;

    @Override
    public int compareTo(@NotNull MethodInfo o) {
        return Integer.compare(depth, o.depth);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MethodInfo && name.equals(((MethodInfo) obj).name);
    }
}


