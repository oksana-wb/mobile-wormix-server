package com.pragmatix.steam.dao;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Vladimir
 * Date: 15.12.11 14:48
 */
public strictfp class QueryBuilder {
    private final StringBuilder sql = new StringBuilder();
    private final List<String> paramNames = new ArrayList<String>();
    private final List<Object> paramValues = new ArrayList<Object>();

    public String getSql() {
        return sql.toString();
    }

    public List<String> getParamNames() {
        return paramNames;
    }

    public List<Object> getParamValues() {
        return paramValues;
    }

    public QueryBuilder appendParameter(String name, Object value) {
        sql.append(":").append(name);
        paramNames.add(name);
        paramValues.add(value);

        return this;
    }

    public QueryBuilder append(String text) {
        sql.append(text);

        return this;
    }
}
