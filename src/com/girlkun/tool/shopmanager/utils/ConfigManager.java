package com.girlkun.tool.shopmanager.utils;

import com.girlkun.tool.shopmanager.models.DbConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * ConfigManager - Đọc/ghi cấu hình Database từ file config.properties
 */
public class ConfigManager {
    private static final String CONFIG_FILE = "data/data/config/config.properties";

    public static DbConfig load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            return new DbConfig("localhost", 3306, "root", "", "nrosamurai", DbConfig.DB_TOMAHAWK);
        }

        try (FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8")) {
            Properties props = new Properties();
            props.load(isr);

            DbConfig config = new DbConfig();
            config.host = props.getProperty("database.host", "localhost");
            config.port = Integer.parseInt(props.getProperty("database.port", "3306"));
            config.user = props.getProperty("database.user", "root");
            config.password = props.getProperty("database.pass", "");
            config.database = props.getProperty("database.name", "nrosamurai");
            config.dbType = Integer.parseInt(props.getProperty("database.type", String.valueOf(DbConfig.DB_TOMAHAWK)));
            return config;
        } catch (Exception e) {
            e.printStackTrace();
            return new DbConfig("localhost", 3306, "root", "", "nrosamurai", DbConfig.DB_TOMAHAWK);
        }
    }

    public static void save(DbConfig config) {
        saveToConfigProperties(config);
    }

    private static void saveToConfigProperties(DbConfig config) {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) return;

        try {
            java.util.List<String> lines = Files.readAllLines(Paths.get(CONFIG_FILE));
            StringBuilder newContent = new StringBuilder();

            boolean hasType = false;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("database.host=")) {
                    newContent.append("database.host=").append(config.host).append("\n");
                } else if (trimmed.startsWith("database.port=")) {
                    newContent.append("database.port=").append(config.port).append("\n");
                } else if (trimmed.startsWith("database.name=")) {
                    newContent.append("database.name=").append(config.database).append("\n");
                } else if (trimmed.startsWith("database.user=")) {
                    newContent.append("database.user=").append(config.user).append("\n");
                } else if (trimmed.startsWith("database.pass=")) {
                    newContent.append("database.pass=").append(config.password).append("\n");
                } else if (trimmed.startsWith("database.type=")) {
                    newContent.append("database.type=").append(config.dbType).append("\n");
                    hasType = true;
                } else if (trimmed.startsWith("database.url=")) {
                    String url = String.format(
                            "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=utf8&allowPublicKeyRetrieval=true",
                            config.host, config.port, config.database);
                    newContent.append("database.url=").append(url).append("\n");
                } else {
                    newContent.append(line).append("\n");
                }
            }
            if (!hasType) {
                newContent.append("database.type=").append(config.dbType).append("\n");
            }

            try (FileOutputStream fos = new FileOutputStream(file);
                    OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8")) {
                osw.write(newContent.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
