package com.girlkun.tool.screens.effect_scr;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

public class EffectEditor extends JInternalFrame {

    // --- Data Classes ---
    static class SpriteInfo {
        int id;
        int x, y, w, h;

        SpriteInfo(int id, int x, int y, int w, int h) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        @Override
        public String toString() {
            return String.format("ID %d: %dx%d at (%d,%d)", id, w, h, x, y);
        }
    }

    static class Part {
        int partId;
        int imgId;
        int dx, dy, z;

        Part(int partId, int imgId, int dx, int dy, int z) {
            this.partId = partId;
            this.imgId = imgId;
            this.dx = dx;
            this.dy = dy;
            this.z = z;
        }

        Part copy(int newPartId) {
            return new Part(newPartId, imgId, dx, dy, z);
        }
    }

    static class EffectFrame {
        List<Part> parts = new ArrayList<>();

        EffectFrame copy(IdGenerator gen) {
            EffectFrame f = new EffectFrame();
            for (Part p : parts) {
                f.parts.add(p.copy(gen.nextPartId++));
            }
            return f;
        }
    }

    static class IdGenerator {
        int nextImgId = 0;
        int nextPartId = 0;
    }

    // --- State ---
    private BufferedImage atlas;
    private String atlasPath;
    private List<SpriteInfo> imgInfo = new ArrayList<>();
    private List<EffectFrame> frames = new ArrayList<>();
    private List<Integer> arrFrame = new ArrayList<>();
    private IdGenerator idGen = new IdGenerator();
    private int selectedFrameIdx = 0;
    private Set<Integer> selectedPartIds = new HashSet<>();
    private List<Part> clipboard = new ArrayList<>();

    // View State
    private double atlasScale = 1.0;
    private double canvasScale = 0.5;
    private int bgMode = 2; // 0: Dark, 1: Light, 2: Checkerboard
    private boolean showBBox = true;
    private boolean showChar = true;
    private int currentScale = 4;
    private int delay = 120;
    private boolean isPlaying = false;
    private javax.swing.Timer playbackTimer;

    // Interaction State
    private Rectangle cropRect;
    private String cropMode; // "draw", "move", "resize-nw", etc.
    private Point dragStart;
    private Map<Integer, Point> dragOrigins = new HashMap<>();
    private boolean isDraggingPart = false;
    private Point marqueeStart;
    private Rectangle marqueeRect;

    // UI Components
    private AtlasCanvas atlasCanvas;
    private FrameCanvas frameCanvas;
    private JList<String> frameList;
    private DefaultListModel<String> frameListModel;
    private JList<SpriteInfo> spriteList;
    private DefaultListModel<SpriteInfo> spriteListModel;
    private JList<String> partList;
    private DefaultListModel<String> partListModel;
    private JLabel statusLabel;
    private JToggleButton playBtn;
    private JTextField delayField;
    private JComboBox<Integer> scaleCombo;
    private JButton cutBtn;
    private JCheckBox autoNewFrameCheck;
    private JCheckBox autoFitCheck;

    // History (Undo/Redo)
    private boolean isUpdatingList = false;
    private LinkedList<byte[]> history = new LinkedList<>();
    private LinkedList<byte[]> redoStack = new LinkedList<>();
    private final int MAX_HISTORY = 50;

    // Static Assets
    private BufferedImage charImg;

    public EffectEditor() {
        super("Effect Editor - NRO (Java Version)", true, true, true, true);
        setSize(1550, 900);

        loadCharImg();
        initUI();
        resetState();
        
        playbackTimer = new javax.swing.Timer(delay, e -> playNextFrame());
    }

    private void loadCharImg() {
        try {
            File f = new File("Char.png");
            if (f.exists()) {
                charImg = ImageIO.read(f);
            }
        } catch (Exception e) {}
    }

    private void initUI() {
        // Main split pane: Atlas (Left) | Editor (Right)
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // --- Top Bar ---
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.setBorder(new EtchedBorder());
        
        JButton loadAtlasBtn = createStyledButton("Load Atlas PNG", new Color(60, 60, 100));
        loadAtlasBtn.addActionListener(e -> loadAtlas());
        
        JButton loadDataBtn = createStyledButton("Load Data", new Color(70, 70, 120));
        loadDataBtn.addActionListener(e -> openLoadDialog());
        
        JButton exportBtn = createStyledButton("Export Data", new Color(50, 90, 50));
        exportBtn.addActionListener(e -> exportBinary());
        
        JButton openOutputBtn = createStyledButton("Open Output", new Color(90, 80, 40));
        openOutputBtn.addActionListener(e -> openOutputFolder());
        
        JButton undoBtn = createStyledButton("Undo", null);
        undoBtn.addActionListener(e -> undo());
        JButton redoBtn = createStyledButton("Redo", null);
        redoBtn.addActionListener(e -> redo());
        
        scaleCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        scaleCombo.setSelectedItem(4);
        scaleCombo.addActionListener(e -> onScaleSelect());

        topBar.add(loadAtlasBtn);
        topBar.add(loadDataBtn);
        topBar.add(exportBtn);
        topBar.add(openOutputBtn);
        topBar.add(new JLabel("|"));
        topBar.add(undoBtn);
        topBar.add(redoBtn);
        topBar.add(new JLabel("|"));
        topBar.add(new JLabel("Scale:"));
        topBar.add(scaleCombo);
        
        mainPanel.add(topBar, BorderLayout.NORTH);

        // --- Content Splitter ---
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(500);

        // 1. Left: Atlas Area
        JPanel atlasPanel = new JPanel(new BorderLayout());
        atlasPanel.setBorder(BorderFactory.createTitledBorder("Atlas (Crop Sprite)"));
        
        JPanel atlasTools = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton azIn = createStyledButton("+", null);
        azIn.addActionListener(e -> { atlasScale = Math.min(4.0, atlasScale + 0.25); updateAtlasDisplay(); });
        JButton azOut = createStyledButton("-", null);
        azOut.addActionListener(e -> { atlasScale = Math.max(0.25, atlasScale - 0.25); updateAtlasDisplay(); });
        
        autoNewFrameCheck = new JCheckBox("Auto New Frame", true);
        autoFitCheck = new JCheckBox("Auto-Fit", true);
        
        cutBtn = createStyledButton("CUT [Enter]", new Color(180, 120, 0));
        cutBtn.setEnabled(false);
        cutBtn.addActionListener(e -> performCut());

        atlasTools.add(azIn);
        atlasTools.add(azOut);
        atlasTools.add(autoNewFrameCheck);
        atlasTools.add(autoFitCheck);
        atlasTools.add(cutBtn);
        
        atlasCanvas = new AtlasCanvas();
        JScrollPane atlasScroll = new JScrollPane(atlasCanvas);
        
        atlasPanel.add(atlasTools, BorderLayout.NORTH);
        atlasPanel.add(atlasScroll, BorderLayout.CENTER);

        // 2. Right Area: Frame Editor + Lists
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Editor Middle
        JPanel editorPanel = new JPanel(new BorderLayout());
        
        // Frame List (Left of Editor)
        JPanel frameListPanel = new JPanel(new BorderLayout());
        frameListPanel.setPreferredSize(new Dimension(150, 0));
        frameListModel = new DefaultListModel<>();
        frameList = new JList<>(frameListModel);
        frameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        frameList.addListSelectionListener(e -> onFrameSelect());
        
        JPanel frameBtns = new JPanel(new GridLayout(1, 3, 2, 2));
        JButton addFr = createStyledButton("+", new Color(40, 80, 40));
        addFr.addActionListener(e -> addFrame());
        JButton delFr = createStyledButton("-", new Color(80, 40, 40));
        delFr.addActionListener(e -> deleteFrame());
        JButton dupFr = createStyledButton("D", new Color(60, 60, 60));
        dupFr.setToolTipText("Duplicate Frame");
        dupFr.addActionListener(e -> duplicateFrame());
        frameBtns.add(addFr); frameBtns.add(delFr); frameBtns.add(dupFr);
        
        frameListPanel.add(new JLabel("Frames"), BorderLayout.NORTH);
        frameListPanel.add(new JScrollPane(frameList), BorderLayout.CENTER);
        frameListPanel.add(frameBtns, BorderLayout.SOUTH);
        
        // Canvas (Right of Middle)
        JPanel canvasPanel = new JPanel(new BorderLayout());
        JPanel canvasTools = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JCheckBox showBoxCheck = new JCheckBox("Viền ảnh", true);
        showBoxCheck.addActionListener(e -> { showBBox = showBoxCheck.isSelected(); frameCanvas.repaint(); });
        
        JCheckBox showCharCheck = new JCheckBox("Nhân vật", true);
        showCharCheck.addActionListener(e -> { showChar = showCharCheck.isSelected(); frameCanvas.repaint(); });
        
        JButton czIn = createStyledButton("+", null);
        czIn.addActionListener(e -> { canvasScale = Math.min(4.0, canvasScale + 0.25); frameCanvas.repaint(); });
        JButton czOut = createStyledButton("-", null);
        czOut.addActionListener(e -> { canvasScale = Math.max(0.5, canvasScale - 0.25); frameCanvas.repaint(); });
        
        JButton bgBtn = createStyledButton("BG: Grid", null);
        bgBtn.addActionListener(e -> {
            bgMode = (bgMode + 1) % 3;
            String[] txts = {"Dark", "Light", "Grid"};
            bgBtn.setText("BG: " + txts[bgMode]);
            frameCanvas.repaint();
        });
        
        delayField = new JTextField("120", 4);
        playBtn = new JToggleButton("Play");
        playBtn.addActionListener(e -> togglePlay());
        
        JButton sortBtn = createStyledButton("Sắp xếp", new Color(120, 50, 50));
        sortBtn.addActionListener(e -> startSort());

        canvasTools.add(showBoxCheck);
        canvasTools.add(showCharCheck);
        canvasTools.add(czIn);
        canvasTools.add(czOut);
        canvasTools.add(bgBtn);
        canvasTools.add(new JLabel("Delay:"));
        canvasTools.add(delayField);
        canvasTools.add(playBtn);
        canvasTools.add(sortBtn);
        
        frameCanvas = new FrameCanvas();
        frameCanvas.setPreferredSize(new Dimension(700, 520));
        
        canvasPanel.add(canvasTools, BorderLayout.NORTH);
        canvasPanel.add(frameCanvas, BorderLayout.CENTER);
        
        editorPanel.add(frameListPanel, BorderLayout.WEST);
        editorPanel.add(canvasPanel, BorderLayout.CENTER);
        
        // Lists (Far Right)
        JPanel listsPanel = new JPanel(new GridLayout(2, 1));
        listsPanel.setPreferredSize(new Dimension(220, 0));
        
        // Sprites List
        JPanel spritePanel = new JPanel(new BorderLayout());
        spriteListModel = new DefaultListModel<>();
        spriteList = new JList<>(spriteListModel);
        spriteList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) addSpriteToFrame();
            }
        });
        
        JPanel spriteBtns = new JPanel(new GridLayout(1, 2, 2, 2));
        JButton addSpr = createStyledButton("ADD", new Color(40, 80, 40));
        addSpr.addActionListener(e -> addSpriteToFrame());
        JButton delSpr = createStyledButton("DEL", new Color(80, 40, 40));
        delSpr.addActionListener(e -> deleteSprite());
        spriteBtns.add(addSpr); spriteBtns.add(delSpr);
        
        spritePanel.add(new JLabel("Sprites"), BorderLayout.NORTH);
        spritePanel.add(new JScrollPane(spriteList), BorderLayout.CENTER);
        spritePanel.add(spriteBtns, BorderLayout.SOUTH);
        
        // Parts List
        JPanel partPanel = new JPanel(new BorderLayout());
        partListModel = new DefaultListModel<>();
        partList = new JList<>(partListModel);
        partList.addListSelectionListener(e -> onPartListSelect());
        
        JPanel partBtns = new JPanel(new GridLayout(1, 3, 2, 2));
        JButton pUp = createStyledButton("▲", null); pUp.addActionListener(e -> partUp());
        JButton pDown = createStyledButton("▼", null); pDown.addActionListener(e -> partDown());
        JButton pDel = createStyledButton("DEL", new Color(80, 40, 40)); pDel.addActionListener(e -> deletePart());
        partBtns.add(pUp); partBtns.add(pDown); partBtns.add(pDel);

        partPanel.add(new JLabel("Parts in Frame"), BorderLayout.NORTH);
        partPanel.add(new JScrollPane(partList), BorderLayout.CENTER);
        partPanel.add(partBtns, BorderLayout.SOUTH);
        
        listsPanel.add(spritePanel);
        listsPanel.add(partPanel);
        
        rightPanel.add(editorPanel, BorderLayout.CENTER);
        rightPanel.add(listsPanel, BorderLayout.EAST);
        
        mainSplit.add(atlasPanel);
        mainSplit.add(rightPanel);
        
        mainPanel.add(mainSplit, BorderLayout.CENTER);
        
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(new EmptyBorder(2, 5, 2, 5));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
        
        // --- Keyboard Shortcuts ---
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        am.put("undo", new AbstractAction() { public void actionPerformed(ActionEvent e) { undo(); } });
        
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
        am.put("redo", new AbstractAction() { public void actionPerformed(ActionEvent e) { redo(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "selectAll");
        am.put("selectAll", new AbstractAction() { public void actionPerformed(ActionEvent e) { selectAllParts(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copy");
        am.put("copy", new AbstractAction() { public void actionPerformed(ActionEvent e) { copyParts(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "paste");
        am.put("paste", new AbstractAction() { public void actionPerformed(ActionEvent e) { pasteParts(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deletePart");
        am.put("deletePart", new AbstractAction() { public void actionPerformed(ActionEvent e) { deletePart(); } });
        
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "cut");
        am.put("cut", new AbstractAction() { public void actionPerformed(ActionEvent e) { performCut(); } });
    }

    // --- State Management ---
    private void resetState() {
        imgInfo.clear();
        frames.clear();
        arrFrame.clear();
        idGen = new IdGenerator();
        selectedFrameIdx = 0;
        selectedPartIds.clear();
        history.clear();
        redoStack.clear();
        
        addFrame();
        updateSpriteList();
        updatePartsList();
        updateFrameList();
        frameCanvas.repaint();
    }

    private void updateSpriteList() {
        spriteListModel.clear();
        for (SpriteInfo info : imgInfo) {
            spriteListModel.addElement(info);
        }
    }

    private void updatePartsList() {
        if (isUpdatingList) return;
        isUpdatingList = true;
        partListModel.clear();
        if (selectedFrameIdx < 0 || selectedFrameIdx >= frames.size()) {
            isUpdatingList = false;
            return;
        }
        EffectFrame f = frames.get(selectedFrameIdx);
        for (int i = 0; i < f.parts.size(); i++) {
            Part p = f.parts.get(i);
            partListModel.addElement("Part " + p.partId + ": img " + p.imgId + " (" + p.dx + "," + p.dy + ")");
        }
        // Restore selection
        for (int i = 0; i < f.parts.size(); i++) {
            if (selectedPartIds.contains(f.parts.get(i).partId)) {
                partList.addSelectionInterval(i, i);
            }
        }
        isUpdatingList = false;
    }

    private void updateFrameList() {
        frameListModel.clear();
        for (int i = 0; i < frames.size(); i++) {
            frameListModel.addElement("Frame " + i + " (" + frames.get(i).parts.size() + " pts)");
        }
        if (selectedFrameIdx >= 0 && selectedFrameIdx < frames.size()) {
            frameList.setSelectedIndex(selectedFrameIdx);
        }
    }

    // --- Actions ---
    private void loadAtlas() {
        if (!imgInfo.isEmpty() || !frames.isEmpty()) {
            if (JOptionPane.showConfirmDialog(this, "Loading a new Atlas will reset current data. Continue?", "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        FileDialog fd = new FileDialog(com.girlkun.tool.main.Main.I, "Select Atlas PNG", FileDialog.LOAD);
        fd.setFile("*.png");
        fd.setVisible(true);
        
        if (fd.getFile() != null) {
            try {
                File f = new File(fd.getDirectory(), fd.getFile());
                BufferedImage img = ImageIO.read(f);
                if (img == null) throw new Exception("Invalid image");
                
                resetState();
                atlas = img;
                atlasPath = f.getAbsolutePath();
                
                // Initial scale
                int max = Math.max(img.getWidth(), img.getHeight());
                if (max > 800) atlasScale = 800.0 / max;
                else atlasScale = 1.0;
                
                updateAtlasDisplay();
                statusLabel.setText("Atlas loaded: " + f.getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void updateAtlasDisplay() {
        if (atlas == null) return;
        atlasCanvas.setPreferredSize(new Dimension((int)(atlas.getWidth() * atlasScale), (int)(atlas.getHeight() * atlasScale)));
        atlasCanvas.revalidate();
        atlasCanvas.repaint();
    }

    private void performCut() {
        if (cropRect == null || atlas == null) return;
        
        pushHistory("Add Sprite");
        
        int x = (int)(cropRect.x / atlasScale);
        int y = (int)(cropRect.y / atlasScale);
        int w = (int)(cropRect.width / atlasScale);
        int h = (int)(cropRect.height / atlasScale);
        
        if (w <= 0 || h <= 0) return;
        
        int sid = idGen.nextImgId++;
        SpriteInfo info = new SpriteInfo(sid, x, y, w, h);
        
        boolean first = imgInfo.isEmpty();
        imgInfo.add(info);
        updateSpriteList();
        
        if (first) {
            selectedFrameIdx = 0;
            if (frames.isEmpty()) addFrame();
        } else if (autoNewFrameCheck.isSelected()) {
            addFrame();
        }
        
        // Add part to current frame
        if (selectedFrameIdx >= 0 && selectedFrameIdx < frames.size()) {
            EffectFrame f = frames.get(selectedFrameIdx);
            Part p = new Part(idGen.nextPartId++, sid, 0, 0, f.parts.size());
            f.parts.add(p);
            updatePartsList();
            frameCanvas.repaint();
        }
        
        cropRect = null;
        cutBtn.setEnabled(false);
        atlasCanvas.repaint();
    }

    private void addFrame() {
        pushHistory("Add Frame");
        frames.add(new EffectFrame());
        selectedFrameIdx = frames.size() - 1;
        updateFrameList();
        updatePartsList();
        frameCanvas.repaint();
    }

    private void deleteFrame() {
        if (frames.isEmpty()) return;
        pushHistory("Delete Frame");
        frames.remove(selectedFrameIdx);
        if (frames.isEmpty()) frames.add(new EffectFrame());
        selectedFrameIdx = Math.max(0, Math.min(selectedFrameIdx, frames.size() - 1));
        updateFrameList();
        updatePartsList();
        frameCanvas.repaint();
    }

    private void duplicateFrame() {
        if (frames.isEmpty()) return;
        pushHistory("Dup Frame");
        EffectFrame copy = frames.get(selectedFrameIdx).copy(idGen);
        frames.add(selectedFrameIdx + 1, copy);
        selectedFrameIdx++;
        updateFrameList();
        updatePartsList();
        frameCanvas.repaint();
    }

    private void addSpriteToFrame() {
        int[] indices = spriteList.getSelectedIndices();
        if (indices.length == 0) return;
        
        pushHistory("Add Part");
        if (frames.isEmpty()) addFrame();
        
        EffectFrame f = frames.get(selectedFrameIdx);
        for (int idx : indices) {
            SpriteInfo info = imgInfo.get(idx);
            f.parts.add(new Part(idGen.nextPartId++, info.id, 0, 0, f.parts.size()));
        }
        updatePartsList();
        frameCanvas.repaint();
    }

    private void deleteSprite() {
        int[] indices = spriteList.getSelectedIndices();
        if (indices.length == 0) return;
        
        pushHistory("Delete Sprite");
        List<SpriteInfo> toDel = new ArrayList<>();
        for (int idx : indices) toDel.add(imgInfo.get(idx));
        
        for (SpriteInfo info : toDel) {
            imgInfo.remove(info);
            // Remove parts using this sprite
            for (EffectFrame f : frames) {
                f.parts.removeIf(p -> p.imgId == info.id);
            }
        }
        updateSpriteList();
        updatePartsList();
        frameCanvas.repaint();
    }

    private void deletePart() {
        if (selectedPartIds.isEmpty()) return;
        pushHistory("Delete Part");
        EffectFrame f = frames.get(selectedFrameIdx);
        f.parts.removeIf(p -> selectedPartIds.contains(p.partId));
        selectedPartIds.clear();
        updatePartsList();
        frameCanvas.repaint();
    }

    private void partUp() {
        if (selectedPartIds.isEmpty()) return;
        pushHistory("Bring Front");
        EffectFrame f = frames.get(selectedFrameIdx);
        int maxZ = -Integer.MAX_VALUE;
        for (Part p : f.parts) if (p.z > maxZ) maxZ = p.z;
        for (Part p : f.parts) if (selectedPartIds.contains(p.partId)) p.z = maxZ + 1;
        frameCanvas.repaint();
    }

    private void partDown() {
        if (selectedPartIds.isEmpty()) return;
        pushHistory("Send Back");
        EffectFrame f = frames.get(selectedFrameIdx);
        int minZ = Integer.MAX_VALUE;
        for (Part p : f.parts) if (p.z < minZ) minZ = p.z;
        for (Part p : f.parts) if (selectedPartIds.contains(p.partId)) p.z = minZ - 1;
        frameCanvas.repaint();
    }

    private void selectAllParts() {
        if (selectedFrameIdx < 0 || selectedFrameIdx >= frames.size()) return;
        selectedPartIds.clear();
        for (Part p : frames.get(selectedFrameIdx).parts) {
            selectedPartIds.add(p.partId);
        }
        updatePartsList();
        frameCanvas.repaint();
    }

    private void copyParts() {
        if (selectedPartIds.isEmpty()) return;
        EffectFrame f = frames.get(selectedFrameIdx);
        clipboard.clear();
        for (Part p : f.parts) {
            if (selectedPartIds.contains(p.partId)) {
                clipboard.add(p);
            }
        }
        statusLabel.setText("Copied " + clipboard.size() + " parts");
    }

    private void pasteParts() {
        if (clipboard.isEmpty()) return;
        pushHistory("Paste Parts");
        EffectFrame f = frames.get(selectedFrameIdx);
        selectedPartIds.clear();
        for (Part p : clipboard) {
            Part np = new Part(idGen.nextPartId++, p.imgId, p.dx + 10, p.dy + 10, f.parts.size());
            f.parts.add(np);
            selectedPartIds.add(np.partId);
        }
        updatePartsList();
        frameCanvas.repaint();
        statusLabel.setText("Pasted " + clipboard.size() + " parts");
    }

    private void onFrameSelect() {
        int idx = frameList.getSelectedIndex();
        if (idx == -1 || idx == selectedFrameIdx) return;
        selectedFrameIdx = idx;
        selectedPartIds.clear();
        updatePartsList();
        frameCanvas.repaint();
    }

    private void onPartListSelect() {
        if (isUpdatingList) return;
        int[] indices = partList.getSelectedIndices();
        selectedPartIds.clear();
        if (selectedFrameIdx < 0 || selectedFrameIdx >= frames.size()) return;
        EffectFrame f = frames.get(selectedFrameIdx);
        for (int idx : indices) {
            if (idx >= 0 && idx < f.parts.size()) {
                selectedPartIds.add(f.parts.get(idx).partId);
            }
        }
        frameCanvas.repaint();
    }

    private void onScaleSelect() {
        int val = (Integer)scaleCombo.getSelectedItem();
        if (val == currentScale) return;
        
        double ratio = (double)val / currentScale;
        
        // Show dialog
        String[] options = {"Resize Coords", "Keep Size", "Cancel"};
        int n = JOptionPane.showOptionDialog(this, 
            "Changing scale from x" + currentScale + " to x" + val + ".\nRatio: " + String.format("%.2f", ratio),
            "Change Scale", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            
        if (n == 2 || n == -1) {
            scaleCombo.setSelectedItem(currentScale);
            return;
        }
        
        if (n == 0) {
            pushHistory("Scale Resize");
            for (SpriteInfo info : imgInfo) {
                info.x = (int)(info.x * ratio);
                info.y = (int)(info.y * ratio);
                info.w = (int)(info.w * ratio);
                info.h = (int)(info.h * ratio);
            }
            for (EffectFrame f : frames) {
                for (Part p : f.parts) {
                    p.dx = (int)(p.dx * ratio);
                    p.dy = (int)(p.dy * ratio);
                }
            }
        }
        
        currentScale = val;
        updateSpriteList();
        updatePartsList();
        frameCanvas.repaint();
    }

    private void togglePlay() {
        if (playBtn.isSelected()) {
            try { delay = Integer.parseInt(delayField.getText()); } catch(Exception e) { delay = 120; }
            if (delay < 10) delay = 10;
            playbackTimer.setDelay(delay);
            playbackTimer.start();
            isPlaying = true;
        } else {
            playbackTimer.stop();
            isPlaying = false;
        }
    }

    private void playNextFrame() {
        if (frames.isEmpty()) return;
        selectedFrameIdx = (selectedFrameIdx + 1) % frames.size();
        frameList.setSelectedIndex(selectedFrameIdx);
        updatePartsList();
        frameCanvas.repaint();
    }

    private void startSort() {
        if (frames.isEmpty()) return;
        EffectFrame f0 = frames.get(0);
        if (f0.parts.isEmpty()) return;
        
        int refX = Integer.MAX_VALUE;
        int refY = Integer.MAX_VALUE;
        for (Part p : f0.parts) {
            if (p.dx < refX) refX = p.dx;
            if (p.dy < refY) refY = p.dy;
        }
        
        pushHistory("Align Frames");
        int count = 0;
        for (int i = 1; i < frames.size(); i++) {
            EffectFrame f = frames.get(i);
            if (f.parts.isEmpty()) continue;
            
            int curX = Integer.MAX_VALUE;
            int curY = Integer.MAX_VALUE;
            for (Part p : f.parts) {
                if (p.dx < curX) curX = p.dx;
                if (p.dy < curY) curY = p.dy;
            }
            
            int dx = refX - curX;
            int dy = refY - curY;
            if (dx == 0 && dy == 0) continue;
            
            for (Part p : f.parts) {
                p.dx += dx;
                p.dy += dy;
            }
            count++;
        }
        statusLabel.setText("Aligned " + count + " frames to Frame 0.");
        frameCanvas.repaint();
    }

    // --- Serialization for Undo/Redo ---
    private byte[] serializeState() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(idGen.nextImgId);
            oos.writeObject(idGen.nextPartId);
            oos.writeObject(selectedFrameIdx);
            
            // Manual serialization to avoid deep complex issues
            oos.writeInt(imgInfo.size());
            for (SpriteInfo info : imgInfo) {
                oos.writeInt(info.id); oos.writeInt(info.x); oos.writeInt(info.y); oos.writeInt(info.w); oos.writeInt(info.h);
            }
            
            oos.writeInt(frames.size());
            for (EffectFrame f : frames) {
                oos.writeInt(f.parts.size());
                for (Part p : f.parts) {
                    oos.writeInt(p.partId); oos.writeInt(p.imgId); oos.writeInt(p.dx); oos.writeInt(p.dy); oos.writeInt(p.z);
                }
            }
            oos.close();
            return bos.toByteArray();
        } catch (Exception e) { return null; }
    }

    private void restoreState(byte[] data) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bis);
            idGen.nextImgId = (Integer)ois.readObject();
            idGen.nextPartId = (Integer)ois.readObject();
            selectedFrameIdx = (Integer)ois.readObject();
            
            int sCnt = ois.readInt();
            imgInfo.clear();
            for (int i = 0; i < sCnt; i++) {
                imgInfo.add(new SpriteInfo(ois.readInt(), ois.readInt(), ois.readInt(), ois.readInt(), ois.readInt()));
            }
            
            int fCnt = ois.readInt();
            frames.clear();
            for (int i = 0; i < fCnt; i++) {
                EffectFrame f = new EffectFrame();
                int pCnt = ois.readInt();
                for (int j = 0; j < pCnt; j++) {
                    f.parts.add(new Part(ois.readInt(), ois.readInt(), ois.readInt(), ois.readInt(), ois.readInt()));
                }
                frames.add(f);
            }
            
            updateSpriteList();
            updateFrameList();
            updatePartsList();
            frameCanvas.repaint();
        } catch (Exception e) {}
    }

    private void pushHistory(String desc) {
        history.add(serializeState());
        if (history.size() > MAX_HISTORY) history.removeFirst();
        redoStack.clear();
        statusLabel.setText(desc);
    }

    private void undo() {
        if (history.isEmpty()) return;
        redoStack.add(serializeState());
        restoreState(history.removeLast());
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        history.add(serializeState());
        restoreState(redoStack.removeLast());
    }

    // --- Data Load/Save ---
    private void openLoadDialog() {
        // Simple 2-file chooser or custom dialog
        JTextField imgField = new JTextField(20);
        JTextField datField = new JTextField(20);
        JButton b1 = new JButton("...");
        b1.addActionListener(e -> {
            FileDialog fd = new FileDialog(com.girlkun.tool.main.Main.I, "Select Image", FileDialog.LOAD);
            fd.setFile("*.png");
            fd.setVisible(true);
            if (fd.getFile() != null) imgField.setText(new File(fd.getDirectory(), fd.getFile()).getAbsolutePath());
        });
        JButton b2 = new JButton("...");
        b2.addActionListener(e -> {
            FileDialog fd = new FileDialog(com.girlkun.tool.main.Main.I, "Select Data", FileDialog.LOAD);
            fd.setVisible(true);
            if (fd.getFile() != null) datField.setText(new File(fd.getDirectory(), fd.getFile()).getAbsolutePath());
        });
        
        JPanel p = new JPanel(new GridLayout(2, 3));
        p.add(new JLabel("Image Path:")); p.add(imgField); p.add(b1);
        p.add(new JLabel("Data Path:")); p.add(datField); p.add(b2);
        
        if (JOptionPane.showConfirmDialog(this, p, "Load Data", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            loadData(imgField.getText(), datField.getText());
        }
    }

    private void loadData(String imgPath, String datPath) {
        try {
            BufferedImage img = ImageIO.read(new File(imgPath));
            if (img == null) return;
            atlas = img; atlasPath = imgPath; updateAtlasDisplay();
            
            DataInputStream dis = new DataInputStream(new FileInputStream(datPath));
            
            // Python struct "B" -> unsigned byte
            int sCnt = dis.readUnsignedByte();
            imgInfo.clear();
            int maxId = -1;
            int scale = (Integer)scaleCombo.getSelectedItem();
            
            for (int i = 0; i < sCnt; i++) {
                int id = dis.readByte();
                int x = dis.readUnsignedByte() * scale;
                int y = dis.readUnsignedByte() * scale;
                int w = dis.readUnsignedByte() * scale;
                int h = dis.readUnsignedByte() * scale;
                imgInfo.add(new SpriteInfo(id, x, y, w, h));
                if (id > maxId) maxId = id;
            }
            idGen.nextImgId = maxId + 1;
            
            // Frames (>h -> BigEndian Short)
            int fCnt = dis.readShort();
            frames.clear();
            int maxPId = -1;
            for (int i = 0; i < fCnt; i++) {
                EffectFrame f = new EffectFrame();
                int pCnt = dis.readUnsignedByte();
                for (int j = 0; j < pCnt; j++) {
                    int dx = dis.readShort() * scale;
                    int dy = dis.readShort() * scale;
                    int pid = dis.readByte();
                    int partId = idGen.nextPartId++;
                    f.parts.add(new Part(partId, pid, dx, dy, j));
                }
                frames.add(f);
            }
            
            // arrFrame
            if (dis.available() >= 2) {
                int aCnt = dis.readShort();
                arrFrame.clear();
                for (int i = 0; i < aCnt; i++) arrFrame.add((int)dis.readShort());
            } else {
                arrFrame.clear();
                for (int i = 0; i < frames.size(); i++) arrFrame.add(i);
            }
            
            dis.close();
            selectedFrameIdx = 0;
            updateSpriteList();
            updateFrameList();
            updatePartsList();
            frameCanvas.repaint();
            JOptionPane.showMessageDialog(this, "Loaded successfully!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Load failed: " + e.getMessage());
        }
    }

    private void exportBinary() {
        String idStr = JOptionPane.showInputDialog(this, "Enter Export ID (e.g. 5):", "Export", JOptionPane.QUESTION_MESSAGE);
        if (idStr == null || idStr.isEmpty()) return;
        
        try {
            File outDir = new File("output");
            if (!outDir.exists()) outDir.mkdir();
            
            int[] scales = {1, 2, 3, 4};
            for (int s : scales) {
                File sd = new File(outDir, "x" + s);
                if (!sd.exists()) sd.mkdir();
                
                int tw = (int)(atlas.getWidth() * s / 4.0);
                int th = (int)(atlas.getHeight() * s / 4.0);
                BufferedImage res = new BufferedImage(tw > 0 ? tw : 1, th > 0 ? th : 1, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = res.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(atlas, 0, 0, tw, th, null);
                g.dispose();
                ImageIO.write(res, "png", new File(sd, "ImgEffect_" + idStr + ".png"));
            }
            
            // Data Save (scale x1)
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(outDir, "DataEffect_" + idStr)));
            
            dos.writeByte(Math.min(imgInfo.size(), 255));
            for (SpriteInfo info : imgInfo) {
                dos.writeByte(info.id);
                dos.writeByte(clamp(info.x / 4, 0, 255));
                dos.writeByte(clamp(info.y / 4, 0, 255));
                dos.writeByte(clamp(info.w / 4, 0, 255));
                dos.writeByte(clamp(info.h / 4, 0, 255));
            }
            
            dos.writeShort(frames.size());
            for (EffectFrame f : frames) {
                dos.writeByte(Math.min(f.parts.size(), 255));
                for (Part p : f.parts) {
                    dos.writeShort(p.dx / 4);
                    dos.writeShort(p.dy / 4);
                    dos.writeByte(p.imgId);
                }
            }
            
            List<Integer> arr = arrFrame.isEmpty() ? new ArrayList<>() : arrFrame;
            if (arr.isEmpty()) for (int i = 0; i < frames.size(); i++) arr.add(i);
            dos.writeShort(arr.size());
            for (int v : arr) dos.writeShort(v);
            
            dos.close();
            JOptionPane.showMessageDialog(this, "Exported successfully to 'output' folder.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage());
        }
    }

    private void openOutputFolder() {
        try {
            File outDir = new File("output");
            if (!outDir.exists()) {
                Desktop.getDesktop().open(new File("."));
            } else {
                Desktop.getDesktop().open(outDir);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not open folder: " + e.getMessage());
        }
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    // --- Custom Painting ---
    private void drawCheckerboard(Graphics g, int w, int h, int step) {
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                if (((x / step) + (y / step)) % 2 == 1) g.setColor(new Color(160, 160, 160));
                else g.setColor(new Color(200, 200, 200));
                g.fillRect(x, y, step, step);
            }
        }
    }

    class AtlasCanvas extends JPanel {
        AtlasCanvas() {
            setBackground(new Color(32, 32, 32));
            MouseAdapter ma = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    onAtlasPress(e);
                }
                public void mouseDragged(MouseEvent e) {
                    onAtlasDrag(e);
                }
                public void mouseReleased(MouseEvent e) {
                    onAtlasRelease(e);
                }
                public void mouseMoved(MouseEvent e) {
                    onAtlasHover(e);
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(e -> {
                if (e.getWheelRotation() < 0) {
                    atlasScale = Math.min(4.0, atlasScale + 0.25);
                } else {
                    atlasScale = Math.max(0.25, atlasScale - 0.25);
                }
                updateAtlasDisplay();
            });
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (atlas == null) return;
            int w = (int)(atlas.getWidth() * atlasScale);
            int h = (int)(atlas.getHeight() * atlasScale);
            
            drawCheckerboard(g, w, h, 20);
            g.drawImage(atlas, 0, 0, w, h, null);
            
            if (cropRect != null) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(2));
                g2.drawRect(cropRect.x, cropRect.y, cropRect.width, cropRect.height);
                
                // Handles
                g2.setColor(Color.WHITE);
                int s = 4;
                int x = cropRect.x, y = cropRect.y, rw = cropRect.width, rh = cropRect.height;
                // Corners
                g2.fillRect(x-s, y-s, s*2, s*2);
                g2.fillRect(x+rw-s, y-s, s*2, s*2);
                g2.fillRect(x-s, y+rh-s, s*2, s*2);
                g2.fillRect(x+rw-s, y+rh-s, s*2, s*2);
                // Edges
                g2.fillRect(x+rw/2-s, y-s, s*2, s*2); // n
                g2.fillRect(x+rw/2-s, y+rh-s, s*2, s*2); // s
                g2.fillRect(x-s, y+rh/2-s, s*2, s*2); // w
                g2.fillRect(x+rw-s, y+rh/2-s, s*2, s*2); // e
                
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1));
                g2.drawRect(x-s, y-s, s*2, s*2);
                g2.drawRect(x+rw-s, y-s, s*2, s*2);
                g2.drawRect(x-s, y+rh-s, s*2, s*2);
                g2.drawRect(x+rw-s, y+rh-s, s*2, s*2);
                g2.drawRect(x+rw/2-s, y-s, s*2, s*2);
                g2.drawRect(x+rw/2-s, y+rh-s, s*2, s*2);
                g2.drawRect(x-s, y+rh/2-s, s*2, s*2);
                g2.drawRect(x+rw-s, y+rh/2-s, s*2, s*2);
            }
        }

        private void onAtlasPress(MouseEvent e) {
            if (atlas == null) return;
            dragStart = e.getPoint();
            
            if (SwingUtilities.isRightMouseButton(e)) {
                if (cropRect == null) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                return;
            }

            if (cropRect != null) {
                int s = 6;
                int x = cropRect.x, y = cropRect.y, w = cropRect.width, h = cropRect.height;
                if (new Rectangle(x-s, y-s, s*2, s*2).contains(dragStart)) cropMode = "resize-nw";
                else if (new Rectangle(x+w-s, y-s, s*2, s*2).contains(dragStart)) cropMode = "resize-ne";
                else if (new Rectangle(x+w-s, y+h-s, s*2, s*2).contains(dragStart)) cropMode = "resize-se";
                else if (new Rectangle(x-s, y+h-s, s*2, s*2).contains(dragStart)) cropMode = "resize-sw";
                else if (new Rectangle(x+w/2-s, y-s, s*2, s*2).contains(dragStart)) cropMode = "resize-n";
                else if (new Rectangle(x+w/2-s, y+h-s, s*2, s*2).contains(dragStart)) cropMode = "resize-s";
                else if (new Rectangle(x-s, y+h/2-s, s*2, s*2).contains(dragStart)) cropMode = "resize-w";
                else if (new Rectangle(x+w-s, y+h/2-s, s*2, s*2).contains(dragStart)) cropMode = "resize-e";
                else if (cropRect.contains(dragStart)) cropMode = "move";
                else {
                    cropRect = null;
                    cropMode = "draw";
                }
            } else {
                if (autoFitCheck.isSelected()) {
                    if (!doAutoFit(e.getX(), e.getY())) {
                        cropMode = "draw";
                    }
                } else {
                    cropMode = "draw";
                }
            }
            repaint();
        }

        private void onAtlasDrag(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e) && cropRect == null) {
                // Pan logic if needed, but JScrollPane handles it usually. 
                // However, we can manual pan:
                Point p = e.getPoint();
                JViewport v = (JViewport)getParent();
                Point viewPos = v.getViewPosition();
                viewPos.x -= (p.x - dragStart.x);
                viewPos.y -= (p.y - dragStart.y);
                v.setViewPosition(new Point(Math.max(0, viewPos.x), Math.max(0, viewPos.y)));
                return;
            }
            if (cropMode == null) return;
            Point p = e.getPoint();
            if (cropMode.equals("draw")) {
                int x1 = Math.min(dragStart.x, p.x);
                int y1 = Math.min(dragStart.y, p.y);
                int w = Math.abs(dragStart.x - p.x);
                int h = Math.abs(dragStart.y - p.y);
                cropRect = new Rectangle(x1, y1, w, h);
            } else if (cropMode.equals("move")) {
                cropRect.x += (p.x - dragStart.x);
                cropRect.y += (p.y - dragStart.y);
                dragStart = p;
            } else if (cropMode.startsWith("resize")) {
                int nx = cropRect.x, ny = cropRect.y, nw = cropRect.width, nh = cropRect.height;
                String dir = cropMode.substring(7);
                if (dir.contains("n")) {
                    int dy = p.y - ny;
                    if (nh - dy > 5) { ny += dy; nh -= dy; }
                }
                if (dir.contains("s")) {
                    nh = Math.max(5, p.y - ny);
                }
                if (dir.contains("w")) {
                    int dx = p.x - nx;
                    if (nw - dx > 5) { nx += dx; nw -= dx; }
                }
                if (dir.contains("e")) {
                    nw = Math.max(5, p.x - nx);
                }
                cropRect.setBounds(nx, ny, nw, nh);
            }
            cutBtn.setEnabled(cropRect != null && cropRect.width > 5);
            repaint();
        }

        private void onAtlasRelease(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                if (cropRect != null) performCut();
                setCursor(Cursor.getDefaultCursor());
            }
            cropMode = null;
        }

        private void onAtlasHover(MouseEvent e) {
            if (cropRect != null) {
                if (cropRect.contains(e.getPoint())) setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                else setCursor(Cursor.getDefaultCursor());
            } else {
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }
        }

        private boolean doAutoFit(int cx, int cy) {
            int ax = (int)(cx / atlasScale);
            int ay = (int)(cy / atlasScale);
            if (ax < 0 || ay < 0 || ax >= atlas.getWidth() || ay >= atlas.getHeight()) return false;
            
            int pix = atlas.getRGB(ax, ay);
            if ((pix >> 24 & 0xFF) == 0) return false; // Transparent
            
            // Simple BFS to find bounding box of connected non-transparent pixels
            Rectangle r = findConnectedComponentBBox(ax, ay);
            if (r != null) {
                cropRect = new Rectangle((int)(r.x * atlasScale), (int)(r.y * atlasScale), 
                                         (int)(r.width * atlasScale), (int)(r.height * atlasScale));
                cutBtn.setEnabled(true);
                repaint();
                return true;
            }
            return false;
        }

        private Rectangle findConnectedComponentBBox(int startX, int startY) {
            int w = atlas.getWidth();
            int h = atlas.getHeight();
            boolean[][] visited = new boolean[h][w];
            Queue<Point> q = new LinkedList<>();
            q.add(new Point(startX, startY));
            visited[startY][startX] = true;
            
            int minX = startX, maxX = startX, minY = startY, maxY = startY;
            
            while (!q.isEmpty()) {
                Point p = q.poll();
                if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y;
                
                int[] dx = {1, -1, 0, 0};
                int[] dy = {0, 0, 1, -1};
                for (int i = 0; i < 4; i++) {
                    int nx = p.x + dx[i], ny = p.y + dy[i];
                    if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[ny][nx]) {
                        if ((atlas.getRGB(nx, ny) >> 24 & 0xFF) > 0) {
                            visited[ny][nx] = true;
                            q.add(new Point(nx, ny));
                        }
                    }
                }
            }
            return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        }
    }

    class FrameCanvas extends JPanel {
        FrameCanvas() {
            setFocusable(true);
            MouseAdapter ma = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    onCanvasPress(e);
                }
                public void mouseDragged(MouseEvent e) {
                    onCanvasDrag(e);
                }
                public void mouseReleased(MouseEvent e) {
                    onCanvasRelease(e);
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(e -> {
                if (e.getWheelRotation() < 0) {
                    canvasScale = Math.min(4.0, canvasScale + 0.25);
                } else {
                    canvasScale = Math.max(0.5, canvasScale - 0.25);
                }
                repaint();
            });
            addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    onArrowKey(e);
                }
            });
        }

        private void onArrowKey(KeyEvent e) {
            if (selectedPartIds.isEmpty()) return;
            int dx = 0, dy = 0;
            int step = e.isShiftDown() ? 10 : 1;
            switch(e.getKeyCode()) {
                case KeyEvent.VK_LEFT: dx = -step; break;
                case KeyEvent.VK_RIGHT: dx = step; break;
                case KeyEvent.VK_UP: dy = -step; break;
                case KeyEvent.VK_DOWN: dy = step; break;
            }
            if (dx == 0 && dy == 0) return;
            
            EffectFrame f = frames.get(selectedFrameIdx);
            for (Part p : f.parts) {
                if (selectedPartIds.contains(p.partId)) {
                    p.dx += dx;
                    p.dy += dy;
                }
            }
            repaint();
            updatePartsList();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int W = getWidth(), H = getHeight();
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            // BG
            if (bgMode == 2) drawCheckerboard(g, W, H, 20);
            else {
                g.setColor(bgMode == 1 ? Color.WHITE : new Color(17, 17, 17));
                g.fillRect(0, 0, W, H);
            }

            int cx = W / 2, cy = (int)(H * 4.0 / 5.0);
            
            // Axis
            g.setColor(new Color(200, 50, 50, 180));
            g.drawLine(0, cy, W, cy);
            g.drawLine(cx, 0, cx, H);

            // Char Ref
            if (charImg != null && showChar) {
                double finalZoom = (currentScale / 4.0) * canvasScale;
                int cw = (int)(charImg.getWidth() * finalZoom);
                int ch = (int)(charImg.getHeight() * finalZoom);
                g.drawImage(charImg, cx - cw/2, cy - ch, cw, ch, null);
            }

            if (frames.isEmpty() || atlas == null) return;
            
            // Draw Previous (Ghost)
            if (selectedFrameIdx > 0) {
                drawFrame(g2, frames.get(selectedFrameIdx - 1), cx, cy, canvasScale, 80, false);
            }
            // Draw Current
            drawFrame(g2, frames.get(selectedFrameIdx), cx, cy, canvasScale, 255, showBBox);
            
            // Marquee
            if (marqueeRect != null) {
                g.setColor(new Color(0, 255, 255, 50));
                g.fillRect(marqueeRect.x, marqueeRect.y, marqueeRect.width, marqueeRect.height);
                g.setColor(Color.CYAN);
                g.drawRect(marqueeRect.x, marqueeRect.y, marqueeRect.width, marqueeRect.height);
            }
        }

        private void drawFrame(Graphics2D g, EffectFrame f, int cx, int cy, double zoom, int alpha, boolean drawBBox) {
            List<Part> sorted = new ArrayList<>(f.parts);
            sorted.sort(Comparator.comparingInt(p -> p.z));
            
            Composite oldComp = g.getComposite();
            if (alpha < 255) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255.0f));

            for (Part p : sorted) {
                SpriteInfo info = null;
                for (SpriteInfo si : imgInfo) if (si.id == p.imgId) { info = si; break; }
                if (info == null) continue;
                
                int sw = info.w, sh = info.h;
                int dw = (int)(sw * zoom), dh = (int)(sh * zoom);
                int dx = cx + (int)(p.dx * zoom);
                int dy = cy + (int)(p.dy * zoom);
                
                g.drawImage(atlas, dx, dy, dx + dw, dy + dh, info.x, info.y, info.x + sw, info.y + sh, null);
                
                if (drawBBox) {
                    boolean sel = selectedPartIds.contains(p.partId);
                    g.setColor(sel ? Color.YELLOW : Color.GREEN);
                    g.setStroke(new BasicStroke(sel ? 2 : 1));
                    g.drawRect(dx, dy, dw, dh);
                }
            }
            g.setComposite(oldComp);
        }

        private void onCanvasPress(MouseEvent e) {
            requestFocus();
            int cx = getWidth() / 2, cy = (int)(getHeight() * 4.0 / 5.0);
            
            if (SwingUtilities.isRightMouseButton(e)) {
                marqueeStart = e.getPoint();
                marqueeRect = null;
                return;
            }

            // Find clicked part (reverse z order)
            Part clicked = null;
            if (selectedFrameIdx >= 0 && selectedFrameIdx < frames.size()) {
                List<Part> parts = new ArrayList<>(frames.get(selectedFrameIdx).parts);
                parts.sort((p1, p2) -> Integer.compare(p2.z, p1.z));
                for (Part p : parts) {
                    SpriteInfo info = null;
                    for (SpriteInfo si : imgInfo) if (si.id == p.imgId) { info = si; break; }
                    if (info == null) continue;
                    
                    int dx = cx + (int)(p.dx * canvasScale);
                    int dy = cy + (int)(p.dy * canvasScale);
                    int dw = (int)(info.w * canvasScale);
                    int dh = (int)(info.h * canvasScale);
                    if (new Rectangle(dx, dy, dw, dh).contains(e.getPoint())) {
                        clicked = p; break;
                    }
                }
            }

            boolean ctrl = e.isControlDown();
            if (clicked != null) {
                if (ctrl) {
                    if (selectedPartIds.contains(clicked.partId)) selectedPartIds.remove(clicked.partId);
                    else selectedPartIds.add(clicked.partId);
                } else if (!selectedPartIds.contains(clicked.partId)) {
                    selectedPartIds.clear();
                    selectedPartIds.add(clicked.partId);
                }
                
                // Start drag
                isDraggingPart = true;
                dragStart = e.getPoint();
                dragOrigins.clear();
                for (Part p : frames.get(selectedFrameIdx).parts) {
                    if (selectedPartIds.contains(p.partId)) dragOrigins.put(p.partId, new Point(p.dx, p.dy));
                }
            } else if (!ctrl) {
                selectedPartIds.clear();
            }
            updatePartsList();
            repaint();
        }

        private void onCanvasDrag(MouseEvent e) {
            if (isDraggingPart) {
                double dx = (e.getX() - dragStart.x) / canvasScale;
                double dy = (e.getY() - dragStart.y) / canvasScale;
                for (Part p : frames.get(selectedFrameIdx).parts) {
                    if (selectedPartIds.contains(p.partId)) {
                        Point ori = dragOrigins.get(p.partId);
                        p.dx = (int)(ori.x + dx);
                        p.dy = (int)(ori.y + dy);
                    }
                }
                repaint();
                updatePartsList();
            } else if (marqueeStart != null) {
                int x1 = Math.min(marqueeStart.x, e.getX());
                int y1 = Math.min(marqueeStart.y, e.getY());
                int w = Math.abs(marqueeStart.x - e.getX());
                int h = Math.abs(marqueeStart.y - e.getY());
                marqueeRect = new Rectangle(x1, y1, w, h);
                repaint();
            }
        }

        private void onCanvasRelease(MouseEvent e) {
            if (isDraggingPart) {
                isDraggingPart = false;
                pushHistory("Move Parts");
            }
            if (marqueeRect != null) {
                int cx = getWidth() / 2, cy = (int)(getHeight() * 4.0 / 5.0);
                boolean ctrl = e.isControlDown();
                if (!ctrl) selectedPartIds.clear();
                
                for (Part p : frames.get(selectedFrameIdx).parts) {
                    SpriteInfo info = null;
                    for (SpriteInfo si : imgInfo) if (si.id == p.imgId) { info = si; break; }
                    if (info == null) continue;
                    int dx = cx + (int)(p.dx * canvasScale);
                    int dy = cy + (int)(p.dy * canvasScale);
                    int dw = (int)(info.w * canvasScale);
                    int dh = (int)(info.h * canvasScale);
                    if (marqueeRect.intersects(new Rectangle(dx, dy, dw, dh))) selectedPartIds.add(p.partId);
                }
                marqueeStart = null;
                marqueeRect = null;
                updatePartsList();
                repaint();
            }
        }
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        if (bg != null) {
            btn.setBackground(bg);
            btn.setForeground(Color.WHITE);
            // Hint for FlatLaf to keep the colored background
            btn.setOpaque(true);
            btn.setBorderPainted(false);
        }
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return btn;
    }

}
