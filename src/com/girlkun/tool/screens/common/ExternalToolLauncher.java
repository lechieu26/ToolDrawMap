package com.girlkun.tool.screens.common;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;

/**
 * JInternalFrame wrapper dùng chung để quản lý và khởi chạy các external
 * executable tools
 */
public class ExternalToolLauncher extends JInternalFrame {

    private final String toolName;
    private final String exePathRelative;
    private final String exeFileName;
    private final String description;

    private JButton btnLaunch;
    private JButton btnStop;
    private JButton btnClearLog;
    private JTextArea logArea;
    private JLabel lblStatus;

    private Process process;
    private Thread outputThread;
    private volatile boolean isRunning = false;

    /**
     * @param toolName        Tên hiển thị của Tool (VD: "Path Editor")
     * @param exePathRelative Đường dẫn tương đối chứa file exe (VD:
     *                        "src/PathEditor")
     * @param exeFileName     Tên file exe (VD: "NROPartEditor.exe")
     * @param description     Mô tả ngắn gọn về tool
     */
    public ExternalToolLauncher(String toolName, String exePathRelative, String exeFileName, String description) {
        super(toolName + " Launcher", true, true, true, true);
        this.toolName = toolName;
        this.exePathRelative = exePathRelative;
        this.exeFileName = exeFileName;
        this.description = description;

        initComponents();
        setSize(650, 480);
        setMinimumSize(new Dimension(450, 350));
    }

    private void initComponents() {
        // Main panel với BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(45, 45, 55));

        // === Header Panel ===
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // === Log Area ===
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 40));
        logArea.setForeground(new Color(200, 200, 200));
        logArea.setCaretColor(Color.WHITE);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 100)),
                "Output Log",
                0, 0,
                new Font("SansSerif", Font.BOLD, 12),
                new Color(150, 150, 180)));
        scrollPane.setBackground(new Color(45, 45, 55));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // === Info Panel ===
        JPanel infoPanel = createInfoPanel();
        mainPanel.add(infoPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Initial log message
        appendLog("=== " + toolName + " Launcher ===");
        appendLog(description);
        appendLog("Click 'Launch' để khởi chạy " + toolName + ".");
        appendLog("");
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setOpaque(false);

        // Title
        JLabel lblTitle = new JLabel("🛠 " + toolName);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitle.setForeground(new Color(100, 200, 255));

        // Control Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);

        btnLaunch = createStyledButton("▶ Launch", new Color(46, 204, 113));
        btnLaunch.addActionListener(this::onLaunch);

        btnStop = createStyledButton("⏹ Stop", new Color(231, 76, 60));
        btnStop.setEnabled(false);
        btnStop.addActionListener(this::onStop);

        btnClearLog = createStyledButton("🗑 Clear Log", new Color(149, 165, 166));
        btnClearLog.addActionListener(e -> logArea.setText(""));

        buttonPanel.add(btnLaunch);
        buttonPanel.add(btnStop);
        buttonPanel.add(btnClearLog);

        // Status Panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.setOpaque(false);

        lblStatus = new JLabel("● Stopped");
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblStatus.setForeground(new Color(231, 76, 60));
        statusPanel.add(lblStatus);

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.CENTER);
        headerPanel.add(statusPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 100)),
                "Thông tin",
                0, 0,
                new Font("SansSerif", Font.BOLD, 12),
                new Color(150, 150, 180)));

        JLabel lblPath = new JLabel("📁 Path: " + exePathRelative + "/" + exeFileName);
        lblPath.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblPath.setForeground(new Color(180, 180, 180));

        JLabel lblDesc = new JLabel("📝 Note: " + description);
        lblDesc.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblDesc.setForeground(new Color(180, 180, 180));

        infoPanel.add(lblPath);
        infoPanel.add(lblDesc);

        return infoPanel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 32));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            Color originalColor = bgColor;

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(originalColor.brighter());
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });

        return button;
    }

    private void onLaunch(ActionEvent e) {
        if (isRunning) {
            appendLog("⚠ " + toolName + " đang chạy!");
            return;
        }

        try {
            String projectDir = System.getProperty("user.dir");
            File scriptDir = new File(projectDir, exePathRelative);

            if (!scriptDir.exists()) {
                appendLog("❌ Lỗi: Không tìm thấy thư mục: " + scriptDir.getAbsolutePath());
                return;
            }

            File exeFile = new File(scriptDir, exeFileName);
            if (!exeFile.exists()) {
                appendLog("❌ Lỗi: Không tìm thấy file: " + exeFile.getAbsolutePath());
                return;
            }

            appendLog("🚀 Đang khởi chạy " + toolName + "...");
            appendLog("📂 Working directory: " + scriptDir.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(exeFile.getAbsolutePath());
            pb.directory(scriptDir);
            pb.redirectErrorStream(true);

            process = pb.start();
            isRunning = true;

            updateUIState(true);
            appendLog("✅ " + toolName + " đã khởi chạy thành công!");
            appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Thread đọc output từ process
            outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String logLine = line;
                        SwingUtilities.invokeLater(() -> appendLog("[Tool] " + logLine));
                    }
                } catch (IOException ex) {
                    if (isRunning) {
                        SwingUtilities.invokeLater(() -> appendLog("⚠ Lỗi đọc output: " + ex.getMessage()));
                    }
                }

                // Process kết thúc
                SwingUtilities.invokeLater(() -> {
                    isRunning = false;
                    updateUIState(false);
                    appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    appendLog("🛑 " + toolName + " đã đóng.");
                });
            });
            outputThread.setDaemon(true);
            outputThread.start();

        } catch (IOException ex) {
            appendLog("❌ Lỗi khởi chạy: " + ex.getMessage());
            isRunning = false;
            updateUIState(false);
        }
    }

    private void onStop(ActionEvent e) {
        if (!isRunning || process == null) {
            appendLog("⚠ Không có process đang chạy.");
            return;
        }

        appendLog("🛑 Đang dừng " + toolName + "...");

        try {
            process.destroy();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        isRunning = false;
        updateUIState(false);
        appendLog("✅ " + toolName + " đã dừng.");
    }

    private void updateUIState(boolean running) {
        btnLaunch.setEnabled(!running);
        btnStop.setEnabled(running);

        if (running) {
            lblStatus.setText("● Running");
            lblStatus.setForeground(new Color(46, 204, 113));
        } else {
            lblStatus.setText("● Stopped");
            lblStatus.setForeground(new Color(231, 76, 60));
        }
    }

    private void appendLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    @Override
    public void dispose() {
        // Cleanup khi đóng cửa sổ
        if (isRunning && process != null) {
            process.destroy();
        }
        super.dispose();
    }

    // Getter để định danh trong Main nếu cần
    public String getToolName() {
        return toolName;
    }
}
