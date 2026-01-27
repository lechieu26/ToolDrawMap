package com.girlkun.tool.shopmanager.models;

public class DbConfig {
    public String host;
    public int port;
    public String user;
    public String password;
    public String database;

    public DbConfig() {
    }

    public DbConfig(String host, int port, String user, String password, String database) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
    }

    public String toConnectionString() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh";
    }
}
