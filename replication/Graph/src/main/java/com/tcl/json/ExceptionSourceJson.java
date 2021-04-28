package com.tcl.json;

@Deprecated
public class ExceptionSourceJson {
    private final String throwFrom;
    private final String exception;

    public ExceptionSourceJson(String throwFrom, String exception) {
        this.throwFrom = throwFrom;
        this.exception = exception;
    }

    public String getThrowFrom() {
        return throwFrom;
    }

    public String getException() {
        return exception;
    }
}
