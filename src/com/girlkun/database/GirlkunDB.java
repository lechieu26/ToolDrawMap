package com.girlkun.database;

import com.girlkun.Log;
import com.girlkun.Resource;
import com.girlkun.result.GirlkunResultSet;
import com.girlkun.result.ResultSetImpl;
import com.zaxxer.hikari.HikariConfig;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GirlkunDB {
   private static final Logger logger = LoggerFactory.getLogger(GirlkunDB.class);
   private static GirlkunDatasource[] datasource;
   public static boolean LOG_QUERY = false;

   private static void setupConfig(HikariConfig c, String driver, String url, String user, String pass, int min,
         int max, int lifeTime) {
      c.setDriverClassName(driver);
      c.setJdbcUrl(url);
      c.setUsername(user);
      c.setPassword(pass);
      c.setMinimumIdle(min);
      c.setMaximumPoolSize(max);
      c.setMaxLifetime((long) lifeTime);
      c.addDataSourceProperty("cachePrepStmts", "true");
      c.addDataSourceProperty("prepStmtCacheSize", "250");
      c.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
   }

   public static void loadDatasource() {
      Properties properties = new Properties();

      try {
         properties.load(new FileInputStream(new String(Resource.pathFileProperties)));
         Object value = null;

         int n = 1;
         if ((value = properties.get(new String(Resource.amount))) != null) {
            n = Integer.parseInt(String.valueOf(value));
         }

         datasource = new GirlkunDatasource[n];

         for (int i = 0; i < n; i++) {
            String nameDS = "GIRLKUN";
            String driver = new String(Resource.defaultDriver);
            String urlFormat = "jdbc:mysql://%s:%s/%s?useUnicode=yes&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&useSSL=false";
            String host = "localhost";
            String port = "3306";
            String name = "tomahoc_db";
            String user = "root";
            String pass = "";
            int minCon = 1;
            int maxCon = 5;
            int maxLifeTime = 1800000;

            String suffix = (n > 1) ? String.valueOf(i) : "";

            if ((value = properties.get(new String(Resource.dsName) + suffix)) != null)
               nameDS = String.valueOf(value);
            if ((value = properties.get(new String(Resource.driver) + suffix)) != null)
               driver = String.valueOf(value);
            else if ((value = properties.get(new String(Resource.driver))) != null)
               driver = String.valueOf(value);

            if ((value = properties.get(new String(Resource.host) + suffix)) != null)
               host = String.valueOf(value);
            else if ((value = properties.get(new String(Resource.host))) != null)
               host = String.valueOf(value);

            if ((value = properties.get(new String(Resource.port) + suffix)) != null)
               port = String.valueOf(value);
            else if ((value = properties.get(new String(Resource.port))) != null)
               port = String.valueOf(value);

            if ((value = properties.get(new String(Resource.name) + suffix)) != null)
               name = String.valueOf(value);
            else if ((value = properties.get(new String(Resource.name))) != null)
               name = String.valueOf(value);

            if ((value = properties.get(new String(Resource.user) + suffix)) != null)
               user = String.valueOf(value);
            else if ((value = properties.get(new String(Resource.user))) != null)
               user = String.valueOf(value);

            if ((value = properties.get(new String(Resource.pass) + suffix)) != null)
               pass = String.valueOf(value);
            else if ((value = properties.get(new String(Resource.pass))) != null)
               pass = String.valueOf(value);

            if ((value = properties.get("database.url")) != null) {
               urlFormat = String.valueOf(value);
            }

            String finalUrl = urlFormat.contains("%s") ? String.format(urlFormat, host, port, name) : urlFormat;

            // Thử mật khẩu trước khi khởi tạo Hikari (Tự động thích nghi với XAMPP mật khẩu
            // trống hoặc 123456)
            if (user.equals("root")) {
               try {
                  Class.forName(driver);
                  try (Connection conn = java.sql.DriverManager.getConnection(finalUrl, user, pass)) {
                     // OK
                  } catch (java.sql.SQLException e) {
                     if (e.getMessage().contains("Access denied") || e.getErrorCode() == 1045) {
                        boolean found = false;
                        // Thử mật khẩu TRỐNG
                        if (!pass.isEmpty()) {
                           try (Connection conn2 = java.sql.DriverManager.getConnection(finalUrl, user, "")) {
                              pass = "";
                              logger.warn("Mật khẩu root sai, đã chuyển sang mật khẩu TRỐNG.");
                              found = true;
                           } catch (java.sql.SQLException e2) {
                           }
                        }
                        // Thử mật khẩu 123456
                        if (!found && !pass.equals("123456")) {
                           try (Connection conn2 = java.sql.DriverManager.getConnection(finalUrl, user, "123456")) {
                              pass = "123456";
                              logger.warn("Mật khẩu root sai, đã chuyển sang mật khẩu 123456.");
                              found = true;
                           } catch (java.sql.SQLException e3) {
                           }
                        }
                     }
                  }
               } catch (Exception e) {
               }
            }

            HikariConfig config = new HikariConfig();
            setupConfig(config, driver, finalUrl, user, pass, minCon, maxCon, maxLifeTime);

            try {
               datasource[i] = new GirlkunDatasource(nameDS, config);
               logger.info("Load thành công datasource: " + nameDS);
            } catch (Exception e) {
               logger.error("KHÔNG THỂ KẾT NỐI DATABASE " + nameDS + ": " + e.getMessage());
            }
         }

         if ((value = properties.get("girlkun.database.log")) != null) {
            LOG_QUERY = Boolean.parseBoolean(String.valueOf(value));
         }
      } catch (Exception var19) {
         var19.printStackTrace();
         logger.error(var19.getMessage());
      } finally {
         properties.clear();
      }
   }

   /**
    * Reload database configuration
    */
   public static void reload() {
      try {
         if (datasource != null) {
            close();
         }
         loadDatasource();
         logger.info("Database configuration reloaded successfully.");
      } catch (Exception e) {
         logger.error("Error reloading database configuration: " + e.getMessage());
      }
   }

   public static void close() {
      if (datasource == null)
         return;
      for (GirlkunDatasource ds : datasource) {
         if (ds != null) {
            ds.close();
         }
      }
   }

   public static Connection getConnection(String dsName) throws Exception {
      for (GirlkunDatasource ds : datasource) {
         if (ds != null && ds.name.equals(dsName)) {
            return ds.getConnection();
         }
      }

      throw new Exception("Datasouce " + dsName + " không tồn tại");
   }

   public static GirlkunResultSet executeQuery(String dsName, String query) throws Exception {
      try (
            Connection con = getConnection(dsName);
            // Thêm TYPE_SCROLL_INSENSITIVE để ResultSet có thể scroll (last, beforeFirst)
            PreparedStatement ps = con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
                  ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = ps.executeQuery();) {
         if (LOG_QUERY) {
            logger.info("Thực thi thành công câu lệnh: " + ps.toString());
            Log.gI().log(ps.toString());
         }

         return new ResultSetImpl(rs);
      } catch (Exception var61) {
         logger.error("Có lỗi xảy ra khi thực thi câu lệnh: " + query);
         throw var61;
      }
   }

   public static GirlkunResultSet executeQuery(String dsName, String query, Object... objs) throws Exception {
      try (
            Connection con = getConnection(dsName);
            // Thêm TYPE_SCROLL_INSENSITIVE để ResultSet có thể scroll (last, beforeFirst)
            PreparedStatement ps = con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
                  ResultSet.CONCUR_READ_ONLY);) {
         for (int i = 0; i < objs.length; i++) {
            ps.setObject(i + 1, objs[i]);
         }

         if (LOG_QUERY) {
            logger.info("Thực thi thành công câu lệnh: " + ps.toString());
            Log.gI().log(ps.toString());
         }

         return new ResultSetImpl(ps.executeQuery());
      } catch (Exception var36) {
         logger.error("Có lỗi xảy ra khi thực thi câu lệnh: " + query);
         throw var36;
      }
   }

   public static int executeUpdate(String dsName, String query) throws Exception {
      int rowUpdated = -1;

      try (
            Connection con = getConnection(dsName);
            PreparedStatement ps = con.prepareStatement(query);) {
         if (LOG_QUERY) {
            logger.info("Thực thi thành công câu lệnh: " + ps.toString());
            Log.gI().log(ps.toString());
         }

         return ps.executeUpdate();
      } catch (Exception var35) {
         logger.error("Có lỗi xảy ra khi thực thi câu lệnh: " + query);
         throw var35;
      }
   }

   public static int executeUpdate(String dsName, String query, Object... objs) throws Exception {
      if (query.indexOf("insert") == 0 && query.lastIndexOf("()") == query.length() - 2) {
         StringBuilder sb = new StringBuilder();
         sb.append("(");

         for (int i = 0; i < objs.length; i++) {
            sb.append("?");
            if (i < objs.length - 1) {
               sb.append(",");
            } else {
               sb.append(")");
            }
         }

         query = query.replace("()", sb.toString());
      }

      try (
            Connection con = getConnection(dsName);
            PreparedStatement ps = con.prepareStatement(query);) {
         for (int ix = 0; ix < objs.length; ix++) {
            ps.setObject(ix + 1, objs[ix]);
         }

         if (LOG_QUERY) {
            logger.info("Thực thi thành công câu lệnh: " + ps.toString());
            Log.gI().log(ps.toString());
         }

         return ps.executeUpdate();
      } catch (Exception var36) {
         logger.error("Có lỗi xảy ra khi thực thi câu lệnh: " + query);
         throw var36;
      }
   }

   static {
      loadDatasource();
   }
}
