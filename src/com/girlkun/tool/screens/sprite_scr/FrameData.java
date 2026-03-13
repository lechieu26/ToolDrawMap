package com.girlkun.tool.screens.sprite_scr;

import java.awt.image.BufferedImage;

public class FrameData {
    private BufferedImage originalImage;
    private double displayWidth;
    private double displayHeight;
    private double x;
    private double y;
    private boolean visible = true;

    public FrameData(BufferedImage image) {
        this.originalImage = image;
        this.displayWidth = image.getWidth();
        this.displayHeight = image.getHeight();
        // Default position: standing on Ox, x=50
        this.x = 50;
        this.y = -this.displayHeight;
    }

    public BufferedImage getOriginalImage() {
        return originalImage;
    }

    public double getDisplayWidth() {
        return displayWidth;
    }

    public void setDisplayWidth(double displayWidth) {
        this.displayWidth = displayWidth;
    }

    public double getDisplayHeight() {
        return displayHeight;
    }

    public void setDisplayHeight(double displayHeight) {
        this.displayHeight = displayHeight;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public String toString() {
        return String.format("Pos(%d,%d) Size(%dx%d)", 
                (int)x, (int)y, (int)displayWidth, (int)displayHeight);
    }
}
