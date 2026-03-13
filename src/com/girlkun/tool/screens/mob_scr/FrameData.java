package com.girlkun.tool.screens.mob_scr;

import java.util.ArrayList;
import java.util.List;

public class FrameData {
    public List<FramePart> parts = new ArrayList<>();

    public FrameData copy() {
        FrameData c = new FrameData();
        for (FramePart p : parts) {
            c.parts.add(p.copy());
        }
        return c;
    }
}
