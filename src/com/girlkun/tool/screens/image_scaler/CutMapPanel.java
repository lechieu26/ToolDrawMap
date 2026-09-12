package com.girlkun.tool.screens.image_scaler;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel Cắt Map - Cho phép chọn một ảnh PNG và tự động cắt thành các hình vuông
 * bằng nhau Kích thước ô cắt tự động tính toán từ Width & Height (min 500x500, max 3000x3000)
 */
public class CutMapPanel extends JPanel {

    // Theme colors
    private static final Color BG_COLOR = new Color(0x1a1a2e);
    private static final Color PANEL_COLOR = new Color(0x16213e);
    private static final Color ACCENT_COLOR = new Color(0x4a69bd);
    private static final Color SUCCESS_COLOR = new Color(0x27ae60);
    private static final Color DANGER_COLOR = new Color(0xe74c3c);
    private static final Color WARNING_COLOR = new Color(0xf39c12);
    private static final Color TEXT_COLOR = new Color(0xefffff);
    private static final Color INFO_COLOR = new Color(0xa0a0a0);
    private static final Color INPUT_BG = new Color(0x1a1a2e);
    private static final Color BORDER_COLOR = new Color(0x2a2a4a);
    private static final Color GRID_COLOR = new Color(255, 215, 0, 220); // Vàng neon
    private static final Color GRID_HOVER_COLOR = new Color(0, 255, 200, 80); // Xanh lơ trong suốt

    // Components
    private MapCanvas mapCanvas;
    private JLabel lblImageInfo;
    private JLabel lblGridStats;
    private JLabel lblStatus;
    private JProgressBar progressBar;
    private JSpinner spinnerCellSize;
    private JSpinner spinnerOverlap;
    private JComboBox<String> comboBleedMode;
    private JComboBox<PresetSizeItem> comboPresets;
    private JComboBox<String> comboNaming;
    private JButton btnChoose;
    private JButton btnAutoCalc;
    private JButton btnCut;
    private JButton btnOpenOutput;
    private JButton btnClear;

    // Data
    private BufferedImage originalImage;
    private File currentImageFile;
    private int currentCellSize = 500;
    private int overlapPixels = 1; // Mặc định bù 1px viền
    private int bleedMode = 0; // 0: Thêm viền phải & dưới, 1: Mở rộng 4 phía, 2: Gối đầu bước nhảy, 3: Không bù
    private File outputDir;

    // Item cho danh sách gợi ý kích thước
    private static class PresetSizeItem {
        final int size;
        final String label;

        PresetSizeItem(int size, String label) {
            this.size = size;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public CutMapPanel() {
        // Output directory tương đối
        outputDir = new File("output/CutMap");

        setBackground(BG_COLOR);
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- TOP TOOLBAR ---
        JPanel topToolbar = new JPanel(new BorderLayout(10, 10));
        topToolbar.setOpaque(false);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);

        btnChoose = createStyledButton("Chọn Ảnh Map (PNG)", SUCCESS_COLOR);
        btnAutoCalc = createStyledButton("Tự Động Tính Toán", ACCENT_COLOR);
        btnCut = createStyledButton("Cắt & Xuất Map", WARNING_COLOR);
        btnOpenOutput = createStyledButton("Mở Folder Output", ACCENT_COLOR);
        btnClear = createStyledButton("Xóa / Làm Mới", DANGER_COLOR);

        btnAutoCalc.setEnabled(false);
        btnCut.setEnabled(false);

        btnChoose.addActionListener(e -> chooseImage());
        btnAutoCalc.addActionListener(e -> autoCalculateBestSize());
        btnCut.addActionListener(e -> cutAndExportMap());
        btnOpenOutput.addActionListener(e -> openOutputFolder());
        btnClear.addActionListener(e -> clearImage());

        btnPanel.add(btnChoose);
        btnPanel.add(btnAutoCalc);
        btnPanel.add(btnCut);
        btnPanel.add(btnOpenOutput);
        btnPanel.add(btnClear);

        lblImageInfo = new JLabel("Chưa có ảnh nào được chọn. Vui lòng chọn ảnh PNG để bắt đầu.");
        lblImageInfo.setForeground(INFO_COLOR);
        lblImageInfo.setFont(new Font("Segoe UI", Font.ITALIC, 13));

        topToolbar.add(btnPanel, BorderLayout.WEST);
        topToolbar.add(lblImageInfo, BorderLayout.CENTER);

        add(topToolbar, BorderLayout.NORTH);

        // --- CENTER SPLIT (Canvas + Settings Panel) ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);

        // Canvas preview
        mapCanvas = new MapCanvas();
        centerPanel.add(mapCanvas, BorderLayout.CENTER);

        // Sidebar settings
        JPanel sidebar = createSidebar();
        centerPanel.add(sidebar, BorderLayout.EAST);

        add(centerPanel, BorderLayout.CENTER);

        // --- BOTTOM STATUS BAR ---
        JPanel statusBar = new JPanel(new BorderLayout(10, 0));
        statusBar.setBackground(PANEL_COLOR);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(5, 10, 5, 10)
        ));
        statusBar.setPreferredSize(new Dimension(0, 32));

        lblStatus = new JLabel(" Sẵn sàng");
        lblStatus.setForeground(TEXT_COLOR);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(200, 18));
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        statusBar.add(lblStatus, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(PANEL_COLOR);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(12, 12, 12, 12)
        ));
        sidebar.setPreferredSize(new Dimension(310, 0));

        // Group 1: Kích thước ô cắt
        JPanel sizeGroup = new JPanel();
        sizeGroup.setLayout(new BoxLayout(sizeGroup, BoxLayout.Y_AXIS));
        sizeGroup.setOpaque(false);
        sizeGroup.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                " Cấu hình kích thước ô cắt ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                TEXT_COLOR
        ));

        // Gợi ý kích thước (Presets)
        JLabel lblPresets = new JLabel("Kích thước đề xuất (500px - 3000px):");
        lblPresets.setForeground(TEXT_COLOR);
        lblPresets.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPresets.setAlignmentX(Component.LEFT_ALIGNMENT);

        comboPresets = new JComboBox<>();
        comboPresets.setBackground(INPUT_BG);
        comboPresets.setForeground(TEXT_COLOR);
        comboPresets.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboPresets.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        comboPresets.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboPresets.addActionListener(e -> {
            PresetSizeItem item = (PresetSizeItem) comboPresets.getSelectedItem();
            if (item != null && item.size > 0 && item.size != currentCellSize) {
                spinnerCellSize.setValue(item.size);
            }
        });

        // Spinner tùy chỉnh kích thước
        JPanel customSizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        customSizePanel.setOpaque(false);
        customSizePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblCustom = new JLabel("Kích thước ô vuông (px):");
        lblCustom.setForeground(TEXT_COLOR);
        lblCustom.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(500, 50, 5000, 10);
        spinnerCellSize = new JSpinner(spinnerModel);
        spinnerCellSize.setFont(new Font("Segoe UI", Font.BOLD, 12));
        spinnerCellSize.setPreferredSize(new Dimension(90, 28));
        spinnerCellSize.addChangeListener(e -> {
            int newSize = (Integer) spinnerCellSize.getValue();
            if (newSize != currentCellSize) {
                currentCellSize = newSize;
                updateGridCalculations();
                mapCanvas.repaint();
            }
        });

        customSizePanel.add(lblCustom);
        customSizePanel.add(spinnerCellSize);

        // Spinner bù trừ viền (Overlap / Bleed)
        JPanel overlapPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        overlapPanel.setOpaque(false);
        overlapPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblOverlap = new JLabel("Số pixel bù viền:");
        lblOverlap.setForeground(TEXT_COLOR);
        lblOverlap.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        SpinnerNumberModel overlapModel = new SpinnerNumberModel(1, 0, 50, 1);
        spinnerOverlap = new JSpinner(overlapModel);
        spinnerOverlap.setFont(new Font("Segoe UI", Font.BOLD, 12));
        spinnerOverlap.setPreferredSize(new Dimension(80, 28));
        spinnerOverlap.setToolTipText("Số pixel lấy thêm/chờm viền để khử đường rãnh trắng");
        spinnerOverlap.addChangeListener(e -> {
            int newOverlap = (Integer) spinnerOverlap.getValue();
            if (newOverlap != overlapPixels) {
                overlapPixels = newOverlap;
                updateGridCalculations();
                mapCanvas.repaint();
            }
        });

        overlapPanel.add(lblOverlap);
        overlapPanel.add(spinnerOverlap);

        // Chế độ bù viền
        JLabel lblBleedMode = new JLabel("Kiểu bù viền chống đường trắng:");
        lblBleedMode.setForeground(TEXT_COLOR);
        lblBleedMode.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblBleedMode.setAlignmentX(Component.LEFT_ALIGNMENT);

        comboBleedMode = new JComboBox<>(new String[]{
                "Thêm viền Phải & Dưới (Ảnh: [S + 1] x [S + 1]) ★",
                "Mở rộng 4 phía (Ảnh: [S + 2] x [S + 2])",
                "Gối đầu viền (Bước nhảy lùi 1px, giữ nguyên cỡ)",
                "Không bù viền (Cắt phẳng thông thường)"
        });
        comboBleedMode.setBackground(INPUT_BG);
        comboBleedMode.setForeground(TEXT_COLOR);
        comboBleedMode.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        comboBleedMode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        comboBleedMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboBleedMode.setToolTipText("Chọn kiểu bù viền: Tùy chọn 1 sẽ lấy thêm 1px của ảnh bên cạnh đè lên, giúp triệt tiêu hoàn toàn đường trắng khi ghép map");
        comboBleedMode.addActionListener(e -> {
            bleedMode = comboBleedMode.getSelectedIndex();
            updateGridCalculations();
            mapCanvas.repaint();
        });

        sizeGroup.add(lblPresets);
        sizeGroup.add(Box.createVerticalStrut(5));
        sizeGroup.add(comboPresets);
        sizeGroup.add(Box.createVerticalStrut(8));
        sizeGroup.add(customSizePanel);
        sizeGroup.add(Box.createVerticalStrut(4));
        sizeGroup.add(overlapPanel);
        sizeGroup.add(Box.createVerticalStrut(4));
        sizeGroup.add(lblBleedMode);
        sizeGroup.add(Box.createVerticalStrut(4));
        sizeGroup.add(comboBleedMode);

        // Group 2: Thống kê lưới cắt
        JPanel statsGroup = new JPanel(new BorderLayout());
        statsGroup.setOpaque(false);
        statsGroup.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                " Thống kê lưới phân mảnh ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                TEXT_COLOR
        ));

        lblGridStats = new JLabel("<html><body style='color:#efffff; font-family:Segoe UI; font-size:11px;'>"
                + "• Chiều rộng: 0 px<br>"
                + "• Chiều cao: 0 px<br>"
                + "• Kích thước ô: 500 x 500 px<br>"
                + "• Số cột (Cols): 0<br>"
                + "• Số hàng (Rows): 0<br>"
                + "• Tổng số mảnh: 0 ô<br>"
                + "• Độ phủ viền: Chưa có ảnh"
                + "</body></html>");
        lblGridStats.setBorder(new EmptyBorder(5, 8, 8, 8));
        statsGroup.add(lblGridStats, BorderLayout.CENTER);

        // Group 3: Tùy chọn xuất file
        JPanel exportGroup = new JPanel();
        exportGroup.setLayout(new BoxLayout(exportGroup, BoxLayout.Y_AXIS));
        exportGroup.setOpaque(false);
        exportGroup.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                " Tùy chọn xuất file ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                TEXT_COLOR
        ));

        JLabel lblNaming = new JLabel("Định dạng tên file xuất:");
        lblNaming.setForeground(TEXT_COLOR);
        lblNaming.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblNaming.setAlignmentX(Component.LEFT_ALIGNMENT);

        comboNaming = new JComboBox<>(new String[]{
                "map_{row}_{col}.png  (Ví dụ: map_0_0.png)",
                "{name}_{row}_{col}.png (Ví dụ: tenanh_0_0.png)",
                "tile_{index}.png  (Ví dụ: tile_001.png)",
                "{name}_{index}.png (Ví dụ: tenanh_001.png)"
        });
        comboNaming.setBackground(INPUT_BG);
        comboNaming.setForeground(TEXT_COLOR);
        comboNaming.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboNaming.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        comboNaming.setAlignmentX(Component.LEFT_ALIGNMENT);

        exportGroup.add(lblNaming);
        exportGroup.add(Box.createVerticalStrut(5));
        exportGroup.add(comboNaming);

        // Hướng dẫn tương tác
        JPanel guideGroup = new JPanel(new BorderLayout());
        guideGroup.setOpaque(false);
        guideGroup.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                " Thao tác xem trước ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                TEXT_COLOR
        ));

        JLabel lblGuide = new JLabel("<html><body style='color:#a0a0a0; font-family:Segoe UI; font-size:11px;'>"
                + "• <b>Cuộn chuột</b>: Phóng to / Thu nhỏ<br>"
                + "• <b>Kéo chuột phải / giữa</b>: Di chuyển bản đồ<br>"
                + "• <b>Rê chuột vào ô</b>: Xem tọa độ & kích thước"
                + "</body></html>");
        lblGuide.setBorder(new EmptyBorder(5, 8, 8, 8));
        guideGroup.add(lblGuide, BorderLayout.CENTER);

        // Thêm các group vào sidebar
        sidebar.add(sizeGroup);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(statsGroup);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(exportGroup);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(guideGroup);
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(0x3a3a4e));
                } else if (getModel().isPressed()) {
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
        btn.setPreferredSize(new Dimension(160, 34));
        return btn;
    }

    /**
     * Chọn file ảnh PNG
     */
    private void chooseImage() {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        FileDialog dialog = new FileDialog(parent, "Chọn Ảnh Map PNG", FileDialog.LOAD);
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png");
        });
        if (currentImageFile != null && currentImageFile.getParent() != null) {
            dialog.setDirectory(currentImageFile.getParent());
        }
        dialog.setVisible(true);

        String dir = dialog.getDirectory();
        String file = dialog.getFile();
        if (dir != null && file != null) {
            File selectedFile = new File(dir, file);
            loadFile(selectedFile);
        }
    }

    /**
     * Tải và phân tích ảnh
     */
    private void loadFile(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                JOptionPane.showMessageDialog(this, "Không thể đọc định dạng ảnh này!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            this.originalImage = img;
            this.currentImageFile = file;

            lblImageInfo.setText(String.format("File: %s | Kích thước: %d x %d px | Dung lượng: %.2f KB",
                    file.getName(), img.getWidth(), img.getHeight(), file.length() / 1024.0));
            lblImageInfo.setForeground(TEXT_COLOR);

            btnAutoCalc.setEnabled(true);
            btnCut.setEnabled(true);

            // Tự động tính toán kích thước ô cắt tối ưu
            calculatePresetsAndBestSize();

            // Reset view trên canvas
            mapCanvas.resetView();
            setStatus("Đã tải ảnh: " + file.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi đọc file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Lấy bước nhảy lưới theo chế độ bù viền
     */
    public int getGridStep(int s, int overlap, int mode) {
        if (mode == 2) { // Gối đầu viền (bước nhảy lùi lại overlap px)
            return Math.max(1, s - overlap);
        }
        return s; // Các chế độ khác bước nhảy đúng bằng s
    }

    /**
     * Tính số cột với kích thước ô s và bù trừ viền
     */
    public int calculateCols(int imgW, int s, int overlap, int mode) {
        if (imgW <= 0 || s <= 0) return 0;
        int step = getGridStep(s, overlap, mode);
        int count = 0;
        for (int x = 0; x < imgW; x += step) {
            count++;
            if (x + s >= imgW) break;
        }
        return count;
    }

    /**
     * Tính số hàng với kích thước ô s và bù trừ viền
     */
    public int calculateRows(int imgH, int s, int overlap, int mode) {
        if (imgH <= 0 || s <= 0) return 0;
        int step = getGridStep(s, overlap, mode);
        int count = 0;
        for (int y = 0; y < imgH; y += step) {
            count++;
            if (y + s >= imgH) break;
        }
        return count;
    }

    /**
     * Thuật toán tính toán kích thước ô cắt tự động:
     * - Giới hạn: min 500x500, max 3000x3000
     * - Ưu tiên các kích thước chia hết cả Width và Height
     */
    private void calculatePresetsAndBestSize() {
        if (originalImage == null) return;

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int overlap = overlapPixels;
        int mode = bleedMode;

        comboPresets.removeAllItems();

        List<PresetSizeItem> perfectSizes = new ArrayList<>();
        List<PresetSizeItem> semiPerfectSizes = new ArrayList<>();
        List<PresetSizeItem> otherSizes = new ArrayList<>();

        // Giới hạn tìm kiếm trong khoảng [500, 3000]
        int minSize = 500;
        int maxSize = 3000;

        // Nếu ảnh nhỏ hơn 500px, điều chỉnh minSize phù hợp
        if (width < 500 && height < 500) {
            minSize = Math.max(50, Math.min(width, height));
            maxSize = Math.max(minSize, Math.max(width, height));
        } else {
            // Giới hạn maxSize không vượt quá kích thước lớn nhất của ảnh hoặc 3000
            maxSize = Math.min(3000, Math.max(500, Math.max(width, height)));
        }

        // 1. Quét tìm các kích thước chia hết
        for (int s = maxSize; s >= minSize; s -= 10) {
            int step = getGridStep(s, overlap, mode);
            boolean dividesW = (width <= s) || ((width - s) % step == 0);
            boolean dividesH = (height <= s) || ((height - s) % step == 0);

            int cols = calculateCols(width, s, overlap, mode);
            int rows = calculateRows(height, s, overlap, mode);

            if (dividesW && dividesH) {
                perfectSizes.add(new PresetSizeItem(s, String.format("★ %d x %d px [Hoàn hảo: %d x %d = %d ô]",
                        s, s, cols, rows, cols * rows)));
            } else if (dividesW || dividesH) {
                String tag = dividesW ? "Khít W" : "Khít H";
                semiPerfectSizes.add(new PresetSizeItem(s, String.format("● %d x %d px [%s: %d x %d ô]",
                        s, s, tag, cols, rows)));
            }
        }

        // 2. Thêm các kích thước phổ biến từ 500 đến 3000
        int[] standardSizes = {3000, 2500, 2048, 2000, 1500, 1200, 1024, 1000, 900, 800, 700, 600, 512, 500};
        for (int s : standardSizes) {
            if (s >= minSize && s <= maxSize) {
                boolean already = false;
                for (PresetSizeItem item : perfectSizes) if (item.size == s) already = true;
                for (PresetSizeItem item : semiPerfectSizes) if (item.size == s) already = true;
                if (!already) {
                    int cols = calculateCols(width, s, overlap, mode);
                    int rows = calculateRows(height, s, overlap, mode);
                    otherSizes.add(new PresetSizeItem(s, String.format("%d x %d px [%d x %d = %d ô]",
                            s, s, cols, rows, cols * rows)));
                }
            }
        }

        // Đổ danh sách vào JComboBox
        int bestSize = 500;
        if (!perfectSizes.isEmpty()) {
            for (PresetSizeItem item : perfectSizes) comboPresets.addItem(item);
            bestSize = perfectSizes.get(0).size; // Kích thước hoàn hảo lớn nhất
        }
        for (PresetSizeItem item : semiPerfectSizes) {
            comboPresets.addItem(item);
        }
        for (PresetSizeItem item : otherSizes) {
            comboPresets.addItem(item);
        }

        if (comboPresets.getItemCount() == 0) {
            comboPresets.addItem(new PresetSizeItem(500, "500 x 500 px (Mặc định)"));
            bestSize = 500;
        }

        // Nếu không có kích thước hoàn hảo, tìm S sao cho phần dư nhỏ nhất
        if (perfectSizes.isEmpty()) {
            int minRemainder = Integer.MAX_VALUE;
            for (int s = minSize; s <= maxSize; s += 10) {
                int step = getGridStep(s, overlap, mode);
                int remW = (width <= s) ? 0 : ((width - s) % step);
                int remH = (height <= s) ? 0 : ((height - s) % step);
                int totalRem = (remW == 0 ? 0 : (step - remW)) + (remH == 0 ? 0 : (step - remH));
                if (totalRem < minRemainder) {
                    minRemainder = totalRem;
                    bestSize = s;
                }
            }
        }

        currentCellSize = bestSize;
        spinnerCellSize.setValue(bestSize);
        updateGridCalculations();
    }

    /**
     * Tự động tính toán lại kích thước tối ưu khi bấm nút
     */
    private void autoCalculateBestSize() {
        if (originalImage == null) return;
        calculatePresetsAndBestSize();
        mapCanvas.resetView();
        JOptionPane.showMessageDialog(this,
                String.format("Đã tự động tính toán kích thước ô cắt tối ưu: %d x %d px", currentCellSize, currentCellSize),
                "Tự động tính toán", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Cập nhật thông tin thống kê lưới cắt
     */
    private void updateGridCalculations() {
        if (originalImage == null) {
            lblGridStats.setText("<html><body style='color:#efffff; font-family:Segoe UI; font-size:11px;'>"
                    + "• Chiều rộng: 0 px<br>"
                    + "• Chiều cao: 0 px<br>"
                    + "• Kích thước ô: " + currentCellSize + " x " + currentCellSize + " px<br>"
                    + "• Số cột (Cols): 0<br>"
                    + "• Số hàng (Rows): 0<br>"
                    + "• Tổng số mảnh: 0 ô<br>"
                    + "• Độ phủ viền: Chưa có ảnh"
                    + "</body></html>");
            return;
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int s = Math.max(1, currentCellSize);
        int overlap = overlapPixels;
        int mode = bleedMode;
        int step = getGridStep(s, overlap, mode);

        int cols = calculateCols(width, s, overlap, mode);
        int rows = calculateRows(height, s, overlap, mode);
        int totalTiles = cols * rows;

        // Tính kích thước file ảnh thực tế xuất ra
        int outW = s;
        int outH = s;
        String modeDesc;
        if (mode == 0) {
            outW = s + overlap;
            outH = s + overlap;
            modeDesc = String.format("<b style='color:#27ae60;'>Thêm viền Phải & Dưới (+%dpx)</b>", overlap);
        } else if (mode == 1) {
            outW = s + 2 * overlap;
            outH = s + 2 * overlap;
            modeDesc = String.format("<b style='color:#27ae60;'>Mở rộng 4 phía (+%dpx)</b>", overlap);
        } else if (mode == 2) {
            modeDesc = String.format("<b style='color:#27ae60;'>Gối đầu viền (Lùi %dpx)</b>", overlap);
        } else {
            modeDesc = "<b style='color:#a0a0a0;'>Không bù viền</b>";
        }

        boolean perfectW = (width <= s) || ((width - s) % step == 0);
        boolean perfectH = (height <= s) || ((height - s) % step == 0);

        String coverageText;
        if (perfectW && perfectH) {
            coverageText = "<span style='color:#27ae60; font-weight:bold;'>Hoàn hảo (Khít 100%)</span>";
        } else {
            int lastX = (cols > 0) ? (cols - 1) * step : 0;
            int lastY = (rows > 0) ? (rows - 1) * step : 0;
            int lastW = Math.min(s, width - lastX);
            int lastH = Math.min(s, height - lastY);
            coverageText = String.format("<span style='color:#f39c12;'>Rìa cuối: %d x %d px</span>", lastW, lastH);
        }

        lblGridStats.setText(String.format("<html><body style='color:#efffff; font-family:Segoe UI; font-size:11px;'>"
                        + "• Kích thước ảnh gốc: <b>%d x %d px</b><br>"
                        + "• Kích thước ô lưới: <b style='color:#f39c12;'>%d x %d px</b><br>"
                        + "• Kích thước ảnh xuất: <b style='color:#00e676;'>%d x %d px</b><br>"
                        + "• Kiểu bù viền: %s<br>"
                        + "• Số cột: <b>%d</b> | Số hàng: <b>%d</b><br>"
                        + "• Tổng số mảnh: <b style='color:#27ae60; font-size:12px;'>%d ô</b><br>"
                        + "• Phủ biên: %s"
                        + "</body></html>",
                width, height, s, s, outW, outH, modeDesc, cols, rows, totalTiles, coverageText));
    }

    /**
     * Trích xuất một mảnh tile:
     * - Ô thông thường: kích thước cellW x cellH (kèm bù viền nếu còn ảnh lân cận).
     * - Ô ở rìa cuối bên phải hoặc dưới: chỉ cắt đúng phần còn lại của ảnh gốc, không thừa ra.
     */
    private BufferedImage extractTileImage(BufferedImage src, int startX, int startY, int cellW, int cellH, int bleed, int mode) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();

        // Kiểm tra xem bên phải và bên dưới còn ảnh lân cận để bù viền không
        boolean hasRightNeighbor = (startX + cellW < srcW);
        boolean hasBottomNeighbor = (startY + cellH < srcH);
        boolean hasLeftNeighbor = (startX > 0);
        boolean hasTopNeighbor = (startY > 0);

        int bleedRight = (mode == 0 || mode == 1) && hasRightNeighbor ? bleed : 0;
        int bleedBottom = (mode == 0 || mode == 1) && hasBottomNeighbor ? bleed : 0;
        int bleedLeft = (mode == 1) && hasLeftNeighbor ? bleed : 0;
        int bleedTop = (mode == 1) && hasTopNeighbor ? bleed : 0;

        int cropX = Math.max(0, startX - bleedLeft);
        int cropY = Math.max(0, startY - bleedTop);
        int targetW = cellW + bleedLeft + bleedRight;
        int targetH = cellH + bleedTop + bleedBottom;

        // Giới hạn không vượt quá kích thước còn lại của ảnh gốc (không thừa ra ngoài)
        targetW = Math.min(targetW, srcW - cropX);
        targetH = Math.min(targetH, srcH - cropY);

        if (targetW <= 0 || targetH <= 0) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        return src.getSubimage(cropX, cropY, targetW, targetH);
    }

    /**
     * Cắt ảnh thành các ô vuông và xuất ra thư mục output
     */
    private void cutAndExportMap() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ảnh trước khi cắt!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int s = currentCellSize;
        final int overlap = overlapPixels;
        final int mode = bleedMode;
        final int step = getGridStep(s, overlap, mode);

        if (s <= 0) {
            JOptionPane.showMessageDialog(this, "Kích thước ô cắt không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final int imgW = originalImage.getWidth();
        final int imgH = originalImage.getHeight();
        final int cols = calculateCols(imgW, s, overlap, mode);
        final int rows = calculateRows(imgH, s, overlap, mode);
        final int totalTiles = cols * rows;

        final int namingOption = comboNaming.getSelectedIndex();

        String rawName = currentImageFile != null ? currentImageFile.getName() : "map";
        int dotIndex = rawName.lastIndexOf('.');
        final String baseName = (dotIndex > 0) ? rawName.substring(0, dotIndex) : rawName;

        // Thư mục lưu: output/CutMap/<baseName>
        final File targetDir = new File(outputDir, baseName);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // Khóa nút trong quá trình xử lý
        btnCut.setEnabled(false);
        btnChoose.setEnabled(false);
        btnClear.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(0);

        setStatus(String.format("Đang cắt %d mảnh map (Bù viền: %dpx)...", totalTiles, overlap));

        SwingWorker<Integer, Integer> worker = new SwingWorker<Integer, Integer>() {
            @Override
            protected Integer doInBackground() throws Exception {
                int count = 0;
                int tileIndex = 1;

                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        int x = c * step;
                        int y = r * step;
                        int w = Math.min(s, imgW - x);
                        int h = Math.min(s, imgH - y);

                        if (w <= 0 || h <= 0) continue;

                        // Trích xuất mảnh ảnh: truyền đúng kích thước w, h còn lại của ảnh gốc
                        BufferedImage tileImg = extractTileImage(originalImage, x, y, w, h, overlap, mode);

                        // Định dạng tên file
                        String fileName;
                        switch (namingOption) {
                            case 1: // {name}_{row}_{col}.png
                                fileName = String.format("%s_%d_%d.png", baseName, r, c);
                                break;
                            case 2: // tile_{index}.png
                                fileName = String.format("tile_%03d.png", tileIndex);
                                break;
                            case 3: // {name}_{index}.png
                                fileName = String.format("%s_%03d.png", baseName, tileIndex);
                                break;
                            case 0: // map_{row}_{col}.png
                            default:
                                fileName = String.format("map_%d_%d.png", r, c);
                                break;
                        }

                        File outFile = new File(targetDir, fileName);
                        ImageIO.write(tileImg, "PNG", outFile);

                        count++;
                        tileIndex++;

                        int progress = (int) (((double) count / totalTiles) * 100);
                        publish(progress);
                    }
                }
                return count;
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    int lastProgress = chunks.get(chunks.size() - 1);
                    progressBar.setValue(lastProgress);
                }
            }

            private void finishTask() {
                btnCut.setEnabled(true);
                btnChoose.setEnabled(true);
                btnClear.setEnabled(true);
                progressBar.setVisible(false);
            }

            @Override
            protected void done() {
                finishTask();
                try {
                    int savedCount = get();
                    setStatus(String.format("Đã cắt thành công %d mảnh map vào thư mục: %s", savedCount, targetDir.getPath()));

                    int option = JOptionPane.showOptionDialog(
                            CutMapPanel.this,
                            String.format("Đã cắt và lưu thành công %d mảnh map!\n\nThư mục lưu:\n%s",
                                    savedCount, targetDir.getAbsolutePath()),
                            "Cắt Map Thành Công",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            new String[]{"Mở Thư Mục", "Đóng"},
                            "Mở Thư Mục"
                    );

                    if (option == 0) {
                        openOutputFolder(targetDir);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CutMapPanel.this, "Lỗi trong quá trình cắt: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void openOutputFolder() {
        openOutputFolder(outputDir);
    }

    private void openOutputFolder(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            Desktop.getDesktop().open(dir);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không thể mở thư mục: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearImage() {
        originalImage = null;
        currentImageFile = null;
        comboPresets.removeAllItems();
        lblImageInfo.setText("Chưa có ảnh nào được chọn. Vui lòng chọn ảnh PNG để bắt đầu.");
        lblImageInfo.setForeground(INFO_COLOR);
        btnAutoCalc.setEnabled(false);
        btnCut.setEnabled(false);
        updateGridCalculations();
        mapCanvas.repaint();
        setStatus("Đã làm mới.");
    }

    private void setStatus(String text) {
        lblStatus.setText(" " + text);
    }

    /**
     * Canvas hiển thị ảnh Map và lưới ô vuông trực quan
     */
    private class MapCanvas extends JPanel {
        private double zoom = 1.0;
        private double offsetX = 0, offsetY = 0;
        private Point lastMouse;
        private Point hoverTile = null; // Tọa độ hàng, cột đang hover

        public MapCanvas() {
            setBackground(new Color(0x0f0f1a));

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastMouse = e.getPoint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)
                            || (SwingUtilities.isLeftMouseButton(e) && e.isAltDown())) {
                        offsetX += e.getX() - lastMouse.x;
                        offsetY += e.getY() - lastMouse.y;
                        lastMouse = e.getPoint();
                        repaint();
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    if (originalImage == null) return;

                    Point2D worldPos = screenToWorld(e.getPoint());
                    int s = currentCellSize;
                    int overlap = overlapPixels;
                    int mode = bleedMode;
                    int step = getGridStep(s, overlap, mode);
                    int imgW = originalImage.getWidth();
                    int imgH = originalImage.getHeight();
                    int cols = calculateCols(imgW, s, overlap, mode);
                    int rows = calculateRows(imgH, s, overlap, mode);

                    if (s > 0 && worldPos.getX() >= 0 && worldPos.getX() < imgW
                            && worldPos.getY() >= 0 && worldPos.getY() < imgH) {
                        int col = (int) (worldPos.getX() / step);
                        int row = (int) (worldPos.getY() / step);
                        if (col >= cols) col = cols - 1;
                        if (row >= rows) row = rows - 1;

                        if (hoverTile == null || hoverTile.x != col || hoverTile.y != row) {
                            hoverTile = new Point(col, row);
                            int x = col * step;
                            int y = row * step;
                            int w = Math.min(s, imgW - x);
                            int h = Math.min(s, imgH - y);
                            setStatus(String.format("Ô [%d, %d] | Vị trí: (%d, %d) | Kích thước ô: %dx%d px",
                                    row, col, x, y, w, h));
                            repaint();
                        }
                    } else {
                        if (hoverTile != null) {
                            hoverTile = null;
                            repaint();
                        }
                    }
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    double delta = e.getPreciseWheelRotation();
                    double scale = Math.pow(1.15, -delta);

                    Point2D mousePos = e.getPoint();
                    Point2D worldPos = screenToWorld(mousePos);

                    zoom *= scale;
                    zoom = Math.max(0.02, Math.min(zoom, 20.0));

                    Point2D newScreenPos = worldToScreen(worldPos);
                    offsetX += mousePos.getX() - newScreenPos.getX();
                    offsetY += mousePos.getY() - newScreenPos.getY();

                    repaint();
                }
            };

            addMouseListener(adapter);
            addMouseMotionListener(adapter);
            addMouseWheelListener(adapter);
        }

        public void resetView() {
            if (originalImage == null) return;
            int canvasW = Math.max(1, getWidth());
            int canvasH = Math.max(1, getHeight());

            double zoomX = (double) (canvasW - 40) / originalImage.getWidth();
            double zoomY = (double) (canvasH - 40) / originalImage.getHeight();
            zoom = Math.min(zoomX, zoomY);
            if (zoom <= 0) zoom = 1.0;

            offsetX = (canvasW - originalImage.getWidth() * zoom) / 2.0;
            offsetY = (canvasH - originalImage.getHeight() * zoom) / 2.0;
            repaint();
        }

        private Point2D screenToWorld(Point2D p) {
            try {
                return getTransform().createInverse().transform(p, null);
            } catch (NoninvertibleTransformException ex) {
                return p;
            }
        }

        private Point2D worldToScreen(Point2D p) {
            return getTransform().transform(p, null);
        }

        private AffineTransform getTransform() {
            AffineTransform at = new AffineTransform();
            at.translate(offsetX, offsetY);
            at.scale(zoom, zoom);
            return at;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // Bật Anti-Aliasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (originalImage == null) {
                // Hiển thị hướng dẫn khi chưa có ảnh
                g2.setColor(new Color(0x3a3a5e));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                String msg = "Kéo thả hoặc bấm 'Chọn Ảnh Map (PNG)' để bắt đầu";
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(msg)) / 2;
                int ty = getHeight() / 2;
                g2.drawString(msg, tx, ty);
                return;
            }

            AffineTransform oldAt = g2.getTransform();
            g2.transform(getTransform());

            // 1. Vẽ ảnh gốc
            g2.drawImage(originalImage, 0, 0, null);

            // 2. Vẽ lưới ô vuông (Grid Lines)
            int imgW = originalImage.getWidth();
            int imgH = originalImage.getHeight();
            int s = Math.max(1, currentCellSize);
            int overlap = overlapPixels;
            int mode = bleedMode;
            int step = getGridStep(s, overlap, mode);

            int cols = calculateCols(imgW, s, overlap, mode);
            int rows = calculateRows(imgH, s, overlap, mode);

            // Vẽ highlight ô hover
            if (hoverTile != null && hoverTile.x < cols && hoverTile.y < rows) {
                int hx = hoverTile.x * step;
                int hy = hoverTile.y * step;
                int hw = Math.min(s, imgW - hx);
                int hh = Math.min(s, imgH - hy);
                g2.setColor(GRID_HOVER_COLOR);
                g2.fillRect(hx, hy, hw, hh);
            }

            // Vẽ viền các ô
            Stroke strokeDashed = new BasicStroke(Math.max(1f, (float) (1.5 / zoom)),
                    BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
                    new float[]{6.0f / (float) zoom, 4.0f / (float) zoom}, 0.0f);
            Stroke strokeSolid = new BasicStroke(Math.max(1.5f, (float) (2.0 / zoom)));

            // Viền ngoài ảnh
            g2.setStroke(strokeSolid);
            g2.setColor(new Color(0x4a69bd));
            g2.drawRect(0, 0, imgW, imgH);

            // Lưới từng ô
            g2.setStroke(strokeDashed);
            g2.setColor(GRID_COLOR);

            int tileIndex = 1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int x = c * step;
                    int y = r * step;
                    int w = Math.min(s, imgW - x);
                    int h = Math.min(s, imgH - y);

                    // Vẽ khung ô
                    g2.setColor(GRID_COLOR);
                    g2.drawRect(x, y, w, h);

                    // Vẽ nhãn tọa độ trên từng ô
                    int labelFontSize = (int) Math.max(10, Math.min(24, (s * 0.08) * zoom));
                    // Scale lại font theo world coordinates
                    float fontWorldSize = (float) (labelFontSize / zoom);
                    if (fontWorldSize >= 8) {
                        g2.setFont(new Font("Segoe UI", Font.BOLD, (int) fontWorldSize));
                        String label = String.format("[%d,%d] #%d", r, c, tileIndex);
                        FontMetrics fm = g2.getFontMetrics();
                        int lx = x + 8;
                        int ly = y + fm.getAscent() + 6;

                        // Vẽ bóng nền đen cho chữ dễ đọc
                        g2.setColor(new Color(0, 0, 0, 180));
                        g2.fillRect(x + 4, y + 4, fm.stringWidth(label) + 8, fm.getHeight() + 4);

                        // Vẽ chữ vàng/trắng
                        g2.setColor(Color.WHITE);
                        g2.drawString(label, lx, ly);
                    }

                    tileIndex++;
                }
            }

            g2.setTransform(oldAt);

            // Vẽ thanh tỉ lệ Zoom ở góc dưới canvas
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(10, getHeight() - 35, 120, 25, 6, 6);
            g2.setColor(TEXT_COLOR);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.drawString(String.format("Zoom: %.0f%%", zoom * 100), 20, getHeight() - 18);
        }
    }
}
