package com.girlkun.tool.shopmanager.ui;

import com.girlkun.tool.shopmanager.models.*;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Calendar;

public class ShopManagerScr extends JInternalFrame {

    private final ShopManagerDAO dao;
    private JTabbedPane tabbedPane;

    // UI Components
    // Shop
    private JComboBox<NpcTemplate> cbShopNpc;
    private JTextField txtShopTag;
    private JComboBox<String> cbShopType;
    private JTable tblShops;
    private DefaultTableModel modelShops;
    private List<Shop> shopList;
    private Shop selectedShop;
    private JLabel lblShopStatus;

    // Tab Shop
    private JComboBox<Shop> cbTabShopList;
    private JTextField txtTabName;
    private JTable tblTabs;
    private DefaultTableModel modelTabs;
    private List<TabShop> tabList;
    private TabShop selectedTabShop;
    private JLabel lblTabStatus;

    // Item Shop
    private JComboBox<Shop> cbItemShopList;
    private JComboBox<TabShop> cbItemTabList;
    private JTextField txtFindItem;
    private JComboBox<ItemTemplate> cbItemTemplate;
    private JCheckBox chkNewItem, chkSellItem;
    private JComboBox<TypeSell> cbItemSellType;
    private JTextField txtItemCost;
    private JComboBox<ItemTemplate> cbIconSpec; // Placeholder
    private JTextField txtFindIconSpec;
    private JButton btnSearchIcon;
    private JLabel lblFindIconSpec;
    private JLabel lblSelectIconSpec;
    private JPanel pnlSearchIcon;
    private JTable tblItems;
    private DefaultTableModel modelItems;
    private JLabel lblItemStatus;

    private JComboBox<ItemOptionTemplate> cbItemOption;
    private JTextField txtOptionParam;
    private JTable tblOptions;
    private DefaultTableModel modelOptions;

    private List<DisplayItem> currentDisplayItems;
    private List<ItemData> currentRawItems;
    private int selectedItemIdx = -1;

    // Giftcode
    private JTable tblGiftcodes;
    private DefaultTableModel modelGiftcodes;
    private List<Giftcode> giftcodeList;

    public ShopManagerScr() {
        super("Shop Manager", true, true, true, true);
        this.setSize(1280, 850);
        this.setFrameIcon(new ImageIcon("icon.png")); // Placeholder

        dao = ShopManagerDAO.gI(); // Use singleton
        initComponents();
        loadData();
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Shop", createShopPanel());
        tabbedPane.addTab("Tab shop", createTabShopPanel());
        tabbedPane.addTab("Item shop", createItemShopPanel());
        tabbedPane.addTab("Giftcode", createGiftcodePanel());

        this.add(tabbedPane);
    }

    private void loadData() {
        // Clear and add shop types if not already added
        if (cbShopType.getItemCount() == 0) {
            cbShopType.addItem("Normal");
            cbShopType.addItem("Special");
        }

        // Run Check operations in background thread
        new Thread(() -> {
            boolean isConnected = dao.checkConnection();
            if (!isConnected) {
                // Try force reconnect once if status is unsure, or rely on internal logic
                try {
                    dao.connect();
                    isConnected = true;
                } catch (Exception e) {
                }
            }

            final boolean connectedFinal = dao.checkConnection();

            // Update UI on EDT
            SwingUtilities.invokeLater(() -> {
                if (!connectedFinal) {
                    JOptionPane.showMessageDialog(ShopManagerScr.this,
                            "Chưa kết nối được Database Shop!\nVui lòng bấm 'Cấu hình DB' để kiểm tra lại thông tin kết nối.",
                            "Cảnh báo kết nối", JOptionPane.WARNING_MESSAGE);
                } else {
                    // Update connection success status silently
                    new Thread(() -> {
                        // Load data from DB
                        List<Shop> shops = dao.getShops();
                        List<NpcTemplate> npcs = dao.getNpcs();
                        List<Giftcode> giftcodes = dao.getGiftcodes();
                        List<TypeSell> typeSells = dao.getTypeSells(); // Load Type Sell

                        SwingUtilities.invokeLater(() -> {
                            // Update UI components
                            shopList = shops;
                            modelShops.setRowCount(0);
                            cbTabShopList.removeAllItems();
                            cbItemShopList.removeAllItems();
                            for (Shop s : shopList) {
                                modelShops.addRow(new Object[] { s.id, s.npcId, s.npcName, s.tagName,
                                        getTypeShopName(s.typeShop) });
                                cbTabShopList.addItem(s);
                                cbItemShopList.addItem(s);
                            }

                            cbShopNpc.removeAllItems();
                            for (NpcTemplate npc : npcs) {
                                cbShopNpc.addItem(npc);
                            }

                            cbItemSellType.removeAllItems();
                            for (TypeSell ts : typeSells) {
                                cbItemSellType.addItem(ts);
                            }

                            giftcodeList = giftcodes;
                            modelGiftcodes.setRowCount(0);
                            for (Giftcode g : giftcodeList) {
                                modelGiftcodes.addRow(
                                        new Object[] { g.id, g.code, g.countLeft, g.detail, g.dateCreate, g.expired });
                            }
                        });
                    }).start();
                }
            });
        }).start();
    }

    // Method for external call to reload data
    public void reloadData() {
        loadData();
    }

    // --- Helper method for form rows ---
    private void addFormRow(JPanel p, GridBagConstraints gbc, int row, String label, Component cmp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        p.add(cmp, gbc);
    }

    // --- Helper for Button Style ---
    private void styleBtn(JButton btn, Color c) {
        btn.setBackground(c);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(80, 30));
    }

    // --- Shop Panel ---
    private JPanel createShopPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Left Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setPreferredSize(new Dimension(300, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cbShopNpc = new JComboBox<>();
        txtShopTag = new JTextField();
        cbShopType = new JComboBox<>();

        addFormRow(form, gbc, 0, "NPC:", cbShopNpc);
        addFormRow(form, gbc, 1, "Tag Name:", txtShopTag);
        addFormRow(form, gbc, 2, "Type Shop:", cbShopType);

        JPanel btns = new JPanel(new FlowLayout());
        JButton btnAdd = new JButton("Thêm");
        JButton btnEdit = new JButton("Sửa");
        JButton btnDel = new JButton("Xóa");

        // Colors: Soft Blue, Dark Grey, Soft Red
        styleBtn(btnAdd, new Color(60, 141, 188));
        styleBtn(btnEdit, new Color(119, 119, 119));
        styleBtn(btnDel, new Color(221, 75, 57));

        btnAdd.addActionListener(e -> addShop());
        btnEdit.addActionListener(e -> editShop());
        btnDel.addActionListener(e -> deleteShop());

        btns.add(btnAdd);
        btns.add(btnEdit);
        btns.add(btnDel);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        form.add(btns, gbc);

        lblShopStatus = new JLabel(" ");
        lblShopStatus.setHorizontalAlignment(SwingConstants.CENTER);
        lblShopStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbc.gridy = 4;
        form.add(lblShopStatus, gbc);

        // Right Table
        String[] cols = { "Id", "Npc Id", "Npc Name", "Tag", "Type" };
        modelShops = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tblShops = new JTable(modelShops);
        tblShops.setRowHeight(25);
        tblShops.setGridColor(Color.LIGHT_GRAY);
        tblShops.setSelectionBackground(new Color(51, 153, 255));
        tblShops.setSelectionForeground(Color.WHITE);
        tblShops.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = tblShops.getSelectedRow();
                if (row >= 0 && row < shopList.size()) {
                    selectShop(shopList.get(row));
                }
            }
        });

        p.add(form, BorderLayout.WEST);
        p.add(new JScrollPane(tblShops), BorderLayout.CENTER);
        return p;
    }

    private void refreshShopList() {
        shopList = dao.getShops();
        modelShops.setRowCount(0);

        // Update combos that depend on ShopList
        cbTabShopList.removeAllItems();
        cbItemShopList.removeAllItems();

        for (Shop s : shopList) {
            modelShops.addRow(new Object[] { s.id, s.npcId, s.npcName, s.tagName, getTypeShopName(s.typeShop) });
            cbTabShopList.addItem(s);
            cbItemShopList.addItem(s);
        }
    }

    private void loadNpcCombo() {
        cbShopNpc.removeAllItems();
        for (NpcTemplate npc : dao.getNpcs()) {
            cbShopNpc.addItem(npc);
        }
    }

    private String getTypeShopName(int type) {
        return switch (type) {
            case 0 -> "Normal";
            default -> "Special";
        };
    }

    private int getTypeShopValue(String name) {
        return switch (name) {
            case "Normal" -> 0;
            default -> 3;
        };
    }

    private void selectShop(Shop s) {
        selectedShop = s;
        txtShopTag.setText(s.tagName);
        cbShopType.setSelectedItem(getTypeShopName(s.typeShop));
        for (int i = 0; i < cbShopNpc.getItemCount(); i++) {
            if (cbShopNpc.getItemAt(i).id == s.npcId) {
                cbShopNpc.setSelectedIndex(i);
                break;
            }
        }
    }

    private void addShop() {
        try {
            NpcTemplate npc = (NpcTemplate) cbShopNpc.getSelectedItem();
            if (npc == null) {
                lblShopStatus.setText("Vui lòng chọn NPC!");
                lblShopStatus.setForeground(Color.RED);
                return;
            }
            Shop s = new Shop();
            s.npcId = npc.id;
            s.tagName = txtShopTag.getText();
            s.typeShop = getTypeShopValue((String) cbShopType.getSelectedItem());
            dao.addShop(s);
            refreshShopList();

            lblShopStatus.setText("Thêm shop thành công!");
            lblShopStatus.setForeground(new Color(0, 153, 51));

            // Select the newly added shop (Find by properties with highest ID)
            int bestIdx = -1;
            int maxId = -1;
            for (int i = 0; i < shopList.size(); i++) {
                Shop sh = shopList.get(i);
                if (sh.npcId == s.npcId && sh.tagName.equals(s.tagName) && sh.typeShop == s.typeShop) {
                    if (sh.id > maxId) {
                        maxId = sh.id;
                        bestIdx = i;
                    }
                }
            }
            final int targetRow = bestIdx;
            if (targetRow != -1) {
                SwingUtilities.invokeLater(() -> {
                    tblShops.setRowSelectionInterval(targetRow, targetRow);
                    tblShops.scrollRectToVisible(tblShops.getCellRect(targetRow, 0, true));
                    selectShop(shopList.get(targetRow));
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblShopStatus.setText("Thêm thất bại!");
            lblShopStatus.setForeground(Color.RED);
        }
    }

    private void editShop() {
        if (selectedShop == null) {
            lblShopStatus.setText("Chưa chọn shop để sửa!");
            lblShopStatus.setForeground(Color.RED);
            return;
        }
        try {
            NpcTemplate npc = (NpcTemplate) cbShopNpc.getSelectedItem();
            selectedShop.npcId = npc.id;
            selectedShop.tagName = txtShopTag.getText();
            selectedShop.typeShop = getTypeShopValue((String) cbShopType.getSelectedItem());
            dao.updateShop(selectedShop);

            int savedId = selectedShop.id;
            refreshShopList();

            lblShopStatus.setText("Sửa shop thành công!");
            lblShopStatus.setForeground(new Color(0, 153, 51));

            // Re-select the shop by ID
            int foundIdx = -1;
            for (int i = 0; i < shopList.size(); i++) {
                if (shopList.get(i).id == savedId) {
                    foundIdx = i;
                    break;
                }
            }

            final int targetRow = foundIdx;
            if (targetRow != -1) {
                SwingUtilities.invokeLater(() -> {
                    tblShops.setRowSelectionInterval(targetRow, targetRow);
                    tblShops.scrollRectToVisible(tblShops.getCellRect(targetRow, 0, true));
                    selectShop(shopList.get(targetRow));
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblShopStatus.setText("Sửa thất bại!");
            lblShopStatus.setForeground(Color.RED);
        }
    }

    private void deleteShop() {
        if (selectedShop == null) {
            lblShopStatus.setText("Chưa chọn shop để xóa!");
            lblShopStatus.setForeground(Color.RED);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Xóa shop này?") == JOptionPane.YES_OPTION) {
            try {
                dao.deleteShop(selectedShop.id);
                refreshShopList();
                selectedShop = null;
                txtShopTag.setText("");
                lblShopStatus.setText("Xóa shop thành công!");
                lblShopStatus.setForeground(new Color(0, 153, 51));
            } catch (Exception e) {
                lblShopStatus.setText("Xóa thất bại!");
                lblShopStatus.setForeground(Color.RED);
            }
        }
    }

    // --- Tab Shop Panel ---
    private JPanel createTabShopPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setPreferredSize(new Dimension(300, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cbTabShopList = new JComboBox<>();
        txtTabName = new JTextField();

        cbTabShopList.addActionListener(e -> loadTabsForSelectedShop());

        addFormRow(form, gbc, 0, "Shop:", cbTabShopList);
        addFormRow(form, gbc, 1, "Tab Name:", txtTabName);

        JPanel btns = new JPanel(new FlowLayout());
        JButton btnAdd = new JButton("Thêm");
        JButton btnEdit = new JButton("Sửa");
        JButton btnDel = new JButton("Xóa");

        styleBtn(btnAdd, new Color(60, 141, 188));
        styleBtn(btnEdit, new Color(119, 119, 119));
        styleBtn(btnDel, new Color(221, 75, 57));

        btnAdd.addActionListener(e -> addTab());
        btnEdit.addActionListener(e -> editTab());
        btnDel.addActionListener(e -> deleteTab());

        btns.add(btnEdit);
        btns.add(btnDel);

        lblTabStatus = new JLabel(" ");
        lblTabStatus.setHorizontalAlignment(SwingConstants.CENTER);
        lblTabStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbc.gridy = 3;
        form.add(lblTabStatus, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        form.add(btns, gbc);

        modelTabs = new DefaultTableModel(new String[] { "ID", "Tab Name" }, 0);
        tblTabs = new JTable(modelTabs);
        tblTabs.setRowHeight(25);
        tblTabs.setGridColor(Color.LIGHT_GRAY);
        tblTabs.setSelectionBackground(new Color(51, 153, 255));
        tblTabs.setSelectionForeground(Color.WHITE);
        tblTabs.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = tblTabs.getSelectedRow();
                if (row >= 0 && row < tabList.size()) {
                    selectedTabShop = tabList.get(row);
                    txtTabName.setText(selectedTabShop.tabName);
                }
            }
        });

        p.add(form, BorderLayout.WEST);
        p.add(new JScrollPane(tblTabs), BorderLayout.CENTER);

        return p;
    }

    private void loadTabsForSelectedShop() {
        Shop s = (Shop) cbTabShopList.getSelectedItem();
        if (s == null)
            return;
        tabList = dao.getTabsByShopId(s.id);
        modelTabs.setRowCount(0);
        for (TabShop t : tabList) {
            modelTabs.addRow(new Object[] { t.id, t.tabName });
        }
    }

    private void addTab() {
        Shop s = (Shop) cbTabShopList.getSelectedItem();
        if (s == null) {
            lblTabStatus.setText("Chưa chọn shop!");
            lblTabStatus.setForeground(Color.RED);
            return;
        }
        int count = dao.countTabsByShopId(s.id);
        TabShop t = new TabShop();
        t.shopId = s.id;
        t.tabName = txtTabName.getText();
        dao.addTab(t, count + 1);
        loadTabsForSelectedShop();
        lblTabStatus.setText("Thêm tab thành công!");
        lblTabStatus.setForeground(new Color(0, 153, 51));
    }

    private void editTab() {
        if (selectedTabShop == null) {
            lblTabStatus.setText("Chưa chọn tab!");
            lblTabStatus.setForeground(Color.RED);
            return;
        }
        selectedTabShop.tabName = txtTabName.getText();
        dao.updateTab(selectedTabShop);
        loadTabsForSelectedShop();
        lblTabStatus.setText("Sửa tab thành công!");
        lblTabStatus.setForeground(new Color(0, 153, 51));
    }

    private void deleteTab() {
        if (selectedTabShop == null) {
            lblTabStatus.setText("Chưa chọn tab!");
            lblTabStatus.setForeground(Color.RED);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Xóa tab này?") == JOptionPane.YES_OPTION) {
            dao.deleteTab(selectedTabShop.id);
            loadTabsForSelectedShop();
            selectedTabShop = null;
            lblTabStatus.setText("Xóa tab thành công!");
            lblTabStatus.setForeground(new Color(0, 153, 51));
        }
    }

    // --- Item Shop Panel ---
    private JPanel createItemShopPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        // FORM
        JPanel form = new JPanel(new GridBagLayout());
        form.setPreferredSize(new Dimension(400, 0)); // Increased width
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cbItemShopList = new JComboBox<>();
        cbItemTabList = new JComboBox<>();

        cbItemShopList.addActionListener(e -> loadTabsForItemShop());
        cbItemTabList.addActionListener(e -> loadItemsForTab());

        txtFindItem = new JTextField();
        JButton btnSearch = new JButton("Tìm");
        styleBtn(btnSearch, new Color(0, 192, 239));
        btnSearch.setPreferredSize(new Dimension(60, 25));
        btnSearch.addActionListener(e -> searchItemTemplate());

        cbItemTemplate = new JComboBox<>();
        chkNewItem = new JCheckBox("New Item");
        chkSellItem = new JCheckBox("Sell", true);
        cbItemSellType = new JComboBox<>();
        txtItemCost = new JTextField();

        txtFindIconSpec = new JTextField();
        btnSearchIcon = new JButton("Tìm");
        styleBtn(btnSearchIcon, new Color(0, 192, 239));
        btnSearchIcon.setPreferredSize(new Dimension(60, 25));
        btnSearchIcon.addActionListener(e -> searchIconSpecTemplate());

        cbIconSpec = new JComboBox<>();

        // Add listener to disable/enable inputs when type sell is specific
        cbItemSellType.addActionListener(e -> updateIconSpecInputState());

        // init templates
        searchItemTemplate();
        searchIconSpecTemplate();

        // Row 0, 1
        gbc.gridwidth = 1;
        addFormRow(form, gbc, 0, "Shop:", cbItemShopList);
        addFormRow(form, gbc, 1, "Tab:", cbItemTabList);
        form.add(new JSeparator(), mkGbc(2));

        // Row 3
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 3;
        form.add(new JLabel("Find Item:"), gbc);
        gbc.gridx = 1;
        JPanel searchP = new JPanel(new BorderLayout());
        searchP.add(txtFindItem, BorderLayout.CENTER);
        searchP.add(btnSearch, BorderLayout.EAST);
        form.add(searchP, gbc);

        // Row 4
        addFormRow(form, gbc, 4, "Select Item:", cbItemTemplate);

        // Row 5
        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checks.add(chkNewItem);
        checks.add(chkSellItem);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        form.add(checks, gbc);

        // Row 6, 7
        gbc.gridwidth = 1; // Reset
        addFormRow(form, gbc, 6, "Type Sell:", cbItemSellType);
        addFormRow(form, gbc, 7, "Cost:", txtItemCost);

        form.add(new JSeparator(), mkGbc(8));

        // Row 9: Find Item Price (Vertical)
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 9;
        lblFindIconSpec = new JLabel("Find item (Dùng làm đơn giá):");
        form.add(lblFindIconSpec, gbc);

        gbc.gridy = 10;
        pnlSearchIcon = new JPanel(new BorderLayout());
        pnlSearchIcon.add(txtFindIconSpec, BorderLayout.CENTER);
        pnlSearchIcon.add(btnSearchIcon, BorderLayout.EAST);
        form.add(pnlSearchIcon, gbc);

        // Row 11: Select Item Price (Vertical)
        gbc.gridy = 11;
        lblSelectIconSpec = new JLabel("Select item (Dùng làm đơn giá):");
        form.add(lblSelectIconSpec, gbc);

        gbc.gridy = 12;
        form.add(cbIconSpec, gbc);

        // Row 13: Buttons
        JPanel itemBtns = new JPanel();
        JButton btnAddItem = new JButton("Thêm item");
        JButton btnEditItem = new JButton("Sửa item");
        JButton btnDelItem = new JButton("Xóa item");

        styleBtn(btnAddItem, new Color(50, 205, 50)); // Lime
        styleBtn(btnEditItem, new Color(255, 165, 0)); // Orange
        styleBtn(btnDelItem, new Color(255, 99, 71)); // Tomato
        btnAddItem.setForeground(Color.BLACK);
        btnEditItem.setForeground(Color.BLACK);
        btnDelItem.setForeground(Color.BLACK);

        btnAddItem.setPreferredSize(new Dimension(110, 30));
        btnEditItem.setPreferredSize(new Dimension(110, 30));
        btnDelItem.setPreferredSize(new Dimension(110, 30));

        btnAddItem.addActionListener(e -> addItem());
        btnEditItem.addActionListener(e -> editItem());
        btnDelItem.addActionListener(e -> deleteItem());

        itemBtns.add(btnAddItem);
        itemBtns.add(btnEditItem);
        itemBtns.add(btnDelItem);

        lblItemStatus = new JLabel(" ");
        lblItemStatus.setHorizontalAlignment(SwingConstants.CENTER);
        lblItemStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbc.gridy = 14;
        form.add(lblItemStatus, gbc);

        gbc.gridx = 0;
        gbc.gridy = 13;
        gbc.gridwidth = 2;
        form.add(itemBtns, gbc);

        // Spacer
        gbc.gridy = 15;
        gbc.weighty = 1.0;
        form.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767)), gbc);

        p.add(form, BorderLayout.WEST);

        // Right Side (Split Pane: Top = Item List, Bottom = Options)
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Item List
        modelItems = new DefaultTableModel(new String[] { "Id", "Name", "Sell", "Cost" }, 0);
        tblItems = new JTable(modelItems);
        tblItems.setRowHeight(25);
        tblItems.setGridColor(Color.LIGHT_GRAY);
        tblItems.setSelectionBackground(new Color(51, 153, 255));
        tblItems.setSelectionForeground(Color.WHITE);
        tblItems.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                selectItem(tblItems.getSelectedRow());
            }
        });
        JScrollPane scrollItems = new JScrollPane(tblItems);
        scrollItems.setBorder(BorderFactory.createTitledBorder("Danh sách Item"));
        split.setTopComponent(scrollItems);

        // Option Section
        JPanel optPanel = new JPanel(new BorderLayout());
        optPanel.setBorder(BorderFactory.createTitledBorder("Item option"));

        JPanel optControls = new JPanel(new GridLayout(2, 1, 5, 5));

        // Row 1: Combo + Param
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cbItemOption = new JComboBox<>();
        cbItemOption.setPreferredSize(new Dimension(200, 30));
        txtOptionParam = new JTextField(10);
        txtOptionParam.setPreferredSize(new Dimension(100, 30));

        row1.add(cbItemOption);
        row1.add(txtOptionParam);

        // Row 2: Buttons
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddOpt = new JButton("Thêm option");
        JButton btnEditOpt = new JButton("Sửa option");
        JButton btnDelOpt = new JButton("Xóa option");
        JButton btnSaveDb = new JButton("Save option");

        styleBtn(btnAddOpt, new Color(221, 221, 221)); // Default/LightGray
        btnAddOpt.setForeground(Color.BLACK);
        styleBtn(btnEditOpt, new Color(221, 221, 221));
        btnEditOpt.setForeground(Color.BLACK);
        styleBtn(btnDelOpt, new Color(221, 221, 221));
        btnDelOpt.setForeground(Color.BLACK);
        styleBtn(btnSaveDb, new Color(144, 238, 144)); // LightGreen
        btnSaveDb.setForeground(Color.BLACK);

        btnAddOpt.setPreferredSize(new Dimension(100, 30));
        btnEditOpt.setPreferredSize(new Dimension(100, 30));
        btnDelOpt.setPreferredSize(new Dimension(100, 30));
        btnSaveDb.setPreferredSize(new Dimension(100, 30));

        // Load Option Templates
        for (ItemOptionTemplate opt : dao.getItemOptionTemplates()) {
            cbItemOption.addItem(opt);
        }

        btnAddOpt.addActionListener(e -> addOption());
        btnEditOpt.addActionListener(e -> updateOption());
        btnDelOpt.addActionListener(e -> deleteOption());
        btnSaveDb.addActionListener(e -> saveItemsToDb());

        row2.add(btnAddOpt);
        row2.add(btnEditOpt);
        row2.add(btnDelOpt);
        row2.add(btnSaveDb);

        optControls.add(row1);
        optControls.add(row2);

        modelOptions = new DefaultTableModel(new String[] { "ID", "Option", "Param" }, 0);
        tblOptions = new JTable(modelOptions);
        tblOptions.setRowHeight(25);
        tblOptions.setGridColor(Color.LIGHT_GRAY);
        tblOptions.setSelectionBackground(new Color(51, 153, 255));
        tblOptions.setSelectionForeground(Color.WHITE);
        tblOptions.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = tblOptions.getSelectedRow();
                if (row >= 0 && selectedItemIdx >= 0) {
                    DisplayItem.ItemOptionDisplay opt = currentDisplayItems.get(selectedItemIdx).options.get(row);
                    txtOptionParam.setText(String.valueOf(opt.param));
                    // Select combo
                    for (int i = 0; i < cbItemOption.getItemCount(); i++) {
                        if (cbItemOption.getItemAt(i).id == opt.id) {
                            cbItemOption.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
        });

        optPanel.add(optControls, BorderLayout.NORTH);
        optPanel.add(new JScrollPane(tblOptions), BorderLayout.CENTER);

        split.setBottomComponent(optPanel);
        split.setDividerLocation(300);

        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private GridBagConstraints mkGbc(int y) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 2;
        g.fill = GridBagConstraints.HORIZONTAL;
        return g;
    }

    private void loadTabsForItemShop() {
        Shop s = (Shop) cbItemShopList.getSelectedItem();
        if (s == null)
            return;
        cbItemTabList.removeAllItems();
        List<TabShop> tabs = dao.getTabsByShopId(s.id);
        for (TabShop t : tabs)
            cbItemTabList.addItem(t);

        // Update input state based on shop type
        updateInputStateByShopType();
    }

    private void loadItemsForTab() {
        TabShop t = (TabShop) cbItemTabList.getSelectedItem();
        if (t == null)
            return;

        ShopManagerDAO.ItemResult res = dao.getItemDataAndDisplayByTabId(t.id);
        currentDisplayItems = res.displayItems;
        currentRawItems = res.rawItems;

        refreshItemTable();
        modelOptions.setRowCount(0);
        selectedItemIdx = -1;
    }

    private void refreshItemTable() {
        modelItems.setRowCount(0);
        for (DisplayItem d : currentDisplayItems) {
            modelItems.addRow(new Object[] { d.id, d.name, d.sellTypeName, d.cost });
        }
    }

    private void updateIconSpecInputState() {
        Shop s = (Shop) cbItemShopList.getSelectedItem();
        TypeSell ts = (TypeSell) cbItemSellType.getSelectedItem();

        // type_shop = 0 (Normal) -> ẩn Find/Select item đơn giá
        // type_shop = 3 (Special) -> hiện và enable Find/Select item đơn giá
        // type_sell = 2 -> enable Find/Select item đơn giá
        if (s != null && s.typeShop == 0) {
            // type_shop = 0: ẩn hoàn toàn
            setIconSpecVisible(false);
        } else if (s != null && s.typeShop == 3) {
            // type_shop = 3: hiện và enable
            setIconSpecVisible(true);
            setIconSpecEnabled(true);
        } else {
            // Các trường hợp khác: hiện và dựa vào type_sell = 2
            setIconSpecVisible(true);
            boolean enable = (ts != null && ts.id == 2);
            setIconSpecEnabled(enable);
        }
    }

    private void setIconSpecVisible(boolean visible) {
        lblFindIconSpec.setVisible(visible);
        pnlSearchIcon.setVisible(visible);
        lblSelectIconSpec.setVisible(visible);
        cbIconSpec.setVisible(visible);
    }

    private void setIconSpecEnabled(boolean enabled) {
        txtFindIconSpec.setEnabled(enabled);
        btnSearchIcon.setEnabled(enabled);
        cbIconSpec.setEnabled(enabled);
    }

    private void updateInputStateByShopType() {
        Shop s = (Shop) cbItemShopList.getSelectedItem();
        if (s == null)
            return;

        // type_shop = 3 (Special) -> dim Type Sell, hiện và enable Find/Select item đơn
        // giá
        if (s.typeShop == 3) {
            cbItemSellType.setEnabled(false);
            setIconSpecVisible(true);
            setIconSpecEnabled(true);
        } else {
            cbItemSellType.setEnabled(true);
            // type_shop = 0 (Normal) -> ẩn Find/Select item đơn giá
            if (s.typeShop == 0) {
                setIconSpecVisible(false);
            } else {
                // Các trường hợp khác: dựa vào type_sell
                updateIconSpecInputState();
            }
        }
    }

    private void searchItemTemplate() {
        cbItemTemplate.removeAllItems();
        List<ItemTemplate> tpls = dao.getItemTemplates(txtFindItem.getText().trim());
        for (ItemTemplate t : tpls) {
            cbItemTemplate.addItem(t);
        }
    }

    private void searchIconSpecTemplate() {
        cbIconSpec.removeAllItems();
        List<ItemTemplate> tpls = dao.getItemTemplates(txtFindIconSpec.getText().trim());
        for (ItemTemplate t : tpls) {
            cbIconSpec.addItem(t);
        }
    }

    private void selectItem(int row) {
        if (row < 0 || row >= currentDisplayItems.size())
            return;
        selectedItemIdx = row;
        // Verify list size match
        if (selectedItemIdx >= currentRawItems.size())
            return;

        DisplayItem d = currentDisplayItems.get(row);
        ItemData raw = currentRawItems.get(row);

        txtItemCost.setText(String.valueOf(raw.cost));
        // Find correct type sell selection
        for (int i = 0; i < cbItemSellType.getItemCount(); i++) {
            TypeSell ts = cbItemSellType.getItemAt(i);
            if (ts.id == raw.type_sell) {
                cbItemSellType.setSelectedIndex(i);
                break;
            }
        }
        chkNewItem.setSelected(raw.is_new);
        chkSellItem.setSelected(raw.is_sell);

        // Load item_spec into cbIconSpec
        if (raw.item_spec > 0) {
            boolean found = false;
            for (int i = 0; i < cbIconSpec.getItemCount(); i++) {
                if (cbIconSpec.getItemAt(i).id == raw.item_spec) {
                    cbIconSpec.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                // If not in combo, try to search and add
                ItemTemplate t = new ItemTemplate(raw.item_spec, dao.getItemName(raw.item_spec));
                cbIconSpec.addItem(t);
                cbIconSpec.setSelectedItem(t);
            }
        } else {
            cbIconSpec.setSelectedIndex(-1);
        }

        refreshOptionTable();
    }

    private void refreshOptionTable() {
        modelOptions.setRowCount(0);
        if (selectedItemIdx < 0 || selectedItemIdx >= currentDisplayItems.size())
            return;
        for (DisplayItem.ItemOptionDisplay opt : currentDisplayItems.get(selectedItemIdx).options) {
            modelOptions.addRow(new Object[] { opt.id, opt.name, opt.param });
        }
    }

    private void addItem() {
        TabShop t = (TabShop) cbItemTabList.getSelectedItem();
        if (t == null) {
            lblItemStatus.setText("Chưa chọn Tab shop!");
            lblItemStatus.setForeground(Color.RED);
            return;
        }
        ItemTemplate tpl = (ItemTemplate) cbItemTemplate.getSelectedItem();
        if (tpl == null) {
            lblItemStatus.setText("Chưa chọn Item!");
            lblItemStatus.setForeground(Color.RED);
            return;
        }

        try {
            int cost = 0;
            try {
                cost = Integer.parseInt(txtItemCost.getText());
            } catch (Exception e) {
            }

            // Khi type_shop = 3, type_sell mặc định = 0
            Shop shop = (Shop) cbItemShopList.getSelectedItem();
            TypeSell selectedTs = (TypeSell) cbItemSellType.getSelectedItem();
            int typeSell;
            if (shop != null && shop.typeShop == 3) {
                typeSell = 0;
            } else {
                typeSell = selectedTs != null ? selectedTs.id : 0;
            }

            ItemData raw = new ItemData();
            raw.temp_id = tpl.id;
            raw.cost = cost;
            raw.type_sell = typeSell;
            raw.is_new = chkNewItem.isSelected();
            raw.is_sell = chkSellItem.isSelected();

            // item_spec logic
            ItemTemplate specTpl = (ItemTemplate) cbIconSpec.getSelectedItem();
            if (specTpl != null && cbIconSpec.isEnabled()) {
                raw.item_spec = specTpl.id;
            } else {
                raw.item_spec = 0;
            }

            DisplayItem disp = new DisplayItem();
            disp.id = tpl.id;
            disp.name = tpl.name;
            disp.cost = cost;
            disp.sellType = typeSell;
            disp.sellTypeName = selectedTs != null ? selectedTs.name : String.valueOf(typeSell);

            currentRawItems.add(raw);
            currentDisplayItems.add(disp);

            // Save directly to DB
            dao.updateItemsJson(t.id, currentRawItems);

            lblItemStatus.setText("Thêm item thành công!");
            lblItemStatus.setForeground(new Color(0, 153, 51));

            refreshItemTable();

            // Select the new item
            final int targetRow = currentDisplayItems.size() - 1;
            SwingUtilities.invokeLater(() -> {
                tblItems.setRowSelectionInterval(targetRow, targetRow);
                tblItems.scrollRectToVisible(tblItems.getCellRect(targetRow, 0, true));
                selectItem(targetRow);
            });
        } catch (Exception e) {
            e.printStackTrace();
            lblItemStatus.setText("Thêm thất bại!");
            lblItemStatus.setForeground(Color.RED);
        }
    }

    private void editItem() {
        TabShop t = (TabShop) cbItemTabList.getSelectedItem();
        if (selectedItemIdx < 0 || selectedItemIdx >= currentRawItems.size() || t == null) {
            lblItemStatus.setText("Chưa chọn item!");
            lblItemStatus.setForeground(Color.RED);
            return;
        }
        ItemTemplate tpl = (ItemTemplate) cbItemTemplate.getSelectedItem();

        try {
            ItemData raw = currentRawItems.get(selectedItemIdx);
            TypeSell selectedTs = (TypeSell) cbItemSellType.getSelectedItem();

            raw.cost = Integer.parseInt(txtItemCost.getText());
            raw.type_sell = selectedTs != null ? selectedTs.id : 0;
            raw.is_new = chkNewItem.isSelected();
            raw.is_sell = chkSellItem.isSelected();
            if (tpl != null)
                raw.temp_id = tpl.id;

            // item_spec logic
            ItemTemplate specTpl = (ItemTemplate) cbIconSpec.getSelectedItem();
            if (specTpl != null && cbIconSpec.isEnabled()) {
                raw.item_spec = specTpl.id;
            } else {
                raw.item_spec = 0;
            }

            DisplayItem disp = currentDisplayItems.get(selectedItemIdx);
            disp.cost = raw.cost;
            disp.sellType = raw.type_sell;
            disp.sellTypeName = selectedTs != null ? selectedTs.name : String.valueOf(raw.type_sell);
            if (tpl != null) {
                disp.id = raw.temp_id;
                disp.name = tpl.name;
            }

            // Save directly to DB
            dao.updateItemsJson(t.id, currentRawItems);

            lblItemStatus.setText("Sửa item thành công!");
            lblItemStatus.setForeground(new Color(0, 153, 51));

            refreshItemTable();

            // Re-select
            final int targetRow = selectedItemIdx;
            SwingUtilities.invokeLater(() -> {
                tblItems.setRowSelectionInterval(targetRow, targetRow);
                tblItems.scrollRectToVisible(tblItems.getCellRect(targetRow, 0, true));
                selectItem(targetRow);
            });
        } catch (Exception e) {
            e.printStackTrace();
            lblItemStatus.setText("Sửa thất bại!");
            lblItemStatus.setForeground(Color.RED);
        }
    }

    private void deleteItem() {
        TabShop t = (TabShop) cbItemTabList.getSelectedItem();
        if (selectedItemIdx < 0 || selectedItemIdx >= currentRawItems.size() || t == null) {
            lblItemStatus.setText("Chưa chọn item!");
            lblItemStatus.setForeground(Color.RED);
            return;
        }

        if (JOptionPane.showConfirmDialog(this, "Xóa item này?") == JOptionPane.YES_OPTION) {
            try {
                currentRawItems.remove(selectedItemIdx);
                currentDisplayItems.remove(selectedItemIdx);

                // Save directly to DB
                dao.updateItemsJson(t.id, currentRawItems);

                lblItemStatus.setText("Xóa item thành công!");
                lblItemStatus.setForeground(new Color(0, 153, 51));

                refreshItemTable();
                modelOptions.setRowCount(0);
                selectedItemIdx = -1;
            } catch (Exception e) {
                lblItemStatus.setText("Xóa thất bại!");
                lblItemStatus.setForeground(Color.RED);
            }
        }
    }

    private void addOption() {
        if (selectedItemIdx < 0 || selectedItemIdx >= currentRawItems.size())
            return;
        ItemOptionTemplate optTpl = (ItemOptionTemplate) cbItemOption.getSelectedItem();
        if (optTpl == null)
            return;

        try {
            int param = 0;
            try {
                param = Integer.parseInt(txtOptionParam.getText());
            } catch (Exception e) {
            }

            ItemData raw = currentRawItems.get(selectedItemIdx);
            DisplayItem disp = currentDisplayItems.get(selectedItemIdx);

            // Check exist
            for (ItemOption o : raw.options)
                if (o.id == optTpl.id)
                    return;

            raw.options.add(new ItemOption(optTpl.id, param));
            disp.options.add(new DisplayItem.ItemOptionDisplay(optTpl.id, optTpl.name, param));

            refreshOptionTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateOption() {
        if (selectedItemIdx < 0 || selectedItemIdx >= currentRawItems.size() || tblOptions.getSelectedRow() < 0)
            return;
        int optIdx = tblOptions.getSelectedRow();

        try {
            int param = Integer.parseInt(txtOptionParam.getText());
            ItemData raw = currentRawItems.get(selectedItemIdx);
            DisplayItem disp = currentDisplayItems.get(selectedItemIdx);

            raw.options.get(optIdx).param = param;
            disp.options.get(optIdx).param = param;

            refreshOptionTable();
        } catch (Exception e) {
        }
    }

    private void deleteOption() {
        if (selectedItemIdx < 0 || selectedItemIdx >= currentRawItems.size() || tblOptions.getSelectedRow() < 0)
            return;
        int optIdx = tblOptions.getSelectedRow();

        ItemData raw = currentRawItems.get(selectedItemIdx);
        DisplayItem disp = currentDisplayItems.get(selectedItemIdx);

        raw.options.remove(optIdx);
        disp.options.remove(optIdx);
        refreshOptionTable();
    }

    private void saveItemsToDb() {
        TabShop t = (TabShop) cbItemTabList.getSelectedItem();
        if (t != null) {
            dao.updateItemsJson(t.id, currentRawItems);
            JOptionPane.showMessageDialog(this, "Đã lưu dữ liệu Items vào DB!");
        }
    }

    // --- Giftcode ---
    private JPanel createGiftcodePanel() {
        JPanel p = new JPanel(new BorderLayout(5, 5));

        // Columns: ID, Code, Count, Detail, Created, Expired

        modelGiftcodes = new DefaultTableModel(
                new String[] { "ID", "Code", "Số lượng", "Chi tiết", "Ngày tạo", "Hết hạn" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblGiftcodes = new JTable(modelGiftcodes) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int row = rowAtPoint(p);
                int col = columnAtPoint(p);
                if (row >= 0 && col >= 0) {
                    // Check if it's the Detail column (index 3)
                    if (convertColumnIndexToModel(col) == 3) {
                        javax.swing.table.TableCellRenderer renderer = getCellRenderer(row, col);
                        Component comp = prepareRenderer(renderer, row, col);
                        if (comp instanceof javax.swing.JComponent) {
                            // Layout to find child at point
                            java.awt.Rectangle cellRect = getCellRect(row, col, false);
                            comp.setSize(cellRect.width, cellRect.height);
                            comp.doLayout();

                            java.awt.Point childPoint = new java.awt.Point(p.x - cellRect.x, p.y - cellRect.y);
                            Component child = comp.getComponentAt(childPoint);
                            if (child instanceof javax.swing.JComponent) {
                                String tip = ((javax.swing.JComponent) child).getToolTipText();
                                if (tip != null)
                                    return tip;
                            }
                        }
                    }
                }
                return super.getToolTipText(e);
            }
        };
        tblGiftcodes.setRowHeight(40); // Height for icons
        tblGiftcodes.setShowGrid(true);
        tblGiftcodes.setGridColor(Color.LIGHT_GRAY);

        // Widths
        tblGiftcodes.getColumnModel().getColumn(0).setMaxWidth(50);
        tblGiftcodes.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblGiftcodes.getColumnModel().getColumn(1).setPreferredWidth(100);
        tblGiftcodes.getColumnModel().getColumn(2).setMaxWidth(80);
        tblGiftcodes.getColumnModel().getColumn(2).setPreferredWidth(80);
        tblGiftcodes.getColumnModel().getColumn(3).setPreferredWidth(300);
        tblGiftcodes.getColumnModel().getColumn(4).setPreferredWidth(120);
        tblGiftcodes.getColumnModel().getColumn(5).setPreferredWidth(120);

        // Renderers
        GiftcodeRenderers.CenterRenderer center = new GiftcodeRenderers.CenterRenderer();
        tblGiftcodes.getColumnModel().getColumn(0).setCellRenderer(center);
        tblGiftcodes.getColumnModel().getColumn(1).setCellRenderer(center);
        tblGiftcodes.getColumnModel().getColumn(2).setCellRenderer(center);
        tblGiftcodes.getColumnModel().getColumn(3).setCellRenderer(new GiftcodeRenderers.DetailRenderer(dao));
        tblGiftcodes.getColumnModel().getColumn(4).setCellRenderer(center);
        tblGiftcodes.getColumnModel().getColumn(5).setCellRenderer(center);

        p.add(new JScrollPane(tblGiftcodes), BorderLayout.CENTER);

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAdd = new JButton("Thêm mới");
        JButton btnEdit = new JButton("Sửa");
        JButton btnDel = new JButton("Xóa");
        JButton btnRefresh = new JButton("Refresh");

        styleBtn(btnAdd, new Color(50, 205, 50)); // Lime
        btnAdd.setForeground(Color.BLACK);
        styleBtn(btnEdit, new Color(255, 165, 0)); // Orange
        btnEdit.setForeground(Color.BLACK);
        styleBtn(btnDel, new Color(255, 99, 71)); // Tomato
        btnDel.setForeground(Color.BLACK);
        styleBtn(btnRefresh, new Color(60, 141, 188));
        btnRefresh.setPreferredSize(new Dimension(80, 30));

        btnAdd.addActionListener(e -> addGiftcode());
        btnEdit.addActionListener(e -> editGiftcode());
        btnDel.addActionListener(e -> deleteGiftcode());
        btnRefresh.addActionListener(e -> refreshGiftcodes());

        bot.add(btnAdd);
        bot.add(btnEdit);
        bot.add(btnDel);
        bot.add(btnRefresh);

        p.add(bot, BorderLayout.SOUTH);

        return p;
    }

    private void addGiftcode() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        new GiftcodeDialog(parent, null, dao).setVisible(true);
        refreshGiftcodes();
    }

    private void editGiftcode() {
        int row = tblGiftcodes.getSelectedRow();
        if (row < 0 || row >= giftcodeList.size())
            return;
        Giftcode gc = giftcodeList.get(row);
        Window parent = SwingUtilities.getWindowAncestor(this);
        new GiftcodeDialog(parent, gc, dao).setVisible(true);
        refreshGiftcodes();
    }

    private void deleteGiftcode() {
        int row = tblGiftcodes.getSelectedRow();
        if (row < 0 || row >= giftcodeList.size())
            return;
        Giftcode gc = giftcodeList.get(row);
        if (JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa code: " + gc.code + "?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            dao.deleteGiftcode(gc.id);
            refreshGiftcodes();
        }
    }

    private void refreshGiftcodes() {
        giftcodeList = dao.getGiftcodes();
        modelGiftcodes.setRowCount(0);
        for (Giftcode g : giftcodeList) {
            modelGiftcodes.addRow(new Object[] {
                    g.id,
                    g.code,
                    g.countLeft,
                    g.detail, // Column 3: Detail (JSON)
                    g.dateCreate, // Column 4: Created
                    g.expired // Column 5: Expired
            });
        }
    }

    // --- Giftcode Dialog ---
    private static class GiftcodeDialog extends JDialog {
        private final ShopManagerDAO dao;
        private final Giftcode currentGiftcode;

        // UI
        private JComboBox<ItemTemplate> cbItemTemplate;
        private JTextField txtFindItem;
        private JTextField txtQuantity;
        private JComboBox<ItemOptionTemplate> cbItemOption;
        private JTextField txtOptionParam;

        private JTextField txtCode;
        private JTextField txtCountLeft;
        private JSpinner spnExpired;

        private JTable tblItems;
        private DefaultTableModel modelItems;
        private JTable tblOptions;
        private DefaultTableModel modelOptions;

        // Data
        private java.util.List<GiftcodeItem> itemList = new ArrayList<>();
        private int selectedItemIdx = -1;

        private static class GiftcodeItem {
            int tempId;
            String name;
            int quantity;
            java.util.List<ItemOption> options = new ArrayList<>();
        }

        public GiftcodeDialog(Window owner, Giftcode gc, ShopManagerDAO dao) {
            super(owner, gc == null ? "Thêm Giftcode mới" : "Sửa Giftcode", ModalityType.APPLICATION_MODAL);
            this.dao = dao;
            this.currentGiftcode = gc;
            this.setSize(900, 650);
            this.setLocationRelativeTo(owner);

            initComponents();
            if (gc != null) {
                loadData(gc);
            }
        }

        private void initComponents() {
            JPanel main = new JPanel(new BorderLayout(10, 10));
            main.setBorder(new EmptyBorder(10, 10, 10, 10));

            // Center: SplitPane
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            split.setResizeWeight(0.6);

            // LEFT: Item List & Option List
            JPanel left = new JPanel(new BorderLayout(5, 5));

            // Item List
            JPanel pItemList = new JPanel(new BorderLayout());
            pItemList.setBorder(BorderFactory.createTitledBorder("Danh sách Item"));
            modelItems = new DefaultTableModel(new String[] { "ID", "Name", "Quantity" }, 0) {
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };
            tblItems = new JTable(modelItems);
            tblItems.setRowHeight(25);
            tblItems.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    selectItem(tblItems.getSelectedRow());
                }
            });
            pItemList.add(new JScrollPane(tblItems), BorderLayout.CENTER);

            // Option List
            JPanel pOptList = new JPanel(new BorderLayout());
            pOptList.setBorder(BorderFactory.createTitledBorder("Item option"));

            // Item Option Controls
            JPanel optCtrl = new JPanel(new FlowLayout(FlowLayout.LEFT));
            cbItemOption = new JComboBox<>();
            cbItemOption.setPreferredSize(new Dimension(200, 25));
            txtOptionParam = new JTextField(8);

            JButton btnAddOpt = new JButton("Add Opt");
            JButton btnDelOpt = new JButton("Del Opt");

            btnAddOpt.addActionListener(e -> addOption());
            btnDelOpt.addActionListener(e -> deleteOption());

            // init option templates
            for (ItemOptionTemplate opt : dao.getItemOptionTemplates()) {
                cbItemOption.addItem(opt);
            }

            optCtrl.add(cbItemOption);
            optCtrl.add(txtOptionParam);
            optCtrl.add(btnAddOpt);
            optCtrl.add(btnDelOpt);

            pOptList.add(optCtrl, BorderLayout.NORTH);

            modelOptions = new DefaultTableModel(new String[] { "ID", "Option", "Param" }, 0);
            tblOptions = new JTable(modelOptions);
            tblOptions.setRowHeight(25);
            pOptList.add(new JScrollPane(tblOptions), BorderLayout.CENTER);

            // Combine Left
            JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            leftSplit.setTopComponent(pItemList);
            leftSplit.setBottomComponent(pOptList);
            leftSplit.setResizeWeight(0.5);

            left.add(leftSplit, BorderLayout.CENTER);

            // DELETE ITEM Button
            JButton btnDelItem = new JButton("Xóa Item đã chọn");
            btnDelItem.setBackground(new Color(255, 99, 71));
            btnDelItem.setForeground(Color.WHITE);
            btnDelItem.addActionListener(e -> deleteItem());
            left.add(btnDelItem, BorderLayout.SOUTH);

            // RIGHT: Search & Add Panel
            JPanel right = new JPanel(new GridBagLayout());
            right.setBorder(BorderFactory.createTitledBorder("Thêm Item"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = 0;

            right.add(new JLabel("Find Item:"), gbc);

            gbc.gridy++;
            txtFindItem = new JTextField();
            txtFindItem.setPreferredSize(new Dimension(150, 25));

            // Search listener
            txtFindItem.addActionListener(e -> searchItem());

            JPanel searchBox = new JPanel(new BorderLayout());
            searchBox.add(txtFindItem, BorderLayout.CENTER);
            JButton btnSearch = new JButton("Find");
            btnSearch.addActionListener(e -> searchItem());
            searchBox.add(btnSearch, BorderLayout.EAST);

            right.add(searchBox, gbc);

            gbc.gridy++;
            right.add(new JLabel("Select Item:"), gbc);

            gbc.gridy++;
            cbItemTemplate = new JComboBox<>();
            right.add(cbItemTemplate, gbc);

            gbc.gridy++;
            right.add(new JLabel("Quantity:"), gbc);

            gbc.gridy++;
            txtQuantity = new JTextField("1");
            right.add(txtQuantity, gbc);

            gbc.gridy++;
            JButton btnAddItem = new JButton("Add Item");
            btnAddItem.setBackground(new Color(50, 205, 50));
            btnAddItem.setForeground(Color.WHITE);
            btnAddItem.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnAddItem.addActionListener(e -> addItem());
            right.add(btnAddItem, gbc);

            // Spacer
            gbc.gridy++;
            gbc.weighty = 1.0;
            right.add(new JPanel(), gbc);

            split.setLeftComponent(left);
            split.setRightComponent(right);
            main.add(split, BorderLayout.CENTER);

            // BOTTOM: Config
            JPanel bottom = new JPanel(new GridBagLayout());
            bottom.setBorder(new EmptyBorder(10, 0, 0, 0));
            gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            bottom.add(new JLabel("Code:"), gbc);
            gbc.gridx = 1;
            txtCode = new JTextField(15);
            bottom.add(txtCode, gbc);

            gbc.gridx = 2;
            bottom.add(new JLabel("Count Left:"), gbc);
            gbc.gridx = 3;
            txtCountLeft = new JTextField("999", 5);
            bottom.add(txtCountLeft, gbc);

            gbc.gridx = 4;
            bottom.add(new JLabel("Expired:"), gbc);
            gbc.gridx = 5;

            // Spinner Date
            spnExpired = new JSpinner(new SpinnerDateModel());
            JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(spnExpired, "yyyy-MM-dd HH:mm:ss");
            spnExpired.setEditor(timeEditor);
            spnExpired.setValue(new Date(System.currentTimeMillis() + 2592000000L)); // +30 days
            spnExpired.setPreferredSize(new Dimension(140, 25));

            JPanel pDate = new JPanel(new BorderLayout());
            pDate.add(spnExpired, BorderLayout.CENTER);

            JButton btnCal = new JButton("📅"); // Calendar Icon or Text
            btnCal.setMargin(new Insets(0, 0, 0, 0));
            btnCal.setPreferredSize(new Dimension(25, 25));
            btnCal.addActionListener(e -> {
                DatePicker dp = new DatePicker(SwingUtilities.getWindowAncestor(this));
                dp.setVisible(true);
                Date d = dp.getSelectedDate();
                if (d != null) {
                    Date current = (Date) spnExpired.getValue();
                    Calendar cOld = Calendar.getInstance();
                    cOld.setTime(current);
                    Calendar cNew = Calendar.getInstance();
                    cNew.setTime(d);

                    cOld.set(Calendar.YEAR, cNew.get(Calendar.YEAR));
                    cOld.set(Calendar.MONTH, cNew.get(Calendar.MONTH));
                    cOld.set(Calendar.DAY_OF_MONTH, cNew.get(Calendar.DAY_OF_MONTH));

                    spnExpired.setValue(cOld.getTime());
                }
            });
            pDate.add(btnCal, BorderLayout.EAST);
            bottom.add(pDate, gbc);

            gbc.gridx = 6;
            JButton btnSave = new JButton("Save");
            btnSave.setBackground(new Color(50, 205, 50));
            btnSave.setForeground(Color.WHITE);
            btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnSave.addActionListener(e -> save());
            bottom.add(btnSave, gbc);

            main.add(bottom, BorderLayout.SOUTH);
            this.add(main);

            // Init search
            searchItem();
        }

        private void loadData(Giftcode gc) {
            txtCode.setText(gc.code);
            txtCountLeft.setText(String.valueOf(gc.countLeft));
            if (gc.expired != null) {
                spnExpired.setValue(new Date(gc.expired.getTime()));
            }

            // Parse detail
            if (gc.detail != null && !gc.detail.isEmpty()) {
                try {
                    JSONParser parser = new JSONParser();
                    Object obj = parser.parse(gc.detail);
                    if (obj instanceof JSONArray) {
                        JSONArray arr = (JSONArray) obj;
                        for (Object o : arr) {
                            JSONObject itemJson = (JSONObject) o;
                            int tempId = ((Long) itemJson.get("temp_id")).intValue();
                            int qty = ((Long) itemJson.get("quantity")).intValue();

                            GiftcodeItem item = new GiftcodeItem();
                            item.tempId = tempId;
                            item.quantity = qty;
                            // Need name
                            List<ItemTemplate> tpls = dao.getItemTemplates(null); // Inefficient but works
                            item.name = "Item " + tempId;
                            for (ItemTemplate t : tpls) {
                                if (t.id == tempId) {
                                    item.name = t.name;
                                    break;
                                }
                            }

                            // Options
                            JSONArray opts = (JSONArray) itemJson.get("options");
                            if (opts != null) {
                                for (Object optO : opts) {
                                    JSONObject optJ = (JSONObject) optO;
                                    int oid = ((Long) optJ.get("id")).intValue();
                                    int op = ((Long) optJ.get("param")).intValue();
                                    item.options.add(new ItemOption(oid, op));
                                }
                            }
                            itemList.add(item);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            refreshItems();
        }

        private void searchItem() {
            cbItemTemplate.removeAllItems();
            String txt = txtFindItem.getText().trim();
            for (ItemTemplate t : dao.getItemTemplates(txt)) {
                cbItemTemplate.addItem(t);
            }
        }

        private void addItem() {
            ItemTemplate tpl = (ItemTemplate) cbItemTemplate.getSelectedItem();
            if (tpl == null)
                return;
            try {
                int qty = Integer.parseInt(txtQuantity.getText().trim());
                GiftcodeItem item = new GiftcodeItem();
                item.tempId = tpl.id;
                item.name = tpl.name;
                item.quantity = qty;
                itemList.add(item);
                refreshItems();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi quantity!");
            }
        }

        private void deleteItem() {
            if (selectedItemIdx >= 0 && selectedItemIdx < itemList.size()) {
                itemList.remove(selectedItemIdx);
                refreshItems();
                selectedItemIdx = -1;
                modelOptions.setRowCount(0);
            }
        }

        private void selectItem(int row) {
            if (row >= 0 && row < itemList.size()) {
                selectedItemIdx = row;
                refreshOptions();
            }
        }

        private void refreshItems() {
            modelItems.setRowCount(0);
            for (GiftcodeItem i : itemList) {
                modelItems.addRow(new Object[] { i.tempId, i.name, i.quantity });
            }
        }

        private void refreshOptions() {
            modelOptions.setRowCount(0);
            if (selectedItemIdx < 0)
                return;
            GiftcodeItem item = itemList.get(selectedItemIdx);
            for (ItemOption opt : item.options) {
                String name = "Option " + opt.id;
                for (int i = 0; i < cbItemOption.getItemCount(); i++) {
                    ItemOptionTemplate t = cbItemOption.getItemAt(i);
                    if (t.id == opt.id) {
                        name = t.name;
                        break;
                    }
                }
                modelOptions.addRow(new Object[] { opt.id, name, opt.param });
            }
        }

        private void addOption() {
            if (selectedItemIdx < 0)
                return;
            ItemOptionTemplate tpl = (ItemOptionTemplate) cbItemOption.getSelectedItem();
            if (tpl == null)
                return;
            try {
                int param = Integer.parseInt(txtOptionParam.getText().trim());
                GiftcodeItem item = itemList.get(selectedItemIdx);
                // check exist
                for (ItemOption o : item.options)
                    if (o.id == tpl.id)
                        return;

                item.options.add(new ItemOption(tpl.id, param));
                refreshOptions();
            } catch (Exception e) {
            }
        }

        private void deleteOption() {
            if (selectedItemIdx < 0)
                return;
            int optRow = tblOptions.getSelectedRow();
            if (optRow < 0)
                return;
            GiftcodeItem item = itemList.get(selectedItemIdx);
            item.options.remove(optRow);
            refreshOptions();
        }

        private void save() {
            try {
                String code = txtCode.getText().trim();
                if (code.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Chưa nhập code!");
                    return;
                }
                int count = Integer.parseInt(txtCountLeft.getText().trim());
                Timestamp expired = new Timestamp(((Date) spnExpired.getValue()).getTime());

                // Build JSON
                JSONArray arr = new JSONArray();
                for (GiftcodeItem item : itemList) {
                    JSONObject obj = new JSONObject();
                    obj.put("temp_id", item.tempId);
                    obj.put("quantity", item.quantity);
                    JSONArray opts = new JSONArray();
                    for (ItemOption o : item.options) {
                        JSONObject op = new JSONObject();
                        op.put("id", o.id);
                        op.put("param", o.param);
                        opts.add(op);
                    }
                    obj.put("options", opts);
                    arr.add(obj);
                }
                String detail = arr.toJSONString();
                if (currentGiftcode == null) {
                    Giftcode g = new Giftcode();
                    g.code = code;
                    g.countLeft = count;
                    g.expired = expired;
                    g.detail = detail;
                    g.dateCreate = new Timestamp(System.currentTimeMillis());
                    g.type = 1;
                    dao.addGiftcode(g);
                } else {
                    currentGiftcode.code = code;
                    currentGiftcode.countLeft = count;
                    currentGiftcode.expired = expired;
                    currentGiftcode.detail = detail;
                    dao.updateGiftcode(currentGiftcode);
                }
                dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
