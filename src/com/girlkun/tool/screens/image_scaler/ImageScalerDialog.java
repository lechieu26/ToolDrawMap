package com.girlkun.tool.screens.image_scaler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Image Scaler Tool - Java version
 * Scale ảnh theo các tỷ lệ x1 (25%), x2 (50%), x3 (75%), x4 (100%)
 */
public class ImageScalerDialog extends JInternalFrame {

    // Theme colors
    private static final Color BG_COLOR = new Color(0x1a1a2e);
    private static final Color LIST_BG = new Color(0x16213e);
    private static final Color TEXT_COLOR = new Color(0xefffff);
    private static final Color ACCENT_COLOR = new Color(0x4a69bd);
    private static final Color INFO_COLOR = new Color(0xa0a0a0);

    // Button colors
    private static final Color BTN_SELECT_COLOR = new Color(0x4a69bd);
    private static final Color BTN_CLEAR_COLOR = new Color(0xe74c3c);
    private static final Color BTN_RENAME_COLOR = new Color(0x9b59b6);
    private static final Color BTN_OPEN_COLOR = new Color(0xf39c12);
    private static final Color BTN_GENERATE_COLOR = new Color(0x27ae60);

    // Components
    private DefaultListModel<String> listModel;
    private JList<String> imageList;
    private JLabel countLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton btnSelect, btnClear, btnRename, btnOpen, btnGenerate;

    // Data
    private List<File> selectedImages = new ArrayList<>();
    private File outputDir;

    public ImageScalerDialog() {
        super("Image Scaler Tool", true, true, true, true);

        // Determine output directory - relative to tool's working directory
        String userDir = System.getProperty("user.dir");
        outputDir = new File(userDir, "output/ImageScaler");

        initUI();
        setSize(900, 650);
        setMinimumSize(new Dimension(800, 500));
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title panel
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Center panel with list
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom panel with progress
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createTitlePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_COLOR);

        // Title label
        JLabel titleLabel = new JLabel("Image Scaler Tool");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Subtitle label
        JLabel subtitleLabel = new JLabel("Scale ảnh theo tỷ lệ x1 (25%), x2 (50%), x3 (75%), x4 (100%)");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(INFO_COLOR);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Button panel
        JPanel buttonPanel = createButtonPanel();
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(subtitleLabel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(buttonPanel);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setBackground(BG_COLOR);

        btnSelect = createStyledButton("Chọn Ảnh", BTN_SELECT_COLOR);
        btnSelect.addActionListener(e -> selectImages());

        btnClear = createStyledButton("Xóa Tất Cả", BTN_CLEAR_COLOR);
        btnClear.addActionListener(e -> clearImages());

        btnRename = createStyledButton("Rename", BTN_RENAME_COLOR);
        btnRename.addActionListener(e -> renameFiles());

        btnOpen = createStyledButton("Open Output", BTN_OPEN_COLOR);
        btnOpen.addActionListener(e -> openOutputFolder());

        btnGenerate = createStyledButton("Generate", BTN_GENERATE_COLOR);
        btnGenerate.addActionListener(e -> generateImages());

        panel.add(btnSelect);
        panel.add(btnClear);
        panel.add(btnRename);
        panel.add(btnOpen);
        panel.add(Box.createHorizontalGlue());
        panel.add(btnGenerate);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(bgColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bgColor.brighter());
                } else {
                    g2.setColor(bgColor);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();

                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 35));
        return btn;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_COLOR);

        // Count label
        countLabel = new JLabel("Đã chọn: 0 ảnh");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        countLabel.setForeground(INFO_COLOR);
        panel.add(countLabel, BorderLayout.NORTH);

        // Image list
        listModel = new DefaultListModel<>();
        imageList = new JList<>(listModel);
        imageList.setFont(new Font("Consolas", Font.PLAIN, 12));
        imageList.setBackground(LIST_BG);
        imageList.setForeground(TEXT_COLOR);
        imageList.setSelectionBackground(ACCENT_COLOR);
        imageList.setSelectionForeground(Color.WHITE);
        imageList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Delete key binding
        imageList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelected();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(imageList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0x2a2a4a)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_COLOR);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Status label
        statusLabel = new JLabel("Sẵn sàng");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(INFO_COLOR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Output label
        JLabel outputLabel = new JLabel("Output: " + outputDir.getAbsolutePath());
        outputLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        outputLabel.setForeground(INFO_COLOR);
        outputLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(progressBar);
        panel.add(Box.createVerticalStrut(10));
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(outputLabel);

        return panel;
    }

    private void selectImages() {
        // Use native Windows FileDialog
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        FileDialog fileDialog = new FileDialog(parentFrame, "Chọn ảnh", FileDialog.LOAD);
        fileDialog.setMultipleMode(true);
        fileDialog.setFilenameFilter((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
                    lowerName.endsWith(".jpeg") || lowerName.endsWith(".bmp") ||
                    lowerName.endsWith(".gif") || lowerName.endsWith(".webp");
        });

        fileDialog.setVisible(true);

        File[] files = fileDialog.getFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (!selectedImages.contains(file)) {
                    selectedImages.add(file);
                    listModel.addElement(file.getName());
                }
            }
            updateCount();
        }
    }

    private void clearImages() {
        selectedImages.clear();
        listModel.clear();
        updateCount();
    }

    private void deleteSelected() {
        int[] indices = imageList.getSelectedIndices();
        // Delete from end to start to avoid index shifting
        for (int i = indices.length - 1; i >= 0; i--) {
            int idx = indices[i];
            selectedImages.remove(idx);
            listModel.remove(idx);
        }
        updateCount();
    }

    private void updateCount() {
        countLabel.setText("Đã chọn: " + selectedImages.size() + " ảnh");
    }

    private void setButtonsEnabled(boolean enabled) {
        btnSelect.setEnabled(enabled);
        btnClear.setEnabled(enabled);
        btnRename.setEnabled(enabled);
        btnOpen.setEnabled(enabled);
        btnGenerate.setEnabled(enabled);
    }

    private void generateImages() {
        if (selectedImages.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ít nhất một ảnh!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setButtonsEnabled(false);
        progressBar.setValue(0);

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Scale ratios
                String[] scaleNames = { "x1", "x2", "x3", "x4" };
                double[] scaleFactors = { 0.25, 0.50, 0.75, 1.00 };

                // Create output directories
                for (String scaleName : scaleNames) {
                    File dir = new File(outputDir, scaleName);
                    dir.mkdirs();
                }

                int total = selectedImages.size();
                for (int idx = 0; idx < total; idx++) {
                    File imgFile = selectedImages.get(idx);
                    updateStatus("Đang xử lý: " + imgFile.getName());

                    try {
                        BufferedImage originalImg = ImageIO.read(imgFile);
                        if (originalImg == null)
                            continue;

                        int origWidth = originalImg.getWidth();
                        int origHeight = originalImg.getHeight();
                        String baseName = getBaseName(imgFile.getName());

                        for (int i = 0; i < scaleNames.length; i++) {
                            String scaleName = scaleNames[i];
                            double scaleFactor = scaleFactors[i];

                            int newWidth = Math.max(1, (int) (origWidth * scaleFactor));
                            int newHeight = Math.max(1, (int) (origHeight * scaleFactor));

                            BufferedImage scaledImg;
                            if (scaleFactor < 1.0) {
                                scaledImg = scaleImage(originalImg, newWidth, newHeight);
                            } else {
                                scaledImg = originalImg;
                            }

                            File outputFile = new File(outputDir, scaleName + "/" + baseName + ".png");
                            ImageIO.write(scaledImg, "PNG", outputFile);
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi xử lý " + imgFile.getName() + ": " + e.getMessage());
                    }

                    int progress = (int) (((idx + 1) / (double) total) * 100);
                    publish(progress);
                }

                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int progress = chunks.get(chunks.size() - 1);
                progressBar.setValue(progress);
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                updateStatus("✅ Hoàn thành! Đã xử lý " + selectedImages.size() + " ảnh");
                JOptionPane.showMessageDialog(ImageScalerDialog.this,
                        "Đã xử lý xong " + selectedImages.size() + " ảnh!\n\nOutput: " + outputDir.getAbsolutePath(),
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }
        };

        worker.execute();
    }

    private void renameFiles() {
        if (selectedImages.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ít nhất một ảnh!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setButtonsEnabled(false);
        progressBar.setValue(0);

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                File renameDir = new File(outputDir, "file_rename");
                renameDir.mkdirs();

                int total = selectedImages.size();
                for (int idx = 0; idx < total; idx++) {
                    File imgFile = selectedImages.get(idx);
                    String baseName = getBaseName(imgFile.getName());

                    updateStatus("Đang rename: " + imgFile.getName() + " -> " + baseName);

                    try {
                        File outputFile = new File(renameDir, baseName);
                        copyFile(imgFile, outputFile);
                    } catch (Exception e) {
                        System.err.println("Lỗi rename " + imgFile.getName() + ": " + e.getMessage());
                    }

                    int progress = (int) (((idx + 1) / (double) total) * 100);
                    publish(progress);
                }

                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int progress = chunks.get(chunks.size() - 1);
                progressBar.setValue(progress);
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                File renameDir = new File(outputDir, "file_rename");
                updateStatus("✅ Hoàn thành! Đã rename " + selectedImages.size() + " file");
                JOptionPane.showMessageDialog(ImageScalerDialog.this,
                        "Đã rename xong " + selectedImages.size() + " file!\n\nOutput: " + renameDir.getAbsolutePath(),
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }
        };

        worker.execute();
    }

    private void openOutputFolder() {
        try {
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            Desktop.getDesktop().open(outputDir);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể mở thư mục: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    // Helper methods
    private String getBaseName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0) ? filename.substring(0, dotIndex) : filename;
    }

    private BufferedImage scaleImage(BufferedImage original, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        return scaled;
    }

    private void copyFile(File source, File dest) throws IOException {
        try (InputStream is = new FileInputStream(source);
                OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    // For standalone testing
    public static void main(String[] args) {
        try {
            // Use system look and feel for standalone testing
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Image Scaler Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);

            JDesktopPane desktop = new JDesktopPane();
            frame.setContentPane(desktop);

            ImageScalerDialog dialog = new ImageScalerDialog();
            desktop.add(dialog);
            dialog.setVisible(true);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
