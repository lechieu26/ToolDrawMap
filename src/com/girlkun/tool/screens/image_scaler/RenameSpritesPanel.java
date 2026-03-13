package com.girlkun.tool.screens.image_scaler;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RenameSpritesPanel extends JPanel {
    private static final Color BG_COLOR = new Color(0x1a1a2e);
    private static final Color PANEL_COLOR = new Color(0x16213e);
    private static final Color ACCENT_COLOR = new Color(0x4a69bd);
    private static final Color SUCCESS_COLOR = new Color(0x27ae60);
    private static final Color DANGER_COLOR = new Color(0xe74c3c);
    private static final Color WARNING_COLOR = new Color(0xf39c12);
    private static final Color TEXT_COLOR = new Color(0xefffff);
    private static final Color INFO_COLOR = new Color(0xa0a0a0);
    private static final Color INPUT_BG = new Color(0x1a1a2e);

    private DefaultListModel<FileItem> listModel;
    private JList<FileItem> imageList;
    private JTextArea missingIdArea;
    private JTextField startNumField;
    private JLabel statusLabel;
    private File lastFolder;

    public RenameSpritesPanel() {
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
        JButton btnCheck = createStyledButton("Check id trống", WARNING_COLOR);
        JButton btnClear = createStyledButton("Clear All", DANGER_COLOR);

        btnChoose.addActionListener(e -> chooseFiles());
        btnCheck.addActionListener(e -> checkMissingIds());
        btnClear.addActionListener(e -> clearAll());

        btnPanel.add(btnChoose);
        btnPanel.add(btnCheck);
        btnPanel.add(btnClear);

        statusLabel = new JLabel("Chưa có ảnh nào được chọn");
        statusLabel.setForeground(INFO_COLOR);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));

        topPanel.add(btnPanel, BorderLayout.WEST);
        topPanel.add(statusLabel, BorderLayout.CENTER);

        // --- Middle Panel ---
        JPanel middlePanel = new JPanel(new BorderLayout(0, 10));
        middlePanel.setOpaque(false);

        // Missing ID section
        JPanel missingPanel = new JPanel(new BorderLayout(5, 5));
        missingPanel.setOpaque(false);
        JLabel missingHeader = new JLabel("Các ID còn trống trong thư mục:");
        missingHeader.setForeground(TEXT_COLOR);
        missingHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        missingPanel.add(missingHeader, BorderLayout.NORTH);

        missingIdArea = new JTextArea();
        missingIdArea.setBackground(PANEL_COLOR);
        missingIdArea.setForeground(new Color(206, 145, 120));
        missingIdArea.setCaretColor(Color.WHITE);
        missingIdArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        missingIdArea.setEditable(false);
        missingIdArea.setLineWrap(true);
        missingIdArea.setWrapStyleWord(true);
        JScrollPane missingScroll = new JScrollPane(missingIdArea);
        missingScroll.setPreferredSize(new Dimension(0, 100));
        missingScroll.setBorder(BorderFactory.createLineBorder(new Color(0x2a2a4a)));
        missingPanel.add(missingScroll, BorderLayout.CENTER);

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

        middlePanel.add(missingPanel, BorderLayout.NORTH);
        middlePanel.add(listContainer, BorderLayout.CENTER);

        // --- Bottom Panel ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        configPanel.setOpaque(false);
        JLabel startLabel = new JLabel("Tên bắt đầu (Số):");
        startLabel.setForeground(TEXT_COLOR);
        startNumField = new JTextField(10);
        startNumField.setBackground(INPUT_BG);
        startNumField.setForeground(Color.WHITE);
        startNumField.setCaretColor(Color.WHITE);
        startNumField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2a2a4a)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        configPanel.add(startLabel);
        configPanel.add(startNumField);

        JButton btnRename = createStyledButton("Đổi tên ngay", ACCENT_COLOR);
        btnRename.addActionListener(e -> renameFiles());

        bottomPanel.add(configPanel, BorderLayout.WEST);
        bottomPanel.add(btnRename, BorderLayout.EAST);

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
        if (lastFolder != null && lastFolder.exists()) {
            dialog.setDirectory(lastFolder.getAbsolutePath());
        }
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp");
        });
        dialog.setVisible(true);

        File[] files = dialog.getFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                // Check duplicate
                boolean exists = false;
                for (int i = 0; i < listModel.size(); i++) {
                    if (listModel.get(i).file.getAbsolutePath().equals(f.getAbsolutePath())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    listModel.addElement(new FileItem(f));
                    lastFolder = f.getParentFile();
                }
            }
            updateStatus();
        }
    }

    private void checkMissingIds() {
        File folder = null;
        if (!listModel.isEmpty()) {
            folder = listModel.get(0).file.getParentFile();
            lastFolder = folder;
        } else if (lastFolder != null && lastFolder.exists()) {
            folder = lastFolder;
        } else {
            Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
            FileDialog chooser = new FileDialog(parent, "Chọn thư mục", FileDialog.LOAD);
            if (lastFolder != null && lastFolder.exists()) {
                chooser.setDirectory(lastFolder.getAbsolutePath());
            }
            // Mẹo cho Windows: Cho phép click "Open" khi chưa chọn file hoặc chọn bất kỳ file nào để lấy thư mục
            chooser.setVisible(true);
            
            String dir = chooser.getDirectory();
            if (dir != null) {
                folder = new File(dir);
                lastFolder = folder;
            }
        }

        if (folder == null || !folder.exists()) return;

        try {
            File[] files = folder.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                if (lower.endsWith(".png")) {
                    String core = name.substring(0, name.length() - 4);
                    return core.matches("\\d+");
                }
                return false;
            });

            if (files == null || files.length == 0) {
                missingIdArea.setText("Không tìm thấy file PNG có tên là số.");
                missingIdArea.setForeground(new Color(206, 145, 120));
                return;
            }

            List<Integer> ids = new ArrayList<>();
            for (File f : files) {
                String name = f.getName();
                ids.add(Integer.parseInt(name.substring(0, name.length() - 4)));
            }
            Collections.sort(ids);

            List<Integer> missing = new ArrayList<>();
            int prev = -1;
            for (int id : ids) {
                if (id - prev > 1) {
                    for (int m = prev + 1; m < id; m++) {
                        missing.add(m);
                    }
                }
                prev = id;
            }

            if (missing.isEmpty()) {
                missingIdArea.setText("✅ Không có ID nào bị thiếu.");
                missingIdArea.setForeground(new Color(78, 201, 176));
            } else {
                String compressed = compressRanges(missing);
                missingIdArea.setText("❌ Các ID thiếu: " + compressed);
                missingIdArea.setForeground(new Color(206, 145, 120));
            }
        } catch (Exception e) {
            missingIdArea.setText("Lỗi: " + e.getMessage());
        }
    }

    private String compressRanges(List<Integer> nums) {
        if (nums == null || nums.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int start = nums.get(0);
        int prev = nums.get(0);

        for (int i = 1; i < nums.size(); i++) {
            int n = nums.get(i);
            if (n == prev + 1) {
                prev = n;
            } else {
                appendRange(sb, start, prev);
                sb.append(", ");
                start = prev = n;
            }
        }
        appendRange(sb, start, prev);
        return sb.toString();
    }

    private void appendRange(StringBuilder sb, int s, int e) {
        int count = e - s + 1;
        if (s == e) {
            sb.append(s).append(" (1)");
        } else {
            sb.append(s).append("-").append(e).append(" (").append(count).append(")");
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

    private void renameFiles() {
        if (listModel.isEmpty()) return;

        String startText = startNumField.getText().trim();
        if (!startText.matches("\\d+")) {
            JOptionPane.showMessageDialog(this, "Số bắt đầu không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int start = Integer.parseInt(startText);
        int count = listModel.size();
        int confirm = JOptionPane.showConfirmDialog(this, "Đổi tên " + count + " ảnh?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            File folder = listModel.get(0).file.getParentFile();
            for (int i = 0; i < count; i++) {
                File oldFile = listModel.get(i).file;
                String ext = getExtension(oldFile);
                File newFile = new File(oldFile.getParent(), (start + i) + ext);
                
                if (!oldFile.getAbsolutePath().equals(newFile.getAbsolutePath())) {
                    if (!oldFile.renameTo(newFile)) {
                        throw new IOException("Không thể đổi tên " + oldFile.getName() + " thành " + newFile.getName() + " (Tên mới có thể đã tồn tại hoặc file đang bị khóa)");
                    }
                }
            }

            JOptionPane.showMessageDialog(this, "Đã đổi tên " + count + " ảnh!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            
            if (folder != null && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(folder);
                } catch (Exception ignored) {}
            }

            listModel.clear();
            updateStatus();
            startNumField.setText("");
            checkMissingIds();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getExtension(File f) {
        String name = f.getName();
        int lastIdx = name.lastIndexOf('.');
        return (lastIdx == -1) ? "" : name.substring(lastIdx);
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
