package com.girlkun.tool.screens.part_scr;

import com.girlkun.database.GirlkunDB;
import com.girlkun.result.GirlkunResultSet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình thêm Skin mới vào DB
 * - Nhiều text box cho part head (type=0), có thể thêm/xóa động
 * - 1 text box cho body (type=1), 1 cho leg (type=2) → lưu vào table `part`
 * - 1 text box cho avatar_id → lưu vào table `head_avatar`
 */
public class AddSkinScr extends JInternalFrame {

    // Dynamic head fields
    private final List<JTextField> headDataFields = new ArrayList<>();
    private final List<JLabel> headIdLabels = new ArrayList<>();
    private final List<JPanel> headRowPanels = new ArrayList<>();
    private JPanel headRowsPanel;

    private JTextField txtBodyData;
    private JTextField txtLegData;
    private JTextField txtAvatarId;

    private JLabel lblBodyId;
    private JLabel lblLegId;

    // Skin Item fields
    private JCheckBox chkAddItem;
    private JLabel lblItemId;
    private JTextField txtIconId;
    private JTextField txtItemName;

    private JLabel lblStatus;
    private JButton btnSave;
    private JButton btnRefreshId;

    private int latestPartId = -1;
    private int nextItemId = -1;

    public AddSkinScr() {
        super("Add new Skin", true, true, true, true);
        this.setSize(900, 800);
        this.setFrameIcon(new ImageIcon("icon.png"));
        initComponents();
        loadNextIds();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        // === Head Section ===
        JPanel headSection = new JPanel(new BorderLayout(5, 5));
        headSection.setBorder(styledBorder("Part Head (type=0)"));

        headRowsPanel = new JPanel();
        headRowsPanel.setLayout(new BoxLayout(headRowsPanel, BoxLayout.Y_AXIS));
        addHeadRow(); // Dòng head đầu tiên (không có nút xóa)

        JButton btnAddHead = new JButton("+ Thêm Head");
        styleBtn(btnAddHead, new Color(0, 153, 204));
        btnAddHead.setPreferredSize(new Dimension(140, 30));
        btnAddHead.setMaximumSize(new Dimension(140, 30));
        btnAddHead.addActionListener(e -> addHeadRow());

        JPanel addBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        addBtnPanel.add(btnAddHead);

        headSection.add(headRowsPanel, BorderLayout.CENTER);
        headSection.add(addBtnPanel, BorderLayout.SOUTH);

        formPanel.add(headSection);
        formPanel.add(Box.createVerticalStrut(8));

        // === Body Section ===
        JPanel bodySection = new JPanel(new GridBagLayout());
        bodySection.setBorder(styledBorder("Part Body (type=1)"));
        GridBagConstraints gbcBody = createGbc();

        lblBodyId = new JLabel("ID: đang tải...");
        lblBodyId.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblBodyId.setForeground(new Color(40, 167, 69));
        addRow(bodySection, gbcBody, 0, "ID sẽ lưu:", lblBodyId);

        txtBodyData = new JTextField(30);
        txtBodyData.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addRow(bodySection, gbcBody, 1, "Data (body):", txtBodyData);

        formPanel.add(bodySection);
        formPanel.add(Box.createVerticalStrut(8));

        // === Leg Section ===
        JPanel legSection = new JPanel(new GridBagLayout());
        legSection.setBorder(styledBorder("Part Leg (type=2)"));
        GridBagConstraints gbcLeg = createGbc();

        lblLegId = new JLabel("ID: đang tải...");
        lblLegId.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblLegId.setForeground(new Color(255, 140, 0));
        addRow(legSection, gbcLeg, 0, "ID sẽ lưu:", lblLegId);

        txtLegData = new JTextField(30);
        txtLegData.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addRow(legSection, gbcLeg, 1, "Data (leg):", txtLegData);

        formPanel.add(legSection);
        formPanel.add(Box.createVerticalStrut(8));

        // === Avatar Section ===
        JPanel avatarSection = new JPanel(new GridBagLayout());
        avatarSection.setBorder(styledBorder("Head Avatar"));
        GridBagConstraints gbcAvatar = createGbc();

        txtAvatarId = new JTextField(15);
        txtAvatarId.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addRow(avatarSection, gbcAvatar, 0, "Avatar ID:", txtAvatarId);

        JLabel lblAvatarNote = new JLabel(
                "Lưu vào head_avatar (head_id = ID head đầu tiên, avatar_id = giá trị nhập)");
        lblAvatarNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblAvatarNote.setForeground(new Color(150, 150, 150));
        gbcAvatar.gridx = 0;
        gbcAvatar.gridy = 1;
        gbcAvatar.gridwidth = 2;
        avatarSection.add(lblAvatarNote, gbcAvatar);

        formPanel.add(avatarSection);
        formPanel.add(Box.createVerticalStrut(8));

        // === Skin Item Section ===
        JPanel itemSection = new JPanel(new GridBagLayout());
        itemSection.setBorder(styledBorder("Skin Item (item_template)"));
        GridBagConstraints gbcItem = createGbc();

        chkAddItem = new JCheckBox("Thêm item vào table item_template", true);
        chkAddItem.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbcItem.gridx = 0;
        gbcItem.gridy = 0;
        gbcItem.gridwidth = 2;
        itemSection.add(chkAddItem, gbcItem);
        gbcItem.gridwidth = 1;

        lblItemId = new JLabel("ID: đang tải...");
        lblItemId.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblItemId.setForeground(new Color(156, 39, 176));
        addRow(itemSection, gbcItem, 1, "Item ID sẽ lưu:", lblItemId);

        txtIconId = new JTextField(15);
        txtIconId.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addRow(itemSection, gbcItem, 2, "Icon ID:", txtIconId);

        txtItemName = new JTextField(25);
        txtItemName.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addRow(itemSection, gbcItem, 3, "Item Name:", txtItemName);

        // Toggle enable/disable khi checkbox thay đổi
        chkAddItem.addActionListener(e -> {
            boolean enabled = chkAddItem.isSelected();
            txtIconId.setEnabled(enabled);
            txtItemName.setEnabled(enabled);
        });

        formPanel.add(itemSection);
        formPanel.add(Box.createVerticalStrut(10));

        // === Action Buttons ===
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));

        btnSave = new JButton("Lưu vào DB");
        styleBtn(btnSave, new Color(40, 167, 69));
        btnSave.setPreferredSize(new Dimension(140, 35));
        btnSave.addActionListener(this::onSave);

        btnRefreshId = new JButton("Refresh ID");
        styleBtn(btnRefreshId, new Color(23, 162, 184));
        btnRefreshId.setPreferredSize(new Dimension(120, 35));
        btnRefreshId.addActionListener(e -> loadNextIds());

        JButton btnClear = new JButton("Xóa form");
        styleBtn(btnClear, new Color(108, 117, 125));
        btnClear.setPreferredSize(new Dimension(100, 35));
        btnClear.addActionListener(e -> clearForm());

        btnPanel.add(btnSave);
        btnPanel.add(btnRefreshId);
        btnPanel.add(btnClear);
        formPanel.add(btnPanel);

        mainPanel.add(new JScrollPane(formPanel), BorderLayout.CENTER);

        // === Status Bar ===
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblStatus = new JLabel("Sẵn sàng");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusPanel.add(lblStatus);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        this.add(mainPanel);
    }

    /**
     * Thêm 1 dòng head data. Dòng đầu tiên không có nút xóa.
     */
    private void addHeadRow() {
        int index = headDataFields.size();

        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        JLabel idLabel = new JLabel("ID: ...");
        idLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        idLabel.setForeground(new Color(0, 153, 204));
        idLabel.setPreferredSize(new Dimension(80, 25));

        JTextField txtData = new JTextField(25);
        txtData.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        rowPanel.add(idLabel);
        rowPanel.add(new JLabel("Data:"));
        rowPanel.add(txtData);

        // Nút xóa cho các dòng thêm (không phải dòng đầu)
        if (index > 0) {
            JButton btnRemove = new JButton("Xóa");
            styleBtn(btnRemove, new Color(220, 53, 69));
            btnRemove.setPreferredSize(new Dimension(65, 28));
            btnRemove.addActionListener(e -> removeHeadRow(rowPanel, txtData, idLabel));
            rowPanel.add(btnRemove);
        }

        headDataFields.add(txtData);
        headIdLabels.add(idLabel);
        headRowPanels.add(rowPanel);
        headRowsPanel.add(rowPanel);

        recalculateIds();
        headRowsPanel.revalidate();
        headRowsPanel.repaint();
    }

    /**
     * Xóa 1 dòng head data và cập nhật lại ID
     */
    private void removeHeadRow(JPanel rowPanel, JTextField txtData, JLabel idLabel) {
        headDataFields.remove(txtData);
        headIdLabels.remove(idLabel);
        headRowPanels.remove(rowPanel);
        headRowsPanel.remove(rowPanel);

        recalculateIds();
        headRowsPanel.revalidate();
        headRowsPanel.repaint();
    }

    /**
     * Tính lại tất cả ID dựa trên số lượng head hiện tại.
     * head[0] = latestId+1, head[1] = latestId+2, ...
     * body = latestId + headCount + 1
     * leg = latestId + headCount + 2
     */
    private void recalculateIds() {
        int headCount = headDataFields.size();
        for (int i = 0; i < headCount; i++) {
            int headId = latestPartId + 1 + i;
            headIdLabels.get(i).setText("ID: " + (latestPartId >= 0 ? String.valueOf(headId) : "..."));
        }

        if (latestPartId >= 0) {
            int bodyId = latestPartId + headCount + 1;
            int legId = latestPartId + headCount + 2;
            lblBodyId.setText("ID: " + bodyId);
            lblLegId.setText("ID: " + legId);
        }
    }

    // === Helper UI methods ===

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, Component cmp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(lbl, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        p.add(cmp, gbc);
    }

    private void styleBtn(JButton btn, Color c) {
        btn.setBackground(c);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    private TitledBorder styledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 15));
        return border;
    }

    // === Data operations ===

    /**
     * Lấy ID tiếp theo cho mỗi loại part từ DB
     * Lấy MAX(id) của toàn bộ table part, rồi tính ID tuần tự
     */
    private void loadNextIds() {
        new Thread(() -> {
            try {
                latestPartId = getLatestPartId();
                nextItemId = getLatestItemTemplateId() + 1;

                SwingUtilities.invokeLater(() -> {
                    recalculateIds();
                    lblItemId.setText("ID: " + nextItemId);
                    setStatus("Đã tải ID thành công (latest part: " + latestPartId + ", next item: " + nextItemId + ")",
                            new Color(40, 167, 69));
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    lblItemId.setText("Lỗi tải ID");
                    setStatus("Lỗi tải ID: " + e.getMessage(), Color.RED);
                });
            }
        }).start();
    }

    /**
     * Lấy ID lớn nhất hiện tại trong toàn bộ table part (không phân biệt type)
     */
    private int getLatestPartId() throws Exception {
        GirlkunResultSet rs = GirlkunDB.executeQuery("GIRLKUN",
                "SELECT IFNULL(MAX(id), -1) as max_id FROM part");
        if (rs.first()) {
            return rs.getInt("max_id");
        }
        return -1;
    }

    /**
     * Lấy ID lớn nhất hiện tại trong table item_template
     */
    private int getLatestItemTemplateId() throws Exception {
        GirlkunResultSet rs = GirlkunDB.executeQuery("GIRLKUN",
                "SELECT IFNULL(MAX(id), -1) as max_id FROM item_template");
        if (rs.first()) {
            return rs.getInt("max_id");
        }
        return -1;
    }

    private void onSave(ActionEvent evt) {
        int headCount = headDataFields.size();

        // Validate head data
        List<String> headDatas = new ArrayList<>();
        for (int i = 0; i < headCount; i++) {
            String data = headDataFields.get(i).getText().trim();
            if (data.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng nhập data cho Head #" + (i + 1) + "!",
                        "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }
            headDatas.add(data);
        }

        String bodyData = txtBodyData.getText().trim();
        String legData = txtLegData.getText().trim();
        String avatarIdStr = txtAvatarId.getText().trim();

        if (bodyData.isEmpty() || legData.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập đầy đủ data cho Body và Leg!",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (avatarIdStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập Avatar ID!",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int avatarId;
        try {
            avatarId = Integer.parseInt(avatarIdStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Avatar ID phải là số nguyên!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate skin item nếu checkbox bật
        boolean addItem = chkAddItem.isSelected();
        String iconIdStr = txtIconId.getText().trim();
        String itemName = txtItemName.getText().trim();
        int iconId = 0;
        if (addItem) {
            if (iconIdStr.isEmpty() || itemName.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng nhập Icon ID và Item Name!",
                        "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                iconId = Integer.parseInt(iconIdStr);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Icon ID phải là số nguyên!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Tính ID
        int firstHeadId = latestPartId + 1;
        int bodyId = latestPartId + headCount + 1;
        int legId = latestPartId + headCount + 2;

        // Xác nhận trước khi lưu
        StringBuilder confirmMsg = new StringBuilder("Xác nhận lưu Skin mới:\n\n");
        for (int i = 0; i < headCount; i++) {
            int headId = latestPartId + 1 + i;
            confirmMsg.append(String.format("Part Head #%d: id=%d, type=0, data=%s\n",
                    i + 1, headId, headDatas.get(i)));
        }
        confirmMsg.append(String.format("\nPart Body: id=%d, type=1, data=%s\n", bodyId, bodyData));
        confirmMsg.append(String.format("Part Leg:  id=%d, type=2, data=%s\n\n", legId, legData));
        confirmMsg.append(String.format("Head Avatar: head_id=%d, avatar_id=%d\n", firstHeadId, avatarId));
        if (headCount > 1) {
            StringBuilder headIds = new StringBuilder("[");
            for (int i = 0; i < headCount; i++) {
                if (i > 0)
                    headIds.append(", ");
                headIds.append(latestPartId + 1 + i);
            }
            headIds.append("]");
            confirmMsg.append(String.format("\narray_head_2_frames: data=%s\n", headIds.toString()));
        }
        if (addItem) {
            confirmMsg.append(String.format("\nSkin Item: id=%d, name=%s, icon=%d, head=%d, body=%d, leg=%d\n",
                    nextItemId, itemName, iconId, firstHeadId, bodyId, legId));
        }
        confirmMsg.append("\nBạn có chắc chắn muốn lưu?");

        int confirm = JOptionPane.showConfirmDialog(this, confirmMsg.toString(),
                "Xác nhận lưu", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        btnSave.setEnabled(false);
        setStatus("Đang lưu...", new Color(255, 193, 7));

        final int fAvatarId = avatarId;
        final int fFirstHeadId = firstHeadId;
        final int fBodyId = bodyId;
        final int fLegId = legId;
        final boolean fAddItem = addItem;
        final int fIconId = iconId;
        final String fItemName = itemName;
        final int fItemId = nextItemId;

        new Thread(() -> {
            try {
                // Insert tất cả head parts (type=0)
                for (int i = 0; i < headCount; i++) {
                    int headId = latestPartId + 1 + i;
                    GirlkunDB.executeUpdate("GIRLKUN",
                            "INSERT INTO `part` (`id`, `type`, `data`) VALUES (?, ?, ?)",
                            headId, 0, headDatas.get(i));
                }

                // Insert part body (type=1)
                GirlkunDB.executeUpdate("GIRLKUN",
                        "INSERT INTO `part` (`id`, `type`, `data`) VALUES (?, ?, ?)",
                        fBodyId, 1, bodyData);

                // Insert part leg (type=2)
                GirlkunDB.executeUpdate("GIRLKUN",
                        "INSERT INTO `part` (`id`, `type`, `data`) VALUES (?, ?, ?)",
                        fLegId, 2, legData);

                // Insert head_avatar (dùng ID head đầu tiên)
                GirlkunDB.executeUpdate("GIRLKUN",
                        "INSERT INTO `head_avatar` (`head_id`, `avatar_id`) VALUES (?, ?)",
                        fFirstHeadId, fAvatarId);

                // Insert array_head_2_frames nếu có > 1 head
                if (headCount > 1) {
                    StringBuilder headIdsData = new StringBuilder("[");
                    for (int i = 0; i < headCount; i++) {
                        if (i > 0)
                            headIdsData.append(", ");
                        headIdsData.append(latestPartId + 1 + i);
                    }
                    headIdsData.append("]");
                    GirlkunDB.executeUpdate("GIRLKUN",
                            "INSERT INTO `array_head_2_frames` (`id`, `data`) VALUES (NULL, ?)",
                            headIdsData.toString());
                }

                // Insert item_template nếu checkbox bật
                if (fAddItem) {
                    GirlkunDB.executeUpdate("GIRLKUN",
                            "INSERT INTO `item_template` (`id`, `TYPE`, `gender`, `NAME`, `description`, `level`, "
                                    + "`icon_id`, `part`, `is_up_to_up`, `power_require`, `gold`, `gem`, "
                                    + "`head`, `body`, `leg`, `is_up_to_up_over_99`, `can_trade`, `comment`, `spine_id`) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)",
                            fItemId, 5, 3, fItemName, fItemName, 1, fIconId, -1, 0,
                            150000000, 0, 0, fFirstHeadId, fBodyId, fLegId, 0, 1, "");
                }

                SwingUtilities.invokeLater(() -> {
                    setStatus(String.format(
                            "Lưu thành công! %d head(s), Body ID=%d, Leg ID=%d, Avatar ID=%d",
                            headCount, fBodyId, fLegId, fAvatarId), new Color(40, 167, 69));
                    btnSave.setEnabled(true);
                    // Reset form và refresh IDs sau khi lưu
                    clearForm();
                    loadNextIds();
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    setStatus("Lỗi khi lưu: " + e.getMessage(), Color.RED);
                    btnSave.setEnabled(true);
                    JOptionPane.showMessageDialog(AddSkinScr.this,
                            "Lỗi khi lưu vào DB:\n" + e.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void clearForm() {
        // Xóa các head thêm, chỉ giữ lại dòng đầu
        while (headDataFields.size() > 1) {
            int lastIdx = headDataFields.size() - 1;
            headDataFields.remove(lastIdx);
            headIdLabels.remove(lastIdx);
            JPanel panel = headRowPanels.remove(lastIdx);
            headRowsPanel.remove(panel);
        }
        headDataFields.get(0).setText("");

        txtBodyData.setText("");
        txtLegData.setText("");
        txtAvatarId.setText("");
        txtIconId.setText("");
        txtItemName.setText("");
        chkAddItem.setSelected(true);
        txtIconId.setEnabled(true);
        txtItemName.setEnabled(true);

        recalculateIds();
        headRowsPanel.revalidate();
        headRowsPanel.repaint();
        setStatus("Đã xóa form", new Color(108, 117, 125));
    }

    private void setStatus(String msg, Color color) {
        lblStatus.setText(msg);
        lblStatus.setForeground(color);
    }
}
