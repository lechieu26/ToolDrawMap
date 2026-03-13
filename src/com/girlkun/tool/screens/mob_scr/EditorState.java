package com.girlkun.tool.screens.mob_scr;

import java.util.ArrayList;
import java.util.List;

/** Snapshot for Undo/Redo */
public class EditorState {
    public List<FrameData> frames;
    public int nextPartId;
    public String desc;

    public EditorState(List<FrameData> frames, int nextPartId, String desc) {
        this.frames = new ArrayList<>();
        for (FrameData f : frames) {
            this.frames.add(f.copy());
        }
        this.nextPartId = nextPartId;
        this.desc = desc;
    }
}
