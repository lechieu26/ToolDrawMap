package com.girlkun.tool.screens.sprite_scr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class CanvasView extends JPanel {
    private List<ResizableSprite> sprites = new ArrayList<>();
    private BufferedImage charRef;
    private BufferedImage customRef;
    private Point panOffset = new Point(0, 0);
    private Point refPos = new Point(0, 0); // Position of custom reference
    private Point lastMousePos;
    private boolean isPanning = false;
    private boolean isMovingRef = false;

    private ResizableSprite interactingSprite;
    private String dragMode;
    private Point dragStartMouse;
    private Rectangle startBounds;

    private Runnable onModified;
    private java.util.function.Consumer<List<FrameData>> onSelectionChanged;

    public CanvasView() {
        setBackground(new Color(40, 40, 40));
        loadReferenceChar();
        setupEvents();
    }

    private void loadReferenceChar() {
        try {
            String[] paths = {"Char.png", "data/Char.png", "../Char.png"};
            for (String p : paths) {
                File f = new File(p);
                if (f.exists()) {
                    charRef = ImageIO.read(f);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load Char.png: " + e.getMessage());
        }
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    public void setOnSelectionChanged(java.util.function.Consumer<List<FrameData>> listener) {
        this.onSelectionChanged = listener;
    }

    public void syncFrames(List<FrameData> frames) {
        List<FrameData> selectedData = getSelectedData();
        sprites.clear();
        for (FrameData f : frames) {
            ResizableSprite s = new ResizableSprite(f);
            for (FrameData sd : selectedData) {
                if (sd == f) s.setSelected(true);
            }
            sprites.add(s);
        }
        repaint();
    }

    private List<FrameData> getSelectedData() {
        List<FrameData> sel = new ArrayList<>();
        for (ResizableSprite s : sprites) {
            if (s.isSelected()) sel.add(s.getData());
        }
        return sel;
    }

    public void updateSelection(List<FrameData> selected) {
        for (ResizableSprite s : sprites) {
            s.setSelected(selected.contains(s.getData()));
        }
        repaint();
    }

    public void addCustomReference(String path) {
        try {
            customRef = ImageIO.read(new File(path));
            refPos = new Point(0, -customRef.getHeight()); // Initial position
            repaint();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading reference: " + e.getMessage());
        }
    }

    private void setupEvents() {
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    isPanning = true;
                    lastMousePos = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    handleLeftPress(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPanning = false;
                isMovingRef = false;
                interactingSprite = null;
                dragMode = null;
                setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isPanning) {
                    panOffset.x += e.getX() - lastMousePos.x;
                    panOffset.y += e.getY() - lastMousePos.y;
                    lastMousePos = e.getPoint();
                    repaint();
                } else if (isMovingRef) {
                    Point worldP = screenToWorld(e.getPoint());
                    refPos.x = startBounds.x + (worldP.x - dragStartMouse.x);
                    refPos.y = startBounds.y + (worldP.y - dragStartMouse.y);
                    repaint();
                } else if (interactingSprite != null) {
                    handleDrag(e.getPoint());
                }
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void handleLeftPress(MouseEvent e) {
        Point worldP = screenToWorld(e.getPoint());
        interactingSprite = null;
        dragMode = null;

        for (int i = sprites.size() - 1; i >= 0; i--) {
            ResizableSprite s = sprites.get(i);
            if (s.isSelected()) {
                String hit = s.hitTest(worldP);
                if (hit != null && !hit.equals("move")) {
                    interactingSprite = s;
                    dragMode = hit;
                    break;
                }
            }
        }

        if (interactingSprite == null) {
            boolean multi = e.isControlDown() || e.isShiftDown();
            boolean hitSomething = false;
            for (int i = sprites.size() - 1; i >= 0; i--) {
                ResizableSprite s = sprites.get(i);
                String hit = s.hitTest(worldP);
                if (hit != null) {
                    if (!multi && !s.isSelected()) {
                        deselectAll();
                    }
                    s.setSelected(true);
                    interactingSprite = s;
                    dragMode = "move";
                    hitSomething = true;
                    break;
                }
            }
            if (!hitSomething && !multi) {
                deselectAll();
                // Check if hit customRef
                if (customRef != null) {
                    Rectangle refRect = new Rectangle(refPos.x, refPos.y, customRef.getWidth(), customRef.getHeight());
                    if (refRect.contains(worldP)) {
                        isMovingRef = true;
                        dragStartMouse = worldP;
                        startBounds = new Rectangle(refPos.x, refPos.y, 0, 0); // Use width/height as 0 for simple offset
                        hitSomething = true;
                    }
                }
            }
            if (onSelectionChanged != null && hitSomething && !isMovingRef) onSelectionChanged.accept(getSelectedData());
        }

        if (interactingSprite != null) {
            dragStartMouse = worldP;
            startBounds = new Rectangle((int)interactingSprite.getData().getX(), 
                                        (int)interactingSprite.getData().getY(), 
                                        (int)interactingSprite.getData().getDisplayWidth(), 
                                        (int)interactingSprite.getData().getDisplayHeight());
        }
        repaint();
    }

    private void deselectAll() {
        for (ResizableSprite s : sprites) s.setSelected(false);
    }

    private void handleDrag(Point p) {
        Point worldP = screenToWorld(p);
        int dx = worldP.x - dragStartMouse.x;
        int dy = worldP.y - dragStartMouse.y;
        
        FrameData d = interactingSprite.getData();
        
        if (dragMode.equals("move")) {
            d.setX(startBounds.x + dx);
            d.setY(startBounds.y + dy);
        } else {
            int nx = startBounds.x;
            int ny = startBounds.y;
            int nw = startBounds.width;
            int nh = startBounds.height;
            
            // Check if it's a corner handle (length 2 like "tl", "tr", etc.)
            boolean isCorner = dragMode.length() == 2;
            
            if (isCorner) {
                double ratio = (double) startBounds.width / startBounds.height;

                // Adjust based on which corner is being dragged
                if (dragMode.equals("br")) {
                    nw = startBounds.width + dx;
                    nh = (int)(nw / ratio);
                } else if (dragMode.equals("tl")) {
                    nw = startBounds.width - dx;
                    nh = (int)(nw / ratio);
                    nx = startBounds.x + (startBounds.width - nw);
                    ny = startBounds.y + (startBounds.height - nh);
                } else if (dragMode.equals("tr")) {
                    nw = startBounds.width + dx;
                    nh = (int)(nw / ratio);
                    ny = startBounds.y + (startBounds.height - nh);
                } else if (dragMode.equals("bl")) {
                    nw = startBounds.width - dx;
                    nh = (int)(nw / ratio);
                    nx = startBounds.x + (startBounds.width - nw);
                }
            } else {
                // Edge resizing (non-uniform)
                if (dragMode.contains("l")) { nx += dx; nw -= dx; }
                if (dragMode.contains("r")) { nw += dx; }
                if (dragMode.contains("t")) { ny += dy; nh -= dy; }
                if (dragMode.contains("b")) { nh += dy; }
            }
            
            if (nw < 1) nw = 1;
            if (nh < 1) nh = 1;
            
            d.setX(nx);
            d.setY(ny);
            d.setDisplayWidth(nw);
            d.setDisplayHeight(nh);
        }
        
        interactingSprite.updateBounds();
        if (onModified != null) onModified.run();
        repaint();
    }

    private Point screenToWorld(Point p) {
        int cx = getWidth() / 2 + panOffset.x;
        int cy = getHeight() / 2 + panOffset.y;
        return new Point(p.x - cx, p.y - cy);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int cx = getWidth() / 2 + panOffset.x;
        int cy = getHeight() / 2 + panOffset.y;

        g2.translate(cx, cy);

        g2.setColor(new Color(100, 100, 100));
        float[] dash = {2.0f};
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dash, 0));
        g2.drawLine(-2000, 0, 2000, 0);
        g2.drawLine(0, -2000, 0, 2000);

        if (charRef != null) {
            int h = charRef.getHeight();
            g2.drawImage(charRef, -150, -h, null);
        }

        if (customRef != null) {
            Composite oldComp = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            g2.drawImage(customRef, refPos.x, refPos.y, null);
            g2.setComposite(oldComp);
            
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dash, 0));
            g2.drawRect(refPos.x, refPos.y, customRef.getWidth(), customRef.getHeight());
        }

        g2.setStroke(new BasicStroke(1));
        for (ResizableSprite s : sprites) {
            s.draw(g2);
        }
    }

    public void resetCamera() {
        List<FrameData> selected = getSelectedData();
        for (FrameData d : selected) {
            d.setX(0);
            d.setY(-d.getDisplayHeight());
        }
        panOffset = new Point(0, 0);
        interactingSprite = null;
        for (ResizableSprite s : sprites) s.updateBounds();
        if (onModified != null) onModified.run();
        repaint();
    }
}
