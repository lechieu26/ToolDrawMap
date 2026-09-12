package com.girlkun.tool.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Quản lý cấu hình đường dẫn dữ liệu (Server Data) và file config cho Tool.
 * Đảm bảo đường dẫn luôn là tương đối khi có thể.
 */
public class PathConfig {
    private static final String TOOL_CONFIG_FILE = "tool_config.properties";
    private static final String DEFAULT_DATA_PATH = "data/data";
    private static final String DEFAULT_CONFIG_PATH = "data/data/config/config.properties";

    private static String dataPath = DEFAULT_DATA_PATH;
    private static String configPath = DEFAULT_CONFIG_PATH;
    private static boolean initialized = false;

    static {
        init();
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        File configFile = new File(TOOL_CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile);
                 InputStreamReader isr = new InputStreamReader(fis, "UTF-8")) {
                Properties props = new Properties();
                props.load(isr);
                String loadedData = props.getProperty("data.path");
                String loadedConfig = props.getProperty("config.path");

                if (loadedData != null && !loadedData.trim().isEmpty()) {
                    dataPath = normalizePath(loadedData.trim());
                }
                if (loadedConfig != null && !loadedConfig.trim().isEmpty()) {
                    configPath = normalizePath(loadedConfig.trim());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Kiểm tra nếu data/data không tồn tại nhưng có server data khác thì giữ mặc định
            saveToolConfig(dataPath, configPath);
        }
    }

    /**
     * Chuyển đường dẫn bất kỳ thành đường dẫn tương đối so với thư mục chạy Tool.
     */
    public static String toRelativePath(String absolutePath) {
        if (absolutePath == null || absolutePath.trim().isEmpty()) {
            return "";
        }
        try {
            Path base = Paths.get(".").toAbsolutePath().normalize();
            Path target = Paths.get(absolutePath).toAbsolutePath().normalize();

            if (base.getRoot() != null && base.getRoot().equals(target.getRoot())) {
                Path relative = base.relativize(target);
                String relStr = relative.toString().replace('\\', '/');
                return relStr.isEmpty() ? "." : relStr;
            }
        } catch (Exception e) {
            // Ignored
        }
        return normalizePath(absolutePath);
    }

    public static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.replace('\\', '/');
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public static synchronized String getDataPath() {
        if (!initialized) {
            init();
        }
        return dataPath;
    }

    public static synchronized void setDataPath(String path) {
        dataPath = normalizePath(toRelativePath(path));
    }

    public static synchronized String getConfigPath() {
        if (!initialized) {
            init();
        }
        return configPath;
    }

    public static synchronized void setConfigPath(String path) {
        configPath = normalizePath(toRelativePath(path));
    }

    public static synchronized void saveToolConfig(String newDataPath, String newConfigPath) {
        setDataPath(newDataPath);
        setConfigPath(newConfigPath);

        Properties props = new Properties();
        props.setProperty("data.path", dataPath);
        props.setProperty("config.path", configPath);

        try (FileOutputStream fos = new FileOutputStream(TOOL_CONFIG_FILE);
             OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8")) {
            props.store(osw, "ToolDrawMap Configuration");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
