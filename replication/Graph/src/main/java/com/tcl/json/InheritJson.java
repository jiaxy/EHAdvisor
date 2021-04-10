package com.tcl.json;

import java.util.ArrayList;
import java.util.List;

public class InheritJson {
    private String name;
    private List<String> superClasses = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSuperClasses() {
        return superClasses;
    }

    public void setSuperClasses(List<String> superClasses) {
        this.superClasses = superClasses;
    }
}
