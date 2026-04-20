package com.girlkun.tool.screens.boss_scr;

import com.girlkun.tool.shopmanager.models.*;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.text.Normalizer;
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

    private List<PartFrame> headFrames = new ArrayList<>();
    private List<PartFrame> bodyFrames = new ArrayList<>();
    private List<PartFrame> legFrames = new ArrayList<>();
    private Map<Integer, BufferedImage> iconCache = new HashMap<>();

    private int selectedHeadPartId = 0, selectedBodyPartId = 0, selectedLegPartId = 0;

    // UI Components
    private JTextField txtBossName, txtBossId, txtDefineName, txtDame, txtHp, txtMapJoin, txtSecondsRest;
    private JTextField txtTextS, txtTextM, txtTextE;
    private JLabel lblDefineNameStatus;
    private JComboBox<String> cboGender;
    private JList<BossConfig> listBoss;
    private DefaultListModel<BossConfig> listBossModel;
    private JTextField txtSearchBoss;
    private JComboBox<String> cboAppearType, cboBossType;
    private JTextField txtBossesTogether, txtLevelIndex, txtSpawnCount;
    private JCheckBox chkNotifyDisabled, chkZone01Disabled, chkPierceReverse, chkAutoLeaveReset, chkAppendRandomName,
            chkDoneChatAfk, chkEnabled;
    private JTextField txtMaxDamage, txtDamageDivisor, txtDamageFlat, txtDodgeRate;
    private JTextField txtAutoLeaveTimeout, txtAutoLeaveMin, txtAutoLeaveMax;
    private JTextField txtSkipNotify, txtSkipMove, txtCustomClass;
    private JTextArea txtSpecialAbilities;
    private JTable skillTable;
    private DefaultTableModel skillTableModel;
    private JComboBox<ShopManagerDAO.SkillTemplate> cboSkillEditor;
        private Map<Integer, ShopManagerDAO.SkillTemplate> skillCache = new HashMap<>();
    private JButton btnReward;


    private List<ItemOptionTemplate> optionTemplates = new ArrayList<>();
    private String lastRewardConfig = "{}";
    private Map<Integer, ItemTemplate> itemCache = new HashMap<>();

    private BossCanvas canvas;
    private javax.swing.Timer animationTimer;
    private int currentFrame = 0;

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

    public CreateBossScr() {
        super("Create Boss Manager (Fixed UI & Preview)", true, true, true, true);
        initComponents();
        setSize(1600, 920);
        loadBossConfigs();
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
                } catch (Exception e) {
                }
            }
        }.execute();
    }

    private void startAnimation() {
        if (animationTimer != null)
            animationTimer.stop();
        animationTimer = new javax.swing.Timer(200, e -> {
            currentFrame = (currentFrame + 1) % 2; // Only loop Stand 1 & 2
            if (canvas != null)
                canvas.repaint();
        });
        animationTimer.start();
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
                    List<BossConfig> configs = get();
                    listBossModel.clear();
                    String sc = txtSearchBoss.getText().toLowerCase().trim();
                    for (BossConfig boss : configs) {
                        if (sc.isEmpty() || boss.bossName.toLowerCase().contains(sc)
                                || String.valueOf(boss.bossId).contains(sc)) {
                            listBossModel.addElement(boss);
                        }
                    }
                } catch (Exception e) {
                }
            }
        }.execute();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // LEFT COLUMN
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(280, 0));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Danh sách Boss"));
        txtSearchBoss = new JTextField();
        txtSearchBoss.setBorder(BorderFactory.createTitledBorder("Tìm Boss"));
        txtSearchBoss.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                loadBossConfigs();
            }

            public void removeUpdate(DocumentEvent e) {
                loadBossConfigs();
            }

            public void changedUpdate(DocumentEvent e) {
                loadBossConfigs();
            }
        });
        leftPanel.add(txtSearchBoss, BorderLayout.NORTH);
        listBossModel = new DefaultListModel<>();
        listBoss = new JList<>(listBossModel);
        listBoss.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                fillBossInfo(listBoss.getSelectedValue());
        });
        leftPanel.add(new JScrollPane(listBoss), BorderLayout.CENTER);
        JButton btnRefresh = new JButton("Refresh List");
        btnRefresh.addActionListener(e -> loadBossConfigs());
        leftPanel.add(btnRefresh, BorderLayout.SOUTH);

        // MIDDLE COLUMN
        JPanel middlePanel = new JPanel(new BorderLayout(5, 5));
        middlePanel.setPreferredSize(new Dimension(900, 0));
        JPanel midTop = createBasicFormPanel();
        canvas = new BossCanvas();
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBorder(BorderFactory.createTitledBorder("== Text Chat =="));
        chatPanel.add(createFormRow("Xuất hiện:", txtTextS = new JTextField("[\"Ta đã đến\"]")));
        chatPanel.add(createFormRow("Khi đánh:", txtTextM = new JTextField("[\"Haha!\"]")));
        chatPanel.add(createFormRow("Khi chết:", txtTextE = new JTextField("[\"Ta sẽ quay lại\"]")));
        middlePanel.add(midTop, BorderLayout.NORTH);
        middlePanel.add(new JScrollPane(canvas), BorderLayout.CENTER);
        middlePanel.add(chatPanel, BorderLayout.SOUTH);

        // COLUMN 3 (Advanced & Skills)
        JPanel skillPanel = createSkillPanel();
        JComponent advancedPanel = createAdvancedFormPanel();
        JSplitPane splitAdvSkill = new JSplitPane(JSplitPane.VERTICAL_SPLIT, advancedPanel, skillPanel);
        splitAdvSkill.setDividerLocation(350);

        // COMBINE MIDDLE AND RIGHT
        JSplitPane splitMidRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, middlePanel, splitAdvSkill);
        splitMidRight.setDividerLocation(900);

        // COMBINE LEFT AND EVERYTHING ELSE
        JSplitPane splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, splitMidRight);
        splitMain.setDividerLocation(250);

        mainPanel.add(splitMain, BorderLayout.CENTER);
        mainPanel.add(createActionPanel(), BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    private JPanel createBasicFormPanel() {
        JPanel p = new JPanel(new GridLayout(0, 2, 5, 2));
        p.setBorder(new EmptyBorder(5, 5, 5, 5));
        p.add(createFormRow("Tên Boss:", txtBossName = new JTextField("New Boss")));
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
        JPanel dnp = new JPanel(new BorderLayout());
        txtDefineName = new JTextField();
        lblDefineNameStatus = new JLabel("OK");
        lblDefineNameStatus.setForeground(new Color(40, 167, 69));
        dnp.add(txtDefineName, BorderLayout.CENTER);
        dnp.add(lblDefineNameStatus, BorderLayout.EAST);
        p.add(createFormRow("Define Name:", dnp));
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
        p.add(createFormRow("ID (số âm):", txtBossId = new JTextField("-1")));
        p.add(createFormRow("Giới tính:", cboGender = new JComboBox<>(new String[] { "Trái Đất", "Namec", "Xayda" })));
        p.add(createFormRow("Dame:", txtDame = new JTextField("10000")));
        p.add(createFormRow("HP (phẩy):", txtHp = new JTextField("1000000")));
        p.add(createFormRow("Map Join:", txtMapJoin = new JTextField("5")));
        p.add(createFormRow("Hồi (s):", txtSecondsRest = new JTextField("600")));
        JPanel btnP = new JPanel(new GridLayout(1, 3, 2, 0));
        JButton bh = new JButton("Head");
        bh.addActionListener(e -> selectOutfit("head"));
        JButton bb = new JButton("Body");
        bb.addActionListener(e -> selectOutfit("body"));
        JButton bl = new JButton("Leg");
        bl.addActionListener(e -> selectOutfit("leg"));
        btnP.add(bh);
        btnP.add(bb);
        btnP.add(bl);
        p.add(createFormRow("Outfit:", btnP));
        return p;
    }

    private JComponent createAdvancedFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(2, 5, 2, 5);
        int row = 0;
        addGbcRow(p, gbc, row++, "Appear Type:", cboAppearType = new JComboBox<>(
                new String[] { "DEFAULT", "WITH_ANOTHER", "ANOTHER_LEVEL", "CALL_BY_ANOTHER" }));
        addGbcRow(p, gbc, row++, "Together IDs:", txtBossesTogether = new JTextField());
        addGbcRow(p, gbc, row++, "Level Index:", txtLevelIndex = new JTextField("0"));
        addGbcRow(p, gbc, row++, "Boss Type:",
                cboBossType = new JComboBox<>(new String[] { "DEFAULT", "FINAL", "SUMMON" }));
        addGbcRow(p, gbc, row++, "Spawn Count:", txtSpawnCount = new JTextField("1"));
        addGbcRow(p, gbc, row++, "Max Dmg/Hit:", txtMaxDamage = new JTextField());
        addGbcRow(p, gbc, row++, "Dmg Divisor:", txtDamageDivisor = new JTextField());
        addGbcRow(p, gbc, row++, "Dmg Flat Reduc:", txtDamageFlat = new JTextField());
        addGbcRow(p, gbc, row++, "Dodge Rate:", txtDodgeRate = new JTextField());
        addGbcRow(p, gbc, row++, "Auto Leave:", txtAutoLeaveTimeout = new JTextField());
        addGbcRow(p, gbc, row++, "Min/Max Leave:",
                createDuoField(txtAutoLeaveMin = new JTextField(), txtAutoLeaveMax = new JTextField()));
        addGbcRow(p, gbc, row++, "Skip Notify/Move:",
                createDuoField(txtSkipNotify = new JTextField(), txtSkipMove = new JTextField()));
        addGbcRow(p, gbc, row++, "Custom Class:", txtCustomClass = new JTextField());
        JPanel checks = new JPanel(new GridLayout(0, 3));
        checks.add(chkPierceReverse = new JCheckBox("Pierce"));
        checks.add(chkAutoLeaveReset = new JCheckBox("Reset"));
        checks.add(chkNotifyDisabled = new JCheckBox("No Notif"));
        checks.add(chkZone01Disabled = new JCheckBox("No Z01"));
        checks.add(chkAppendRandomName = new JCheckBox("RandNm"));
        checks.add(chkDoneChatAfk = new JCheckBox("ChatAfk"));
        checks.add(chkEnabled = new JCheckBox("Enab", true));
        gbc.gridy = row++;
        p.add(checks, gbc);

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        txtSpecialAbilities = new JTextArea(3, 20);
        p.add(createScrollRow("Special Abilities (JSON):", txtSpecialAbilities), gbc);

        return new JScrollPane(p);
    }

    private void addGbcRow(JPanel p, GridBagConstraints gbc, int y, String label, JComponent comp) {
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0.3;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        p.add(comp, gbc);
    }

    private JPanel createDuoField(JTextField f1, JTextField f2) {
        JPanel p = new JPanel(new GridLayout(1, 2, 2, 0));
        p.add(f1);
        p.add(f2);
        return p;
    }

    private JPanel createSkillPanel() {
        JPanel sp = new JPanel(new BorderLayout(5, 5));
        sp.setBorder(BorderFactory.createTitledBorder("Kỹ năng Boss"));

        skillTableModel = new DefaultTableModel(new Object[] { "Skill", "Lv", "CD" }, 0);
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
                } catch (Exception e) {
                }
            }
        }.execute();
        skillTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(cboSkillEditor));

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addS = new JButton("+ Skill");
        addS.addActionListener(e -> skillTableModel.addRow(new Object[] { "0", "7", "1000" }));
        JButton remS = new JButton("- Skill");
        remS.addActionListener(e -> {
            int r = skillTable.getSelectedRow();
            if (r >= 0)
                skillTableModel.removeRow(r);
        });
                btnReward = new JButton("Cấu hình Phần thưởng");

        btnReward.setBackground(new Color(153, 50, 204));
        btnReward.setForeground(Color.WHITE);
        btnReward.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnReward.addActionListener(e -> {
            BossConfig b = listBoss.getSelectedValue();
            if (b == null) {
                b = new BossConfig();
                b.bossName = txtBossName.getText();
                b.rewardConfig = this.lastRewardConfig;
            }
            Window win = SwingUtilities.getWindowAncestor(CreateBossScr.this);
            RewardConfigDialog diag = new RewardConfigDialog(win, b, optionTemplates);
            diag.setVisible(true);
                        this.lastRewardConfig = b.rewardConfig;
            updateRewardButtonText();

        });

        bp.add(addS);
        bp.add(remS);
        sp.add(new JScrollPane(skillTable), BorderLayout.CENTER);

        JPanel bp2 = new JPanel(new GridLayout(2, 1, 5, 5));
        bp2.add(bp);
        bp2.add(btnReward);
        sp.add(bp2, BorderLayout.SOUTH);
        return sp;
    }



    private void styleBtn(JButton b, Color c) {
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setPreferredSize(new Dimension(100, 30));
    }

    private JPanel createActionPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 5));
        JButton btnSave = new JButton("💾 LƯU BOSS VÀO CƠ SỞ DỮ LIỆU");
        btnSave.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
        btnSave.setPreferredSize(new Dimension(300, 45));
        btnSave.addActionListener(e -> saveBoss());
        JButton btnRes = new JButton("Reset Form");
        btnRes.addActionListener(e -> resetForm());
        p.add(btnRes);
        p.add(btnSave);
        return p;
    }

    private void fillBossInfo(BossConfig boss) {
        if (boss == null)
            return;
        txtBossName.setText(boss.bossName);
        txtBossId.setText(String.valueOf(boss.bossId));
        txtDame.setText(String.valueOf(boss.dame));
        txtHp.setText(boss.hp);
        txtMapJoin.setText(boss.mapJoin);
        txtSecondsRest.setText(String.valueOf(boss.secondsRest));
        cboGender.setSelectedIndex(boss.gender >= 0 && boss.gender <= 2 ? boss.gender : 0);
        txtTextS.setText(boss.textS);
        txtTextM.setText(boss.textM);
        txtTextE.setText(boss.textE);
        cboAppearType.setSelectedIndex(boss.appearType);
        txtBossesTogether.setText(boss.bossesAppearTogether);
        txtLevelIndex.setText(String.valueOf(boss.levelIndex));
        cboBossType.setSelectedItem(boss.bossType);
        txtSpawnCount.setText(String.valueOf(boss.spawnCount));
        txtMaxDamage.setText(boss.maxDamagePerHit != null ? String.valueOf(boss.maxDamagePerHit) : "");
        txtDamageDivisor.setText(boss.damageDivisor != null ? String.valueOf(boss.damageDivisor) : "");
        txtDamageFlat.setText(boss.damageFlatReduction != null ? String.valueOf(boss.damageFlatReduction) : "");
        txtDodgeRate.setText(boss.dodgeRate != null ? String.valueOf(boss.dodgeRate) : "");
        txtAutoLeaveTimeout.setText(boss.autoLeaveTimeout != null ? String.valueOf(boss.autoLeaveTimeout) : "");
        txtAutoLeaveMin.setText(boss.autoLeaveRandomMin != null ? String.valueOf(boss.autoLeaveRandomMin) : "");
        txtAutoLeaveMax.setText(boss.autoLeaveRandomMax != null ? String.valueOf(boss.autoLeaveRandomMax) : "");
        txtSkipNotify.setText(boss.skipNotifyAtLevel != null ? String.valueOf(boss.skipNotifyAtLevel) : "");
        txtSkipMove.setText(boss.skipMoveAtLevel != null ? String.valueOf(boss.skipMoveAtLevel) : "");
        txtCustomClass.setText(boss.customClass);
        chkPierceReverse.setSelected(boss.pierceReverse);
        chkAutoLeaveReset.setSelected(boss.autoLeaveResetOnPlayer);
        chkNotifyDisabled.setSelected(boss.isNotifyDisabled);
        chkZone01Disabled.setSelected(boss.isZone01SpawnDisabled);
        chkAppendRandomName.setSelected(boss.appendRandomName);
        chkDoneChatAfk.setSelected(boss.doneChatSToAfk);
        chkEnabled.setSelected(boss.enabled);
        txtSpecialAbilities.setText(boss.specialAbilities);

                                this.lastRewardConfig = boss.rewardConfig != null ? boss.rewardConfig : "{}";
        updateRewardButtonText();


        skillTableModel.setRowCount(0);
        if (boss.skills != null && !boss.skills.isEmpty()) {
            try {
                JSONArray sa = (JSONArray) JSONValue.parse(boss.skills);
                for (Object i : sa) {
                    JSONArray s = (JSONArray) i;
                    int skillId = Integer.parseInt(s.get(0).toString());
                    ShopManagerDAO.SkillTemplate st = skillCache.get(skillId);
                    skillTableModel.addRow(new Object[] {
                            st != null ? st : String.valueOf(skillId),
                            s.get(1).toString(),
                            s.get(2).toString()
                    });
                }
            } catch (Exception e) {
            }
        }
        if (boss.outfit != null) {
            String[] p = boss.outfit.split(",");
            if (p.length >= 3) {
                selectedHeadPartId = Integer.parseInt(p[0].trim());
                selectedBodyPartId = Integer.parseInt(p[1].trim());
                selectedLegPartId = Integer.parseInt(p[2].trim());
                loadPartPositions();
            }
        }
    }

    private void loadPartPositions() {
        headFrames.clear();
        bodyFrames.clear();
        legFrames.clear();
        loadFrames(selectedHeadPartId, "head");
        loadFrames(selectedBodyPartId, "body");
        loadFrames(selectedLegPartId, "leg");
        canvas.repaint();
    }

    private void loadFrames(int partId, String type) {
        if (partId <= 0)
            return;
        new SwingWorker<List<PartFrame>, Void>() {
            @Override
            protected List<PartFrame> doInBackground() {
                List<PartFrame> list = new ArrayList<>();
                try {
                    ShopManagerDAO.PartData pd = ShopManagerDAO.gI().getPartData(partId);
                    if (pd != null && pd.data != null) {
                        org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();
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
                    canvas.repaint();
                } catch (Exception e) {
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
        } catch (Exception e) {
        }
        return null;
    }

    private void saveBoss() {
        try {
            BossConfig b = new BossConfig();
            b.bossId = Integer.parseInt(txtBossId.getText());
            b.bossName = txtBossName.getText().trim();
            b.gender = (byte) cboGender.getSelectedIndex();
            b.outfit = selectedHeadPartId + "," + selectedBodyPartId + "," + selectedLegPartId + ",-1,-1,-1";
            b.dame = Long.parseLong(txtDame.getText());
            b.hp = txtHp.getText().trim();
            b.mapJoin = txtMapJoin.getText().trim();
            b.secondsRest = Integer.parseInt(txtSecondsRest.getText());
            b.textS = txtTextS.getText().trim();
            b.textM = txtTextM.getText().trim();
            b.textE = txtTextE.getText().trim();
            b.appearType = (byte) cboAppearType.getSelectedIndex();
            b.bossesAppearTogether = txtBossesTogether.getText().trim();
            b.levelIndex = Byte.parseByte(txtLevelIndex.getText());
            b.bossType = cboBossType.getSelectedItem().toString();
            b.enabled = chkEnabled.isSelected();
            b.isNotifyDisabled = chkNotifyDisabled.isSelected();
            b.isZone01SpawnDisabled = chkZone01Disabled.isSelected();
            b.spawnCount = Integer.parseInt(txtSpawnCount.getText());
            b.maxDamagePerHit = txtMaxDamage.getText().isEmpty() ? null : Long.parseLong(txtMaxDamage.getText());
            b.damageDivisor = txtDamageDivisor.getText().isEmpty() ? null
                    : Integer.parseInt(txtDamageDivisor.getText());
            b.damageFlatReduction = txtDamageFlat.getText().isEmpty() ? null : Long.parseLong(txtDamageFlat.getText());
            b.dodgeRate = txtDodgeRate.getText().isEmpty() ? null : Integer.parseInt(txtDodgeRate.getText());
            b.pierceReverse = chkPierceReverse.isSelected();
            b.autoLeaveTimeout = txtAutoLeaveTimeout.getText().isEmpty() ? null
                    : Long.parseLong(txtAutoLeaveTimeout.getText());
            b.autoLeaveResetOnPlayer = chkAutoLeaveReset.isSelected();
            b.autoLeaveRandomMin = txtAutoLeaveMin.getText().isEmpty() ? null
                    : Long.parseLong(txtAutoLeaveMin.getText());
            b.autoLeaveRandomMax = txtAutoLeaveMax.getText().isEmpty() ? null
                    : Long.parseLong(txtAutoLeaveMax.getText());
            b.appendRandomName = chkAppendRandomName.isSelected();
            b.doneChatSToAfk = chkDoneChatAfk.isSelected();
            b.skipNotifyAtLevel = txtSkipNotify.getText().isEmpty() ? null : Integer.parseInt(txtSkipNotify.getText());
            b.skipMoveAtLevel = txtSkipMove.getText().isEmpty() ? null : Integer.parseInt(txtSkipMove.getText());
            b.specialAbilities = txtSpecialAbilities.getText().trim();

            b.rewardConfig = this.lastRewardConfig;

            b.customClass = txtCustomClass.getText().trim();
            JSONArray sa = new JSONArray();
            for (int i = 0; i < skillTableModel.getRowCount(); i++) {
                JSONArray s = new JSONArray();
                Object skillObj = skillTableModel.getValueAt(i, 0);
                int skillId = 0;
                if (skillObj instanceof ShopManagerDAO.SkillTemplate) {
                    skillId = ((ShopManagerDAO.SkillTemplate) skillObj).id;
                } else {
                    skillId = Integer.parseInt(skillObj.toString());
                }
                s.add(skillId);
                s.add(Integer.parseInt(skillTableModel.getValueAt(i, 1).toString()));
                s.add(Integer.parseInt(skillTableModel.getValueAt(i, 2).toString()));
                sa.add(s);
            }
            b.skills = sa.toJSONString();
            ShopManagerDAO.gI().saveBossConfig(b);
            JOptionPane.showMessageDialog(this, "Save Success!");
            loadBossConfigs();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error Save: " + e.getMessage());
        }
    }

    private void generateDefineName() {
        if (!txtBossName.isFocusOwner())
            return;
        String in = txtBossName.getText().trim();
        if (in.isEmpty()) {
            txtDefineName.setText("");
            return;
        }
        String normalized = Normalizer.normalize(in, Normalizer.Form.NFD);
        String res = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toUpperCase().replace(" ", "_")
                .replaceAll("[^A-Z0-9_]", "");
        txtDefineName.setText(res);
    }

    private void validateDefineName() {
        String n = txtDefineName.getText().trim();
        if (n.isEmpty()) {
            lblDefineNameStatus.setText("--");
            return;
        }
        boolean ex = false;
        for (int i = 0; i < listBossModel.size(); i++) {
            if (listBossModel.get(i).bossName.equalsIgnoreCase(n.replace("_", " "))) {
                ex = true;
                break;
            }
        }
        lblDefineNameStatus.setText(ex ? "EXIST" : "OK");
        lblDefineNameStatus.setForeground(ex ? Color.RED : new Color(40, 167, 69));
    }

    private void resetForm() {
        txtBossName.setText("New");
        txtBossId.setText("-1");
        txtDame.setText("0");
        txtHp.setText("0");
        skillTableModel.setRowCount(0);
        headFrames.clear();
        bodyFrames.clear();
        legFrames.clear();
        selectedHeadPartId = selectedBodyPartId = selectedLegPartId = 0;
        txtSpecialAbilities.setText("");
                lastRewardConfig = "{}";
        updateRewardButtonText();

        canvas.repaint();
    }

    private void selectOutfit(String type) {
        OutfitSelectorDialog dialog = new OutfitSelectorDialog(type);
        dialog.setVisible(true);
        if (dialog.getSelectedId() > 0) {
            if (type.equals("head"))
                selectedHeadPartId = dialog.getSelectedId();
            else if (type.equals("body"))
                selectedBodyPartId = dialog.getSelectedId();
            else
                selectedLegPartId = dialog.getSelectedId();
            loadPartPositions();
        }
    }

    private class OutfitSelectorDialog extends JDialog {
        private int selectedId = -1;

        public OutfitSelectorDialog(String type) {
            super((Frame) null, "Chọn " + type, true);
            setSize(400, 500);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            DefaultListModel<String> model = new DefaultListModel<>();
            JList<String> list = new JList<>(model);

            // Load IDs from DB part_template
            new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() {
                    try {
                        Connection conn = ShopManagerDAO.gI().getConnection();
                        try (PreparedStatement stmt = conn
                                .prepareStatement("SELECT id FROM part WHERE TYPE = ? LIMIT 500")) {
                            stmt.setInt(1, type.equals("head") ? 0 : (type.equals("body") ? 1 : 2));
                            ResultSet rs = stmt.executeQuery();
                            while (rs.next())
                                publish(String.valueOf(rs.getInt("id")));
                        }
                    } catch (Exception e) {
                    }
                    return null;
                }

                @Override
                protected void process(List<String> chunks) {
                    for (String s : chunks)
                        model.addElement(s);
                }
            }.execute();

            list.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                    selectedId = Integer.parseInt(list.getSelectedValue());
                }
            });

            JButton btnOk = new JButton("Chọn");
            btnOk.addActionListener(e -> dispose());

            add(new JScrollPane(list), BorderLayout.CENTER);
            add(btnOk, BorderLayout.SOUTH);
        }

        public int getSelectedId() {
            return selectedId;
        }
    }

    private JPanel createFormRow(String label, JComponent comp) {
        JPanel r = new JPanel(new BorderLayout(5, 0));
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(80, 25));
        r.add(l, BorderLayout.WEST);
        r.add(comp, BorderLayout.CENTER);
        return r;
    }

    private JPanel createScrollRow(String label, JTextArea area) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(label), BorderLayout.NORTH);
        area.setFont(new Font("Consolas", Font.PLAIN, 11));
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        return p;
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
            g2d.drawString("Preview: " + selectedHeadPartId + "-" + selectedBodyPartId + "-" + selectedLegPartId, 10,
                    20);

            // Draw Order: Leg -> Body -> Head (Following CreateBossOld.java)
            drawPart(g2d, "leg", cx, cy);
            drawPart(g2d, "body", cx, cy);
            drawPart(g2d, "head", cx, cy);

            // Draw Animation Name
            g2d.setColor(Color.YELLOW);
            g2d.drawString("Anim: " + ANIM_NAMES[currentFrame % ANIM_NAMES.length], 10, 40);
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
                    g2d.drawImage(img, x, y, null); // Use natural size as icons are already x4
                }
            }
        }
    }
    private void updateRewardButtonText() {
        int count = 0;
        try {
            if (lastRewardConfig != null && !lastRewardConfig.isEmpty() && !lastRewardConfig.equals("{}")) {
                org.json.simple.JSONObject rj = (org.json.simple.JSONObject) org.json.simple.JSONValue.parse(lastRewardConfig);
                if (rj != null && rj.containsKey("items")) {
                    org.json.simple.JSONArray ia = (org.json.simple.JSONArray) rj.get("items");
                    count = ia.size();
                }
            }
        } catch (Exception e) {
        }
        if (btnReward != null) {
            btnReward.setText("Cấu hình Phần thưởng (" + count + " items)");
        }
    }
}

