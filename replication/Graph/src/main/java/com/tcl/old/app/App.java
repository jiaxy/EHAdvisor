package com.tcl.old.app;

import com.tcl.old.utils.OldFileUtils;
import com.tcl.old.utils.GBuilder;
import com.tcl.old.utils.Parser;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.File;
import java.io.IOException;


public class App
{

    public static String prefix = "F:\\data\\";
    public static void main(String[] args) throws IOException {

        String[] path = {"big-data", "commons", "database-connection", "json", "logging", "network", "orm", "web"};
        String suffix = "\\project";

        int i = -1;

        for (String s : path) {
            if (s.equals("commons")) break;
            File file = new File(prefix + s + suffix);
            File[] files = file.listFiles();
            for (File f : files) {
                if (i == 1) break;
                ++i;
                String projectName = f.getName();
                System.out.println("Now parsing project " + i + ": " + projectName);
                System.out.println("=======================================================");
                Parser p = new Parser();
//                if (!f.getAbsolutePath().endsWith("flink")) continue;
                p.parseProject(f.getAbsolutePath(), prefix + s + "\\jar");
                GBuilder builder = new GBuilder(p);
                DefaultDirectedGraph<String, DefaultEdge> methodGraph = builder.buildMethodGraph(p);
//                try {
//                    builder.buildPAndCGraph();
//                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
//                    e.printStackTrace();
//                }
                System.out.println("method edge size: " + methodGraph.edgeSet().size());
                System.out.println("method vertex size: "+ methodGraph.vertexSet().size());
//                System.out.println("class edge size: " + builder.getClassG().edgeSet().size());
//                System.out.println("class vertex size: " + builder.getClassG().vertexSet().size());
//                System.out.println("package edge size: " + builder.getPackageG().edgeSet().size());
//                System.out.println("package vertex size: " + builder.getPackageG().vertexSet().size());

//                Features features = new Features(p);
//                features.extractFromMethod(methodGraph);
//
//                features.extractFromClass(builder.getClassG());
//
//                features.extractFromPackage(builder.getPackageG());
//
//                FileUtils.toFullFeaturesFile(features, prefix + projectName + ".csv", i, projectName);
                System.out.println("=======================================================");
                OldFileUtils.callingLink(methodGraph, prefix + projectName + ".txt");

            }
        }
    }

}











