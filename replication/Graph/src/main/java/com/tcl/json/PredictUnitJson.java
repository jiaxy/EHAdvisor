package com.tcl.json;

import com.tcl.entity.MethodSignature;
import com.tcl.graph.call.CallChain;

import java.util.*;

@Deprecated
public class PredictUnitJson {
    private final ExceptionSourceJson source;
    private final ChainEntryJson2 chainEntry;

    public PredictUnitJson(ExceptionSourceJson source, ChainEntryJson2 chainEntry) {
        this.source = source;
        this.chainEntry = chainEntry;
    }

    public ExceptionSourceJson getSource() {
        return source;
    }

    public ChainEntryJson2 getChainEntry() {
        return chainEntry;
    }

    public static List<PredictUnitJson> predictUnitsFromChain(CallChain chain) {
        //multi-map
        var clsMethods = new HashMap<String, LinkedHashSet<MethodSignature>>();
        var pkgClasses = new HashMap<String, LinkedHashSet<String>>();
        var packageSet = new LinkedHashSet<String>();
        for (var entry : chain.getChain()) {
            String className = entry.getMethod().getQualifiedClassName();
            clsMethods.putIfAbsent(className, new LinkedHashSet<>());
            clsMethods.get(className).add(entry.getMethod());
            String pkg = entry.getMethod().getPackageName().orElse(null);
            pkgClasses.putIfAbsent(pkg, new LinkedHashSet<>());
            pkgClasses.get(pkg).add(entry.getMethod().getQualifiedClassName());
            packageSet.add(pkg);
        }
        var methodPos = new LinkedHashMap<MethodSignature, ChainEntryJson2>();
        for (var entry : chain.getChain()) {
            var json = new ChainEntryJson2();
            json.setSimpleMethod(entry.getMethod().getSimpleSignature().toString());
            json.setHandled(entry.isHandled());
            methodPos.put(entry.getMethod(), json);
        }
        //method-top, method-bottom
        for (String className : clsMethods.keySet()) {
            var methodSet = clsMethods.get(className);
            int i = 0;
            for (var method : methodSet) {
                methodPos.get(method).setMethodTop(i);
                methodPos.get(method).setMethodBottom(methodSet.size() - 1 - i);
                i++;
            }
        }
        //class-top, class-bottom
        var clsTopDict = new HashMap<String, Integer>();
        var clsBottomDict = new HashMap<String, Integer>();
        for (String pkg : pkgClasses.keySet()) {
            var classSet = pkgClasses.get(pkg);
            int i = 0;
            for (String className : classSet) {
                clsTopDict.put(className, i);
                clsBottomDict.put(className, classSet.size() - 1 - i);
                i++;
            }
        }
        for (var method : methodPos.keySet()) {
            methodPos.get(method).setClassTop(
                    clsTopDict.get(method.getQualifiedClassName()));
            methodPos.get(method).setClassBottom(
                    clsBottomDict.get(method.getQualifiedClassName()));
        }
        //package-top, package-bottom
        var pkgTopDict = new HashMap<String, Integer>();
        var pkgBottomDict = new HashMap<String, Integer>();
        int i = 0;
        for (String pkg : packageSet) {
            pkgTopDict.put(pkg, i);
            pkgBottomDict.put(pkg, packageSet.size() - 1 - i);
            i++;
        }
        for (var method : methodPos.keySet()) {
            methodPos.get(method).setPackageTop(
                    pkgTopDict.get(method.getPackageName().orElse(null))
            );
            methodPos.get(method).setPackageBottom(
                    pkgBottomDict.get(method.getPackageName().orElse(null))
            );
        }
        //pos-in-chain
        for (i = 0; i < chain.getChain().size(); i++) {
            MethodSignature method = chain.getChain().get(i).getMethod();
            methodPos.get(method).setPosInChain(i + 1);
        }
        //build predict units
        var predictUnits = new ArrayList<PredictUnitJson>();
        var source = new ExceptionSourceJson(
                chain.getThrowFrom().getSimpleSignature().toString(),
                chain.getException());
        for (var entry : methodPos.values()) {
            predictUnits.add(new PredictUnitJson(source, entry));
        }
        return predictUnits;
    }
}
