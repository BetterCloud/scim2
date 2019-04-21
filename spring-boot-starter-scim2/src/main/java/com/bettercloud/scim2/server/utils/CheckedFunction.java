package com.bettercloud.scim2.server.utils;

@FunctionalInterface
public interface CheckedFunction<T, U, R> {
    R apply(T t, U u) throws Exception;
}