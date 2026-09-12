package com.girlkun.tool.shopmanager.ui;

import com.girlkun.tool.shopmanager.models.DbConfig;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO;
import com.girlkun.tool.shopmanager.utils.ConfigManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.*;

/**
 * Cửa sổ cấu hình Database - JInternalFrame hiển thị trong desktop
 */
public class DbConfigScr extends JInternalFrame {

    private static final int CONNECTION_TIMEOUT_SECONDS = 3; // Timeout 3 giây

    // Icon paths (relative path)
    private static final String ICON_SUCCESS = "data/success.png";
    private static final String ICON_FAILED = "data/failed.png";

    private JTextField txtDataPath, txtConfigPath;
    private JButton btnBrowseData, btnBrowseConfig;
    private JTextField txtDbHost, txtDbPort, txtDbUser, txtDbPass, txtDbName;
    private JComboBox<String> cbDbType;
    private JTextArea txtDbStatus; // Dùng JTextArea để hiển thị toàn bộ lỗi
    private JLabel lblStatusIcon; // Icon hiển thị trạng thái
    private JButton btnSave, btnTest;
    private final ShopManagerDAO dao;

    public DbConfigScr() {
        super("Cấu hình Database & Data Path", true, true, true, true);
        this.setSize(680, 680);
        this.setFrameIcon(new ImageIcon("icon.png"));

        this.dao = ShopManagerDAO.gI();
        initComponents();
        loadConfig();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Title
        JLabel lblTitle = new JLabel("Cấu hình kết nối Database & Đường dẫn Data", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(60, 141, 188));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Path Data row
        txtDataPath = new JTextField(20);
        btnBrowseData = new JButton("Chọn folder...");
        styleSmallButton(btnBrowseData, new Color(41, 128, 185));
        btnBrowseData.addActionListener(e -> chooseDataFolder());
        JPanel pnlDataPath = new JPanel(new BorderLayout(5, 0));
        pnlDataPath.add(txtDataPath, BorderLayout.CENTER);
        pnlDataPath.add(btnBrowseData, BorderLayout.EAST);

        // Path Config row
        txtConfigPath = new JTextField(20);
        btnBrowseConfig = new JButton("Chọn file...");
        styleSmallButton(btnBrowseConfig, new Color(41, 128, 185));
        btnBrowseConfig.addActionListener(e -> chooseConfigFile());
        JPanel pnlConfigPath = new JPanel(new BorderLayout(5, 0));
        pnlConfigPath.add(txtConfigPath, BorderLayout.CENTER);
        pnlConfigPath.add(btnBrowseConfig, BorderLayout.EAST);

        txtDbHost = new JTextField(20);
        txtDbPort = new JTextField(20);
        txtDbUser = new JTextField(20);
        txtDbPass = new JPasswordField(20);
        txtDbName = new JTextField(20);
        
        cbDbType = new JComboBox<>(new String[]{"Tomahawk (Cũ)", "NRO ARN (Mới)"});

        addFormRow(formPanel, gbc, 0, "Path Data:", pnlDataPath);
        addFormRow(formPanel, gbc, 1, "Path Config:", pnlConfigPath);
        addFormRow(formPanel, gbc, 2, "Host:", txtDbHost);
        addFormRow(formPanel, gbc, 3, "Port:", txtDbPort);
        addFormRow(formPanel, gbc, 4, "User:", txtDbUser);
        addFormRow(formPanel, gbc, 5, "Password:", txtDbPass);
        addFormRow(formPanel, gbc, 6, "Database:", txtDbName);
        addFormRow(formPanel, gbc, 7, "DB Type:", cbDbType);

        // Status area - icon và text cùng 1 dòng
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Panel chứa icon và text status - FlowLayout để nằm cùng dòng
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Trạng thái"));

        // Icon label
        lblStatusIcon = new JLabel();
        lblStatusIcon.setPreferredSize(new Dimension(24, 24));
        statusPanel.add(lblStatusIcon);

        // Text status - dùng JLabel thay vì JTextArea
        txtDbStatus = new JTextArea(2, 42);
        txtDbStatus.setEditable(false);
        txtDbStatus.setLineWrap(true);
        txtDbStatus.setWrapStyleWord(true);
        txtDbStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtDbStatus.setBackground(statusPanel.getBackground());
        txtDbStatus.setBorder(null);
        statusPanel.add(txtDbStatus);

        formPanel.add(statusPanel, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        btnSave = new JButton("Lưu cấu hình");
        btnTest = new JButton("Retry connect DB");

        styleButton(btnSave, new Color(60, 141, 188));
        styleButton(btnTest, new Color(0, 166, 90));

        btnSave.addActionListener(e -> saveConfig());
        btnTest.addActionListener(e -> saveConfig());

        btnPanel.add(btnSave);
        btnPanel.add(btnTest);

        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void styleSmallButton(JButton btn, Color bgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void addFormRow(JPanel p, GridBagConstraints gbc, int row, String label, Component cmp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        p.add(cmp, gbc);
    }

    private void styleButton(JButton btn, Color bgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(120, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Set icon và text cho status
     * 
     * @param success true = success icon, false = failed icon, null = no icon
     */
    private void setStatus(Boolean success, String message, Color textColor) {
        txtDbStatus.setText(message);
        txtDbStatus.setForeground(textColor);

        if (success == null) {
            lblStatusIcon.setIcon(null);
        } else if (success) {
            try {
                ImageIcon icon = new ImageIcon(ICON_SUCCESS);
                Image img = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                lblStatusIcon.setIcon(new ImageIcon(img));
            } catch (Exception e) {
                lblStatusIcon.setIcon(null);
            }
        } else {
            try {
                ImageIcon icon = new ImageIcon(ICON_FAILED);
                Image img = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                lblStatusIcon.setIcon(new ImageIcon(img));
            } catch (Exception e) {
                lblStatusIcon.setIcon(null);
            }
        }
    }

    private void chooseDataFolder() {
        String cur = txtDataPath.getText().trim();
        String selectedDir = chooseNativeFolder("Chọn thư mục Data Server", cur);
        if (selectedDir != null && !selectedDir.trim().isEmpty()) {
            java.io.File selected = new java.io.File(selectedDir);
            String relPath = com.girlkun.tool.utils.PathConfig.toRelativePath(selected.getAbsolutePath());
            txtDataPath.setText(relPath);

            // Kiểm tra tự động file config trong folder được chọn
            java.io.File cfgFile = new java.io.File(selected, "config/config.properties");
            if (!cfgFile.exists()) {
                cfgFile = new java.io.File(selected, "config.properties");
            }
            if (cfgFile.exists()) {
                String relCfg = com.girlkun.tool.utils.PathConfig.toRelativePath(cfgFile.getAbsolutePath());
                txtConfigPath.setText(relCfg);
                loadFromConfigFile(relCfg);
            }
        }
    }

    private void chooseConfigFile() {
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        FileDialog dialog = new FileDialog(frame, "Chọn file config.properties", FileDialog.LOAD);

        String cur = txtConfigPath.getText().trim();
        if (!cur.isEmpty()) {
            java.io.File f = new java.io.File(cur);
            if (f.exists()) {
                dialog.setDirectory(f.getParent());
                dialog.setFile(f.getName());
            } else {
                dialog.setDirectory(cur);
            }
        }
        dialog.setFile("*.properties");
        dialog.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".properties"));
        dialog.setVisible(true);

        String fileName = dialog.getFile();
        String dirName = dialog.getDirectory();
        if (fileName != null && dirName != null) {
            java.io.File selectedFile = new java.io.File(dirName, fileName);
            String relCfg = com.girlkun.tool.utils.PathConfig.toRelativePath(selectedFile.getAbsolutePath());
            txtConfigPath.setText(relCfg);
            loadFromConfigFile(relCfg);

            // Nếu ô Data Path trống hoặc chưa thiết lập, thử suy ra data path từ config path
            java.io.File parent = selectedFile.getParentFile();
            if (parent != null && parent.getName().equalsIgnoreCase("config")) {
                java.io.File dataDir = parent.getParentFile();
                if (dataDir != null) {
                    txtDataPath.setText(com.girlkun.tool.utils.PathConfig.toRelativePath(dataDir.getAbsolutePath()));
                }
            }
        }
    }

    private String chooseNativeFolder(String title, String initialPath) {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                String initDir = "";
                if (initialPath != null && !initialPath.trim().isEmpty()) {
                    java.io.File f = new java.io.File(initialPath);
                    if (f.exists()) {
                        initDir = f.getAbsolutePath();
                    }
                }

                String script = "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8;\n"
                        + "Add-Type -AssemblyName System.Windows.Forms;\n"
                        + "$dlg = New-Object System.Windows.Forms.FolderBrowserDialog;\n"
                        + "$dlg.Description = '" + (title != null ? title.replace("'", "''") : "Chọn thư mục") + "';\n"
                        + "$dlg.ShowNewFolderButton = $true;\n"
                        + "$dlg.AutoUpgradeEnabled = $true;\n";
                if (!initDir.isEmpty()) {
                    script += "$dlg.SelectedPath = '" + initDir.replace("'", "''") + "';\n";
                }
                script += "if ($dlg.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {\n"
                        + "    Write-Output $dlg.SelectedPath\n"
                        + "}\n";

                String encoded = java.util.Base64.getEncoder().encodeToString(
                        script.getBytes(java.nio.charset.StandardCharsets.UTF_16LE)
                );

                ProcessBuilder pb = new ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-ExecutionPolicy", "Bypass",
                        "-STA",
                        "-EncodedCommand", encoded
                );
                pb.redirectErrorStream(true);
                Process p = pb.start();

                String selected = null;
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(p.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty() && new java.io.File(trimmed).isDirectory()) {
                            selected = trimmed;
                        }
                    }
                }
                p.waitFor();
                if (selected != null && !selected.isEmpty()) {
                    return selected;
                }
            } catch (Exception ignored) {
            }
        }

        // Fallback: JFileChooser
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title != null ? title : "Chọn thư mục");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (initialPath != null && !initialPath.trim().isEmpty()) {
            chooser.setCurrentDirectory(new java.io.File(initialPath));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    private void loadFromConfigFile(String configFilePath) {
        DbConfig cfg = ConfigManager.load(configFilePath);
        txtDbHost.setText(cfg.host);
        txtDbPort.setText(String.valueOf(cfg.port));
        txtDbUser.setText(cfg.user);
        txtDbPass.setText(cfg.password);
        txtDbName.setText(cfg.database);
        cbDbType.setSelectedIndex(cfg.dbType == DbConfig.DB_NRO_ARN ? 1 : 0);
        if (cfg.dataPath != null && !cfg.dataPath.trim().isEmpty()) {
            txtDataPath.setText(cfg.dataPath);
        }
    }

    private void loadConfig() {
        DbConfig cfg = ConfigManager.load();
        txtDataPath.setText(cfg.dataPath != null ? cfg.dataPath : com.girlkun.tool.utils.PathConfig.getDataPath());
        txtConfigPath.setText(cfg.configPath != null ? cfg.configPath : com.girlkun.tool.utils.PathConfig.getConfigPath());
        txtDbHost.setText(cfg.host);
        txtDbPort.setText(String.valueOf(cfg.port));
        txtDbUser.setText(cfg.user);
        txtDbPass.setText(cfg.password);
        txtDbName.setText(cfg.database);
        cbDbType.setSelectedIndex(cfg.dbType == DbConfig.DB_NRO_ARN ? 1 : 0);

        // Check connection status on load với timeout
        testConnectionQuick();
    }

    /**
     * Test kết nối nhanh với timeout - không block UI
     */
    private void testConnectionQuick() {
        setStatus(null, "Đang kiểm tra kết nối...", Color.ORANGE);

        // Disable buttons during test
        btnSave.setEnabled(false);
        btnTest.setEnabled(false);

        // Lấy thông tin từ form
        String host = txtDbHost.getText().trim();
        String portStr = txtDbPort.getText().trim();
        String user = txtDbUser.getText().trim();
        String pass = txtDbPass.getText();
        String dbName = txtDbName.getText().trim();

        // Chạy test trong background với timeout
        CompletableFuture.supplyAsync(() -> {
            try {
                int port = Integer.parseInt(portStr);
                return testConnectionWithTimeout(host, port, user, pass, dbName);
            } catch (NumberFormatException e) {
                return "Port không hợp lệ!";
            }
        }).orTimeout(CONNECTION_TIMEOUT_SECONDS + 1, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    SwingUtilities.invokeLater(() -> {
                        // Re-enable buttons
                        btnSave.setEnabled(true);
                        btnTest.setEnabled(true);

                        if (ex != null) {
                            // Timeout hoặc lỗi khác
                            setStatus(false,
                                    "Timeout - Không thể kết nối trong " + CONNECTION_TIMEOUT_SECONDS + " giây!",
                                    Color.RED);
                        } else if (result == null) {
                            // Kết nối thành công
                            setStatus(true, "Kết nối thành công!", new Color(0, 166, 90));
                        } else {
                            // Có lỗi - hiển thị toàn bộ
                            setStatus(false, result, Color.RED);
                        }
                    });
                });
    }

    /**
     * Test connection với timeout ngắn
     * 
     * @return null nếu thành công, message lỗi nếu thất bại
     */
    private String testConnectionWithTimeout(String host, int port, String user, String pass, String dbName) {
        String url = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&connectTimeout=%d&socketTimeout=%d",
                host, port, dbName,
                CONNECTION_TIMEOUT_SECONDS * 1000,
                CONNECTION_TIMEOUT_SECONDS * 1000);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                if (conn != null && conn.isValid(CONNECTION_TIMEOUT_SECONDS)) {
                    return null; // Thành công
                }
                return "Kết nối không hợp lệ!";
            }
        } catch (ClassNotFoundException e) {
            return "Không tìm thấy MySQL Driver!";
        } catch (Exception e) {
            String msg = e.getMessage();
            return msg != null ? msg : "Lỗi không xác định!";
        }
    }

    private void saveConfig() {
        try {
            String dataPath = txtDataPath.getText().trim();
            String configPath = txtConfigPath.getText().trim();
            if (dataPath.isEmpty()) {
                dataPath = "data/data";
            }
            if (configPath.isEmpty()) {
                configPath = dataPath + "/config/config.properties";
            }

            dataPath = com.girlkun.tool.utils.PathConfig.toRelativePath(dataPath);
            configPath = com.girlkun.tool.utils.PathConfig.toRelativePath(configPath);
            txtDataPath.setText(dataPath);
            txtConfigPath.setText(configPath);

            int port = Integer.parseInt(txtDbPort.getText().trim());
            int dbType = cbDbType.getSelectedIndex() == 1 ? DbConfig.DB_NRO_ARN : DbConfig.DB_TOMAHAWK;
            DbConfig cfg = new DbConfig(
                    txtDbHost.getText().trim(),
                    port,
                    txtDbUser.getText().trim(),
                    txtDbPass.getText(),
                    txtDbName.getText().trim(),
                    dbType);
            cfg.dataPath = dataPath;
            cfg.configPath = configPath;

            com.girlkun.tool.utils.PathConfig.saveToolConfig(dataPath, configPath);
            com.girlkun.Resource.setPathFileProperties(configPath);
            ConfigManager.save(cfg);
            dao.reloadConfig();
            com.girlkun.database.GirlkunDB.reload(); // Reload Main Tool DB config
            com.girlkun.tool.main.Manager.gI().reload(); // Reload cached templates (NPCs, Items, etc.)

            setStatus(true, "Đã lưu cấu hình và đồng bộ toàn bộ Tool!", new Color(0, 166, 90));

            // Test connection after save
            testConnectionQuick();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port phải là số nguyên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
