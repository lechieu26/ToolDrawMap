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

import com.girlkun.tool.utils.PathConfig;

/**
 * ConfigManager - Đọc/ghi cấu hình Database từ file config.properties (đường dẫn động)
 */
public class ConfigManager {

    public static String getConfigFile() {
        return PathConfig.getConfigPath();
    }

    public static DbConfig load() {
        return load(PathConfig.getConfigPath());
    }

    public static DbConfig load(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            filePath = PathConfig.getConfigPath();
        }
        File file = new File(filePath);
        DbConfig config = new DbConfig("localhost", 3306, "root", "", "nrosamurai", DbConfig.DB_TOMAHAWK);
        config.dataPath = PathConfig.getDataPath();
        config.configPath = filePath;

        if (!file.exists()) {
            return config;
        }

        try (FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8")) {
            Properties props = new Properties();
            props.load(isr);

            config.host = props.getProperty("database.host", "localhost");
            config.port = Integer.parseInt(props.getProperty("database.port", "3306"));
            config.user = props.getProperty("database.user", "root");
            config.password = props.getProperty("database.pass", "");
            config.database = props.getProperty("database.name", "nrosamurai");
            config.dbType = Integer.parseInt(props.getProperty("database.type", String.valueOf(DbConfig.DB_TOMAHAWK)));
            
            // Nếu trong file config có lưu data.path thì ưu tiên
            String propDataPath = props.getProperty("data.path");
            if (propDataPath != null && !propDataPath.trim().isEmpty()) {
                config.dataPath = PathConfig.normalizePath(propDataPath.trim());
            }
            return config;
        } catch (Exception e) {
            e.printStackTrace();
            return config;
        }
    }

    public static void save(DbConfig config) {
        if (config.dataPath != null && !config.dataPath.trim().isEmpty() &&
            config.configPath != null && !config.configPath.trim().isEmpty()) {
            PathConfig.saveToolConfig(config.dataPath, config.configPath);
        }
        saveToConfigProperties(config);
    }

    private static void saveToConfigProperties(DbConfig config) {
        String targetPath = config.configPath != null && !config.configPath.trim().isEmpty()
                ? config.configPath
                : PathConfig.getConfigPath();
        File file = new File(targetPath);

        try {
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                file.createNewFile();
            }

            java.util.List<String> lines = file.length() > 0 
                    ? Files.readAllLines(Paths.get(targetPath)) 
                    : new java.util.ArrayList<>();
            StringBuilder newContent = new StringBuilder();

            boolean hasHost = false;
            boolean hasPort = false;
            boolean hasName = false;
            boolean hasUser = false;
            boolean hasPass = false;
            boolean hasType = false;
            boolean hasUrl = false;
            boolean hasDataPath = false;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("database.host=")) {
                    newContent.append("database.host=").append(config.host).append("\n");
                    hasHost = true;
                } else if (trimmed.startsWith("database.port=")) {
                    newContent.append("database.port=").append(config.port).append("\n");
                    hasPort = true;
                } else if (trimmed.startsWith("database.name=")) {
                    newContent.append("database.name=").append(config.database).append("\n");
                    hasName = true;
                } else if (trimmed.startsWith("database.user=")) {
                    newContent.append("database.user=").append(config.user).append("\n");
                    hasUser = true;
                } else if (trimmed.startsWith("database.pass=")) {
                    newContent.append("database.pass=").append(config.password).append("\n");
                    hasPass = true;
                } else if (trimmed.startsWith("database.type=")) {
                    newContent.append("database.type=").append(config.dbType).append("\n");
                    hasType = true;
                } else if (trimmed.startsWith("database.url=")) {
                    String url = String.format(
                            "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=utf8&allowPublicKeyRetrieval=true",
                            config.host, config.port, config.database);
                    newContent.append("database.url=").append(url).append("\n");
                    hasUrl = true;
                } else if (trimmed.startsWith("data.path=")) {
                    newContent.append("data.path=").append(config.dataPath != null ? config.dataPath : PathConfig.getDataPath()).append("\n");
                    hasDataPath = true;
                } else {
                    newContent.append(line).append("\n");
                }
            }

            if (!hasHost) newContent.append("database.host=").append(config.host).append("\n");
            if (!hasPort) newContent.append("database.port=").append(config.port).append("\n");
            if (!hasName) newContent.append("database.name=").append(config.database).append("\n");
            if (!hasUser) newContent.append("database.user=").append(config.user).append("\n");
            if (!hasPass) newContent.append("database.pass=").append(config.password).append("\n");
            if (!hasType) newContent.append("database.type=").append(config.dbType).append("\n");
            if (!hasUrl) {
                String url = String.format(
                        "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=utf8&allowPublicKeyRetrieval=true",
                        config.host, config.port, config.database);
                newContent.append("database.url=").append(url).append("\n");
            }
            if (!hasDataPath && config.dataPath != null) {
                newContent.append("data.path=").append(config.dataPath).append("\n");
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
