package com.girlkun.tool.screens.mob_reward;

import com.girlkun.tool.shopmanager.models.ItemTemplate;
import com.girlkun.tool.shopmanager.models.ItemOptionTemplate;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO.MobTemplate;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO.MapTemplate;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình quản lý Mob Reward (Vật phẩm rơi từ quái)
 */
public class MobRewardScr extends JInternalFrame {

    private MobRewardDAO dao;
    private ShopManagerDAO shopDao;

    // Data
    private List<MobRewardModel> rewardList = new ArrayList<>();
    private MobRewardModel selectedReward;
    private List<MobRewardModel.ItemOption> currentOptions = new ArrayList<>();

    // UI Components - Filter
    private JTextField txtSearch;
    private JComboBox<String> cbEventFilter;
    private JComboBox<String> cbMapTypeFilter;
    private JButton btnSearch;
    private JButton btnRefresh;

    // UI Components - Table
    private JTable tblRewards;
    private DefaultTableModel modelRewards;

    // UI Components - Form
    private JComboBox<MobTemplate> cbMobId;
    private JComboBox<MapTemplate> cbMapId;
    private JTextField txtItemId;
    private JTextField txtRate;
    private JTextField txtQtyMin;
    private JTextField txtQtyMax;
    private JComboBox<String> cbGender;
    private JComboBox<String> cbEventKey;
    private JComboBox<String> cbMapType;
    private JComboBox<String> cbConditionType;
    private JCheckBox chkRandomRange;
    private JTextField txtRandomRange;
    private JCheckBox chkNotifyGlobal;
    private JTextArea txtDescription;
    private JCheckBox chkActive;

    // Options table
    private JTable tblOptions;
    private DefaultTableModel modelOptions;

    // Item search
    private JTextField txtFindItem;
    private JComboBox<ItemTemplate> cbItemTemplate;

    // Status
    private JLabel lblStatus;
    private JLabel lblCount;
    private JLabel lblOptionsCount;
    private JLabel lblRatePercent;

    public MobRewardScr() {
        super("Mob Reward Manager - Quản lý vật phẩm rơi", true, true, true, true);
        this.setSize(1400, 900);
        this.setFrameIcon(new ImageIcon("icon.png"));

        dao = MobRewardDAO.gI();
        shopDao = ShopManagerDAO.gI();

        initComponents();
        loadData();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top - Filter Panel
        mainPanel.add(createFilterPanel(), BorderLayout.NORTH);

        // Center - Split Pane (Table + Form)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createTablePanel());
        splitPane.setRightComponent(createFormPanel());
        splitPane.setDividerLocation(650);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Bottom - Status
        mainPanel.add(createStatusPanel(), BorderLayout.SOUTH);

        this.add(mainPanel);
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Bộ lọc"));

        txtSearch = new JTextField(20);
        cbEventFilter = new JComboBox<>();
        cbMapTypeFilter = new JComboBox<>();
        btnSearch = new JButton("Tìm kiếm");
        btnRefresh = new JButton("Làm mới");

        styleBtn(btnSearch, new Color(0, 123, 255));
        styleBtn(btnRefresh, new Color(40, 167, 69));

        // Load filter options
        for (String s : dao.getEventKeys()) {
            cbEventFilter.addItem(s);
        }
        for (String s : dao.getMapTypes()) {
            cbMapTypeFilter.addItem(s);
        }

        btnSearch.addActionListener(e -> search());
        btnRefresh.addActionListener(e -> loadData());

        panel.add(new JLabel("Tìm kiếm:"));
        panel.add(txtSearch);
        panel.add(new JLabel("Event:"));
        panel.add(cbEventFilter);
        panel.add(new JLabel("Map Type:"));
        panel.add(cbMapTypeFilter);
        panel.add(btnSearch);
        panel.add(btnRefresh);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Danh sách Mob Reward"));

        String[] columns = { "ID", "Item", "Rate (%)", "Mob", "Map Type", "Event", "Active" };
        modelRewards = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 6)
                    return Boolean.class;
                return Object.class;
            }
        };

        tblRewards = new JTable(modelRewards);
        tblRewards.setRowHeight(28);
        tblRewards.setGridColor(Color.LIGHT_GRAY);
        tblRewards.setSelectionBackground(new Color(51, 153, 255));
        tblRewards.setSelectionForeground(Color.WHITE);
        tblRewards.setAutoCreateRowSorter(true);

        // Column widths
        tblRewards.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblRewards.getColumnModel().getColumn(1).setPreferredWidth(180);
        tblRewards.getColumnModel().getColumn(2).setPreferredWidth(60);
        tblRewards.getColumnModel().getColumn(3).setPreferredWidth(80);
        tblRewards.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblRewards.getColumnModel().getColumn(5).setPreferredWidth(80);
        tblRewards.getColumnModel().getColumn(6).setPreferredWidth(50);

        tblRewards.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = tblRewards.getSelectedRow();
                if (viewRow >= 0) {
                    int modelRow = tblRewards.convertRowIndexToModel(viewRow);
                    if (modelRow >= 0 && modelRow < rewardList.size()) {
                        selectReward(rewardList.get(modelRow));
                    }
                }
            }
        });

        panel.add(new JScrollPane(tblRewards), BorderLayout.CENTER);

        // Count label
        lblCount = new JLabel("Tổng: 0");
        lblCount.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(lblCount, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Chi tiết Mob Reward"));

        // Just use form fields directly, no split pane
        panel.add(createFormFields(), BorderLayout.CENTER);

        return panel;
    }

    private JScrollPane createFormFields() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Item search section
        txtFindItem = new JTextField(10);
        cbItemTemplate = new JComboBox<>();
        cbItemTemplate.setPreferredSize(new Dimension(220, 28));
        JButton btnSearchItem = new JButton("Tìm");
        styleBtn(btnSearchItem, new Color(23, 162, 184));
        btnSearchItem.setPreferredSize(new Dimension(60, 28));
        btnSearchItem.addActionListener(e -> searchItem());

        JPanel itemSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        itemSearchPanel.add(txtFindItem);
        itemSearchPanel.add(btnSearchItem);

        addFormRow(form, gbc, row++, "Tìm Item:", itemSearchPanel);
        addFormRow(form, gbc, row++, "Chọn Item:", cbItemTemplate);

        form.add(new JSeparator(), createSeparatorGbc(row++));

        // Main fields - Mob and Map combo boxes
        cbMobId = new JComboBox<>();
        cbMobId.setPreferredSize(new Dimension(220, 28));
        for (MobTemplate mob : shopDao.getMobTemplates()) {
            cbMobId.addItem(mob);
        }

        cbMapId = new JComboBox<>();
        cbMapId.setPreferredSize(new Dimension(220, 28));
        for (MapTemplate map : shopDao.getMapTemplates()) {
            cbMapId.addItem(map);
        }

        txtItemId = new JTextField(10);
        txtRate = new JTextField(10);
        txtQtyMin = new JTextField(5);
        txtQtyMax = new JTextField(5);
        cbGender = new JComboBox<>(new String[] { "Tất cả (-1)", "Trái Đất (0)", "Namếc (1)", "Xayda (2)" });
        cbEventKey = new JComboBox<>();
        cbMapType = new JComboBox<>();
        cbConditionType = new JComboBox<>();
        chkRandomRange = new JCheckBox();
        txtRandomRange = new JTextField(5);
        chkNotifyGlobal = new JCheckBox();
        txtDescription = new JTextArea(2, 20);
        txtDescription.setLineWrap(true);
        chkActive = new JCheckBox("Kích hoạt", true);

        // Load combo data
        for (String s : dao.getEventKeys()) {
            if (!s.equals("Tất cả"))
                cbEventKey.addItem(s);
        }
        for (String s : dao.getMapTypes()) {
            if (!s.equals("Tất cả"))
                cbMapType.addItem(s);
        }
        for (String s : dao.getConditionTypes()) {
            cbConditionType.addItem(s);
        }

        // Item combo listener
        cbItemTemplate.addActionListener(e -> {
            ItemTemplate it = (ItemTemplate) cbItemTemplate.getSelectedItem();
            if (it != null) {
                txtItemId.setText(String.valueOf(it.id));
            }
        });

        addFormRow(form, gbc, row++, "Mob:", cbMobId);
        addFormRow(form, gbc, row++, "Map:", cbMapId);
        addFormRow(form, gbc, row++, "Item Template ID:", txtItemId);

        // Rate with percentage label
        lblRatePercent = new JLabel("= 1.00%");
        lblRatePercent.setForeground(new Color(40, 167, 69));
        lblRatePercent.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JPanel ratePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        ratePanel.add(txtRate);
        ratePanel.add(lblRatePercent);

        // Update percentage when rate changes
        txtRate.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateRatePercent();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateRatePercent();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateRatePercent();
            }
        });

        addFormRow(form, gbc, row++, "Tỉ lệ (1/rate):", ratePanel);

        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        qtyPanel.add(new JLabel("Min:"));
        qtyPanel.add(txtQtyMin);
        qtyPanel.add(new JLabel("Max:"));
        qtyPanel.add(txtQtyMax);
        addFormRow(form, gbc, row++, "Số lượng:", qtyPanel);

        addFormRow(form, gbc, row++, "Giới tính:", cbGender);
        addFormRow(form, gbc, row++, "Event Key:", cbEventKey);
        addFormRow(form, gbc, row++, "Map Type:", cbMapType);
        addFormRow(form, gbc, row++, "Condition:", cbConditionType);

        // Options button
        lblOptionsCount = new JLabel("0 options");
        lblOptionsCount.setForeground(new Color(100, 100, 100));
        JButton btnManageOptions = new JButton("Quản lý Options");
        styleBtn(btnManageOptions, new Color(156, 39, 176)); // Purple
        btnManageOptions.setPreferredSize(new Dimension(140, 28));
        btnManageOptions.addActionListener(e -> showOptionsDialog());

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        optionsPanel.add(btnManageOptions);
        optionsPanel.add(lblOptionsCount);
        addFormRow(form, gbc, row++, "Item Options:", optionsPanel);

        JPanel randomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        randomPanel.add(chkRandomRange);
        randomPanel.add(new JLabel("Range:"));
        randomPanel.add(txtRandomRange);
        addFormRow(form, gbc, row++, "Random Range:", randomPanel);

        addFormRow(form, gbc, row++, "Notify Global:", chkNotifyGlobal);
        addFormRow(form, gbc, row++, "Mô tả:", new JScrollPane(txtDescription));
        addFormRow(form, gbc, row++, "", chkActive);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        JButton btnAdd = new JButton("Thêm");
        JButton btnUpdate = new JButton("Cập nhật");
        JButton btnDelete = new JButton("Xóa");
        JButton btnClear = new JButton("Làm mới");
        JButton btnToggle = new JButton("Bật/Tắt");

        styleBtn(btnAdd, new Color(40, 167, 69));
        styleBtn(btnUpdate, new Color(255, 193, 7));
        btnUpdate.setForeground(Color.BLACK);
        styleBtn(btnDelete, new Color(220, 53, 69));
        styleBtn(btnClear, new Color(108, 117, 125));
        styleBtn(btnToggle, new Color(23, 162, 184));

        btnAdd.setPreferredSize(new Dimension(80, 28));
        btnUpdate.setPreferredSize(new Dimension(80, 28));
        btnDelete.setPreferredSize(new Dimension(70, 28));
        btnClear.setPreferredSize(new Dimension(80, 28));
        btnToggle.setPreferredSize(new Dimension(80, 28));

        btnAdd.addActionListener(e -> addReward());
        btnUpdate.addActionListener(e -> updateReward());
        btnDelete.addActionListener(e -> deleteReward());
        btnClear.addActionListener(e -> clearForm());
        btnToggle.addActionListener(e -> toggleActive());

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);
        btnPanel.add(btnToggle);

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        form.add(btnPanel, gbc);

        JScrollPane scrollForm = new JScrollPane(form);
        scrollForm.setBorder(null);
        return scrollForm;
    }

    private void showOptionsDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Danh sách Chỉ Số (Options)",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(600, 450);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton btnAddOption = new JButton("Thêm dòng");
        JButton btnSearchOption = new JButton("Tra cứu Option");
        JButton btnDeleteOption = new JButton("Xóa dòng");

        styleBtn(btnAddOption, new Color(40, 167, 69));
        styleBtn(btnSearchOption, new Color(23, 162, 184));
        styleBtn(btnDeleteOption, new Color(220, 53, 69));

        btnAddOption.addActionListener(e -> addOptionRow());
        btnSearchOption.addActionListener(e -> showOptionSearchDialog());
        btnDeleteOption.addActionListener(e -> deleteOptionRow());

        btnPanel.add(btnAddOption);
        btnPanel.add(btnSearchOption);
        btnPanel.add(btnDeleteOption);

        // Options table - editable
        String[] optCols = { "Option ID", "Chỉ số (Param)", "Mô tả tự động" };
        if (modelOptions == null) {
            modelOptions = new DefaultTableModel(optCols, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return col == 0 || col == 1; // ID and Param editable
                }
            };
        }

        tblOptions = new JTable(modelOptions);
        tblOptions.setRowHeight(28);
        tblOptions.setGridColor(Color.LIGHT_GRAY);
        tblOptions.setSelectionBackground(new Color(51, 153, 255));
        tblOptions.setSelectionForeground(Color.WHITE);
        tblOptions.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        tblOptions.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblOptions.getColumnModel().getColumn(1).setPreferredWidth(100);
        tblOptions.getColumnModel().getColumn(2).setPreferredWidth(250);

        // Update description when ID changes
        modelOptions.addTableModelListener(e -> {
            if (e.getColumn() == 0) {
                int row = e.getFirstRow();
                try {
                    int optId = Integer.parseInt(modelOptions.getValueAt(row, 0).toString());
                    String name = shopDao.getOptionName(optId);
                    modelOptions.setValueAt(name, row, 2);
                } catch (Exception ex) {
                    // Ignore
                }
            }
        });

        mainPanel.add(btnPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(tblOptions), BorderLayout.CENTER);

        // Bottom panel with OK button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOK = new JButton("Xác nhận");
        styleBtn(btnOK, new Color(40, 167, 69));
        btnOK.setPreferredSize(new Dimension(100, 32));
        btnOK.addActionListener(e -> {
            updateOptionsCount();
            dialog.dispose();
        });
        bottomPanel.add(btnOK);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void updateOptionsCount() {
        int count = modelOptions != null ? modelOptions.getRowCount() : 0;
        lblOptionsCount.setText(count + " options");
        if (count > 0) {
            lblOptionsCount.setForeground(new Color(40, 167, 69));
        } else {
            lblOptionsCount.setForeground(new Color(100, 100, 100));
        }
    }

    private void addOptionRow() {
        if (modelOptions == null) {
            String[] optCols = { "Option ID", "Chỉ số (Param)", "Mô tả tự động" };
            modelOptions = new DefaultTableModel(optCols, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return col == 0 || col == 1;
                }
            };
        }
        modelOptions.addRow(new Object[] { 0, 0, "" });
        updateOptionsCount();
    }

    private void deleteOptionRow() {
        int selectedRow = tblOptions.getSelectedRow();
        if (selectedRow >= 0) {
            modelOptions.removeRow(selectedRow);
            updateOptionsCount();
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dòng để xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showOptionSearchDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Tìm kiếm Option",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(550, 550);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(45, 45, 48));

        // Search panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBackground(new Color(45, 45, 48));
        JLabel lblSearch = new JLabel("Tìm kiếm: ");
        lblSearch.setForeground(Color.WHITE);
        JTextField txtSearchOpt = new JTextField();
        txtSearchOpt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearchOpt.setPreferredSize(new Dimension(0, 30));
        searchPanel.add(lblSearch, BorderLayout.WEST);
        searchPanel.add(txtSearchOpt, BorderLayout.CENTER);
        mainPanel.add(searchPanel, BorderLayout.NORTH);

        // Options table - only 2 columns now
        String[] cols = { "ID", "Tên Option" };
        DefaultTableModel modelSearch = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable tblSearch = new JTable(modelSearch);
        tblSearch.setRowHeight(28);
        tblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblSearch.setGridColor(new Color(70, 70, 75));
        tblSearch.setBackground(new Color(60, 60, 65));
        tblSearch.setForeground(Color.WHITE);
        tblSearch.setSelectionBackground(new Color(0, 122, 204));
        tblSearch.setSelectionForeground(Color.WHITE);
        tblSearch.getTableHeader().setBackground(new Color(45, 45, 48));
        tblSearch.getTableHeader().setForeground(new Color(200, 200, 200));
        tblSearch.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        tblSearch.getColumnModel().getColumn(0).setPreferredWidth(60);
        tblSearch.getColumnModel().getColumn(0).setMaxWidth(80);
        tblSearch.getColumnModel().getColumn(1).setPreferredWidth(400);

        // Set alternating row colors
        tblSearch.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? new Color(60, 60, 65) : new Color(50, 50, 55));
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        });

        // Load all options
        List<ItemOptionTemplate> allOptions = shopDao.getItemOptionTemplates();
        for (ItemOptionTemplate opt : allOptions) {
            modelSearch.addRow(new Object[] { opt.id, opt.name });
        }

        // Filter on key typed
        txtSearchOpt.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                String filter = txtSearchOpt.getText().toLowerCase();
                modelSearch.setRowCount(0);
                for (ItemOptionTemplate opt : allOptions) {
                    if (opt.name.toLowerCase().contains(filter) || String.valueOf(opt.id).contains(filter)) {
                        modelSearch.addRow(new Object[] { opt.id, opt.name });
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblSearch);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 75)));
        scrollPane.getViewport().setBackground(new Color(60, 60, 65));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with Add button and info
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBackground(new Color(45, 45, 48));

        JButton btnAdd = new JButton("Thêm Option đã chọn");
        btnAdd.setBackground(new Color(40, 167, 69));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAdd.setPreferredSize(new Dimension(180, 35));
        btnAdd.addActionListener(e -> {
            int row = tblSearch.getSelectedRow();
            if (row >= 0) {
                int optId = (int) modelSearch.getValueAt(row, 0);
                String optName = (String) modelSearch.getValueAt(row, 1);

                // Add to options table
                modelOptions.addRow(new Object[] { optId, 0, optName });
                setStatus("Đã thêm option: " + optName, new Color(40, 167, 69));
            } else {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn option để thêm!", "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        // Double-click to add
        tblSearch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tblSearch.getSelectedRow();
                    if (row >= 0) {
                        int optId = (int) modelSearch.getValueAt(row, 0);
                        String optName = (String) modelSearch.getValueAt(row, 1);
                        modelOptions.addRow(new Object[] { optId, 0, optName });
                        setStatus("Đã thêm option: " + optName, new Color(40, 167, 69));
                    }
                }
            }
        });

        JLabel lblInfo = new JLabel("Chọn option rồi nhấn nút hoặc double-click để thêm");
        lblInfo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblInfo.setForeground(new Color(150, 150, 150));

        bottomPanel.add(btnAdd, BorderLayout.WEST);
        bottomPanel.add(lblInfo, BorderLayout.CENTER);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private String buildOptionsJson() {
        if (modelOptions.getRowCount() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < modelOptions.getRowCount(); i++) {
            if (i > 0)
                sb.append(",");
            int id = 0;
            int param = 0;
            try {
                id = Integer.parseInt(modelOptions.getValueAt(i, 0).toString());
                param = Integer.parseInt(modelOptions.getValueAt(i, 1).toString());
            } catch (Exception e) {
                // Ignore
            }
            sb.append("{\"id\":").append(id).append(",\"param\":").append(param).append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private void loadOptionsFromJson(String json) {
        if (modelOptions == null) {
            String[] optCols = { "Option ID", "Chỉ số (Param)", "Mô tả tự động" };
            modelOptions = new DefaultTableModel(optCols, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return col == 0 || col == 1;
                }
            };
        }
        modelOptions.setRowCount(0);
        currentOptions.clear();

        if (json == null || json.isEmpty()) {
            updateOptionsCount();
            return;
        }

        // Parse options using model's parseOptions
        MobRewardModel temp = new MobRewardModel();
        temp.optionsJson = json;
        List<MobRewardModel.ItemOption> options = temp.parseOptions();

        for (MobRewardModel.ItemOption opt : options) {
            String name = shopDao.getOptionName(opt.id);
            modelOptions.addRow(new Object[] { opt.id, opt.param, name });
            currentOptions.add(opt);
        }
        updateOptionsCount();
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblStatus = new JLabel("Sẵn sàng");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        panel.add(lblStatus);
        return panel;
    }

    private void addFormRow(JPanel p, GridBagConstraints gbc, int row, String label, Component cmp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        p.add(cmp, gbc);
    }

    private GridBagConstraints createSeparatorGbc(int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);
        return gbc;
    }

    private void updateRatePercent() {
        try {
            int rate = Integer.parseInt(txtRate.getText().trim());
            if (rate > 0) {
                double percent = 100.0 / rate;
                lblRatePercent.setText(String.format("= %.2f%%", percent));
                lblRatePercent.setForeground(new Color(40, 167, 69)); // Green
            } else {
                lblRatePercent.setText("= N/A");
                lblRatePercent.setForeground(Color.RED);
            }
        } catch (NumberFormatException e) {
            lblRatePercent.setText("= ?");
            lblRatePercent.setForeground(Color.GRAY);
        }
    }

    private void styleBtn(JButton btn, Color c) {
        btn.setBackground(c);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(100, 30));
    }

    // === DATA OPERATIONS ===

    private void loadData() {
        new Thread(() -> {
            rewardList = dao.getAll();
            SwingUtilities.invokeLater(() -> {
                refreshTable();
                setStatus("Đã tải " + rewardList.size() + " bản ghi", new Color(40, 167, 69));
            });
        }).start();
    }

    private void search() {
        String keyword = txtSearch.getText().trim();
        String eventKey = (String) cbEventFilter.getSelectedItem();
        String mapType = (String) cbMapTypeFilter.getSelectedItem();

        new Thread(() -> {
            rewardList = dao.search(keyword, eventKey, mapType);
            SwingUtilities.invokeLater(() -> {
                refreshTable();
                setStatus("Tìm thấy " + rewardList.size() + " bản ghi", new Color(23, 162, 184));
            });
        }).start();
    }

    private void refreshTable() {
        modelRewards.setRowCount(0);
        for (MobRewardModel m : rewardList) {
            String itemDisplay = m.itemName.isEmpty() ? String.valueOf(m.itemTemplateId)
                    : m.itemName + " (" + m.itemTemplateId + ")";
            String mobDisplay = m.mobId == -1 ? "Tất cả" : (m.mobName.isEmpty() ? String.valueOf(m.mobId) : m.mobName);

            // Calculate rate as percentage: (1/rate) * 100
            double ratePercent = m.rate > 0 ? (100.0 / m.rate) : 0;
            String rateDisplay = String.format("%.2f%%", ratePercent);

            modelRewards.addRow(new Object[] {
                    m.id,
                    itemDisplay,
                    rateDisplay,
                    mobDisplay,
                    m.mapType == null ? "" : m.mapType,
                    m.eventKey == null ? "" : m.eventKey,
                    m.isActive
            });
        }
        lblCount.setText("Tổng: " + rewardList.size());
    }

    private void selectComboByMobId(int mobId) {
        for (int i = 0; i < cbMobId.getItemCount(); i++) {
            MobTemplate mob = cbMobId.getItemAt(i);
            if (mob.id == mobId) {
                cbMobId.setSelectedIndex(i);
                return;
            }
        }
        cbMobId.setSelectedIndex(0); // Default to "Tất cả quái"
    }

    private void selectComboByMapId(int mapId) {
        for (int i = 0; i < cbMapId.getItemCount(); i++) {
            MapTemplate map = cbMapId.getItemAt(i);
            if (map.id == mapId) {
                cbMapId.setSelectedIndex(i);
                return;
            }
        }
        cbMapId.setSelectedIndex(0); // Default to "Tất cả map"
    }

    private void selectReward(MobRewardModel m) {
        selectedReward = m;

        // Select Mob in combo
        selectComboByMobId(m.mobId);
        // Select Map in combo
        selectComboByMapId(m.mapId);

        txtItemId.setText(String.valueOf(m.itemTemplateId));
        txtRate.setText(String.valueOf(m.rate));
        txtQtyMin.setText(String.valueOf(m.quantityMin));
        txtQtyMax.setText(String.valueOf(m.quantityMax));

        // Gender
        cbGender.setSelectedIndex(m.gender + 1);

        // Event key
        cbEventKey.setSelectedItem(m.eventKey == null ? "" : m.eventKey);

        // Map type
        cbMapType.setSelectedItem(m.mapType == null ? "" : m.mapType);

        // Condition type
        cbConditionType.setSelectedItem(m.conditionType == null ? "" : m.conditionType);

        // Load options from JSON
        loadOptionsFromJson(m.optionsJson);

        chkRandomRange.setSelected(m.isRandomRange);
        txtRandomRange.setText(String.valueOf(m.randomRange));
        chkNotifyGlobal.setSelected(m.notifyGlobal);
        txtDescription.setText(m.description == null ? "" : m.description);
        chkActive.setSelected(m.isActive);

        setStatus("Đã chọn: " + m.toString(), Color.BLUE);
    }

    private void clearForm() {
        selectedReward = null;
        cbMobId.setSelectedIndex(0); // "Tất cả quái"
        cbMapId.setSelectedIndex(0); // "Tất cả map"
        txtItemId.setText("");
        txtRate.setText("100");
        txtQtyMin.setText("1");
        txtQtyMax.setText("1");
        cbGender.setSelectedIndex(0);
        cbEventKey.setSelectedIndex(0);
        cbMapType.setSelectedIndex(0);
        cbConditionType.setSelectedIndex(0);
        if (modelOptions != null) {
            modelOptions.setRowCount(0);
        }
        currentOptions.clear();
        updateOptionsCount();
        chkRandomRange.setSelected(false);
        txtRandomRange.setText("0");
        chkNotifyGlobal.setSelected(false);
        txtDescription.setText("");
        chkActive.setSelected(true);

        setStatus("Form đã được làm mới", Color.GRAY);
    }

    private MobRewardModel getFormData() {
        MobRewardModel m = selectedReward != null ? selectedReward : new MobRewardModel();

        // Get Mob ID from combo
        MobTemplate selectedMob = (MobTemplate) cbMobId.getSelectedItem();
        m.mobId = selectedMob != null ? selectedMob.id : -1;

        // Get Map ID from combo
        MapTemplate selectedMap = (MapTemplate) cbMapId.getSelectedItem();
        m.mapId = selectedMap != null ? selectedMap.id : -1;

        try {
            m.itemTemplateId = Integer.parseInt(txtItemId.getText().trim());
        } catch (NumberFormatException e) {
            setStatus("Item Template ID không hợp lệ!", Color.RED);
            return null;
        }

        try {
            m.rate = Integer.parseInt(txtRate.getText().trim());
            if (m.rate <= 0)
                m.rate = 1;
        } catch (NumberFormatException e) {
            m.rate = 100;
        }

        try {
            m.quantityMin = Integer.parseInt(txtQtyMin.getText().trim());
            m.quantityMax = Integer.parseInt(txtQtyMax.getText().trim());
        } catch (NumberFormatException e) {
            m.quantityMin = 1;
            m.quantityMax = 1;
        }

        m.gender = cbGender.getSelectedIndex() - 1; // -1, 0, 1, 2
        m.eventKey = (String) cbEventKey.getSelectedItem();
        m.mapType = (String) cbMapType.getSelectedItem();
        m.conditionType = (String) cbConditionType.getSelectedItem();

        // Build options JSON from table
        m.optionsJson = buildOptionsJson();

        m.isRandomRange = chkRandomRange.isSelected();

        try {
            m.randomRange = Integer.parseInt(txtRandomRange.getText().trim());
        } catch (NumberFormatException e) {
            m.randomRange = 0;
        }

        m.notifyGlobal = chkNotifyGlobal.isSelected();
        m.description = txtDescription.getText().trim();
        m.isActive = chkActive.isSelected();

        return m;
    }

    private void addReward() {
        MobRewardModel m = getFormData();
        if (m == null)
            return;

        if (dao.add(m)) {
            loadData();
            clearForm();
            setStatus("Thêm mới thành công!", new Color(40, 167, 69));
            JOptionPane.showMessageDialog(this, "Thêm mới thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        } else {
            setStatus("Thêm mới thất bại!", Color.RED);
            JOptionPane.showMessageDialog(this, "Thêm mới thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateReward() {
        if (selectedReward == null) {
            setStatus("Vui lòng chọn một bản ghi để cập nhật!", Color.RED);
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một bản ghi để cập nhật!", "Cảnh báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        MobRewardModel m = getFormData();
        if (m == null)
            return;

        if (dao.update(m)) {
            loadData();
            setStatus("Cập nhật thành công!", new Color(40, 167, 69));
            JOptionPane.showMessageDialog(this, "Cập nhật thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        } else {
            setStatus("Cập nhật thất bại!", Color.RED);
            JOptionPane.showMessageDialog(this, "Cập nhật thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteReward() {
        if (selectedReward == null) {
            setStatus("Vui lòng chọn một bản ghi để xóa!", Color.RED);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa bản ghi này?\n" + selectedReward.toString(),
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (dao.delete(selectedReward.id)) {
                loadData();
                clearForm();
                setStatus("Xóa thành công!", new Color(40, 167, 69));
            } else {
                setStatus("Xóa thất bại!", Color.RED);
            }
        }
    }

    private void toggleActive() {
        if (selectedReward == null) {
            setStatus("Vui lòng chọn một bản ghi!", Color.RED);
            return;
        }

        boolean newState = !selectedReward.isActive;
        if (dao.toggleActive(selectedReward.id, newState)) {
            selectedReward.isActive = newState;
            chkActive.setSelected(newState);
            loadData();
            setStatus("Đã " + (newState ? "kích hoạt" : "vô hiệu hóa") + " bản ghi!", new Color(23, 162, 184));
        } else {
            setStatus("Thao tác thất bại!", Color.RED);
        }
    }

    private void searchItem() {
        String keyword = txtFindItem.getText().trim();
        List<ItemTemplate> items = shopDao.getItemTemplates(keyword);

        cbItemTemplate.removeAllItems();
        for (ItemTemplate it : items) {
            cbItemTemplate.addItem(it);
        }

        if (items.isEmpty()) {
            setStatus("Không tìm thấy item nào!", Color.ORANGE);
        }
    }

    private void setStatus(String msg, Color color) {
        lblStatus.setText(msg);
        lblStatus.setForeground(color);
    }
}
