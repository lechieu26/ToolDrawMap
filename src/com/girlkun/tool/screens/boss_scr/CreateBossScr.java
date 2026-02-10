package com.girlkun.tool.screens.boss_scr;

import java.awt.FileDialog;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO.PartData;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import javax.imageio.ImageIO;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Create Boss Screen - Công cụ tạo Boss cho game NRO
 * Cho phép chọn outfit (head, body, leg), nhập thông tin Boss
 * và generate code Java để copy vào project TOMAHOC
 */
public class CreateBossScr extends JInternalFrame {

    // Icon path
    private static final String ICON_PATH = "data/data/icon/x4";
    private static String lastImageDir = new File(ICON_PATH).getAbsolutePath();

    // Outfit images
    private BufferedImage headImage, bodyImage, legImage;
    private int headIconId = 0, bodyIconId = 0, legIconId = 0;

    // Path IDs from database
    private List<Integer> headPathIds = new ArrayList<>();
    private List<Integer> bodyPathIds = new ArrayList<>();
    private List<Integer> legPathIds = new ArrayList<>();
    private int selectedHeadPathId = 0, selectedBodyPathId = 0, selectedLegPathId = 0;

    // Path positions
    private Point headPos = new Point(0, 0);
    private Point bodyPos = new Point(0, 0);
    private Point legPos = new Point(0, 0);

    // Animation Frames
    private static class PartFrame {
        int iconId;
        int dx;
        int dy;

        public PartFrame(int iconId, int dx, int dy) {
            this.iconId = iconId;
            this.dx = dx;
            this.dy = dy;
        }
    }

    private List<PartFrame> headFrames = new ArrayList<>();
    private List<PartFrame> bodyFrames = new ArrayList<>();
    private List<PartFrame> legFrames = new ArrayList<>();
    private Map<Integer, BufferedImage> iconCache = new HashMap<>();

    // UI Components - Form
    private JTextField txtBossName, txtBossId, txtDefineName, txtDame, txtHp, txtMapJoin, txtSecondsRest;
    private JTextField txtTextS, txtTextM, txtTextE;
    private JLabel lblDefineNameStatus;

    // Boss ID File Analysis
    private static final String BOSS_ID_FILE_PATH = "E:/NRO/SourceCode/TOMAHOC/src/boss/BossID.java";
    private Set<String> existingDefineNames = new HashSet<>();
    private Set<Integer> existingBossIds = new HashSet<>();
    private int nextAvailableBossId = -1;
    private JComboBox<String> cboGender;

    // CHAR_INFO from Path Editor (Simulated)
    // Structure: [FrameIdx][PartType][3: Index, DX, DY]
    // PartType Order in Info: 0:Head, 1:Leg, 2:Body
    private static final int[][][] CHAR_INFO = {
            { { 0, -13, 34 }, { 1, -8, 10 }, { 1, -9, 16 } }, // 0: Dung 1
            { { 0, -13, 35 }, { 1, -8, 10 }, { 1, -9, 17 } }, // 1: Dung 2
            { { 1, -10, 33 }, { 2, -10, 11 }, { 2, -8, 16 } }, // 2: Chay 1
            { { 1, -10, 32 }, { 3, -12, 10 }, { 3, -11, 15 } }, // 3: Chay 2
            { { 1, -10, 34 }, { 4, -8, 11 }, { 4, -7, 17 } }, // 4: Chay 3
            { { 1, -10, 34 }, { 5, -12, 11 }, { 5, -9, 17 } }, // 5: Chay 4
            { { 1, -10, 33 }, { 6, -10, 10 }, { 6, -8, 16 } }, // 6: Chay 5
            { { 0, -9, 36 }, { 7, -5, 17 }, { 7, -11, 25 } }, // 7: Nhay 1
            { { 0, -7, 35 }, { 0, -18, 22 }, { 7, -10, 25 } }, // 8: Bay 1
            { { 1, -11, 35 }, { 10, -3, 25 }, { 12, -10, 26 } }, // 9: Da 1
            { { 1, -11, 37 }, { 11, -3, 25 }, { 12, -11, 27 } }, // 10: Da 2
            { { 0, -14, 34 }, { 12, -8, 21 }, { 9, -7, 31 } }, // 11: Da 3
            { { 0, -12, 35 }, { 8, -5, 14 }, { 8, -15, 29 } }, // 12: Nhay xuong
            { { 1, -9, 34 }, { 9, -12, 9 }, { 10, -7, 19 } }, // 13: Dam 1
            { { 1, -13, 34 }, { 9, -12, 9 }, { 11, -10, 19 } }, // 14: Dam 2
            { { 1, -8, 32 }, { 9, -12, 9 }, { 2, -6, 15 } }, // 15: Dam 3 (Fixed Index Issue)
            { { 1, -8, 32 }, { 9, -12, 9 }, { 13, -12, 16 } }, // 16: Dam 4
            { { 0, -10, 31 }, { 9, -12, 9 }, { 7, -13, 20 } }, // 17: Gong 1
            { { 0, -11, 32 }, { 9, -12, 9 }, { 8, -15, 26 } }, // 18: Gong 2
            { { 0, -9, 33 }, { 9, -12, 9 }, { 14, -8, 18 } }, // 19: Tu chuong
            { { 0, -11, 33 }, { 9, -12, 9 }, { 15, -6, 19 } }, // 20: Chuong 1
            { { 0, -16, 31 }, { 9, -12, 9 }, { 9, -8, 28 } }, // 21: Dam moc
            { { 0, -14, 34 }, { 1, -8, 10 }, { 8, -16, 28 } }, // 22: Tu chuong 2
            { { 0, -8, 36 }, { 7, -5, 17 }, { 0, -5, 25 } }, // 23: Do don
            { { 0, -9, 31 }, { 9, -12, 9 }, { 0, -6, 20 } }, // 24: Do don 2
            { { 2, -9, 36 }, { 13, -5, 17 }, { 16, -11, 25 } }, // 25: Bong mo
            { { 1, -9, 34 }, { 8, -5, 13 }, { 10, -7, 19 } }, // 26: Bay dam 1
            { { 1, -13, 34 }, { 8, -5, 13 }, { 11, -10, 19 } }, // 27: Bay dam 2
            { { 1, -8, 32 }, { 8, -5, 13 }, { 2, -6, 15 } }, // 28: Bay dam 3
            { { 1, -8, 32 }, { 8, -5, 13 }, { 13, -12, 16 } }, // 29: Bay dam 4
            { { 0, -9, 33 }, { 8, -5, 13 }, { 14, -8, 18 } }, // 30: Bay tu
            { { 0, -11, 33 }, { 8, -5, 13 }, { 15, -6, 19 } }, // 31: Bay chuong
            { { 0, -16, 32 }, { 8, -5, 13 }, { 9, -8, 29 } } // 32: Bay moc
    };

    private static final String[] ANIM_NAMES = {
            "Dung 1", "Dung 2", "Chay 1", "Chay 2", "Chay 3", "Chay 4", "Chay 5",
            "Nhay 1", "Bay 1", "Da 1", "Da 2", "Da 3", "Nhay xuong",
            "Dam 1", "Dam 2", "Dam 3", "Dam 4", "Gong 1", "Gong 2",
            "Tu chuong", "Chuong 1", "Dam moc", "Tu chuong 2", "Do don", "Do don 2",
            "Bong mo", "Bay dam 1", "Bay dam 2", "Bay dam 3", "Bay dam 4",
            "Bay tu", "Bay chuong", "Bay moc"
    };

    // Outfit selectors
    private JButton btnHead, btnBody, btnLeg;
    private JComboBox<String> cboHeadPath, cboBodyPath, cboLegPath;
    private JLabel lblHeadInfo, lblBodyInfo, lblLegInfo;

    // Canvas
    private BossCanvas canvas;

    // Skill table
    private JTable skillTable;
    private DefaultTableModel skillTableModel;
    private JComboBox<ShopManagerDAO.SkillTemplate> cboSkillEditor;

    private void loadSkills() {
        SwingWorker<List<ShopManagerDAO.SkillTemplate>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ShopManagerDAO.SkillTemplate> doInBackground() {
                return ShopManagerDAO.gI().getAllSkills();
            }

            @Override
            protected void done() {
                try {
                    List<ShopManagerDAO.SkillTemplate> skills = get();
                    if (cboSkillEditor != null) {
                        cboSkillEditor.removeAllItems();
                        Set<Integer> addedIds = new HashSet<>();
                        List<ShopManagerDAO.SkillTemplate> uniqueSkills = new ArrayList<>();

                        for (ShopManagerDAO.SkillTemplate skill : skills) {
                            if (!addedIds.contains(skill.id)) {
                                uniqueSkills.add(skill);
                                addedIds.add(skill.id);
                            }
                        }

                        // Sort by ID
                        uniqueSkills.sort((s1, s2) -> Integer.compare(s1.id, s2.id));

                        for (ShopManagerDAO.SkillTemplate skill : uniqueSkills) {
                            cboSkillEditor.addItem(skill);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    // Animation
    private Timer animationTimer;
    private boolean isAnimating = false;
    private int currentFrame = 0;

    // Output
    private JTextArea outputTextArea;

    public CreateBossScr() {
        super("Create Boss", true, true, true, true);
        initComponents();
        setSize(1400, 850);

        // Analyze BossID file and generate next ID
        analyzeBossIdFile();
    }

    private void analyzeBossIdFile() {
        existingDefineNames.clear();
        existingBossIds.clear();

        File file = new File(BOSS_ID_FILE_PATH);
        if (!file.exists()) {
            System.out.println("BossID file not found at: " + BOSS_ID_FILE_PATH);
            // Fallback to DB min ID logic if file missing
            generateNextBossIdFromDB();
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            Pattern pattern = Pattern.compile("public static final int\\s+([A-Z0-9_]+)\\s*=\s*(-?\\d+);");

            while ((line = br.readLine()) != null) {
                Matcher matcher = pattern.matcher(line.trim());
                if (matcher.find()) {
                    String name = matcher.group(1);
                    int id = Integer.parseInt(matcher.group(2));
                    existingDefineNames.add(name);
                    existingBossIds.add(id);
                }
            }

            // Find next available negative ID (min existing - 1)
            int minId = 0;
            for (int id : existingBossIds) {
                if (id < minId)
                    minId = id;
            }
            nextAvailableBossId = (minId == 0) ? -1 : (minId - 1);
            txtBossId.setText(String.valueOf(nextAvailableBossId));

        } catch (Exception e) {
            e.printStackTrace();
            generateNextBossIdFromDB();
        }
    }

    private void generateNextBossIdFromDB() {
        // ... (Original logic renamed)
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                return ShopManagerDAO.gI().getMinBossId();
            }

            @Override
            protected void done() {
                try {
                    int minId = get();
                    int nextId = (minId == 0) ? -1 : minId - 1;
                    txtBossId.setText(String.valueOf(nextId));
                } catch (Exception e) {
                    txtBossId.setText("-999999");
                }
            }
        };
        worker.execute();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Left panel - Form inputs
        JPanel leftPanel = createFormPanel();
        mainPanel.add(leftPanel, BorderLayout.WEST);

        // Center panel - Canvas + Outfit
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Right panel - Skill Editor + Output
        JPanel rightPanel = createRightPanel();
        mainPanel.add(rightPanel, BorderLayout.EAST);

        // Bottom panel - Buttons
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(280, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Thông tin Boss"));

        // Boss Name
        txtBossName = new JTextField("New Boss");
        panel.add(createFormRow("Tên Boss:", txtBossName));

        // Define Name (Auto-generated)
        JPanel defineNamePanel = new JPanel(new BorderLayout(5, 0));
        txtDefineName = new JTextField();
        lblDefineNameStatus = new JLabel("OK");
        lblDefineNameStatus.setForeground(new Color(40, 167, 69));
        lblDefineNameStatus.setPreferredSize(new Dimension(30, 25));
        defineNamePanel.add(txtDefineName, BorderLayout.CENTER);
        defineNamePanel.add(lblDefineNameStatus, BorderLayout.EAST);
        panel.add(createFormRow("Boss Define Name:", defineNamePanel));

        // Add Listeners
        txtBossName.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                generateDefineName();
            }

            public void removeUpdate(DocumentEvent e) {
                generateDefineName();
            }

            public void changedUpdate(DocumentEvent e) {
                generateDefineName();
            }
        });

        txtDefineName.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                validateDefineName();
            }

            public void removeUpdate(DocumentEvent e) {
                validateDefineName();
            }

            public void changedUpdate(DocumentEvent e) {
                validateDefineName();
            }
        });

        // Boss ID
        panel.add(createFormRow("Boss ID (số âm):", txtBossId = new JTextField("-999999")));

        // Gender
        cboGender = new JComboBox<>(new String[] { "Trái Đất (0)", "Namec (1)", "Xayda (2)" });
        panel.add(createFormRow("Giới tính:", cboGender));

        // Dame
        panel.add(createFormRow("Dame:", txtDame = new JTextField("10000")));

        // HP
        JPanel hpPanel = createFormRow("HP (phẩy = nhiều giá trị):", txtHp = new JTextField("1000000"));
        panel.add(hpPanel);

        // Map Join
        panel.add(createFormRow("Map IDs (phẩy):", txtMapJoin = new JTextField("5, 10, 15")));

        // Seconds Rest
        panel.add(createFormRow("Thời gian respawn (s):", txtSecondsRest = new JTextField("60")));

        panel.add(Box.createVerticalStrut(10));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(5));

        // Text Chat sections
        JLabel lblChat = new JLabel("== Text Chat ==");
        lblChat.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblChat.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblChat);

        panel.add(createFormRow("Chat xuất hiện:", txtTextS = new JTextField("Ta đã đến")));
        panel.add(createFormRow("Chat khi đánh:", txtTextM = new JTextField("Haha!")));
        panel.add(createFormRow("Chat khi chết:", txtTextE = new JTextField("Ta sẽ quay lại")));

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createFormRow(String label, JComponent component) {
        JPanel row = new JPanel(new BorderLayout(5, 2));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        row.add(lbl, BorderLayout.NORTH);
        row.add(component, BorderLayout.CENTER);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Top - Outfit selectors
        JPanel outfitPanel = new JPanel(new GridLayout(4, 1, 5, 5)); // 4 rows now
        outfitPanel.setBorder(BorderFactory.createTitledBorder("Chọn Outfit (Head, Body, Leg)"));
        outfitPanel.setPreferredSize(new Dimension(0, 220)); // Increased height

        outfitPanel.add(createOutfitRow("Head", btnHead = new JButton("Chọn Head"),
                cboHeadPath = new JComboBox<>(), lblHeadInfo = new JLabel("ID: --")));
        outfitPanel.add(createOutfitRow("Body", btnBody = new JButton("Chọn Body"),
                cboBodyPath = new JComboBox<>(), lblBodyInfo = new JLabel("ID: --")));
        outfitPanel.add(createOutfitRow("Leg", btnLeg = new JButton("Chọn Leg"),
                cboLegPath = new JComboBox<>(), lblLegInfo = new JLabel("ID: --")));

        // Add "Chọn Cải trang" button row
        JPanel caiTrangRow = new JPanel(new BorderLayout(5, 5));
        JLabel lblCaiTrang = new JLabel("Cải trang:");
        lblCaiTrang.setPreferredSize(new Dimension(50, 25));
        caiTrangRow.add(lblCaiTrang, BorderLayout.WEST);

        JButton btnCaiTrang = new JButton("Chọn Cải trang");
        btnCaiTrang.setBackground(new Color(156, 39, 176)); // Purple color
        btnCaiTrang.setForeground(Color.WHITE);
        btnCaiTrang.addActionListener(e -> showCaiTrangDialog());
        caiTrangRow.add(btnCaiTrang, BorderLayout.CENTER);

        outfitPanel.add(caiTrangRow);

        // Button actions
        btnHead.addActionListener(e -> selectOutfitImage("head"));
        btnBody.addActionListener(e -> selectOutfitImage("body"));
        btnLeg.addActionListener(e -> selectOutfitImage("leg"));

        // Path combobox selection listeners
        cboHeadPath.addActionListener(e -> onPathSelected("head"));
        cboBodyPath.addActionListener(e -> onPathSelected("body"));
        cboLegPath.addActionListener(e -> onPathSelected("leg"));

        panel.add(outfitPanel, BorderLayout.NORTH);

        // Center - Canvas
        canvas = new BossCanvas();
        JPanel canvasPanel = new JPanel(new BorderLayout());
        canvasPanel.setBorder(BorderFactory.createTitledBorder("Preview Boss"));
        canvasPanel.add(canvas, BorderLayout.CENTER);
        panel.add(canvasPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createOutfitRow(String partName, JButton btn, JComboBox<String> cbo, JLabel infoLabel) {
        JPanel row = new JPanel(new BorderLayout(5, 5));

        JLabel lbl = new JLabel(partName + ":");
        lbl.setPreferredSize(new Dimension(50, 25));
        row.add(lbl, BorderLayout.WEST);

        btn.setPreferredSize(new Dimension(100, 25));
        btn.setBackground(new Color(70, 130, 180));
        btn.setForeground(Color.WHITE);
        row.add(btn, BorderLayout.CENTER);

        JPanel rightPart = new JPanel(new BorderLayout(5, 0));
        cbo.setPreferredSize(new Dimension(120, 25));
        rightPart.add(cbo, BorderLayout.CENTER);
        infoLabel.setPreferredSize(new Dimension(80, 25));
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rightPart.add(infoLabel, BorderLayout.EAST);
        row.add(rightPart, BorderLayout.EAST);

        return row;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(350, 0));

        // Skill Editor
        JPanel skillPanel = new JPanel(new BorderLayout(5, 5));
        skillPanel.setBorder(BorderFactory.createTitledBorder("Skill Editor"));
        skillPanel.setPreferredSize(new Dimension(0, 250));

        skillTableModel = new DefaultTableModel(new Object[] { "Skill ID", "Level", "Cooldown (ms)" }, 0);
        skillTable = new JTable(skillTableModel);
        skillTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        skillTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        skillTable.getColumnModel().getColumn(2).setPreferredWidth(80);

        // Setup ComboBox Editor for Skill ID
        cboSkillEditor = new JComboBox<>();
        skillTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(cboSkillEditor));

        // Setup ComboBox Editor for Level (1-7)
        JComboBox<String> cboLevelEditor = new JComboBox<>(new String[] { "1", "2", "3", "4", "5", "6", "7" });
        skillTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(cboLevelEditor));

        // Load skills
        loadSkills();

        // Add default skills
        addDefaultSkills();

        JScrollPane skillScroll = new JScrollPane(skillTable);
        skillPanel.add(skillScroll, BorderLayout.CENTER);

        JPanel skillBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddSkill = new JButton("+ Add");
        btnAddSkill.setBackground(new Color(40, 167, 69));
        btnAddSkill.setForeground(Color.WHITE);
        btnAddSkill.addActionListener(e -> skillTableModel.addRow(new Object[] { "0", "7", "1000" }));

        JButton btnRemoveSkill = new JButton("- Remove");
        btnRemoveSkill.setBackground(new Color(220, 53, 69));
        btnRemoveSkill.setForeground(Color.WHITE);
        btnRemoveSkill.addActionListener(e -> {
            int row = skillTable.getSelectedRow();
            if (row >= 0)
                skillTableModel.removeRow(row);
        });

        skillBtnPanel.add(btnAddSkill);
        skillBtnPanel.add(btnRemoveSkill);
        skillPanel.add(skillBtnPanel, BorderLayout.SOUTH);
        panel.add(skillPanel, BorderLayout.NORTH);

        // Output TextArea
        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        outputPanel.setBorder(BorderFactory.createTitledBorder("Generated Code"));

        outputTextArea = new JTextArea();
        outputTextArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        outputTextArea.setEditable(false);
        JScrollPane outputScroll = new JScrollPane(outputTextArea);
        outputPanel.add(outputScroll, BorderLayout.CENTER);

        JButton btnCopy = new JButton("Copy to Clipboard");
        btnCopy.setBackground(new Color(13, 110, 253));
        btnCopy.setForeground(Color.WHITE);
        btnCopy.addActionListener(e -> {
            outputTextArea.selectAll();
            outputTextArea.copy();
            JOptionPane.showMessageDialog(this, "Đã copy code!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
        outputPanel.add(btnCopy, BorderLayout.SOUTH);

        panel.add(outputPanel, BorderLayout.CENTER);

        return panel;
    }

    private void addDefaultSkills() {
        // Add some default skills
        skillTableModel.addRow(new Object[] { "7", "7", "1000" }); // DRAGON
        skillTableModel.addRow(new Object[] { "2", "7", "1000" }); // KAMEJOKO
        skillTableModel.addRow(new Object[] { "8", "7", "1000" }); // DEMON
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Animation Controls
        JPanel animPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        JButton btnPrev = new JButton("<< Prev");
        JButton btnNext = new JButton("Next >>");
        JCheckBox chkAuto = new JCheckBox("Auto Play", false);
        JLabel lblFrame = new JLabel("Frame: 0");

        btnPrev.addActionListener(e -> {
            changeFrame(-1);
            chkAuto.setSelected(false);
            stopAnimation();
        });

        btnNext.addActionListener(e -> {
            changeFrame(1);
            chkAuto.setSelected(false);
            stopAnimation();
        });

        chkAuto.addActionListener(e -> {
            if (chkAuto.isSelected()) {
                startAnimation();
            } else {
                stopAnimation();
            }
        });

        animPanel.add(btnPrev);
        animPanel.add(lblFrame);
        animPanel.add(btnNext);
        animPanel.add(chkAuto);

        // Main Actions
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton btnGenerate = new JButton("📄 Generate Code");
        btnGenerate.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnGenerate.setBackground(new Color(255, 193, 7));
        btnGenerate.setForeground(Color.BLACK);
        btnGenerate.setPreferredSize(new Dimension(180, 40));
        btnGenerate.addActionListener(e -> generateCode());

        JButton btnReset = new JButton("🔄 Reset");
        btnReset.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnReset.setBackground(new Color(108, 117, 125));
        btnReset.setForeground(Color.WHITE);
        btnReset.setPreferredSize(new Dimension(120, 40));
        btnReset.addActionListener(e -> resetForm());

        actionPanel.add(btnGenerate);
        actionPanel.add(btnReset);

        panel.add(animPanel, BorderLayout.NORTH);
        panel.add(actionPanel, BorderLayout.CENTER);

        // Expose label for update
        this.lblFrameStatus = lblFrame;

        return panel;
    }

    private JLabel lblFrameStatus;

    private void changeFrame(int delta) {
        currentFrame += delta;
        if (currentFrame < 0)
            currentFrame = 0; // Prevent negative lookup logic
        // We don't cap max here because parts have diff lengths, we mod in draw
        updateFrameLabel();
        canvas.repaint();
    }

    private void updateFrameLabel() {
        if (lblFrameStatus != null) {
            String animName = "Unknown";
            if (CHAR_INFO != null && CHAR_INFO.length > 0) { // Safety check
                int animIdx = currentFrame % CHAR_INFO.length;
                if (animIdx >= 0 && animIdx < ANIM_NAMES.length) {
                    animName = ANIM_NAMES[animIdx];
                }
            }
            lblFrameStatus.setText("Frame: " + currentFrame + " (" + animName + ")");
        }
    }

    private void startAnimation() {
        if (animationTimer == null) {
            animationTimer = new Timer(150, e -> {
                currentFrame++;
                updateFrameLabel();
                canvas.repaint();
            });
        }
        isAnimating = true;
        animationTimer.start();
    }

    private void stopAnimation() {
        isAnimating = false;
        if (animationTimer != null)
            animationTimer.stop();
    }

    private void selectOutfitImage(String partType) {
        // Sử dụng Windows Explorer native (FileDialog)
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        FileDialog dialog = new FileDialog(frame, "Chọn ảnh " + partType, FileDialog.LOAD);

        // Set thư mục: ưu tiên thư mục cuối cùng đã chọn, nếu chưa có thì dùng mặc định
        dialog.setDirectory(lastImageDir != null ? lastImageDir : ICON_PATH);

        // Filter file ảnh
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg") || lower.endsWith(".gif");
        });

        dialog.setVisible(true);

        String fileName = dialog.getFile();
        if (fileName == null)
            return;

        // Lưu lại thư mục đã chọn để lần sau mở nhanh hơn
        lastImageDir = dialog.getDirectory();
        File selectedFile = new File(dialog.getDirectory(), fileName);

        try {
            BufferedImage img = ImageIO.read(selectedFile);
            int iconId = extractIconIdFromFilename(selectedFile.getName());

            switch (partType) {
                case "head":
                    headImage = img;
                    headIconId = iconId;
                    headFrames.clear(); // Clear old frames immediately
                    headPos = null;
                    lblHeadInfo.setText("ID: " + iconId);
                    lookupPathsForIcon(iconId, "head");
                    break;
                case "body":
                    bodyImage = img;
                    bodyIconId = iconId;
                    bodyFrames.clear();
                    bodyPos = null;
                    lblBodyInfo.setText("ID: " + iconId);
                    lookupPathsForIcon(iconId, "body");
                    break;
                case "leg":
                    legImage = img;
                    legIconId = iconId;
                    legFrames.clear();
                    legPos = null;
                    lblLegInfo.setText("ID: " + iconId);
                    lookupPathsForIcon(iconId, "leg");
                    break;
            }

            canvas.repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Không thể load ảnh: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showCaiTrangDialog() {
        // Load cải trang list in background
        SwingWorker<List<ShopManagerDAO.CaiTrangTemplate>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ShopManagerDAO.CaiTrangTemplate> doInBackground() {
                return ShopManagerDAO.gI().getAllCaiTrang();
            }

            @Override
            protected void done() {
                try {
                    List<ShopManagerDAO.CaiTrangTemplate> caiTrangList = get();
                    if (caiTrangList.isEmpty()) {
                        JOptionPane.showMessageDialog(CreateBossScr.this,
                                "Không tìm thấy cải trang nào trong database!",
                                "Thông báo", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Create dialog
                    JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(CreateBossScr.this),
                            "Chọn Cải trang", true);
                    dialog.setSize(900, 700);
                    dialog.setLocationRelativeTo(CreateBossScr.this);

                    // Search field panel
                    JPanel searchPanel = new JPanel(new BorderLayout());
                    searchPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
                    JLabel lblSearch = new JLabel("Tìm kiếm: ");
                    lblSearch.setFont(new Font("SansSerif", Font.BOLD, 12));
                    JTextField txtSearch = new JTextField();
                    txtSearch.setPreferredSize(new Dimension(0, 28));
                    txtSearch.setFont(new Font("SansSerif", Font.PLAIN, 13));
                    searchPanel.add(lblSearch, BorderLayout.WEST);
                    searchPanel.add(txtSearch, BorderLayout.CENTER);

                    // Grid panel for cải trang items
                    JPanel gridPanel = new JPanel(new GridLayout(0, 4, 10, 10));
                    gridPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

                    // Method to populate grid
                    Runnable populateGrid = () -> {
                        gridPanel.removeAll();
                        String searchText = txtSearch.getText().toLowerCase().trim();

                        for (ShopManagerDAO.CaiTrangTemplate ct : caiTrangList) {
                            if (!searchText.isEmpty() && !ct.name.toLowerCase().contains(searchText)) {
                                continue;
                            }

                            JPanel itemPanel = new JPanel(new BorderLayout(5, 5));
                            itemPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                            itemPanel.setBackground(Color.WHITE);
                            itemPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                            // Load icon
                            JLabel iconLabel = new JLabel();
                            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
                            iconLabel.setPreferredSize(new Dimension(64, 64));
                            try {
                                File iconFile = new File(ICON_PATH + "/" + ct.iconId + ".png");
                                if (iconFile.exists()) {
                                    BufferedImage iconImg = ImageIO.read(iconFile);
                                    // Scale to fit
                                    int size = 56;
                                    BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                                    java.awt.Graphics2D g2d = scaled.createGraphics();
                                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                    double scale = Math.min((double) size / iconImg.getWidth(),
                                            (double) size / iconImg.getHeight());
                                    int w = (int) (iconImg.getWidth() * scale);
                                    int h = (int) (iconImg.getHeight() * scale);
                                    g2d.drawImage(iconImg, (size - w) / 2, (size - h) / 2, w, h, null);
                                    g2d.dispose();
                                    iconLabel.setIcon(new ImageIcon(scaled));
                                } else {
                                    iconLabel.setText("ID: " + ct.iconId);
                                }
                            } catch (Exception e) {
                                iconLabel.setText("ID: " + ct.iconId);
                            }

                            // Name label with multi-line - use black text for visibility
                            JLabel nameLabel = new JLabel("<html><center>" + ct.name + "</center></html>");
                            nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
                            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
                            nameLabel.setForeground(Color.BLACK);

                            itemPanel.add(iconLabel, BorderLayout.CENTER);
                            itemPanel.add(nameLabel, BorderLayout.SOUTH);

                            // Click listener
                            itemPanel.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    applyCaiTrang(ct);
                                    dialog.dispose();
                                }

                                @Override
                                public void mouseEntered(MouseEvent e) {
                                    itemPanel.setBackground(new Color(230, 230, 250));
                                }

                                @Override
                                public void mouseExited(MouseEvent e) {
                                    itemPanel.setBackground(Color.WHITE);
                                }
                            });

                            gridPanel.add(itemPanel);
                        }
                        gridPanel.revalidate();
                        gridPanel.repaint();
                    };

                    // Initial populate
                    populateGrid.run();

                    // Search listener
                    txtSearch.getDocument().addDocumentListener(new DocumentListener() {
                        public void insertUpdate(DocumentEvent e) {
                            populateGrid.run();
                        }

                        public void removeUpdate(DocumentEvent e) {
                            populateGrid.run();
                        }

                        public void changedUpdate(DocumentEvent e) {
                            populateGrid.run();
                        }
                    });

                    // Scroll pane
                    JScrollPane scrollPane = new JScrollPane(gridPanel);
                    scrollPane.getVerticalScrollBar().setUnitIncrement(16);

                    dialog.setLayout(new BorderLayout());
                    dialog.add(searchPanel, BorderLayout.NORTH);
                    dialog.add(scrollPane, BorderLayout.CENTER);

                    dialog.setVisible(true);

                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(CreateBossScr.this,
                            "Lỗi load cải trang: " + e.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void applyCaiTrang(ShopManagerDAO.CaiTrangTemplate ct) {
        // Set selected path IDs from cải trang
        selectedHeadPathId = ct.head;
        selectedBodyPathId = ct.body;
        selectedLegPathId = ct.leg;

        // Update labels
        lblHeadInfo.setText("ID: " + ct.head);
        lblBodyInfo.setText("ID: " + ct.body);
        lblLegInfo.setText("ID: " + ct.leg);

        // Clear existing frames
        headFrames.clear();
        bodyFrames.clear();
        legFrames.clear();
        headPos = null;
        bodyPos = null;
        legPos = null;

        // Clear comboboxes and add selected path
        cboHeadPath.removeAllItems();
        cboBodyPath.removeAllItems();
        cboLegPath.removeAllItems();

        headPathIds.clear();
        bodyPathIds.clear();
        legPathIds.clear();

        headPathIds.add(ct.head);
        bodyPathIds.add(ct.body);
        legPathIds.add(ct.leg);

        cboHeadPath.addItem("Path ID: " + ct.head);
        cboBodyPath.addItem("Path ID: " + ct.body);
        cboLegPath.addItem("Path ID: " + ct.leg);

        // Load path positions and images
        loadPathPositions();

        // Load first frame icons for display
        loadFirstFrameIcons();

        JOptionPane.showMessageDialog(this,
                "Đã áp dụng cải trang: " + ct.name + "\n" +
                        "Head: " + ct.head + ", Body: " + ct.body + ", Leg: " + ct.leg,
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadFirstFrameIcons() {
        // Load icons from path data for preview
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    // Load head icon
                    if (selectedHeadPathId > 0) {
                        ShopManagerDAO.PartData headData = ShopManagerDAO.gI().getPartData(selectedHeadPathId);
                        if (headData != null) {
                            int iconId = extractFirstIconIdFromJson(headData.data);
                            if (iconId > 0) {
                                headIconId = iconId;
                                File iconFile = new File(ICON_PATH + "/" + iconId + ".png");
                                if (iconFile.exists()) {
                                    headImage = ImageIO.read(iconFile);
                                }
                            }
                        }
                    }
                    // Load body icon
                    if (selectedBodyPathId > 0) {
                        ShopManagerDAO.PartData bodyData = ShopManagerDAO.gI().getPartData(selectedBodyPathId);
                        if (bodyData != null) {
                            int iconId = extractFirstIconIdFromJson(bodyData.data);
                            if (iconId > 0) {
                                bodyIconId = iconId;
                                File iconFile = new File(ICON_PATH + "/" + iconId + ".png");
                                if (iconFile.exists()) {
                                    bodyImage = ImageIO.read(iconFile);
                                }
                            }
                        }
                    }
                    // Load leg icon
                    if (selectedLegPathId > 0) {
                        ShopManagerDAO.PartData legData = ShopManagerDAO.gI().getPartData(selectedLegPathId);
                        if (legData != null) {
                            int iconId = extractFirstIconIdFromJson(legData.data);
                            if (iconId > 0) {
                                legIconId = iconId;
                                File iconFile = new File(ICON_PATH + "/" + iconId + ".png");
                                if (iconFile.exists()) {
                                    legImage = ImageIO.read(iconFile);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                canvas.repaint();
            }
        };
        worker.execute();
    }

    private int extractFirstIconIdFromJson(String jsonData) {
        if (jsonData == null || jsonData.isEmpty())
            return 0;
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(jsonData);
            if (obj instanceof JSONArray) {
                JSONArray arr = (JSONArray) obj;
                if (!arr.isEmpty()) {
                    Object first = arr.get(0);
                    JSONArray frameData = null;
                    if (first instanceof JSONArray) {
                        frameData = (JSONArray) first;
                    } else if (first instanceof String) {
                        String s = (String) first;
                        if (s.startsWith("[") && s.endsWith("]")) {
                            Object parsed = parser.parse(s);
                            if (parsed instanceof JSONArray) {
                                frameData = (JSONArray) parsed;
                            }
                        }
                    }
                    if (frameData != null && !frameData.isEmpty()) {
                        return ((Long) frameData.get(0)).intValue();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error extracting icon ID: " + e.getMessage());
        }
        return 0;
    }

    private int extractIconIdFromFilename(String filename) {
        try {
            // Remove extension and parse as number
            String name = filename.substring(0, filename.lastIndexOf('.'));
            return Integer.parseInt(name);
        } catch (Exception e) {
            return 0;
        }
    }

    private void lookupPathsForIcon(int iconId, String partType) {
        // Query DB to find paths containing this icon ID
        SwingWorker<List<Integer>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Integer> doInBackground() {
                return ShopManagerDAO.gI().findPathsByIconId(iconId);
            }

            @Override
            protected void done() {
                try {
                    List<Integer> paths = get();
                    JComboBox<String> cbo;
                    List<Integer> pathList;

                    switch (partType) {
                        case "head":
                            cbo = cboHeadPath;
                            pathList = headPathIds;
                            break;
                        case "body":
                            cbo = cboBodyPath;
                            pathList = bodyPathIds;
                            break;
                        case "leg":
                            cbo = cboLegPath;
                            pathList = legPathIds;
                            break;
                        default:
                            return;
                    }

                    pathList.clear();
                    cbo.removeAllItems();

                    if (paths.isEmpty()) {
                        cbo.addItem("Không tìm thấy path");
                    } else {
                        for (Integer pathId : paths) {
                            pathList.add(pathId);
                            cbo.addItem("Path ID: " + pathId);
                        }
                        // Auto-select first
                        cbo.setSelectedIndex(0);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void onPathSelected(String partType) {
        int selectedIndex;
        List<Integer> pathList;

        switch (partType) {
            case "head":
                selectedIndex = cboHeadPath.getSelectedIndex();
                pathList = headPathIds;
                if (selectedIndex >= 0 && selectedIndex < pathList.size()) {
                    selectedHeadPathId = pathList.get(selectedIndex);
                } else {
                    selectedHeadPathId = 0;
                    headFrames.clear();
                    headPos = null;
                }
                break;
            case "body":
                selectedIndex = cboBodyPath.getSelectedIndex();
                pathList = bodyPathIds;
                if (selectedIndex >= 0 && selectedIndex < pathList.size()) {
                    selectedBodyPathId = pathList.get(selectedIndex);
                } else {
                    selectedBodyPathId = 0;
                    bodyFrames.clear();
                    bodyPos = null;
                }
                break;
            case "leg":
                selectedIndex = cboLegPath.getSelectedIndex();
                pathList = legPathIds;
                if (selectedIndex >= 0 && selectedIndex < pathList.size()) {
                    selectedLegPathId = pathList.get(selectedIndex);
                } else {
                    selectedLegPathId = 0;
                    legFrames.clear();
                    legPos = null;
                }
                break;
        }

        // Load path data and update positions
        loadPathPositions();
    }

    private void loadPathPositions() {
        // Load positions from selected paths
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                if (selectedHeadPathId > 0) {
                    PartData data = ShopManagerDAO.gI().getPartData(selectedHeadPathId);
                    if (data != null) {
                        parsePosition(data.data, "head");
                    }
                }
                if (selectedBodyPathId > 0) {
                    PartData data = ShopManagerDAO.gI().getPartData(selectedBodyPathId);
                    if (data != null) {
                        parsePosition(data.data, "body");
                    }
                }
                if (selectedLegPathId > 0) {
                    PartData data = ShopManagerDAO.gI().getPartData(selectedLegPathId);
                    if (data != null) {
                        parsePosition(data.data, "leg");
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                canvas.repaint();
            }
        };
        worker.execute();
    }

    private void parsePosition(String jsonData, String partType) {
        if (jsonData == null || jsonData.isEmpty())
            return;

        // Reset frames
        switch (partType) {
            case "head":
                headFrames.clear();
                break;
            case "body":
                bodyFrames.clear();
                break;
            case "leg":
                legFrames.clear();
                break;
        }

        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(jsonData);
            if (obj instanceof JSONArray) {
                JSONArray arr = (JSONArray) obj;

                // Iterate ALL frames
                for (int i = 0; i < arr.size(); i++) {
                    Object frame = arr.get(i);
                    JSONArray frameData = null;
                    if (frame instanceof JSONArray) {
                        frameData = (JSONArray) frame;
                    } else if (frame instanceof String) {
                        String s = (String) frame;
                        if (s.startsWith("[") && s.endsWith("]")) {
                            Object parsedC = parser.parse(s);
                            if (parsedC instanceof JSONArray) {
                                frameData = (JSONArray) parsedC;
                            }
                        }
                    }

                    if (frameData != null && frameData.size() >= 3) {
                        int iconId = ((Long) frameData.get(0)).intValue();
                        int dx = ((Long) frameData.get(1)).intValue();
                        int dy = ((Long) frameData.get(2)).intValue();

                        // Add to frames list
                        PartFrame pf = new PartFrame(iconId, dx, dy);
                        switch (partType) {
                            case "head":
                                headFrames.add(pf);
                                break;
                            case "body":
                                bodyFrames.add(pf);
                                break;
                            case "leg":
                                legFrames.add(pf);
                                break;
                        }

                        // Use first frame for static pos
                        if (i == 0) {
                            switch (partType) {
                                case "head":
                                    headPos = new Point(dx * 4, dy * 4);
                                    break;
                                case "body":
                                    bodyPos = new Point(dx * 4, dy * 4);
                                    break;
                                case "leg":
                                    legPos = new Point(dx * 4, dy * 4);
                                    break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing position: " + e.getMessage());
            e.printStackTrace();
        }

        // Debug frame count
        int count = 0;
        switch (partType) {
            case "head":
                count = headFrames.size();
                break;
            case "body":
                count = bodyFrames.size();
                break;
            case "leg":
                count = legFrames.size();
                break;
        }
        System.out.println("Parsed " + partType + ": " + count + " frames from data: " + jsonData);
    }

    private void generateCode() {
        StringBuilder sb = new StringBuilder();

        String bossName = txtBossName.getText().trim();
        String defineName = txtDefineName.getText().trim();
        String bossId = txtBossId.getText().trim();
        int gender = cboGender.getSelectedIndex();
        String dame = txtDame.getText().trim();
        String hp = txtHp.getText().trim();
        String mapJoin = txtMapJoin.getText().trim();
        String secondsRest = txtSecondsRest.getText().trim();
        String textS = txtTextS.getText().trim();
        String textM = txtTextM.getText().trim();
        String textE = txtTextE.getText().trim();

        String className = bossName.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]", "");

        // === BOSS ID ===
        sb.append("// ==================== BƯỚC 1: Thêm vào BossID.java ====================\n");
        sb.append("public static final int ").append(defineName)
                .append(" = ").append(bossId).append(";\n\n");

        // === BOSS CLASS ===
        sb.append("// ==================== BƯỚC 2: Tạo file ").append(className).append(".java ====================\n");
        sb.append("package boss.boss_manifest.").append(className).append(";\n\n");
        sb.append("import boss.Boss;\n");
        sb.append("import boss.BossData;\n");
        sb.append("import boss.BossID;\n");
        sb.append("import boss.BossStatus;\n");
        sb.append("import consts.ConstPlayer;\n");
        sb.append("import map.ItemMap;\n");
        sb.append("import map.Zone;\n");
        sb.append("import player.Player;\n");
        sb.append("import skill.Skill;\n");
        sb.append("import utils.Util;\n");
        sb.append("import services.EffectSkillService;\n");
        sb.append("import services.Service;\n");
        sb.append("import services.SkillService;\n");
        sb.append("import services.PetService;\n");
        sb.append("import services.func.ChangeMapService;\n\n");

        sb.append("public class ").append(className).append(" extends Boss {\n\n");
        sb.append("    public ").append(className).append("() throws Exception {\n");
        sb.append("        super(BossID.").append(defineName).append(", new BossData(\n"); // Fixed: Use defineName
        sb.append("            \"").append(bossName).append("\", // name\n");
        sb.append("            ConstPlayer.").append(getGenderConst(gender)).append(", // gender\n");
        sb.append("            new short[]{").append(selectedHeadPathId).append(", ")
                .append(selectedBodyPathId).append(", ").append(selectedLegPathId)
                .append(", -1, -1, -1}, // outfit {head, body, leg, bag, aura, eff}\n");
        sb.append("            ").append(dame).append(", // dame\n");
        sb.append("            new long[]{").append(formatHpArray(hp)).append("}, // hp\n");
        sb.append("            new int[]{").append(mapJoin).append("}, // map join\n");

        // Skills
        sb.append("            new int[][]{\n");
        for (int i = 0; i < skillTableModel.getRowCount(); i++) {
            Object idObj = skillTableModel.getValueAt(i, 0);
            String skillId;
            if (idObj instanceof ShopManagerDAO.SkillTemplate) {
                skillId = String.valueOf(((ShopManagerDAO.SkillTemplate) idObj).id);
            } else {
                skillId = idObj.toString();
            }

            String level = skillTableModel.getValueAt(i, 1).toString();
            String cooldown = skillTableModel.getValueAt(i, 2).toString();
            sb.append("                {").append(skillId).append(", ").append(level)
                    .append(", ").append(cooldown).append("}");
            if (i < skillTableModel.getRowCount() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("            }, // skills\n");

        // Text chats
        sb.append("            new String[]{\"|-1|").append(textS).append("\"}, // textS\n");
        sb.append("            new String[]{\"|-1|").append(textM).append("\"}, // textM\n");
        sb.append("            new String[]{\"|-1|").append(textE).append("\"}, // textE\n");
        sb.append("            ").append(secondsRest).append(" // secondsRest\n");

        sb.append("        ));\n");
        sb.append("    }\n\n");

        // Overrides
        sb.append("    @Override\n");
        sb.append("    public void reward(Player plKill) {\n");
        sb.append(
                "        // int[] itemDos = new int[]{555, 557, 559, 556, 558, 560, 562, 564, 566, 563, 565, 567};\n");
        sb.append("        // int[] NRO = new int[]{17, 18};\n");
        sb.append("        // int randomDo = Util.nextInt(itemDos.length);\n");
        sb.append("        // int randomNRO = Util.nextInt(NRO.length);\n");
        sb.append("        // if (Util.isTrue(1, 10)) {\n");
        sb.append("        //     if (Util.isTrue(1, 10)) {\n");
        sb.append(
                "        //         Service.gI().dropItemMap(this.zone, Util.ratiItem(zone, 16, 1, this.location.x, this.location.y, plKill.id));\n");
        sb.append("        //         return;\n");
        sb.append("        //     }\n");
        sb.append(
                "        //     Service.gI().dropItemMap(this.zone, Util.ratiItem(zone, NRO[randomNRO], 1, this.location.x, this.location.y, plKill.id));\n");
        sb.append("        //     return;\n");
        sb.append("        // } else {\n");
        sb.append(
                "        //     Service.gI().dropItemMap(this.zone, Util.ratiItem(zone, itemDos[randomDo], 1, this.location.x, this.location.y, plKill.id));\n");
        sb.append("        // }\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public void active() {\n");
        sb.append("        super.active();\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public void joinMap() {\n");
        sb.append("        super.joinMap(); // Default join map\n");
        sb.append("        // st = System.currentTimeMillis();\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public void leaveMap() {\n");
        sb.append("        super.leaveMap();\n");
        sb.append("        // ChangeMapService.gI().exitMap(this);\n");
        sb.append("        // this.lastZone = null;\n");
        sb.append("        // this.lastTimeRest = System.currentTimeMillis();\n");
        sb.append("        // this.changeStatus(BossStatus.REST);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public void autoLeaveMap() {\n");
        sb.append("        // if (Util.canDoWithTime(st, 900000)) {\n");
        sb.append("        //     this.leaveMap();\n");
        sb.append("        // }\n");
        sb.append("        // if (this.zone != null && this.zone.getNumOfPlayers() > 0) {\n");
        sb.append("        //     st = System.currentTimeMillis();\n");
        sb.append("        // }\n");
        sb.append("        super.autoLeaveMap();\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append(
                "    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {\n");
        sb.append("        if (!this.isDie()) {\n");
        sb.append("            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {\n");
        sb.append("                this.chat(\"Xí hụt\");\n");
        sb.append("                return 0;\n");
        sb.append("            }\n");
        sb.append("            damage = this.nPoint.subDameInjureWithDeff(damage/2);\n");
        sb.append("            if (!piercing && effectSkill.isShielding) {\n");
        sb.append("                if (damage > nPoint.hpMax) {\n");
        sb.append("                    EffectSkillService.gI().breakShield(this);\n");
        sb.append("                }\n");
        sb.append("                damage = 1;\n");
        sb.append("            }\n");
        sb.append("            return super.injured(plAtt, damage, piercing, isMobAttack);\n");
        sb.append("        }\n");
        sb.append("        return 0;\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public void attack() {\n");
        sb.append(
                "        // if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {\n");
        sb.append("        //     this.lastTimeAttack = System.currentTimeMillis();\n");
        sb.append("        //     try {\n");
        sb.append("        //         Player pl = getPlayerAttack();\n");
        sb.append("        //         if (pl == null || pl.isDie()) return;\n");
        sb.append(
                "        //         this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(this.playerSkill.skills.size()));\n");
        sb.append("        //         if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {\n");
        sb.append("        //             if (Util.isTrue(5, 20)) {\n");
        sb.append("        //                 if (SkillUtil.isUseSkillChuong(this)) {\n");
        sb.append(
                "        //                     this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)), Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));\n");
        sb.append("        //                 } else {\n");
        sb.append(
                "        //                     this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)), Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50));\n");
        sb.append("        //                 }\n");
        sb.append("        //             }\n");
        sb.append("        //             SkillService.gI().useSkill(this, pl, null, -1, null);\n");
        sb.append("        //             checkPlayerDie(pl);\n");
        sb.append("        //         } else {\n");
        sb.append("        //             if (Util.isTrue(1, 2)) this.moveToPlayer(pl);\n");
        sb.append("        //         }\n");
        sb.append("        //     } catch (Exception ex) { ex.printStackTrace(); }\n");
        sb.append("        // }\n");
        sb.append("        super.attack();\n");
        sb.append("    }\n");

        sb.append("}\n\n");

        // === BOSS MANAGER ===
        sb.append("// ==================== BƯỚC 3: Thêm vào BossManager.java ====================\n\n");
        sb.append("// Trong method createBoss(int bossID), thêm case:\n");
        sb.append("case BossID.").append(defineName).append(" -> new ").append(className)
                .append("();\n\n");
        sb.append("// Trong method loadBoss(), thêm dòng:\n");
        sb.append("this.createBoss(BossID.").append(defineName).append(");\n");

        outputTextArea.setText(sb.toString());
        outputTextArea.setCaretPosition(0); // Scroll to top
    }

    private String getGenderConst(int gender) {
        switch (gender) {
            case 0:
                return "TRAI_DAT";
            case 1:
                return "NAMEC";
            case 2:
                return "XAYDA";
            default:
                return "TRAI_DAT";
        }
    }

    private String formatHpArray(String hp) {
        // Convert comma-separated values to proper format
        String[] parts = hp.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i].trim()).append("L");
            if (i < parts.length - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    private void generateDefineName() {
        if (!txtBossName.isFocusOwner())
            return; // Only process if user is typing name

        String input = txtBossName.getText().trim();
        if (input.isEmpty()) {
            txtDefineName.setText("");
            return;
        }

        // Convert to uppercase
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String result = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        result = result.toUpperCase();

        // Replace spaces with UNDERSCORE
        result = result.replaceAll("\\s+", "_");

        // Remove non-alphanumeric (except underscore)
        result = result.replaceAll("[^A-Z0-9_]", "");

        txtDefineName.setText(result);
        validateDefineName();
    }

    private void validateDefineName() {
        String currentName = txtDefineName.getText().trim();
        if (existingDefineNames.contains(currentName)) {
            lblDefineNameStatus.setText("EXIST");
            lblDefineNameStatus.setForeground(Color.RED);
            txtDefineName.setBackground(new Color(255, 230, 230));
        } else if (currentName.isEmpty()) {
            lblDefineNameStatus.setText("--");
            lblDefineNameStatus.setForeground(Color.GRAY);
            txtDefineName.setBackground(Color.WHITE);
        } else {
            lblDefineNameStatus.setText("OK");
            lblDefineNameStatus.setForeground(new Color(40, 167, 69));
            txtDefineName.setBackground(new Color(230, 255, 230));
        }
    }

    private void resetForm() {
        // Stop animation and reset frame
        stopAnimation();
        currentFrame = 0;

        txtBossName.setText("New Boss");
        // Reload ID from File
        analyzeBossIdFile();

        if (cboGender != null && cboGender.getItemCount() > 0)
            cboGender.setSelectedIndex(0);

        txtDame.setText("10000");
        txtHp.setText("1000000");
        txtMapJoin.setText("5, 10, 15");
        txtSecondsRest.setText("60");
        txtTextS.setText("Ta đã đến");
        txtTextM.setText("Haha!");
        txtTextE.setText("Ta sẽ quay lại");

        headImage = bodyImage = legImage = null;
        headIconId = bodyIconId = legIconId = 0;
        selectedHeadPathId = selectedBodyPathId = selectedLegPathId = 0;

        // Reset Animation Data
        headFrames.clear();
        bodyFrames.clear();
        legFrames.clear();
        headPos = null;
        bodyPos = null;
        legPos = null;

        cboHeadPath.removeAllItems();
        cboBodyPath.removeAllItems();
        cboLegPath.removeAllItems();

        lblHeadInfo.setText("ID: --");
        lblBodyInfo.setText("ID: --");
        lblLegInfo.setText("ID: --");

        // Update label
        updateFrameLabel();

        skillTableModel.setRowCount(0);
        addDefaultSkills();

        outputTextArea.setText("");
        canvas.repaint();
    }

    private BufferedImage loadIcon(int iconId) {
        if (iconCache.containsKey(iconId)) {
            return iconCache.get(iconId);
        }
        try {
            File f = new File(ICON_PATH + "/" + iconId + ".png");
            if (f.exists()) {
                BufferedImage img = ImageIO.read(f);
                iconCache.put(iconId, img);
                return img;
            }
        } catch (Exception e) {
            System.out.println("Cannot load image " + iconId + ": " + e.getMessage());
        }
        return null;
    }

    // ==================== INNER CLASS: BossCanvas ====================
    private class BossCanvas extends JPanel {

        private static final int ZOOM = 4;

        // SKEL constants removed, using CHAR_INFO instead

        public BossCanvas() {
            setBackground(new Color(40, 40, 50));
            setPreferredSize(new Dimension(400, 400));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2 + 50;

            // Draw ground line
            g2d.setColor(new Color(80, 80, 80));
            g2d.drawLine(0, centerY + 20, getWidth(), centerY + 20);

            // Draw coordinate guides
            g2d.setColor(new Color(60, 60, 60));
            g2d.drawLine(centerX, 0, centerX, getHeight());
            g2d.drawLine(0, centerY, getWidth(), centerY);

            // Draw parts
            drawPart(g2d, "leg", centerX, centerY);
            drawPart(g2d, "body", centerX, centerY);
            drawPart(g2d, "head", centerX, centerY);

            // Draw info
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2d.drawString("Head Path: " + selectedHeadPathId, 10, 20);
            g2d.drawString("Body Path: " + selectedBodyPathId, 10, 35);
            g2d.drawString("Leg Path: " + selectedLegPathId, 10, 50);

            if (isAnimating) {
                g2d.setColor(Color.GREEN);
                g2d.drawString("Playing Frame: " + currentFrame, 10, 70);
            }

            g2d.dispose();
        }

        private void drawPart(Graphics2D g2d, String partType, int centerX, int centerY) {
            List<PartFrame> frames;
            BufferedImage staticImg;
            Point staticPos;

            // Map partName to CHAR_INFO index
            // Info Order: 0:Head, 1:Leg, 2:Body
            int infoTypeIdx;

            switch (partType) {
                case "head":
                    frames = headFrames;
                    staticImg = headImage;
                    staticPos = headPos;
                    infoTypeIdx = 0;
                    break;
                case "body":
                    frames = bodyFrames;
                    staticImg = bodyImage;
                    staticPos = bodyPos;
                    infoTypeIdx = 2;
                    break;
                case "leg":
                    frames = legFrames;
                    staticImg = legImage;
                    staticPos = legPos;
                    infoTypeIdx = 1;
                    break;
                default:
                    return;
            }

            if (!frames.isEmpty()) {
                // Determine Frame Info to use from CHAR_INFO
                int animFrame = currentFrame % CHAR_INFO.length; // Loop through global anim frames
                if (animFrame < 0)
                    animFrame = 0;

                int[] info = CHAR_INFO[animFrame][infoTypeIdx]; // [Index, DX, DY]
                int requiredPartIdx = info[0];
                int frameDx = info[1];
                int frameDy = info[2];

                // Get Part Frame
                // If required index is out of bounds (e.g. static head), use 0
                int actualPartIdx = requiredPartIdx;
                if (actualPartIdx >= frames.size())
                    actualPartIdx = 0;

                PartFrame pf = frames.get(actualPartIdx);
                BufferedImage img = loadIcon(pf.iconId);

                if (img != null) {
                    // Formula:
                    // Final X = (FrameDX + PartDX) * Scale
                    // Final Y = (-FrameDY + PartDY) * Scale (Note: Y Axis inverted logic)
                    // PartDY usually adds to Y (moves down), FrameDY reduces Y (moves up/height)

                    int x = centerX + (frameDx + pf.dx) * ZOOM;
                    int y = centerY + (-frameDy + pf.dy) * ZOOM;

                    g2d.drawImage(img, x, y, null);
                }
            } else {
                // Fallback
                if (staticImg != null) {
                    // For static, we don't have frame offsets, so just use staticPos (which is
                    // frame 0 dx/dy * 4)
                    // But we should also apply Frame 0 skeletal offset if possible?
                    // Let's stick to simple logic for static
                    // Old logic: x = centerX + staticPos.x + skelX*ZOOM.
                    // But we don't use Skel constants anymore.
                    // Use CHAR_INFO[0] as default skeletal for static

                    int[] info = CHAR_INFO[0][infoTypeIdx];
                    int frameDx = info[1];
                    int frameDy = info[2];

                    // staticPos.x is already (dx * 4). We need dx. so / 4.
                    // Fix NPE: staticPos might be null if data not loaded yet
                    int pfDx = 0;
                    int pfDy = 0;
                    if (staticPos != null) {
                        pfDx = staticPos.x / 4;
                        pfDy = staticPos.y / 4;
                    }

                    int x = centerX + (frameDx + pfDx) * ZOOM;
                    int y = centerY + (-frameDy + pfDy) * ZOOM;

                    g2d.drawImage(staticImg, x, y, null);
                }
            }
        }
    }
}
