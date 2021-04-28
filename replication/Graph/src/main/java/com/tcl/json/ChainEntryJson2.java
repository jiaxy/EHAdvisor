package com.tcl.json;

@Deprecated
public class ChainEntryJson2 {
    private String simpleMethod;
    private boolean handled;
    private int methodTop;
    private int methodBottom;
    private int classTop;
    private int classBottom;
    private int packageTop;
    private int packageBottom;
    /**
     * start from 1
     */
    private int posInChain;

    public String getSimpleMethod() {
        return simpleMethod;
    }

    public void setSimpleMethod(String simpleMethod) {
        this.simpleMethod = simpleMethod;
    }

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

    public int getMethodTop() {
        return methodTop;
    }

    public void setMethodTop(int methodTop) {
        this.methodTop = methodTop;
    }

    public int getMethodBottom() {
        return methodBottom;
    }

    public void setMethodBottom(int methodBottom) {
        this.methodBottom = methodBottom;
    }

    public int getClassTop() {
        return classTop;
    }

    public void setClassTop(int classTop) {
        this.classTop = classTop;
    }

    public int getClassBottom() {
        return classBottom;
    }

    public void setClassBottom(int classBottom) {
        this.classBottom = classBottom;
    }

    public int getPackageTop() {
        return packageTop;
    }

    public void setPackageTop(int packageTop) {
        this.packageTop = packageTop;
    }

    public int getPackageBottom() {
        return packageBottom;
    }

    public void setPackageBottom(int packageBottom) {
        this.packageBottom = packageBottom;
    }

    public int getPosInChain() {
        return posInChain;
    }

    public void setPosInChain(int posInChain) {
        this.posInChain = posInChain;
    }
}
