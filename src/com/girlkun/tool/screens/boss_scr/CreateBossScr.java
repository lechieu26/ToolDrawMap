package com.girlkun.tool.screens.boss_scr;

import com.girlkun.tool.shopmanager.models.*;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.List;

public class CreateBossScr extends JInternalFrame {

    private static final String ICON_PATH = "data/data/icon/x4";
    private static final int ZOOM = 4;

    private static class PartFrame {
        int iconId;
        int dx, dy;

        public PartFrame(int iconId, int dx, int dy) {
            this.iconId = iconId;
            this.dx = dx;
            this.dy = dy;
        }
    }

    private final List<PartFrame> headFrames = new ArrayList<>();
    private final List<PartFrame> bodyFrames = new ArrayList<>();
    private final List<PartFrame> legFrames = new ArrayList<>();
    private final Map<Integer, BufferedImage> iconCache = new HashMap<>();
    private final Map<Integer, ImageIcon> smallIconCache = new HashMap<>();

    private ImageIcon getSmallIcon(int id) {
        if (smallIconCache.containsKey(id)) {
            return smallIconCache.get(id);
        }
        BufferedImage img = loadIcon(id);
        if (img != null) {
            int maxDim = 70;
            int w = img.getWidth();
            int h = img.getHeight();
            if (w > h) {
                h = (h * maxDim) / Math.max(w, 1);
                w = maxDim;
            } else {
                w = (w * maxDim) / Math.max(h, 1);
                h = maxDim;
            }
            if (w <= 0) w = 1;
            if (h <= 0) h = 1;
            ImageIcon ic = new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
            smallIconCache.put(id, ic);
            return ic;
        }
        smallIconCache.put(id, null);
        return null;
    }

    private int selectedHeadPartId = -1, selectedBodyPartId = -1, selectedLegPartId = -1;
    private JLabel lblCurrentOutfit;

    // Boss Data state
    private BossConfig currentBoss = null;
    private int currentFormIdx = 0;
    private boolean isUpdatingUi = false;
    private List<BossConfig> allBossCache = new ArrayList<>();
    private List<ShopManagerDAO.MapTemplate> allMapCache = new ArrayList<>();

    // UI Components - Header / Template
    private JTextField txtBossName, txtBossId, txtSubType, txtSpawnCount, txtRespawnDelay, txtDespawnTimeout;
    private JTextField txtMapJoin, txtBossesTogether, txtRequireTaskId, txtExtraConfig;
    private JLabel lblTogetherPreview;
    private JComboBox<String> cboBossType, cboGender;
    private JCheckBox chkEnabled, chkNotify, chkZone01Disabled;

    // UI Components - Forms
    private JComboBox<String> cboForms;
    private JTextField txtFormName, txtDame, txtHp;
    private JTextField txtOutfitHead, txtOutfitBody, txtOutfitLeg;
    private JTextField txtTextS, txtTextM, txtTextE;

    // UI Components - Left List
    private JList<BossConfig> listBoss;
    private DefaultListModel<BossConfig> listBossModel;
    private JTextField txtSearchBoss;

    // UI Components - Skills
    private JTable skillTable;
    private DefaultTableModel skillTableModel;
    private JComboBox<ShopManagerDAO.SkillTemplate> cboSkillEditor;
    private final Map<Integer, ShopManagerDAO.SkillTemplate> skillCache = new HashMap<>();
    private JButton btnReward;

    private List<ItemOptionTemplate> optionTemplates = new ArrayList<>();

    // Canvas animation
    private BossCanvas canvas;
    private javax.swing.Timer animationTimer;
    private int currentFrame = 0;
    private int animMode = 0; // 0: Stand, 1: Play All, 2: Pause

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
            { { 1, -8, 32 }, { 9, -12, 9 }, { 2, -6, 15 } }, // 15: Dam 3
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

    public CreateBossScr() {
        super("Quản Lý & Tạo Boss (Server NRO ARN Database)", true, true, true, true);
        initComponents();
        setSize(1600, 940);
        loadBossConfigs();
        loadMapTemplates();
        startAnimation();

        new SwingWorker<List<ItemOptionTemplate>, Void>() {
            @Override
            protected List<ItemOptionTemplate> doInBackground() {
                return ShopManagerDAO.gI().getItemOptionTemplates();
            }

            @Override
            protected void done() {
                try {
                    optionTemplates = get();
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void startAnimation() {
        if (animationTimer != null)
            animationTimer.stop();
        animationTimer = new javax.swing.Timer(200, e -> {
            if (animMode == 0) {
                currentFrame = (currentFrame + 1) % 2;
                if (canvas != null) canvas.repaint();
            } else if (animMode == 1) {
                currentFrame = (currentFrame + 1) % CHAR_INFO.length;
                if (canvas != null) canvas.repaint();
            }
        });
        animationTimer.start();
    }

    private void loadMapTemplates() {
        new SwingWorker<List<ShopManagerDAO.MapTemplate>, Void>() {
            @Override
            protected List<ShopManagerDAO.MapTemplate> doInBackground() {
                return ShopManagerDAO.gI().getMapTemplates();
            }

            @Override
            protected void done() {
                try {
                    allMapCache = get();
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void loadBossConfigs() {
        new SwingWorker<List<BossConfig>, Void>() {
            @Override
            protected List<BossConfig> doInBackground() {
                List<BossConfig> list = ShopManagerDAO.gI().getAllBossConfigs();
                list.sort(Comparator.comparingInt(b -> b.bossId));
                return list;
            }

            @Override
            protected void done() {
                try {
                    allBossCache = get();
                    listBossModel.clear();
                    String sc = txtSearchBoss.getText().toLowerCase().trim();
                    for (BossConfig boss : allBossCache) {
                        if (sc.isEmpty() || boss.bossName.toLowerCase().contains(sc)
                                || String.valueOf(boss.bossId).contains(sc)
                                || (boss.subType != null && boss.subType.toLowerCase().contains(sc))) {
                            listBossModel.addElement(boss);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        // LEFT COLUMN: List Boss
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(310, 0));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Danh sách Boss trong Database"));

        txtSearchBoss = new JTextField();
        txtSearchBoss.setBorder(BorderFactory.createTitledBorder("Tìm Boss (Tên / ID / SubType)"));
        txtSearchBoss.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { loadBossConfigs(); }
            public void removeUpdate(DocumentEvent e) { loadBossConfigs(); }
            public void changedUpdate(DocumentEvent e) { loadBossConfigs(); }
        });
        leftPanel.add(txtSearchBoss, BorderLayout.NORTH);

        listBossModel = new DefaultListModel<>();
        listBoss = new JList<>(listBossModel);
        listBoss.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BossConfig) {
                    BossConfig b = (BossConfig) value;
                    String tag = b.bossType != null ? b.bossType : "NORMAL";
                    if (b.bossesAppearTogether != null && !b.bossesAppearTogether.trim().isEmpty()) {
                        tag += " | Cha";
                    } else if (b.parentBossId != null) {
                        tag += " | Con";
                    }
                    l.setText(String.format("[%d] %s  (%s)", b.bossId, b.bossName, tag));
                    if (!b.enabled) {
                        l.setForeground(Color.GRAY);
                    }
                }
                return l;
            }
        });
        listBoss.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                BossConfig sel = listBoss.getSelectedValue();
                if (sel != null) {
                    fillBossInfo(sel);
                }
            }
        });
        leftPanel.add(new JScrollPane(listBoss), BorderLayout.CENTER);

        JPanel leftActions = new JPanel(new GridLayout(1, 3, 3, 0));
        JButton btnNewBoss = new JButton("+ Tạo Mới");
        btnNewBoss.setBackground(new Color(40, 167, 69));
        btnNewBoss.setForeground(Color.WHITE);
        btnNewBoss.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnNewBoss.addActionListener(e -> createNewBoss());

        JButton btnDeleteBoss = new JButton("Xóa");
        btnDeleteBoss.setBackground(new Color(220, 53, 69));
        btnDeleteBoss.setForeground(Color.WHITE);
        btnDeleteBoss.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDeleteBoss.addActionListener(e -> deleteSelectedBoss());

        JButton btnRefresh = new JButton("Tải Lại");
        btnRefresh.addActionListener(e -> loadBossConfigs());

        leftActions.add(btnNewBoss);
        leftActions.add(btnDeleteBoss);
        leftActions.add(btnRefresh);
        leftPanel.add(leftActions, BorderLayout.SOUTH);

        // MIDDLE COLUMN: Header Boss + Canvas Preview + Form management
        JPanel middlePanel = new JPanel(new BorderLayout(5, 5));
        middlePanel.setPreferredSize(new Dimension(880, 0));

        // Top: Template Header
        JPanel pnlHeader = createHeaderPanel();
        middlePanel.add(pnlHeader, BorderLayout.NORTH);

        // Center: Canvas Preview with controls
        canvas = new BossCanvas();
        JPanel pnlCanvasWrapper = new JPanel(new BorderLayout());
        pnlCanvasWrapper.setBorder(BorderFactory.createTitledBorder("Xem trước & Diễn hoạt nhân vật"));
        pnlCanvasWrapper.add(new JScrollPane(canvas), BorderLayout.CENTER);

        JPanel pnlControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnPrev = new JButton("<");
        JButton btnPlayPause = new JButton("Run All");
        JButton btnStand = new JButton("Stand");
        JButton btnNext = new JButton(">");
        JSlider sliderSpeed = new JSlider(1, 60, 5);

        btnPrev.addActionListener(e -> {
            animMode = 2; // Pause
            currentFrame--;
            if (currentFrame < 0) currentFrame = CHAR_INFO.length - 1;
            btnPlayPause.setText("Run All");
            canvas.repaint();
        });
        btnNext.addActionListener(e -> {
            animMode = 2; // Pause
            currentFrame = (currentFrame + 1) % CHAR_INFO.length;
            btnPlayPause.setText("Run All");
            canvas.repaint();
        });
        btnPlayPause.addActionListener(e -> {
            if (animMode == 1) {
                animMode = 2; // Pause
                btnPlayPause.setText("Run All");
            } else {
                animMode = 1; // Play all
                btnPlayPause.setText("|| Pause");
            }
        });
        btnStand.addActionListener(e -> {
            animMode = 0; // Stand loop
            currentFrame = 0;
            btnPlayPause.setText("Run All");
            canvas.repaint();
        });
        sliderSpeed.addChangeListener(e -> {
            if (animationTimer != null) {
                int delay = 1000 / sliderSpeed.getValue();
                animationTimer.setDelay(delay);
            }
        });

        pnlControls.add(btnPrev);
        pnlControls.add(btnPlayPause);
        pnlControls.add(btnStand);
        pnlControls.add(btnNext);
        pnlControls.add(new JLabel("Tốc độ:"));
        pnlControls.add(sliderSpeed);

                // Outfit buttons & Skin Selector
        JPanel pnlOutfitButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 3));
        JButton btnSkin = new JButton("Chọn Cải Trang (Skin)");
        btnSkin.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSkin.setBackground(new Color(111, 66, 193));
        btnSkin.setForeground(Color.WHITE);
        btnSkin.setToolTipText("Chọn cải trang từ item_template (type = 5)");
        btnSkin.addActionListener(e -> openSkinSelectorDialog());

        txtOutfitHead = new JTextField("0", 4);
        txtOutfitHead.setHorizontalAlignment(JTextField.CENTER);
        txtOutfitHead.setToolTipText("ID Part Head");
        JButton btnHead = new JButton("Head");
        btnHead.addActionListener(e -> selectOutfit("head"));

        txtOutfitBody = new JTextField("0", 4);
        txtOutfitBody.setHorizontalAlignment(JTextField.CENTER);
        txtOutfitBody.setToolTipText("ID Part Body");
        JButton btnBody = new JButton("Body");
        btnBody.addActionListener(e -> selectOutfit("body"));

        txtOutfitLeg = new JTextField("0", 4);
        txtOutfitLeg.setHorizontalAlignment(JTextField.CENTER);
        txtOutfitLeg.setToolTipText("ID Part Leg");
        JButton btnLeg = new JButton("Leg");
        btnLeg.addActionListener(e -> selectOutfit("leg"));

        DocumentListener partDocListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onPartTextUpdated(); }
            public void removeUpdate(DocumentEvent e) { onPartTextUpdated(); }
            public void changedUpdate(DocumentEvent e) { onPartTextUpdated(); }
        };
        txtOutfitHead.getDocument().addDocumentListener(partDocListener);
        txtOutfitBody.getDocument().addDocumentListener(partDocListener);
        txtOutfitLeg.getDocument().addDocumentListener(partDocListener);

        lblCurrentOutfit = new JLabel("");
        lblCurrentOutfit.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCurrentOutfit.setForeground(new Color(30, 144, 255));

        pnlOutfitButtons.add(btnSkin);
        pnlOutfitButtons.add(new JLabel("Head:"));
        pnlOutfitButtons.add(txtOutfitHead);
        pnlOutfitButtons.add(btnHead);
        pnlOutfitButtons.add(new JLabel("Body:"));
        pnlOutfitButtons.add(txtOutfitBody);
        pnlOutfitButtons.add(btnBody);
        pnlOutfitButtons.add(new JLabel("Leg:"));
        pnlOutfitButtons.add(txtOutfitLeg);
        pnlOutfitButtons.add(btnLeg);

        JPanel pnlCanvasSouth = new JPanel(new GridLayout(2, 1));
        pnlCanvasSouth.add(pnlOutfitButtons);
        pnlCanvasSouth.add(pnlControls);
        pnlCanvasWrapper.add(pnlCanvasSouth, BorderLayout.SOUTH);

        middlePanel.add(pnlCanvasWrapper, BorderLayout.CENTER);

        // South: Form management & Text chats
        JPanel pnlFormAndChat = createFormAndChatPanel();
        middlePanel.add(pnlFormAndChat, BorderLayout.SOUTH);

        // RIGHT COLUMN: Skills & Reward
        JPanel rightPanel = createSkillAndRewardPanel();

        // SPLITS
        JSplitPane splitMidRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, middlePanel, rightPanel);
        splitMidRight.setDividerLocation(880);

        JSplitPane splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, splitMidRight);
        splitMain.setDividerLocation(310);

        mainPanel.add(splitMain, BorderLayout.CENTER);
        mainPanel.add(createActionPanel(), BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Cấu hình Chung của Boss (boss_template / boss_map / boss_appear_together)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0
        gbc.gridy = 0;
        gbc.gridx = 0; gbc.weightx = 0.1;
        p.add(new JLabel("ID Boss:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.4;
        JPanel idPnl = new JPanel(new BorderLayout(3, 0));
        txtBossId = new JTextField("1");
        JButton btnNextId = new JButton("ID Tiếp Theo");
        btnNextId.addActionListener(e -> txtBossId.setText(String.valueOf(ShopManagerDAO.gI().getNextBossId())));
        idPnl.add(txtBossId, BorderLayout.CENTER);
        idPnl.add(btnNextId, BorderLayout.EAST);
        p.add(idPnl, gbc);

        gbc.gridx = 2; gbc.weightx = 0.1;
        p.add(new JLabel("Tên Boss:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.4;
        p.add(txtBossName = new JTextField("New Boss"), gbc);

        // Row 1
        gbc.gridy = 1;
        gbc.gridx = 0;
        p.add(new JLabel("Loại Boss:"), gbc);
        gbc.gridx = 1;
        cboBossType = new JComboBox<>(new String[] { "NORMAL", "TASK", "EVENT", "DUNGEON", "PHOBAN", "MINI", "FINAL" });
        p.add(cboBossType, gbc);

        gbc.gridx = 2;
        p.add(new JLabel("Nhóm (sub_type):"), gbc);
        gbc.gridx = 3;
        txtSubType = new JTextField("DEFAULT");
        p.add(txtSubType, gbc);

        // Row 2
        gbc.gridy = 2;
        gbc.gridx = 0;
        p.add(new JLabel("Hành tinh:"), gbc);
        gbc.gridx = 1;
        cboGender = new JComboBox<>(new String[] { "0: Trái Đất", "1: Namếc", "2: Xayda" });
        p.add(cboGender, gbc);

        gbc.gridx = 2;
        p.add(new JLabel("Số lượng spawn:"), gbc);
        gbc.gridx = 3;
        p.add(txtSpawnCount = new JTextField("1"), gbc);

        // Row 3
        gbc.gridy = 3;
        gbc.gridx = 0;
        p.add(new JLabel("Hồi sinh (giây):"), gbc);
        gbc.gridx = 1;
        p.add(txtRespawnDelay = new JTextField("300"), gbc);

        gbc.gridx = 2;
        p.add(new JLabel("Tự rời map (giây):"), gbc);
        gbc.gridx = 3;
        p.add(txtDespawnTimeout = new JTextField("900"), gbc);

        // Row 4: Map Join & Boss Đi Cùng
        gbc.gridy = 4;
        gbc.gridx = 0;
        p.add(new JLabel("Map Join:"), gbc);
        gbc.gridx = 1;
        JPanel mapPnl = new JPanel(new BorderLayout(3, 0));
        txtMapJoin = new JTextField("5");
        JButton btnSelectMap = new JButton("Chọn...");
        btnSelectMap.addActionListener(e -> openMapSelectorDialog());
        mapPnl.add(txtMapJoin, BorderLayout.CENTER);
        mapPnl.add(btnSelectMap, BorderLayout.EAST);
        p.add(mapPnl, gbc);

        gbc.gridx = 2;
        p.add(new JLabel("Boss đi cùng:"), gbc);
        gbc.gridx = 3;
        JPanel togetherPnl = new JPanel(new BorderLayout(3, 0));
        txtBossesTogether = new JTextField("");
        txtBossesTogether.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateTogetherPreview(); }
            public void removeUpdate(DocumentEvent e) { updateTogetherPreview(); }
            public void changedUpdate(DocumentEvent e) { updateTogetherPreview(); }
        });
        JButton btnSelectTogether = new JButton("Chọn...");
        btnSelectTogether.addActionListener(e -> openBossTogetherSelectorDialog());
        togetherPnl.add(txtBossesTogether, BorderLayout.CENTER);
        togetherPnl.add(btnSelectTogether, BorderLayout.EAST);
        p.add(togetherPnl, gbc);

        // Row 5: Preview status của Boss đi cùng
        gbc.gridy = 5;
        gbc.gridx = 0;
        p.add(new JLabel("Chi tiết đi cùng:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        lblTogetherPreview = new JLabel("(Không có boss đi cùng)");
        lblTogetherPreview.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTogetherPreview.setForeground(Color.GRAY);
        p.add(lblTogetherPreview, gbc);
        gbc.gridwidth = 1; // reset

        // Row 6: Require Task ID & Extra Config
        gbc.gridy = 6;
        gbc.gridx = 0;
        p.add(new JLabel("ID Nhiệm vụ:"), gbc);
        gbc.gridx = 1;
        p.add(txtRequireTaskId = new JTextField(""), gbc);

        gbc.gridx = 2;
        p.add(new JLabel("Cấu hình Extra JSON:"), gbc);
        gbc.gridx = 3;
        p.add(txtExtraConfig = new JTextField(""), gbc);

        // Row 7: Checkboxes
        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        JPanel chkPnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        chkPnl.add(chkEnabled = new JCheckBox("Bật Spawn (enabled)", true));
        chkPnl.add(chkNotify = new JCheckBox("Thông báo toàn Server khi xuất hiện/chết (is_notify)", true));
        chkZone01Disabled = new JCheckBox("Không spawn ở khu 0 và 1 (is_zone_0_1_disabled)", true);
        chkPnl.add(chkZone01Disabled);
        p.add(chkPnl, gbc);

        return p;
    }

    private JPanel createFormAndChatPanel() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createTitledBorder("Quản lý Dạng biến hình (boss_form) & Câu thoại"));

        // Forms selector bar
        JPanel formBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        formBar.add(new JLabel("Chọn Dạng (Form):"));
        cboForms = new JComboBox<>();
        cboForms.setPreferredSize(new Dimension(220, 26));
        cboForms.addActionListener(e -> {
            if (!isUpdatingUi && currentBoss != null) {
                int sel = cboForms.getSelectedIndex();
                if (sel >= 0 && sel != currentFormIdx) {
                    saveCurrentFormValues();
                    currentFormIdx = sel;
                    loadFormValues(currentFormIdx);
                }
            }
        });
        formBar.add(cboForms);

        JButton btnAddForm = new JButton("+ Thêm Dạng Mới");
        btnAddForm.setBackground(new Color(40, 167, 69));
        btnAddForm.setForeground(Color.WHITE);
        btnAddForm.addActionListener(e -> addNewForm());

        JButton btnRemoveForm = new JButton("- Xóa Dạng Này");
        btnRemoveForm.setBackground(new Color(220, 53, 69));
        btnRemoveForm.setForeground(Color.WHITE);
        btnRemoveForm.addActionListener(e -> removeCurrentForm());

        formBar.add(btnAddForm);
        formBar.add(btnRemoveForm);
        p.add(formBar, BorderLayout.NORTH);

        // Form fields + Chats
        JPanel content = new JPanel(new GridLayout(2, 1, 4, 4));

        JPanel formStats = new JPanel(new GridLayout(1, 3, 5, 0));
        formStats.add(createFormRow("Tên Dạng:", txtFormName = new JTextField("Gốc")));
        formStats.add(createFormRow("Sát thương:", txtDame = new JTextField("10000")));
        formStats.add(createFormRow("HP (min, max):", txtHp = new JTextField("1000000")));
        content.add(formStats);

        JPanel chats = new JPanel(new GridLayout(1, 3, 5, 0));
        chats.add(createFormRow("Thoại Xuất Hiện:", txtTextS = new JTextField("[\"Ta đã đến\"]")));
        chats.add(createFormRow("Thoại Đánh:", txtTextM = new JTextField("[\"Haha!\"]")));
        chats.add(createFormRow("Thoại Khi Chết:", txtTextE = new JTextField("[\"Ta sẽ quay lại\"]")));
        content.add(chats);

        p.add(content, BorderLayout.CENTER);
        return p;
    }

    private JPanel createSkillAndRewardPanel() {
        JPanel sp = new JPanel(new BorderLayout(5, 5));
        sp.setPreferredSize(new Dimension(380, 0));
        sp.setBorder(BorderFactory.createTitledBorder("Kỹ năng của Dạng này & Phần thưởng"));

        skillTableModel = new DefaultTableModel(new Object[] { "Kỹ Năng", "Cấp (1-7)", "Hồi chiêu (ms)" }, 0);
        skillTable = new JTable(skillTableModel);
        cboSkillEditor = new JComboBox<>();
        new SwingWorker<List<ShopManagerDAO.SkillTemplate>, Void>() {
            @Override
            protected List<ShopManagerDAO.SkillTemplate> doInBackground() {
                return ShopManagerDAO.gI().getAllSkills();
            }

            @Override
            protected void done() {
                try {
                    for (ShopManagerDAO.SkillTemplate s : get()) {
                        cboSkillEditor.addItem(s);
                        skillCache.put(s.id, s);
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
        skillTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(cboSkillEditor));

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        JButton addS = new JButton("+ Thêm Skill");
        addS.addActionListener(e -> skillTableModel.addRow(new Object[] { "0", "7", "1000" }));
        JButton remS = new JButton("- Xóa Skill");
        remS.addActionListener(e -> {
            int r = skillTable.getSelectedRow();
            if (r >= 0) {
                skillTableModel.removeRow(r);
            }
        });
        bp.add(addS);
        bp.add(remS);

        sp.add(new JScrollPane(skillTable), BorderLayout.CENTER);

        JPanel southP = new JPanel(new BorderLayout(3, 3));
        southP.add(bp, BorderLayout.NORTH);

        btnReward = new JButton("Cấu hình Phần thưởng (boss_reward)");
        btnReward.setBackground(new Color(138, 43, 226));
        btnReward.setForeground(Color.WHITE);
        btnReward.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnReward.setPreferredSize(new Dimension(0, 40));
        btnReward.addActionListener(e -> openRewardDialog());
        southP.add(btnReward, BorderLayout.SOUTH);

        sp.add(southP, BorderLayout.SOUTH);
        return sp;
    }

    private JPanel createActionPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 8));
        JButton btnRes = new JButton("Reset Trắng Form");
        btnRes.setPreferredSize(new Dimension(180, 42));
        btnRes.addActionListener(e -> createNewBoss());

        JButton btnSave = new JButton("LƯU BOSS VÀO CƠ SỞ DỮ LIỆU");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
        btnSave.setPreferredSize(new Dimension(350, 42));
        btnSave.addActionListener(e -> saveBoss());

        p.add(btnRes);
        p.add(btnSave);
        return p;
    }

    private void fillBossInfo(BossConfig boss) {
        if (boss == null) return;
        this.currentBoss = boss;
        isUpdatingUi = true;

        txtBossName.setText(boss.bossName != null ? boss.bossName : "");
        txtBossId.setText(String.valueOf(boss.bossId));
        cboBossType.setSelectedItem(boss.bossType != null ? boss.bossType : "NORMAL");
        txtSubType.setText(boss.subType != null ? boss.subType : "DEFAULT");
        cboGender.setSelectedIndex(boss.gender >= 0 && boss.gender <= 2 ? boss.gender : 0);
        txtSpawnCount.setText(String.valueOf(boss.spawnCount > 0 ? boss.spawnCount : 1));
        txtRespawnDelay.setText(String.valueOf(boss.respawnDelay > 0 ? boss.respawnDelay : boss.secondsRest));
        txtDespawnTimeout.setText(String.valueOf(boss.despawnTimeout > 0 ? boss.despawnTimeout : 900));
        txtMapJoin.setText(boss.mapJoin != null ? boss.mapJoin : "");
        txtBossesTogether.setText(boss.bossesAppearTogether != null ? boss.bossesAppearTogether : "");
        txtRequireTaskId.setText(boss.requireTaskId != null ? String.valueOf(boss.requireTaskId) : "");
        txtExtraConfig.setText(boss.extraConfig != null ? boss.extraConfig : "");

        chkEnabled.setSelected(boss.enabled);
        chkNotify.setSelected(boss.isNotify);
        chkZone01Disabled.setSelected(boss.isZone01SpawnDisabled);

        // Ensure at least 1 form exists
        if (boss.forms == null || boss.forms.isEmpty()) {
            boss.forms = new ArrayList<>();
            BossFormConfig f0 = new BossFormConfig(0, boss.bossName);
            f0.dame = (int) boss.dame;
            boss.forms.add(f0);
        }

        refreshFormsCombo();
        currentFormIdx = 0;
        cboForms.setSelectedIndex(0);
        loadFormValues(0);

        updateRewardButtonText();
        updateTogetherPreview();
        isUpdatingUi = false;
    }

    private void updateTogetherPreview() {
        if (lblTogetherPreview == null) return;
        String rawTogether = txtBossesTogether.getText().trim();
        if (!rawTogether.isEmpty()) {
            String[] parts = rawTogether.split("[,;\\s]+");
            List<String> resolved = new ArrayList<>();
            for (String p : parts) {
                String s = p.trim();
                if (!s.isEmpty()) {
                    try {
                        int sid = Integer.parseInt(s);
                        String sname = getBossNameById(sid);
                        resolved.add("[" + sid + "] " + sname);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (!resolved.isEmpty()) {
                lblTogetherPreview.setText("Gọi theo các Boss con: " + String.join(", ", resolved));
                lblTogetherPreview.setForeground(new Color(40, 167, 69));
                return;
            }
        }

        if (currentBoss != null && currentBoss.parentBossId != null) {
            lblTogetherPreview.setText(String.format("Lưu ý: Boss này là Boss con đi kèm theo Boss: [%d] %s",
                    currentBoss.parentBossId, currentBoss.parentBossName != null ? currentBoss.parentBossName : ""));
            lblTogetherPreview.setForeground(new Color(255, 140, 0));
            return;
        }

        lblTogetherPreview.setText("(Boss độc lập, không có boss đi cùng)");
        lblTogetherPreview.setForeground(Color.GRAY);
    }

    private String getBossNameById(int id) {
        for (BossConfig b : allBossCache) {
            if (b.bossId == id) {
                return b.bossName;
            }
        }
        return "Boss " + id;
    }

    private void refreshFormsCombo() {
        cboForms.removeAllItems();
        if (currentBoss != null && currentBoss.forms != null) {
            for (int i = 0; i < currentBoss.forms.size(); i++) {
                BossFormConfig f = currentBoss.forms.get(i);
                cboForms.addItem(String.format("Dạng %d: %s", i, f.name != null ? f.name : ""));
            }
        }
    }

    private void loadFormValues(int formIdx) {
        if (currentBoss == null || currentBoss.forms == null || formIdx < 0 || formIdx >= currentBoss.forms.size()) {
            return;
        }
        BossFormConfig form = currentBoss.forms.get(formIdx);
        txtFormName.setText(form.name != null ? form.name : "");
        txtDame.setText(String.valueOf(form.dame));
        if (form.hpMin == form.hpMax) {
            txtHp.setText(String.valueOf(form.hpMin));
        } else {
            txtHp.setText(form.hpMin + ", " + form.hpMax);
        }

        txtTextS.setText(form.textStart != null ? form.textStart : "[]");
        txtTextM.setText(form.textMid != null ? form.textMid : "[]");
        txtTextE.setText(form.textEnd != null ? form.textEnd : "[]");

        selectedHeadPartId = form.outfitHead;
        selectedBodyPartId = form.outfitBody;
        selectedLegPartId = form.outfitLeg;
        if (txtOutfitHead != null) txtOutfitHead.setText(String.valueOf(selectedHeadPartId));
        if (txtOutfitBody != null) txtOutfitBody.setText(String.valueOf(selectedBodyPartId));
        if (txtOutfitLeg != null) txtOutfitLeg.setText(String.valueOf(selectedLegPartId));
        updateOutfitLabel();
        loadPartPositions();

        // Load skills
        skillTableModel.setRowCount(0);
        if (form.skills != null) {
            for (BossSkillConfig sk : form.skills) {
                ShopManagerDAO.SkillTemplate st = skillCache.get(sk.skillId);
                skillTableModel.addRow(new Object[] {
                        st != null ? st : String.valueOf(sk.skillId),
                        String.valueOf(sk.skillLevel),
                        String.valueOf(sk.cooldown)
                });
            }
        }
    }

    private void saveCurrentFormValues() {
        if (currentBoss == null || currentBoss.forms == null || currentFormIdx < 0 || currentFormIdx >= currentBoss.forms.size()) {
            return;
        }
        BossFormConfig form = currentBoss.forms.get(currentFormIdx);
        form.name = txtFormName.getText().trim();
        try {
            form.dame = Integer.parseInt(txtDame.getText().trim());
        } catch (Exception e) {
            form.dame = 10000;
        }

        String hpStr = txtHp.getText().trim();
        if (hpStr.contains(",")) {
            String[] parts = hpStr.split(",");
            try {
                form.hpMin = Long.parseLong(parts[0].trim());
                form.hpMax = Long.parseLong(parts[1].trim());
            } catch (Exception e) {
                form.hpMin = form.hpMax = 1000000;
            }
        } else {
            try {
                form.hpMin = form.hpMax = Long.parseLong(hpStr);
            } catch (Exception e) {
                form.hpMin = form.hpMax = 1000000;
            }
        }

        form.textStart = txtTextS.getText().trim();
        form.textMid = txtTextM.getText().trim();
        form.textEnd = txtTextE.getText().trim();

        try {
            if (txtOutfitHead != null && !txtOutfitHead.getText().trim().isEmpty()) {
                form.outfitHead = (short) Integer.parseInt(txtOutfitHead.getText().trim());
                selectedHeadPartId = form.outfitHead;
            } else {
                form.outfitHead = (short) selectedHeadPartId;
            }
        } catch (Exception e) {
            form.outfitHead = (short) selectedHeadPartId;
        }
        try {
            if (txtOutfitBody != null && !txtOutfitBody.getText().trim().isEmpty()) {
                form.outfitBody = (short) Integer.parseInt(txtOutfitBody.getText().trim());
                selectedBodyPartId = form.outfitBody;
            } else {
                form.outfitBody = (short) selectedBodyPartId;
            }
        } catch (Exception e) {
            form.outfitBody = (short) selectedBodyPartId;
        }
        try {
            if (txtOutfitLeg != null && !txtOutfitLeg.getText().trim().isEmpty()) {
                form.outfitLeg = (short) Integer.parseInt(txtOutfitLeg.getText().trim());
                selectedLegPartId = form.outfitLeg;
            } else {
                form.outfitLeg = (short) selectedLegPartId;
            }
        } catch (Exception e) {
            form.outfitLeg = (short) selectedLegPartId;
        }

        // Skills
        form.skills.clear();
        for (int i = 0; i < skillTableModel.getRowCount(); i++) {
            Object skillObj = skillTableModel.getValueAt(i, 0);
            int skillId = 0;
            if (skillObj instanceof ShopManagerDAO.SkillTemplate) {
                skillId = ((ShopManagerDAO.SkillTemplate) skillObj).id;
            } else {
                try {
                    skillId = Integer.parseInt(skillObj.toString());
                } catch (Exception ignored) {
                }
            }
            int level = 1;
            int cd = 1000;
            try {
                level = Integer.parseInt(skillTableModel.getValueAt(i, 1).toString());
                cd = Integer.parseInt(skillTableModel.getValueAt(i, 2).toString());
            } catch (Exception ignored) {
            }
            form.skills.add(new BossSkillConfig(skillId, level, cd));
        }
    }

    private void addNewForm() {
        if (currentBoss == null) {
            currentBoss = new BossConfig();
        }
        saveCurrentFormValues();
        int newIdx = currentBoss.forms.size();
        BossFormConfig newForm = new BossFormConfig(newIdx, currentBoss.bossName + " Form " + (newIdx + 1));
        if (!currentBoss.forms.isEmpty()) {
            BossFormConfig prev = currentBoss.forms.get(currentBoss.forms.size() - 1);
            newForm.outfitHead = prev.outfitHead;
            newForm.outfitBody = prev.outfitBody;
            newForm.outfitLeg = prev.outfitLeg;
            newForm.dame = (int) (prev.dame * 1.5);
            newForm.hpMin = (long) (prev.hpMin * 1.5);
            newForm.hpMax = (long) (prev.hpMax * 1.5);
        }
        currentBoss.forms.add(newForm);
        isUpdatingUi = true;
        refreshFormsCombo();
        currentFormIdx = newIdx;
        cboForms.setSelectedIndex(newIdx);
        loadFormValues(newIdx);
        isUpdatingUi = false;
    }

    private void removeCurrentForm() {
        if (currentBoss == null || currentBoss.forms == null || currentBoss.forms.size() <= 1) {
            JOptionPane.showMessageDialog(this, "Boss phải có ít nhất một dạng (Form 0)!");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa dạng " + currentFormIdx + " này?", "Xác nhận xóa dạng", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            currentBoss.forms.remove(currentFormIdx);
            for (int i = 0; i < currentBoss.forms.size(); i++) {
                currentBoss.forms.get(i).formOrder = i;
            }
            isUpdatingUi = true;
            refreshFormsCombo();
            currentFormIdx = Math.max(0, currentFormIdx - 1);
            cboForms.setSelectedIndex(currentFormIdx);
            loadFormValues(currentFormIdx);
            isUpdatingUi = false;
        }
    }

    private void updateOutfitLabel() {
        if (lblCurrentOutfit != null) {
            lblCurrentOutfit.setText(String.format("Outfit: Head [%d], Body [%d], Leg [%d]", selectedHeadPartId, selectedBodyPartId, selectedLegPartId));
        }
    }

    private void loadPartPositions() {
        headFrames.clear();
        bodyFrames.clear();
        legFrames.clear();
        if (selectedHeadPartId > 0) loadFrames(selectedHeadPartId, "head");
        if (selectedBodyPartId > 0) loadFrames(selectedBodyPartId, "body");
        if (selectedLegPartId > 0) loadFrames(selectedLegPartId, "leg");
        if (canvas != null) canvas.repaint();
    }

    private void loadFrames(int partId, String type) {
        if (partId <= 0) return;
        new SwingWorker<List<PartFrame>, Void>() {
            @Override
            protected List<PartFrame> doInBackground() {
                List<PartFrame> list = new ArrayList<>();
                try {
                    ShopManagerDAO.PartData pd = ShopManagerDAO.gI().getPartData(partId);
                    if (pd != null && pd.data != null) {
                        JSONParser parser = new JSONParser();
                        Object obj = parser.parse(pd.data);
                        if (obj instanceof JSONArray) {
                            JSONArray arr = (JSONArray) obj;
                            for (Object item : arr) {
                                if (item instanceof JSONArray) {
                                    JSONArray frame = (JSONArray) item;
                                    if (frame.size() >= 3) {
                                        list.add(new PartFrame(
                                                ((Long) frame.get(0)).intValue(),
                                                ((Long) frame.get(1)).intValue(),
                                                ((Long) frame.get(2)).intValue()));
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return list;
            }

            @Override
            protected void done() {
                try {
                    List<PartFrame> res = get();
                    if (type.equals("head"))
                        headFrames.addAll(res);
                    else if (type.equals("body"))
                        bodyFrames.addAll(res);
                    else
                        legFrames.addAll(res);
                    if (canvas != null) canvas.repaint();
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private BufferedImage loadIcon(int id) {
        if (iconCache.containsKey(id))
            return iconCache.get(id);
        try {
            File f = new File(ICON_PATH, id + ".png");
            if (f.exists()) {
                BufferedImage img = ImageIO.read(f);
                iconCache.put(id, img);
                return img;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void saveBoss() {
        try {
            if (currentBoss == null) {
                currentBoss = new BossConfig();
            }

            currentBoss.bossId = Integer.parseInt(txtBossId.getText().trim());
            currentBoss.bossName = txtBossName.getText().trim();
            if (currentBoss.bossName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tên Boss!");
                return;
            }

            currentBoss.bossType = cboBossType.getSelectedItem().toString();
            currentBoss.subType = txtSubType.getText().trim();
            currentBoss.gender = (byte) cboGender.getSelectedIndex();
            currentBoss.enabled = chkEnabled.isSelected();
            currentBoss.isNotify = chkNotify.isSelected();
            currentBoss.isZone01SpawnDisabled = chkZone01Disabled.isSelected();

            currentBoss.spawnCount = Integer.parseInt(txtSpawnCount.getText().trim());
            currentBoss.respawnDelay = Integer.parseInt(txtRespawnDelay.getText().trim());
            currentBoss.despawnTimeout = Integer.parseInt(txtDespawnTimeout.getText().trim());
            currentBoss.secondsRest = currentBoss.respawnDelay;

            currentBoss.mapJoin = txtMapJoin.getText().trim();
            currentBoss.bossesAppearTogether = txtBossesTogether.getText().trim();

            String reqTask = txtRequireTaskId.getText().trim();
            currentBoss.requireTaskId = reqTask.isEmpty() ? null : Integer.parseInt(reqTask);

            String extra = txtExtraConfig.getText().trim();
            currentBoss.extraConfig = extra.isEmpty() ? null : extra;

            // Save active form values
            saveCurrentFormValues();

            ShopManagerDAO.gI().saveBossConfig(currentBoss);
            JOptionPane.showMessageDialog(this, "Đã lưu thành công Boss: " + currentBoss.bossName + " (ID: " + currentBoss.bossId + ") vào Database (đã cập nhật boss_appear_together)!");
            loadBossConfigs();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi lưu Boss: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteSelectedBoss() {
        BossConfig sel = listBoss.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Boss muốn xóa trong danh sách!");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format("Bạn có chắc chắn muốn xóa Boss '%s' (ID: %d)?\nThao tác này sẽ xóa tất cả Forms, Skills, Maps và Rewards liên quan.", sel.bossName, sel.bossId),
                "Xác nhận xóa Boss",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            ShopManagerDAO.gI().deleteBoss(sel.bossId);
            JOptionPane.showMessageDialog(this, "Đã xóa Boss thành công!");
            loadBossConfigs();
            createNewBoss();
        }
    }

    private void createNewBoss() {
        int nextId = ShopManagerDAO.gI().getNextBossId();
        BossConfig b = new BossConfig();
        b.bossId = nextId;
        b.bossName = "Boss Mới " + nextId;
        b.bossType = "NORMAL";
        b.subType = "DEFAULT";
        b.gender = 0;
        b.enabled = true;
        b.spawnCount = 1;
        b.respawnDelay = 300;
        b.despawnTimeout = 900;
        b.isNotify = true;
        b.isZone01SpawnDisabled = true;
        b.mapJoin = "5";
        b.bossesAppearTogether = "";

        BossFormConfig f0 = new BossFormConfig(0, b.bossName);
        f0.hpMin = f0.hpMax = 1000000;
        f0.dame = 10000;
        b.forms.add(f0);

        fillBossInfo(b);
    }

    private void openRewardDialog() {
        if (currentBoss == null) {
            currentBoss = new BossConfig();
            currentBoss.bossId = Integer.parseInt(txtBossId.getText().trim());
            currentBoss.bossName = txtBossName.getText().trim();
        }
        Window win = SwingUtilities.getWindowAncestor(this);
        RewardConfigDialog diag = new RewardConfigDialog(win, currentBoss, optionTemplates);
        diag.setVisible(true);
        updateRewardButtonText();
    }

    private void updateRewardButtonText() {
        int count = 0;
        if (currentBoss != null) {
            if (currentBoss.rewards != null && !currentBoss.rewards.isEmpty()) {
                count = currentBoss.rewards.size();
            } else if (currentBoss.rewardConfig != null && !currentBoss.rewardConfig.equals("{}")) {
                try {
                    org.json.simple.JSONObject rj = (org.json.simple.JSONObject) org.json.simple.JSONValue.parse(currentBoss.rewardConfig);
                    if (rj != null && rj.containsKey("items")) {
                        JSONArray ia = (JSONArray) rj.get("items");
                        count = ia.size();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (btnReward != null) {
            btnReward.setText(String.format("Cấu hình Phần thưởng (%d items)", count));
        }
    }

        private void onPartTextUpdated() {
        if (isUpdatingUi) return;
        try {
            String h = txtOutfitHead != null ? txtOutfitHead.getText().trim() : "";
            selectedHeadPartId = h.isEmpty() ? -1 : Integer.parseInt(h);
        } catch (Exception ignored) {
            selectedHeadPartId = -1;
        }
        try {
            String b = txtOutfitBody != null ? txtOutfitBody.getText().trim() : "";
            selectedBodyPartId = b.isEmpty() ? -1 : Integer.parseInt(b);
        } catch (Exception ignored) {
            selectedBodyPartId = -1;
        }
        try {
            String l = txtOutfitLeg != null ? txtOutfitLeg.getText().trim() : "";
            selectedLegPartId = l.isEmpty() ? -1 : Integer.parseInt(l);
        } catch (Exception ignored) {
            selectedLegPartId = -1;
        }
        updateOutfitLabel();
        saveCurrentFormValues();
        loadPartPositions();
    }

    private void selectOutfit(String type) {
        OutfitSelectorDialog dialog = new OutfitSelectorDialog(type);
        dialog.setVisible(true);
        if (dialog.getSelectedId() >= 0) {
            int sel = dialog.getSelectedId();
            isUpdatingUi = true;
            if (type.equals("head")) {
                selectedHeadPartId = sel;
                if (txtOutfitHead != null) txtOutfitHead.setText(String.valueOf(sel));
            } else if (type.equals("body")) {
                selectedBodyPartId = sel;
                if (txtOutfitBody != null) txtOutfitBody.setText(String.valueOf(sel));
            } else {
                selectedLegPartId = sel;
                if (txtOutfitLeg != null) txtOutfitLeg.setText(String.valueOf(sel));
            }
            isUpdatingUi = false;
            updateOutfitLabel();
            saveCurrentFormValues();
            loadPartPositions();
        }
    }

    private void openSkinSelectorDialog() {
        SkinSelectorDialog dialog = new SkinSelectorDialog();
        dialog.setVisible(true);
        ShopManagerDAO.CaiTrangTemplate sel = dialog.getSelectedSkin();
        if (sel != null) {
            isUpdatingUi = true;
            selectedHeadPartId = sel.head;
            selectedBodyPartId = sel.body;
            selectedLegPartId = sel.leg;
            if (txtOutfitHead != null) txtOutfitHead.setText(String.valueOf(sel.head));
            if (txtOutfitBody != null) txtOutfitBody.setText(String.valueOf(sel.body));
            if (txtOutfitLeg != null) txtOutfitLeg.setText(String.valueOf(sel.leg));
            isUpdatingUi = false;
            updateOutfitLabel();
            saveCurrentFormValues();
            loadPartPositions();
        }
    }

    // Dialog chọn Boss đi cùng
    private void openBossTogetherSelectorDialog() {
        int myBossId = 0;
        try {
            myBossId = Integer.parseInt(txtBossId.getText().trim());
        } catch (Exception ignored) {
        }
        final int excludeId = myBossId;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chọn Boss Đi Cùng (Sub-Boss)", true);
        dialog.setSize(550, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(5, 5));

        Set<Integer> selectedIds = new HashSet<>();
        String raw = txtBossesTogether.getText().trim();
        if (!raw.isEmpty()) {
            for (String p : raw.split("[,;\\s]+")) {
                try {
                    selectedIds.add(Integer.parseInt(p.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        DefaultTableModel model = new DefaultTableModel(new Object[] { "Chọn", "ID", "Tên Boss", "Loại" }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : super.getColumnClass(columnIndex);
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        for (BossConfig b : allBossCache) {
            if (b.bossId != excludeId) {
                model.addRow(new Object[] { selectedIds.contains(b.bossId), b.bossId, b.bossName, b.bossType });
            }
        }

        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setMaxWidth(80);

        JTextField txtSearch = new JTextField();
        txtSearch.setBorder(BorderFactory.createTitledBorder("Lọc theo tên / ID Boss:"));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterTable(); }
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            public void changedUpdate(DocumentEvent e) { filterTable(); }
            private void filterTable() {
                String q = txtSearch.getText().trim().toLowerCase();
                model.setRowCount(0);
                for (BossConfig b : allBossCache) {
                    if (b.bossId != excludeId) {
                        if (q.isEmpty() || b.bossName.toLowerCase().contains(q) || String.valueOf(b.bossId).contains(q)) {
                            model.addRow(new Object[] { selectedIds.contains(b.bossId), b.bossId, b.bossName, b.bossType });
                        }
                    }
                }
            }
        });

        table.getModel().addTableModelListener(e -> {
            int row = e.getFirstRow();
            if (row >= 0 && row < table.getRowCount()) {
                Boolean checked = (Boolean) table.getValueAt(row, 0);
                int bid = (Integer) table.getValueAt(row, 1);
                if (Boolean.TRUE.equals(checked)) {
                    selectedIds.add(bid);
                } else {
                    selectedIds.remove(bid);
                }
            }
        });

        JButton btnOk = new JButton("Xác Nhận Chọn");
        btnOk.setBackground(new Color(40, 167, 69));
        btnOk.setForeground(Color.WHITE);
        btnOk.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnOk.setPreferredSize(new Dimension(0, 38));
        btnOk.addActionListener(e -> {
            List<Integer> list = new ArrayList<>(selectedIds);
            Collections.sort(list);
            List<String> sList = new ArrayList<>();
            for (int id : list) sList.add(String.valueOf(id));
            txtBossesTogether.setText(String.join(", ", sList));
            updateTogetherPreview();
            dialog.dispose();
        });

        dialog.add(txtSearch, BorderLayout.NORTH);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.add(btnOk, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // Dialog chọn Map xuất hiện
    private void openMapSelectorDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chọn Map Xuất Hiện (boss_map)", true);
        dialog.setSize(520, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(5, 5));

        Set<Integer> selectedIds = new HashSet<>();
        String raw = txtMapJoin.getText().trim();
        if (!raw.isEmpty()) {
            for (String p : raw.split("[,;\\s]+")) {
                try {
                    selectedIds.add(Integer.parseInt(p.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        DefaultTableModel model = new DefaultTableModel(new Object[] { "Chọn", "ID Map", "Tên Bản Đồ" }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : super.getColumnClass(columnIndex);
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        for (ShopManagerDAO.MapTemplate m : allMapCache) {
            model.addRow(new Object[] { selectedIds.contains(m.id), m.id, m.name });
        }

        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setMaxWidth(90);

        JTextField txtSearch = new JTextField();
        txtSearch.setBorder(BorderFactory.createTitledBorder("Lọc theo tên / ID Map:"));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterTable(); }
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            public void changedUpdate(DocumentEvent e) { filterTable(); }
            private void filterTable() {
                String q = txtSearch.getText().trim().toLowerCase();
                model.setRowCount(0);
                for (ShopManagerDAO.MapTemplate m : allMapCache) {
                    if (q.isEmpty() || m.name.toLowerCase().contains(q) || String.valueOf(m.id).contains(q)) {
                        model.addRow(new Object[] { selectedIds.contains(m.id), m.id, m.name });
                    }
                }
            }
        });

        table.getModel().addTableModelListener(e -> {
            int row = e.getFirstRow();
            if (row >= 0 && row < table.getRowCount()) {
                Boolean checked = (Boolean) table.getValueAt(row, 0);
                int mid = (Integer) table.getValueAt(row, 1);
                if (Boolean.TRUE.equals(checked)) {
                    selectedIds.add(mid);
                } else {
                    selectedIds.remove(mid);
                }
            }
        });

        JButton btnOk = new JButton("Xác Nhận Chọn Map");
        btnOk.setBackground(new Color(40, 167, 69));
        btnOk.setForeground(Color.WHITE);
        btnOk.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnOk.setPreferredSize(new Dimension(0, 38));
        btnOk.addActionListener(e -> {
            List<Integer> list = new ArrayList<>(selectedIds);
            Collections.sort(list);
            List<String> sList = new ArrayList<>();
            for (int id : list) sList.add(String.valueOf(id));
            txtMapJoin.setText(String.join(", ", sList));
            dialog.dispose();
        });

        dialog.add(txtSearch, BorderLayout.NORTH);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.add(btnOk, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private class PartItem {
        int id;
        int iconId;

        public PartItem(int id, int iconId) {
            this.id = id;
            this.iconId = iconId;
        }

        @Override
        public String toString() {
            return String.valueOf(id);
        }
    }

        private class OutfitSelectorDialog extends JDialog {
        private int selectedId = -1;
        private final List<PartItem> allParts = new ArrayList<>();
        private final DefaultListModel<PartItem> model = new DefaultListModel<>();
        private final JTextField txtSearch;
        private final JLabel lblInfo;
        private final JList<PartItem> list;

        public OutfitSelectorDialog(String type) {
            super((Frame) null, "Chọn Part " + type.toUpperCase(), true);
            setSize(540, 620);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout(5, 5));

            // Top Panel: Search & Info
            JPanel topPnl = new JPanel(new BorderLayout(5, 5));
            topPnl.setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));
            txtSearch = new JTextField();
            txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            txtSearch.setBorder(BorderFactory.createTitledBorder("Lọc theo Part ID:"));
            lblInfo = new JLabel("Đang tải toàn bộ part...");
            lblInfo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            topPnl.add(txtSearch, BorderLayout.CENTER);
            topPnl.add(lblInfo, BorderLayout.SOUTH);
            add(topPnl, BorderLayout.NORTH);

            list = new JList<>(model);
            list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
            list.setVisibleRowCount(-1);
            list.setFixedCellWidth(92);
            list.setFixedCellHeight(95);

            list.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setVerticalAlignment(SwingConstants.CENTER);
                    if (value instanceof PartItem) {
                        PartItem part = (PartItem) value;
                        label.setToolTipText("Part ID: " + part.id);
                        if (part.iconId > 0) {
                            label.setIcon(getSmallIcon(part.iconId));
                            label.setText(String.valueOf(part.id));
                            label.setHorizontalTextPosition(SwingConstants.CENTER);
                            label.setVerticalTextPosition(SwingConstants.BOTTOM);
                        } else {
                            label.setIcon(null);
                            label.setText("ID: " + part.id);
                        }
                    }
                    return label;
                }
            });

            new SwingWorker<List<PartItem>, Void>() {
                @Override
                protected List<PartItem> doInBackground() {
                    List<PartItem> res = new ArrayList<>();
                    try {
                        Connection conn = ShopManagerDAO.gI().getConnection();
                        int partType = type.equals("head") ? 0 : (type.equals("body") ? 1 : 2);
                        // Query ALL parts without LIMIT
                        String sql = "SELECT id, DATA FROM part WHERE TYPE = ? ORDER BY id ASC";
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setInt(1, partType);
                            ResultSet rs = stmt.executeQuery();
                            JSONParser parser = new JSONParser();
                            int targetIdx = (type.equals("body") || type.equals("leg")) ? 1 : 0;
                            while (rs.next()) {
                                int pId = rs.getInt("id");
                                String pData = rs.getString("DATA");
                                int iconId = -1;
                                if (pData != null && !pData.isEmpty()) {
                                    try {
                                        Object obj = parser.parse(pData);
                                        if (obj instanceof JSONArray) {
                                            JSONArray arr = (JSONArray) obj;
                                            if (arr.size() > targetIdx) {
                                                Object targetFrameObj = arr.get(targetIdx);
                                                if (targetFrameObj instanceof JSONArray) {
                                                    JSONArray frame = (JSONArray) targetFrameObj;
                                                    if (frame.size() > 0) {
                                                        iconId = ((Long) frame.get(0)).intValue();
                                                    }
                                                }
                                            }
                                            // Fallback to first non-zero frame icon
                                            if (iconId <= 0) {
                                                for (Object fObj : arr) {
                                                    if (fObj instanceof JSONArray) {
                                                        JSONArray frame = (JSONArray) fObj;
                                                        if (frame.size() > 0) {
                                                            int cand = ((Long) frame.get(0)).intValue();
                                                            if (cand > 0) {
                                                                iconId = cand;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                                res.add(new PartItem(pId, iconId));
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error loading parts: " + e.getMessage());
                    }
                    return res;
                }

                @Override
                protected void done() {
                    try {
                        allParts.clear();
                        allParts.addAll(get());
                        lblInfo.setText("Tổng số: " + allParts.size() + " parts (" + type.toUpperCase() + ")");
                        filterParts();
                    } catch (Exception ignored) {
                    }
                }
            }.execute();

            txtSearch.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { filterParts(); }
                public void removeUpdate(DocumentEvent e) { filterParts(); }
                public void changedUpdate(DocumentEvent e) { filterParts(); }
            });

            list.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                    selectedId = list.getSelectedValue().id;
                }
            });

            list.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    if (evt.getClickCount() == 2) {
                        int index = list.locationToIndex(evt.getPoint());
                        if (index >= 0 && list.getCellBounds(index, index).contains(evt.getPoint())) {
                            selectedId = model.getElementAt(index).id;
                            dispose();
                        }
                    }
                }
            });

            JPanel btmPnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
            JButton btnOk = new JButton("Chọn Part Này");
            btnOk.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnOk.setBackground(new Color(40, 167, 69));
            btnOk.setForeground(Color.WHITE);
            btnOk.addActionListener(e -> {
                if (list.getSelectedValue() != null) {
                    selectedId = list.getSelectedValue().id;
                }
                dispose();
            });

            JButton btnCancel = new JButton("Hủy");
            btnCancel.addActionListener(e -> {
                selectedId = -1;
                dispose();
            });

            btmPnl.add(btnOk);
            btmPnl.add(btnCancel);

            add(new JScrollPane(list), BorderLayout.CENTER);
            add(btmPnl, BorderLayout.SOUTH);
        }

        private void filterParts() {
            String kw = txtSearch.getText().trim();
            model.clear();
            for (PartItem p : allParts) {
                if (kw.isEmpty() || String.valueOf(p.id).contains(kw)) {
                    model.addElement(p);
                }
            }
        }

        public int getSelectedId() {
            return selectedId;
        }
    }

    // Dialog chọn Cải Trang (Skin từ item_template type = 5)
    private class SkinSelectorDialog extends JDialog {
        private ShopManagerDAO.CaiTrangTemplate selectedSkin = null;
        private List<ShopManagerDAO.CaiTrangTemplate> allSkins = new ArrayList<>();
        private List<ShopManagerDAO.CaiTrangTemplate> currentFiltered = new ArrayList<>();
        private final DefaultTableModel skinTableModel;
        private final JTable skinTable;
        private final JTextField txtSearch;
        private final JLabel lblCount;

        public SkinSelectorDialog() {
            super((Frame) null, "Chọn Cải Trang (Skin từ item_template type = 5)", true);
            setSize(720, 560);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout(5, 5));

            // Top Search Panel
            JPanel topPnl = new JPanel(new BorderLayout(5, 5));
            topPnl.setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));
            txtSearch = new JTextField();
            txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            txtSearch.setBorder(BorderFactory.createTitledBorder("Tìm kiếm Skin (Tên, ID Cải Trang, Head, Body, Leg):"));
            topPnl.add(txtSearch, BorderLayout.CENTER);
            add(topPnl, BorderLayout.NORTH);

            // Table Model
            String[] columns = {"Icon", "ID Item", "Tên Cải Trang", "Head ID", "Body ID", "Leg ID"};
            skinTableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if (columnIndex == 0) return ImageIcon.class;
                    return Object.class;
                }
            };

            skinTable = new JTable(skinTableModel);
            skinTable.setRowHeight(48);
            skinTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            skinTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            skinTable.getColumnModel().getColumn(0).setPreferredWidth(60);
            skinTable.getColumnModel().getColumn(0).setMaxWidth(70);
            skinTable.getColumnModel().getColumn(1).setPreferredWidth(70);
            skinTable.getColumnModel().getColumn(1).setMaxWidth(85);
            skinTable.getColumnModel().getColumn(2).setPreferredWidth(260);
            skinTable.getColumnModel().getColumn(3).setPreferredWidth(75);
            skinTable.getColumnModel().getColumn(4).setPreferredWidth(75);
            skinTable.getColumnModel().getColumn(5).setPreferredWidth(75);

            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            skinTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
            skinTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
            skinTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
            skinTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

            JScrollPane scrollPane = new JScrollPane(skinTable);
            add(scrollPane, BorderLayout.CENTER);

            // Bottom Panel
            JPanel btmPnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
            lblCount = new JLabel("Đang tải dữ liệu...");
            lblCount.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            btmPnl.add(lblCount);

            JButton btnSelect = new JButton("Áp Dụng Skin Này");
            btnSelect.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnSelect.setBackground(new Color(40, 167, 69));
            btnSelect.setForeground(Color.WHITE);
            btnSelect.addActionListener(e -> applySelection());

            JButton btnCancel = new JButton("Đóng");
            btnCancel.addActionListener(e -> dispose());

            btmPnl.add(btnSelect);
            btmPnl.add(btnCancel);
            add(btmPnl, BorderLayout.SOUTH);

            // Double click to select
            skinTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        applySelection();
                    }
                }
            });

            // Search filtering
            txtSearch.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { filterTable(); }
                public void removeUpdate(DocumentEvent e) { filterTable(); }
                public void changedUpdate(DocumentEvent e) { filterTable(); }
            });

            // Background loader
            new SwingWorker<List<ShopManagerDAO.CaiTrangTemplate>, Void>() {
                @Override
                protected List<ShopManagerDAO.CaiTrangTemplate> doInBackground() {
                    return ShopManagerDAO.gI().getAllCaiTrang();
                }

                @Override
                protected void done() {
                    try {
                        allSkins = get();
                        lblCount.setText("Tổng số: " + allSkins.size() + " cải trang");
                        filterTable();
                    } catch (Exception e) {
                        lblCount.setText("Lỗi tải cải trang: " + e.getMessage());
                    }
                }
            }.execute();
        }

        private void filterTable() {
            String kw = txtSearch.getText().trim().toLowerCase();
            skinTableModel.setRowCount(0);
            currentFiltered.clear();
            for (ShopManagerDAO.CaiTrangTemplate ct : allSkins) {
                boolean match = kw.isEmpty()
                        || String.valueOf(ct.id).contains(kw)
                        || (ct.name != null && ct.name.toLowerCase().contains(kw))
                        || String.valueOf(ct.head).contains(kw)
                        || String.valueOf(ct.body).contains(kw)
                        || String.valueOf(ct.leg).contains(kw);
                if (match) {
                    currentFiltered.add(ct);
                    ImageIcon icon = ct.iconId > 0 ? getSmallIcon(ct.iconId) : null;
                    skinTableModel.addRow(new Object[]{
                        icon,
                        ct.id,
                        ct.name,
                        ct.head,
                        ct.body,
                        ct.leg
                    });
                }
            }
        }

        private void applySelection() {
            int row = skinTable.getSelectedRow();
            if (row >= 0 && row < currentFiltered.size()) {
                selectedSkin = currentFiltered.get(row);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một cải trang trong bảng!");
            }
        }

        public ShopManagerDAO.CaiTrangTemplate getSelectedSkin() {
            return selectedSkin;
        }
    }

    private JPanel createFormRow(String label, JComponent comp) {
        JPanel r = new JPanel(new BorderLayout(5, 0));
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(90, 25));
        r.add(l, BorderLayout.WEST);
        r.add(comp, BorderLayout.CENTER);
        return r;
    }

    private class BossCanvas extends JPanel {
        public BossCanvas() {
            setPreferredSize(new Dimension(400, 300));
            setBackground(new Color(30, 30, 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            int cx = getWidth() / 2, cy = getHeight() / 2 + 50;
            g2d.setColor(Color.WHITE);
            g2d.drawString("Outfit: Head [" + selectedHeadPartId + "] | Body [" + selectedBodyPartId + "] | Leg [" + selectedLegPartId + "]", 10, 20);

            // Draw Order: Leg -> Body -> Head
            drawPart(g2d, "leg", cx, cy);
            drawPart(g2d, "body", cx, cy);
            drawPart(g2d, "head", cx, cy);
        }

        private void drawPart(Graphics2D g2d, String type, int cx, int cy) {
            List<PartFrame> frms = type.equals("head") ? headFrames : (type.equals("body") ? bodyFrames : legFrames);
            int idx = type.equals("head") ? 0 : (type.equals("body") ? 2 : 1);
            if (!frms.isEmpty()) {
                int frameIdx = currentFrame % CHAR_INFO.length;
                int[] info = CHAR_INFO[frameIdx][idx];
                PartFrame pf = frms.get(info[0] % frms.size());
                BufferedImage img = loadIcon(pf.iconId);
                if (img != null) {
                    int x = cx + (info[1] + pf.dx) * ZOOM;
                    int y = cy + (-info[2] + pf.dy) * ZOOM;
                    g2d.drawImage(img, x, y, null);
                }
            }
        }
    }
}
