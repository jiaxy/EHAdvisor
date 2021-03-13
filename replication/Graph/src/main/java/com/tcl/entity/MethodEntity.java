package com.tcl.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
//@EqualsAndHashCode
@NoArgsConstructor
public class MethodEntity implements Serializable {
    private String fullName;
    private String methodName;
    private String className;
    private String packageName;
    private List<String> parameters;
//    private List<String> callingLists;
    private HashSet<String> callingSets;
//    private List<String> throwsName;
    private Set<String> throwsName;
//    private List<String> catchName;
    private Set<String> catchName;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("fullName: ").append(fullName);
        if (parameters != null) {
            builder.append("\nparameters: \n");
            if (parameters.isEmpty()) {
                builder.append("\tEmpty.");
            } else for (Object para : parameters) {
//                String typeName = ((SingleVariableDeclaration) para).getType().toString();
//                String varName = ((SingleVariableDeclaration) para).getName().getFullyQualifiedName();
//                builder.append("\t").append(typeName).append("@").append(varName).append("\n");
//                System.out.println(para);
                builder.append("\t").append((String) para).append("\n");
            }
        }
//        if (callingLists != null) {
////            builder.append("\ncallingLists: \n");
////            if (callingLists.isEmpty()) {
////                builder.append("\tEmpty.");
////            }
////            else for (Object m : callingLists) {
////                builder.append("\t").append(m).append("\n");
////            }
//
            builder.append("\ncallingSets: \n");

            if (callingSets == null || callingSets.isEmpty()) {
                builder.append("\tEmpty.");
            } else for (Object m : callingSets) {
                builder.append("\t").append(m).append("\n");
            }
//
//        }
        if (throwsName != null) {
            builder.append("\nthrowsName:\n");
            if (throwsName.isEmpty()) {
                builder.append("\tEmpty.");
            } else for (Object s : throwsName) {
//                builder.append("\t").append(((SimpleType) s).getName().getFullyQualifiedName()).append("\n");
                builder.append("\t").append((String) s).append("\n");
            }
        }
        if (catchName != null) {
            builder.append("\ncatchName:\n");
            if (catchName.isEmpty()) {
                builder.append("\tEmpty.");
            } else for (String s : catchName) {
                if (!s.equals("")) builder.append("\t").append(s).append("\n");
            }
        }
        return builder.toString();
    }

    // TODO
//    @Override
//    public boolean equals(Object m) {
//        return (m instanceof MethodEntity)
//                && fullName.equals(((MethodEntity) m).fullName)
//                && parameters.equals(((MethodEntity) m).parameters);
//    }

    @Override
    public int hashCode() {
        return fullName.hashCode() + parameters.hashCode();
    }


}
