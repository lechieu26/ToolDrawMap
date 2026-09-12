package com.girlkun.tool.screens.boss_scr;

import com.girlkun.tool.shopmanager.models.*;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewardConfigDialog extends JDialog {

    private final BossConfig boss;
    private final List<ItemOptionTemplate> optionTemplates;
    private final Map<Integer, String> optionNameMap = new HashMap<>();
    private final Map<Integer, ItemTemplate> itemCache = new HashMap<>();
    private final List<RewardItem> currentRewardItems = new ArrayList<>();

    private JTable rewardItemTable;
    private DefaultTableModel rewardItemTableModel;

    private JComboBox<ItemTemplate> cbRewardItemTemplate;
    private JTextField txtRewardFindItem, txtQuantityMin, txtQuantityMax, txtRewardRate;
    private JTextField txtEventPoint, txtActivePoint;
    private JComboBox<ItemOptionTemplate> cbRewardOpt;
    private JTextField txtRewardOptParam;
    private JTable tblRewardOpts;
    private DefaultTableModel modelRewardOpts;
    private int selectedRewardItemIdx = -1;

    public RewardConfigDialog(Window owner, BossConfig boss, List<ItemOptionTemplate> optionTemplates) {
        super(owner, "Cấu hình Phần thưởng Boss: " + boss.bossName, ModalityType.APPLICATION_MODAL);
        this.boss = boss;
        this.optionTemplates = optionTemplates != null ? optionTemplates : new ArrayList<>();
        for (ItemOptionTemplate t : this.optionTemplates) {
            optionNameMap.put(t.id, t.name);
        }

        setSize(1050, 750);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(5, 5));

        parseInitialData();

        add(createRewardContentPanel(), BorderLayout.CENTER);

        JButton btnClose = new JButton("Lưu & Đóng Cấu Hình Phần Thưởng");
        styleBtn(btnClose, new Color(40, 167, 69));
        btnClose.setPreferredSize(new Dimension(0, 42));
        btnClose.addActionListener(e -> {
            applyRewardsToBoss();
            dispose();
        });
        add(btnClose, BorderLayout.SOUTH);
    }

    private void parseInitialData() {
        currentRewardItems.clear();
        // 1. Try loading from boss.rewards list first
        if (boss.rewards != null && !boss.rewards.isEmpty()) {
            for (BossRewardConfig r : boss.rewards) {
                ItemTemplate it = getItemTemplate(r.itemId);
                RewardItem ri = new RewardItem(it, r.quantityMin, r.quantityMax, r.rate, r.eventPoint, r.activePoint);
                parseItemOptions(ri, r.itemOptions);
                currentRewardItems.add(ri);
            }
            return;
        }

        // 2. Fallback to legacy rewardConfig JSON string
        if (boss.rewardConfig != null && !boss.rewardConfig.trim().isEmpty() && !boss.rewardConfig.equals("{}")) {
            try {
                JSONObject rj = (JSONObject) JSONValue.parse(boss.rewardConfig);
                if (rj != null && rj.containsKey("items")) {
                    JSONArray ia = (JSONArray) rj.get("items");
                    for (Object o : ia) {
                        JSONObject item = (JSONObject) o;
                        int tid = ((Number) item.get("id")).intValue();
                        ItemTemplate it = getItemTemplate(tid);
                        int qMin = 1, qMax = 1;
                        if (item.containsKey("quantityMin")) {
                            qMin = ((Number) item.get("quantityMin")).intValue();
                            qMax = item.containsKey("quantityMax") ? ((Number) item.get("quantityMax")).intValue() : qMin;
                        } else if (item.containsKey("quantity")) {
                            qMin = qMax = ((Number) item.get("quantity")).intValue();
                        }
                        double rate = item.containsKey("rate") ? ((Number) item.get("rate")).doubleValue() : 100.0;
                        int evPt = item.containsKey("eventPoint") ? ((Number) item.get("eventPoint")).intValue() : 0;
                        int actPt = item.containsKey("activePoint") ? ((Number) item.get("activePoint")).intValue() : 0;

                        RewardItem ri = new RewardItem(it, qMin, qMax, rate, evPt, actPt);
                        if (item.containsKey("options")) {
                            JSONArray opts = (JSONArray) item.get("options");
                            for (Object optO : opts) {
                                if (optO instanceof JSONObject) {
                                    JSONObject op = (JSONObject) optO;
                                    int oid = ((Number) op.get("id")).intValue();
                                    int oprm = ((Number) op.get("param")).intValue();
                                    ri.options.add(new ItemOption(oid, oprm));
                                } else if (optO instanceof JSONArray) {
                                    JSONArray op = (JSONArray) optO;
                                    if (op.size() >= 2) {
                                        int oid = ((Number) op.get(0)).intValue();
                                        int oprm = ((Number) op.get(1)).intValue();
                                        ri.options.add(new ItemOption(oid, oprm));
                                    }
                                }
                            }
                        }
                        currentRewardItems.add(ri);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ItemTemplate getItemTemplate(int itemId) {
        ItemTemplate it = itemCache.get(itemId);
        if (it == null) {
            String name = ShopManagerDAO.gI().getItemName(itemId);
            it = new ItemTemplate(itemId, (name != null && !name.isEmpty()) ? name : ("Item " + itemId));
            itemCache.put(itemId, it);
        }
        return it;
    }

    private void parseItemOptions(RewardItem ri, String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty() || jsonStr.equals("[]")) {
            return;
        }
        try {
            Object obj = JSONValue.parse(jsonStr);
            if (obj instanceof JSONArray) {
                JSONArray arr = (JSONArray) obj;
                for (Object item : arr) {
                    if (item instanceof JSONArray) {
                        JSONArray optArr = (JSONArray) item;
                        if (optArr.size() >= 2) {
                            int oid = Integer.parseInt(String.valueOf(optArr.get(0)));
                            int oprm = Integer.parseInt(String.valueOf(optArr.get(1)));
                            ri.options.add(new ItemOption(oid, oprm));
                        }
                    } else if (item instanceof JSONObject) {
                        JSONObject optObj = (JSONObject) item;
                        int oid = Integer.parseInt(String.valueOf(optObj.get("id")));
                        int oprm = Integer.parseInt(String.valueOf(optObj.get("param")));
                        ri.options.add(new ItemOption(oid, oprm));
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private JPanel createRewardContentPanel() {
        JPanel main = new JPanel(new BorderLayout(5, 5));
        main.setBorder(new EmptyBorder(8, 8, 8, 8));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Danh sách item phần thưởng
        JPanel pnlItems = new JPanel(new BorderLayout(5, 5));
        pnlItems.setBorder(BorderFactory.createTitledBorder("Danh sách Vật Phẩm rơi từ Boss"));
        rewardItemTableModel = new DefaultTableModel(new Object[]{"ID", "Tên Item", "Số Lượng", "Tỉ Lệ (%)", "Điểm SK", "Điểm NĐ"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rewardItemTable = new JTable(rewardItemTableModel);
        rewardItemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rewardItemTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectRewardItem(rewardItemTable.getSelectedRow());
            }
        });
        pnlItems.add(new JScrollPane(rewardItemTable), BorderLayout.CENTER);

        JButton btnDeleteSelected = new JButton("Xóa Item Đã Chọn");
        btnDeleteSelected.setBackground(new Color(220, 53, 69));
        btnDeleteSelected.setForeground(Color.WHITE);
        btnDeleteSelected.addActionListener(e -> deleteSelectedRewardItem());
        pnlItems.add(btnDeleteSelected, BorderLayout.SOUTH);
        leftSplit.setTopComponent(pnlItems);

        // Bảng Option của item được chọn
        JPanel optPanel = new JPanel(new BorderLayout(5, 5));
        optPanel.setBorder(BorderFactory.createTitledBorder("Chỉ số / Option của Item đã chọn"));
        JPanel optTop = new JPanel(new GridBagLayout());
        GridBagConstraints gbcO = new GridBagConstraints();
        gbcO.insets = new Insets(2, 4, 2, 4);
        gbcO.fill = GridBagConstraints.HORIZONTAL;

        cbRewardOpt = new JComboBox<>();
        for (ItemOptionTemplate t : optionTemplates) {
            cbRewardOpt.addItem(t);
        }
        txtRewardOptParam = new JTextField("10");

        JButton btnAddOpt = new JButton("+ Option");
        btnAddOpt.setBackground(new Color(40, 167, 69));
        btnAddOpt.setForeground(Color.WHITE);
        btnAddOpt.addActionListener(e -> addRewardOption());

        JButton btnDelOpt = new JButton("- Option");
        btnDelOpt.setBackground(new Color(220, 53, 69));
        btnDelOpt.setForeground(Color.WHITE);
        btnDelOpt.addActionListener(e -> deleteRewardOption());

        gbcO.gridx = 0;
        gbcO.weightx = 1.0;
        optTop.add(cbRewardOpt, gbcO);
        gbcO.gridx = 1;
        gbcO.weightx = 0.3;
        optTop.add(txtRewardOptParam, gbcO);
        gbcO.gridx = 2;
        gbcO.weightx = 0;
        optTop.add(btnAddOpt, gbcO);
        gbcO.gridx = 3;
        gbcO.weightx = 0;
        optTop.add(btnDelOpt, gbcO);

        modelRewardOpts = new DefaultTableModel(new Object[]{"ID", "Chỉ số / Option", "Param"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblRewardOpts = new JTable(modelRewardOpts);
        optPanel.add(optTop, BorderLayout.NORTH);
        optPanel.add(new JScrollPane(tblRewardOpts), BorderLayout.CENTER);
        leftSplit.setBottomComponent(optPanel);
        leftSplit.setDividerLocation(340);

        // Panel thêm mới item
        JPanel addPanel = new JPanel(new GridBagLayout());
        addPanel.setBorder(BorderFactory.createTitledBorder("Thêm Vật Phẩm Rơi Mới"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        int row = 0;
        gbc.gridy = row++;
        addPanel.add(new JLabel("Tìm kiếm Item theo tên:"), gbc);
        gbc.gridy = row++;
        JPanel searchP = new JPanel(new BorderLayout(3, 0));
        txtRewardFindItem = new JTextField();
        JButton btnFind = new JButton("Tìm");
        btnFind.addActionListener(e -> searchRewardTemplate());
        txtRewardFindItem.addActionListener(e -> searchRewardTemplate());
        searchP.add(txtRewardFindItem, BorderLayout.CENTER);
        searchP.add(btnFind, BorderLayout.EAST);
        addPanel.add(searchP, gbc);

        gbc.gridy = row++;
        addPanel.add(new JLabel("Chọn Item:"), gbc);
        gbc.gridy = row++;
        addPanel.add(cbRewardItemTemplate = new JComboBox<>(), gbc);

        gbc.gridy = row++;
        addPanel.add(new JLabel("Số lượng rơi (Min - Max):"), gbc);
        gbc.gridy = row++;
        JPanel qtyP = new JPanel(new GridLayout(1, 2, 4, 0));
        qtyP.add(txtQuantityMin = new JTextField("1"));
        qtyP.add(txtQuantityMax = new JTextField("1"));
        addPanel.add(qtyP, gbc);

        gbc.gridy = row++;
        addPanel.add(new JLabel("Tỉ lệ rơi (%: vd 100 = 100%, 5.5 = 5.5%):"), gbc);
        gbc.gridy = row++;
        addPanel.add(txtRewardRate = new JTextField("100"), gbc);

        gbc.gridy = row++;
        addPanel.add(new JLabel("Thưởng người hạ: Điểm SK - Điểm Năng Động:"), gbc);
        gbc.gridy = row++;
        JPanel ptP = new JPanel(new GridLayout(1, 2, 4, 0));
        ptP.add(txtEventPoint = new JTextField("0"));
        ptP.add(txtActivePoint = new JTextField("0"));
        addPanel.add(ptP, gbc);

        gbc.gridy = row++;
        JButton btnAdd = new JButton("+ Thêm Vào Danh Sách Rơi");
        btnAdd.setBackground(new Color(40, 167, 69));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAdd.setPreferredSize(new Dimension(0, 36));
        btnAdd.addActionListener(e -> addRewardItem());
        addPanel.add(btnAdd, gbc);

        gbc.gridy = row++;
        gbc.weighty = 1.0;
        addPanel.add(new JPanel(), gbc);

        mainSplit.setLeftComponent(leftSplit);
        mainSplit.setRightComponent(addPanel);
        mainSplit.setDividerLocation(660);
        main.add(mainSplit, BorderLayout.CENTER);

        refreshRewardUI();
        return main;
    }

    private void refreshRewardUI() {
        rewardItemTableModel.setRowCount(0);
        for (RewardItem ri : currentRewardItems) {
            String qtyStr = (ri.quantityMin == ri.quantityMax) ? String.valueOf(ri.quantityMin) : (ri.quantityMin + " - " + ri.quantityMax);
            rewardItemTableModel.addRow(new Object[]{
                    ri.template.id,
                    ri.template.name,
                    qtyStr,
                    ri.rate + "%",
                    ri.eventPoint,
                    ri.activePoint
            });
        }
    }

    private void searchRewardTemplate() {
        cbRewardItemTemplate.removeAllItems();
        new SwingWorker<List<ItemTemplate>, Void>() {
            @Override
            protected List<ItemTemplate> doInBackground() {
                return ShopManagerDAO.gI().getItemTemplates(txtRewardFindItem.getText().trim());
            }

            @Override
            protected void done() {
                try {
                    for (ItemTemplate t : get()) {
                        cbRewardItemTemplate.addItem(t);
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void addRewardItem() {
        ItemTemplate t = (ItemTemplate) cbRewardItemTemplate.getSelectedItem();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng tìm và chọn một Item!");
            return;
        }
        try {
            int qMin = Integer.parseInt(txtQuantityMin.getText().trim());
            int qMax = Integer.parseInt(txtQuantityMax.getText().trim());
            if (qMax < qMin) qMax = qMin;
            double r = Double.parseDouble(txtRewardRate.getText().trim());
            int ev = Integer.parseInt(txtEventPoint.getText().trim());
            int act = Integer.parseInt(txtActivePoint.getText().trim());

            RewardItem ri = new RewardItem(t, qMin, qMax, r, ev, act);
            currentRewardItems.add(ri);
            refreshRewardUI();

            int lastRow = currentRewardItems.size() - 1;
            rewardItemTable.setRowSelectionInterval(lastRow, lastRow);
            selectRewardItem(lastRow);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Thông tin số lượng hoặc tỉ lệ không hợp lệ!");
        }
    }

    private void selectRewardItem(int row) {
        selectedRewardItemIdx = row;
        modelRewardOpts.setRowCount(0);
        if (row >= 0 && row < currentRewardItems.size()) {
            RewardItem ri = currentRewardItems.get(row);
            for (ItemOption o : ri.options) {
                modelRewardOpts.addRow(new Object[]{o.id, optionNameMap.getOrDefault(o.id, "Option " + o.id), o.param});
            }
        }
    }

    private void addRewardOption() {
        if (selectedRewardItemIdx < 0 || selectedRewardItemIdx >= currentRewardItems.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 item trong bảng trước!");
            return;
        }
        ItemOptionTemplate t = (ItemOptionTemplate) cbRewardOpt.getSelectedItem();
        if (t == null) return;
        try {
            int p = Integer.parseInt(txtRewardOptParam.getText().trim());
            currentRewardItems.get(selectedRewardItemIdx).options.add(new ItemOption(t.id, p));
            selectRewardItem(selectedRewardItemIdx);
        } catch (Exception ignored) {
        }
    }

    private void deleteRewardOption() {
        if (selectedRewardItemIdx < 0 || selectedRewardItemIdx >= currentRewardItems.size()) return;
        int row = tblRewardOpts.getSelectedRow();
        if (row >= 0) {
            currentRewardItems.get(selectedRewardItemIdx).options.remove(row);
            selectRewardItem(selectedRewardItemIdx);
        }
    }

    private void deleteSelectedRewardItem() {
        if (selectedRewardItemIdx >= 0 && selectedRewardItemIdx < currentRewardItems.size()) {
            currentRewardItems.remove(selectedRewardItemIdx);
            selectedRewardItemIdx = -1;
            refreshRewardUI();
            modelRewardOpts.setRowCount(0);
        }
    }

    private void applyRewardsToBoss() {
        if (boss.rewards == null) {
            boss.rewards = new ArrayList<>();
        }
        boss.rewards.clear();

        JSONObject rj = new JSONObject();
        JSONArray ria = new JSONArray();

        for (RewardItem item : currentRewardItems) {
            JSONArray optArr = new JSONArray();
            for (ItemOption o : item.options) {
                JSONArray singleOpt = new JSONArray();
                singleOpt.add(o.id);
                singleOpt.add(o.param);
                optArr.add(singleOpt);
            }
            String optJson = optArr.toJSONString();

            BossRewardConfig rc = new BossRewardConfig(
                    0,
                    boss.bossId,
                    item.template.id,
                    item.quantityMin,
                    item.quantityMax,
                    item.rate,
                    optJson,
                    item.eventPoint,
                    item.activePoint
            );
            boss.rewards.add(rc);

            JSONObject legacyObj = new JSONObject();
            legacyObj.put("id", item.template.id);
            legacyObj.put("quantityMin", item.quantityMin);
            legacyObj.put("quantityMax", item.quantityMax);
            legacyObj.put("rate", item.rate);
            legacyObj.put("options", optArr);
            legacyObj.put("eventPoint", item.eventPoint);
            legacyObj.put("activePoint", item.activePoint);
            ria.add(legacyObj);
        }
        rj.put("items", ria);
        boss.rewardConfig = rj.toJSONString();
    }

    private void styleBtn(JButton b, Color c) {
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    static class RewardItem {
        ItemTemplate template;
        int quantityMin = 1;
        int quantityMax = 1;
        double rate = 100.0;
        int eventPoint = 0;
        int activePoint = 0;
        List<ItemOption> options = new ArrayList<>();

        RewardItem(ItemTemplate t, int qMin, int qMax, double r, int ev, int act) {
            this.template = t;
            this.quantityMin = qMin;
            this.quantityMax = qMax;
            this.rate = r;
            this.eventPoint = ev;
            this.activePoint = act;
        }

        @Override
        public String toString() {
            return template != null ? template.toString() : "RewardItem";
        }
    }
}