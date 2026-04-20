package com.girlkun.tool.screens.boss_scr;

import com.girlkun.tool.shopmanager.models.BossConfig;
import com.girlkun.tool.shopmanager.models.ItemOption;
import com.girlkun.tool.shopmanager.models.ItemOptionTemplate;
import com.girlkun.tool.shopmanager.models.ItemTemplate;
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

    private JCheckBox chkCheckTask;
    private JTextField txtPointBossDay;
    private JTable rewardItemTable;
    private DefaultTableModel rewardItemTableModel;

    private JComboBox<ItemTemplate> cbRewardItemTemplate;
    private JTextField txtRewardFindItem, txtRewardQuantity, txtRewardRate;
    private JComboBox<ItemOptionTemplate> cbRewardOpt;
    private JTextField txtRewardOptParam;
    private JTable tblRewardOpts;
    private DefaultTableModel modelRewardOpts;
    private int selectedRewardItemIdx = -1;

    public RewardConfigDialog(Window owner, BossConfig boss, List<ItemOptionTemplate> optionTemplates) {
        super(owner, "Cấu hình Phần thưởng: " + boss.bossName, ModalityType.APPLICATION_MODAL);
        this.boss = boss;
        this.optionTemplates = optionTemplates;
        for (ItemOptionTemplate t : optionTemplates) {
            optionNameMap.put(t.id, t.name);
        }

        setSize(1000, 750);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        parseInitialData();

        add(createRewardContentPanel(), BorderLayout.CENTER);

        JButton btnClose = new JButton("Đóng & Lưu tạm");
        styleBtn(btnClose, new Color(40, 167, 69));
        btnClose.addActionListener(e -> {
            boss.rewardConfig = generateRewardJson();
            dispose();
        });
        add(btnClose, BorderLayout.SOUTH);
    }

    private void parseInitialData() {
        currentRewardItems.clear();
        if (boss.rewardConfig != null && !boss.rewardConfig.isEmpty()) {
            try {
                JSONObject rj = (JSONObject) JSONValue.parse(boss.rewardConfig);
                if (rj != null) {
                    if (rj.containsKey("items")) {
                        JSONArray ia = (JSONArray) rj.get("items");
                        for (Object o : ia) {
                            JSONObject item = (JSONObject) o;
                            int tid = ((Long) item.get("id")).intValue();
                            ItemTemplate it = itemCache.get(tid);
                            if (it == null) {
                                String name = ShopManagerDAO.gI().getItemName(tid);
                                it = new ItemTemplate(tid, name);
                                itemCache.put(tid, it);
                            }
                            int qty = ((Long) item.get("quantity")).intValue();
                            int rate = ((Long) item.get("rate")).intValue();
                            RewardItem ri = new RewardItem(it, qty, rate);

                            if (item.containsKey("options")) {
                                JSONArray opts = (JSONArray) item.get("options");
                                for (Object optO : opts) {
                                    JSONObject optJ = (JSONObject) optO;
                                    ri.options.add(new ItemOption(((Long) optJ.get("id")).intValue(),
                                            ((Long) optJ.get("param")).intValue()));
                                }
                            }
                            currentRewardItems.add(ri);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private JPanel createRewardContentPanel() {
        JPanel main = new JPanel(new BorderLayout(5, 5));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JPanel rt = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        rt.add(chkCheckTask = new JCheckBox("Check hoàn thành nhiệm vụ"));
        rt.add(new JLabel("Điểm Boss ngày:"));
        rt.add(txtPointBossDay = new JTextField("0", 5));

        // Fill initial header data
        if (boss.rewardConfig != null && !boss.rewardConfig.isEmpty()) {
            JSONObject rj = (JSONObject) JSONValue.parse(boss.rewardConfig);
            if (rj != null) {
                if (rj.containsKey("checkTask"))
                    chkCheckTask.setSelected((Boolean) rj.get("checkTask"));
                if (rj.containsKey("pointBossDay"))
                    txtPointBossDay.setText(rj.get("pointBossDay").toString());
            }
        }

        main.add(rt, BorderLayout.NORTH);

        // Center Split (List + Options vs Add Panel)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Left Side: List & Options
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Reward Item Table
        rewardItemTableModel = new DefaultTableModel(new Object[] { "ID", "Name", "Quantity" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        rewardItemTable = new JTable(rewardItemTableModel);
        rewardItemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectRewardItem(rewardItemTable.getSelectedRow());
            }
        });
        JScrollPane scrollItems = new JScrollPane(rewardItemTable);
        scrollItems.setBorder(BorderFactory.createTitledBorder("Danh sách Item"));
        leftSplit.setTopComponent(scrollItems);

        // Item Options Section
        JPanel optPanel = new JPanel(new BorderLayout());
        optPanel.setBorder(BorderFactory.createTitledBorder("Item option"));
        JPanel optTop = new JPanel(new GridBagLayout());
        GridBagConstraints gbcO = new GridBagConstraints();
        gbcO.insets = new Insets(2, 2, 2, 2);
        gbcO.fill = GridBagConstraints.HORIZONTAL;

        cbRewardOpt = new JComboBox<>();
        cbRewardOpt.setPreferredSize(new Dimension(250, 25)); // Limit width
        for (ItemOptionTemplate t : optionTemplates)
            cbRewardOpt.addItem(t);

        txtRewardOptParam = new JTextField("0", 5);
        JButton btnAddOpt = new JButton("Add Opt");
        JButton btnDelOpt = new JButton("Del Opt");
        styleBtn(btnAddOpt, new Color(119, 119, 119));
        styleBtn(btnDelOpt, new Color(119, 119, 119));
        btnAddOpt.addActionListener(e -> addRewardOption());
        btnDelOpt.addActionListener(e -> deleteRewardOption());

        gbcO.gridx = 0;
        gbcO.weightx = 1.0;
        optTop.add(cbRewardOpt, gbcO);
        gbcO.gridx = 1;
        gbcO.weightx = 0.2;
        optTop.add(txtRewardOptParam, gbcO);
        gbcO.gridx = 2;
        gbcO.weightx = 0;
        optTop.add(btnAddOpt, gbcO);
        gbcO.gridx = 3;
        gbcO.weightx = 0;
        optTop.add(btnDelOpt, gbcO);

        modelRewardOpts = new DefaultTableModel(new Object[] { "ID", "Option", "Param" }, 0);
        tblRewardOpts = new JTable(modelRewardOpts);
        optPanel.add(optTop, BorderLayout.NORTH);
        optPanel.add(new JScrollPane(tblRewardOpts), BorderLayout.CENTER);
        leftSplit.setBottomComponent(optPanel);
        leftSplit.setDividerLocation(300);

        // Right Side: Add Panel
        JPanel addPanel = new JPanel(new GridBagLayout());
        addPanel.setBorder(BorderFactory.createTitledBorder("Thêm Item"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        gbc.gridy = 0;
        addPanel.add(new JLabel("Find Item:"), gbc);
        gbc.gridy = 1;
        JPanel searchP = new JPanel(new BorderLayout());
        txtRewardFindItem = new JTextField();
        JButton btnFind = new JButton("Find");
        btnFind.addActionListener(e -> searchRewardTemplate());
        searchP.add(txtRewardFindItem, BorderLayout.CENTER);
        searchP.add(btnFind, BorderLayout.EAST);
        addPanel.add(searchP, gbc);

        gbc.gridy = 2;
        addPanel.add(new JLabel("Select Item:"), gbc);
        gbc.gridy = 3;
        addPanel.add(cbRewardItemTemplate = new JComboBox<>(), gbc);
        gbc.gridy = 4;
        addPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridy = 5;
        addPanel.add(txtRewardQuantity = new JTextField("1"), gbc);
        gbc.gridy = 6;
        addPanel.add(new JLabel("Rate (0-100):"), gbc);
        gbc.gridy = 7;
        addPanel.add(txtRewardRate = new JTextField("10"), gbc);

        gbc.gridy = 8;
        JButton btnAdd = new JButton("Add Item");
        btnAdd.setBackground(new Color(50, 205, 50));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAdd.addActionListener(e -> addRewardItem());
        addPanel.add(btnAdd, gbc);

        gbc.gridy = 9;
        gbc.weighty = 1.0;
        addPanel.add(new JPanel(), gbc); // Spacer

        mainSplit.setLeftComponent(leftSplit);
        mainSplit.setRightComponent(addPanel);
        mainSplit.setDividerLocation(650);
        main.add(mainSplit, BorderLayout.CENTER);

        JButton btnDeleteSelected = new JButton("Xóa Item đã chọn");
        btnDeleteSelected.setBackground(new Color(255, 99, 71));
        btnDeleteSelected.setForeground(Color.WHITE);
        btnDeleteSelected.addActionListener(e -> deleteSelectedRewardItem());
        main.add(btnDeleteSelected, BorderLayout.SOUTH);

        refreshRewardUI();

        return main;
    }

    private void refreshRewardUI() {
        rewardItemTableModel.setRowCount(0);
        for (RewardItem ri : currentRewardItems) {
            rewardItemTableModel.addRow(new Object[] { ri.template.id, ri.template.name, ri.quantity });
        }
    }

    private String generateRewardJson() {
        JSONObject rj = new JSONObject();
        rj.put("checkTask", chkCheckTask.isSelected());
        rj.put("pointBossDay", Integer.parseInt(txtPointBossDay.getText()));
        JSONArray ria = new JSONArray();
        for (RewardItem item : currentRewardItems) {
            JSONObject ri = new JSONObject();
            ri.put("id", item.template.id);
            ri.put("quantity", item.quantity);
            ri.put("rate", item.rate);
            JSONArray opts = new JSONArray();
            for (ItemOption o : item.options) {
                JSONObject optJ = new JSONObject();
                optJ.put("id", o.id);
                optJ.put("param", o.param);
                opts.add(optJ);
            }
            ri.put("options", opts);
            ria.add(ri);
        }
        rj.put("items", ria);
        return rj.toJSONString();
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
                    for (ItemTemplate t : get())
                        cbRewardItemTemplate.addItem(t);
                } catch (Exception e) {
                }
            }
        }.execute();
    }

    private void addRewardItem() {
        ItemTemplate t = (ItemTemplate) cbRewardItemTemplate.getSelectedItem();
        if (t == null)
            return;
        try {
            int q = Integer.parseInt(txtRewardQuantity.getText());
            int r = Integer.parseInt(txtRewardRate.getText());
            RewardItem ri = new RewardItem(t, q, r);
            currentRewardItems.add(ri);
            rewardItemTableModel.addRow(new Object[] { t.id, t.name, q });
            int lastRow = rewardItemTable.getRowCount() - 1;
            rewardItemTable.setRowSelectionInterval(lastRow, lastRow);
            rewardItemTable.scrollRectToVisible(rewardItemTable.getCellRect(lastRow, 0, true));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Số lượng/Tỷ lệ không hợp lệ!");
        }
    }

    private void selectRewardItem(int row) {
        selectedRewardItemIdx = row;
        modelRewardOpts.setRowCount(0);
        if (row >= 0 && row < currentRewardItems.size()) {
            RewardItem ri = currentRewardItems.get(row);
            for (ItemOption o : ri.options) {
                modelRewardOpts.addRow(new Object[] { o.id, optionNameMap.getOrDefault(o.id, "Option " + o.id), o.param });
            }
        }
    }


    private void addRewardOption() {
        if (selectedRewardItemIdx < 0)
            return;
        ItemOptionTemplate t = (ItemOptionTemplate) cbRewardOpt.getSelectedItem();
        if (t == null)
            return;
        try {
            int p = Integer.parseInt(txtRewardOptParam.getText());
            currentRewardItems.get(selectedRewardItemIdx).options.add(new ItemOption(t.id, p));
            selectRewardItem(selectedRewardItemIdx);
        } catch (Exception e) {
        }
    }

    private void deleteRewardOption() {
        if (selectedRewardItemIdx < 0)
            return;
        int row = tblRewardOpts.getSelectedRow();
        if (row >= 0) {
            currentRewardItems.get(selectedRewardItemIdx).options.remove(row);
            selectRewardItem(selectedRewardItemIdx);
        }
    }

    private void deleteSelectedRewardItem() {
        if (selectedRewardItemIdx >= 0 && selectedRewardItemIdx < currentRewardItems.size()) {
            currentRewardItems.remove(selectedRewardItemIdx);
            rewardItemTableModel.removeRow(selectedRewardItemIdx);
            selectedRewardItemIdx = -1;
            modelRewardOpts.setRowCount(0);
        }
    }

    private void styleBtn(JButton b, Color c) {
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setPreferredSize(new Dimension(100, 30));
    }
}

class RewardItem {
    ItemTemplate template;
    int quantity;
    int rate;
    List<ItemOption> options = new ArrayList<>();

    RewardItem(ItemTemplate t, int q, int r) {
        this.template = t;
        this.quantity = q;
        this.rate = r;
    }

    @Override
    public String toString() {
        return template.toString();
    }
}
