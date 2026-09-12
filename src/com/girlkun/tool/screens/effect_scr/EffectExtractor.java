package com.girlkun.tool.screens.effect_scr;

import com.girlkun.tool.entities.EffectTemplate;
import com.girlkun.tool.main.Manager;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

/**
 * NRO Effect Data Extractor - JInternalFrame version
 * Hỗ trợ chọn linh hoạt file Data Effect và file Atlas PNG (tự động ghép cặp hoặc chỉ định thủ công)
 * Tích hợp Live Preview xem trước frame trực quan theo thời gian thực
 * và trích xuất từng frame thành file ảnh PNG riêng biệt.
 */
public class EffectExtractor extends JInternalFrame {

    private DefaultTableModel tableModel;
    private JTable itemTable;
    private JTextField outputField;
    private JTextArea logArea;
    private ButtonGroup scaleGroup;
    private JComboBox<String> atlasScaleCombo;
    private JCheckBox extractAnimCheck;
    private JCheckBox promptIfMissingAtlasCheck;

    // Preview components
    private JPanel previewPanel;
    private JLabel previewInfoLabel;
    private JSpinner frameSpinner;
    private BufferedImage currentPreviewImg = null;
    private EffectData currentPreviewData = null;
    private BufferedImage currentPreviewAtlas = null;

    private List<EffectExtractItem> items = new ArrayList<>();
    private Map<String, String> droppedPngMap = new HashMap<>();

    public EffectExtractor() {
        super("NRO Effect Data Extractor", true, true, true, true);
        setSize(1100, 750);
        // Tắt cache file tạm của ImageIO để tránh lỗi trên Windows
        try {
            ImageIO.setUseCache(false);
        } catch (Exception ignored) {}
        initUI();
    }

    private void initUI() {
        Color bgColor = new Color(30, 30, 30);
        Color itemBgColor = new Color(22, 22, 22);
        Color textColor = new Color(255, 255, 255);
        Color accentColor = new Color(0, 120, 212);
        Color greenColor = new Color(46, 139, 87);
        Color orangeColor = new Color(211, 102, 11);
        Color purpleColor = new Color(142, 68, 173);
        Color redColor = new Color(192, 57, 43);
        Color grayColor = new Color(80, 90, 100);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(mainPanel);

        // --- Header Toolbar ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        headerPanel.setBackground(bgColor);

        JButton chooseDataBtn = createStyledButton("📁 Chọn file Data", greenColor);
        chooseDataBtn.setToolTipText("Chọn một hoặc nhiều file Effect Data");
        chooseDataBtn.addActionListener(e -> addDataFiles());
        headerPanel.add(chooseDataBtn);

        JButton chooseAtlasBtn = createStyledButton("🖼️ Chọn Atlas PNG", orangeColor);
        chooseAtlasBtn.setToolTipText("Gán file Atlas PNG cho dòng đang chọn hoặc nạp danh sách Atlas");
        chooseAtlasBtn.addActionListener(e -> addAtlasFiles());
        headerPanel.add(chooseAtlasBtn);

        JButton choosePairBtn = createStyledButton("🔗 Thêm cặp (Data + Atlas)", purpleColor);
        choosePairBtn.setToolTipText("Chọn cùng lúc 1 file Data và 1 file Atlas PNG");
        choosePairBtn.addActionListener(e -> addPairDialog());
        headerPanel.add(choosePairBtn);

        JButton chooseFolderBtn = createStyledButton("📂 Chọn Thư mục", grayColor);
        chooseFolderBtn.setToolTipText("Quét toàn bộ file Data và file ảnh PNG trong thư mục");
        chooseFolderBtn.addActionListener(e -> addFolder());
        headerPanel.add(chooseFolderBtn);

        JButton deleteBtn = createStyledButton("Xóa chọn", redColor);
        deleteBtn.addActionListener(e -> removeSelectedItems());
        headerPanel.add(deleteBtn);

        JButton clearBtn = createStyledButton("Clear All", new Color(120, 30, 30));
        clearBtn.addActionListener(e -> clearAll());
        headerPanel.add(clearBtn);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // --- Center Panel (Table + Preview + Options) ---
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(bgColor);

        // Split Pane: Table (Left) + Preview (Right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.68);
        splitPane.setBackground(bgColor);
        splitPane.setBorder(null);

        // Table Section
        JPanel tableOuter = new JPanel(new BorderLayout(0, 5));
        tableOuter.setBackground(bgColor);

        JLabel tableLabel = new JLabel("Danh sách Effect (Kéo thả file Data/PNG vào bảng, nhấp đúp để chọn Atlas):");
        tableLabel.setForeground(textColor);
        tableLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableOuter.add(tableLabel, BorderLayout.NORTH);

        String[] columns = {"STT", "Tên File Data", "File Atlas PNG", "Đường dẫn Data"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        itemTable = new JTable(tableModel);
        itemTable.setBackground(itemBgColor);
        itemTable.setForeground(textColor);
        itemTable.setGridColor(new Color(45, 45, 45));
        itemTable.setRowHeight(26);
        itemTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemTable.setSelectionBackground(new Color(0, 85, 145));
        itemTable.setSelectionForeground(textColor);
        itemTable.getTableHeader().setBackground(new Color(40, 40, 40));
        itemTable.getTableHeader().setForeground(Color.WHITE);
        itemTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        itemTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        itemTable.getColumnModel().getColumn(0).setMaxWidth(55);
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        itemTable.getColumnModel().getColumn(3).setPreferredWidth(280);

        // Custom renderer for Atlas column
        itemTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String val = value != null ? value.toString() : "";
                if (val.startsWith("[Chưa có")) {
                    setForeground(new Color(255, 120, 120));
                } else if (val.startsWith("[Tự động")) {
                    setForeground(new Color(255, 215, 0));
                } else {
                    setForeground(isSelected ? Color.WHITE : new Color(130, 230, 130));
                }
                return c;
            }
        });

        // Table Selection Listener -> Update Live Preview
        itemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = itemTable.getSelectedRow();
                if (row >= 0 && row < items.size()) {
                    updatePreview(items.get(row));
                }
            }
        });

        // Double click on table row to choose/change Atlas PNG
        itemTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = itemTable.getSelectedRow();
                    if (row >= 0 && row < items.size()) {
                        chooseAtlasForItem(row);
                    }
                }
            }
        });

        // Drag and Drop
        itemTable.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    List<File> droppedFiles = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File f : droppedFiles) {
                        processDroppedFile(f);
                    }
                    refreshTable();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        itemTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeSelectedItems();
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(itemTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(55, 55, 55)));
        tableOuter.add(tableScroll, BorderLayout.CENTER);
        splitPane.setLeftComponent(tableOuter);

        // Preview Section (Right)
        JPanel previewOuter = new JPanel(new BorderLayout(0, 5));
        previewOuter.setBackground(bgColor);
        previewOuter.setPreferredSize(new Dimension(320, 0));
        previewOuter.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)), "👁️ Live Preview Frame",
                0, 0, new Font("Segoe UI", Font.BOLD, 13), Color.WHITE));

        previewInfoLabel = new JLabel("Chọn một dòng trong bảng để xem trước");
        previewInfoLabel.setForeground(new Color(180, 180, 180));
        previewInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        previewOuter.add(previewInfoLabel, BorderLayout.NORTH);

        previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                // Draw checkerboard background
                int size = 16;
                for (int x = 0; x < getWidth(); x += size) {
                    for (int y = 0; y < getHeight(); y += size) {
                        if ((x / size + y / size) % 2 == 0) {
                            g2.setColor(new Color(35, 35, 35));
                        } else {
                            g2.setColor(new Color(45, 45, 45));
                        }
                        g2.fillRect(x, y, size, size);
                    }
                }

                if (currentPreviewImg != null) {
                    int iw = currentPreviewImg.getWidth();
                    int ih = currentPreviewImg.getHeight();
                    int cx = (getWidth() - iw) / 2;
                    int cy = (getHeight() - ih) / 2;

                    // Draw crosshair center
                    g2.setColor(new Color(80, 80, 80, 100));
                    g2.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
                    g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);

                    g2.drawImage(currentPreviewImg, cx, cy, null);

                    g2.setColor(new Color(0, 180, 255, 150));
                    g2.drawRect(cx, cy, iw, ih);
                }
            }
        };
        previewPanel.setBackground(new Color(25, 25, 25));
        previewPanel.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50)));
        previewOuter.add(previewPanel, BorderLayout.CENTER);

        JPanel previewControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
        previewControlPanel.setBackground(bgColor);
        previewControlPanel.add(new JLabel("Frame:"));
        frameSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
        frameSpinner.setPreferredSize(new Dimension(55, 26));
        frameSpinner.addChangeListener(e -> refreshCurrentPreviewFrame());
        previewControlPanel.add(frameSpinner);

        JButton prevBtn = createStyledButton("◀", grayColor);
        prevBtn.setPreferredSize(new Dimension(36, 26));
        prevBtn.addActionListener(e -> {
            int cur = (Integer) frameSpinner.getValue();
            if (cur > 0) frameSpinner.setValue(cur - 1);
        });
        previewControlPanel.add(prevBtn);

        JButton nextBtn = createStyledButton("▶", grayColor);
        nextBtn.setPreferredSize(new Dimension(36, 26));
        nextBtn.addActionListener(e -> {
            int cur = (Integer) frameSpinner.getValue();
            SpinnerNumberModel model = (SpinnerNumberModel) frameSpinner.getModel();
            int max = (Integer) model.getMaximum();
            if (cur < max) frameSpinner.setValue(cur + 1);
        });
        previewControlPanel.add(nextBtn);

        previewOuter.add(previewControlPanel, BorderLayout.SOUTH);
        splitPane.setRightComponent(previewOuter);

        centerPanel.add(splitPane, BorderLayout.CENTER);

        // Options Section
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBackground(bgColor);

        // Output Path
        JPanel outputPanel = new JPanel(new BorderLayout(10, 0));
        outputPanel.setBackground(bgColor);
        outputPanel.setMaximumSize(new Dimension(2000, 36));
        JLabel outputLabel = new JLabel("Thư mục đầu ra:");
        outputLabel.setForeground(textColor);
        outputPanel.add(outputLabel, BorderLayout.WEST);

        outputField = new JTextField("output");
        outputField.setBackground(new Color(45, 45, 45));
        outputField.setForeground(textColor);
        outputField.setCaretColor(Color.WHITE);
        outputPanel.add(outputField, BorderLayout.CENTER);

        JButton browseBtn = createStyledButton("Browse", grayColor);
        browseBtn.addActionListener(e -> browseOutput());
        outputPanel.add(browseBtn, BorderLayout.EAST);
        optionsPanel.add(outputPanel);
        optionsPanel.add(Box.createVerticalStrut(8));

        // Scale & Checkboxes Row
        JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        scalePanel.setBackground(bgColor);

        JLabel atlasScaleLabel = new JLabel("Scale Atlas gốc:");
        atlasScaleLabel.setForeground(textColor);
        scalePanel.add(atlasScaleLabel);

        // Mặc định chọn x4 chuẩn NRO HD
        atlasScaleCombo = new JComboBox<>(new String[]{"x4 (NRO HD Chuẩn - Khuyên dùng)", "x1 (Gốc J2ME)", "x2", "x3", "Auto (Tự động)"});
        atlasScaleCombo.setSelectedIndex(0);
        atlasScaleCombo.addActionListener(e -> {
            int row = itemTable.getSelectedRow();
            if (row >= 0 && row < items.size()) {
                updatePreview(items.get(row));
            }
        });
        scalePanel.add(atlasScaleCombo);

        JLabel scaleLabel = new JLabel("Tỉ lệ xuất (Scale Output):");
        scaleLabel.setForeground(textColor);
        scalePanel.add(scaleLabel);

        scaleGroup = new ButtonGroup();
        for (int i = 1; i <= 4; i++) {
            JRadioButton rb = new JRadioButton("x" + i);
            rb.setActionCommand(String.valueOf(i));
            rb.setBackground(bgColor);
            rb.setForeground(new Color(220, 220, 220));
            if (i == 4) rb.setSelected(true); // Mặc định xuất x4 sắc nét hoặc x1
            scaleGroup.add(rb);
            scalePanel.add(rb);
        }

        extractAnimCheck = new JCheckBox("Xuất chuỗi Animation (arrFrame)", false);
        extractAnimCheck.setBackground(bgColor);
        extractAnimCheck.setForeground(new Color(220, 220, 220));
        scalePanel.add(extractAnimCheck);

        promptIfMissingAtlasCheck = new JCheckBox("Hỏi chọn Atlas nếu chưa có", true);
        promptIfMissingAtlasCheck.setBackground(bgColor);
        promptIfMissingAtlasCheck.setForeground(new Color(220, 220, 220));
        scalePanel.add(promptIfMissingAtlasCheck);

        optionsPanel.add(scalePanel);
        centerPanel.add(optionsPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // --- Footer Section (Buttons + Log) ---
        JPanel footerPanel = new JPanel(new BorderLayout(0, 10));
        footerPanel.setBackground(bgColor);

        // Action Buttons
        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        btnPanel.setBackground(bgColor);
        btnPanel.setPreferredSize(new Dimension(0, 42));

        JButton startBtn = createStyledButton("⚡ BẮT ĐẦU TRÍCH XUẤT", accentColor);
        startBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        startBtn.addActionListener(e -> runExtract());
        btnPanel.add(startBtn);

        JButton convertBtn = createStyledButton("📦 CONVERT SANG EFFECT TOOL", purpleColor);
        convertBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        convertBtn.setToolTipText("Đóng gói effect_data + PNG thành file Tool (" + com.girlkun.tool.utils.PathConfig.getDataPath() + "/effdata/x1/{id}) và tạo thumbnail preview (data/effect/{id}/0.png)");
        convertBtn.addActionListener(e -> runConvertToTool());
        btnPanel.add(convertBtn);

        JButton openBtn = createStyledButton("📂 Mở thư mục đầu ra", grayColor);
        openBtn.addActionListener(e -> openOutputFolder());
        btnPanel.add(openBtn);
        footerPanel.add(btnPanel, BorderLayout.NORTH);

        // Log Section
        JPanel logOuter = new JPanel(new BorderLayout(0, 4));
        logOuter.setBackground(bgColor);
        JLabel logLabel = new JLabel("Nhật ký (Log):");
        logLabel.setForeground(textColor);
        logOuter.add(logLabel, BorderLayout.NORTH);

        logArea = new JTextArea(6, 0);
        logArea.setEditable(false);
        logArea.setBackground(itemBgColor);
        logArea.setForeground(new Color(220, 220, 220));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logOuter.add(new JScrollPane(logArea), BorderLayout.CENTER);
        footerPanel.add(logOuter, BorderLayout.CENTER);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    private int getSelectedAtlasScale() {
        int idx = atlasScaleCombo.getSelectedIndex();
        switch (idx) {
            case 0: return 4; // x4
            case 1: return 1; // x1
            case 2: return 2; // x2
            case 3: return 3; // x3
            default: return 0; // Auto
        }
    }

    private void updatePreview(EffectExtractItem item) {
        if (item == null || item.dataFile == null || !item.dataFile.exists()) {
            currentPreviewImg = null;
            currentPreviewData = null;
            currentPreviewAtlas = null;
            previewInfoLabel.setText("Chưa chọn file hợp lệ");
            previewPanel.repaint();
            return;
        }

        try {
            currentPreviewData = parseEffectData(item.dataFile);
            if (currentPreviewData == null) {
                previewInfoLabel.setText("Không thể đọc Data Effect");
                currentPreviewImg = null;
                previewPanel.repaint();
                return;
            }

            BufferedImage atlas = null;
            if (currentPreviewData.pngData != null && currentPreviewData.pngData.length > 0) {
                atlas = ImageIO.read(new ByteArrayInputStream(currentPreviewData.pngData));
            }
            if (atlas == null) {
                File imgFile = item.atlasFile;
                if (imgFile == null || !imgFile.exists()) {
                    imgFile = findMatchingAtlasImage(item.dataFile, stripExtension(item.dataFile.getName()));
                }
                if (imgFile != null && imgFile.exists()) {
                    atlas = ImageIO.read(imgFile);
                }
            }

            currentPreviewAtlas = atlas;
            if (atlas == null) {
                previewInfoLabel.setText("Chưa tìm thấy ảnh Atlas PNG");
                currentPreviewImg = null;
                previewPanel.repaint();
                return;
            }

            int frameCount = currentPreviewData.frames.size();
            if (frameCount > 0) {
                int curVal = (Integer) frameSpinner.getValue();
                if (curVal >= frameCount) curVal = 0;
                frameSpinner.setModel(new SpinnerNumberModel(curVal, 0, frameCount - 1, 1));
            }

            refreshCurrentPreviewFrame();
        } catch (Exception e) {
            previewInfoLabel.setText("Lỗi nạp preview: " + e.getMessage());
            currentPreviewImg = null;
            previewPanel.repaint();
        }
    }

    private void refreshCurrentPreviewFrame() {
        if (currentPreviewData == null || currentPreviewAtlas == null || currentPreviewData.frames.isEmpty()) {
            currentPreviewImg = null;
            previewPanel.repaint();
            return;
        }

        int frameIdx = (Integer) frameSpinner.getValue();
        if (frameIdx < 0 || frameIdx >= currentPreviewData.frames.size()) {
            frameIdx = 0;
        }

        int atlasScale = getSelectedAtlasScale();
        if (atlasScale == 0) {
            // Auto
            atlasScale = detectActualScale(currentPreviewData, currentPreviewAtlas);
        }

        Map<Integer, SpriteInfoLocal> spriteMap = new HashMap<>();
        for (SpriteInfoLocal info : currentPreviewData.imgInfo) {
            spriteMap.put(info.id, info);
        }

        List<PartLocal> parts = currentPreviewData.frames.get(frameIdx);
        currentPreviewImg = renderFrameImage(parts, spriteMap, currentPreviewAtlas, atlasScale, atlasScale);

        if (currentPreviewImg != null) {
            previewInfoLabel.setText("Frame " + frameIdx + "/" + (currentPreviewData.frames.size() - 1)
                    + " (" + currentPreviewImg.getWidth() + "x" + currentPreviewImg.getHeight() + " px, Scale Atlas: x" + atlasScale + ")");
        } else {
            previewInfoLabel.setText("Frame " + frameIdx + " (Trống / 0 part)");
        }
        previewPanel.repaint();
    }

    private int detectActualScale(EffectData data, BufferedImage atlas) {
        if (atlas == null) return 4;
        // Kiểm tra đường kính và độ phân giải của Atlas
        if (atlas.getWidth() >= 2048 || atlas.getHeight() >= 2048) {
            return 4;
        }
        if (data != null && data.maxXMeta > 0) {
            double ratio = (double) atlas.getWidth() / data.maxXMeta;
            int scale = (int) Math.round(ratio);
            if (scale >= 1 && scale <= 4) return scale;
        }
        return 4; // Mặc định chuẩn HD NRO
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        if (bg != null) {
            btn.setBackground(bg);
            btn.setForeground(Color.WHITE);
            btn.setOpaque(true);
            btn.setBorderPainted(false);
        }
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return btn;
    }

    private void addDataFiles() {
        FileDialog dialog = new FileDialog((Frame) null, "Chọn file Effect Data", FileDialog.LOAD);
        dialog.setMultipleMode(true);
        dialog.setFile("*");
        dialog.setVisible(true);

        File[] selectedFiles = dialog.getFiles();
        if (selectedFiles != null && selectedFiles.length > 0) {
            for (File f : selectedFiles) {
                processDroppedFile(f);
            }
            refreshTable();
        }
    }

    private void addAtlasFiles() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < items.size()) {
            chooseAtlasForItem(selectedRow);
            return;
        }

        FileDialog dialog = new FileDialog((Frame) null, "Chọn file Atlas PNG", FileDialog.LOAD);
        dialog.setMultipleMode(true);
        dialog.setFile("*.png");
        dialog.setVisible(true);

        File[] selectedFiles = dialog.getFiles();
        if (selectedFiles != null && selectedFiles.length > 0) {
            for (File f : selectedFiles) {
                registerAtlasFile(f);
            }
            // Tự động gán lại cho các item chưa có Atlas
            for (EffectExtractItem item : items) {
                if (item.atlasFile == null) {
                    item.atlasFile = findMatchingAtlasImage(item.dataFile, stripExtension(item.dataFile.getName()));
                }
            }
            refreshTable();
            if (itemTable.getSelectedRow() >= 0) {
                updatePreview(items.get(itemTable.getSelectedRow()));
            }
        }
    }

    private void chooseAtlasForItem(int rowIndex) {
        EffectExtractItem item = items.get(rowIndex);
        FileDialog dialog = new FileDialog((Frame) null, "Chọn file Atlas PNG cho: " + item.dataFile.getName(), FileDialog.LOAD);
        dialog.setFile("*.png");
        dialog.setVisible(true);

        String file = dialog.getFile();
        if (file != null) {
            File atlasFile = new File(dialog.getDirectory(), file);
            item.atlasFile = atlasFile;
            registerAtlasFile(atlasFile);
            refreshTable();
            updatePreview(item);
            log("Đã gán Atlas [" + atlasFile.getName() + "] cho " + item.dataFile.getName());
        }
    }

    private void addPairDialog() {
        JTextField dataField = new JTextField(22);
        JTextField atlasField = new JTextField(22);

        JButton btnData = new JButton("...");
        btnData.addActionListener(e -> {
            FileDialog fd = new FileDialog((Frame) null, "Chọn file Data", FileDialog.LOAD);
            fd.setVisible(true);
            if (fd.getFile() != null) {
                dataField.setText(new File(fd.getDirectory(), fd.getFile()).getAbsolutePath());
            }
        });

        JButton btnAtlas = new JButton("...");
        btnAtlas.addActionListener(e -> {
            FileDialog fd = new FileDialog((Frame) null, "Chọn file Atlas PNG", FileDialog.LOAD);
            fd.setFile("*.png");
            fd.setVisible(true);
            if (fd.getFile() != null) {
                atlasField.setText(new File(fd.getDirectory(), fd.getFile()).getAbsolutePath());
            }
        });

        JPanel panel = new JPanel(new GridLayout(2, 3, 5, 5));
        panel.add(new JLabel("File Data:"));
        panel.add(dataField);
        panel.add(btnData);

        panel.add(new JLabel("File Atlas PNG:"));
        panel.add(atlasField);
        panel.add(btnAtlas);

        int result = JOptionPane.showConfirmDialog(this, panel, "Thêm cặp Data & Atlas", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String dPath = dataField.getText().trim();
            String aPath = atlasField.getText().trim();

            if (!dPath.isEmpty()) {
                File dFile = new File(dPath);
                File aFile = !aPath.isEmpty() ? new File(aPath) : null;
                if (dFile.exists()) {
                    EffectExtractItem item = new EffectExtractItem(dFile, aFile);
                    if (aFile == null) {
                        item.atlasFile = findMatchingAtlasImage(dFile, stripExtension(dFile.getName()));
                    }
                    items.add(item);
                    refreshTable();
                    itemTable.setRowSelectionInterval(items.size() - 1, items.size() - 1);
                } else {
                    JOptionPane.showMessageDialog(this, "File Data không tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void addFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Chọn thư mục chứa Data & Atlas Effect");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            processDroppedFile(dir);
            refreshTable();
        }
    }

    private void processDroppedFile(File f) {
        if (f.isDirectory()) {
            File[] list = f.listFiles();
            if (list != null) {
                // Quét trước các file PNG
                for (File sub : list) {
                    if (sub.isFile() && sub.getName().toLowerCase().endsWith(".png")) {
                        registerAtlasFile(sub);
                    }
                }
                // Sau đó quét các file data hoặc thư mục con
                for (File sub : list) {
                    processDroppedFile(sub);
                }
            }
            return;
        }

        String name = f.getName().toLowerCase();
        if (name.endsWith(".png")) {
            registerAtlasFile(f);
            // Thử gán cho các item hiện tại nếu khớp
            for (EffectExtractItem item : items) {
                if (item.atlasFile == null) {
                    item.atlasFile = findMatchingAtlasImage(item.dataFile, stripExtension(item.dataFile.getName()));
                }
            }
            return;
        }

        // Kiểm tra trùng lặp data file
        for (EffectExtractItem item : items) {
            if (item.dataFile.getAbsolutePath().equalsIgnoreCase(f.getAbsolutePath())) {
                return;
            }
        }

        EffectExtractItem newItem = new EffectExtractItem(f, null);
        newItem.atlasFile = findMatchingAtlasImage(f, stripExtension(f.getName()));
        items.add(newItem);
    }

    private void registerAtlasFile(File f) {
        String fullName = f.getName();
        String noExt = stripExtension(fullName);
        droppedPngMap.put(fullName.toLowerCase(), f.getAbsolutePath());
        droppedPngMap.put(noExt.toLowerCase(), f.getAbsolutePath());

        String numId = noExt.replaceAll("\\D+", "");
        if (!numId.isEmpty()) {
            droppedPngMap.put(numId, f.getAbsolutePath());
            droppedPngMap.put("imgeffect_" + numId, f.getAbsolutePath());
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (int i = 0; i < items.size(); i++) {
            EffectExtractItem item = items.get(i);
            String atlasDisplay;
            if (item.atlasFile != null && item.atlasFile.exists()) {
                atlasDisplay = item.atlasFile.getName();
            } else {
                File auto = findMatchingAtlasImage(item.dataFile, stripExtension(item.dataFile.getName()));
                if (auto != null && auto.exists()) {
                    item.atlasFile = auto;
                    atlasDisplay = "[Tự động] " + auto.getName();
                } else {
                    atlasDisplay = "[Chưa có - Nhấp đúp để chọn]";
                }
            }

            tableModel.addRow(new Object[]{
                    (i + 1),
                    item.dataFile.getName(),
                    atlasDisplay,
                    item.dataFile.getAbsolutePath()
            });
        }
        if (!items.isEmpty() && itemTable.getSelectedRow() == -1) {
            itemTable.setRowSelectionInterval(0, 0);
        }
    }

    private void removeSelectedItems() {
        int[] selectedRows = itemTable.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            int row = selectedRows[i];
            if (row >= 0 && row < items.size()) {
                items.remove(row);
            }
        }
        refreshTable();
        if (items.isEmpty()) {
            currentPreviewImg = null;
            previewPanel.repaint();
        }
    }

    private void clearAll() {
        items.clear();
        droppedPngMap.clear();
        currentPreviewImg = null;
        currentPreviewData = null;
        currentPreviewAtlas = null;
        previewPanel.repaint();
        refreshTable();
    }

    private void browseOutput() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Chọn thư mục đầu ra");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputField.setText(chooser.getSelectedFile().getPath());
        }
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void openOutputFolder() {
        String path = outputField.getText().trim();
        File folder = new File(path.isEmpty() ? "output" : path).getAbsoluteFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        try {
            Desktop.getDesktop().open(folder);
        } catch (Exception e) {
            log("Không thể mở thư mục: " + e.getMessage());
        }
    }

    private void runExtract() {
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Danh sách đang trống! Vui lòng chọn file Data Effect.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String outBase = outputField.getText().trim();
        if (outBase.isEmpty()) outBase = "output";

        int tScale = Integer.parseInt(scaleGroup.getSelection().getActionCommand());
        int configuredAtlasScale = getSelectedAtlasScale();
        boolean extractAnim = extractAnimCheck.isSelected();
        boolean promptIfMissing = promptIfMissingAtlasCheck.isSelected();

        // Kiểm tra trước các file chưa có Atlas nếu bật chế độ hỏi chọn
        if (promptIfMissing) {
            for (int i = 0; i < items.size(); i++) {
                EffectExtractItem item = items.get(i);
                if (item.atlasFile == null || !item.atlasFile.exists()) {
                    File found = findMatchingAtlasImage(item.dataFile, stripExtension(item.dataFile.getName()));
                    if (found != null && found.exists()) {
                        item.atlasFile = found;
                    } else {
                        byte[] preview = tryReadEmbeddedPng(item.dataFile);
                        if (preview == null) {
                            int opt = JOptionPane.showConfirmDialog(this,
                                    "Chưa tìm thấy ảnh Atlas PNG cho [" + item.dataFile.getName() + "].\nBạn có muốn chọn file ảnh PNG ngay bây giờ không?",
                                    "Thiếu file ảnh Atlas", JOptionPane.YES_NO_CANCEL_OPTION);
                            if (opt == JOptionPane.YES_OPTION) {
                                chooseAtlasForItem(i);
                            } else if (opt == JOptionPane.CANCEL_OPTION) {
                                return;
                            }
                        }
                    }
                }
            }
        }

        logArea.setText("");
        log("================ BẮT ĐẦU TRÍCH XUẤT EFFECT ================");
        log("Số lượng file: " + items.size());
        log("Scale mục tiêu (Output): x" + tScale);
        log("Chế độ Scale Atlas gốc: " + atlasScaleCombo.getSelectedItem());
        log("Thư mục đầu ra: " + outBase);

        String finalOutBase = outBase;
        new Thread(() -> {
            int successCount = 0;
            int errorCount = 0;

            for (EffectExtractItem item : items) {
                try {
                    boolean ok = extractSingleEffect(item, finalOutBase, tScale, configuredAtlasScale, extractAnim);
                    if (ok) successCount++;
                    else errorCount++;
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> log("  [!] LỖI NGOẠI LỆ trên " + item.dataFile.getName() + ": " + e.getMessage()));
                    errorCount++;
                }
            }

            final int finalSuccessCount = successCount;
            final int finalErrorCount = errorCount;
            SwingUtilities.invokeLater(() -> {
                log("================ HOÀN TẤT TRÍCH XUẤT ================");
                log("Kết quả: " + finalSuccessCount + " thành công, " + finalErrorCount + " lỗi.");
                if (finalErrorCount == 0) {
                    JOptionPane.showMessageDialog(this, "Đã trích xuất thành công toàn bộ Effect!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Hoàn tất với " + finalErrorCount + " lỗi. Xem log chi tiết.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                }
                openOutputFolder();
            });
        }).start();
    }

    private boolean extractSingleEffect(EffectExtractItem item, String outputBase, int targetScale, int configuredAtlasScale, boolean extractAnim) throws Exception {
        File dataFile = item.dataFile;
        String fileName = dataFile.getName();
        String nameNoExt = stripExtension(fileName);

        SwingUtilities.invokeLater(() -> log("\n-> Đang xử lý: " + fileName));

        EffectData data = parseEffectData(dataFile);
        if (data == null) {
            SwingUtilities.invokeLater(() -> log("  [!] Không thể đọc định dạng dữ liệu: " + fileName));
            return false;
        }

        BufferedImage atlas = null;
        if (data.pngData != null && data.pngData.length > 0) {
            atlas = ImageIO.read(new ByteArrayInputStream(data.pngData));
            if (atlas != null) {
                SwingUtilities.invokeLater(() -> log("  -> Sử dụng ảnh Atlas nhúng sẵn trong file .bin"));
            }
        }

        if (atlas == null) {
            File imgFile = item.atlasFile;
            if (imgFile == null || !imgFile.exists()) {
                imgFile = findMatchingAtlasImage(dataFile, nameNoExt);
            }

            if (imgFile != null && imgFile.exists()) {
                atlas = ImageIO.read(imgFile);
                final String imgName = imgFile.getName();
                SwingUtilities.invokeLater(() -> log("  -> Đã nạp Atlas: " + imgName));
            }
        }

        if (atlas == null) {
            SwingUtilities.invokeLater(() -> {
                log("  [!] LỖI: Không tìm thấy file ảnh Atlas PNG cho " + fileName);
                log("      Gợi ý: Hãy nhấp đúp vào dòng '" + fileName + "' trong bảng để chọn file ảnh PNG tương ứng.");
            });
            return false;
        }

        // Xác định scale thực tế của Atlas
        int actualScale = configuredAtlasScale;
        if (actualScale == 0) {
            actualScale = detectActualScale(data, atlas);
        }

        final int detectedScale = actualScale;
        final BufferedImage finalAtlas = atlas;
        final int fSize = data.frames.size();
        SwingUtilities.invokeLater(() -> {
            log("  Atlas kích thước: " + finalAtlas.getWidth() + "x" + finalAtlas.getHeight() + " (Actual Scale: x" + detectedScale + ")");
            log("  Số Frame: " + fSize);
        });

        Map<Integer, SpriteInfoLocal> spriteMap = new HashMap<>();
        for (SpriteInfoLocal info : data.imgInfo) {
            spriteMap.put(info.id, info);
        }

        File effectDir = new File(outputBase, nameNoExt);
        if (effectDir.exists() && effectDir.isFile()) {
            // Tránh xung đột nếu đã có 1 file nhị phân trùng tên trong thư mục output
            effectDir = new File(outputBase, nameNoExt + "_frames");
        }
        if (!effectDir.exists()) {
            effectDir.mkdirs();
        }

        List<BufferedImage> renderedFrames = new ArrayList<>();
        int savedFrames = 0;

        for (int i = 0; i < data.frames.size(); i++) {
            List<PartLocal> parts = data.frames.get(i);
            BufferedImage frameImg = renderFrameImage(parts, spriteMap, atlas, actualScale, targetScale);
            renderedFrames.add(frameImg);

            if (frameImg != null) {
                File outFile = new File(effectDir, nameNoExt + "_" + i + ".png");
                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(outFile))) {
                    ImageIO.write(frameImg, "png", fos);
                    savedFrames++;
                }
            }
        }

        // Xuất chuỗi Animation nếu có
        if (extractAnim && data.arrFrame != null && !data.arrFrame.isEmpty()) {
            File animDir = new File(effectDir, "anim");
            if (animDir.exists() && animDir.isFile()) {
                animDir = new File(effectDir, "anim_frames");
            }
            if (!animDir.exists()) {
                animDir.mkdirs();
            }

            for (int k = 0; k < data.arrFrame.size(); k++) {
                int fIndex = data.arrFrame.get(k);
                if (fIndex >= 0 && fIndex < renderedFrames.size()) {
                    BufferedImage fImg = renderedFrames.get(fIndex);
                    if (fImg != null) {
                        File animFile = new File(animDir, nameNoExt + "_anim_" + String.format("%03d", k) + "_f" + fIndex + ".png");
                        File parent = animFile.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(animFile))) {
                            ImageIO.write(fImg, "png", fos);
                        }
                    }
                }
            }
            final int animCount = data.arrFrame.size();
            SwingUtilities.invokeLater(() -> log("  -> Đã xuất " + animCount + " frame animation vào thư mục 'anim'"));
        }

        final int finalSaved = savedFrames;
        final File finalEffectDir = effectDir;
        SwingUtilities.invokeLater(() -> log("  -> Xuất thành công " + finalSaved + " frame PNG vào: " + finalEffectDir.getPath()));
        return true;
    }

    private BufferedImage renderFrameImage(List<PartLocal> parts, Map<Integer, SpriteInfoLocal> spriteMap,
                                           BufferedImage atlas, int atlasScale, int targetScale) {
        if (parts == null || parts.isEmpty()) return null;

        List<Rectangle> rects = new ArrayList<>();
        for (PartLocal p : parts) {
            SpriteInfoLocal sInfo = spriteMap.get(p.imgId);
            if (sInfo == null) continue;
            int w = sInfo.w * atlasScale;
            int h = sInfo.h * atlasScale;
            int dx = p.dx * atlasScale;
            int dy = p.dy * atlasScale;
            rects.add(new Rectangle(dx, dy, w, h));
        }

        if (rects.isEmpty()) return null;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (Rectangle r : rects) {
            if (r.x < minX) minX = r.x;
            if (r.y < minY) minY = r.y;
            if (r.x + r.width > maxX) maxX = r.x + r.width;
            if (r.y + r.height > maxY) maxY = r.y + r.height;
        }

        int fw = maxX - minX;
        int fh = maxY - minY;
        if (fw <= 0 || fh <= 0) return null;

        BufferedImage frameImg = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = frameImg.createGraphics();

        for (PartLocal p : parts) {
            SpriteInfoLocal sInfo = spriteMap.get(p.imgId);
            if (sInfo == null) continue;

            int sx = sInfo.x * atlasScale;
            int sy = sInfo.y * atlasScale;
            int sw = sInfo.w * atlasScale;
            int sh = sInfo.h * atlasScale;
            int dx = p.dx * atlasScale;
            int dy = p.dy * atlasScale;

            if (sx < 0) sx = 0;
            if (sy < 0) sy = 0;
            if (sx >= atlas.getWidth() || sy >= atlas.getHeight()) continue;
            if (sx + sw > atlas.getWidth()) sw = atlas.getWidth() - sx;
            if (sy + sh > atlas.getHeight()) sh = atlas.getHeight() - sy;

            if (sw > 0 && sh > 0) {
                BufferedImage spriteImg = atlas.getSubimage(sx, sy, sw, sh);
                g.drawImage(spriteImg, dx - minX, dy - minY, null);
            }
        }
        g.dispose();

        if (targetScale != atlasScale) {
            double ratio = (double) targetScale / atlasScale;
            int targetW = (int) Math.round(fw * ratio);
            int targetH = (int) Math.round(fh * ratio);
            if (targetW > 0 && targetH > 0) {
                BufferedImage scaledImg = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = scaledImg.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.drawImage(frameImg, 0, 0, targetW, targetH, null);
                g2.dispose();
                frameImg = scaledImg;
            }
        }

        return frameImg;
    }

    private File findMatchingAtlasImage(File dataFile, String nameNoExt) {
        String lowerName = nameNoExt.toLowerCase();
        String numId = nameNoExt.replaceAll("\\D+", "");

        // 1. Kiểm tra từ map dropped/registered PNG
        if (droppedPngMap.containsKey(lowerName)) {
            return new File(droppedPngMap.get(lowerName));
        }
        if (droppedPngMap.containsKey("imgeffect_" + lowerName)) {
            return new File(droppedPngMap.get("imgeffect_" + lowerName));
        }
        if (!numId.isEmpty()) {
            if (droppedPngMap.containsKey(numId)) {
                return new File(droppedPngMap.get(numId));
            }
            if (droppedPngMap.containsKey("imgeffect_" + numId)) {
                return new File(droppedPngMap.get("imgeffect_" + numId));
            }
        }

        File parentDir = dataFile.getParentFile();
        if (parentDir == null) parentDir = new File(".");

        List<String> candidates = new ArrayList<>();
        // Tên trực tiếp
        candidates.add(nameNoExt + ".png");
        candidates.add(lowerName + ".png");
        candidates.add("ImgEffect_" + nameNoExt + ".png");
        candidates.add("imgeffect_" + lowerName + ".png");
        candidates.add("DataEffect_" + nameNoExt + ".png");

        if (!numId.isEmpty()) {
            candidates.add("ImgEffect_" + numId + ".png");
            candidates.add("imgeffect_" + numId + ".png");
            candidates.add("ImgEffect" + numId + ".png");
            candidates.add(numId + ".png");
            candidates.add("eff_" + numId + ".png");
            candidates.add("effect_" + numId + ".png");

            // Subdirectories (x4, x3, x2, x1, images)
            for (String sub : new String[]{"x4", "x3", "x2", "x1", "images", "img", "res"}) {
                candidates.add(sub + "/ImgEffect_" + numId + ".png");
                candidates.add(sub + "/imgeffect_" + numId + ".png");
                candidates.add(sub + "/" + numId + ".png");
            }
        }

        for (String c : candidates) {
            File f = new File(parentDir, c);
            if (f.exists()) return f;
        }

        // Tìm trong thư mục cha (nếu data nằm trong thư mục con)
        File grandParent = parentDir.getParentFile();
        if (grandParent != null && grandParent.exists()) {
            for (String c : candidates) {
                File f = new File(grandParent, c);
                if (f.exists()) return f;
            }
        }

        // Tìm lướt các file PNG trong thư mục
        File[] allPngs = parentDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (allPngs != null) {
            for (File p : allPngs) {
                String pName = stripExtension(p.getName()).toLowerCase();
                if (pName.contains(lowerName) || (!numId.isEmpty() && pName.contains(numId))) {
                    return p;
                }
            }
            if (allPngs.length == 1) {
                return allPngs[0];
            }
        }

        return null;
    }

    private byte[] tryReadEmbeddedPng(File file) {
        try {
            EffectData d = parseEffectData(file);
            return d != null ? d.pngData : null;
        } catch (Exception e) {
            return null;
        }
    }

    private EffectData parseEffectData(File file) {
        try {
            byte[] fileData = Files.readAllBytes(file.toPath());
            if (fileData.length < 4) return null;

            // Thử Format 1: Packaged Bin
            EffectData d1 = tryParsePackagedBin(fileData);
            if (d1 != null) return d1;

            // Thử Format 2: Standard NRO DataEffect
            EffectData d2 = tryParseStandardDataEffect(fileData);
            if (d2 != null) return d2;

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private EffectData tryParsePackagedBin(byte[] fileData) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(fileData));
            int tFlag = dis.readByte();
            int dLen = dis.readInt();

            if (dLen <= 0 || dLen > fileData.length) return null;

            int nImg = dis.readUnsignedByte();
            List<SpriteInfoLocal> imgInfo = new ArrayList<>();
            int maxX = 0;

            for (int i = 0; i < nImg; i++) {
                int id = dis.readByte() & 0xFF;
                int x, y;
                if (tFlag == 1) {
                    x = dis.readUnsignedByte();
                    y = dis.readUnsignedByte();
                } else if (tFlag == 2) {
                    x = dis.readShort();
                    y = dis.readShort();
                } else {
                    x = dis.readUnsignedByte();
                    y = dis.readUnsignedByte();
                }
                int w = dis.readUnsignedByte();
                int h = dis.readUnsignedByte();

                imgInfo.add(new SpriteInfoLocal(id, x, y, w, h));
                if (x + w > maxX) maxX = x + w;
            }

            int nFrames = dis.readShort();
            List<List<PartLocal>> frames = new ArrayList<>();
            for (int i = 0; i < nFrames; i++) {
                int nParts = dis.readUnsignedByte();
                List<PartLocal> parts = new ArrayList<>();
                for (int j = 0; j < nParts; j++) {
                    int dx = dis.readShort();
                    int dy = dis.readShort();
                    int imgId = dis.readByte() & 0xFF;
                    parts.add(new PartLocal(dx, dy, imgId));
                }
                frames.add(parts);
            }

            int pngStart = 5 + dLen;
            byte[] pngData = null;
            if (pngStart + 4 <= fileData.length) {
                dis = new DataInputStream(new ByteArrayInputStream(fileData, pngStart, fileData.length - pngStart));
                int pngLen = dis.readInt();
                if (pngStart + 4 + pngLen <= fileData.length) {
                    pngData = new byte[pngLen];
                    dis.readFully(pngData);
                }
            }

            EffectData ed = new EffectData();
            ed.imgInfo = imgInfo;
            ed.frames = frames;
            ed.pngData = pngData;
            ed.maxXMeta = maxX;
            return ed;
        } catch (Exception e) {
            return null;
        }
    }

    private EffectData tryParseStandardDataEffect(byte[] fileData) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(fileData));
            int sCnt = dis.readUnsignedByte();
            if (sCnt <= 0 || sCnt > 255) return null;

            List<SpriteInfoLocal> imgInfo = new ArrayList<>();
            int maxX = 0;

            for (int i = 0; i < sCnt; i++) {
                int id = dis.readByte() & 0xFF;
                int x = dis.readUnsignedByte();
                int y = dis.readUnsignedByte();
                int w = dis.readUnsignedByte();
                int h = dis.readUnsignedByte();
                imgInfo.add(new SpriteInfoLocal(id, x, y, w, h));
                if (x + w > maxX) maxX = x + w;
            }

            int fCnt = dis.readShort();
            if (fCnt < 0 || fCnt > 5000) return null;

            List<List<PartLocal>> frames = new ArrayList<>();
            for (int i = 0; i < fCnt; i++) {
                int pCnt = dis.readUnsignedByte();
                List<PartLocal> parts = new ArrayList<>();
                for (int j = 0; j < pCnt; j++) {
                    int dx = dis.readShort();
                    int dy = dis.readShort();
                    int imgId = dis.readByte() & 0xFF;
                    parts.add(new PartLocal(dx, dy, imgId));
                }
                frames.add(parts);
            }

            List<Integer> arrFrame = new ArrayList<>();
            if (dis.available() >= 2) {
                int aCnt = dis.readShort();
                if (aCnt > 0 && aCnt <= 10000 && dis.available() >= aCnt * 2) {
                    for (int i = 0; i < aCnt; i++) {
                        arrFrame.add((int) dis.readShort());
                    }
                }
            }

            EffectData ed = new EffectData();
            ed.imgInfo = imgInfo;
            ed.frames = frames;
            ed.arrFrame = arrFrame;
            ed.maxXMeta = maxX;
            return ed;
        } catch (Exception e) {
            return null;
        }
    }

    private void runConvertToTool() {
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Danh sách đang trống! Vui lòng chọn file Data Effect.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int opt = JOptionPane.showConfirmDialog(this,
                "Chức năng này sẽ đóng gói các Effect trong danh sách thành:\n"
                + "1. File dữ liệu: " + com.girlkun.tool.utils.PathConfig.getDataPath() + "/effdata/x1/{id}\n"
                + "2. Ảnh Thumbnail: data/effect/{id}/0.png\n\n"
                + "Và nạp trực tiếp vào bộ nhớ Tool để bạn có thể sử dụng ngay trong Draw Map.\nBạn có muốn tiếp tục?",
                "Xác nhận Convert sang Effect Tool", JOptionPane.YES_NO_OPTION);
        if (opt != JOptionPane.YES_OPTION) {
            return;
        }

        int configuredAtlasScale = getSelectedAtlasScale();

        logArea.setText("");
        log("================ BẮT ĐẦU CONVERT SANG EFFECT TOOL ================");
        log("Số lượng file: " + items.size());
        log("Chế độ Scale Atlas gốc: " + atlasScaleCombo.getSelectedItem());

        new Thread(() -> {
            int successCount = 0;
            int errorCount = 0;
            List<Integer> convertedIds = new ArrayList<>();

            for (EffectExtractItem item : items) {
                try {
                    int id = convertSingleEffectToTool(item, configuredAtlasScale);
                    if (id >= 0) {
                        successCount++;
                        convertedIds.add(id);
                    } else {
                        errorCount++;
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> log("  [!] LỖI NGOẠI LỆ trên " + item.dataFile.getName() + ": " + e.getMessage()));
                    errorCount++;
                }
            }

            // Tải lại các Effect vừa convert vào Manager
            for (int id : convertedIds) {
                try {
                    EffectTemplate eff = Manager.gI().readEff(id);
                    if (eff != null && eff.getSizeFrame() > 0) {
                        Manager.gI().getEffectTemplates().removeIf(e -> e.getId() == id);
                        Manager.gI().getEffectTemplates().add(eff);
                    }
                } catch (Exception ignored) {}
            }
            try {
                Manager.gI().getEffectTemplates().sort(Comparator.comparingInt(EffectTemplate::getId));
            } catch (Exception ignored) {}

            final int finalSuccessCount = successCount;
            final int finalErrorCount = errorCount;
            SwingUtilities.invokeLater(() -> {
                log("================ HOÀN TẤT CONVERT ================");
                log("Kết quả: " + finalSuccessCount + " thành công, " + finalErrorCount + " lỗi.");
                if (finalErrorCount == 0) {
                    JOptionPane.showMessageDialog(this,
                            "Đã convert và nạp thành công " + finalSuccessCount + " Effect vào Tool!\n"
                            + "Dữ liệu đã được tạo tại:\n- " + com.girlkun.tool.utils.PathConfig.getDataPath() + "/effdata/x1/\n- data/effect/{id}/0.png\n\n"
                            + "Bạn có thể mở Effect Table trong Draw Map để chọn và vẽ effect ngay.",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Hoàn tất convert với " + finalErrorCount + " lỗi. Xem log chi tiết.",
                            "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                }
            });
        }).start();
    }

    private int convertSingleEffectToTool(EffectExtractItem item, int configuredAtlasScale) throws Exception {
        File dataFile = item.dataFile;
        String fileName = dataFile.getName();
        String nameNoExt = stripExtension(fileName);

        SwingUtilities.invokeLater(() -> log("\n-> Đang convert: " + fileName));

        // 1. Xác định ID Effect
        String numIdStr = nameNoExt.replaceAll("\\D+", "");
        int id = -1;
        if (!numIdStr.isEmpty()) {
            try {
                id = Integer.parseInt(numIdStr);
            } catch (Exception ignored) {}
        }
        if (id < 0) {
            final String fName = fileName;
            final int[] promptId = new int[]{-1};
            try {
                SwingUtilities.invokeAndWait(() -> {
                    String input = JOptionPane.showInputDialog(this, "Nhập ID Effect dạng số cho file: " + fName, "Nhập ID Effect", JOptionPane.QUESTION_MESSAGE);
                    if (input != null && !input.trim().isEmpty()) {
                        try {
                            promptId[0] = Integer.parseInt(input.trim());
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}
            id = promptId[0];
            if (id < 0) {
                SwingUtilities.invokeLater(() -> log("  [!] Bỏ qua do không có ID Effect hợp lệ."));
                return -1;
            }
        }

        // 2. Phân tích dữ liệu Effect
        EffectData data = parseEffectData(dataFile);
        if (data == null) {
            SwingUtilities.invokeLater(() -> log("  [!] Không thể đọc dữ liệu: " + fileName));
            return -1;
        }

        // 3. Nạp Atlas PNG
        BufferedImage atlas = null;
        if (data.pngData != null && data.pngData.length > 0) {
            atlas = ImageIO.read(new ByteArrayInputStream(data.pngData));
        }
        File imgFile = null;
        if (atlas == null) {
            imgFile = item.atlasFile;
            if (imgFile == null || !imgFile.exists()) {
                imgFile = findMatchingAtlasImage(dataFile, nameNoExt);
            }
            if (imgFile != null && imgFile.exists()) {
                atlas = ImageIO.read(imgFile);
            }
        }
        if (atlas == null) {
            SwingUtilities.invokeLater(() -> log("  [!] LỖI: Không tìm thấy file ảnh Atlas PNG cho " + fileName));
            return -1;
        }

        // 4. Xác định scale của Atlas
        int actualScale = configuredAtlasScale;
        if (actualScale == 0) {
            actualScale = detectActualScale(data, atlas);
        }

        // 5. Chuẩn bị byte[] dữ liệu DataEffect
        byte[] rawDataBytes = getRawEffectDataBytes(dataFile);
        if (rawDataBytes == null || rawDataBytes.length == 0) {
            SwingUtilities.invokeLater(() -> log("  [!] Không lấy được byte dữ liệu dataEffect."));
            return -1;
        }

        // 6. Chuẩn bị byte[] hình ảnh PNG (Chuẩn hóa về x1 cho thư mục x1/)
        byte[] imgBytes;
        if (actualScale > 1) {
            int w1 = atlas.getWidth() / actualScale;
            int h1 = atlas.getHeight() / actualScale;
            if (w1 <= 0) w1 = 1;
            if (h1 <= 0) h1 = 1;
            BufferedImage imgX1 = new BufferedImage(w1, h1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gX1 = imgX1.createGraphics();
            gX1.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gX1.drawImage(atlas, 0, 0, w1, h1, null);
            gX1.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(imgX1, "png", baos);
            imgBytes = baos.toByteArray();
        } else {
            if (imgFile != null && imgFile.exists()) {
                imgBytes = Files.readAllBytes(imgFile.toPath());
            } else if (data.pngData != null) {
                imgBytes = data.pngData;
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(atlas, "png", baos);
                imgBytes = baos.toByteArray();
            }
        }

        // 7. Đóng gói file data/effdata/x1/{id}
        File effDataDir = new File(com.girlkun.tool.utils.PathConfig.getDataPath() + "/effdata/x1");
        if (!effDataDir.exists()) {
            effDataDir.mkdirs();
        }
        File targetFile = new File(effDataDir, String.valueOf(id));
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile)))) {
            dos.writeShort(id);
            dos.writeInt(rawDataBytes.length);
            dos.write(rawDataBytes);
            dos.writeInt(imgBytes.length);
            dos.write(imgBytes);
            dos.flush();
        }

        // 8. Tạo ảnh Thumbnail preview: data/effect/{id}/0.png
        File previewDir = new File("data/effect/" + id);
        if (!previewDir.exists()) {
            previewDir.mkdirs();
        }
        Map<Integer, SpriteInfoLocal> spriteMap = new HashMap<>();
        for (SpriteInfoLocal s : data.imgInfo) {
            spriteMap.put(s.id, s);
        }
        List<PartLocal> frame0Parts = data.frames.isEmpty() ? null : data.frames.get(0);
        BufferedImage frame0 = null;
        if (frame0Parts != null) {
            frame0 = renderFrameImage(frame0Parts, spriteMap, atlas, actualScale, actualScale);
        }
        if (frame0 == null && !data.imgInfo.isEmpty()) {
            SpriteInfoLocal s0 = data.imgInfo.get(0);
            int sx = s0.x * actualScale;
            int sy = s0.y * actualScale;
            int sw = s0.w * actualScale;
            int sh = s0.h * actualScale;
            if (sx >= 0 && sy >= 0 && sx + sw <= atlas.getWidth() && sy + sh <= atlas.getHeight() && sw > 0 && sh > 0) {
                frame0 = atlas.getSubimage(sx, sy, sw, sh);
            }
        }
        if (frame0 == null) {
            frame0 = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        }
        File previewFile = new File(previewDir, "0.png");
        try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(previewFile))) {
            ImageIO.write(frame0, "png", fos);
        }

        final int finalId = id;
        SwingUtilities.invokeLater(() -> {
            log("  [✓] Đã tạo file Tool: " + com.girlkun.tool.utils.PathConfig.getDataPath() + "/effdata/x1/" + finalId);
            log("  [✓] Đã tạo thumbnail: data/effect/" + finalId + "/0.png");
        });
        return id;
    }

    private byte[] getRawEffectDataBytes(File file) {
        try {
            byte[] fileData = Files.readAllBytes(file.toPath());
            if (fileData.length < 5) return fileData;

            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(fileData));
            int tFlag = dis.readByte();
            int dLen = dis.readInt();
            if (dLen > 0 && dLen < fileData.length && (tFlag == 0 || tFlag == 1 || tFlag == 2)) {
                return Arrays.copyOfRange(fileData, 5, 5 + dLen);
            }
            return fileData;
        } catch (Exception e) {
            return null;
        }
    }

    private String stripExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        return dotIndex == -1 ? name : name.substring(0, dotIndex);
    }

    static class EffectExtractItem {
        File dataFile;
        File atlasFile;

        EffectExtractItem(File dataFile, File atlasFile) {
            this.dataFile = dataFile;
            this.atlasFile = atlasFile;
        }
    }

    static class EffectData {
        List<SpriteInfoLocal> imgInfo;
        List<List<PartLocal>> frames;
        List<Integer> arrFrame;
        byte[] pngData;
        int maxXMeta;
    }

    static class SpriteInfoLocal {
        int id, x, y, w, h;

        SpriteInfoLocal(int id, int x, int y, int w, int h) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    static class PartLocal {
        int dx, dy, imgId;

        PartLocal(int dx, int dy, int imgId) {
            this.dx = dx;
            this.dy = dy;
            this.imgId = imgId;
        }
    }
}
