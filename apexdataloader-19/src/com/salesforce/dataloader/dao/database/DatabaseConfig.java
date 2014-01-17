package com.salesforce.dataloader.dao.database;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;

public class DatabaseConfig {
    private BasicDataSource dataSource;
    private SqlConfig sqlConfig;

    public DatabaseConfig() {
    }

    /**
     * Factory method
     *
     * @param dbConfigFilename
     * @param dbConnectionName
     * @return instance of database configuration
     */
    public static DatabaseConfig getInstance (String dbConfigFilename, String dbConnectionName) {
        XmlBeanFactory configFactory = new XmlBeanFactory(new FileSystemResource(dbConfigFilename));
        return (DatabaseConfig)configFactory.getBean(dbConnectionName);
    }

    public BasicDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public SqlConfig getSqlConfig() {
        return sqlConfig;
    }

    public void setSqlConfig(SqlConfig sqlConfig) {
        this.sqlConfig = sqlConfig;
    }

}
