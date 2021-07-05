package com.tcl.entity;

import javax.annotation.Nullable;
import java.util.*;

public class MethodInfo {
    public MethodSignature signature;
    final public List<String> paramNames = new ArrayList<>();
    @Nullable
    public String javaDoc = null;
    final public Set<MethodSignature> callings = new HashSet<>();
    final public Set<String> throwsInBody = new HashSet<>();
    final public Map<MethodSignature, Set<String>> callingToHandlers = new HashMap<>();

    public void putCallingHandler(MethodSignature calling, String exception) {
        callingToHandlers.putIfAbsent(calling, new HashSet<>());
        callingToHandlers.get(calling).add(exception);
    }

    public void putCallingHandlers(MethodSignature calling, Iterable<String> exceptions) {
        callingToHandlers.putIfAbsent(calling, new HashSet<>());
        exceptions.forEach(callingToHandlers.get(calling)::add);
    }

    @Override
    public String toString() {
        return "MethodInfo{" +
                "signature=" + signature +
                ", paramNames=" + paramNames +
                ", callings=" + callings +
                ", throwsInBody=" + throwsInBody +
                ", callingToHandlers=" + callingToHandlers +
                '}';
    }
}
