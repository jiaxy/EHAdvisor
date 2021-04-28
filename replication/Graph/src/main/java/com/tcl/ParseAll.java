package com.tcl;

import com.tcl.parse.ProjectDatabase;
import com.tcl.parse.ProjectParser;
import com.tcl.utils.FileUtils;

import java.io.File;
import java.io.IOException;

public class ParseAll {
    public static void main(String[] args) {
        String inPrefix = "E:\\projects";
        String outPrefix = "E:\\outputs";
        for (int i = 0; i < projects.length; i++) {
            String name = projects[i];
            if (name.equals("flink")) {
                continue; //跳过 flink，过于缓慢
            }
            String inFolder = inPrefix + '\\' + name;
            String outFolder = outPrefix + '\\' + name;
            mkDirsIfNotExist(outFolder);
            long parseStartTime = System.currentTimeMillis();
            var parser = new ProjectParser(inFolder);
            try {
                System.out.println(name + " parse start");
                parser.parse();
                System.out.println(name + " parse end");
            } catch (Exception e) {
                System.out.println("proj: " + name + ", when: parser.parse()");
                e.printStackTrace();
                continue;
            }
            long parseStopTime = System.currentTimeMillis();
            ProjectDatabase db = parser.getDatabase();
            try {
                System.out.println(name + " output start");
                FileUtils.outputProjectToFolder(db, outFolder);
                System.out.println(name + " output end");
            } catch (IOException e) {
                System.out.println("proj: " + name + ", when: FileUtils.outputProjectToFolder");
                e.printStackTrace();
            }
            long outputStopTime = System.currentTimeMillis();
            System.out.println("parse use: " + secondUsed(parseStartTime, parseStopTime)
                    + "s, output use: " + secondUsed(parseStopTime, outputStopTime)
                    + "s, all use: " + secondUsed(parseStartTime, outputStopTime) + "s.");
        }
    }

    static void mkDirsIfNotExist(String dirPath) {
        var dir = new File(dirPath);
        if (!dir.exists()) {
            boolean b = dir.mkdirs();
            assert b;
        }
    }

    static long secondUsed(long startMillis, long stopMillis) {
        return (stopMillis - startMillis) / 1000;
    }

    static String[] projects = {
            "activemq",//0
            "c3p0",//1
            "commons-collections",//2
            "commons-dbcp",//3
            "commons-logging",//4
            "druid",//5
            "dubbo",//6
            "fastjson",//7
            "flink",//8 跳过 flink，过于缓慢
            "grizzly",//9
            "gson",//10
            "guava",//11
            "hbase",//12
            "HikariCP",//13
            "httpclient",//14
            "jackson",//15
            "jersey",//16
            "log4j2",//17
            "mybatis",//18
            "netty",//19
            "rocketmq",//20
            "shiro",//21
            "slf4j",//22
            "spring-data-jpa",//23
            "storm",//24
            "struts",//25
            "tomcat",//26
            "xnio",//27
            "zookeeper"//28
    };

}
