package com.girlkun.tool.screens.mob_scr;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import com.girlkun.tool.main.Main;

public class MobEditor extends JInternalFrame {

    // Config
    private int currentScale = 4;

    // State
    private List<SpriteInfo> imgInfo = new ArrayList<>();
    private int nextImgId = 0;
    private List<FrameData> frames = new ArrayList<>();
    private int nextPartId = 0;
    private List<Integer> arrFrame = new ArrayList<>();
    private int currentFrameIndex = 0;
    private Set<Integer> selectedPartIds = new HashSet<>();
    private List<FramePart> clipboardParts = new ArrayList<>();
    private boolean showBBox = true;

    // Undo/Redo
    private List<EditorState> history = new ArrayList<>();
    private List<EditorState> redoStack = new ArrayList<>();
    private static final int MAX_HISTORY = 50;

    private static final String[] FRAME_NAMES = {
            "0: Stand", "1: Stand",
            "2: Move", "3: Move",
            "4: Attack 1", "5: Attack 1", "6: Attack 1",
            "7: Attack 2", "8: Attack 2", "9: Attack 2",
            "10: Hurt", "11: Die"
    };

    // UI Components
    private AtlasPanel atlasPanel;
    private FrameCanvas frameCanvas;
    private JList<String> frameListbox;
    private DefaultListModel<String> frameListModel;
    private JList<String> spriteListbox;
    private DefaultListModel<String> spriteListModel;
    private JList<String> partsListbox;
    private DefaultListModel<String> partsListModel;
    private JLabel spritePreviewLabel;
    private JLabel infoLabel;
    private JLabel canvasZoomLabel;
    private JButton btnPreview;
    private JTextField mobIdField, typeDataField;
    private ButtonGroup typeFlagGroup;
    private JComboBox<Integer> scaleCombo;
    private JCheckBox showBBoxCb;

    public MobEditor() {
        super("NRO Mob Data Editor", true, true, true, true);
        int h = Main.I.getDesktop().getHeight();
        setSize(1600, h - 80);
        buildUI();
        bindShortcuts();
        initFixedFrames();
    }

    // --- Accessors for child components ---
    public BufferedImage getAtlas() {
        return atlasPanel.getAtlas();
    }

    public List<SpriteInfo> getImgInfo() {
        return imgInfo;
    }

    public List<FrameData> getFrameList() {
        return frames;
    }

    public List<Integer> getArrFrame() {
        return arrFrame;
    }

    public int getCurrentFrameIndex() {
        return currentFrameIndex;
    }

    public Set<Integer> getSelectedPartIds() {
        return selectedPartIds;
    }

    public boolean isShowBBox() {
        return showBBox;
    }

    public SpriteInfo findSpriteById(int id) {
        for (SpriteInfo s : imgInfo)
            if (s.ID == id)
                return s;
        return null;
    }

    public List<FramePart> getCurrentFrameParts() {
        if (currentFrameIndex < 0 || currentFrameIndex >= frames.size())
            return Collections.emptyList();
        return frames.get(currentFrameIndex).parts;
    }

    // --- UI Build ---
    private void buildUI() {
        JPanel topBar = buildTopBar();
        getContentPane().add(topBar, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout(6, 6));
        main.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // LEFT: Atlas
        atlasPanel = new AtlasPanel(this);
        atlasPanel.setPreferredSize(new Dimension(350, 0));
        atlasPanel.setMinimumSize(new Dimension(250, 0));

        // MIDDLE: Frame editor
        JPanel midPanel = buildMiddlePanel();
        midPanel.setMinimumSize(new Dimension(600, 0));

        // RIGHT: Lists
        JPanel rightPanel = buildRightPanel();
        rightPanel.setPreferredSize(new Dimension(220, 0));
        rightPanel.setMinimumSize(new Dimension(200, 0));
        rightPanel.setPreferredSize(new Dimension(150, 0));
        rightPanel.setMinimumSize(new Dimension(100, 0));

        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, atlasPanel, midPanel);
        leftSplit.setDividerLocation(350);
        leftSplit.setResizeWeight(0.2);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightPanel);
        mainSplit.setResizeWeight(0.9); // Canvas mở rộng tối đa
        main.add(mainSplit, BorderLayout.CENTER);
        getContentPane().add(main, BorderLayout.CENTER);

        infoLabel = new JLabel("Ready");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        getContentPane().add(infoLabel, BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 4));
        top.setBorder(BorderFactory.createRaisedBevelBorder());

        JButton loadAtlasBtn = new JButton("📂 Load PNG Atlas");
        loadAtlasBtn.setBackground(new Color(102, 0, 153));
        loadAtlasBtn.setForeground(Color.WHITE);
        loadAtlasBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        loadAtlasBtn.addActionListener(e -> loadAtlas());
        top.add(loadAtlasBtn);

        JButton exportBtn = new JButton("🔧 Mob Extractor");
        exportBtn.setBackground(new Color(204, 51, 102));
        exportBtn.setForeground(Color.WHITE);
        exportBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        exportBtn.addActionListener(e -> exportAtlas());
        top.add(exportBtn);

        JButton loadDataBtn = new JButton("📁 Load Data (.bin)");
        loadDataBtn.setBackground(new Color(0, 153, 51));
        loadDataBtn.setForeground(Color.WHITE);
        loadDataBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        loadDataBtn.addActionListener(e -> loadMobData());
        top.add(loadDataBtn);

        JButton saveDataBtn = new JButton("💾 Save Data (.bin)");
        saveDataBtn.setBackground(new Color(204, 102, 0));
        saveDataBtn.setForeground(Color.WHITE);
        saveDataBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        saveDataBtn.addActionListener(e -> saveMobData());
        top.add(saveDataBtn);

        JButton undoBtn = new JButton("↩ Undo");
        undoBtn.setBackground(new Color(75, 0, 130));
        undoBtn.setForeground(Color.WHITE);
        undoBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        undoBtn.addActionListener(e -> undo());
        top.add(undoBtn);

        JButton redoBtn = new JButton("↪ Redo");
        redoBtn.setBackground(new Color(75, 0, 130));
        redoBtn.setForeground(Color.WHITE);
        redoBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        redoBtn.addActionListener(e -> redo());
        top.add(redoBtn);

        top.add(new JLabel(" | "));

        top.add(new JLabel("Mob ID:"));
        mobIdField = new JTextField("0", 5);
        top.add(mobIdField);

        top.add(new JLabel("  Type Flag:"));
        typeFlagGroup = new ButtonGroup();
        JRadioButton rb0 = new JRadioButton("0 (Byte)", true);
        rb0.setActionCommand("0");
        JRadioButton rb1 = new JRadioButton("1 (Boss)");
        rb1.setActionCommand("1");
        JRadioButton rb2 = new JRadioButton("2 (Short)");
        rb2.setActionCommand("2");
        typeFlagGroup.add(rb0);
        typeFlagGroup.add(rb1);
        typeFlagGroup.add(rb2);
        top.add(rb0);
        top.add(rb1);
        top.add(rb2);

        top.add(new JLabel("  Scale:"));
        scaleCombo = new JComboBox<>(new Integer[] { 1, 2, 3, 4 });
        scaleCombo.setSelectedItem(4);
        scaleCombo.addActionListener(e -> onScaleSelect());
        top.add(scaleCombo);

        top.add(new JLabel("  Type Data:"));
        typeDataField = new JTextField("0", 5);
        top.add(typeDataField);

        return top;
    }

    private JPanel buildMiddlePanel() {
        JPanel mid = new JPanel(new BorderLayout(5, 0));

        // Frame list
        frameListModel = new DefaultListModel<>();
        frameListbox = new JList<>(frameListModel);
        frameListbox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        frameListbox.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !isRefreshing) {
                int idx = frameListbox.getSelectedIndex();
                if (idx >= 0)
                    selectFrameIndex(idx);
            }
        });
        JScrollPane frameScroll = new JScrollPane(frameListbox);
        frameScroll.setPreferredSize(new Dimension(160, 0));
        JPanel frameListPanel = new JPanel(new BorderLayout());
        frameListPanel.add(new JLabel("Frames (Fixed)"), BorderLayout.NORTH);
        frameListPanel.add(frameScroll, BorderLayout.CENTER);
        mid.add(frameListPanel, BorderLayout.WEST);

        // Canvas area
        JPanel canvasArea = new JPanel(new BorderLayout());
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        showBBoxCb = new JCheckBox("Show BBox", true);
        showBBoxCb.addActionListener(e -> {
            showBBox = showBBoxCb.isSelected();
            frameCanvas.repaint();
        });
        toolbar.add(showBBoxCb);
        toolbar.add(new JLabel(" | "));
        JButton zIn = new JButton("🔍+");
        zIn.setBackground(new Color(0, 153, 51));
        zIn.addActionListener(e -> canvasZoomIn());
        JButton zOut = new JButton("🔍-");
        zOut.setBackground(new Color(0, 153, 51));
        zOut.addActionListener(e -> canvasZoomOut());
        canvasZoomLabel = new JLabel("100%");
        toolbar.add(zIn);
        toolbar.add(zOut);
        toolbar.add(canvasZoomLabel);

        btnPreview = new JButton("▶ Run Preview");
        btnPreview.setBackground(new Color(0x81, 0xC7, 0x84));
        btnPreview.setForeground(new Color(0x1B, 0x5E, 0x20));
        btnPreview.setFont(btnPreview.getFont().deriveFont(Font.BOLD));
        btnPreview.addActionListener(e -> togglePreview());
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(btnPreview);
        canvasArea.add(toolbar, BorderLayout.NORTH);

        frameCanvas = new FrameCanvas(this);
        canvasArea.add(frameCanvas, BorderLayout.CENTER);
        mid.add(canvasArea, BorderLayout.CENTER);

        return mid;
    }

    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout(0, 5));

        // Sprites
        JPanel spritePanel = new JPanel(new BorderLayout());
        spritePanel.setBorder(BorderFactory.createTitledBorder("Sprites (ImageInfo)"));
        spriteListModel = new DefaultListModel<>();
        spriteListbox = new JList<>(spriteListModel);
        spriteListbox.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        spriteListbox.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                onSpriteSelect();
        });
        spritePanel.add(new JScrollPane(spriteListbox), BorderLayout.CENTER);

        JPanel spTools = new JPanel(new BorderLayout(4, 4));
        BufferedImage placeholder = EditorUtils.createCheckerboard(100, 100);
        spritePreviewLabel = new JLabel(new ImageIcon(placeholder));
        spritePreviewLabel.setPreferredSize(new Dimension(100, 100));
        spTools.add(spritePreviewLabel, BorderLayout.WEST);

        JPanel spBtns = new JPanel(new GridLayout(2, 1, 2, 2));
        JButton addToFrame = new JButton("Add");
        addToFrame.setBackground(new Color(0, 153, 51));
        addToFrame.setForeground(Color.WHITE);
        addToFrame.addActionListener(e -> addSelectedSpritesToFrame());

        JButton delSprite = new JButton("Del");
        delSprite.setBackground(new Color(211, 47, 47));
        delSprite.setForeground(Color.WHITE);
        delSprite.addActionListener(e -> deleteSelectedSprite());
        spBtns.add(addToFrame);
        spBtns.add(delSprite);
        spTools.add(spBtns, BorderLayout.CENTER);
        spritePanel.add(spTools, BorderLayout.SOUTH);

        // Parts
        JPanel partsPanel = new JPanel(new BorderLayout());
        partsPanel.setBorder(BorderFactory.createTitledBorder("Parts in Frame"));
        partsListModel = new DefaultListModel<>();
        partsListbox = new JList<>(partsListModel);
        partsListbox.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        partsListbox.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                onPartListSelect();
        });
        partsPanel.add(new JScrollPane(partsListbox), BorderLayout.CENTER);

        JPanel partTools = new JPanel(new GridLayout(2, 3, 2, 2));
        JButton selAll = new JButton("All");
        selAll.setBackground(new Color(0, 120, 212));
        selAll.setForeground(Color.WHITE);
        selAll.addActionListener(e -> selectAllParts());

        JButton copyBtn = new JButton("Copy");
        copyBtn.setBackground(new Color(102, 0, 153));
        copyBtn.setForeground(Color.WHITE);
        copyBtn.addActionListener(e -> copyParts());

        JButton pasteBtn = new JButton("Paste");
        pasteBtn.setBackground(new Color(102, 0, 153));
        pasteBtn.setForeground(Color.WHITE);
        pasteBtn.addActionListener(e -> pasteParts());

        JButton delPart = new JButton("Del");
        delPart.setBackground(new Color(211, 47, 47));
        delPart.setForeground(Color.WHITE);
        delPart.addActionListener(e -> deleteSelectedPart());

        JButton downBtn = new JButton("▼");
        downBtn.setBackground(new Color(204, 102, 0));
        downBtn.setForeground(Color.WHITE);
        downBtn.addActionListener(e -> bringFront());

        JButton upBtn = new JButton("▲");
        upBtn.setBackground(new Color(204, 102, 0));
        upBtn.setForeground(Color.WHITE);
        upBtn.addActionListener(e -> sendBack());
        partTools.add(selAll);
        partTools.add(copyBtn);
        partTools.add(pasteBtn);
        partTools.add(delPart);
        partTools.add(downBtn);
        partTools.add(upBtn);
        partsPanel.add(partTools, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spritePanel, partsPanel);
        split.setDividerLocation(350);
        split.setResizeWeight(0.5);
        right.add(split, BorderLayout.CENTER);

        return right;
    }

    private void bindShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke("control Z"), "undo");
        am.put("undo", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                undo();
            }
        });
        im.put(KeyStroke.getKeyStroke("control Y"), "redo");
        am.put("redo", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                redo();
            }
        });
        im.put(KeyStroke.getKeyStroke("control C"), "copy");
        am.put("copy", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                copyParts();
            }
        });
        im.put(KeyStroke.getKeyStroke("control V"), "paste");
        am.put("paste", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                pasteParts();
            }
        });
        im.put(KeyStroke.getKeyStroke("DELETE"), "del");
        am.put("del", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                deleteSelectedPart();
            }
        });
        im.put(KeyStroke.getKeyStroke("ENTER"), "cut");
        am.put("cut", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                atlasPanel.performCut();
            }
        });
        im.put(KeyStroke.getKeyStroke("control A"), "selAll");
        am.put("selAll", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                selectAllParts();
            }
        });
    }

    private void initFixedFrames() {
        frames.clear();
        for (int i = 0; i < 12; i++)
            frames.add(new FrameData());
        arrFrame.clear();
        for (int i = 0; i < 12; i++)
            arrFrame.add(i);
        currentFrameIndex = 0;
        refreshFrameList();
        selectFrameIndex(0);
    }

    private void resetState() {
        imgInfo.clear();
        nextImgId = 0;
        nextPartId = 0;
        history.clear();
        redoStack.clear();
        atlasPanel.setAtlas(null);
        initFixedFrames();
        updateSpriteList();
        updatePartsList();
        infoLabel.setText("State reset.");
    }

    private boolean isRefreshing = false;

    void refreshFrameList() {
        if (frameListbox == null)
            return;
        isRefreshing = true;
        try {
            int sel = currentFrameIndex;
            frameListModel.clear();
            for (int i = 0; i < 12; i++) {
                String label = i < FRAME_NAMES.length ? FRAME_NAMES[i] : i + ": Extra";
                if (i < frames.size())
                    label += " (" + frames.get(i).parts.size() + " parts)";
                frameListModel.addElement(label);
            }
            if (sel >= 0 && sel < frameListModel.size())
                frameListbox.setSelectedIndex(sel);
        } finally {
            isRefreshing = false;
        }
    }

    void selectFrameIndex(int idx) {
        idx = EditorUtils.clamp(idx, 0, Math.max(0, frames.size() - 1));
        currentFrameIndex = idx;
        selectedPartIds.clear();
        if (frameListbox != null && frameListModel.size() > 0 && idx < frameListModel.size()) {
            if (frameListbox.getSelectedIndex() != idx)
                frameListbox.setSelectedIndex(idx);
        }
        updatePartsList();
        if (frameCanvas != null)
            frameCanvas.repaint();
    }

    public void addSprite(int x0, int y0, int w, int h) {
        int id = nextImgId++;
        imgInfo.add(new SpriteInfo(id, x0, y0, w, h));
        updateSpriteList();
        if (spriteListModel.size() > 0) {
            spriteListbox.setSelectedIndex(spriteListModel.size() - 1);
            spriteListbox.ensureIndexIsVisible(spriteListModel.size() - 1);
        }
        createFrameForSprite(id);
    }

    void updateSpriteList() {
        spriteListModel.clear();
        for (SpriteInfo s : imgInfo) {
            spriteListModel.addElement("ID " + s.ID + ": " + s.w + "x" + s.h + " at (" + s.x0 + "," + s.y0 + ")");
        }
    }

    private void onSpriteSelect() {
        int idx = spriteListbox.getSelectedIndex();
        if (idx < 0 || idx >= imgInfo.size()) {
            spritePreviewLabel.setIcon(new ImageIcon(EditorUtils.createCheckerboard(100, 100)));
            return;
        }
        SpriteInfo info = imgInfo.get(idx);
        BufferedImage atlas = getAtlas();
        if (atlas != null) {
            try {
                int sx = info.getSrcX(), sy = info.getSrcY();
                int sw = info.getSrcW(), sh = info.getSrcH();
                sx = Math.max(0, Math.min(sx, atlas.getWidth() - 1));
                sy = Math.max(0, Math.min(sy, atlas.getHeight() - 1));
                sw = Math.min(sw, atlas.getWidth() - sx);
                sh = Math.min(sh, atlas.getHeight() - sy);
                if (sw > 0 && sh > 0) {
                    BufferedImage region = atlas.getSubimage(sx, sy, sw, sh);
                    int PW = 100, PH = 100;
                    double fit = Math.min((double) PW / Math.max(1, info.w), (double) PH / Math.max(1, info.h));
                    int nw = (int) (info.w * fit), nh = (int) (info.h * fit);
                    BufferedImage bg = EditorUtils.createCheckerboard(PW, PH);
                    if (nw > 0 && nh > 0) {
                        region = EditorUtils.resizeNearest(region, nw, nh);
                        Graphics2D g = bg.createGraphics();
                        g.drawImage(region, (PW - nw) / 2, (PH - nh) / 2, null);
                        g.dispose();
                    }
                    spritePreviewLabel.setIcon(new ImageIcon(bg));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void addSelectedSpritesToFrame() {
        int[] sel = spriteListbox.getSelectedIndices();
        if (sel.length == 0)
            return;
        saveState("Add Sprites to Frame");
        if (currentFrameIndex >= frames.size())
            return;
        int added = 0;
        for (int idx : sel) {
            SpriteInfo info = imgInfo.get(idx);
            int off = added * 5;
            FramePart part = new FramePart(nextPartId++, info.ID,
                    -info.w / 2 + off, -info.h + off, frames.get(currentFrameIndex).parts.size());
            frames.get(currentFrameIndex).parts.add(part);
            added++;
        }
        updatePartsList();
        frameCanvas.repaint();
        infoLabel.setText("Added " + added + " sprites to Frame " + currentFrameIndex);
    }

    private void deleteSelectedSprite() {
        int[] sel = spriteListbox.getSelectedIndices();
        if (sel.length == 0)
            return;
        if (sel.length > 1
                && JOptionPane.showConfirmDialog(this, "Delete " + sel.length + " sprites?") != JOptionPane.YES_OPTION)
            return;

        Set<Integer> toDelete = new HashSet<>();
        for (int idx : sel)
            toDelete.add(idx);

        Map<Integer, Integer> idMap = new HashMap<>();
        List<SpriteInfo> newInfo = new ArrayList<>();
        int newId = 0;
        for (int i = 0; i < imgInfo.size(); i++) {
            int oldId = imgInfo.get(i).ID;
            if (toDelete.contains(i)) {
                idMap.put(oldId, -1);
            } else {
                imgInfo.get(i).ID = newId;
                idMap.put(oldId, newId);
                newInfo.add(imgInfo.get(i));
                newId++;
            }
        }
        imgInfo = newInfo;
        nextImgId = newId;

        for (FrameData fd : frames) {
            fd.parts.removeIf(p -> {
                Integer mapped = idMap.get(p.imgID);
                if (mapped == null || mapped == -1)
                    return true;
                p.imgID = mapped;
                return false;
            });
        }
        updateSpriteList();
        frameCanvas.repaint();
        infoLabel.setText("Deleted " + toDelete.size() + " sprites and re-indexed.");
    }

    private void createFrameForSprite(int imgID) {
        int cur = EditorUtils.clamp(currentFrameIndex, 0, frames.size() - 1);
        FramePart part = new FramePart(nextPartId++, imgID, 0, 0, frames.get(cur).parts.size());
        frames.get(cur).parts.add(part);
        selectedPartIds.clear();
        selectedPartIds.add(part.partId);
        int next = cur;
        if (atlasPanel.isAutoNext() && cur < 11)
            next = cur + 1;
        selectFrameIndex(next);
        refreshFrameList();
    }

    void updatePartsList() {
        partsListModel.clear();
        if (currentFrameIndex >= frames.size())
            return;
        List<Integer> selIndices = new ArrayList<>();
        List<FramePart> parts = frames.get(currentFrameIndex).parts;
        for (int i = 0; i < parts.size(); i++) {
            FramePart p = parts.get(i);
            partsListModel
                    .addElement("Part " + p.partId + ": img " + p.imgID + " (" + (int) p.dx + "," + (int) p.dy + ")");
            if (selectedPartIds.contains(p.partId))
                selIndices.add(i);
        }
        if (!selIndices.isEmpty()) {
            for (int idx : selIndices)
                partsListbox.addSelectionInterval(idx, idx);
            partsListbox.ensureIndexIsVisible(selIndices.get(selIndices.size() - 1));
        }
        refreshFrameList();
    }

    public void syncPartsListSelection() {
        partsListbox.clearSelection();
        if (currentFrameIndex >= frames.size())
            return;
        List<FramePart> parts = frames.get(currentFrameIndex).parts;
        for (int i = 0; i < parts.size(); i++) {
            if (selectedPartIds.contains(parts.get(i).partId))
                partsListbox.addSelectionInterval(i, i);
        }
    }

    private void onPartListSelect() {
        int[] sel = partsListbox.getSelectedIndices();
        if (sel.length == 0)
            return;
        selectedPartIds.clear();
        List<FramePart> parts = frames.get(currentFrameIndex).parts;
        for (int idx : sel) {
            if (idx < parts.size())
                selectedPartIds.add(parts.get(idx).partId);
        }
        if (sel.length > 0) {
            int lastIdx = sel[sel.length - 1];
            if (lastIdx < parts.size()) {
                int imgID = parts.get(lastIdx).imgID;
                for (int i = 0; i < imgInfo.size(); i++) {
                    if (imgInfo.get(i).ID == imgID) {
                        spriteListbox.setSelectedIndex(i);
                        spriteListbox.ensureIndexIsVisible(i);
                        onSpriteSelect();
                        break;
                    }
                }
            }
        }
        frameCanvas.repaint();
    }

    private void deleteSelectedPart() {
        int[] sel = partsListbox.getSelectedIndices();
        if (sel.length == 0)
            return;
        saveState("Delete Part(s)");
        List<FramePart> parts = frames.get(currentFrameIndex).parts;
        for (int i = sel.length - 1; i >= 0; i--) {
            if (sel[i] < parts.size())
                parts.remove(sel[i]);
        }
        updatePartsList();
        frameCanvas.repaint();
    }

    private void bringFront() {
        int idx = partsListbox.getSelectedIndex();
        if (idx < 0)
            return;
        List<FramePart> parts = frames.get(currentFrameIndex).parts;
        if (idx < parts.size() - 1) {
            saveState("Bring Front");
            Collections.swap(parts, idx, idx + 1);
            updatePartsList();
            partsListbox.setSelectedIndex(idx + 1);
            frameCanvas.repaint();
        }
    }

    private void sendBack() {
        int idx = partsListbox.getSelectedIndex();
        if (idx <= 0)
            return;
        List<FramePart> parts = frames.get(currentFrameIndex).parts;
        saveState("Send Back");
        Collections.swap(parts, idx, idx - 1);
        updatePartsList();
        partsListbox.setSelectedIndex(idx - 1);
        frameCanvas.repaint();
    }

    public void moveSelectedParts(int dx, int dy) {
        for (FramePart p : getCurrentFrameParts()) {
            if (selectedPartIds.contains(p.partId)) {
                p.dx += dx;
                p.dy += dy;
            }
        }
        updatePartsList();
        frameCanvas.repaint();
    }

    private void selectAllParts() {
        if (currentFrameIndex >= frames.size())
            return;
        selectedPartIds.clear();
        for (FramePart p : frames.get(currentFrameIndex).parts)
            selectedPartIds.add(p.partId);
        if (partsListModel.size() > 0)
            partsListbox.setSelectionInterval(0, partsListModel.size() - 1);
        frameCanvas.repaint();
    }

    private void copyParts() {
        int[] sel = partsListbox.getSelectedIndices();
        if (sel.length == 0) {
            infoLabel.setText("No parts selected to copy.");
            return;
        }
        clipboardParts.clear();
        List<FramePart> parts = frames.get(currentFrameIndex).parts;
        for (int idx : sel) {
            if (idx < parts.size())
                clipboardParts.add(parts.get(idx).copy());
        }
        infoLabel.setText("Copied " + clipboardParts.size() + " parts.");
    }

    private void pasteParts() {
        if (clipboardParts.isEmpty()) {
            infoLabel.setText("Clipboard empty.");
            return;
        }
        if (currentFrameIndex >= frames.size())
            return;
        saveState("Paste Parts");
        int count = 0;
        for (FramePart p : clipboardParts) {
            FramePart np = p.copy();
            np.partId = nextPartId++;
            np.dx += 10;
            np.dy += 10;
            np.z = frames.get(currentFrameIndex).parts.size();
            frames.get(currentFrameIndex).parts.add(np);
            count++;
        }
        updatePartsList();
        frameCanvas.repaint();
        infoLabel.setText("Pasted " + count + " parts.");
    }

    void canvasZoomIn() {
        double s = Math.min(4.0, frameCanvas.getCanvasScale() + 0.25);
        frameCanvas.setCanvasScale(s);
        canvasZoomLabel.setText((int) (s * 100) + "%");
    }

    void canvasZoomOut() {
        double s = Math.max(0.25, frameCanvas.getCanvasScale() - 0.25);
        frameCanvas.setCanvasScale(s);
        canvasZoomLabel.setText((int) (s * 100) + "%");
    }

    private void togglePreview() {
        frameCanvas.togglePreview();
        if (frameCanvas.isPreviewing()) {
            btnPreview.setText("■ Stop Preview");
            btnPreview.setBackground(new Color(0xEF, 0x53, 0x50));
            btnPreview.setForeground(Color.WHITE);
        } else {
            btnPreview.setText("▶ Run Preview");
            btnPreview.setBackground(new Color(0x81, 0xC7, 0x84));
            btnPreview.setForeground(new Color(0x1B, 0x5E, 0x20));
        }
    }

    public void saveState(String desc) {
        history.add(new EditorState(frames, nextPartId, desc));
        if (history.size() > MAX_HISTORY)
            history.remove(0);
        redoStack.clear();
    }

    private void undo() {
        if (history.isEmpty()) {
            infoLabel.setText("Nothing to undo.");
            return;
        }
        redoStack.add(new EditorState(frames, nextPartId, "Current"));
        EditorState s = history.remove(history.size() - 1);
        frames = s.frames;
        nextPartId = s.nextPartId;
        infoLabel.setText("Undid: " + s.desc);
        refreshFrameList();
        selectFrameIndex(currentFrameIndex);
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            infoLabel.setText("Nothing to redo.");
            return;
        }
        history.add(new EditorState(frames, nextPartId, "Redo_Pre"));
        EditorState s = redoStack.remove(redoStack.size() - 1);
        frames = s.frames;
        nextPartId = s.nextPartId;
        infoLabel.setText("Redid change.");
        refreshFrameList();
        selectFrameIndex(currentFrameIndex);
    }

    private void onScaleSelect() {
        int newScale = (Integer) scaleCombo.getSelectedItem();
        if (newScale == currentScale)
            return;
        double ratio = (double) newScale / currentScale;
        Boolean ans = AskScaleDialog.show(this, currentScale, newScale, ratio);
        if (ans == null) {
            scaleCombo.setSelectedItem(currentScale);
            return;
        }
        if (ans) {
            for (SpriteInfo s : imgInfo) {
                s.x0 = (int) Math.round(s.x0 * ratio);
                s.y0 = (int) Math.round(s.y0 * ratio);
                s.w = (int) Math.round(s.w * ratio);
                s.h = (int) Math.round(s.h * ratio);
                s.srcX = (int) Math.round(s.srcX * ratio);
                s.srcY = (int) Math.round(s.srcY * ratio);
                s.srcW = (int) Math.round(s.srcW * ratio);
                s.srcH = (int) Math.round(s.srcH * ratio);
            }
            for (FrameData fd : frames)
                for (FramePart p : fd.parts) {
                    p.dx = Math.round(p.dx * ratio);
                    p.dy = Math.round(p.dy * ratio);
                }
        }
        currentScale = newScale;
        updateSpriteList();
        updatePartsList();
        frameCanvas.repaint();
        atlasPanel.updateDisplay();
        infoLabel.setText("Switched to Scale x" + newScale);
    }

    private void loadAtlas() {
        if (!imgInfo.isEmpty() || frames.stream().anyMatch(f -> !f.parts.isEmpty())) {
            if (JOptionPane.showConfirmDialog(this,
                    "Loading new Atlas will reset. Continue?") != JOptionPane.YES_OPTION)
                return;
        }
        FileDialog fd = new FileDialog(Main.I, "Select Atlas PNG", FileDialog.LOAD);
        fd.setFile("*.png");
        fd.setVisible(true);
        if (fd.getFile() == null)
            return;
        try {
            File f = new File(fd.getDirectory(), fd.getFile());
            BufferedImage img = ImageIO.read(f);
            resetState();
            atlasPanel.setAtlas(img);
            scaleCombo.setSelectedItem(4);
            currentScale = 4;
            infoLabel.setText("Atlas loaded: " + f.getName() + " (" + img.getWidth() + "x" + img.getHeight() + ")");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed: " + ex.getMessage());
        }
    }

    private void exportAtlas() {
        JDesktopPane desktop = getDesktopPane();
        if (desktop == null) {
            // Fallback if not inside a desktop pane yet
            return;
        }

        // Check if MobExtractor is already open
        for (JInternalFrame frame : desktop.getAllFrames()) {
            if (frame instanceof MobExtractor) {
                try {
                    if (frame.isIcon())
                        frame.setIcon(false);
                    frame.setSelected(true);
                    frame.moveToFront();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return;
            }
        }

        MobExtractor extractor = new MobExtractor();
        desktop.add(extractor);
        extractor.setLocation(50, 50);
        extractor.setVisible(true);
        try {
            extractor.setSelected(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private int getTypeFlag() {
        if (typeFlagGroup.getSelection() == null)
            return 0;
        return Integer.parseInt(typeFlagGroup.getSelection().getActionCommand());
    }

    private void setTypeFlag(int f) {
        for (var e = typeFlagGroup.getElements(); e.hasMoreElements();) {
            AbstractButton b = e.nextElement();
            if (b.getActionCommand().equals(String.valueOf(f))) {
                b.setSelected(true);
                break;
            }
        }
    }

    private void saveMobData() {
        if (getAtlas() == null) {
            JOptionPane.showMessageDialog(this, "Atlas required!");
            return;
        }
        FileDialog fd = new FileDialog(Main.I, "Save Bin File", FileDialog.SAVE);
        fd.setFile("*.bin");
        fd.setVisible(true);
        if (fd.getFile() == null)
            return;

        String fileName = fd.getFile();
        // Loại bỏ phần mở rộng nếu người dùng lỡ nhập vào
        if (fileName.contains(".")) {
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        }

        File selectedFile = new File(fd.getDirectory(), fileName);
        String stem = fileName; // Tên file không có extension
        File parentDir = selectedFile.getParentFile();

        try {
            int scaleSetting = (Integer) scaleCombo.getSelectedItem();
            int tFlag = getTypeFlag();

            int MAX_W = 256;
            int curX = 0, curY = 0, rowH = 0, totalW = 0, totalH = 0;
            int[][] coords = new int[imgInfo.size()][4];
            BufferedImage[] x1Images = new BufferedImage[imgInfo.size()];

            for (int i = 0; i < imgInfo.size(); i++) {
                SpriteInfo s = imgInfo.get(i);
                int sx = s.getSrcX(), sy = s.getSrcY(), sw = s.getSrcW(), sh = s.getSrcH();
                BufferedImage atlas = getAtlas();
                sx = Math.max(0, Math.min(sx, atlas.getWidth() - 1));
                sy = Math.max(0, Math.min(sy, atlas.getHeight() - 1));
                sw = Math.min(sw, atlas.getWidth() - sx);
                sh = Math.min(sh, atlas.getHeight() - sy);
                BufferedImage region = (sw > 0 && sh > 0) ? atlas.getSubimage(sx, sy, sw, sh)
                        : new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                int tw = Math.max(1, (int) Math.round((double) s.w / scaleSetting));
                int th = Math.max(1, (int) Math.round((double) s.h / scaleSetting));
                x1Images[i] = EditorUtils.resizeNearest(region, tw, th);

                if (curX + tw > MAX_W && curX > 0) {
                    curX = 0;
                    curY += rowH;
                    rowH = 0;
                }
                coords[i] = new int[] { curX, curY, tw, th };
                curX += tw;
                rowH = Math.max(rowH, th);
                totalW = Math.max(totalW, curX);
                totalH = Math.max(totalH, curY + rowH);
            }

            int maxCoord = 0;
            for (int[] c : coords)
                maxCoord = Math.max(maxCoord, Math.max(c[0], c[1]));
            if (maxCoord > 255 && tFlag != 2) {
                if (JOptionPane.showConfirmDialog(this, "Coords exceed 255. Use Type 2?") == JOptionPane.YES_OPTION) {
                    tFlag = 2;
                    setTypeFlag(2);
                }
            }

            ByteArrayOutputStream metaBuf = new ByteArrayOutputStream();
            DataOutputStream metaOut = new DataOutputStream(metaBuf);
            metaOut.writeByte(imgInfo.size());
            for (int i = 0; i < imgInfo.size(); i++) {
                SpriteInfo s = imgInfo.get(i);
                metaOut.writeByte(s.ID);
                if (tFlag == 0 || tFlag == 1) {
                    metaOut.writeByte(Math.min(255, Math.max(0, coords[i][0])));
                    metaOut.writeByte(Math.min(255, Math.max(0, coords[i][1])));
                } else {
                    metaOut.writeShort(coords[i][0]);
                    metaOut.writeShort(coords[i][1]);
                }
                metaOut.writeByte(Math.min(255, Math.max(0, coords[i][2])));
                metaOut.writeByte(Math.min(255, Math.max(0, coords[i][3])));
            }
            metaOut.writeShort(frames.size());
            for (FrameData frame : frames) {
                metaOut.writeByte(frame.parts.size());
                for (FramePart p : frame.parts) {
                    metaOut.writeShort((int) Math.round(p.dx / scaleSetting));
                    metaOut.writeShort((int) Math.round(p.dy / scaleSetting));
                    metaOut.writeByte(p.imgID);
                }
            }
            metaOut.writeShort(arrFrame.size());
            for (int idx : arrFrame)
                metaOut.writeShort(idx);
            metaOut.flush();
            byte[] metaBytes = metaBuf.toByteArray();

            int tData = 0;
            try {
                tData = Integer.parseInt(typeDataField.getText().trim());
            } catch (Exception ignored) {
            }

            for (int sf = 1; sf <= 4; sf++) {
                int tw = Math.max(1, totalW * sf), th = Math.max(1, totalH * sf);
                BufferedImage finalAtlas = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = finalAtlas.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                // Crop trực tiếp từ atlas gốc cho mỗi scale factor (giống Python)
                // thay vì scale up từ x1Images (gây mất chất lượng + sai size)
                for (int i = 0; i < imgInfo.size(); i++) {
                    SpriteInfo si = imgInfo.get(i);
                    int rsx = si.getSrcX(), rsy = si.getSrcY(), rsw = si.getSrcW(), rsh = si.getSrcH();
                    BufferedImage atlasImg = getAtlas();
                    rsx = Math.max(0, Math.min(rsx, atlasImg.getWidth() - 1));
                    rsy = Math.max(0, Math.min(rsy, atlasImg.getHeight() - 1));
                    rsw = Math.min(rsw, atlasImg.getWidth() - rsx);
                    rsh = Math.min(rsh, atlasImg.getHeight() - rsy);
                    if (rsw > 0 && rsh > 0) {
                        BufferedImage region = atlasImg.getSubimage(rsx, rsy, rsw, rsh);
                        int dw = Math.max(1, coords[i][2] * sf), dh = Math.max(1, coords[i][3] * sf);
                        BufferedImage rs = EditorUtils.resizeNearest(region, dw, dh);
                        g.drawImage(rs, coords[i][0] * sf, coords[i][1] * sf, null);
                    }
                }
                g.dispose();

                ByteArrayOutputStream pngBuf = new ByteArrayOutputStream();
                ImageIO.write(finalAtlas, "PNG", pngBuf);
                byte[] pngData = pngBuf.toByteArray();

                ByteArrayOutputStream finalBuf = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(finalBuf);
                out.writeByte(tFlag);
                out.writeInt(metaBytes.length);
                out.write(metaBytes);
                out.writeInt(pngData.length);
                out.write(pngData);
                out.writeByte(tData);
                out.flush();

                File xDir = new File(parentDir, "x" + sf);
                if (!xDir.exists())
                    xDir.mkdirs();
                File outFile = new File(xDir, stem); // Không có .bin
                Files.write(outFile.toPath(), finalBuf.toByteArray());

                // Lưu file gốc tại vị trí đã chọn
                if (sf == 1) {
                    Files.write(selectedFile.toPath(), finalBuf.toByteArray());
                }
            }

            JOptionPane.showMessageDialog(this, "Saved to x1-x4 subfolders.");
            try {
                Desktop.getDesktop().open(parentDir);
            } catch (Exception ignored) {
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void loadMobData() {
        FileDialog fd = new FileDialog(Main.I, "Open Mob Data", FileDialog.LOAD);
        fd.setVisible(true);
        if (fd.getFile() == null)
            return;
        File file = new File(fd.getDirectory(), fd.getFile());

        try {
            String n = file.getName().replaceAll("\\.[^.]+$", "");
            if (n.matches("\\d+"))
                mobIdField.setText(n);
        } catch (Exception ignored) {
        }

        try {
            int scale = (Integer) scaleCombo.getSelectedItem();
            byte[] data = Files.readAllBytes(file.toPath());
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

            int tFlag = dis.readByte();
            setTypeFlag(tFlag);
            int dLen = dis.readInt();
            int nImg = dis.readUnsignedByte();

            imgInfo.clear();
            frames.clear();
            arrFrame.clear();
            int maxImgId = -1;

            for (int i = 0; i < nImg; i++) {
                int id = dis.readByte();
                int x0, y0;
                if (tFlag == 2) {
                    x0 = dis.readShort();
                    y0 = dis.readShort();
                } else {
                    x0 = dis.readUnsignedByte();
                    y0 = dis.readUnsignedByte();
                }
                int w = dis.readUnsignedByte(), h = dis.readUnsignedByte();

                SpriteInfo s = new SpriteInfo(id, x0 * scale, y0 * scale, w * scale, h * scale);
                s.srcX = x0 * scale;
                s.srcY = y0 * scale;
                s.srcW = w * scale;
                s.srcH = h * scale;
                imgInfo.add(s);
                maxImgId = Math.max(maxImgId, id);
            }
            nextImgId = maxImgId + 1;

            for (int i = 0; i < 12; i++)
                frames.add(new FrameData());

            int nFrames = dis.readShort();
            for (int fi = 0; fi < nFrames; fi++) {
                if (fi >= 12)
                    frames.add(new FrameData());
                int nParts = dis.readUnsignedByte();
                for (int j = 0; j < nParts; j++) {
                    int dx = dis.readShort(), dy = dis.readShort();
                    int pid = dis.readByte();
                    frames.get(fi).parts.add(new FramePart(nextPartId++, pid, dx * scale, dy * scale, j));
                }
            }

            int offset = 5 + dLen;
            arrFrame.clear();
            for (int i = 0; i < frames.size(); i++)
                arrFrame.add(i);

            DataInputStream dis2 = new DataInputStream(new ByteArrayInputStream(data, offset, data.length - offset));
            int pngLen = dis2.readInt();
            byte[] pngData = new byte[pngLen];
            dis2.readFully(pngData);
            BufferedImage atlas = ImageIO.read(new ByteArrayInputStream(pngData));
            atlasPanel.setAtlas(atlas);

            int tdOffset = offset + 4 + pngLen;
            if (tdOffset < data.length)
                typeDataField.setText(String.valueOf(data[tdOffset]));

            updateSpriteList();
            updatePartsList();
            selectFrameIndex(0);
            refreshFrameList();
            history.clear();
            redoStack.clear();
            infoLabel.setText("Loaded mob data (Scale x" + scale + " applied).");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
