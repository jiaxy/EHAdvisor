package com.tcl.parse;

import com.tcl.entity.MethodInfo;
import com.tcl.entity.MethodSignature;
import com.tcl.graph.call.*;
import com.tcl.graph.inheritance.InheritanceGraph;
import com.tcl.utils.JdtUtils;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectDatabase {

    public final Map<MethodSignature, MethodInfo> methodToInfo = new HashMap<>();
    /**
     * Used for `addMoreMethodInfoFromBindings`
     */
    public final Map<MethodSignature, IMethodBinding> methodToBinding = new HashMap<>();
    /**
     * Used for `buildInheritanceGraph`
     */
    public final Map<String, ITypeBinding> classToBinding = new HashMap<>();

    public InheritanceGraph inheritGraph;
    public final Set<CallEdge> originalCallEdges = new HashSet<>();
    public final Set<CallEdgeDyn> dynCallEdges = new HashSet<>();
    /**
     * callee to its edge dict, the edge dict is caller to call edge
     * <p>
     * Dict[callee, Dict[caller, edge]]
     */
    public final Map<MethodSignature, Map<MethodSignature, CallEdgeDyn>>
            dynCallGraph = new HashMap<>();

    /**
     * Call `build` first, then use other methods
     */
    public void build() {
        addMoreMethodInfoFromBindings();
        buildInheritanceGraph();
        buildOriginalCallEdges();
        buildDynCallEdges();
        buildDynCallGraph();
    }


    @Nonnull
    private List<List<MethodSignature>> bfs(@Nonnull MethodSignature source) {
        List<List<MethodSignature>> chains = new ArrayList<>();
        var vis = new HashSet<MethodSignature>();
        var fa = new HashMap<MethodSignature, MethodSignature>();
        Queue<MethodSignature> q = new LinkedList<>();
        vis.add(source);
        fa.put(source, null);
        q.add(source);
        while (!q.isEmpty()) {
            MethodSignature u = q.poll();
            if (dynCallGraph.get(u).size() == 0) { // u is a leaf
                var chainWithHead = new ArrayList<MethodSignature>();
                for (MethodSignature x = u; x != null; x = fa.get(x)) {
                    chainWithHead.add(x);
                }
                Collections.reverse(chainWithHead);
                chains.add(chainWithHead);
                continue;
            }
            for (var edge : dynCallGraph.get(u).values()) {
                assert u.equals(edge.callee);
                MethodSignature v = edge.caller;
                if (!vis.contains(v)) {
                    vis.add(v);
                    fa.put(v, u);
                    q.add(v);
                }
            }
        }
        return chains;
    }

    @Nonnull
    public List<CallChain> chainsFromSource(@Nonnull MethodSignature source) {
        List<List<MethodSignature>> chainsWithHead = bfs(source);
        MethodInfo info = methodToInfo.get(source);
        // exceptions declared in method signature or thrown in body
        var exceptions = new HashSet<String>();
        exceptions.addAll(Arrays.asList(info.signature.getThrowsDeclaration()));
        exceptions.addAll(info.throwsInBody);
        var result = new ArrayList<CallChain>();
        chainsWithHead.forEach(
                seq -> result.addAll(makeCallChains(seq, exceptions)));
        return result;
    }

    @Nonnull
    private List<CallChain> makeCallChains(@Nonnull List<MethodSignature> sequence,
                                           @Nonnull Iterable<String> exceptionsOfHead) {
        var chains = new ArrayList<CallChain>();
        CallChain template = listToCallChain(sequence);
        //Method sequences with different exceptions are different call chains
        for (String exception : exceptionsOfHead) {
            var chain = new CallChain(template);
            chain.setException(exception);
            //set handled for each entry
            for (int i = 0; i < chain.getChain().size(); i++) {
                ChainEntry entry = chain.getChain().get(i);
                MethodSignature callee = (i > 0)
                        ? chain.getChain().get(i - 1).getMethod() // last entry
                        : chain.getThrowFrom(); // head of chain
                MethodSignature caller = entry.getMethod();
                entry.setHandled(resolveHandled(callee, caller, exception));
            }
            chains.add(chain);
        }
        return chains;
    }

    @Nonnull
    public List<PredUnit> predUnitsFromSource(@Nonnull MethodSignature source) {
        var chains = chainsFromSource(source);
        //chains group by exception
        var chainsByEx = new HashMap<String, ArrayList<CallChain>>();
        for (var chain : chains) {
            chainsByEx.putIfAbsent(chain.getException(), new ArrayList<>());
            chainsByEx.get(chain.getException()).add(chain);
        }
        var predUnits = new ArrayList<PredUnit>();
        for (var chainsOfEx : chainsByEx.values()) {
            predUnits.addAll(chainsOfExToPredUnits(chainsOfEx));
        }
        return predUnits;
    }

    @Deprecated
    @Nonnull
    public List<CallChain> exactlyAllChainsFromSource(@Nonnull MethodSignature source) {
        List<List<MethodSignature>> chainsWithHead = new ArrayList<>();
        dfs(source, new ArrayList<>(), new HashSet<>(), chainsWithHead);
        MethodInfo info = methodToInfo.get(source);
        // exceptions declared in method signature or thrown in body
        var exceptions = new HashSet<String>();
        exceptions.addAll(Arrays.asList(info.signature.getThrowsDeclaration()));
        exceptions.addAll(info.throwsInBody);
        var result = new ArrayList<CallChain>();
        chainsWithHead.forEach(
                seq -> result.addAll(makeCallChains(seq, exceptions)));
        return result;
    }

    @Deprecated
    private void dfs(MethodSignature u,
                     List<MethodSignature> currentChain,
                     Set<MethodSignature> methodsInCurrent,
                     List<List<MethodSignature>> chainsWithHead) {
        assert dynCallGraph.containsKey(u);
        currentChain.add(u);
        methodsInCurrent.add(u);
        if (dynCallGraph.get(u).size() == 0) {
            //u is a leaf
            chainsWithHead.add(new ArrayList<>(currentChain));
        } else {
            for (var edge : dynCallGraph.get(u).values()) {
                assert u.equals(edge.callee);
                MethodSignature v = edge.caller;
                if (!methodsInCurrent.contains(v)) {
                    dfs(v, currentChain, methodsInCurrent, chainsWithHead);
                }
            }
        }
        assert currentChain.get(currentChain.size() - 1).equals(u);
        currentChain.remove(currentChain.size() - 1);
        methodsInCurrent.remove(u);
    }

    /**
     * 1. Contains throw stmts in body
     * 2. Is a method in standard library and has throws decl
     */
    public boolean isExceptionSource(@Nonnull MethodSignature method) {
        MethodInfo info = methodToInfo.get(method);
        assert info != null;
        if (!info.throwsInBody.isEmpty()) {
            return true;
        }
        boolean isJdk;
        if (method.getPackageName().isEmpty()) {
            isJdk = false;
        } else {
            String pkg = method.getPackageName().get();
            isJdk = pkg.startsWith("java") || pkg.startsWith("javax");
        }
        return isJdk && method.getThrowsDeclaration().length > 0;
    }

    public boolean isExceptionType(@Nonnull String clsName) {
        for (ITypeBinding x = classToBinding.get(clsName);
             x != null; x = x.getSuperclass()) {
            String name = JdtUtils.toClassName(x);
            if ("java.lang.Exception".equals(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean isThrowableType(@Nonnull String clsName) {
        for (ITypeBinding x = classToBinding.get(clsName);
             x != null; x = x.getSuperclass()) {
            String name = JdtUtils.toClassName(x);
            if ("java.lang.Throwable".equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Some methods were called, but their declarations are not in source file,
     * so we dont' have there `MethodSignature`.
     * <p>
     * But we have their `IMethodBinding`, so we can create their signature entities
     * from their bindings.
     */
    private void addMoreMethodInfoFromBindings() {
        for (var signature : methodToBinding.keySet()) {
            if (!methodToInfo.containsKey(signature)) {
                var info = new MethodInfo();
                info.signature = signature;
                methodToInfo.put(signature, info);
            }
        }
    }

    private void buildInheritanceGraph() {
        var infoList = classToBinding.values().stream()
                .map(JdtUtils::bindingToClassInfo).collect(Collectors.toList());
        inheritGraph = new InheritanceGraph(infoList);
    }

    private void buildOriginalCallEdges() {
        for (MethodSignature method : methodToInfo.keySet()) {
            MethodInfo info = methodToInfo.get(method);
            for (MethodSignature calling : info.callings) {
                originalCallEdges.add(new CallEdge(calling, method));
            }
        }
    }

    private void buildDynCallEdges() {
        for (CallEdge e : originalCallEdges) {
            MethodSignature originalCallee = e.callee;
            MethodSignature caller = e.caller;
            var candidates = inheritGraph.allOverriddenMethods(
                    originalCallee.getQualifiedClassName(), originalCallee);
            candidates.add(originalCallee);
            for (var maybeCallee : candidates) {
                dynCallEdges.add(new CallEdgeDyn(maybeCallee, originalCallee, caller));
            }
        }
    }

    private void buildDynCallGraph() {
        //Ensure each method(graph node) is in the graph
        for (var m : methodToInfo.keySet()) {
            //Some methods don't call others and don't called by others,
            //so they are not in the call edge set.
            dynCallGraph.putIfAbsent(m, new HashMap<>());
        }
        for (var e : dynCallEdges) {
            //Some method are overridden methods and are not in source file
            dynCallGraph.putIfAbsent(e.callee, new HashMap<>());
            dynCallGraph.putIfAbsent(e.caller, new HashMap<>());
            dynCallGraph.get(e.callee).put(e.caller, e);
        }
    }

    private boolean resolveHandled(@Nonnull MethodSignature callee,
                                   @Nonnull MethodSignature caller,
                                   @Nonnull String exception) {
        MethodSignature originalCallee = dynCallGraph.get(callee).get(caller).originalCallee;
        MethodInfo callerInfo = methodToInfo.get(caller);
        if (!callerInfo.callingToHandlers.containsKey(originalCallee)) {
            return false;
        }
        Set<String> handlers = callerInfo.callingToHandlers.get(originalCallee);
        assert handlers != null;
        return handlers.stream().anyMatch(
                handler -> canHandleException(exception, handler));
    }

    private boolean canHandleException(@Nonnull String throwName,
                                       @Nonnull String catchName) {
        //If the binding is not found, return can handle by default.
        if (!classToBinding.containsKey(throwName)
                || !classToBinding.containsKey(catchName)) {
            return true;
        }
        return inheritGraph.isCompatible(throwName, catchName);
    }

    @Nonnull
    private static CallChain listToCallChain(@Nonnull List<MethodSignature> chainWithHead) {
        assert chainWithHead.size() >= 1;
        var chain = new ArrayList<ChainEntry>();
        for (int i = 1; i < chainWithHead.size(); i++) {
            chain.add(new ChainEntry(chainWithHead.get(i), false));
        }
        return new CallChain(chainWithHead.get(0), chain);
    }

    private static List<PredUnit> chainToPredUnits(@Nonnull CallChain chain) {
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
        var methodPos = new LinkedHashMap<MethodSignature, MethodPos>();
        chain.getChain().forEach(
                entry -> methodPos.put(entry.getMethod(), new MethodPos()));
        //method-top, method-bottom
        for (String className : clsMethods.keySet()) {
            var methodSet = clsMethods.get(className);
            int i = 0;
            for (var method : methodSet) {
                methodPos.get(method).methodTop = i;
                methodPos.get(method).methodBottom = methodSet.size() - 1 - i;
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
            methodPos.get(method).classTop =
                    clsTopDict.get(method.getQualifiedClassName());
            methodPos.get(method).classBottom =
                    clsBottomDict.get(method.getQualifiedClassName());
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
            methodPos.get(method).packageTop =
                    pkgTopDict.get(method.getPackageName().orElse(null));
            methodPos.get(method).packageBottom =
                    pkgBottomDict.get(method.getPackageName().orElse(null));
        }
        //chain-top, chain-bottom
        int chainLen = chain.getChain().size();
        for (i = 0; i < chainLen; i++) {
            MethodSignature method = chain.getChain().get(i).getMethod();
            methodPos.get(method).chainTop = i;
            methodPos.get(method).chainBottom = chainLen - 1 - i;
        }
        //build predict units
        var predUnits = new ArrayList<PredUnit>();
        for (var entry : chain.getChain()) {
            var unit = new PredUnit();
            unit.throwFrom = chain.getThrowFrom().getSimpleSignature().toString();
            unit.exception = chain.getException();
            unit.simpleMethod = entry.getMethod().getSimpleSignature().toString();
            unit.handled = entry.isHandled();
            unit.position = methodPos.get(entry.getMethod());
            predUnits.add(unit);
        }
        return predUnits;
    }

    //all chains should have same throwFrom and some exception
    private static List<PredUnit> chainsOfExToPredUnits(
            @Nonnull Iterable<CallChain> chainsOfEx) {
        var methodPred = new HashMap<String, PredUnit>();
        for (var chain : chainsOfEx) {
            List<PredUnit> units = chainToPredUnits(chain);
            for (var unit : units) {
                String method = unit.simpleMethod;
                if (!methodPred.containsKey(method)
                        || methodPred.get(method).position.chainBottom > unit.position.chainBottom) {
                    methodPred.put(method, unit);
                }
            }
        }
        return new ArrayList<>(methodPred.values());
    }
}
