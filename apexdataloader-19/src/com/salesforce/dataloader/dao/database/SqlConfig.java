package com.salesforce.dataloader.dao.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SqlConfig {
    ArrayList<String> columnNames;
    HashMap<String,String> sqlParams;
    String sqlString;
    List<String> generatedKeysColumnNames = new ArrayList<String>();

    public SqlConfig() {
    }

    public void setColumnNames (ArrayList<String> columnNames) {
        this.columnNames = columnNames;
    }

    public ArrayList<String> getColumnNames() {
        return columnNames;
    }

    public void setSqlString(String sqlString) {
        this.sqlString = sqlString;
    }

    public String getSqlString() {
        return sqlString;
    }

    public HashMap<String,String> getSqlParams() {
        return sqlParams;
    }

    public void setSqlParams(HashMap<String,String> queryParams) {
        this.sqlParams = queryParams;
    }

    public List<String> getGeneratedKeysColumnNames() {
        return generatedKeysColumnNames;
    }

    public void setGeneratedKeysColumnNames(List<String> generatedKeysColumnNames) {
        this.generatedKeysColumnNames = generatedKeysColumnNames;
    }

    public boolean hasGeneratedKeys() {
            return !getGeneratedKeysColumnNames().isEmpty();
    }
}
