package org.example.javawebdemo.mapper.provider;

public class ResidentSqlProvider {

    private boolean isNotBlank(Object value) {
        return value instanceof String str && !str.isBlank();
    }
}
