package com.girlkun.tool.screens.part_scr;

import com.girlkun.database.GirlkunDB;
import com.girlkun.result.GirlkunResultSet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.json.simple.JSONObject;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình thêm Skin mới vào DB
 * - Nhiều text box cho part head (type=0), có thể thêm/xóa động
 * - 1 text box cho body (type=1), 1 cho leg (type=2) → lưu vào table `part`
 * - Avatar ID và Head ID → lưu vào table `head_avatar`
 * - Các mục có dữ liệu được lưu độc lập sau khi xác nhận raw data.
 */
public class AddSkinScr extends JInternalFrame {

    private enum Section { ALL, HEAD, BODY, LEG, AVATAR, ITEM }
    private final List<JButton> sectionSaveButtons = new ArrayList<>();

    // Dynamic head fields
    private final List<JTextField> headDataFields = new ArrayList<>();
    private final List<JLabel> headIdLabels = new ArrayList<>();
    private final List<JPanel> headRowPanels = new ArrayList<>();
    private JPanel headRowsPanel;

    private JTextField txtBodyData;
    private JTextField txtLegData;
    private JTextField txtAvatarId;
    private JTextField txtAvatarHeadId;
    private JTextField txtItemHeadId;
    private JTextField txtItemBodyId;
    private JTextField txtItemLegId;
    private boolean idsLoaded;
    private boolean loadingIds;
    private boolean saving;

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
    private int nextArrayHead2FramesId = -1;
    private JComboBox<String> cbDbType;

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

        // === DB Type Section ===
        JPanel dbTypeSection = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dbTypeSection.setBorder(new EmptyBorder(0, 0, 10, 0));
        JLabel lblDbType = new JLabel("Loại DB:");
        lblDbType.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dbTypeSection.add(lblDbType);
        cbDbType = new JComboBox<>(new String[]{"GIRLKUN", "NRO ARN"});
        cbDbType.setSelectedItem("NRO ARN");
        cbDbType.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dbTypeSection.add(cbDbType);
        formPanel.add(dbTypeSection);

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
        addBtnPanel.add(sectionActions(Section.HEAD));

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
        watchPartData(txtBodyData);
        txtBodyData.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addRow(bodySection, gbcBody, 1, "Data (body):", txtBodyData);

        addRow(bodySection, gbcBody, 2, "", sectionActions(Section.BODY));
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
        watchPartData(txtLegData);
        txtLegData.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addRow(legSection, gbcLeg, 1, "Data (leg):", txtLegData);

        addRow(legSection, gbcLeg, 2, "", sectionActions(Section.LEG));
        formPanel.add(legSection);
        formPanel.add(Box.createVerticalStrut(8));

        // === Avatar Section ===
        JPanel avatarSection = new JPanel(new GridBagLayout());
        avatarSection.setBorder(styledBorder("Head Avatar"));
        GridBagConstraints gbcAvatar = createGbc();

        txtAvatarId = new JTextField(15);
        txtAvatarId.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addRow(avatarSection, gbcAvatar, 0, "Avatar ID:", txtAvatarId);

        txtAvatarHeadId = new JTextField(15);
        addRow(avatarSection, gbcAvatar, 1, "Head ID:", txtAvatarHeadId);

        JLabel lblAvatarNote = new JLabel(
                "Head ID tự điền từ head đầu tiên có data; nếu không, nhập ID head đã có.");
        lblAvatarNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblAvatarNote.setForeground(new Color(150, 150, 150));
        gbcAvatar.gridx = 0;
        gbcAvatar.gridy = 2;
        gbcAvatar.gridwidth = 2;
        avatarSection.add(lblAvatarNote, gbcAvatar);

        addRow(avatarSection, gbcAvatar, 3, "", sectionActions(Section.AVATAR));
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

        txtItemHeadId = new JTextField("-1", 5);
        txtItemBodyId = new JTextField("-1", 5);
        txtItemLegId = new JTextField("-1", 5);
        JPanel partIds = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        partIds.add(new JLabel("Head:"));
        partIds.add(txtItemHeadId);
        partIds.add(new JLabel("Body:"));
        partIds.add(txtItemBodyId);
        partIds.add(new JLabel("Leg:"));
        partIds.add(txtItemLegId);
        addRow(itemSection, gbcItem, 4, "Part ID:", partIds);

        JLabel itemNote = new JLabel("Bỏ trống để không lưu item. ID tự điền từ part có data; các ID còn lại nhập tay.");
        itemNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        gbcItem.gridx = 0;
        gbcItem.gridy = 5;
        gbcItem.gridwidth = 2;
        itemSection.add(itemNote, gbcItem);

        // Toggle enable/disable khi checkbox thay đổi
        chkAddItem.addActionListener(e -> {
            boolean enabled = chkAddItem.isSelected();
            txtIconId.setEnabled(enabled);
            txtItemName.setEnabled(enabled);
            txtItemHeadId.setEnabled(enabled);
            txtItemBodyId.setEnabled(enabled);
            txtItemLegId.setEnabled(enabled);
        });

        addRow(itemSection, gbcItem, 6, "", sectionActions(Section.ITEM));
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
        watchPartData(txtData);
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

    private void watchPartData(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { recalculateIds(); }
            public void removeUpdate(DocumentEvent e) { recalculateIds(); }
            public void changedUpdate(DocumentEvent e) { recalculateIds(); }
        });
    }

    private boolean hasData(JTextField field) {
        return field != null && !field.getText().trim().isEmpty();
    }

    // Only populated parts consume IDs. Automatically supplied references are read-only.
    private void recalculateIds() {
        int nextId = latestPartId + 1;
        Integer firstHead = null;
        for (int i = 0; i < headDataFields.size(); i++) {
            Integer id = idsLoaded && hasData(headDataFields.get(i)) ? nextId++ : null;
            headIdLabels.get(i).setText(id == null ? "ID: —" : "ID: " + id);
            if (firstHead == null && id != null) firstHead = id;
        }
        Integer body = idsLoaded && hasData(txtBodyData) ? nextId++ : null;
        Integer leg = idsLoaded && hasData(txtLegData) ? nextId++ : null;
        if (lblBodyId != null) lblBodyId.setText(body == null ? "ID: —" : "ID: " + body);
        if (lblLegId != null) lblLegId.setText(leg == null ? "ID: —" : "ID: " + leg);
        syncReference(txtAvatarHeadId, firstHead);
        syncReference(txtItemHeadId, firstHead);
        syncReference(txtItemBodyId, body);
        syncReference(txtItemLegId, leg);
    }

    private void syncReference(JTextField field, Integer id) {
        if (field == null) return;
        if (id != null) {
            field.setText(String.valueOf(id));
            field.setEditable(false);
        } else {
            if (!field.isEditable()) field.setText(field == txtAvatarHeadId ? "" : "-1");
            field.setEditable(true);
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
        if (loadingIds || saving) return;
        loadingIds = true;
        idsLoaded = false;
        btnSave.setEnabled(false);
        for (JButton button : sectionSaveButtons) button.setEnabled(false);
        btnRefreshId.setEnabled(false);
        recalculateIds();
        new Thread(() -> {
            try {
                int partId = getLatestPartId();
                int itemId = getLatestItemTemplateId() + 1;
                int arrayId = getLatestArrayHead2FramesId() + 1;
                SwingUtilities.invokeLater(() -> {
                    latestPartId = partId;
                    nextItemId = itemId;
                    nextArrayHead2FramesId = arrayId;
                    idsLoaded = true;
                    loadingIds = false;
                    recalculateIds();
                    lblItemId.setText("ID: " + nextItemId);
                    btnSave.setEnabled(true);
                    for (JButton button : sectionSaveButtons) button.setEnabled(true);
                    btnRefreshId.setEnabled(true);
                    setStatus("Đã tải ID thành công", new Color(40, 167, 69));
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    loadingIds = false;
                    btnRefreshId.setEnabled(true);
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

    /**
     * ARN không set AUTO_INCREMENT cho array_head_2_frames.id, nên không được insert NULL.
     */
    private int getLatestArrayHead2FramesId() throws Exception {
        GirlkunResultSet rs = GirlkunDB.executeQuery("GIRLKUN",
                "SELECT IFNULL(MAX(id), -1) as max_id FROM array_head_2_frames");
        if (rs.first()) {
            return rs.getInt("max_id");
        }
        return -1;
    }

    /** One immutable save snapshot supplies both the raw preview and bound SQL values. */
    private static final class InsertRow {
        final String table;
        final String[] columns;
        final Object[] values;

        InsertRow(String table, String columns, Object... values) {
            this.table = table;
            this.columns = columns.split(",");
            this.values = values;
            if (this.columns.length != values.length) throw new IllegalArgumentException("Column count");
        }

        String rawData() {
            StringBuilder raw = new StringBuilder(table).append("\n{\n");
            for (int i = 0; i < columns.length; i++) {
                raw.append("  ").append(columns[i]).append(": ");
                Object value = values[i];
                if (value instanceof String) raw.append('"').append(JSONObject.escape((String) value)).append('"');
                else raw.append(value);
                raw.append(i + 1 < columns.length ? ",\n" : "\n");
            }
            return raw.append("}\n\n").toString();
        }

        void insert(Connection connection) throws Exception {
            StringBuilder sql = new StringBuilder("INSERT INTO `").append(table).append("` (`");
            sql.append(String.join("`,`", columns)).append("`) VALUES (");
            for (int i = 0; i < values.length; i++) sql.append(i == 0 ? "?" : ",?");
            sql.append(")");
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
                statement.executeUpdate();
            }
        }
    }

    private int readId(JTextField field, String name) {
        return readId(field, name, 0);
    }

    private int readId(JTextField field, String name, int minimum) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            if (value < minimum) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " phải là số nguyên >= " + minimum + "!");
        }
    }

    private boolean includes(Section scope, Section section) {
        return scope == Section.ALL || scope == section;
    }

    private boolean manualItemReference(JTextField field) {
        return field.isEditable() && hasData(field) && !"-1".equals(field.getText().trim());
    }

    private List<InsertRow> buildSaveRows(Section scope) {
        List<InsertRow> rows = new ArrayList<>();
        List<Integer> headIds = new ArrayList<>();
        int nextId = latestPartId + 1;
        for (JTextField field : headDataFields) {
            if (hasData(field)) {
                headIds.add(nextId);
                if (includes(scope, Section.HEAD))
                    rows.add(new InsertRow("part", "id,type,data", nextId, 0, field.getText().trim()));
                nextId++;
            }
        }
        if (hasData(txtBodyData)) {
            if (includes(scope, Section.BODY))
                rows.add(new InsertRow("part", "id,type,data", nextId, 1, txtBodyData.getText().trim()));
            nextId++;
        }
        if (hasData(txtLegData) && includes(scope, Section.LEG))
            rows.add(new InsertRow("part", "id,type,data", nextId, 2, txtLegData.getText().trim()));
        if (includes(scope, Section.AVATAR) && hasData(txtAvatarId)) {
            if (scope == Section.AVATAR && !txtAvatarHeadId.isEditable())
                throw new IllegalArgumentException("Hãy lưu Head trước hoặc dùng nút Lưu vào DB chung để lưu cùng Avatar.");
            rows.add(new InsertRow("head_avatar", "head_id,avatar_id",
                    readId(txtAvatarHeadId, "Avatar Head ID"), readId(txtAvatarId, "Avatar ID")));
        }
        if (includes(scope, Section.HEAD) && headIds.size() > 1) {
            rows.add(new InsertRow("array_head_2_frames", "id,data", nextArrayHead2FramesId, headIds.toString()));
        }
        // Auto-filled references alone do not opt an otherwise empty item into saving.
        boolean itemEntered = hasData(txtIconId) || hasData(txtItemName)
                || manualItemReference(txtItemHeadId)
                || manualItemReference(txtItemBodyId)
                || manualItemReference(txtItemLegId);
        if (includes(scope, Section.ITEM) && (scope == Section.ITEM || chkAddItem.isSelected()) && itemEntered) {
            if (scope == Section.ITEM && (!txtItemHeadId.isEditable()
                    || !txtItemBodyId.isEditable() || !txtItemLegId.isEditable()))
                throw new IllegalArgumentException("Hãy lưu các Part đang nhập trước hoặc dùng nút Lưu vào DB chung để lưu cùng Item.");
            if (!hasData(txtItemName)) throw new IllegalArgumentException("Vui lòng nhập Item Name!");
            int icon = readId(txtIconId, "Icon ID");
            int head = readId(txtItemHeadId, "Item Head ID", -1);
            int body = readId(txtItemBodyId, "Item Body ID", -1);
            int leg = readId(txtItemLegId, "Item Leg ID", -1);
            String name = txtItemName.getText().trim();
            String columns = "id,TYPE,gender,NAME,description,level,icon_id,part,is_up_to_up,power_require,gold,gem,head,body,leg";
            if ("NRO ARN".equals(cbDbType.getSelectedItem())) {
                rows.add(new InsertRow("item_template", columns,
                        nextItemId, 5, 3, name, name, 1, icon, -1, 0, 150000000, 0, 0, head, body, leg));
            } else {
                rows.add(new InsertRow("item_template", columns + ",is_up_to_up_over_99,can_trade,comment,spine_id",
                        nextItemId, 5, 3, name, name, 1, icon, -1, 0, 150000000, 0, 0, head, body, leg, 0, 1, "", null));
            }
        }
        return rows;
    }

    private void onSave(ActionEvent evt) {
        saveSection(Section.ALL);
    }

    private void saveSection(Section scope) {
        if (!idsLoaded || loadingIds || saving) return;
        recalculateIds();
        final List<InsertRow> rows;
        try {
            rows = buildSaveRows(scope);
            if (rows.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập ít nhất một mục cần lưu!");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Kiểm tra dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StringBuilder raw = new StringBuilder("Loại DB: ").append(cbDbType.getSelectedItem())
                .append("\nCác bản ghi sẽ lưu (mục trống được bỏ qua):\n\n");
        for (InsertRow row : rows) raw.append(row.rawData());
        JTextArea preview = new JTextArea(raw.toString());
        preview.setEditable(false);
        preview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        preview.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(preview);
        scroll.setPreferredSize(new Dimension(720, 480));
        if (JOptionPane.showConfirmDialog(this, scroll, "Xác nhận raw data trước khi lưu",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) return;

        saving = true;
        btnSave.setEnabled(false);
        for (JButton button : sectionSaveButtons) button.setEnabled(false);
        btnRefreshId.setEnabled(false);
        setFormEnabled(false);
        setStatus("Đang lưu...", new Color(255, 193, 7));
        new Thread(() -> {
            try {
                try (Connection connection = GirlkunDB.getConnection("GIRLKUN")) {
                    connection.setAutoCommit(false);
                    try {
                        for (InsertRow row : rows) row.insert(connection);
                        connection.commit();
                    } catch (Exception e) {
                        try { connection.rollback(); } catch (Exception rollbackError) { e.addSuppressed(rollbackError); }
                        throw e;
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    saving = false;
                    setFormEnabled(true);
                    preserveSavedReferences(rows);
                    clearSection(scope);
                    setStatus("Đã lưu thành công " + rows.size() + " bản ghi.", new Color(40, 167, 69));
                    loadNextIds();
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    saving = false;
                    setFormEnabled(true);
                    btnSave.setEnabled(true);
                    for (JButton button : sectionSaveButtons) button.setEnabled(true);
                    btnRefreshId.setEnabled(true);
                    setStatus("Lỗi khi lưu: " + e.getMessage(), Color.RED);
                    JOptionPane.showMessageDialog(AddSkinScr.this, "Lỗi khi lưu vào DB:\n" + e.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void setFormEnabled(boolean enabled) {
        setChildrenEnabled(getContentPane(), enabled);
        if (enabled) {
            boolean itemEnabled = chkAddItem.isSelected();
            for (JTextField field : new JTextField[]{txtIconId, txtItemName, txtItemHeadId, txtItemBodyId, txtItemLegId})
                field.setEnabled(itemEnabled);
        }
    }

    private void setChildrenEnabled(Container parent, boolean enabled) {
        for (Component child : parent.getComponents()) {
            child.setEnabled(enabled);
            if (child instanceof Container) setChildrenEnabled((Container) child, enabled);
        }
    }

    private JPanel sectionActions(Section section) {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton clear = new JButton("Clear");
        styleBtn(clear, new Color(108, 117, 125));
        clear.addActionListener(e -> clearSection(section));
        JButton save = new JButton("Save DB");
        styleBtn(save, new Color(40, 167, 69));
        save.setEnabled(false);
        save.addActionListener(e -> saveSection(section));
        sectionSaveButtons.add(save);
        buttons.add(clear);
        buttons.add(save);
        return buttons;
    }

    // Keep newly persisted part IDs available for subsequent avatar/item saves.
    private void preserveSavedReferences(List<InsertRow> rows) {
        boolean firstHead = true;
        for (InsertRow row : rows) {
            if (!"part".equals(row.table)) continue;
            int type = (Integer) row.values[1];
            if (type == 0 && firstHead) {
                txtAvatarHeadId.setEditable(true);
                txtItemHeadId.setEditable(true);
                firstHead = false;
            } else if (type == 1) txtItemBodyId.setEditable(true);
            else if (type == 2) txtItemLegId.setEditable(true);
        }
    }

    private void clearForm() {
        clearSection(Section.ALL);
    }

    private void clearSection(Section scope) {
        if (includes(scope, Section.HEAD)) {
            // Remove extra rows before changing documents so IDs update only once.
            while (headDataFields.size() > 1) {
                int last = headDataFields.size() - 1;
                headDataFields.remove(last);
                headIdLabels.remove(last);
                headRowsPanel.remove(headRowPanels.remove(last));
            }
            headDataFields.get(0).setText("");
        }
        if (includes(scope, Section.BODY)) txtBodyData.setText("");
        if (includes(scope, Section.LEG)) txtLegData.setText("");
        if (includes(scope, Section.AVATAR)) {
            txtAvatarId.setText("");
            txtAvatarHeadId.setText("");
        }
        if (includes(scope, Section.ITEM)) {
            txtItemHeadId.setText("-1");
            txtItemBodyId.setText("-1");
            txtItemLegId.setText("-1");
            txtIconId.setText("");
            txtItemName.setText("");
            chkAddItem.setSelected(true);
            for (JTextField field : new JTextField[]{txtIconId, txtItemName, txtItemHeadId, txtItemBodyId, txtItemLegId})
                field.setEnabled(true);
        }
        recalculateIds();
        headRowsPanel.revalidate();
        headRowsPanel.repaint();
        setStatus("Đã xóa " + (scope == Section.ALL ? "form" : scope.name()), new Color(108, 117, 125));
    }

    private void setStatus(String msg, Color color) {
        lblStatus.setText(msg);
        lblStatus.setForeground(color);
    }
}
