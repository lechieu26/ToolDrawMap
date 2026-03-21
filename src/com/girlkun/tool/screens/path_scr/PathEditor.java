package com.girlkun.tool.screens.path_scr;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathEditor extends JInternalFrame {

    // --- Constants ---
    private static final Map<Integer, String[]> PART_PRESETS = new HashMap<>();
    private static final Map<Integer, Map<Integer, String>> FIXED_SLOTS = new HashMap<>();
    private static final List<AnimationInfo> GLOBAL_ANIMATIONS = new ArrayList<>();
    private static final List<int[][]> CHAR_INFO = new ArrayList<>();

    static {
        PART_PRESETS.put(0, new String[] { "0: Wait", "1: Talk", "2: Blink" });
        PART_PRESETS.put(1,
                new String[] { "0: Idle", "1: Move", "2: Jump/Fly", "3: Attack 1", "4: Attack 2", "5: Attack 3",
                        "6: Charge", "7: Hurt", "8: Die", "9: Fly High", "10: Special 1", "11: Special 2",
                        "12: Special 3", "13: Special 4", "14: Special 5", "15: Special 6", "16: Special 7" });
        PART_PRESETS.put(2,
                new String[] { "0: Stand", "1: Move", "2: Jump", "3: Action 1", "4: Action 2", "5: Action 3",
                        "6: Action 4", "7: Action 5", "8: Action 6", "9: Action 7", "10: Action 8", "11: Action 9",
                        "12: Action 10" });

        Map<Integer, String> headFixed = new HashMap<>();
        headFixed.put(2, "3000.png");
        Map<Integer, String> bodyFixed = new HashMap<>();
        bodyFixed.put(16, "3001.png");
        Map<Integer, String> legFixed = new HashMap<>();
        legFixed.put(13, "3002.png");
        FIXED_SLOTS.put(0, headFixed);
        FIXED_SLOTS.put(1, bodyFixed);
        FIXED_SLOTS.put(2, legFixed);

        GLOBAL_ANIMATIONS.add(new AnimationInfo("Dung 1", new int[][] { { 0, 0 }, { 1, 1 }, { 2, 1 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Dung 2", new int[][] { { 0, 0 }, { 1, 1 }, { 2, 1 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Chay 1", new int[][] { { 0, 1 }, { 1, 2 }, { 2, 2 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Chay 2", new int[][] { { 0, 1 }, { 1, 3 }, { 2, 3 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Chay 3", new int[][] { { 0, 1 }, { 1, 4 }, { 2, 4 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Chay 4", new int[][] { { 0, 1 }, { 1, 5 }, { 2, 5 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Chay 5", new int[][] { { 0, 1 }, { 1, 6 }, { 2, 6 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Nhay 1", new int[][] { { 0, 0 }, { 1, 7 }, { 2, 7 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Bay 1", new int[][] { { 0, 0 }, { 1, 7 }, { 2, 0 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Da gio tay 1", new int[][] { { 0, 1 }, { 1, 12 }, { 2, 10 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Da gio tay 2", new int[][] { { 0, 1 }, { 1, 12 }, { 2, 11 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Da gio tay 3", new int[][] { { 0, 0 }, { 1, 9 }, { 2, 12 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Nhay xuong", new int[][] { { 0, 0 }, { 1, 8 }, { 2, 8 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Dam 1", new int[][] { { 0, 1 }, { 1, 10 }, { 2, 9 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Dam 2", new int[][] { { 0, 1 }, { 1, 11 }, { 2, 9 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Dam 3", new int[][] { { 0, 1 }, { 1, 2 }, { 2, 9 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Dam 4", new int[][] { { 0, 1 }, { 1, 13 }, { 2, 9 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Gong 1", new int[][] { { 0, 0 }, { 1, 7 }, { 2, 9 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Gong 2", new int[][] { { 0, 0 }, { 1, 8 }, { 2, 9 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Tu chuong", new int[][] { { 0, 0 }, { 1, 14 }, { 2, 9 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Chuong 1", new int[][] { { 0, 0 }, { 1, 15 }, { 2, 9 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Dam moc", new int[][] { { 0, 0 }, { 1, 9 }, { 2, 9 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Tu chuong 2", new int[][] { { 0, 0 }, { 1, 8 }, { 2, 1 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Do don", new int[][] { { 0, 0 }, { 1, 0 }, { 2, 7 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Do don 2", new int[][] { { 0, 0 }, { 1, 0 }, { 2, 9 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Bong mo", new int[][] { { 0, 2 }, { 1, 16 }, { 2, 13 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Bay dam 1", new int[][] { { 0, 1 }, { 1, 10 }, { 2, 8 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Bay dam 2", new int[][] { { 0, 1 }, { 1, 11 }, { 2, 8 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Bay dam 3", new int[][] { { 0, 1 }, { 1, 2 }, { 2, 8 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Bay dam 4", new int[][] { { 0, 1 }, { 1, 13 }, { 2, 8 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Bay tu chuong", new int[][] { { 0, 0 }, { 1, 14 }, { 2, 8 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Ban chuong", new int[][] { { 0, 0 }, { 1, 15 }, { 2, 8 } }));
        GLOBAL_ANIMATIONS.add(new AnimationInfo("Bay dam moc", new int[][] { { 0, 0 }, { 1, 9 }, { 2, 8 } }));

        // Adding CHAR_INFO
        CHAR_INFO.add(new int[][] { { 0, -13, 34 }, { 1, -8, 10 }, { 1, -9, 16 } }); // 0
        CHAR_INFO.add(new int[][] { { 0, -13, 35 }, { 1, -8, 10 }, { 1, -9, 17 } }); // 1
        CHAR_INFO.add(new int[][] { { 1, -10, 33 }, { 2, -10, 11 }, { 2, -8, 16 } }); // 2
        CHAR_INFO.add(new int[][] { { 1, -10, 32 }, { 3, -12, 10 }, { 3, -11, 15 } }); // 3
        CHAR_INFO.add(new int[][] { { 1, -10, 34 }, { 4, -8, 11 }, { 4, -7, 17 } }); // 4
        CHAR_INFO.add(new int[][] { { 1, -10, 34 }, { 5, -12, 11 }, { 5, -9, 17 } }); // 5
        CHAR_INFO.add(new int[][] { { 1, -10, 33 }, { 6, -10, 10 }, { 6, -8, 16 } }); // 6
        CHAR_INFO.add(new int[][] { { 0, -9, 36 }, { 7, -5, 17 }, { 7, -11, 25 } }); // 7
        CHAR_INFO.add(new int[][] { { 0, -7, 35 }, { 0, -18, 22 }, { 7, -10, 25 } }); // 8
        CHAR_INFO.add(new int[][] { { 1, -11, 35 }, { 10, -3, 25 }, { 12, -10, 26 } }); // 9
        CHAR_INFO.add(new int[][] { { 1, -11, 37 }, { 11, -3, 25 }, { 12, -11, 27 } }); // 10
        CHAR_INFO.add(new int[][] { { 0, -14, 34 }, { 12, -8, 21 }, { 9, -7, 31 } }); // 11
        CHAR_INFO.add(new int[][] { { 0, -12, 35 }, { 8, -5, 14 }, { 8, -15, 29 } }); // 12
        CHAR_INFO.add(new int[][] { { 1, -9, 34 }, { 9, -12, 9 }, { 10, -7, 19 } }); // 13
        CHAR_INFO.add(new int[][] { { 1, -13, 34 }, { 9, -12, 9 }, { 11, -10, 19 } }); // 14
        CHAR_INFO.add(new int[][] { { 1, -8, 32 }, { 9, -12, 9 }, { 2, -6, 15 } }); // 15
        CHAR_INFO.add(new int[][] { { 1, -8, 32 }, { 9, -12, 9 }, { 13, -12, 16 } }); // 16
        CHAR_INFO.add(new int[][] { { 0, -10, 31 }, { 9, -12, 9 }, { 7, -13, 20 } }); // 17
        CHAR_INFO.add(new int[][] { { 0, -11, 32 }, { 9, -12, 9 }, { 8, -15, 26 } }); // 18
        CHAR_INFO.add(new int[][] { { 0, -9, 33 }, { 9, -12, 9 }, { 14, -8, 18 } }); // 19
        CHAR_INFO.add(new int[][] { { 0, -11, 33 }, { 9, -12, 9 }, { 15, -6, 19 } }); // 20
        CHAR_INFO.add(new int[][] { { 0, -16, 31 }, { 9, -12, 9 }, { 9, -8, 28 } }); // 21
        CHAR_INFO.add(new int[][] { { 0, -14, 34 }, { 1, -8, 10 }, { 8, -16, 28 } }); // 22
        CHAR_INFO.add(new int[][] { { 0, -8, 36 }, { 7, -5, 17 }, { 0, -5, 25 } }); // 23
        CHAR_INFO.add(new int[][] { { 0, -9, 31 }, { 9, -12, 9 }, { 0, -6, 20 } }); // 24
        CHAR_INFO.add(new int[][] { { 2, -9, 36 }, { 13, -5, 17 }, { 16, -11, 25 } }); // 25
        CHAR_INFO.add(new int[][] { { 1, -9, 34 }, { 8, -5, 13 }, { 10, -7, 19 } }); // 26
        CHAR_INFO.add(new int[][] { { 1, -13, 34 }, { 8, -5, 13 }, { 11, -10, 19 } }); // 27
        CHAR_INFO.add(new int[][] { { 1, -8, 32 }, { 8, -5, 13 }, { 2, -6, 15 } }); // 28
        CHAR_INFO.add(new int[][] { { 1, -8, 32 }, { 8, -5, 13 }, { 13, -12, 16 } }); // 29
        CHAR_INFO.add(new int[][] { { 0, -9, 33 }, { 8, -5, 13 }, { 14, -8, 18 } }); // 30
        CHAR_INFO.add(new int[][] { { 0, -11, 33 }, { 8, -5, 13 }, { 15, -6, 19 } }); // 31
        CHAR_INFO.add(new int[][] { { 0, -16, 32 }, { 8, -5, 13 }, { 9, -8, 29 } }); // 32
    }

    // --- State ---
    private Map<Integer, List<PartImage>> partImages = new HashMap<>(); // type -> slots
    private List<List<Layer>> frames = new ArrayList<>();
    private int currentFrameIdx = 0;
    private int selectedLayerIdx = -1;
    private double zoom = 0.5;
    private int camX = 0, camY = 0;
    private int importScale = 4;
    private boolean isPlaying = false;
    private int playSpeed = 100;
    private javax.swing.Timer playTimer;

    private Stack<List<List<Layer>>> history = new Stack<>();
    private static final int MAX_HISTORY = 20;

    private Point dragStart = null;
    private Point dragItemStart = null;
    private Point camStart = null;
    private boolean isRightDragging = false;

    private static final int CENTER_X = 430;
    private static final int CENTER_Y = 450;

    private int[] activeSlot = { -1, -1 }; // {type, idx}
    private int[] highlightedPart = null; // {type, id}
    private boolean isRefreshing = false;

    // UI Components
    private CanvasPanel canvas;
    private JList<String> frameListbox;
    private DefaultListModel<String> frameListModel;
    private JList<String> layerListbox;
    private DefaultListModel<String> layerListModel;
    private JTextField txtDx, txtDy;
    private JButton btnPlay, btnReset;
    private JSlider speedSlider;
    private JLabel lblSpeedValue;
    private ScrollableResourcePanel resPanel;
    private String lastDir = ".";

    public PathEditor() {
        super("Path Editor", true, true, true, true);
        setSize(1700, 850);

        // Init State
        for (int i = 0; i < 3; i++) {
            int count = (i == 0) ? 3 : (i == 1) ? 17 : 14;
            List<PartImage> slots = new ArrayList<>();
            for (int j = 0; j < count; j++)
                slots.add(null);
            partImages.put(i, slots);
        }

        setupUI();
        loadTemplates();
        loadFixedResources();
        loadDefaultFrames();

        // Auto-load path_temp.txt
        File tempFile = new File("Part template/path_temp.txt");
        if (tempFile.exists()) {
            loadDataInternal(tempFile, true);
        }

        saveState();
        updateResetButton();
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Top Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topBar.setBackground(new Color(45, 45, 45));

        btnReset = new JButton("Reset Tool");
        styleButton(btnReset, new Color(255, 76, 76), Color.WHITE);
        btnReset.addActionListener(e -> resetTool());
        topBar.add(btnReset);

        JButton btnLoadTxt = new JButton("Load TXT");
        styleButton(btnLoadTxt, new Color(76, 161, 255), Color.WHITE);
        btnLoadTxt.addActionListener(e -> loadTxt());
        topBar.add(btnLoadTxt);

        JButton btnExportTxt = new JButton("Export TXT");
        styleButton(btnExportTxt, new Color(155, 89, 182), Color.WHITE);
        btnExportTxt.addActionListener(e -> exportTxt());
        topBar.add(btnExportTxt);

        JButton btnRunPython = new JButton("Run tool python");
        styleButton(btnRunPython, new Color(241, 196, 15), new Color(30, 30, 30));
        btnRunPython.addActionListener(e -> runToolPython());
        topBar.add(btnRunPython);

        add(topBar, BorderLayout.NORTH);

        // Main Split
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Left: Resources
        resPanel = new ScrollableResourcePanel();
        resPanel.setPreferredSize(new Dimension(360, 800));
        resPanel.setMinimumSize(new Dimension(360, 0));
        mainSplit.setLeftComponent(resPanel);
        mainSplit.setDividerLocation(360);
        mainSplit.setResizeWeight(0.0);

        // Middle & Right Parent
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightSplit.setDividerLocation(1000);
        mainSplit.setRightComponent(rightSplit);

        // Middle: Canvas
        JPanel midPanel = new JPanel(new BorderLayout());
        JPanel playCtrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        playCtrl.setBackground(new Color(60, 63, 65));

        JButton btnPrev = new JButton("<< Prev");
        styleButton(btnPrev, new Color(80, 80, 80), Color.WHITE);
        btnPrev.addActionListener(e -> prevFrame());
        playCtrl.add(btnPrev);

        btnPlay = new JButton("Play");
        styleButton(btnPlay, new Color(46, 204, 113), Color.WHITE);
        btnPlay.addActionListener(e -> togglePlay());
        playCtrl.add(btnPlay);

        JButton btnNext = new JButton("Next >>");
        styleButton(btnNext, new Color(80, 80, 80), Color.WHITE);
        btnNext.addActionListener(e -> nextFrame());
        playCtrl.add(btnNext);

        JLabel lblSpeed = new JLabel("Speed: ");
        lblSpeed.setForeground(Color.WHITE);
        playCtrl.add(lblSpeed);

        speedSlider = new JSlider(1, 20, 10);
        speedSlider.setPreferredSize(new Dimension(150, 20));
        speedSlider.setBackground(new Color(60, 63, 65));
        speedSlider.setUI(new BasicSliderUI(speedSlider) {
            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int trackHeight = 4;
                int trackY = trackRect.y + (trackRect.height - trackHeight) / 2;

                // Draw background (white)
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(trackRect.x, trackY, trackRect.width, trackHeight, 2, 2);

                // Draw filled part (purple)
                int fillWidth = thumbRect.x + thumbRect.width / 2 - trackRect.x;
                g2.setColor(new Color(155, 89, 182)); // Purple
                g2.fillRoundRect(trackRect.x, trackY, fillWidth, trackHeight, 2, 2);
            }

            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(155, 89, 182));
                g2.fillOval(thumbRect.x, thumbRect.y + 2, thumbRect.width, thumbRect.height - 4);
            }
        });
        speedSlider.addChangeListener(e -> {
            lblSpeedValue.setText(String.valueOf(speedSlider.getValue()));
            if (isPlaying) {
                playTimer.setDelay(1000 / speedSlider.getValue());
            }
        });
        playCtrl.add(speedSlider);

        lblSpeedValue = new JLabel("10");
        lblSpeedValue.setForeground(Color.WHITE);
        lblSpeedValue.setPreferredSize(new Dimension(30, 20));
        playCtrl.add(lblSpeedValue);

        midPanel.add(playCtrl, BorderLayout.NORTH);

        canvas = new CanvasPanel();
        canvas.setFocusable(true);
        midPanel.add(canvas, BorderLayout.CENTER);
        rightSplit.setLeftComponent(midPanel);

        // Right: Control
        JPanel ctrlPanel = new JPanel();
        ctrlPanel.setLayout(new BoxLayout(ctrlPanel, BoxLayout.Y_AXIS));

        // Animations list
        JPanel animGrp = new JPanel(new BorderLayout());
        animGrp.setBorder(BorderFactory.createTitledBorder("Animations"));
        frameListModel = new DefaultListModel<>();
        frameListbox = new JList<>(frameListModel);
        frameListbox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        frameListbox.addListSelectionListener(this::onFrameSelect);
        animGrp.add(new JScrollPane(frameListbox), BorderLayout.CENTER);
        ctrlPanel.add(animGrp);

        // Layers list
        JPanel layerGrp = new JPanel(new BorderLayout());
        layerGrp.setBorder(BorderFactory.createTitledBorder("Layers in Frame"));
        layerListModel = new DefaultListModel<>();
        layerListbox = new JList<>(layerListModel);
        layerListbox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        layerListbox.addListSelectionListener(this::onLayerSelect);
        layerGrp.add(new JScrollPane(layerListbox), BorderLayout.CENTER);
        ctrlPanel.add(layerGrp);

        // Properties
        JPanel propGrp = new JPanel(new GridLayout(1, 4, 5, 5));
        propGrp.setBorder(BorderFactory.createTitledBorder("Part Properties"));
        propGrp.add(new JLabel("DX:"));
        txtDx = new JTextField();
        txtDx.addActionListener(e -> updateLayerProp());
        propGrp.add(txtDx);
        propGrp.add(new JLabel("DY:"));
        txtDy = new JTextField();
        txtDy.addActionListener(e -> updateLayerProp());
        propGrp.add(txtDy);
        ctrlPanel.add(propGrp);

        rightSplit.setRightComponent(ctrlPanel);
        add(mainSplit, BorderLayout.CENTER);

        // Play Timer
        playTimer = new javax.swing.Timer(100, e -> {
            if (isPlaying)
                nextFrame();
        });

        // Key bindings
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (!isShowing())
                return false;
            // Check if application is active
            Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            if (activeWindow == null)
                return false;

            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z) {
                    undo();
                    return true;
                }
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    moveSelectedPart(-1, 0);
                    return true;
                }
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    moveSelectedPart(1, 0);
                    return true;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    moveSelectedPart(0, -1);
                    return true;
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    moveSelectedPart(0, 1);
                    return true;
                }

                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (e.isShiftDown())
                        cycleLayerPrev();
                    else
                        cycleLayerNext();
                    return true;
                }
            }
            return false;
        });
    }

    private void loadTemplates() {
        File templateDir = new File("Part template");
        if (!templateDir.exists())
            return;

        File[] files = templateDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null)
            return;

        Arrays.sort(files, (f1, f2) -> naturalCompare(f1.getName(), f2.getName()));

        Set<String> fixedFiles = new HashSet<>();
        for (Map<Integer, String> map : FIXED_SLOTS.values()) {
            for (String s : map.values())
                fixedFiles.add(s.toLowerCase());
        }

        List<File> sequentialFiles = new ArrayList<>();
        for (File f : files) {
            if (!fixedFiles.contains(f.getName().toLowerCase())) {
                sequentialFiles.add(f);
            }
        }

        int fileIdx = 0;
        int[] typesOrder = { 0, 1, 2 };
        for (int ptype : typesOrder) {
            List<PartImage> slots = partImages.get(ptype);
            for (int i = 0; i < slots.size(); i++) {
                if (isFixedSlot(ptype, i))
                    continue;
                if (fileIdx >= sequentialFiles.size())
                    break;

                File f = sequentialFiles.get(fileIdx++);
                try {
                    BufferedImage img = ImageIO.read(f);
                    BufferedImage alphaImg = applyAlpha(img, 0.5f);
                    PartImage pi = new PartImage(f.getAbsolutePath(), f.getName(), alphaImg);
                    pi.isTemplate = true;
                    slots.set(i, pi);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadFixedResources() {
        File templateDir = new File("Part template");
        for (int ptype = 0; ptype < 3; ptype++) {
            Map<Integer, String> fixedMap = FIXED_SLOTS.get(ptype);
            if (fixedMap == null)
                continue;
            for (Map.Entry<Integer, String> entry : fixedMap.entrySet()) {
                String filename = entry.getValue();
                File f = new File(templateDir, filename);
                if (!f.exists())
                    f = new File("Part template/" + filename);

                if (f.exists()) {
                    try {
                        BufferedImage img = ImageIO.read(f);
                        partImages.get(ptype).set(entry.getKey(), new PartImage(f.getAbsolutePath(), filename, img));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void loadDefaultFrames() {
        frames.clear();
        for (AnimationInfo anim : GLOBAL_ANIMATIONS) {
            List<Layer> layers = new ArrayList<>();
            for (int[] p : anim.parts) {
                layers.add(new Layer(p[0], p[1], 0, 0));
            }
            frames.add(layers);
        }
        refreshFrameList();
        selectFrame(0);
        history.clear();
    }

    private void refreshFrameList() {
        frameListModel.clear();
        for (int i = 0; i < frames.size(); i++) {
            String name = (i < GLOBAL_ANIMATIONS.size()) ? GLOBAL_ANIMATIONS.get(i).name : "Frame " + i;
            String label = name + " (" + frames.get(i).size() + ")";
            if (highlightedPart != null) {
                boolean match = false;
                for (Layer l : frames.get(i)) {
                    if (l.type == highlightedPart[0] && l.id == highlightedPart[1]) {
                        match = true;
                        break;
                    }
                }
                if (match)
                    label += " ●";
            }
            frameListModel.addElement(label);
        }
        if (currentFrameIdx >= 0 && currentFrameIdx < frameListModel.size()) {
            frameListbox.setSelectedIndex(currentFrameIdx);
        }
    }

    private void refreshLayerList() {
        isRefreshing = true;
        layerListModel.clear();
        if (currentFrameIdx < 0 || currentFrameIdx >= frames.size()) {
            isRefreshing = false;
            return;
        }
        List<Layer> fr = frames.get(currentFrameIdx);
        for (int i = 0; i < fr.size(); i++) {
            Layer l = fr.get(i);
            String imgName = "Unknown";
            PartImage pi = getPartImage(l.type, l.id);
            if (pi != null)
                imgName = pi.name;
            String typeStr = (l.type == 0) ? "Head" : (l.type == 1) ? "Body" : "Leg";
            layerListModel.addElement(i + ": [" + typeStr + "] " + imgName + " (" + l.dx + ", " + l.dy + ")");
        }
        if (selectedLayerIdx >= 0 && selectedLayerIdx < fr.size()) {
            layerListbox.setSelectedIndex(selectedLayerIdx);
        }
        isRefreshing = false;
        updatePropPanel();
    }

    private void updatePropPanel() {
        if (currentFrameIdx >= 0 && currentFrameIdx < frames.size() && selectedLayerIdx >= 0
                && selectedLayerIdx < frames.get(currentFrameIdx).size()) {
            Layer l = frames.get(currentFrameIdx).get(selectedLayerIdx);
            txtDx.setText(String.valueOf(l.dx));
            txtDy.setText(String.valueOf(l.dy));
        } else {
            txtDx.setText("");
            txtDy.setText("");
        }
    }

    private void selectFrame(int idx) {
        if (idx >= 0 && idx < frames.size()) {
            currentFrameIdx = idx;
            refreshLayerList();
            canvas.repaint();
        }
    }

    private void onFrameSelect(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            int idx = frameListbox.getSelectedIndex();
            if (idx != -1)
                selectFrame(idx);
        }
    }

    private void onLayerSelect(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && !isRefreshing) {
            selectedLayerIdx = layerListbox.getSelectedIndex();
            if (selectedLayerIdx >= 0 && currentFrameIdx >= 0
                    && selectedLayerIdx < frames.get(currentFrameIdx).size()) {
                Layer l = frames.get(currentFrameIdx).get(selectedLayerIdx);
                highlightedPart = new int[] { l.type, l.id };
            } else {
                highlightedPart = null;
            }
            updatePropPanel();
            canvas.repaint();
        }
    }

    private void togglePlay() {
        isPlaying = !isPlaying;
        btnPlay.setText(isPlaying ? "Stop" : "Play");
        if (isPlaying) {
            btnPlay.setBackground(new Color(230, 126, 34));
            playTimer.setDelay(1000 / speedSlider.getValue());
            playTimer.start();
        } else {
            btnPlay.setBackground(new Color(46, 204, 113));
            playTimer.stop();
        }
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void prevFrame() {
        if (frames.isEmpty())
            return;
        int newIdx = (currentFrameIdx - 1 + frames.size()) % frames.size();
        selectFrame(newIdx);
    }

    private void nextFrame() {
        if (frames.isEmpty())
            return;
        int newIdx = (currentFrameIdx + 1) % frames.size();
        selectFrame(newIdx);
    }

    private void updateLayerProp() {
        if (selectedLayerIdx >= 0) {
            try {
                int dx = Integer.parseInt(txtDx.getText());
                int dy = Integer.parseInt(txtDy.getText());
                Layer l = frames.get(currentFrameIdx).get(selectedLayerIdx);
                if (l.dx != dx || l.dy != dy) {
                    saveState();
                    l.dx = dx;
                    l.dy = dy;
                    syncPartPosition(l.type, l.id, dx, dy);
                    refreshLayerList();
                    canvas.repaint();
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void moveSelectedPart(int dx, int dy) {
        if (selectedLayerIdx >= 0) {
            saveState();
            Layer l = frames.get(currentFrameIdx).get(selectedLayerIdx);
            l.dx += dx;
            l.dy += dy;
            syncPartPosition(l.type, l.id, l.dx, l.dy);
            updatePropPanel();
            refreshLayerList();
            canvas.repaint();
        }
    }

    private void syncPartPosition(int type, int id, int dx, int dy) {
        int sFdx = 0, sFdy = 0;
        int[][] sInfo = getFrameCharInfo(currentFrameIdx);
        if (sInfo != null) {
            int ciIdx = (type == 0) ? 0 : (type == 1) ? 2 : 1;
            sFdx = sInfo[ciIdx][1];
            sFdy = sInfo[ciIdx][2];
        }

        for (int i = 0; i < frames.size(); i++) {
            if (i == currentFrameIdx)
                continue;
            int[][] tInfo = getFrameCharInfo(i);
            int tFdx = 0, tFdy = 0;
            if (tInfo != null) {
                int ciIdx = (type == 0) ? 0 : (type == 1) ? 2 : 1;
                tFdx = tInfo[ciIdx][1];
                tFdy = tInfo[ciIdx][2];
            }

            int deltaX = (tFdx - sFdx) * importScale;
            int deltaY = (sFdy - tFdy) * importScale;

            for (Layer layer : frames.get(i)) {
                if (layer.type == type && layer.id == id) {
                    layer.dx = dx + deltaX;
                    layer.dy = dy + deltaY;
                }
            }
        }
    }

    private int[][] getFrameCharInfo(int frameIdx) {
        if (frameIdx >= 0 && frameIdx < CHAR_INFO.size()) {
            return CHAR_INFO.get(frameIdx);
        }
        return null;
    }

    private void saveState() {
        List<List<Layer>> snapshot = new ArrayList<>();
        for (List<Layer> fr : frames) {
            List<Layer> frCopy = new ArrayList<>();
            for (Layer l : fr)
                frCopy.add(new Layer(l.type, l.id, l.dx, l.dy));
            snapshot.add(frCopy);
        }
        history.push(snapshot);
        if (history.size() > MAX_HISTORY)
            history.remove(0);
    }

    private void undo() {
        if (history.size() <= 1)
            return;
        history.pop();
        List<List<Layer>> prevState = history.peek();
        frames.clear();
        for (List<Layer> fr : prevState) {
            List<Layer> frCopy = new ArrayList<>();
            for (Layer l : fr)
                frCopy.add(new Layer(l.type, l.id, l.dx, l.dy));
            frames.add(frCopy);
        }
        refreshLayerList();
        canvas.repaint();
    }

    private void resetTool() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to reset?", "Confirm Reset",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;

        for (int i = 0; i < 3; i++) {
            List<PartImage> slots = partImages.get(i);
            for (int j = 0; j < slots.size(); j++)
                slots.set(j, null);
        }

        importScale = 4;
        zoom = 0.5;
        camX = 0;
        camY = 0;

        loadTemplates();
        loadFixedResources();
        loadDefaultFrames();

        File tempFile = new File("Part template/path_temp.txt");
        if (tempFile.exists())
            loadDataInternal(tempFile, true);

        resPanel.refreshUI();
        refreshFrameList();
        refreshLayerList();
        canvas.repaint();
        updateResetButton();
    }

    private void updateResetButton() {
        boolean hasUserImages = false;
        for (int ptype = 0; ptype < 3; ptype++) {
            List<PartImage> slots = partImages.get(ptype);
            for (int i = 0; i < slots.size(); i++) {
                PartImage pi = slots.get(i);
                if (pi != null && !pi.isTemplate && !isFixedSlot(ptype, i)) {
                    hasUserImages = true;
                    break;
                }
            }
            if (hasUserImages)
                break;
        }
        btnReset.setEnabled(hasUserImages);
    }

    private void runToolPython() {
        File exeFile = new File("external tools/PathEditor/NROPartEditor.exe");
        if (!exeFile.exists()) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy file: " + exeFile.getAbsolutePath(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(exeFile.getAbsolutePath());
            pb.directory(exeFile.getParentFile());
            pb.start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi chạy tool: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private boolean isFixedSlot(int type, int idx) {
        Map<Integer, String> map = FIXED_SLOTS.get(type);
        return map != null && map.containsKey(idx);
    }

    private PartImage getPartImage(int type, int idx) {
        List<PartImage> slots = partImages.get(type);
        if (slots != null && idx >= 0 && idx < slots.size())
            return slots.get(idx);
        return null;
    }

    private void cycleLayerNext() {
        List<Layer> fr = frames.get(currentFrameIdx);
        if (fr.isEmpty())
            return;
        selectedLayerIdx = (selectedLayerIdx + 1) % fr.size();
        layerListbox.setSelectedIndex(selectedLayerIdx);
        layerListbox.ensureIndexIsVisible(selectedLayerIdx);
    }

    private void cycleLayerPrev() {
        List<Layer> fr = frames.get(currentFrameIdx);
        if (fr.isEmpty())
            return;
        selectedLayerIdx = (selectedLayerIdx - 1 + fr.size()) % fr.size();
        layerListbox.setSelectedIndex(selectedLayerIdx);
        layerListbox.ensureIndexIsVisible(selectedLayerIdx);
    }

    private void loadTxt() {
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Load TXT", FileDialog.LOAD);
        fd.setFile("*.txt");
        fd.setDirectory(lastDir);
        fd.setVisible(true);
        if (fd.getFile() != null) {
            lastDir = fd.getDirectory();
            loadDataInternal(new File(fd.getDirectory(), fd.getFile()), false);
        }
    }

    private void loadDataInternal(File file, boolean silent) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty())
                    lines.add(line.trim());
            }

            if (lines.size() < 3) {
                if (!silent)
                    JOptionPane.showMessageDialog(this, "File must have at least 3 lines.");
                return;
            }

            for (int t = 0; t < 3; t++) {
                List<PartImage> slots = partImages.get(t);
                for (int i = 0; i < slots.size(); i++) {
                    if (!isFixedSlot(t, i))
                        slots.set(i, null);
                }
            }

            Map<String, int[]> offsetMap = new HashMap<>();
            File baseDir = file.getParentFile();

            for (int t = 0; t < 3; t++) {
                String l = lines.get(t);
                int start = l.indexOf("[[");
                if (start != -1)
                    l = l.substring(start);

                Pattern p = Pattern.compile("\\[(\\d+),(-?\\d+),(-?\\d+)\\]");
                Matcher m = p.matcher(l);
                int idx = 0;
                while (m.find()) {
                    int imgId = Integer.parseInt(m.group(1));
                    int dx = Integer.parseInt(m.group(2));
                    int dy = Integer.parseInt(m.group(3));
                    offsetMap.put(t + "," + imgId, new int[] { dx, dy });

                    if (idx < partImages.get(t).size() && !isFixedSlot(t, idx)) {
                        File png = new File(baseDir, imgId + ".png");
                        if (png.exists()) {
                            try {
                                partImages.get(t).set(idx,
                                        new PartImage(png.getAbsolutePath(), png.getName(), ImageIO.read(png)));
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    idx++;
                }
            }

            loadDefaultFrames();
            int updatedCount = 0;
            for (int fIdx = 0; fIdx < frames.size(); fIdx++) {
                int[][] info = getFrameCharInfo(fIdx);
                for (Layer layer : frames.get(fIdx)) {
                    PartImage pi = getPartImage(layer.type, layer.id);
                    if (pi != null) {
                        try {
                            String baseName = pi.name.replace(".png", "");
                            int imgId = Integer.parseInt(baseName);
                            int[] off = offsetMap.get(layer.type + "," + imgId);
                            if (off != null) {
                                int fdx = 0, fdy = 0;
                                if (info != null) {
                                    int ciIdx = (layer.type == 0) ? 0 : (layer.type == 1) ? 2 : 1;
                                    fdx = info[ciIdx][1];
                                    fdy = info[ciIdx][2];
                                }
                                layer.dx = (fdx + off[0]) * importScale;
                                layer.dy = (-fdy + off[1]) * importScale;
                                updatedCount++;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            zoom = 0.5;
            resPanel.refreshUI();
            refreshFrameList();
            selectFrame(0);
            updateResetButton();
            if (!silent)
                JOptionPane.showMessageDialog(this, "Loaded and applied to " + updatedCount + " parts.");

        } catch (Exception e) {
            if (!silent)
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void exportTxt() {
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Export TXT", FileDialog.SAVE);
        fd.setFile("*.txt");
        fd.setDirectory(lastDir);
        fd.setVisible(true);
        if (fd.getFile() != null) {
            lastDir = fd.getDirectory();
            File f = new File(fd.getDirectory(), fd.getFile());
            if (!f.getName().toLowerCase().endsWith(".txt")) {
                f = new File(f.getAbsolutePath() + ".txt");
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                String[] typeNames = { "HEAD", "BODY", "LEG" };
                for (int t = 0; t < 3; t++) {
                    List<String> partData = new ArrayList<>();
                    List<PartImage> slots = partImages.get(t);
                    for (int i = 0; i < slots.size(); i++) {
                        int imgId = 0;
                        PartImage pi = slots.get(i);
                        if (pi != null) {
                            try {
                                imgId = Integer.parseInt(pi.name.replace(".png", ""));
                            } catch (Exception ignored) {
                            }
                        }

                        int cDx = 0, cDy = 0, fIdxMatch = -1;
                        for (int fIdx = 0; fIdx < frames.size(); fIdx++) {
                            for (Layer layer : frames.get(fIdx)) {
                                if (layer.type == t && layer.id == i) {
                                    cDx = layer.dx;
                                    cDy = layer.dy;
                                    fIdxMatch = fIdx;
                                    break;
                                }
                            }
                            if (fIdxMatch != -1)
                                break;
                        }

                        int pDx = 0, pDy = 0;
                        if (fIdxMatch != -1) {
                            int[][] info = getFrameCharInfo(fIdxMatch);
                            if (info != null) {
                                int ciIdx = (t == 0) ? 0 : (t == 1) ? 2 : 1;
                                int fDx = info[ciIdx][1];
                                int fDy = info[ciIdx][2];
                                pDx = (int) Math.round((double) cDx / importScale) - fDx;
                                pDy = (int) Math.round((double) cDy / importScale) + fDy;
                            }
                        }
                        partData.add("[" + imgId + "," + pDx + "," + pDy + "]");
                    }
                    pw.println(typeNames[t] + " Type: " + t + "\t[" + String.join(",", partData) + "]");
                }
                JOptionPane.showMessageDialog(this, "Exported successfully!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage());
            }
        }
    }

    class CanvasPanel extends JPanel {
        public CanvasPanel() {
            setBackground(new Color(32, 32, 32));
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    canvas.requestFocusInWindow();
                    int cx = CENTER_X + camX;
                    int cy = CENTER_Y + camY;
                    dragStart = e.getPoint();

                    if (SwingUtilities.isRightMouseButton(e)) {
                        if (selectedLayerIdx >= 0) {
                            isRightDragging = true;
                            Layer l = frames.get(currentFrameIdx).get(selectedLayerIdx);
                            dragItemStart = new Point(l.dx, l.dy);
                        }
                    } else {
                        List<Layer> fr = frames.get(currentFrameIdx);
                        int clickedIdx = -1;
                        // Prioritize Body (Type 1)
                        for (int i = fr.size() - 1; i >= 0; i--) {
                            Layer l = fr.get(i);
                            if (l.type != 1)
                                continue;
                            if (checkHit(l, cx, cy, e.getX(), e.getY())) {
                                clickedIdx = i;
                                break;
                            }
                        }
                        if (clickedIdx == -1) {
                            for (int i = fr.size() - 1; i >= 0; i--) {
                                Layer l = fr.get(i);
                                if (l.type == 1)
                                    continue;
                                if (checkHit(l, cx, cy, e.getX(), e.getY())) {
                                    clickedIdx = i;
                                    break;
                                }
                            }
                        }

                        if (clickedIdx != -1) {
                            saveState();
                            selectedLayerIdx = clickedIdx;
                            Layer l = fr.get(clickedIdx);
                            highlightedPart = new int[] { l.type, l.id };
                            dragItemStart = new Point(l.dx, l.dy);
                            refreshLayerList();
                            refreshFrameList();
                        } else {
                            camStart = new Point(camX, camY);
                            selectedLayerIdx = -1;
                            highlightedPart = null;
                            dragItemStart = null;
                            refreshLayerList();
                            refreshFrameList();
                        }
                    }
                    repaint();
                }

                private boolean checkHit(Layer l, int cx, int cy, int mx, int my) {
                    PartImage pi = getPartImage(l.type, l.id);
                    if (pi == null)
                        return false;
                    int x = (int) (cx + l.dx * zoom);
                    int y = (int) (cy + l.dy * zoom);
                    int w = (int) (pi.img.getWidth() * zoom);
                    int h = (int) (pi.img.getHeight() * zoom);
                    return mx >= x && mx <= x + w && my >= y && my <= y + h;
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (dragItemStart != null && dragStart != null) {
                        saveState();
                        refreshLayerList();
                    }
                    dragStart = null;
                    dragItemStart = null;
                    camStart = null;
                    isRightDragging = false;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragStart == null)
                        return;
                    if (isRightDragging || (selectedLayerIdx != -1 && dragItemStart != null)) {
                        int dxMouse = e.getX() - dragStart.x;
                        int dyMouse = e.getY() - dragStart.y;
                        int dxLog = (int) (dxMouse / zoom);
                        int dyLog = (int) (dyMouse / zoom);

                        Layer l = frames.get(currentFrameIdx).get(selectedLayerIdx);
                        l.dx = dragItemStart.x + dxLog;
                        l.dy = dragItemStart.y + dyLog;
                        syncPartPosition(l.type, l.id, l.dx, l.dy);
                        txtDx.setText(String.valueOf(l.dx));
                        txtDy.setText(String.valueOf(l.dy));
                    } else if (camStart != null) {
                        camX = camStart.x + (e.getX() - dragStart.x);
                        camY = camStart.y + (e.getY() - dragStart.y);
                    }
                    repaint();
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (e.getWheelRotation() < 0)
                        zoom += 0.05;
                    else if (zoom > 0.1)
                        zoom -= 0.05;
                    repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(ma);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            int cellSize = 20;
            for (int y = 0; y < getHeight(); y += cellSize) {
                for (int x = 0; x < getWidth(); x += cellSize) {
                    g2.setColor(((x / cellSize + y / cellSize) % 2 == 0) ? Color.WHITE : new Color(240, 240, 240));
                    g2.fillRect(x, y, cellSize, cellSize);
                }
            }

            int cx = CENTER_X + camX;
            int cy = CENTER_Y + camY;

            g2.setColor(new Color(100, 50, 50));
            g2.drawLine(cx - 2000, cy, cx + 2000, cy);
            g2.setColor(new Color(50, 100, 50));
            g2.drawLine(cx, cy - 2000, cx, cy + 2000);

            if (!isPlaying && currentFrameIdx > 0) {
                for (Layer l : frames.get(currentFrameIdx - 1)) {
                    PartImage pi = getPartImage(l.type, l.id);
                    if (pi != null) {
                        int x = (int) (cx + l.dx * zoom);
                        int y = (int) (cy + l.dy * zoom);
                        int w = (int) (pi.img.getWidth() * zoom);
                        int h = (int) (pi.img.getHeight() * zoom);
                        Composite old = g2.getComposite();
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
                        g2.drawImage(pi.img, x, y, w, h, null);
                        g2.setComposite(old);
                    }
                }
            }

            List<Layer> fr = frames.get(currentFrameIdx);
            List<Integer> sortedIndices = new ArrayList<>();
            for (int i = 0; i < fr.size(); i++)
                sortedIndices.add(i);
            sortedIndices.sort((aIdx, bIdx) -> {
                int typeA = fr.get(aIdx).type;
                int typeB = fr.get(bIdx).type;
                if (typeA == 1 && typeB != 1)
                    return 1;
                if (typeA != 1 && typeB == 1)
                    return -1;
                return aIdx.compareTo(bIdx);
            });

            for (int i : sortedIndices) {
                Layer l = fr.get(i);
                PartImage pi = getPartImage(l.type, l.id);
                if (pi != null) {
                    int x = (int) (cx + l.dx * zoom);
                    int y = (int) (cy + l.dy * zoom);
                    int w = (int) (pi.img.getWidth() * zoom);
                    int h = (int) (pi.img.getHeight() * zoom);
                    g2.drawImage(pi.img, x, y, w, h, null);
                }
            }

            if (selectedLayerIdx >= 0 && selectedLayerIdx < fr.size()) {
                Layer l = fr.get(selectedLayerIdx);
                PartImage pi = getPartImage(l.type, l.id);
                if (pi != null) {
                    int x = (int) (cx + l.dx * zoom);
                    int y = (int) (cy + l.dy * zoom);
                    int w = (int) (pi.img.getWidth() * zoom);
                    int h = (int) (pi.img.getHeight() * zoom);
                    g2.setColor(Color.RED);
                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
                            new float[] { 5.0f }, 0.0f));
                    g2.drawRect(x, y, w, h);
                }
            }

            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("+", cx - 5, cy + 5);
        }
    }

    class ScrollableResourcePanel extends JPanel {
        private JPanel content;

        public ScrollableResourcePanel() {
            setLayout(new BorderLayout());
            content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            add(new JScrollPane(content), BorderLayout.CENTER);
            refreshUI();
        }

        public void refreshUI() {
            content.removeAll();
            String[] titles = { "Head Parts (3 slots)", "Body Parts (17 slots)", "Leg Parts (14 slots)" };
            int[] counts = { 3, 17, 14 };
            for (int t = 0; t < 3; t++) {
                JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
                header.setBackground(Color.BLACK);
                JLabel lTitle = new JLabel(titles[t]);
                lTitle.setForeground(Color.WHITE);
                lTitle.setFont(new Font("Arial", Font.BOLD, 12));
                header.add(lTitle);
                content.add(header);

                JPanel grid = new JPanel(new GridLayout(0, 4, 2, 2));
                for (int i = 0; i < counts[t]; i++) {
                    grid.add(createSlot(t, i));
                }
                JPanel gridWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                gridWrapper.add(grid);
                content.add(gridWrapper);
            }
            content.revalidate();
            content.repaint();
        }

        private JPanel createSlot(int type, int idx) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(Color.WHITE);
            p.setBorder(new LineBorder(Color.GRAY));
            p.setPreferredSize(new Dimension(85, 90));

            boolean fixed = isFixedSlot(type, idx);
            JLabel lbl = new JLabel("ID " + (idx + 1) + (fixed ? " (F)" : ""));
            lbl.setFont(new Font("Arial", Font.PLAIN, 9));
            lbl.setForeground(fixed ? Color.RED : Color.BLACK);
            p.add(lbl, BorderLayout.NORTH);

            PartImage pi = getPartImage(type, idx);
            JLabel imgLbl = new JLabel();
            imgLbl.setHorizontalAlignment(SwingConstants.CENTER);
            if (pi != null) {
                imgLbl.setIcon(new ImageIcon(getScaledImage(pi.img, 65, 75)));
                if (highlightedPart != null && highlightedPart[0] == type && highlightedPart[1] == idx) {
                    p.setBorder(new LineBorder(Color.RED, 2));
                }
            } else {
                imgLbl.setText(fixed ? "FIXED" : "Load");
                imgLbl.setForeground(Color.LIGHT_GRAY);
                imgLbl.setFont(new Font("Arial", Font.PLAIN, 10));
            }
            p.add(imgLbl, BorderLayout.CENTER);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    activeSlot = new int[] { type, idx };
                    if (e.getClickCount() == 2) {
                        addImageToFrame();
                    } else if (!fixed && SwingUtilities.isLeftMouseButton(e)) {
                        loadSlotImages(type, idx);
                    }
                    highlightedPart = new int[] { type, idx };
                    resPanel.refreshUI();
                }
            };
            imgLbl.addMouseListener(ma);
            return p;
        }
    }

    private void loadSlotImages(int type, int idx) {
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Select Images",
                FileDialog.LOAD);
        fd.setMultipleMode(true);
        fd.setFile("*.png");
        fd.setDirectory(lastDir);
        fd.setVisible(true);

        File[] files = fd.getFiles();
        if (files != null && files.length > 0) {
            lastDir = fd.getDirectory();
            saveState();
            int currentT = type, currentI = idx;
            for (File f : files) {
                while (currentT < 3) {
                    if (!isFixedSlot(currentT, currentI)) {
                        try {
                            partImages.get(currentT).set(currentI,
                                    new PartImage(f.getAbsolutePath(), f.getName(), ImageIO.read(f)));
                        } catch (IOException ignored) {
                        }
                        currentI++;
                        if (currentI >= partImages.get(currentT).size()) {
                            currentT++;
                            currentI = 0;
                        }
                        break;
                    }
                    currentI++;
                    if (currentI >= partImages.get(currentT).size()) {
                        currentT++;
                        currentI = 0;
                    }
                }
            }
            resPanel.refreshUI();
            updateResetButton();
            canvas.repaint();
        }
    }

    private void addImageToFrame() {
        if (activeSlot[0] == -1)
            return;
        PartImage pi = getPartImage(activeSlot[0], activeSlot[1]);
        if (pi == null)
            return;
        saveState();
        frames.get(currentFrameIdx).add(new Layer(activeSlot[0], activeSlot[1], 0, 0));
        refreshLayerList();
        canvas.repaint();
    }

    private BufferedImage getScaledImage(BufferedImage src, int maxWidth, int maxHeight) {
        if (src == null)
            return null;
        double ratio = Math.min((double) maxWidth / src.getWidth(), (double) maxHeight / src.getHeight());
        int w = Math.max(1, (int) (src.getWidth() * ratio));
        int h = Math.max(1, (int) (src.getHeight() * ratio));
        BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return resized;
    }

    private BufferedImage applyAlpha(BufferedImage src, float alpha) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return out;
    }

    private int naturalCompare(String s1, String s2) {
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher m1 = pattern.matcher(s1);
        Matcher m2 = pattern.matcher(s2);

        if (m1.find() && m2.find()) {
            return Integer.compare(Integer.parseInt(m1.group()), Integer.parseInt(m2.group()));
        }
        return s1.compareToIgnoreCase(s2);
    }

    // --- Data Classes ---
    static class AnimationInfo {
        String name;
        int[][] parts;

        AnimationInfo(String name, int[][] parts) {
            this.name = name;
            this.parts = parts;
        }
    }

    static class Layer {
        int type, id, dx, dy;

        Layer(int type, int id, int dx, int dy) {
            this.type = type;
            this.id = id;
            this.dx = dx;
            this.dy = dy;
        }
    }

    static class PartImage {
        String path, name;
        BufferedImage img;
        boolean isTemplate = false;

        PartImage(String path, String name, BufferedImage img) {
            this.path = path;
            this.name = name;
            this.img = img;
        }
    }
}
