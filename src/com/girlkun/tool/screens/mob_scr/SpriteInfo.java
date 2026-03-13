package com.girlkun.tool.screens.mob_scr;

public class SpriteInfo {
    public int ID;
    public int x0, y0, w, h;
    public int srcX, srcY, srcW, srcH;
    public int anchorX, anchorY;

    public SpriteInfo(int id, int x0, int y0, int w, int h) {
        this.ID = id;
        this.x0 = x0; this.y0 = y0;
        this.w = w; this.h = h;
        this.srcX = x0; this.srcY = y0;
        this.srcW = w; this.srcH = h;
        this.anchorX = 0; this.anchorY = 0;
    }

    public SpriteInfo copy() {
        SpriteInfo c = new SpriteInfo(ID, x0, y0, w, h);
        c.srcX = srcX; c.srcY = srcY;
        c.srcW = srcW; c.srcH = srcH;
        c.anchorX = anchorX; c.anchorY = anchorY;
        return c;
    }

    public int getSrcX() { return srcX; }
    public int getSrcY() { return srcY; }
    public int getSrcW() { return srcW; }
    public int getSrcH() { return srcH; }
}
