package com.girlkun.tool.screens.mob_scr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Panel hiển thị atlas image, hỗ trợ crop sprite (auto-fit, manual draw, resize handles).
 */
public class AtlasPanel extends JPanel {
    private MobEditor editor;

    // Atlas display
    private BufferedImage atlas;
    private double atlasScale = 1.0;
    private BufferedImage displayImage;

    // Crop rect in canvas coords
    private Rectangle cropRect; // null = no crop
    private String cropMode; // "draw", "move", "resize-nw", etc.
    private int cropStartX, cropStartY;
    private static final int HANDLE_SIZE = 5;
    private static final int MIN_CROP_SIZE = 10;

    // Canvas for drawing
    private JPanel canvas;
    private JScrollPane scrollPane;
    private JButton btnCut;
    private JLabel zoomLabel;
    private JCheckBox autoFitCb, autoNextCb;

    // Right-click pan
    private Point panStart;

    public AtlasPanel(MobEditor editor) {
        this.editor = editor;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Atlas (Crop Sprite)"));
        buildUI();
    }

    private void buildUI() {
        // Toolbar 2 dòng
        JPanel toolbarWrap = new JPanel(new GridLayout(2, 1, 0, 1));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));
        JButton zoomIn = new JButton("Zoom +");
        zoomIn.addActionListener(e -> zoomIn());
        JButton zoomOut = new JButton("Zoom -");
        zoomOut.addActionListener(e -> zoomOut());
        zoomLabel = new JLabel("100%");
        autoFitCb = new JCheckBox("Auto-Fit", true);
        autoNextCb = new JCheckBox("Auto Next", false);
        row1.add(zoomIn); row1.add(zoomOut); row1.add(zoomLabel);
        row1.add(autoFitCb); row1.add(autoNextCb);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));
        btnCut = new JButton("CUT [Enter]");
        btnCut.setBackground(new Color(0xFF, 0xCC, 0x00));
        btnCut.setForeground(Color.BLACK);
        btnCut.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnCut.setEnabled(false);
        btnCut.addActionListener(e -> performCut());
        row2.add(btnCut);

        toolbarWrap.add(row1);
        toolbarWrap.add(row2);
        add(toolbarWrap, BorderLayout.NORTH);

        // Canvas
        canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawCanvas((Graphics2D) g);
            }
        };
        canvas.setBackground(new Color(0x20, 0x20, 0x20));
        canvas.setPreferredSize(new Dimension(400, 400));

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    panStart = e.getPoint();
                    canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    onPress(e);
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    panStart = null;
                    canvas.setCursor(Cursor.getDefaultCursor());
                } else {
                    onRelease(e);
                }
            }
        });

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (panStart != null && SwingUtilities.isRightMouseButton(e)) {
                    // Pan via scrollbar
                    JViewport vp = scrollPane.getViewport();
                    Point vpp = vp.getViewPosition();
                    int dx = panStart.x - e.getX();
                    int dy = panStart.y - e.getY();
                    vpp.translate(dx, dy);
                    canvas.scrollRectToVisible(new Rectangle(vpp, vp.getSize()));
                } else {
                    onDrag(e);
                }
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                onHover(e);
            }
        });

        canvas.addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) zoomIn();
            else zoomOut();
        });

        scrollPane = new JScrollPane(canvas);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    public boolean isAutoFit() { return autoFitCb.isSelected(); }
    public boolean isAutoNext() { return autoNextCb.isSelected(); }

    public void setAtlas(BufferedImage img) {
        this.atlas = img;
        this.atlasScale = 1.0;
        if (img != null && Math.max(img.getWidth(), img.getHeight()) > 800) {
            atlasScale = 800.0 / Math.max(img.getWidth(), img.getHeight());
        }
        clearCrop();
        updateDisplay();
    }

    public BufferedImage getAtlas() { return atlas; }

    private void zoomIn() {
        if (atlas == null) return;
        atlasScale *= 1.25;
        clearCrop();
        updateDisplay();
    }

    private void zoomOut() {
        if (atlas == null) return;
        atlasScale /= 1.25;
        if (atlasScale < 0.1) atlasScale = 0.1;
        clearCrop();
        updateDisplay();
    }

    public void updateDisplay() {
        if (atlas == null) return;
        int w = Math.max(1, (int)(atlas.getWidth() * atlasScale));
        int h = Math.max(1, (int)(atlas.getHeight() * atlasScale));

        displayImage = EditorUtils.resizeNearest(atlas, w, h);

        // Composite on checkerboard
        BufferedImage bg = EditorUtils.createCheckerboard(w, h);
        Graphics2D g = bg.createGraphics();
        g.drawImage(displayImage, 0, 0, null);
        g.dispose();
        displayImage = bg;

        canvas.setPreferredSize(new Dimension(w, h));
        canvas.revalidate();
        canvas.repaint();
        zoomLabel.setText((int)(atlasScale * 100) + "%");
    }

    private void drawCanvas(Graphics2D g) {
        if (displayImage != null) {
            g.drawImage(displayImage, 0, 0, null);
        }
        // Draw crop rect
        if (cropRect != null) {
            g.setColor(Color.YELLOW);
            g.setStroke(new BasicStroke(2));
            g.drawRect(cropRect.x, cropRect.y, cropRect.width, cropRect.height);

            // Draw handles
            g.setColor(Color.WHITE);
            int hs = HANDLE_SIZE;
            int cx = cropRect.x + cropRect.width / 2;
            int cy = cropRect.y + cropRect.height / 2;
            int x1 = cropRect.x, y1 = cropRect.y;
            int x2 = cropRect.x + cropRect.width, y2 = cropRect.y + cropRect.height;

            // Corner handles
            drawHandle(g, x1, y1, hs); drawHandle(g, x2, y1, hs);
            drawHandle(g, x1, y2, hs); drawHandle(g, x2, y2, hs);
            // Edge handles
            g.setColor(Color.RED);
            drawHandle(g, cx, y1, hs); drawHandle(g, cx, y2, hs);
            drawHandle(g, x1, cy, hs); drawHandle(g, x2, cy, hs);
        }
    }

    private void drawHandle(Graphics2D g, int x, int y, int size) {
        g.fillRect(x - size, y - size, size * 2, size * 2);
    }

    // --- Mouse Interactions ---
    private void onPress(MouseEvent e) {
        if (atlas == null) return;
        int mx = e.getX(), my = e.getY();

        // Check crop hit
        if (cropRect != null) {
            String mode = hitTestCrop(mx, my);
            if (mode != null) {
                cropMode = mode;
                cropStartX = mx; cropStartY = my;
                return;
            }
        }

        // Auto-fit
        if (autoFitCb.isSelected()) {
            doAutoFit(mx, my);
            return;
        }

        // Start new crop
        clearCrop();
        cropMode = "draw";
        cropStartX = mx; cropStartY = my;
    }

    private void onDrag(MouseEvent e) {
        if (atlas == null || cropMode == null) return;
        int mx = e.getX(), my = e.getY();

        if ("draw".equals(cropMode)) {
            int x1 = Math.min(cropStartX, mx), y1 = Math.min(cropStartY, my);
            int x2 = Math.max(cropStartX, mx), y2 = Math.max(cropStartY, my);
            cropRect = new Rectangle(x1, y1, x2 - x1, y2 - y1);
            btnCut.setEnabled(true);
        } else if ("move".equals(cropMode)) {
            int dx = mx - cropStartX, dy = my - cropStartY;
            cropRect.x += dx; cropRect.y += dy;
            cropStartX = mx; cropStartY = my;
        } else if (cropMode != null && cropMode.startsWith("resize")) {
            resizeCrop(mx, my);
        }
        canvas.repaint();
    }

    private void onRelease(MouseEvent e) {
        cropMode = null;
        if (cropRect != null) {
            if (cropRect.width < MIN_CROP_SIZE || cropRect.height < MIN_CROP_SIZE) {
                clearCrop();
            } else {
                btnCut.setEnabled(true);
            }
        }
        canvas.repaint();
    }

    private void onHover(MouseEvent e) {
        if (cropRect == null) {
            canvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            return;
        }
        String mode = hitTestCrop(e.getX(), e.getY());
        if (mode == null) {
            canvas.setCursor(Cursor.getDefaultCursor());
        } else if ("move".equals(mode)) {
            canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else {
            String d = mode.replace("resize-", "");
            switch (d) {
                case "nw": case "se": canvas.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)); break;
                case "ne": case "sw": canvas.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)); break;
                case "n": case "s": canvas.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)); break;
                case "w": case "e": canvas.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)); break;
            }
        }
    }

    private String hitTestCrop(int x, int y) {
        if (cropRect == null) return null;
        int hs = HANDLE_SIZE + 3;
        int x1 = cropRect.x, y1 = cropRect.y;
        int x2 = cropRect.x + cropRect.width, y2 = cropRect.y + cropRect.height;
        int cx = (x1 + x2) / 2, cy = (y1 + y2) / 2;

        // Handles
        if (near(x, y, x1, y1, hs)) return "resize-nw";
        if (near(x, y, x2, y1, hs)) return "resize-ne";
        if (near(x, y, x1, y2, hs)) return "resize-sw";
        if (near(x, y, x2, y2, hs)) return "resize-se";
        if (near(x, y, cx, y1, hs)) return "resize-n";
        if (near(x, y, cx, y2, hs)) return "resize-s";
        if (near(x, y, x1, cy, hs)) return "resize-w";
        if (near(x, y, x2, cy, hs)) return "resize-e";

        // Body
        if (x >= x1 && x <= x2 && y >= y1 && y <= y2) return "move";
        return null;
    }

    private boolean near(int x, int y, int tx, int ty, int r) {
        return Math.abs(x - tx) <= r && Math.abs(y - ty) <= r;
    }

    private void resizeCrop(int mx, int my) {
        if (cropRect == null) return;
        String d = cropMode.replace("resize-", "");
        int x1 = cropRect.x, y1 = cropRect.y;
        int x2 = cropRect.x + cropRect.width, y2 = cropRect.y + cropRect.height;

        if (d.contains("n") && y2 - my > MIN_CROP_SIZE) y1 = my;
        if (d.contains("s") && my - y1 > MIN_CROP_SIZE) y2 = my;
        if (d.contains("w") && x2 - mx > MIN_CROP_SIZE) x1 = mx;
        if (d.contains("e") && mx - x1 > MIN_CROP_SIZE) x2 = mx;

        cropRect = new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }

    private void doAutoFit(int cx, int cy) {
        if (atlas == null) return;
        int ax = (int)(cx / atlasScale);
        int ay = (int)(cy / atlasScale);

        int[] bbox = EditorUtils.findConnectedBBox(atlas, ax, ay);
        if (bbox == null) return;

        int bx = (int)(bbox[0] * atlasScale);
        int by = (int)(bbox[1] * atlasScale);
        int bw = (int)(bbox[2] * atlasScale);
        int bh = (int)(bbox[3] * atlasScale);

        cropRect = new Rectangle(bx, by, bw, bh);
        btnCut.setEnabled(true);
        canvas.repaint();
    }

    public void clearCrop() {
        cropRect = null;
        cropMode = null;
        btnCut.setEnabled(false);
        canvas.repaint();
    }

    public void performCut() {
        if (cropRect == null || atlas == null) return;

        int ax0 = Math.max(0, (int) Math.round(cropRect.x / atlasScale));
        int ay0 = Math.max(0, (int) Math.round(cropRect.y / atlasScale));
        int aw = Math.max(1, (int) Math.round(cropRect.width / atlasScale));
        int ah = Math.max(1, (int) Math.round(cropRect.height / atlasScale));

        editor.addSprite(ax0, ay0, aw, ah);
        clearCrop();
    }

    public double getAtlasScale() { return atlasScale; }
}
