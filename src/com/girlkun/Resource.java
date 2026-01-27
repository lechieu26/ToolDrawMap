package com.girlkun;

/**
 * Resource class - Override để hỗ trợ MySQL 8.x
 * Thay đổi URL format để thêm serverTimezone và các tham số cần thiết
 */
public class Resource {
    // Path to properties file
    public static final byte[] pathFileProperties = "data/config/girlkundb.properties".getBytes();

    // JDBC URL format cho MySQL 8.x
    // Thêm serverTimezone, allowPublicKeyRetrieval, useSSL
    public static final byte[] formatURL = "jdbc:mysql://%s:%s/%s?useUnicode=yes&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&useSSL=false"
            .getBytes();

    // Default driver cho MySQL 8.x
    public static final byte[] defaultDriver = "com.mysql.cj.jdbc.Driver".getBytes();

    // Property keys
    public static final byte[] amount = "girlkun.database.amount".getBytes();
    public static final byte[] dsName = "girlkun.database.ds.name.".getBytes();
    public static final byte[] driver = "girlkun.database.driver.".getBytes();
    public static final byte[] host = "girlkun.database.host.".getBytes();
    public static final byte[] port = "girlkun.database.port.".getBytes();
    public static final byte[] name = "girlkun.database.name.".getBytes();
    public static final byte[] user = "girlkun.database.user.".getBytes();
    public static final byte[] pass = "girlkun.database.pass.".getBytes();
    public static final byte[] min = "girlkun.database.min.".getBytes();
    public static final byte[] max = "girlkun.database.max.".getBytes();
    public static final byte[] lifeTime = "girlkun.database.lifetime.".getBytes();

    private Resource() {
    }
}
