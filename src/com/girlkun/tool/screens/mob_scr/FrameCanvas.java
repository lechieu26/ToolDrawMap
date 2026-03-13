package com.girlkun.tool.screens.mob_scr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.File;

/**
 * Canvas hiển thị frame editor - vẽ parts, drag/drop, resize, marquee selection, keyboard move.
 */
public class FrameCanvas extends JPanel {
    private MobEditor editor;
    private double canvasScale = 1.0;

    // Drag state
    private boolean dragging = false;
    private boolean dragHasMoved = false;
    private Point dragLastPos;
    private String resizingHandle; // "tl","tr","bl","br"
    private FramePart resizingPart;
    private Point resizingStartMouse;
    private double resizeStartW, resizeStartH, resizeStartDx, resizeStartDy;

    // Marquee
    private Point marqueeStart;
    private Rectangle marqueeRect;

    // Key movement
    private Set<Integer> activeKeys = new HashSet<>();
    private javax.swing.Timer moveTimer;

    // Preview
    private boolean previewing = false;
    private int previewIdx = 0;
    private javax.swing.Timer previewTimer;

    // Char reference
    private BufferedImage charImg;

    public FrameCanvas(MobEditor editor) {
        this.editor = editor;
        setBackground(new Color(0x11, 0x11, 0x11));
        setFocusable(true);

        // Load Char.png
        try {
            File charFile = new File("Char.png");
            if (charFile.exists()) charImg = ImageIO.read(charFile);
        } catch (Exception ignored) {}

        setupMouseListeners();
        setupKeyListeners();
    }

    private int getCanvasW() { return Math.max(100, getWidth()); }
    private int getCanvasH() { return Math.max(100, getHeight()); }
    public double getCanvasScale() { return canvasScale; }
    public void setCanvasScale(double s) { canvasScale = s; repaint(); }
    private int getAxisY() { return getCanvasH() * 3 / 4; }

    public Point worldToScreen(double wx, double wy) {
        int cx = getCanvasW() / 2;
        int sx = (int)(cx + wx * canvasScale);
        int sy = (int)(getAxisY() + wy * canvasScale);
        return new Point(sx, sy);
    }

    public Point screenToWorld(int sx, int sy) {
        int cx = getCanvasW() / 2;
        double wx = (sx - cx) / canvasScale;
        double wy = (sy - getAxisY()) / canvasScale;
        return new Point((int) Math.round(wx), (int) Math.round(wy));
    }

    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (SwingUtilities.isRightMouseButton(e)) onRightPress(e);
                else onPress(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) { onRelease(e); }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) { onDrag(e); }
        });
        addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) editor.canvasZoomIn();
            else editor.canvasZoomOut();
        });
    }

    private void setupKeyListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT ||
                    code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) {
                    if (editor.getSelectedPartIds().isEmpty()) return;
                    boolean first = activeKeys.isEmpty();
                    activeKeys.add(code);
                    if (first) {
                        editor.saveState("Move Parts Keyboard");
                        startKeyMove();
                    }
                    e.consume();
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                activeKeys.remove(e.getKeyCode());
                if (activeKeys.isEmpty() && moveTimer != null) {
                    moveTimer.stop();
                    moveTimer = null;
                }
                e.consume();
            }
        });
    }

    private void startKeyMove() {
        if (moveTimer != null) moveTimer.stop();
        moveTimer = new javax.swing.Timer(30, e -> doKeyMove());
        moveTimer.start();
    }

    private void doKeyMove() {
        if (activeKeys.isEmpty()) { if (moveTimer != null) moveTimer.stop(); return; }
        int dx = 0, dy = 0;
        if (activeKeys.contains(KeyEvent.VK_LEFT) && !activeKeys.contains(KeyEvent.VK_RIGHT)) dx = -1;
        if (activeKeys.contains(KeyEvent.VK_RIGHT) && !activeKeys.contains(KeyEvent.VK_LEFT)) dx = 1;
        if (activeKeys.contains(KeyEvent.VK_UP) && !activeKeys.contains(KeyEvent.VK_DOWN)) dy = -1;
        if (activeKeys.contains(KeyEvent.VK_DOWN) && !activeKeys.contains(KeyEvent.VK_UP)) dy = 1;
        if (dx == 0 && dy == 0) return;

        editor.moveSelectedParts(dx, dy);
    }

    // --- Mouse handlers ---
    private void onPress(MouseEvent e) {
        int mx = e.getX(), my = e.getY();
        boolean ctrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;

        // 1. Check resize handles
        Object[] handleResult = findHandleAt(mx, my);
        if (handleResult != null) {
            resizingPart = (FramePart) handleResult[0];
            resizingHandle = (String) handleResult[1];
            resizingStartMouse = new Point(mx, my);
            SpriteInfo info = editor.findSpriteById(resizingPart.imgID);
            if (info != null) {
                if (resizingPart.customW <= 0) resizingPart.customW = info.w;
                if (resizingPart.customH <= 0) resizingPart.customH = info.h;
                resizeStartW = resizingPart.customW;
                resizeStartH = resizingPart.customH;
                resizeStartDx = resizingPart.dx;
                resizeStartDy = resizingPart.dy;
                editor.saveState("Resize Part");
            }
            return;
        }

        // 2. Check part hit
        FramePart p = findPartAt(mx, my);
        if (p != null) {
            if (ctrl) {
                if (editor.getSelectedPartIds().contains(p.partId))
                    editor.getSelectedPartIds().remove(p.partId);
                else
                    editor.getSelectedPartIds().add(p.partId);
            } else {
                if (!editor.getSelectedPartIds().contains(p.partId)) {
                    editor.getSelectedPartIds().clear();
                    editor.getSelectedPartIds().add(p.partId);
                }
            }
            dragging = true;
            dragHasMoved = false;
            dragLastPos = new Point(mx, my);
        } else {
            if (!ctrl) editor.getSelectedPartIds().clear();
            marqueeStart = new Point(mx, my);
            marqueeRect = new Rectangle(mx, my, 0, 0);
            dragging = false;
        }
        editor.syncPartsListSelection();
        repaint();
    }

    private void onRightPress(MouseEvent e) {
        FramePart p = findPartAt(e.getX(), e.getY());
        if (p != null) {
            if (!editor.getSelectedPartIds().contains(p.partId)) {
                editor.getSelectedPartIds().clear();
                editor.getSelectedPartIds().add(p.partId);
                editor.syncPartsListSelection();
            }
            dragging = true;
            dragHasMoved = false;
            dragLastPos = new Point(e.getX(), e.getY());
            repaint();
        }
    }

    private void onDrag(MouseEvent e) {
        int mx = e.getX(), my = e.getY();

        if (marqueeStart != null) {
            int x1 = Math.min(marqueeStart.x, mx), y1 = Math.min(marqueeStart.y, my);
            int x2 = Math.max(marqueeStart.x, mx), y2 = Math.max(marqueeStart.y, my);
            marqueeRect = new Rectangle(x1, y1, x2 - x1, y2 - y1);
            repaint();
        } else if (dragging && !editor.getSelectedPartIds().isEmpty()) {
            if (!dragHasMoved) {
                editor.saveState("Move Parts");
                dragHasMoved = true;
            }
            double wdx = (mx - dragLastPos.x) / canvasScale;
            double wdy = (my - dragLastPos.y) / canvasScale;
            dragLastPos = new Point(mx, my);

            for (FramePart p : editor.getCurrentFrameParts()) {
                if (editor.getSelectedPartIds().contains(p.partId)) {
                    p.dx += wdx;
                    p.dy += wdy;
                }
            }
            repaint();
        } else if (resizingPart != null) {
            doResize(mx, my);
        }
    }

    private void onRelease(MouseEvent e) {
        if (marqueeStart != null) {
            // Finalize marquee
            if (marqueeRect != null && marqueeRect.width > 2 && marqueeRect.height > 2) {
                for (FramePart p : editor.getCurrentFrameParts()) {
                    SpriteInfo info = editor.findSpriteById(p.imgID);
                    if (info == null) continue;
                    Point sp = worldToScreen(p.dx, p.dy);
                    int pw = (int)(info.w * canvasScale);
                    int ph = (int)(info.h * canvasScale);
                    if (sp.x < marqueeRect.x + marqueeRect.width && sp.x + pw > marqueeRect.x &&
                        sp.y < marqueeRect.y + marqueeRect.height && sp.y + ph > marqueeRect.y) {
                        editor.getSelectedPartIds().add(p.partId);
                    }
                }
                editor.syncPartsListSelection();
            }
            marqueeStart = null;
            marqueeRect = null;
            repaint();
        } else if (dragging || resizingPart != null) {
            // Round positions
            for (FramePart p : editor.getCurrentFrameParts()) {
                if (editor.getSelectedPartIds().contains(p.partId)) {
                    p.dx = Math.round(p.dx);
                    p.dy = Math.round(p.dy);
                }
            }
            if (resizingPart != null) {
                // Sync resize to SpriteInfo
                SpriteInfo info = editor.findSpriteById(resizingPart.imgID);
                if (info != null && resizingPart.customW > 0) {
                    info.w = (int) Math.round(resizingPart.customW);
                    info.h = (int) Math.round(resizingPart.customH);
                    // Clear custom sizes on all parts with same imgID
                    for (FrameData fd : editor.getFrameList()) {
                        for (FramePart fp : fd.parts) {
                            if (fp.imgID == resizingPart.imgID) fp.clearCustomSize();
                        }
                    }
                    editor.updateSpriteList();
                }
            }
            dragging = false;
            resizingPart = null;
            resizingHandle = null;
            repaint();
        }
    }

    private void doResize(int mx, int my) {
        if (resizingPart == null || resizingStartMouse == null) return;
        double dxs = mx - resizingStartMouse.x;
        double dys = my - resizingStartMouse.y;

        double scale;
        if (Math.abs(dxs) > Math.abs(dys)) {
            if ("tr".equals(resizingHandle) || "br".equals(resizingHandle))
                scale = (resizeStartW * canvasScale + dxs) / (resizeStartW * canvasScale);
            else
                scale = (resizeStartW * canvasScale - dxs) / (resizeStartW * canvasScale);
        } else {
            if ("bl".equals(resizingHandle) || "br".equals(resizingHandle))
                scale = (resizeStartH * canvasScale + dys) / (resizeStartH * canvasScale);
            else
                scale = (resizeStartH * canvasScale - dys) / (resizeStartH * canvasScale);
        }
        scale = Math.max(0.05, scale);
        double nw = resizeStartW * scale, nh = resizeStartH * scale;
        resizingPart.customW = nw;
        resizingPart.customH = nh;

        switch (resizingHandle) {
            case "br": resizingPart.dx = resizeStartDx; resizingPart.dy = resizeStartDy; break;
            case "tr": resizingPart.dx = resizeStartDx; resizingPart.dy = resizeStartDy + (resizeStartH - nh); break;
            case "bl": resizingPart.dx = resizeStartDx + (resizeStartW - nw); resizingPart.dy = resizeStartDy; break;
            case "tl": resizingPart.dx = resizeStartDx + (resizeStartW - nw); resizingPart.dy = resizeStartDy + (resizeStartH - nh); break;
        }
        repaint();
    }

    private FramePart findPartAt(int x, int y) {
        java.util.List<FramePart> parts = editor.getCurrentFrameParts();
        for (int i = parts.size() - 1; i >= 0; i--) {
            FramePart p = parts.get(i);
            SpriteInfo info = editor.findSpriteById(p.imgID);
            if (info == null) continue;
            Point sp = worldToScreen(p.dx, p.dy);
            double sw = p.getEffectiveW(info), sh = p.getEffectiveH(info);
            int pw = (int)(sw * canvasScale), ph = (int)(sh * canvasScale);
            if (x >= sp.x && x <= sp.x + pw && y >= sp.y && y <= sp.y + ph) return p;
        }
        return null;
    }

    private Object[] findHandleAt(int x, int y) {
        int hs = 8;
        java.util.List<FramePart> parts = editor.getCurrentFrameParts();
        for (int i = parts.size() - 1; i >= 0; i--) {
            FramePart p = parts.get(i);
            if (!editor.getSelectedPartIds().contains(p.partId)) continue;
            SpriteInfo info = editor.findSpriteById(p.imgID);
            if (info == null) continue;
            Point sp = worldToScreen(p.dx, p.dy);
            int sw = (int)(p.getEffectiveW(info) * canvasScale);
            int sh = (int)(p.getEffectiveH(info) * canvasScale);
            int px = sp.x, py = sp.y;
            String[][] handles = {{"tl", px+"", py+""}, {"tr", (px+sw)+"", py+""},
                                  {"bl", px+"", (py+sh)+""}, {"br", (px+sw)+"", (py+sh)+""}};
            for (String[] h : handles) {
                int hx = Integer.parseInt(h[1]), hy = Integer.parseInt(h[2]);
                if (Math.abs(x-hx) <= hs && Math.abs(y-hy) <= hs) return new Object[]{p, h[0]};
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;

        int cur = previewing ? previewIdx : editor.getCurrentFrameIndex();
        if (cur < 0 || cur >= editor.getFrameList().size()) return;

        // Background
        int cw = getCanvasW(), ch = getCanvasH(), ay = getAxisY();
        BufferedImage bg = EditorUtils.createCheckerboard(cw, ch, 20,
            new Color(200,200,200), new Color(160,160,160));
        g.drawImage(bg, 0, 0, null);

        int cx = cw / 2;
        // Axis
        g.setColor(new Color(80, 80, 80));
        g.drawLine(cx, 0, cx, ch);
        g.drawLine(0, ay, cw, ay);
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(2));
        g.drawLine(cx-5, ay, cx+5, ay);
        g.drawLine(cx, ay-5, cx, ay+5);
        g.setStroke(new BasicStroke(1));

        // Char reference
        if (charImg != null) {
            int sw = (int)(charImg.getWidth() * canvasScale);
            int sh = (int)(charImg.getHeight() * canvasScale);
            if (sw > 0 && sh > 0) {
                g.drawImage(charImg, 10, ay - sh, sw, sh, null);
            }
        }

        // Ghost previous frame
        if (cur > 0) drawFrame(g, cur - 1, 100, false);

        // Current frame
        drawFrame(g, cur, 255, editor.isShowBBox() && !previewing);

        // Marquee
        if (marqueeRect != null && !previewing) {
            g.setColor(new Color(0, 255, 255, 50));
            g.fillRect(marqueeRect.x, marqueeRect.y, marqueeRect.width, marqueeRect.height);
            g.setColor(Color.CYAN);
            g.setStroke(new BasicStroke(2));
            g.drawRect(marqueeRect.x, marqueeRect.y, marqueeRect.width, marqueeRect.height);
        }
    }

    private void drawFrame(Graphics2D g, int fIdx, int alpha, boolean drawBBox) {
        if (fIdx < 0 || fIdx >= editor.getFrameList().size()) return;
        BufferedImage atlas = editor.getAtlas();
        if (atlas == null) return;

        FrameData fd = editor.getFrameList().get(fIdx);
        for (FramePart p : fd.parts) {
            SpriteInfo info = editor.findSpriteById(p.imgID);
            if (info == null) continue;

            int sxs = info.getSrcX(), sys = info.getSrcY();
            int sws = info.getSrcW(), shs = info.getSrcH();
            double ew = p.getEffectiveW(info), eh = p.getEffectiveH(info);
            int sw = (int)(ew * canvasScale), sh = (int)(eh * canvasScale);
            if (sws <= 0 || shs <= 0 || sw <= 0 || sh <= 0) continue;

            // Crop from atlas
            int csx = Math.max(0, Math.min(sxs, atlas.getWidth() - 1));
            int csy = Math.max(0, Math.min(sys, atlas.getHeight() - 1));
            int csw = Math.min(sws, atlas.getWidth() - csx);
            int csh = Math.min(shs, atlas.getHeight() - csy);
            if (csw <= 0 || csh <= 0) continue;

            BufferedImage sprite = atlas.getSubimage(csx, csy, csw, csh);
            sprite = EditorUtils.resizeNearest(sprite, sw, sh);
            if (alpha < 255) sprite = EditorUtils.setAlpha(sprite, alpha);

            Point sp = worldToScreen(p.dx, p.dy);
            g.drawImage(sprite, sp.x, sp.y, null);

            if (drawBBox) {
                boolean sel = editor.getSelectedPartIds().contains(p.partId);
                g.setColor(sel ? Color.YELLOW : Color.GREEN);
                g.setStroke(new BasicStroke(sel ? 2 : 1));
                g.drawRect(sp.x, sp.y, sw, sh);
                if (sel) {
                    int hs = 6;
                    int[][] handles = {{sp.x, sp.y}, {sp.x+sw, sp.y}, {sp.x, sp.y+sh}, {sp.x+sw, sp.y+sh}};
                    for (int[] h : handles) {
                        g.setColor(Color.WHITE);
                        g.fillRect(h[0]-hs/2, h[1]-hs/2, hs, hs);
                        g.setColor(Color.BLUE);
                        g.drawRect(h[0]-hs/2, h[1]-hs/2, hs, hs);
                    }
                }
            }
        }
    }

    // --- Preview ---
    public void togglePreview() {
        if (previewing) stopPreview(); else startPreview();
    }

    public void startPreview() {
        if (editor.getFrameList().isEmpty()) return;
        previewing = true;
        previewIdx = 0;
        if (previewTimer != null) previewTimer.stop();
        previewTimer = new javax.swing.Timer(150, e -> {
            java.util.List<Integer> seq = editor.getArrFrame();
            if (seq.isEmpty()) { stopPreview(); return; }
            previewIdx = seq.get(previewIdx % seq.size());
            repaint();
            previewIdx = (editor.getArrFrame().indexOf(previewIdx) + 1) % seq.size();
        });
        previewTimer.start();
    }

    public void stopPreview() {
        previewing = false;
        if (previewTimer != null) { previewTimer.stop(); previewTimer = null; }
        repaint();
    }

    public boolean isPreviewing() { return previewing; }
}
