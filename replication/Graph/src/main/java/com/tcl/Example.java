package com.tcl;

import com.tcl.parse.ProjectDatabase;
import com.tcl.parse.ProjectParser;
import com.tcl.utils.FileUtils;

import java.io.IOException;

public class Example {
    public static void main(String[] args) {
        String proj = "project folder here";
        String[] jars = {"jar path here", "jar path here"};
        String outputFolder = "output folder here";
        var parser = new ProjectParser(proj, jars);
        parser.parse();
        ProjectDatabase db = parser.getDatabase();
        try {
            FileUtils.outputProjectToFolder(db, outputFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
