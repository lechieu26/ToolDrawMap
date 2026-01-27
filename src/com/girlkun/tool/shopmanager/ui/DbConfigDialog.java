package com.girlkun.tool.shopmanager.ui;

import com.girlkun.tool.shopmanager.models.DbConfig;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO;
import com.girlkun.tool.shopmanager.utils.ConfigManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Dialog cấu hình Database - Hiển thị khi khởi động ứng dụng
 */
public class DbConfigDialog extends JDialog {

    private JTextField txtDbHost, txtDbPort, txtDbUser, txtDbPass, txtDbName;
    private JLabel lblDbStatus;
    private boolean configSaved = false;
    private final ShopManagerDAO dao;

    public DbConfigDialog(Frame parent, boolean modal) {
        super(parent, "Cấu hình Database", modal);
        this.dao = ShopManagerDAO.gI();
        initComponents();
        loadConfig();

        // Center on screen
        setLocationRelativeTo(parent);

        // Handle close button
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
    }

    private void initComponents() {
        setSize(450, 350);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title
        JLabel lblTitle = new JLabel("Cấu hình kết nối Database", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(60, 141, 188));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtDbHost = new JTextField(20);
        txtDbPort = new JTextField(20);
        txtDbUser = new JTextField(20);
        txtDbPass = new JPasswordField(20);
        txtDbName = new JTextField(20);
        lblDbStatus = new JLabel(" ");
        lblDbStatus.setFont(lblDbStatus.getFont().deriveFont(Font.BOLD, 12f));
        lblDbStatus.setHorizontalAlignment(JLabel.CENTER);

        addFormRow(formPanel, gbc, 0, "Host:", txtDbHost);
        addFormRow(formPanel, gbc, 1, "Port:", txtDbPort);
        addFormRow(formPanel, gbc, 2, "User:", txtDbUser);
        addFormRow(formPanel, gbc, 3, "Password:", txtDbPass);
        addFormRow(formPanel, gbc, 4, "Database:", txtDbName);

        // Status label
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        formPanel.add(lblDbStatus, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton btnSave = new JButton("Lưu cấu hình");
        JButton btnTest = new JButton("Thử kết nối");
        JButton btnContinue = new JButton("Tiếp tục");

        styleButton(btnSave, new Color(60, 141, 188));
        styleButton(btnTest, new Color(0, 166, 90));
        styleButton(btnContinue, new Color(119, 119, 119));

        btnSave.addActionListener(e -> saveConfig());
        btnTest.addActionListener(e -> testConnection());
        btnContinue.addActionListener(e -> onContinue());

        btnPanel.add(btnSave);
        btnPanel.add(btnTest);
        btnPanel.add(btnContinue);

        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);
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

    private void loadConfig() {
        DbConfig cfg = ConfigManager.load();
        txtDbHost.setText(cfg.host);
        txtDbPort.setText(String.valueOf(cfg.port));
        txtDbUser.setText(cfg.user);
        txtDbPass.setText(cfg.password);
        txtDbName.setText(cfg.database);

        // Check connection status on load
        checkConnectionAsync();
    }

    private void checkConnectionAsync() {
        lblDbStatus.setText("Đang kiểm tra kết nối...");
        lblDbStatus.setForeground(Color.ORANGE);

        new Thread(() -> {
            boolean connected = dao.checkConnection();
            if (!connected) {
                try {
                    dao.connect();
                    connected = dao.checkConnection();
                } catch (Exception e) {
                    // Ignore
                }
            }

            final boolean isConnected = connected;
            SwingUtilities.invokeLater(() -> {
                if (isConnected) {
                    lblDbStatus.setText("✓ Kết nối thành công!");
                    lblDbStatus.setForeground(new Color(0, 166, 90));
                } else {
                    lblDbStatus.setText("✗ Chưa kết nối được Database!");
                    lblDbStatus.setForeground(Color.RED);
                }
            });
        }).start();
    }

    private void saveConfig() {
        try {
            int port = Integer.parseInt(txtDbPort.getText().trim());
            DbConfig cfg = new DbConfig(
                    txtDbHost.getText().trim(),
                    port,
                    txtDbUser.getText().trim(),
                    txtDbPass.getText(),
                    txtDbName.getText().trim());

            ConfigManager.save(cfg);
            dao.reloadConfig();

            lblDbStatus.setText("✓ Đã lưu cấu hình!");
            lblDbStatus.setForeground(new Color(0, 166, 90));
            configSaved = true;

            // Test connection after save
            testConnection();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port phải là số nguyên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void testConnection() {
        lblDbStatus.setText("Đang kết nối...");
        lblDbStatus.setForeground(Color.ORANGE);

        new Thread(() -> {
            try {
                dao.close();
                dao.connect();
                boolean connected = dao.checkConnection();

                SwingUtilities.invokeLater(() -> {
                    if (connected) {
                        lblDbStatus.setText("✓ Kết nối thành công!");
                        lblDbStatus.setForeground(new Color(0, 166, 90));
                    } else {
                        lblDbStatus.setText("✗ Kết nối thất bại!");
                        lblDbStatus.setForeground(Color.RED);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    lblDbStatus.setText("✗ Lỗi: " + e.getMessage());
                    lblDbStatus.setForeground(Color.RED);
                });
            }
        }).start();
    }

    private void onContinue() {
        dispose();
    }

    private void onClose() {
        int result = JOptionPane.showConfirmDialog(this,
                "Bạn có muốn thoát không?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    public boolean isConfigSaved() {
        return configSaved;
    }

    /**
     * Static method để mở dialog từ bất kỳ đâu
     */
    public static void showDialog(Frame parent) {
        DbConfigDialog dialog = new DbConfigDialog(parent, true);
        dialog.setVisible(true);
    }
}
