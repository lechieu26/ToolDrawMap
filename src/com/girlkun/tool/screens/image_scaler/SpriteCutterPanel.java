package com.girlkun.tool.screens.image_scaler;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SpriteCutterPanel extends JPanel {
    private static final Color BG_COLOR = new Color(0x1a1a2e);
    private static final Color PANEL_COLOR = new Color(0x16213e);
    private static final Color ACCENT_COLOR = new Color(0x4a69bd);
    private static final Color SUCCESS_COLOR = new Color(0x27ae60);
    private static final Color WARNING_COLOR = new Color(0xf39c12);

    private BufferedImage originalImg;
    private List<Rectangle> bboxes = new ArrayList<>();
    private int selectedIndex = -1;
    private File currentFile;

    private ImagePanel imagePanel;
    private JLabel statusLabel;

    public SpriteCutterPanel() {
        setBackground(BG_COLOR);
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Top Panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBackground(PANEL_COLOR);
        topPanel.setPreferredSize(new Dimension(0, 60));

        JButton btnLoad = createStyledButton("Chọn Ảnh", ACCENT_COLOR);
        JButton btnDetect = createStyledButton("Tự Động Tìm Sprite", SUCCESS_COLOR);
        JButton btnSave = createStyledButton("Lưu Tất Cả", WARNING_COLOR);

        btnLoad.addActionListener(e -> loadImage());
        btnDetect.addActionListener(e -> detectSprites());
        btnSave.addActionListener(e -> saveSprites());

        topPanel.add(btnLoad);
        topPanel.add(btnDetect);
        topPanel.add(btnSave);

        add(topPanel, BorderLayout.NORTH);

        // Main Workspace
        imagePanel = new ImagePanel();
        add(imagePanel, BorderLayout.CENTER);

        // Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(ACCENT_COLOR);
        statusBar.setPreferredSize(new Dimension(0, 25));
        statusLabel = new JLabel(" Sẵn sàng");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);

        // Key Bindings
        setupKeyBindings();
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        am.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelected();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "open");
        am.put("open", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadImage();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        am.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSprites();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "detect");
        am.put("detect", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                detectSprites();
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
        btn.setPreferredSize(new Dimension(150, 35));
        return btn;
    }

    private void loadImage() {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        FileDialog dialog = new FileDialog(parent, "Chọn Ảnh", FileDialog.LOAD);
        if (currentFile != null) {
            dialog.setDirectory(currentFile.getParent());
        }
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp");
        });
        dialog.setVisible(true);

        String fileName = dialog.getFile();
        if (fileName != null) {
            try {
                currentFile = new File(dialog.getDirectory(), fileName);
                originalImg = ImageIO.read(currentFile);
                if (originalImg == null)
                    throw new IOException("Không thể đọc ảnh.");

                bboxes.clear();
                selectedIndex = -1;
                imagePanel.resetView();
                statusLabel.setText(String.format(" Đã tải: %s | Resolution: %dx%d",
                        currentFile.getName(), originalImg.getWidth(), originalImg.getHeight()));
                repaint();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi tải ảnh: " + ex.getMessage());
            }
        }
    }

    private void detectSprites() {
        if (originalImg == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ảnh trước!");
            return;
        }

        int w = originalImg.getWidth();
        int h = originalImg.getHeight();
        boolean[][] visited = new boolean[w][h];
        bboxes.clear();
        selectedIndex = -1;

        int minSize = 5;
        int threshold = 10;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!visited[x][y]) {
                    int argb = originalImg.getRGB(x, y);
                    int alpha = (argb >> 24) & 0xff;

                    if (alpha > threshold) {
                        Rectangle rect = floodFill(x, y, visited, threshold);
                        if (rect.width >= minSize && rect.height >= minSize) {
                            bboxes.add(rect);
                        }
                    }
                }
            }
        }

        // Sắp xếp (Y trước, X sau)
        bboxes.sort((r1, r2) -> {
            if (r1.y != r2.y)
                return Integer.compare(r1.y, r2.y);
            return Integer.compare(r1.x, r2.x);
        });

        statusLabel.setText(String.format(" Tìm thấy %d sprite(s)", bboxes.size()));
        repaint();
    }

    private Rectangle floodFill(int startX, int startY, boolean[][] visited, int threshold) {
        int minX = startX, maxX = startX;
        int minY = startY, maxY = startY;

        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));
        visited[startX][startY] = true;

        int w = originalImg.getWidth();
        int h = originalImg.getHeight();

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = p.x + dx;
                    int ny = p.y + dy;

                    if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[nx][ny]) {
                        int argb = originalImg.getRGB(nx, ny);
                        int alpha = (argb >> 24) & 0xff;
                        if (alpha > threshold) {
                            visited[nx][ny] = true;
                            queue.add(new Point(nx, ny));
                        }
                    }
                }
            }
        }

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private void deleteSelected() {
        if (selectedIndex != -1) {
            bboxes.remove(selectedIndex);
            selectedIndex = -1;
            statusLabel.setText(String.format(" Đã xóa sprite. Còn lại %d sprite(s)", bboxes.size()));
            repaint();
        }
    }

    private void saveSprites() {
        if (bboxes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có sprite nào để lưu!");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File outDir = chooser.getSelectedFile();
            outDir.mkdirs();

            String baseName = currentFile.getName().substring(0, currentFile.getName().lastIndexOf('.'));
            int count = 0;
            for (int i = 0; i < bboxes.size(); i++) {
                Rectangle r = bboxes.get(i);
                BufferedImage sprite = originalImg.getSubimage(r.x, r.y, r.width, r.height);
                try {
                    File outFile = new File(outDir, String.format("%s_%03d.png", baseName, i));
                    ImageIO.write(sprite, "PNG", outFile);
                    count++;
                } catch (IOException e) {
                    System.err.println("Lỗi khi lưu " + i);
                }
            }
            JOptionPane.showMessageDialog(this,
                    String.format("Đã lưu %d sprite vào thư mục:\n%s", count, outDir.getAbsolutePath()));
            try {
                Desktop.getDesktop().open(outDir);
            } catch (Exception ignored) {
            }
        }
    }

    class ImagePanel extends JPanel {
        private double zoom = 1.0;
        private double offsetX = 0, offsetY = 0;
        private Point lastMouse;

        public ImagePanel() {
            setBackground(BG_COLOR);
            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastMouse = e.getPoint();
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        selectAt(e.getPoint());
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                        offsetX += e.getX() - lastMouse.x;
                        offsetY += e.getY() - lastMouse.y;
                        lastMouse = e.getPoint();
                        repaint();
                    }
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    double delta = e.getPreciseWheelRotation();
                    double scale = Math.pow(1.2, -delta);

                    Point2D mousePos = e.getPoint();
                    Point2D worldPos = screenToWorld(mousePos);

                    zoom *= scale;
                    zoom = Math.max(0.01, Math.min(zoom, 50.0));

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
            if (originalImg == null)
                return;
            zoom = Math.min((double) getWidth() / originalImg.getWidth(),
                    (double) getHeight() / originalImg.getHeight()) * 0.9;
            if (zoom <= 0)
                zoom = 1.0;
            offsetX = (getWidth() - originalImg.getWidth() * zoom) / 2.0;
            offsetY = (getHeight() - originalImg.getHeight() * zoom) / 2.0;
            repaint();
        }

        private void selectAt(Point p) {
            if (originalImg == null)
                return;
            Point2D wp = screenToWorld(p);
            int oldIndex = selectedIndex;
            selectedIndex = -1;

            double minArea = Double.MAX_VALUE;
            for (int i = 0; i < bboxes.size(); i++) {
                Rectangle r = bboxes.get(i);
                if (r.contains(wp.getX(), wp.getY())) {
                    double area = r.getWidth() * r.getHeight();
                    if (area < minArea) {
                        minArea = area;
                        selectedIndex = i;
                    }
                }
            }
            if (oldIndex != selectedIndex)
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
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            if (originalImg != null) {
                AffineTransform oldAt = g2.getTransform();
                g2.transform(getTransform());

                g2.drawImage(originalImg, 0, 0, null);

                for (int i = 0; i < bboxes.size(); i++) {
                    Rectangle r = bboxes.get(i);
                    if (i == selectedIndex) {
                        g2.setColor(Color.RED);
                        g2.setStroke(new BasicStroke((float) (2.0 / zoom)));
                    } else {
                        g2.setColor(Color.GREEN);
                        g2.setStroke(new BasicStroke((float) (1.0 / zoom)));
                    }
                    g2.draw(r);
                }

                g2.setTransform(oldAt);
            }
        }
    }
}
