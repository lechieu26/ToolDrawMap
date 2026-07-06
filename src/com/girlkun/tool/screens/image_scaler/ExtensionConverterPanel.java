package com.girlkun.tool.screens.image_scaler;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ExtensionConverterPanel extends JPanel {
    private static final Color BG_COLOR = new Color(0x1a1a2e);
    private static final Color PANEL_COLOR = new Color(0x16213e);
    private static final Color ACCENT_COLOR = new Color(0x4a69bd);
    private static final Color SUCCESS_COLOR = new Color(0x27ae60);
    private static final Color DANGER_COLOR = new Color(0xe74c3c);
    private static final Color WARNING_COLOR = new Color(0xf39c12);
    private static final Color TEXT_COLOR = new Color(0xefffff);
    private static final Color INFO_COLOR = new Color(0xa0a0a0);

    private DefaultListModel<FileItem> listModel;
    private JList<FileItem> imageList;
    private JLabel statusLabel;
    private JCheckBox reverseCheckBox;
    private File lastImagesFolder;
    private File outputDir;

    public ExtensionConverterPanel() {
        String userDir = System.getProperty("user.dir");
        outputDir = new File(userDir, "output/ExtensionConverter");
        
        setBackground(BG_COLOR);
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- Top Panel ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnChoose = createStyledButton("Chọn ảnh", SUCCESS_COLOR);
        JButton btnClear = createStyledButton("Clear All", DANGER_COLOR);
        JButton btnOpen = createStyledButton("Open Output", WARNING_COLOR);

        btnChoose.addActionListener(e -> chooseFiles());
        btnClear.addActionListener(e -> clearAll());
        btnOpen.addActionListener(e -> openOutputFolder());

        btnPanel.add(btnChoose);
        btnPanel.add(btnClear);
        btnPanel.add(btnOpen);

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        optionsPanel.setOpaque(false);
        reverseCheckBox = new JCheckBox("Chuyển từ .png sang không đuôi");
        reverseCheckBox.setForeground(TEXT_COLOR);
        reverseCheckBox.setOpaque(false);
        reverseCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        reverseCheckBox.addActionListener(e -> {
            clearAll(); // Clear list when changing mode to avoid confusion
        });
        optionsPanel.add(reverseCheckBox);

        statusLabel = new JLabel("Chưa có ảnh nào được chọn");
        statusLabel.setForeground(INFO_COLOR);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));

        JPanel topCenterPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        topCenterPanel.setOpaque(false);
        topCenterPanel.add(optionsPanel);
        topCenterPanel.add(statusLabel);

        topPanel.add(btnPanel, BorderLayout.WEST);
        topPanel.add(topCenterPanel, BorderLayout.CENTER);

        // --- Middle Panel ---
        JPanel middlePanel = new JPanel(new BorderLayout(0, 10));
        middlePanel.setOpaque(false);

        // Image List section
        JPanel listContainer = new JPanel(new BorderLayout(5, 5));
        listContainer.setOpaque(false);
        JLabel listHeader = new JLabel("Danh sách ảnh đã chọn:");
        listHeader.setForeground(TEXT_COLOR);
        listHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        listContainer.add(listHeader, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        imageList = new JList<>(listModel);
        imageList.setBackground(PANEL_COLOR);
        imageList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        imageList.setVisibleRowCount(-1);
        imageList.setCellRenderer(new ImageListRenderer());
        imageList.setFixedCellWidth(110);
        imageList.setFixedCellHeight(120);
        imageList.setSelectionBackground(ACCENT_COLOR);
        imageList.setBorder(BorderFactory.createLineBorder(new Color(0x2a2a4a)));

        JScrollPane listScroll = new JScrollPane(imageList);
        listScroll.getVerticalScrollBar().setUnitIncrement(16);
        listScroll.setBorder(null);
        listContainer.add(listScroll, BorderLayout.CENTER);

        middlePanel.add(listContainer, BorderLayout.CENTER);

        // --- Bottom Panel ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomPanel.setOpaque(false);

        JButton btnConvert = createStyledButton("Convert Ngay", ACCENT_COLOR);
        btnConvert.setPreferredSize(new Dimension(150, 35));
        btnConvert.addActionListener(e -> convertFiles());

        bottomPanel.add(btnConvert);

        // Assemble
        add(topPanel, BorderLayout.NORTH);
        add(middlePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Key Listeners
        imageList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelected();
                }
            }
        });
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(bg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bg.brighter());
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 35));
        return btn;
    }

    private void chooseFiles() {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        FileDialog dialog = new FileDialog(parent, "Chọn ảnh", FileDialog.LOAD);
        dialog.setMultipleMode(true);
        if (lastImagesFolder != null && lastImagesFolder.exists()) {
            dialog.setDirectory(lastImagesFolder.getAbsolutePath());
        }
        
        boolean isReverse = reverseCheckBox.isSelected();
        dialog.setFilenameFilter((dir, name) -> {
            if (isReverse) {
                return name.toLowerCase().endsWith(".png");
            } else {
                return !name.contains("."); // Filter files without extension
            }
        });
        
        dialog.setVisible(true);

        File[] files = dialog.getFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                boolean exists = false;
                for (int i = 0; i < listModel.size(); i++) {
                    if (listModel.get(i).file.getAbsolutePath().equals(f.getAbsolutePath())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    listModel.addElement(new FileItem(f));
                    lastImagesFolder = f.getParentFile();
                }
            }
            updateStatus();
        }
    }

    private void clearAll() {
        if (!listModel.isEmpty()) {
            listModel.clear();
            updateStatus();
        }
    }

    private void updateStatus() {
        int count = listModel.size();
        statusLabel.setText(count > 0 ? "Đã chọn " + count + " ảnh" : "Chưa có ảnh nào được chọn");
    }

    private void deleteSelected() {
        int[] indices = imageList.getSelectedIndices();
        for (int i = indices.length - 1; i >= 0; i--) {
            listModel.remove(indices[i]);
        }
        updateStatus();
    }

    private void convertFiles() {
        if (listModel.isEmpty()) return;

        int count = listModel.size();
        int confirm = JOptionPane.showConfirmDialog(this, "Convert " + count + " file?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        boolean isReverse = reverseCheckBox.isSelected();
        int successCount = 0;

        try {
            for (int i = 0; i < count; i++) {
                File oldFile = listModel.get(i).file;
                String oldName = oldFile.getName();
                String newName;

                if (isReverse) {
                    if (oldName.toLowerCase().endsWith(".png")) {
                        newName = oldName.substring(0, oldName.length() - 4);
                    } else {
                        newName = oldName;
                    }
                } else {
                    newName = oldName + ".png";
                }

                File newFile = new File(outputDir, newName);
                Files.copy(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                successCount++;
            }

            JOptionPane.showMessageDialog(this, "Đã convert thành công " + successCount + " file!\nLưu tại: " + outputDir.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
            
            listModel.clear();
            updateStatus();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi convert: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
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

    static class FileItem {
        File file;
        ImageIcon thumbnail;

        FileItem(File f) {
            this.file = f;
            createThumbnail();
        }

        private void createThumbnail() {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    int w = img.getWidth();
                    int h = img.getHeight();
                    double scale = Math.min(80.0/w, 70.0/h);
                    int tw = (int)(w * scale);
                    int th = (int)(h * scale);
                    Image thumb = img.getScaledInstance(tw, th, Image.SCALE_SMOOTH);
                    this.thumbnail = new ImageIcon(thumb);
                }
            } catch (Exception ignored) {}
        }
    }

    static class ImageListRenderer extends JPanel implements ListCellRenderer<FileItem> {
        private JLabel iconLabel = new JLabel();
        private JLabel textLabel = new JLabel();

        ImageListRenderer() {
            setLayout(new BorderLayout(5, 5));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            iconLabel.setPreferredSize(new Dimension(80, 70));
            iconLabel.setHorizontalAlignment(JLabel.CENTER);
            add(iconLabel, BorderLayout.CENTER);

            textLabel.setForeground(TEXT_COLOR);
            textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            textLabel.setHorizontalAlignment(JLabel.CENTER);
            add(textLabel, BorderLayout.SOUTH);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends FileItem> list, FileItem value, int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setBorder(BorderFactory.createLineBorder(new Color(0, 162, 255), 2));
            } else {
                setBackground(new Color(0x16213e));
                setBorder(BorderFactory.createLineBorder(new Color(0x2a2a4a), 1));
            }

            if (value != null) {
                iconLabel.setIcon(value.thumbnail);
                iconLabel.setText(value.thumbnail == null ? "N/A" : "");
                String name = value.file.getName();
                if (name.length() > 12) name = name.substring(0, 9) + "..";
                textLabel.setText(name);
            }
            return this;
        }
    }
}
