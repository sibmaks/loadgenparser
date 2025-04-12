package com.github.sibmaks;

public record RequestKey(String method, String uri, RequestKind requestKind) {

    public boolean isStatic() {
        return requestKind == RequestKind.STATIC;
    }

}