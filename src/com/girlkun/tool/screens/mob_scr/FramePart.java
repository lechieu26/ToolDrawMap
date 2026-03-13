package com.girlkun.tool.screens.mob_scr;

public class FramePart {
    public int partId;
    public int imgID;
    public double dx, dy;
    public int z;
    public double customW = -1, customH = -1; // -1 means use sprite default

    public FramePart(int partId, int imgID, double dx, double dy, int z) {
        this.partId = partId;
        this.imgID = imgID;
        this.dx = dx; this.dy = dy;
        this.z = z;
    }

    public FramePart copy() {
        FramePart c = new FramePart(partId, imgID, dx, dy, z);
        c.customW = customW;
        c.customH = customH;
        return c;
    }

    public double getEffectiveW(SpriteInfo info) {
        return customW > 0 ? customW : info.w;
    }

    public double getEffectiveH(SpriteInfo info) {
        return customH > 0 ? customH : info.h;
    }

    public void clearCustomSize() {
        customW = -1;
        customH = -1;
    }
}
