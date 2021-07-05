package com.tcl.json;

import com.tcl.graph.call.CallChain;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class CallChainJson {
    private MethodJson throwFrom;
    private String exception;
    private List<ChainEntryJson> chain = new ArrayList<>();

    public CallChainJson(CallChain callChain) {
        throwFrom = new MethodJson(callChain.getThrowFrom());
        exception = callChain.getException();
        callChain.getChain().forEach(
                entry -> chain.add(new ChainEntryJson(entry)));
    }

    public MethodJson getThrowFrom() {
        return throwFrom;
    }

    public void setThrowFrom(MethodJson throwFrom) {
        this.throwFrom = throwFrom;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public List<ChainEntryJson> getChain() {
        return chain;
    }

    public void setChain(List<ChainEntryJson> chain) {
        this.chain = chain;
    }
}
