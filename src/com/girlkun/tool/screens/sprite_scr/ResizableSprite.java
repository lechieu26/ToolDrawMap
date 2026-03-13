package com.girlkun.tool.screens.sprite_scr;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ResizableSprite {
    private FrameData data;
    private Rectangle bounds;
    private boolean selected;
    private final int HANDLE_SIZE = 8;
    
    public ResizableSprite(FrameData data) {
        this.data = data;
        updateBounds();
    }
    
    public void updateBounds() {
        this.bounds = new Rectangle((int)data.getX(), (int)data.getY(), 
                                   (int)data.getDisplayWidth(), (int)data.getDisplayHeight());
    }

    public FrameData getData() {
        return data;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public void draw(Graphics2D g2) {
        if (!data.isVisible()) return;
        
        g2.drawImage(data.getOriginalImage(), (int)data.getX(), (int)data.getY(), 
                     (int)data.getDisplayWidth(), (int)data.getDisplayHeight(), null);
        
        if (selected) {
            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5}, 0));
            g2.draw(bounds);
            
            // Size label
            g2.setColor(Color.YELLOW);
            g2.drawString((int)data.getDisplayWidth() + "x" + (int)data.getDisplayHeight(), 
                          (int)data.getX(), (int)data.getY() - 5);
            
            // Handles
            g2.setStroke(new BasicStroke(1));
            g2.setColor(Color.WHITE);
            for (Rectangle h : getHandles().values()) {
                g2.fill(h);
            }
            g2.setColor(Color.BLACK);
            for (Rectangle h : getHandles().values()) {
                g2.draw(h);
            }
        }
    }

    public Map<String, Rectangle> getHandles() {
        Map<String, Rectangle> h = new HashMap<>();
        int hs = HANDLE_SIZE;
        int hh = hs / 2;
        int x = bounds.x, y = bounds.y, w = bounds.width, h1 = bounds.height;
        h.put("tl", new Rectangle(x - hh, y - hh, hs, hs));
        h.put("t", new Rectangle(x + w / 2 - hh, y - hh, hs, hs));
        h.put("tr", new Rectangle(x + w - hh, y - hh, hs, hs));
        h.put("r", new Rectangle(x + w - hh, y + h1 / 2 - hh, hs, hs));
        h.put("br", new Rectangle(x + w - hh, y + h1 - hh, hs, hs));
        h.put("b", new Rectangle(x + w / 2 - hh, y + h1 - hh, hs, hs));
        h.put("bl", new Rectangle(x - hh, y + h1 - hh, hs, hs));
        h.put("l", new Rectangle(x - hh, y + h1 / 2 - hh, hs, hs));
        return h;
    }

    public String hitTest(Point p) {
        if (!selected) return (bounds != null && bounds.contains(p)) ? "move" : null;
        for (Map.Entry<String, Rectangle> entry : getHandles().entrySet()) {
            if (entry.getValue().contains(p)) return entry.getKey();
        }
        return (bounds != null && bounds.contains(p)) ? "move" : null;
    }
}
