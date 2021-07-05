package com.tcl.old.utils;

import com.tcl.old.entity.MethodEntity;
import lombok.Getter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class GBuilder {

    // 0 - none  1 - catch  2 - throw  3 - all
    public Map<String, EType> classException = new HashMap<>();

    @Getter
    private DefaultDirectedGraph<String, DefaultEdge> methodG;
    @Getter
    private DefaultDirectedGraph<String, DefaultEdge> packageG;
    @Getter
    private DefaultDirectedGraph<String, DefaultEdge> classG;

    private HashMap<String, MethodEntity> nameToEntity;

    public GBuilder() {}

    public GBuilder(Parser parser) {
        this.nameToEntity = parser.getNameToEntity();
    }

    public DefaultDirectedGraph<String, DefaultEdge> buildMethodGraph(Parser parser) {
        LinkedList<MethodEntity> list = parser.getList();
        methodG = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (MethodEntity m : list) {
            if (!methodG.containsVertex(m.getFullName())) {
                methodG.addVertex(m.getFullName());
            }
            if (m.getCallingSets() != null && m.getCallingSets().size() > 0) {
                for (var inner : m.getCallingSets()) {
                    if (inner.startsWith("java.") || inner.startsWith("javax.")) continue;
                    if (!methodG.containsVertex(inner)) {
                        methodG.addVertex(inner);
                    }
                    methodG.addEdge(m.getFullName(), inner);
                }
            } else {
                if ((m.getCatchName() != null && m.getCatchName().size() > 0)
                        || (m.getThrowsName() != null && m.getThrowsName().size() > 0)) {
                    methodG.addVertex(m.getFullName());
                }

//                if (m.getCatchName() != null && m.getCatchName().size() > 0
//                        && m.getThrowsName() != null && m.getThrowsName().size() > 0) {
//
//                    // TODO
////                    methodG.addEdge(m.getFullName(), "3-all");
////                    System.out.println("reached here-3" + m.getFullName());
//                } else if (m.getCatchName() != null && m.getCatchName().size() > 0) {
//
//                    // TODO
////                    methodG.addEdge(m.getFullName(), "1-catch");
////                    System.out.println("reached here-1" + m.getFullName());
//                } else if (m.getThrowsName() != null && m.getThrowsName().size() > 0) {
//
//                    // TODO
////                    methodG.addEdge(m.getFullName(), "2-throw");
////                    System.out.println("reached here-2" + m.getFullName());
//                }
            }
        }
        Set<String> strings = new HashSet<>(methodG.vertexSet());
        for (var i : strings) {
            if (methodG.inDegreeOf(i) == 0 && methodG.outDegreeOf(i) == 0) {
                MethodEntity entity = nameToEntity.get(i);
                if ((entity.getThrowsName() == null || entity.getThrowsName().size() == 0)
                        && (entity.getCatchName() == null || entity.getCatchName().size() == 0))
                {
                    methodG.removeVertex(i);
                }
            }
        }
        return methodG;
    }

    public DefaultDirectedGraph<String, DefaultEdge> buildClassGraph(Parser parser) {
        classG = new DefaultDirectedGraph<>(DefaultEdge.class);
        Map<String, String> mapping = new HashMap<>();
        LinkedList<MethodEntity> list = parser.getList();
        for (var m : list) {
            mapping.put(m.getFullName(), m.getClassName());
            if (m.getThrowsName() != null && m.getThrowsName().size() > 0
                    && m.getCatchName() != null && m.getCatchName().size() > 0) {
                classException.put(m.getClassName(), EType.ALL);
            } else if (m.getThrowsName() != null && m.getThrowsName().size() > 0) {
                classException.put(m.getClassName(), EType.THROW);
            } else if (m.getCatchName() != null && m.getCatchName().size() > 0) {
                classException.put(m.getClassName(), EType.CATCH);
            } else classException.put(m.getClassName(), EType.NONE);
        }
        for (var m : list) {
            if (!classG.containsVertex(m.getClassName())) {
                classG.addVertex(m.getClassName());
            }
            if (m.getCallingSets() != null && m.getCallingSets().size() > 0) {
                for (var i : m.getCallingSets()) {
                    if (!mapping.containsKey(i)) continue;
                    String callingClass = mapping.get(i);
                    if (!classG.containsVertex(callingClass)) {
                        classG.addVertex(callingClass);
                    }
                    classG.addEdge(m.getClassName(), callingClass);
//                    String[] split = i.split("\\$");
//                    if (split != null) {
//                        String[] split1 = split[0].split("\\.");
//                        if (split1 != null) {
//                            String tmp = split1[split1.length - 1];
//                            if (!classG.containsVertex(tmp)) {
//                                classG.addVertex(tmp);
//                            }
//                            classG.addEdge(m.getClassName(), tmp);
//                        }
//                    }
                }
            }
        }
        Set<String> strings = new HashSet<>(classG.vertexSet());
        for (var i : strings) {
            if (classException.get(i) == EType.NONE) {
                classG.removeVertex(i);
            }
//            if (classG.inDegreeOf(i) == 0 && classG.outDegreeOf(i) == 0) {
//                classG.removeVertex(i);
//            }
            /*else if ((classG.outDegreeOf(i) == 1 && classG.containsEdge(i, i)) || (
                        classG.inDegreeOf(i) == 1 && classG.containsEdge(i, i)
                    )) {
                classG.removeVertex(i);
            }*/

        }
        return classG;
    }

    public void buildPAndCGraph() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Set<DefaultEdge> edges = methodG.edgeSet();

        classG = new DefaultDirectedGraph<>(DefaultEdge.class);
        packageG = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (DefaultEdge edge : edges) {
            Class<? extends DefaultEdge> clazz = edge.getClass();
            var getSource = clazz.getDeclaredMethod("getSource");
            var getTarget = clazz.getDeclaredMethod("getTarget");
            getSource.setAccessible(true);
            getTarget.setAccessible(true);
            String source = (String) getSource.invoke(edge);
            String target = (String) getTarget.invoke(edge);

            MethodEntity e1 = nameToEntity.get(source);
            MethodEntity e2 = nameToEntity.get(target);

            String e1_class = e1.getClassName();
            String e1_package = e1.getPackageName();
            String e2_class;
            String e2_package;

//            if (e2 == null) {
//                var split = target.split("\\$");
//                System.out.println(target);
////                e2_class = split[split.length - 1];
////                e2_package = split[0].replaceAll("\\." + e2_class, "");
////                System.out.println(e2_class);
////                System.out.println(e2_package);
//            }

            if (e2 != null) {
                if (!classG.containsVertex(e1.getClassName())) classG.addVertex(e1.getClassName());
                if (!classG.containsVertex(e2.getClassName())) classG.addVertex(e2.getClassName());
                classG.addEdge(e1.getClassName(), e2.getClassName());
                if (!packageG.containsVertex(e1.getPackageName())) packageG.addVertex(e1.getPackageName());
                if (!packageG.containsVertex(e2.getPackageName())) packageG.addVertex(e2.getPackageName());
                packageG.addEdge(e1.getPackageName(), e2.getPackageName());
            }

//            System.out.println(getSource.invoke(null));
//            Field source = clazz.getDeclaredField("source");
//            Field target = clazz.getDeclaredField("target");
//            System.out.println(source.toString() + " -> " + target.toString());
        }
    }


}


