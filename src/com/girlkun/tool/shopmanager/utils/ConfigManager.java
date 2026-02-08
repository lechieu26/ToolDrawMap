package com.girlkun.tool.shopmanager.utils;

import com.girlkun.tool.shopmanager.models.DbConfig;

import java.io.*;
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
            // Return default config if file doesn't exist
            return new DbConfig("localhost", 3306, "root", "", "nrosamurai");
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
            return config;
        } catch (Exception e) {
            e.printStackTrace();
            return new DbConfig("localhost", 3306, "root", "", "nrosamurai");
        }
    }

    public static void save(DbConfig config) {
        File file = new File(CONFIG_FILE);

        try {
            // Read existing content
            java.util.List<String> lines = Files.readAllLines(Paths.get(CONFIG_FILE));
            StringBuilder newContent = new StringBuilder();

            boolean hostUpdated = false;
            boolean portUpdated = false;
            boolean nameUpdated = false;
            boolean userUpdated = false;
            boolean passUpdated = false;
            boolean urlUpdated = false;

            for (String line : lines) {
                String trimmed = line.trim();

                if (trimmed.startsWith("database.host=")) {
                    newContent.append("database.host=").append(config.host).append("\n");
                    hostUpdated = true;
                } else if (trimmed.startsWith("database.port=")) {
                    newContent.append("database.port=").append(config.port).append("\n");
                    portUpdated = true;
                } else if (trimmed.startsWith("database.name=")) {
                    newContent.append("database.name=").append(config.database).append("\n");
                    nameUpdated = true;
                } else if (trimmed.startsWith("database.user=")) {
                    newContent.append("database.user=").append(config.user).append("\n");
                    userUpdated = true;
                } else if (trimmed.startsWith("database.pass=")) {
                    newContent.append("database.pass=").append(config.password).append("\n");
                    passUpdated = true;
                } else if (trimmed.startsWith("database.url=")) {
                    // Update URL automatically based on host, port, database
                    String url = String.format(
                            "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=utf8",
                            config.host, config.port, config.database);
                    newContent.append("database.url=").append(url).append("\n");
                    urlUpdated = true;
                } else {
                    newContent.append(line).append("\n");
                }
            }

            // Write back to file
            try (FileOutputStream fos = new FileOutputStream(file);
                    OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8")) {
                osw.write(newContent.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
