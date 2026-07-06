package com.girlkun.tool.screens.draw_map_scr;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * TilesetEditorDialog - GUI để tạo/chỉnh sửa tileset
 * Chuyển đổi từ tileset_editor.py sang Java
 */
public class TilesetEditorDialog extends JFrame {

    // Collision Types
    private static final Map<Integer, String> COLLISION_TYPES = new LinkedHashMap<>();
    static {
        COLLISION_TYPES.put(2, "Sàn (Top)");
        COLLISION_TYPES.put(8192, "Trần (Bottom)");
        COLLISION_TYPES.put(4, "Trái (Left)");
        COLLISION_TYPES.put(8, "Phải (Right)");
        COLLISION_TYPES.put(512, "Nước (Water)");
    }

    // Output paths (relative)
    private static final String TILE_SET_INFO_PATH = "data/data/map/tile_set_info";
    private static final String RES_BASE_PATH = "data/data/res";
    private static final String TILE_PATH = "data/tile";

    // Cache thư mục cuối cùng đã chọn
    private static String lastMapDir = null;

    // App state
    private BufferedImage currentImage; // Ảnh gốc (map)
    private double zoomPercent = 100; // 10% - 500%
    private double imageScale = 1.0; // Scale riêng cho ảnh (Ctrl + scroll)
    private int imageOffsetX = 0; // Offset X của ảnh (chuột phải kéo)
    private int imageOffsetY = 0; // Offset Y của ảnh
    private int canvasOffsetX = 0; // Offset X chung cho cả ảnh và grid (chuột giữa/trái giữ)
    private int canvasOffsetY = 0; // Offset Y chung
    private boolean showGrid = true;

    // Biến hỗ trợ kéo chuột phải (di chuyển ảnh)
    private Point dragStartPoint = null;
    private int dragStartOffsetX = 0;
    private int dragStartOffsetY = 0;

    // Biến hỗ trợ kéo chuột giữa/trái giữ (di chuyển cả canvas)
    private Point panStartPoint = null;
    private int panStartOffsetX = 0;
    private int panStartOffsetY = 0;
    private boolean isPanning = false;

    private Set<Point> selectedCells = new HashSet<>();
    private Map<Point, Set<Integer>> cellCollisions = new HashMap<>();
    private List<TileItem> exportItems = new ArrayList<>();
    private int selectedListIndex = -1; // Index của item đang chọn trong list

    // Data manager
    private TilesetData dataManager;

    // UI Components
    private JPanel canvasPanel;
    private JScrollPane canvasScrollPane;
    private JLabel listTitleLabel;
    private Map<Integer, JCheckBox> collisionCheckboxes = new HashMap<>();
    private JSlider zoomSlider;
    private JLabel zoomValueLabel;
    private JButton btnGrid;
    private JScrollPane listScrollPane;
    private JPanel innerListPanel;

    private BufferedImage canvasBuffer;

    // Icons
    private ImageIcon iconFolder;
    private ImageIcon iconEdit;
    private ImageIcon iconCancel;
    private ImageIcon iconTrash;
    private ImageIcon iconDelete;

    public TilesetEditorDialog() {
        super("NRO Tileset Editor");
        this.dataManager = new TilesetData();
        this.dataManager.load(TILE_SET_INFO_PATH);
        loadIcons();
        initUI();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);
    }

    private void loadIcons() {
        String dataDir = "data/tool_icons/";
        iconFolder = loadIcon(dataDir + "folder.png", 16, 16);
        iconEdit = loadIcon(dataDir + "edit.png", 16, 16);
        iconCancel = loadIcon(dataDir + "cancel.png", 16, 16);
        iconTrash = loadIcon(dataDir + "trash.png", 14, 14);
        iconDelete = loadIcon(dataDir + "delete.png", 12, 12);
    }

    private ImageIcon loadIcon(String path, int width, int height) {
        try {
            File file = new File(path);
            if (file.exists()) {
                BufferedImage img = ImageIO.read(file);
                Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception e) {
            System.err.println("Could not load icon: " + path);
        }
        return null;
    }

    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(45, 45, 48));

        // TOP TOOLBAR
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(30, 30, 32));
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton btnLoadMap = createStyledButton("1. Chọn Map", new Color(66, 133, 244), Color.WHITE, iconFolder);
        btnLoadMap.addActionListener(e -> loadMapImage());
        toolbar.add(btnLoadMap);
        toolbar.add(Box.createHorizontalStrut(10));

        JButton btnLoadTileset = createStyledButton("2. Load Tileset", new Color(102, 51, 153), Color.WHITE, iconEdit);
        btnLoadTileset.addActionListener(e -> loadExistingTileset());
        toolbar.add(btnLoadTileset);
        toolbar.add(Box.createHorizontalStrut(10));

        btnGrid = createStyledButton("Grid: ON", new Color(80, 80, 85), Color.WHITE, null);
        btnGrid.addActionListener(e -> toggleGrid());
        toolbar.add(btnGrid);
        toolbar.add(Box.createHorizontalStrut(10));

        JButton btnClearSelection = createStyledButton("Xóa Chọn", new Color(220, 53, 69), Color.WHITE, iconCancel);
        btnClearSelection.addActionListener(e -> clearSelection());
        toolbar.add(btnClearSelection);
        toolbar.add(Box.createHorizontalStrut(10));

        JButton btnResetImage = createStyledButton("Reset Img", new Color(108, 117, 125), Color.WHITE, null);
        btnResetImage.setToolTipText("Đưa ảnh về vị trí và kích thước gốc");
        btnResetImage.addActionListener(e -> resetImageTransform());
        toolbar.add(btnResetImage);
        toolbar.add(Box.createHorizontalStrut(20));

        // Zoom panel với style đẹp
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(55, 55, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        zoomPanel.setOpaque(false);
        zoomPanel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JLabel zoomLabel = new JLabel("Zoom:");
        zoomLabel.setForeground(new Color(180, 180, 180));
        zoomLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        zoomPanel.add(zoomLabel);

        zoomSlider = new JSlider(10, 500, 100);
        zoomSlider.setMajorTickSpacing(100);
        zoomSlider.setMinorTickSpacing(10);
        zoomSlider.setSnapToTicks(false);
        zoomSlider.setPaintTicks(false);
        zoomSlider.setPaintLabels(false);
        zoomSlider.setBackground(new Color(55, 55, 60));
        zoomSlider.setForeground(new Color(150, 150, 150));
        zoomSlider.setPreferredSize(new Dimension(120, 20));
        zoomSlider.addChangeListener(e -> {
            zoomPercent = zoomSlider.getValue();
            zoomValueLabel.setText((int) zoomPercent + "%");
            refreshCanvas();
        });
        zoomPanel.add(zoomSlider);

        zoomValueLabel = new JLabel("100%");
        zoomValueLabel.setForeground(new Color(100, 200, 255));
        zoomValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        zoomValueLabel.setPreferredSize(new Dimension(40, 20));
        zoomPanel.add(zoomValueLabel);

        toolbar.add(zoomPanel);

        toolbar.add(Box.createHorizontalGlue());

        JButton btnGuide = createStyledButton("Guide", new Color(255, 193, 7), Color.BLACK, null);
        btnGuide.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnGuide.addActionListener(e -> showGuide());
        toolbar.add(btnGuide);
        toolbar.add(Box.createHorizontalStrut(10));

        JButton btnExport = createStyledButton("3. XUẤT FILE", new Color(40, 167, 69), Color.WHITE, null);
        btnExport.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnExport.addActionListener(e -> doExport());
        toolbar.add(btnExport);

        add(toolbar, BorderLayout.NORTH);

        // MAIN SPLIT
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);

        // LEFT: Canvas
        canvasPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (canvasBuffer != null) {
                    g.drawImage(canvasBuffer, 0, 0, null);
                }
            }
        };
        canvasPanel.setBackground(Color.DARK_GRAY);
        canvasPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && !isPanning) {
                    onCanvasClick(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Chuột phải - di chuyển ảnh riêng
                    dragStartPoint = e.getPoint();
                    dragStartOffsetX = imageOffsetX;
                    dragStartOffsetY = imageOffsetY;
                    canvasPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else if (SwingUtilities.isMiddleMouseButton(e) ||
                        (SwingUtilities.isLeftMouseButton(e) && e.isShiftDown())) {
                    // Chuột giữa hoặc Shift+Trái - di chuyển cả ảnh và grid
                    isPanning = true;
                    panStartPoint = e.getPoint();
                    panStartOffsetX = canvasOffsetX;
                    panStartOffsetY = canvasOffsetY;
                    canvasPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    dragStartPoint = null;
                    canvasPanel.setCursor(Cursor.getDefaultCursor());
                } else if (SwingUtilities.isMiddleMouseButton(e) ||
                        (SwingUtilities.isLeftMouseButton(e) && isPanning)) {
                    panStartPoint = null;
                    isPanning = false;
                    canvasPanel.setCursor(Cursor.getDefaultCursor());
                }
            }
        });
        canvasPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isPanning && panStartPoint != null) {
                    // Di chuyển cả ảnh và grid
                    int dx = e.getX() - panStartPoint.x;
                    int dy = e.getY() - panStartPoint.y;
                    canvasOffsetX = panStartOffsetX + dx;
                    canvasOffsetY = panStartOffsetY + dy;
                    refreshCanvas();
                } else if (SwingUtilities.isLeftMouseButton(e) && !isPanning) {
                    onCanvasDrag(e);
                } else if (SwingUtilities.isRightMouseButton(e) && dragStartPoint != null) {
                    // Chuột phải - di chuyển ảnh riêng
                    int dx = e.getX() - dragStartPoint.x;
                    int dy = e.getY() - dragStartPoint.y;
                    imageOffsetX = dragStartOffsetX + dx;
                    imageOffsetY = dragStartOffsetY + dy;
                    refreshCanvas();
                }
            }
        });

        // Ctrl + Mouse Wheel để scale ảnh
        canvasPanel.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                // Ctrl + scroll: thay đổi kích thước ảnh
                int rotation = e.getWheelRotation();
                if (rotation < 0) {
                    // Scroll up - tăng scale
                    imageScale = Math.min(5.0, imageScale + 0.1);
                } else {
                    // Scroll down - giảm scale
                    imageScale = Math.max(0.1, imageScale - 0.1);
                }
                refreshCanvas();
                e.consume();
            }
        });

        canvasScrollPane = new JScrollPane(canvasPanel);
        splitPane.setLeftComponent(canvasScrollPane);

        // RIGHT: Controls & List
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(350, 0));

        // Collision Checkboxes
        JPanel colPanel = new JPanel();
        colPanel.setLayout(new BoxLayout(colPanel, BoxLayout.Y_AXIS));
        colPanel.setBorder(BorderFactory.createTitledBorder("Cấu hình Va Chạm (Cho ô đang chọn)"));

        for (Map.Entry<Integer, String> entry : COLLISION_TYPES.entrySet()) {
            JCheckBox cb = new JCheckBox(entry.getValue());
            int typeId = entry.getKey();
            cb.addActionListener(e -> onCheckCollision(typeId, cb.isSelected()));
            collisionCheckboxes.put(typeId, cb);
            colPanel.add(cb);
        }

        JButton btnAddToList = createStyledButton("THÊM VÀO LIST", new Color(0, 123, 255), Color.WHITE, null);
        btnAddToList.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAddToList.addActionListener(e -> addToList());
        colPanel.add(Box.createVerticalStrut(15));
        colPanel.add(btnAddToList);
        colPanel.add(Box.createVerticalStrut(5));

        rightPanel.add(colPanel, BorderLayout.NORTH);

        // List Panel
        JPanel listContainer = new JPanel(new BorderLayout());
        listContainer.setBackground(new Color(50, 50, 55));

        listTitleLabel = new JLabel("Danh sách chờ xuất (0 items)");
        listTitleLabel.setForeground(Color.WHITE);
        listTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        listTitleLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 5));

        JButton btnClearList = createStyledButton("Xóa Hết", new Color(108, 117, 125), Color.WHITE, iconTrash);
        btnClearList.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnClearList.addActionListener(e -> clearList());

        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setBackground(new Color(50, 50, 55));
        listHeader.add(listTitleLabel, BorderLayout.WEST);
        listHeader.add(btnClearList, BorderLayout.EAST);
        listHeader.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        innerListPanel = new JPanel();
        innerListPanel.setLayout(new BoxLayout(innerListPanel, BoxLayout.Y_AXIS));
        innerListPanel.setBackground(new Color(58, 58, 58));

        listScrollPane = new JScrollPane(innerListPanel);
        listScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        listScrollPane.getViewport().setBackground(new Color(58, 58, 58));
        listScrollPane.setBorder(BorderFactory.createEmptyBorder());

        listContainer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 85)),
                "Danh sách chờ xuất",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Segoe UI", Font.BOLD, 12),
                Color.WHITE));
        listContainer.add(listHeader, BorderLayout.NORTH);
        listContainer.add(listScrollPane, BorderLayout.CENTER);

        rightPanel.add(listContainer, BorderLayout.CENTER);

        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private void loadMapImage() {
        // Sử dụng Windows Explorer native (FileDialog)
        FileDialog dialog = new FileDialog(this, "Chọn ảnh Map gốc", FileDialog.LOAD);

        // Set thư mục: ưu tiên thư mục cuối cùng đã chọn, nếu chưa có thì dùng
        // "data/bg"
        dialog.setDirectory(lastMapDir != null ? lastMapDir : "data/bg");

        // Filter file ảnh
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg") || lower.endsWith(".gif");
        });

        dialog.setVisible(true);

        String fileName = dialog.getFile();
        if (fileName != null) {
            // Lưu lại thư mục đã chọn để lần sau mở nhanh hơn
            lastMapDir = dialog.getDirectory();
            File selected = new File(dialog.getDirectory(), fileName);

            try {
                currentImage = ImageIO.read(selected);
                selectedCells.clear();
                cellCollisions.clear();
                refreshCanvas();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadExistingTileset() {
        FileDialog dialog = new FileDialog(this, "Chọn File Tileset muốn sửa", FileDialog.LOAD);
        dialog.setDirectory(TILE_PATH);
        dialog.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".png"));
        dialog.setVisible(true);

        String fileName = dialog.getFile();
        if (fileName != null) {
            try {
                File file = new File(dialog.getDirectory(), fileName);
                String filename = file.getName();
                String nameNoExt = filename.replace(".png", "");

                if (!nameNoExt.matches("\\d+")) {
                    JOptionPane.showMessageDialog(this, "Tên file phải là số (ID của tileset).", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int tsId = Integer.parseInt(nameNoExt);

                // Find data
                Map<Integer, List<Integer>> tInfo = null;
                for (TilesetInfo ts : dataManager.getTilesets()) {
                    if (ts.getId() == tsId) {
                        tInfo = ts.getTypes();
                        break;
                    }
                }

                // Load tileset image (strip format)
                BufferedImage strip = ImageIO.read(file);
                int h = strip.getHeight();

                if (strip.getWidth() != 24 || h % 24 != 0) {
                    int result = JOptionPane.showConfirmDialog(this,
                            "Kích thước ảnh có vẻ không chuẩn (Rộng != 24). Vẫn tiếp tục?",
                            "Warning", JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                int nTiles = h / 24;
                int cols = 11;
                int rows = (nTiles + cols - 1) / cols;
                int gridW = cols * 24;
                int gridH = rows * 24;

                BufferedImage gridImg = new BufferedImage(gridW, gridH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = gridImg.createGraphics();

                // Clear previous
                int result = JOptionPane.showConfirmDialog(this,
                        "Xóa danh sách hiện tại để load mới? (No = Gộp thêm)",
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    clearList();
                    cellCollisions.clear();
                }

                currentImage = gridImg;
                selectedCells.clear();

                // Populate
                for (int i = 0; i < nTiles; i++) {
                    BufferedImage sub = strip.getSubimage(0, i * 24, 24, 24);

                    int c = i % cols;
                    int r = i / cols;
                    g.drawImage(sub, c * 24, r * 24, null);

                    Set<Integer> types = new HashSet<>();
                    int tileIndex = i + 1; // 1-based
                    if (tInfo != null && tInfo.containsKey(tileIndex)) {
                        types.addAll(tInfo.get(tileIndex));
                    }

                    addItemToListData(sub, types, new Point(c, r));

                    if (!types.isEmpty()) {
                        cellCollisions.put(new Point(c, r), new HashSet<>(types));
                    }
                }

                g.dispose();
                updateListUI();
                refreshCanvas();

                JOptionPane.showMessageDialog(this,
                        "Đã load " + nTiles + " tiles lên Canvas (Dạng Grid 11x" + rows
                                + ").\nCác thông tin va chạm đã được nạp lại.",
                        "OK", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void toggleGrid() {
        showGrid = !showGrid;
        btnGrid.setText("Grid: " + (showGrid ? "ON" : "OFF"));
        refreshCanvas();
    }

    private JButton createStyledButton(String text, Color bgColor, Color fgColor, ImageIcon icon) {
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
        if (icon != null) {
            btn.setIcon(icon);
            btn.setIconTextGap(6);
        }
        btn.setForeground(fgColor);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    private void clearSelection() {
        selectedCells.clear();
        refreshCanvas();
        updateCollisionUI();
    }

    // Helper method để lấy zoom scale từ phần trăm
    private double getZoomScale() {
        return zoomPercent / 100.0;
    }

    private Point getCell(MouseEvent e) {
        double scale = getZoomScale();
        int sz = (int) (24 * scale);
        // Trừ canvasOffset để tính vị trí cell chính xác
        int cx = (e.getX() - canvasOffsetX) / sz;
        int cy = (e.getY() - canvasOffsetY) / sz;
        return new Point(cx, cy);
    }

    private void resetImageTransform() {
        imageScale = 1.0;
        imageOffsetX = 0;
        imageOffsetY = 0;
        canvasOffsetX = 0;
        canvasOffsetY = 0;
        refreshCanvas();
    }

    private void onCanvasClick(MouseEvent e) {
        if (currentImage == null)
            return;

        selectedListIndex = -1; // Bỏ chọn item trong list khi click canvas
        selectedCells.clear();
        selectedCells.add(getCell(e));
        refreshCanvas();
        updateCollisionUI();
        updateListUI(); // Refresh để bỏ highlight
    }

    private void onCanvasDrag(MouseEvent e) {
        if (currentImage == null)
            return;

        Point cell = getCell(e);
        if (!selectedCells.contains(cell) || selectedCells.size() > 1) {
            selectedCells.clear();
            selectedCells.add(cell);
            refreshCanvas();
            updateCollisionUI();
        }
    }

    private void updateCollisionUI() {
        // Nếu có item đang chọn trong list, dùng types của item đó
        if (selectedListIndex >= 0 && selectedListIndex < exportItems.size()) {
            TileItem item = exportItems.get(selectedListIndex);
            for (Map.Entry<Integer, JCheckBox> entry : collisionCheckboxes.entrySet()) {
                entry.getValue().setSelected(item.types.contains(entry.getKey()));
            }
            return;
        }

        // Ngược lại dùng cellCollisions từ canvas
        if (selectedCells.isEmpty()) {
            for (JCheckBox cb : collisionCheckboxes.values()) {
                cb.setSelected(false);
            }
            return;
        }

        Point cell = selectedCells.iterator().next();
        Set<Integer> types = cellCollisions.getOrDefault(cell, new HashSet<>());

        for (Map.Entry<Integer, JCheckBox> entry : collisionCheckboxes.entrySet()) {
            entry.getValue().setSelected(types.contains(entry.getKey()));
        }
    }

    private void onCheckCollision(int typeId, boolean isOn) {
        // Nếu đang chọn item trong list, cập nhật types của item đó
        if (selectedListIndex >= 0 && selectedListIndex < exportItems.size()) {
            TileItem item = exportItems.get(selectedListIndex);
            if (isOn) {
                item.types.add(typeId);
            } else {
                item.types.remove(typeId);
            }
            // Cập nhật cellCollisions nếu item có coords hợp lệ
            if (item.coords.x != -1) {
                cellCollisions.computeIfAbsent(item.coords, k -> new HashSet<>());
                if (isOn) {
                    cellCollisions.get(item.coords).add(typeId);
                } else {
                    cellCollisions.get(item.coords).remove(typeId);
                }
            }
            updateListUI();
            refreshCanvas();
            return;
        }

        // Ngược lại cập nhật cellCollisions từ canvas selection
        if (selectedCells.isEmpty())
            return;

        for (Point cell : selectedCells) {
            cellCollisions.computeIfAbsent(cell, k -> new HashSet<>());
            if (isOn) {
                cellCollisions.get(cell).add(typeId);
            } else {
                cellCollisions.get(cell).remove(typeId);
            }
        }
        refreshCanvas();
    }

    private void refreshCanvas() {
        if (currentImage == null)
            return;

        double scale = getZoomScale();
        int w = currentImage.getWidth();
        int h = currentImage.getHeight();

        // Kích thước canvas dựa trên Grid (zoom chuẩn)
        int zw = (int) (w * scale);
        int zh = (int) (h * scale);

        if (zw < 1)
            zw = 1;
        if (zh < 1)
            zh = 1;

        // Kích thước ảnh với imageScale riêng
        double totalImageScale = scale * imageScale;
        int imgW = (int) (w * totalImageScale);
        int imgH = (int) (h * totalImageScale);

        // Canvas size phải đủ lớn để chứa mọi thứ đã di chuyển
        int canvasW = Math.max(zw + Math.abs(canvasOffsetX), imgW + Math.abs(imageOffsetX + canvasOffsetX)) + 100;
        int canvasH = Math.max(zh + Math.abs(canvasOffsetY), imgH + Math.abs(imageOffsetY + canvasOffsetY)) + 100;

        canvasBuffer = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvasBuffer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Vẽ nền tối
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, canvasW, canvasH);

        // Áp dụng canvasOffset cho transform chung
        int baseX = canvasOffsetX;
        int baseY = canvasOffsetY;

        // Vẽ ảnh với offset riêng + offset chung
        g.drawImage(currentImage, baseX + imageOffsetX, baseY + imageOffsetY, imgW, imgH, null);

        int sz = (int) (24 * scale);
        if (sz < 1)
            sz = 1;

        // Grid - áp dụng canvasOffset
        if (showGrid) {
            g.setColor(new Color(85, 85, 85, 180));
            for (int x = 0; x < zw; x += sz) {
                g.drawLine(baseX + x, baseY, baseX + x, baseY + zh);
            }
            for (int y = 0; y < zh; y += sz) {
                g.drawLine(baseX, baseY + y, baseX + zw, baseY + y);
            }
        }

        // Added markers - theo Grid với canvasOffset
        g.setColor(new Color(68, 68, 68));
        for (TileItem item : exportItems) {
            if (item.coords.x != -1) {
                int ax = baseX + item.coords.x * sz;
                int ay = baseY + item.coords.y * sz;
                g.drawLine(ax, ay, ax + sz, ay + sz);
                g.drawLine(ax, ay + sz, ax + sz, ay);
            }
        }

        // Collisions - theo Grid với canvasOffset
        for (Map.Entry<Point, Set<Integer>> entry : cellCollisions.entrySet()) {
            Point cell = entry.getKey();
            Set<Integer> types = entry.getValue();
            int px = baseX + cell.x * sz;
            int py = baseY + cell.y * sz;

            float strokeWidth = Math.max(1, (float) (2 * scale));
            g.setStroke(new BasicStroke(strokeWidth));
            int offset = (int) Math.max(1, 5 * scale);
            int offset2 = (int) Math.max(1, 2 * scale);

            if (types.contains(2)) {
                g.setColor(Color.RED);
                g.drawLine(px, py + offset, px + sz, py + offset);
            }
            if (types.contains(4)) {
                g.setColor(Color.GREEN);
                g.drawLine(px + offset2, py, px + offset2, py + sz);
            }
            if (types.contains(8)) {
                g.setColor(Color.ORANGE);
                g.drawLine(px + sz - offset2, py, px + sz - offset2, py + sz);
            }
            if (types.contains(8192)) {
                g.setColor(Color.BLUE);
                g.drawLine(px, py + sz - offset2, px + sz, py + sz - offset2);
            }
        }

        // Selection - theo Grid với canvasOffset
        g.setColor(Color.CYAN);
        g.setStroke(new BasicStroke(2));
        for (Point cell : selectedCells) {
            int sx = baseX + cell.x * sz;
            int sy = baseY + cell.y * sz;
            g.drawRect(sx, sy, sz - 1, sz - 1);
        }

        g.dispose();

        canvasPanel.setPreferredSize(new Dimension(canvasW, canvasH));
        canvasPanel.revalidate();
        canvasPanel.repaint();
    }

    private void addToList() {
        if (selectedCells.isEmpty() || currentImage == null)
            return;

        List<Point> cells = new ArrayList<>(selectedCells);
        cells.sort((p1, p2) -> {
            if (p1.y != p2.y)
                return p1.y - p2.y;
            return p1.x - p2.x;
        });

        double scale = getZoomScale();
        double totalImageScale = scale * imageScale;
        int sz = (int) (24 * scale);
        if (sz < 1)
            sz = 1;

        for (Point cell : cells) {
            // Vị trí ô grid trên canvas (đã tính canvasOffset)
            int gridX = canvasOffsetX + cell.x * sz;
            int gridY = canvasOffsetY + cell.y * sz;

            // Vị trí ảnh trên canvas
            int imgX = canvasOffsetX + imageOffsetX;
            int imgY = canvasOffsetY + imageOffsetY;

            // Tính vị trí tương đối của grid cell so với ảnh (trong không gian canvas)
            int relX = gridX - imgX;
            int relY = gridY - imgY;

            // Chuyển về tọa độ trong ảnh gốc
            double srcX = relX / totalImageScale;
            double srcY = relY / totalImageScale;
            double srcW = sz / totalImageScale;
            double srcH = sz / totalImageScale;

            // Tạo tile 24x24 từ vùng tương ứng trong ảnh gốc
            BufferedImage tile = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = tile.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(new Color(0, 0, 0, 0)); // Transparent background
            g.fillRect(0, 0, 24, 24);

            // Tính toán vùng cắt trong ảnh gốc (clamp to bounds)
            int iSrcX = (int) Math.max(0, srcX);
            int iSrcY = (int) Math.max(0, srcY);
            int iSrcX2 = (int) Math.min(currentImage.getWidth(), srcX + srcW);
            int iSrcY2 = (int) Math.min(currentImage.getHeight(), srcY + srcH);

            if (iSrcX2 > iSrcX && iSrcY2 > iSrcY) {
                // Map từ source sang destination
                double destX = (iSrcX - srcX) / srcW * 24;
                double destY = (iSrcY - srcY) / srcH * 24;
                double destW = (iSrcX2 - iSrcX) / srcW * 24;
                double destH = (iSrcY2 - iSrcY) / srcH * 24;

                g.drawImage(currentImage,
                        (int) destX, (int) destY, (int) (destX + destW), (int) (destY + destH),
                        iSrcX, iSrcY, iSrcX2, iSrcY2,
                        null);
            }
            g.dispose();

            Set<Integer> types = new HashSet<>(cellCollisions.getOrDefault(cell, new HashSet<>()));
            addItemToListData(tile, types, new Point(cell));
        }

        selectedCells.clear();
        refreshCanvas();
        updateListUI();
    }

    private void addItemToListData(BufferedImage pilImg, Set<Integer> types, Point coords) {
        exportItems.add(new TileItem(pilImg, types, coords));
    }

    private void updateListUI() {
        innerListPanel.removeAll();

        listTitleLabel.setText("Danh sách chờ xuất (" + exportItems.size() + " items)");

        for (int i = 0; i < exportItems.size(); i++) {
            TileItem item = exportItems.get(i);
            JPanel row = createListRow(i, item);
            innerListPanel.add(row);
        }

        innerListPanel.revalidate();
        innerListPanel.repaint();
    }

    private JPanel createListRow(int index, TileItem item) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(70, 70, 75)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        // Highlight nếu đang được chọn
        if (index == selectedListIndex) {
            row.setBackground(new Color(0, 100, 180));
        } else {
            row.setBackground(new Color(65, 65, 70));
        }

        // Click handler để chọn item
        final int idx = index;
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onListItemClick(idx);
            }
        });

        // Thumbnail với viền
        JPanel thumbContainer = new JPanel(new BorderLayout());
        thumbContainer.setBackground(new Color(45, 45, 48));
        thumbContainer.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 105), 1));
        JLabel thumbLabel = new JLabel(new ImageIcon(item.image));
        thumbLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        thumbContainer.add(thumbLabel, BorderLayout.CENTER);

        JPanel westPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        westPanel.setBackground(row.getBackground());
        westPanel.setOpaque(false);
        westPanel.add(thumbContainer);
        row.add(westPanel, BorderLayout.WEST);

        // Info
        String coordStr = item.coords.x != -1 ? "Map(" + item.coords.x + "," + item.coords.y + ")" : "Imported";
        String typeStr = item.types.isEmpty() ? "None" : item.types.toString();
        JLabel infoLabel = new JLabel("<html><span style='color:#FFFFFF;font-weight:bold;'>#" + (index + 1)
                + "</span> <span style='color:#AAAAAA;'>" + coordStr + "</span><br><span style='color:#88CCFF;'>Types: "
                + typeStr + "</span></html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        row.add(infoLabel, BorderLayout.CENTER);

        // Delete button
        JButton btnDelete = createStyledButton("", new Color(220, 53, 69), Color.WHITE, iconDelete);
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDelete.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        btnDelete.setPreferredSize(new Dimension(32, 28));
        btnDelete.addActionListener(e -> removeItem(idx));
        row.add(btnDelete, BorderLayout.EAST);

        return row;
    }

    private void onListItemClick(int index) {
        if (index < 0 || index >= exportItems.size())
            return;

        selectedListIndex = index;
        TileItem item = exportItems.get(index);

        // Chọn ô tương ứng trên canvas (nếu có coords hợp lệ)
        selectedCells.clear();
        if (item.coords.x != -1) {
            selectedCells.add(new Point(item.coords));

            // Scroll canvas đến vị trí ô được chọn
            int sz = (int) (24 * getZoomScale());
            Rectangle rect = new Rectangle(item.coords.x * sz, item.coords.y * sz, sz, sz);
            canvasPanel.scrollRectToVisible(rect);
        }

        refreshCanvas();
        updateCollisionUI();
        updateListUI(); // Refresh để highlight row
    }

    private void removeItem(int index) {
        if (index >= 0 && index < exportItems.size()) {
            exportItems.remove(index);
            updateListUI();
            refreshCanvas();
        }
    }

    private void clearList() {
        exportItems.clear();
        updateListUI();
        refreshCanvas();
    }

    private void doExport() {
        if (exportItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Danh sách trống!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Suggest next ID
        int suggest = 1;
        if (!dataManager.getTilesets().isEmpty()) {
            suggest = dataManager.getTilesets().stream().mapToInt(TilesetInfo::getId).max().orElse(0) + 1;
        }

        String input = JOptionPane.showInputDialog(this, "Nhập ID Tileset:", suggest);
        if (input == null || input.trim().isEmpty())
            return;

        int tsId;
        try {
            tsId = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ID không hợp lệ!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int n = exportItems.size();
        if (n > 255) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Tileset có " + n + " ô (max 255). Các ô > 255 sẽ không lưu được collision. Tiếp tục?",
                    "Warning", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION)
                return;
        }

        try {
            // Prepare types data
            Map<Integer, List<Integer>> newTypes = new HashMap<>();
            for (int i = 0; i < exportItems.size(); i++) {
                TileItem item = exportItems.get(i);
                if (!item.types.isEmpty()) {
                    newTypes.put(i + 1, new ArrayList<>(item.types));
                }
            }

            // Ensure output directories exist
            ensureDir(RES_BASE_PATH);
            ensureDir(TILE_PATH);
            ensureDir(new File(TILE_SET_INFO_PATH).getParent());

            // Save images for all zoom levels
            for (int zoomLevel = 1; zoomLevel <= 4; zoomLevel++) {
                String zoomDir = RES_BASE_PATH + "/x" + zoomLevel;
                ensureDir(zoomDir);

                int targetSize = 24 * zoomLevel;

                for (int i = 0; i < exportItems.size(); i++) {
                    BufferedImage orgImg = exportItems.get(i).image;

                    BufferedImage img;
                    if (zoomLevel > 1) {
                        img = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = img.createGraphics();
                        g.drawImage(orgImg, 0, 0, targetSize, targetSize, null);
                        g.dispose();
                    } else {
                        img = orgImg;
                    }

                    // Save without extension (format PNG)
                    String filename = tsId + "$" + (i + 1);
                    File saveFile = new File(zoomDir, filename);
                    ImageIO.write(img, "PNG", saveFile);

                    // Save with .png extension
                    String filenamePng = tsId + "$" + (i + 1) + ".png";
                    File saveFilePng = new File(zoomDir, filenamePng);
                    ImageIO.write(img, "PNG", saveFilePng);
                }
            }

            // Save tile strip (x1) to data/tile/<tsId>
            BufferedImage stripX1 = new BufferedImage(24, n * 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = stripX1.createGraphics();
            for (int i = 0; i < exportItems.size(); i++) {
                g.drawImage(exportItems.get(i).image, 0, i * 24, null);
            }
            g.dispose();

            File stripFile = new File(TILE_PATH, String.valueOf(tsId));
            ImageIO.write(stripX1, "PNG", stripFile);

            // Save tile_set_info
            dataManager.updateTileset(tsId, newTypes);
            dataManager.save(TILE_SET_INFO_PATH);

            // Note: Restart tool hoặc reload map để cập nhật tile_set_info

            String msg = "Đã lưu Tileset " + tsId + " thành công!\n" +
                    "- Images: " + RES_BASE_PATH + "/x[1-4]/" + tsId + "$*.png\n" +
                    "- Strip: " + TILE_PATH + "/" + tsId + "\n" +
                    "- Info: " + TILE_SET_INFO_PATH;

            JOptionPane.showMessageDialog(this, msg, "OK", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void showGuide() {
        String guide = """
                ╔══════════════════════════════════════════════════════════════════╗
                ║              NRO TILESET EDITOR - HƯỚNG DẪN SỬ DỤNG              ║
                ╠══════════════════════════════════════════════════════════════════╣
                ║                                                                  ║
                ║   CÁC BƯỚC SỬ DỤNG:                                              ║
                ║  ──────────────────────────────────────────────────────────────  ║
                ║  1. "Chọn Map" - Chọn ảnh background map từ thư mục data/bg      ║
                ║     để cắt tile từ ảnh gốc.                                      ║
                ║                                                                  ║
                ║  2. "Load Tileset" - Load tileset đã có từ thư mục data/tile     ║
                ║     để chỉnh sửa collision hoặc thêm/xóa tile.                   ║
                ║                                                                  ║
                ║  3. Click vào từng ô 24x24 trên Canvas để chọn tile.             ║
                ║     Có thể kéo chuột để chọn nhiều ô liên tiếp.                  ║
                ║                                                                  ║
                ║  4. Cấu hình Va Chạm (Collision):                                ║
                ║     • Sàn (Top=2)   - Đứng lên được                              ║
                ║     • Trần (Bottom=8192) - Đầu đụng trần                         ║
                ║     • Trái (Left=4) - Va chạm bên trái                           ║
                ║     • Phải (Right=8) - Va chạm bên phải                          ║
                ║     • Nước (Water=512) - Tile nước                               ║
                ║                                                                  ║
                ║  5. "THÊM VÀO LIST" - Thêm các ô đã chọn vào danh sách xuất.     ║
                ║     Click vào item trong list để sửa collision.                  ║
                ║                                                                  ║
                ║  6. "XUẤT FILE" - Nhập ID tileset và xuất file.                  ║
                ║                                                                  ║
                ╠══════════════════════════════════════════════════════════════════╣
                ║   OUTPUT FILES:                                                  ║
                ║  ──────────────────────────────────────────────────────────────  ║
                ║                                                                  ║
                ║  1. Ảnh tile riêng lẻ (cho client):                              ║
                ║     - data/data/res/x1/<ID>$<index>.png                       ║
                ║     - data/data/res/x2/<ID>$<index>.png                       ║
                ║     - data/data/res/x3/<ID>$<index>.png                       ║
                ║     - data/data/res/x4/<ID>$<index>.png                       ║
                ║     - Mỗi zoom level có thư mục riêng (x1=24px, x4=96px)         ║
                ║                                                                  ║
                ║  2. Tile strip (cho tool vẽ map):                                ║
                ║     - data/tile/<ID>                                             ║
                ║     - Ảnh dạng strip dọc 24px width, dùng trong DrawMapScr       ║
                ║                                                                  ║
                ║  3. Thông tin collision:                                         ║
                ║     - data/data/map/tile_set_info                             ║
                ║     - Binary file chứa collision data của tất cả tileset         ║
                ║     - Copy file này vào server và client res                     ║
                ║                                                                  ║
                ╠══════════════════════════════════════════════════════════════════╣
                ║  ĐIỀU KHIỂN ẢNH:                                                 ║
                ║  ──────────────────────────────────────────────────────────────  ║
                ║  • Ctrl + Scroll chuột: Thay đổi kích thước ảnh (10%-500%)       ║
                ║  • Chuột phải kéo: Di chuyển vị trí ảnh (riêng)                  ║
                ║  • Shift + Chuột trái kéo: Di chuyển cả ảnh và grid              ║
                ║  • Chuột giữa kéo: Di chuyển cả ảnh và grid                      ║
                ║  • Nút "Reset Img": Đưa ảnh về vị trí và kích thước gốc          ║
                ║  • Zoom slider: Thay đổi zoom của Grid (không ảnh hưởng ảnh lưu) ║
                ║                                                                  ║
                ╠══════════════════════════════════════════════════════════════════╣
                ║  LƯU Ý:                                                          ║
                ║  • Max 255 tile/tileset do giới hạn 1 byte index                 ║
                ║  • Restart tool sau khi xuất để cập nhật tile_set_info           ║
                ║  • Backup file tile_set_info.bak được tạo tự động                ║
                ║  • Scale/Offset ảnh chỉ để căn chỉnh, KHÔNG ảnh hưởng file lưu   ║
                ╚══════════════════════════════════════════════════════════════════╝
                """;

        JTextArea textArea = new JTextArea(guide);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        textArea.setEditable(false);
        textArea.setBackground(new Color(45, 45, 48));
        textArea.setForeground(new Color(220, 220, 220));
        textArea.setCaretColor(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 500));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JOptionPane.showMessageDialog(this, scrollPane, "Hướng Dẫn Sử Dụng", JOptionPane.PLAIN_MESSAGE);
    }

    private void ensureDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Inner classes
    private static class TileItem {
        BufferedImage image;
        Set<Integer> types;
        Point coords;

        TileItem(BufferedImage image, Set<Integer> types, Point coords) {
            this.image = image;
            this.types = types;
            this.coords = coords;
        }
    }

    private static class TilesetInfo {
        private int id;
        private Map<Integer, List<Integer>> types;

        TilesetInfo(int id) {
            this.id = id;
            this.types = new HashMap<>();
        }

        int getId() {
            return id;
        }

        Map<Integer, List<Integer>> getTypes() {
            return types;
        }

        void setTypes(Map<Integer, List<Integer>> types) {
            this.types = types;
        }
    }

    private static class TilesetData {
        private List<TilesetInfo> tilesets = new ArrayList<>();

        List<TilesetInfo> getTilesets() {
            return tilesets;
        }

        void load(String path) {
            File file = new File(path);
            if (!file.exists()) {
                tilesets.clear();
                return;
            }

            try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
                int nTileset = dis.readUnsignedByte();
                tilesets.clear();

                for (int i = 0; i < nTileset; i++) {
                    TilesetInfo tsInfo = new TilesetInfo(i + 1);

                    int nTypes = dis.readUnsignedByte();
                    for (int j = 0; j < nTypes; j++) {
                        int typeVal = dis.readInt();
                        int nTiles = dis.readUnsignedByte();
                        for (int k = 0; k < nTiles; k++) {
                            int tIdx = dis.readUnsignedByte();
                            tsInfo.types.computeIfAbsent(tIdx, x -> new ArrayList<>()).add(typeVal);
                        }
                    }
                    tilesets.add(tsInfo);
                }
            } catch (Exception e) {
                System.err.println("Load Error: " + e.getMessage());
                tilesets.clear();
            }
        }

        void save(String path) {
            try {
                // Backup
                File file = new File(path);
                if (file.exists()) {
                    File backup = new File(path + ".bak");
                    java.nio.file.Files.copy(file.toPath(), backup.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                // Ensure parent dir exists
                file.getParentFile().mkdirs();

                int maxId = tilesets.stream().mapToInt(TilesetInfo::getId).max().orElse(0);

                // Re-map: Create list of size maxId
                List<TilesetInfo> orderedList = new ArrayList<>();
                for (int i = 0; i < maxId; i++) {
                    orderedList.add(new TilesetInfo(i + 1));
                }

                for (TilesetInfo ts : tilesets) {
                    int tId = ts.getId();
                    if (tId >= 1 && tId <= maxId) {
                        orderedList.set(tId - 1, ts);
                    }
                }

                try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                    dos.writeByte(orderedList.size());

                    for (TilesetInfo ts : orderedList) {
                        // Group by Type
                        Map<Integer, List<Integer>> typeMap = new HashMap<>();
                        for (Map.Entry<Integer, List<Integer>> entry : ts.types.entrySet()) {
                            int tIdx = entry.getKey();
                            for (int typeVal : entry.getValue()) {
                                typeMap.computeIfAbsent(typeVal, x -> new ArrayList<>()).add(tIdx);
                            }
                        }

                        dos.writeByte(typeMap.size());
                        List<Integer> sortedTypes = new ArrayList<>(typeMap.keySet());
                        Collections.sort(sortedTypes);

                        for (int typeVal : sortedTypes) {
                            List<Integer> indices = typeMap.get(typeVal);
                            dos.writeInt(typeVal);
                            dos.writeByte(indices.size());
                            Collections.sort(indices);
                            for (int idx : indices) {
                                dos.writeByte(idx);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Save Error: " + e.getMessage());
            }
        }

        void updateTileset(int tsId, Map<Integer, List<Integer>> typesData) {
            // Remove old if exists
            tilesets.removeIf(ts -> ts.getId() == tsId);
            TilesetInfo newTs = new TilesetInfo(tsId);
            newTs.setTypes(typesData);
            tilesets.add(newTs);
            tilesets.sort(Comparator.comparingInt(TilesetInfo::getId));
        }
    }

    // Để test độc lập
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TilesetEditorDialog dialog = new TilesetEditorDialog();
            dialog.setVisible(true);
        });
    }
}
