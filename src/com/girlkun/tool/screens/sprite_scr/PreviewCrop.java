package com.girlkun.tool.screens.sprite_scr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;

public class PreviewCrop extends JPanel {
    private BufferedImage originalImage;
    private double scale = 1.0;
    private Rectangle cropRect = null;
    private boolean active = false;
    private String dragMode = null; // "move", "tl", "t", etc.
    private Point dragStartPoint;
    private Rectangle rectStart;
    
    private final int HANDLE_SIZE = 8;
    private Consumer<BufferedImage> onCropListener;
    
    private JLabel lblZoom;
    private ImagePanel imagePanel;

    public PreviewCrop() {
        setLayout(new BorderLayout());
        setupUI();
    }

    private void setupUI() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnZoomIn = createStyledButton("Zoom +", new Color(102, 102, 102));
        JButton btnZoomOut = createStyledButton("Zoom -", new Color(102, 102, 102));
        lblZoom = new JLabel("100%");
        JButton btnCut = createStyledButton("CUT (Enter)", new Color(180, 120, 0));

        btnZoomIn.addActionListener(e -> zoomIn());
        btnZoomOut.addActionListener(e -> zoomOut());
        btnCut.addActionListener(e -> performCut());

        toolbar.add(btnZoomIn);
        toolbar.add(btnZoomOut);
        toolbar.add(lblZoom);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(btnCut);

        add(toolbar, BorderLayout.NORTH);

        imagePanel = new ImagePanel();
        JScrollPane scrollPane = new JScrollPane(imagePanel);
        scrollPane.setBackground(new Color(48, 48, 48));
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setOnCropListener(Consumer<BufferedImage> listener) {
        this.onCropListener = listener;
    }

    public void loadImage(BufferedImage img) {
        this.originalImage = img;
        this.cropRect = null;
        this.active = false;
        
        if (img.getWidth() <= 200) {
            scale = 1.0;
        } else {
            scale = 0.2;
        }
        updateUIState();
    }

    private void zoomIn() {
        scale += 0.1;
        updateUIState();
    }

    private void zoomOut() {
        if (scale > 0.1) {
            scale -= 0.1;
            if (scale < 0.1) scale = 0.1;
        }
        updateUIState();
    }

    private void updateUIState() {
        lblZoom.setText((int) (scale * 100) + "%");
        imagePanel.revalidate();
        imagePanel.repaint();
    }

    private void performCut() {
        if (active && cropRect != null && !cropRect.isEmpty()) {
            BufferedImage cropped = originalImage.getSubimage(
                cropRect.x, cropRect.y, cropRect.width, cropRect.height
            );
            // Create a copy to avoid subimage issues
            BufferedImage copy = new BufferedImage(cropped.getWidth(), cropped.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = copy.getGraphics();
            g.drawImage(cropped, 0, 0, null);
            g.dispose();
            
            if (onCropListener != null) {
                onCropListener.accept(copy);
            }
        }
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        if (bg != null) {
            btn.setBackground(bg);
            btn.setForeground(Color.WHITE);
        }
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusable(false);
        return btn;
    }

    private class ImagePanel extends JPanel {
        private TexturePaint checkerPaint;
        private Point panLastPos;

        public ImagePanel() {
            setBackground(new Color(48, 48, 48));
            checkerPaint = createCheckerPaint();
            setFocusable(true);
            
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        panLastPos = e.getLocationOnScreen();
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        handleLeftPress(e);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        dragMode = null;
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        handlePan(e);
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        handleDrag(e.getPoint());
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    updateCursor(e.getPoint());
                }
            };

            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
            
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        performCut();
                    }
                }
            });
            
            addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    if (e.getWheelRotation() < 0) zoomIn();
                    else zoomOut();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            if (originalImage == null) return new Dimension(400, 400);
            return new Dimension((int) (originalImage.getWidth() * scale), (int) (originalImage.getHeight() * scale));
        }

        private TexturePaint createCheckerPaint() {
            int size = 20;
            BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = bi.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, size, size);
            g2.setColor(new Color(220, 220, 220));
            g2.fillRect(0, 0, size / 2, size / 2);
            g2.fillRect(size / 2, size / 2, size / 2, size / 2);
            g2.dispose();
            return new TexturePaint(bi, new Rectangle(0, 0, size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(checkerPaint);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (originalImage != null) {
                int dw = (int) (originalImage.getWidth() * scale);
                int dh = (int) (originalImage.getHeight() * scale);
                g2.drawImage(originalImage, 0, 0, dw, dh, null);

                if (active && cropRect != null) {
                    Rectangle sRect = mapImageRectToScreen(cropRect);
                    g2.setColor(new Color(0, 255, 0, 30));
                    g2.fill(sRect);
                    g2.setColor(Color.GREEN);
                    g2.setStroke(new BasicStroke(2));
                    g2.draw(sRect);

                    // Handles
                    Map<String, Rectangle> handles = getHandles(sRect);
                    g2.setColor(Color.WHITE);
                    for (Rectangle r : handles.values()) {
                        g2.fill(r);
                    }
                    g2.setColor(Color.BLACK);
                    g2.setStroke(new BasicStroke(1));
                    for (Rectangle r : handles.values()) {
                        g2.draw(r);
                    }
                }
            }
        }

        private void handlePan(MouseEvent e) {
            Point currentPos = e.getLocationOnScreen();
            int dx = currentPos.x - panLastPos.x;
            int dy = currentPos.y - panLastPos.y;
            panLastPos = currentPos;

            JViewport viewport = (JViewport) getParent();
            Point viewPos = viewport.getViewPosition();
            viewPos.x = Math.max(0, viewPos.x - dx);
            viewPos.y = Math.max(0, viewPos.y - dy);
            viewport.setViewPosition(viewPos);
        }

        private void handleLeftPress(MouseEvent e) {
            if (originalImage == null) return;
            
            Point p = e.getPoint();
            String hit = hitTest(p);
            
            if (hit != null) {
                dragMode = hit;
                dragStartPoint = p;
                rectStart = new Rectangle(cropRect);
            } else {
                Point imgP = mapScreenPointToImage(p);
                if (imgP.x >= 0 && imgP.x < originalImage.getWidth() && imgP.y >= 0 && imgP.y < originalImage.getHeight()) {
                    Rectangle smart = smartDetect(imgP.x, imgP.y);
                    if (smart != null) {
                        cropRect = smart;
                        active = true;
                    } else {
                        active = true;
                        dragMode = "new";
                        dragStartPoint = p;
                        rectStart = new Rectangle(imgP.x, imgP.y, 0, 0);
                        cropRect = new Rectangle(imgP.x, imgP.y, 0, 0);
                    }
                    repaint();
                } else {
                    active = false;
                    repaint();
                }
            }
        }

        private void handleDrag(Point p) {
            if (dragMode == null) return;

            double dx = (p.x - dragStartPoint.x) / scale;
            double dy = (p.y - dragStartPoint.y) / scale;

            Rectangle newRect = new Rectangle(rectStart);

            if (dragMode.equals("new")) {
                Point imgStart = mapScreenPointToImage(dragStartPoint);
                Point imgCurr = mapScreenPointToImage(p);
                int x = Math.min(imgStart.x, imgCurr.x);
                int y = Math.min(imgStart.y, imgCurr.y);
                int w = Math.abs(imgCurr.x - imgStart.x);
                int h = Math.abs(imgCurr.y - imgStart.y);
                newRect = new Rectangle(x, y, w, h);
            } else if (dragMode.equals("move")) {
                newRect.translate((int) dx, (int) dy);
            } else {
                if (dragMode.contains("l")) { newRect.x = rectStart.x + (int)dx; newRect.width = rectStart.width - (int)dx; }
                if (dragMode.contains("r")) { newRect.width = rectStart.width + (int)dx; }
                if (dragMode.contains("t")) { newRect.y = rectStart.y + (int)dy; newRect.height = rectStart.height - (int)dy; }
                if (dragMode.contains("b")) { newRect.height = rectStart.height + (int)dy; }
            }

            // Normalize
            if (newRect.width < 0) { newRect.x += newRect.width; newRect.width = -newRect.width; }
            if (newRect.height < 0) { newRect.y += newRect.height; newRect.height = -newRect.height; }
            
            cropRect = newRect;
            repaint();
        }

        private void updateCursor(Point p) {
            String hit = hitTest(p);
            if (hit == null) setCursor(Cursor.getDefaultCursor());
            else if (hit.equals("move")) setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            else if (hit.equals("tl") || hit.equals("br")) setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
            else if (hit.equals("tr") || hit.equals("bl")) setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
            else if (hit.equals("t") || hit.equals("b")) setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
            else if (hit.equals("l") || hit.equals("r")) setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
        }

        private String hitTest(Point p) {
            if (!active || cropRect == null) return null;
            Rectangle sRect = mapImageRectToScreen(cropRect);
            Map<String, Rectangle> handles = getHandles(sRect);
            for (Map.Entry<String, Rectangle> entry : handles.entrySet()) {
                if (entry.getValue().contains(p)) return entry.getKey();
            }
            if (sRect.contains(p)) return "move";
            return null;
        }

        private Map<String, Rectangle> getHandles(Rectangle s) {
            Map<String, Rectangle> h = new HashMap<>();
            int hs = HANDLE_SIZE;
            int hh = hs / 2;
            h.put("tl", new Rectangle(s.x - hh, s.y - hh, hs, hs));
            h.put("t", new Rectangle(s.x + s.width / 2 - hh, s.y - hh, hs, hs));
            h.put("tr", new Rectangle(s.x + s.width - hh, s.y - hh, hs, hs));
            h.put("r", new Rectangle(s.x + s.width - hh, s.y + s.height / 2 - hh, hs, hs));
            h.put("br", new Rectangle(s.x + s.width - hh, s.y + s.height - hh, hs, hs));
            h.put("b", new Rectangle(s.x + s.width / 2 - hh, s.y + s.height - hh, hs, hs));
            h.put("bl", new Rectangle(s.x - hh, s.y + s.height - hh, hs, hs));
            h.put("l", new Rectangle(s.x - hh, s.y + s.height / 2 - hh, hs, hs));
            return h;
        }

        private Point mapScreenPointToImage(Point p) {
            return new Point((int) (p.x / scale), (int) (p.y / scale));
        }

        private Rectangle mapImageRectToScreen(Rectangle r) {
            return new Rectangle((int) (r.x * scale), (int) (r.y * scale), (int) (r.width * scale), (int) (r.height * scale));
        }

        private Rectangle smartDetect(int x, int y) {
            if (originalImage == null) return null;
            int w = originalImage.getWidth();
            int h = originalImage.getHeight();
            
            // Check alpha
            int argb = originalImage.getRGB(x, y);
            int alpha = (argb >> 24) & 0xFF;
            if (alpha < 10) return null;

            // Simple Breadth-First Search for connected non-transparent pixels
            boolean[][] visited = new boolean[h][w];
            Stack<Point> stack = new Stack<>();
            stack.push(new Point(x, y));
            visited[y][x] = true;

            int minX = x, maxX = x, minY = y, maxY = y;

            while (!stack.isEmpty()) {
                Point p = stack.pop();
                if (p.x < minX) minX = p.x;
                if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.y > maxY) maxY = p.y;

                int[] dx = {1, -1, 0, 0};
                int[] dy = {0, 0, 1, -1};

                for (int i = 0; i < 4; i++) {
                    int nx = p.x + dx[i];
                    int ny = p.y + dy[i];

                    if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[ny][nx]) {
                        int nargb = originalImage.getRGB(nx, ny);
                        int nalpha = (nargb >> 24) & 0xFF;
                        if (nalpha >= 10) {
                            visited[ny][nx] = true;
                            stack.push(new Point(nx, ny));
                        }
                    }
                }
                
                // Safety break for extremely large sprites to avoid long hangs
                if (stack.size() > 50000) break; 
            }

            return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        }
    }
}
