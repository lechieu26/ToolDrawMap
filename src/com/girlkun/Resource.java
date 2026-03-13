package com.girlkun;

/**
 * Resource class - Override để hỗ trợ MySQL 8.x
 * Thay đổi URL format để thêm serverTimezone và các tham số cần thiết
 */
public class Resource {
    // Path to properties file
    public static final byte[] pathFileProperties = "data/data/config/config.properties".getBytes();

    // JDBC URL format cho MySQL 8.x
    public static final byte[] formatURL = "%s".getBytes(); // Sẽ dùng trực tiếp URL từ file config

    // Default driver cho MySQL 8.x
    public static final byte[] defaultDriver = "com.mysql.cj.jdbc.Driver".getBytes();

    // Property keys - Map lại theo file config.properties
    public static final byte[] amount = "database.amount".getBytes(); // Mặc định là 1 nếu không có
    public static final byte[] dsName = "database.ds.name.".getBytes();
    public static final byte[] driver = "database.driver".getBytes();
    public static final byte[] host = "database.host".getBytes();
    public static final byte[] port = "database.port".getBytes();
    public static final byte[] name = "database.name".getBytes();
    public static final byte[] user = "database.user".getBytes();
    public static final byte[] pass = "database.pass".getBytes();
    public static final byte[] url = "database.url".getBytes();
    public static final byte[] min = "database.min".getBytes();
    public static final byte[] max = "database.max".getBytes();
    public static final byte[] lifeTime = "database.lifetime".getBytes();

    private Resource() {
    }
}
