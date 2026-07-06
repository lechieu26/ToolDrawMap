package com.girlkun.tool.shopmanager.models;

public class DbConfig {
    public static final int DB_TOMAHAWK = 0;
    public static final int DB_NRO_ARN = 1;

    public String host;
    public int port;
    public String user;
    public String password;
    public String database;
    public int dbType = DB_TOMAHAWK;

    public DbConfig() {
    }

    public DbConfig(String host, int port, String user, String password, String database, int dbType) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
        this.dbType = dbType;
    }

    public String toConnectionString() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh";
    }
}
