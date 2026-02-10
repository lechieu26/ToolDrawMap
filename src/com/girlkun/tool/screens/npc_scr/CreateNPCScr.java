package com.girlkun.tool.screens.npc_scr;

import java.awt.FileDialog;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO.CaiTrangTemplate;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO.NpcFullInfo;
import com.girlkun.tool.shopmanager.services.ShopManagerDAO.PartData;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Create NPC Screen - Công cụ tạo NPC cho game NRO
 * Cho phép chọn avatar, chỉnh sửa vị trí các part (head, body, leg)
 * Hỗ trợ load NPC từ database
 */
public class CreateNPCScr extends JInternalFrame {

    // Cache thư mục đã chọn (dùng chung)
    private static String lastImageDir = System.getProperty("user.home");
    private static final String ICON_PATH = "data/data/icon/x4";

    // Avatar
    private BufferedImage avatarImage;
    private int avatarId = 0;
    private JPanel avatarPanel;
    private JLabel avatarIdLabel;

    // Parts
    private BufferedImage headImage;
    private BufferedImage bodyImage;
    private BufferedImage legImage;
    private int headId = 0, bodyId = 0, legId = 0;

    // Icon ID (tên file ảnh thực tế sau khi parse logic)
    private int headIconId = 0, bodyIconId = 0, legIconId = 0;

    private Point headPos = new Point(0, 0);
    private Point bodyPos = new Point(0, 0);
    private Point legPos = new Point(0, 0);

    // Skeleton offsets (Frame 0 - Standing)
    private static final int ZOOM_LEVEL = 4;
    private static final int SKEL_HEAD_X = -13;
    private static final int SKEL_HEAD_Y = 34; // Will be subtracted
    private static final int SKEL_BODY_X = -9;
    private static final int SKEL_BODY_Y = 16;
    private static final int SKEL_LEG_X = -8;
    private static final int SKEL_LEG_Y = 10;

    // Default Part Icon IDs (sử dụng khi chưa chọn ảnh)
    private static final int DEFAULT_HEAD_ICON_ID = 2187;
    private static final int DEFAULT_BODY_ICON_ID = 2188;
    private static final int DEFAULT_LEG_ICON_ID = 2189;

    // Canvas
    private NPCCanvas canvas;
    private String selectedPart = null; // "head", "body", "leg"

    // Output
    private JTextArea outputTextArea;

    // Part buttons
    private JButton btnHead, btnBody, btnLeg;

    // Part info labels
    private JLabel headInfoLabel, bodyInfoLabel, legInfoLabel;

    // NPC List table
    private JTable npcTable;
    private DefaultTableModel npcTableModel;
    private List<NpcFullInfo> npcList;

    // Current loaded NPC
    private NpcFullInfo currentNpc = null;

    // Cải trang flag - khi true thì save chỉ cập nhật npc_template, không tạo part
    // mới
    private boolean useCaiTrang = false;
    private String caiTrangName = "";

    public CreateNPCScr() {
        super("Create NPC", true, true, true, true);
        initComponents();
        setSize(1500, 850);

        // Load NPC list in background
        loadNpcListAsync();
    }

    private BufferedImage loadImageById(int id) {
        try {
            File f = new File(ICON_PATH + "/" + id + ".png");
            if (f.exists()) {
                return ImageIO.read(f);
            } else {
                // Try absolute path if relative fails
                f = new File(System.getProperty("user.dir") + "/" + ICON_PATH + "/" + id + ".png");
                if (f.exists()) {
                    return ImageIO.read(f);
                }
            }
        } catch (Exception e) {
            System.out.println("Cannot load image " + id + ": " + e.getMessage());
        }
        return null;
    }

    private int getIconIdFromPartData(String jsonData, String partType) {
        if (jsonData == null || jsonData.isEmpty())
            return -1;
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(jsonData);
            if (obj instanceof JSONArray) {
                JSONArray arr = (JSONArray) obj;

                // Body và Leg lấy frame index 1, Head lấy index 0
                int targetIndex = (partType.equalsIgnoreCase("body") || partType.equalsIgnoreCase("leg")) ? 1 : 0;

                // Fallback nếu không đủ phần tử thì lấy 0
                if (targetIndex >= arr.size() && targetIndex > 0)
                    targetIndex = 0;

                if (targetIndex < arr.size()) {
                    Object subObj = arr.get(targetIndex);
                    if (subObj instanceof JSONArray) {
                        JSONArray subArr = (JSONArray) subObj;
                        if (subArr.size() >= 1) {
                            long id = (Long) subArr.get(0);
                            if (id > 0)
                                return (int) id;
                        }
                    }
                }

                // Fallback cũ: duyệt tìm cái đầu tiên > 0
                for (Object subObj : arr) {
                    if (subObj instanceof JSONArray) {
                        JSONArray subArr = (JSONArray) subObj;
                        if (subArr.size() >= 1) {
                            long id = (Long) subArr.get(0);
                            if (id > 0)
                                return (int) id;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Far left panel - NPC List
        JPanel npcListPanel = createNpcListPanel();
        mainPanel.add(npcListPanel, BorderLayout.WEST);

        // Center panel contains Left panel + Canvas
        JPanel centerContainer = new JPanel(new BorderLayout(10, 10));

        // Left panel - Avatar và Part buttons
        JPanel leftPanel = createLeftPanel();
        centerContainer.add(leftPanel, BorderLayout.WEST);

        // Center - Canvas (cố định vừa khung, không scroll)
        canvas = new NPCCanvas();
        JPanel canvasPanel = new JPanel(new BorderLayout());
        canvasPanel.setBorder(BorderFactory.createTitledBorder("Canvas - Chỉnh vị trí Part (Kéo thả)"));
        canvasPanel.add(canvas, BorderLayout.CENTER);
        centerContainer.add(canvasPanel, BorderLayout.CENTER);

        mainPanel.add(centerContainer, BorderLayout.CENTER);

        // Right panel - Output
        JPanel rightPanel = createRightPanel();
        mainPanel.add(rightPanel, BorderLayout.EAST);

        setContentPane(mainPanel);
    }

    private void showNewNpcDialog() {
        // Create Dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Tạo NPC Mới",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(350, 200);
        dialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // ID Field (Auto-generated)
        JPanel idPanel = new JPanel(new BorderLayout(5, 5));
        idPanel.add(new JLabel("NPC ID (Auto): "), BorderLayout.WEST);
        JTextField txtId = new JTextField("Loading...");
        txtId.setEditable(false);
        txtId.setFont(new Font("SansSerif", Font.BOLD, 12));
        idPanel.add(txtId, BorderLayout.CENTER);
        contentPanel.add(idPanel);

        // Name Field
        JPanel namePanel = new JPanel(new BorderLayout(5, 5));
        namePanel.add(new JLabel("NPC Name: "), BorderLayout.WEST);
        JTextField txtName = new JTextField();
        namePanel.add(txtName, BorderLayout.CENTER);
        contentPanel.add(namePanel);

        // Note Label
        JLabel lblNote = new JLabel(
                "<html><i>* Lưu ý: Sau khi tạo, hãy chọn NPC trong danh sách để chỉnh sửa.</i></html>");
        lblNote.setForeground(Color.GRAY);
        contentPanel.add(lblNote);

        dialog.add(contentPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Lưu (Save)");
        JButton btnCancel = new JButton("Hủy (Cancel)");

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        // Logic
        final int[] nextId = { 0 };

        // Async fetch max ID
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                return ShopManagerDAO.gI().getMaxNpcId();
            }

            @Override
            protected void done() {
                try {
                    nextId[0] = get() + 1;
                    txtId.setText(String.valueOf(nextId[0]));
                } catch (Exception e) {
                    txtId.setText("Error");
                }
            }
        };
        worker.execute();

        btnCancel.addActionListener(e -> dialog.dispose());

        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên NPC!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Save to DB
            try {
                ShopManagerDAO.gI().addNewNpc(nextId[0], name);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Tạo NPC thành công!", "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);

                // Reload list and select new NPC
                loadNpcListAsyncAndSelect(nextId[0]);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi khi lưu DB: " + ex.getMessage(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private void showRenameNpcDialog() {
        if (currentNpc == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn NPC muốn đổi tên!", "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create Dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Đổi tên NPC",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(350, 250);
        dialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // ID Field
        JPanel idPanel = new JPanel(new BorderLayout(5, 5));
        idPanel.add(new JLabel("NPC ID: "), BorderLayout.WEST);
        JTextField txtId = new JTextField(String.valueOf(currentNpc.id));
        txtId.setEditable(false);
        idPanel.add(txtId, BorderLayout.CENTER);
        contentPanel.add(idPanel);

        // Old Name Field
        JPanel oldNamePanel = new JPanel(new BorderLayout(5, 5));
        oldNamePanel.add(new JLabel("Tên cũ: "), BorderLayout.WEST);
        JTextField txtOldName = new JTextField(currentNpc.name);
        txtOldName.setEditable(false);
        oldNamePanel.add(txtOldName, BorderLayout.CENTER);
        contentPanel.add(oldNamePanel);

        // New Name Field
        JPanel newNamePanel = new JPanel(new BorderLayout(5, 5));
        newNamePanel.add(new JLabel("Tên mới: "), BorderLayout.WEST);
        JTextField txtNewName = new JTextField(currentNpc.name);
        newNamePanel.add(txtNewName, BorderLayout.CENTER);
        contentPanel.add(newNamePanel);

        dialog.add(contentPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Lưu (Save)");
        JButton btnCancel = new JButton("Hủy (Cancel)");

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dialog.dispose());

        btnSave.addActionListener(e -> {
            String newName = txtNewName.getText().trim();
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Tên mới không được để trống!", "Lỗi",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (newName.equals(currentNpc.name)) {
                dialog.dispose();
                return;
            }

            // Save to DB
            try {
                ShopManagerDAO.gI().updateNpcName(currentNpc.id, newName);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Đổi tên thành công!", "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);

                // Reload list
                loadNpcListAsyncAndSelect(currentNpc.id);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi khi cập nhật DB: " + ex.getMessage(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private void loadNpcListAsyncAndSelect(int selectId) {
        SwingWorker<List<NpcFullInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<NpcFullInfo> doInBackground() {
                return ShopManagerDAO.gI().getNpcsWithFullInfo();
            }

            @Override
            protected void done() {
                try {
                    npcList = get();
                    npcTableModel.setRowCount(0);
                    int selectRow = -1;
                    for (int i = 0; i < npcList.size(); i++) {
                        NpcFullInfo npc = npcList.get(i);
                        npcTableModel.addRow(new Object[] { npc.id, npc.name });
                        if (npc.id == selectId) {
                            selectRow = i;
                        }
                    }
                    if (selectRow != -1) {
                        npcTable.setRowSelectionInterval(selectRow, selectRow);
                        npcTable.scrollRectToVisible(npcTable.getCellRect(selectRow, 0, true));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private JPanel createNpcListPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(250, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Danh sách NPC"));

        // Table model
        npcTableModel = new DefaultTableModel(new Object[] { "ID", "Name" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        npcTable = new JTable(npcTableModel);
        npcTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        npcTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        npcTable.getColumnModel().getColumn(0).setMaxWidth(60);
        npcTable.getColumnModel().getColumn(1).setPreferredWidth(180);

        // Row selection listener
        npcTable.getSelectionModel().addListSelectionListener(this::onNpcSelected);

        JScrollPane scrollPane = new JScrollPane(npcTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons Panel
        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        // Refresh button
        JButton btnRefresh = new JButton("Làm mới danh sách");
        btnRefresh.setBackground(new Color(70, 130, 180));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.addActionListener(e -> loadNpcListAsync());

        // New NPC Button
        JButton btnNewNpc = new JButton("New NPC (Tạo mới)");
        btnNewNpc.setBackground(new Color(40, 167, 69));
        btnNewNpc.setForeground(Color.WHITE);
        btnNewNpc.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnNewNpc.addActionListener(e -> showNewNpcDialog());

        // Rename NPC Button
        JButton btnRenameNpc = new JButton("Đổi tên (Rename)");
        btnRenameNpc.setBackground(new Color(255, 193, 7)); // Amber/Orange color
        btnRenameNpc.setForeground(Color.BLACK);
        btnRenameNpc.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnRenameNpc.addActionListener(e -> showRenameNpcDialog());

        btnPanel.add(btnRefresh);
        btnPanel.add(btnNewNpc);
        btnPanel.add(btnRenameNpc);

        // Update layout to 3 rows
        btnPanel.setLayout(new GridLayout(3, 1, 5, 5));

        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadNpcListAsync() {
        SwingWorker<List<NpcFullInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<NpcFullInfo> doInBackground() {
                return ShopManagerDAO.gI().getNpcsWithFullInfo();
            }

            @Override
            protected void done() {
                try {
                    npcList = get();
                    npcTableModel.setRowCount(0);
                    for (NpcFullInfo npc : npcList) {
                        npcTableModel.addRow(new Object[] { npc.id, npc.name });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(CreateNPCScr.this,
                            "Không thể load danh sách NPC: " + e.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void onNpcSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting())
            return;

        int selectedRow = npcTable.getSelectedRow();
        if (selectedRow < 0 || npcList == null || selectedRow >= npcList.size())
            return;

        NpcFullInfo npc = npcList.get(selectedRow);
        loadNpcData(npc);
    }

    private void loadNpcData(NpcFullInfo npc) {
        currentNpc = npc;

        // Clear current images
        avatarImage = null;
        headImage = null;
        bodyImage = null;
        legImage = null;

        // Update IDs (Part ID)
        avatarId = npc.avatar;
        headId = npc.head;
        bodyId = npc.body;
        legId = npc.leg;

        // Reset Icon IDs
        headIconId = 0;
        bodyIconId = 0;
        legIconId = 0;

        // Reset positions
        headPos = new Point(0, 0);
        bodyPos = new Point(0, 0);
        legPos = new Point(0, 0);

        // Load avatar image immediately (if small) or async
        new Thread(() -> {
            avatarImage = loadImageById(avatarId);
            SwingUtilities.invokeLater(() -> avatarPanel.repaint());
        }).start();

        // Load part data from database and parse positions + load images
        loadPartDataAsync(npc);

        // Update UI
        avatarIdLabel.setText("ID: " + avatarId);
        updatePartInfoLabels();
        avatarPanel.repaint();
        canvas.repaint();

        // Generate output data
        generateOutputFromDatabase();
    }

    private void loadPartDataAsync(NpcFullInfo npc) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            PartData headPartData, bodyPartData, legPartData;
            BufferedImage imgHead, imgBody, imgLeg;
            int hIcon = -1, bIcon = -1, lIcon = -1;

            @Override
            protected Void doInBackground() {
                // 1. Get Data
                headPartData = ShopManagerDAO.gI().getPartData(npc.head);
                bodyPartData = ShopManagerDAO.gI().getPartData(npc.body);
                legPartData = ShopManagerDAO.gI().getPartData(npc.leg);

                // 2. Parse & Load Images with correct frame logic
                if (headPartData != null) {
                    hIcon = getIconIdFromPartData(headPartData.data, "head");
                    if (hIcon > 0)
                        imgHead = loadImageById(hIcon);
                }
                if (bodyPartData != null) {
                    bIcon = getIconIdFromPartData(bodyPartData.data, "body");
                    if (bIcon > 0)
                        imgBody = loadImageById(bIcon);
                }
                if (legPartData != null) {
                    lIcon = getIconIdFromPartData(legPartData.data, "leg");
                    if (lIcon > 0)
                        imgLeg = loadImageById(lIcon);
                }
                return null;
            }

            @Override
            protected void done() {
                // Set images
                headImage = imgHead;
                bodyImage = imgBody;
                legImage = imgLeg;

                // Update Icon IDs
                headIconId = hIcon;
                bodyIconId = bIcon;
                legIconId = lIcon;

                // Parse positions from part data
                if (headPartData != null) {
                    parseAndSetPosition(headPartData.data, "head");
                }
                if (bodyPartData != null) {
                    parseAndSetPosition(bodyPartData.data, "body");
                }
                if (legPartData != null) {
                    parseAndSetPosition(legPartData.data, "leg");
                }

                updatePartInfoLabels();
                canvas.repaint();
                generateOutputFromDatabase();
            }
        };
        worker.execute();
    }

    private void parseAndSetPosition(String jsonData, String partType) {
        if (jsonData == null || jsonData.isEmpty())
            return;

        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(jsonData);
            if (obj instanceof JSONArray) {
                JSONArray arr = (JSONArray) obj;

                // Body/Leg lấy index 1, Head index 0
                int targetIndex = (partType.equalsIgnoreCase("body") || partType.equalsIgnoreCase("leg")) ? 1 : 0;
                // Fallback nếu không đủ phần tử
                if (targetIndex >= arr.size() && targetIndex > 0)
                    targetIndex = 0;

                // Ưu tiên lấy frame target nếu valid
                if (targetIndex < arr.size()) {
                    Object firstFrame = arr.get(targetIndex);
                    if (firstFrame instanceof JSONArray) {
                        JSONArray frameData = (JSONArray) firstFrame;
                        if (frameData.size() >= 3) {
                            long id = (Long) frameData.get(0);
                            long x = (Long) frameData.get(1);
                            long y = (Long) frameData.get(2);

                            // Nếu id > 0, lấy luôn tọa độ frame này
                            if (id > 0) {
                                switch (partType) {
                                    case "head":
                                        headPos = new Point((int) x * 4, (int) y * 4);
                                        break;
                                    case "body":
                                        bodyPos = new Point((int) x * 4, (int) y * 4);
                                        break;
                                    case "leg":
                                        legPos = new Point((int) x * 4, (int) y * 4);
                                        break;
                                }
                                return;
                            }
                        }
                    }
                }

                // Fallback cũ: duyệt hết
                for (Object subObj : arr) {
                    if (subObj instanceof JSONArray) {
                        JSONArray subArr = (JSONArray) subObj;
                        if (subArr.size() >= 3) {
                            long id = (Long) subArr.get(0);
                            long x = (Long) subArr.get(1);
                            long y = (Long) subArr.get(2);
                            if (id != 0 || x != 0 || y != 0) {
                                switch (partType) {
                                    case "head":
                                        headPos = new Point((int) x * 4, (int) y * 4);
                                        break;
                                    case "body":
                                        bodyPos = new Point((int) x * 4, (int) y * 4);
                                        break;
                                    case "leg":
                                        legPos = new Point((int) x * 4, (int) y * 4);
                                        break;
                                }
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing part data: " + e.getMessage());
        }
    }

    private void generateOutputFromDatabase() {
        if (currentNpc == null)
            return;

        StringBuilder sb = new StringBuilder();
        sb.append("=== NPC: ").append(currentNpc.name).append(" (ID: ").append(currentNpc.id).append(") ===\n\n");
        sb.append("Avatar ID: ").append(currentNpc.avatar).append("\n\n");
        sb.append("Part IDs:\n");
        sb.append("- Head Part ID: ").append(currentNpc.head).append("\n");
        sb.append("- Body Part ID: ").append(currentNpc.body).append("\n");
        sb.append("- Leg Part ID: ").append(currentNpc.leg).append("\n\n");

        // Load and display path data
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                StringBuilder data = new StringBuilder();
                PartData headPart = ShopManagerDAO.gI().getPartData(currentNpc.head);
                PartData bodyPart = ShopManagerDAO.gI().getPartData(currentNpc.body);
                PartData legPart = ShopManagerDAO.gI().getPartData(currentNpc.leg);

                data.append("=== Path Data ===\n\n");
                data.append("Head Path (Type 0):\n");
                data.append(headPart != null ? headPart.data : "N/A").append("\n\n");
                data.append("Body Path (Type 1):\n");
                data.append(bodyPart != null ? bodyPart.data : "N/A").append("\n\n");
                data.append("Leg Path (Type 2):\n");
                data.append(legPart != null ? legPart.data : "N/A");

                return data.toString();
            }

            @Override
            protected void done() {
                try {
                    String pathData = get();
                    outputTextArea.setText(sb.toString() + pathData);
                } catch (Exception e) {
                    outputTextArea.setText(sb.toString() + "Error loading path data: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setPreferredSize(new Dimension(220, 0));

        // Avatar section
        JPanel avatarSection = new JPanel(new BorderLayout(5, 5));
        avatarSection.setBorder(BorderFactory.createTitledBorder("Avatar NPC"));

        avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight()) - 10;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // Vẽ hình tròn
                Ellipse2D.Double circle = new Ellipse2D.Double(x, y, size, size);

                if (avatarImage != null) {
                    // Clip theo hình tròn
                    g2d.setClip(circle);
                    g2d.drawImage(avatarImage, x, y, size, size, null);
                    g2d.setClip(null);
                } else {
                    g2d.setColor(new Color(60, 60, 60));
                    g2d.fill(circle);
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    FontMetrics fm = g2d.getFontMetrics();
                    String text = "Click để chọn";
                    int textX = (getWidth() - fm.stringWidth(text)) / 2;
                    int textY = getHeight() / 2 + fm.getAscent() / 2;
                    g2d.drawString(text, textX, textY);
                }

                // Viền tròn
                g2d.setColor(new Color(100, 100, 255));
                g2d.setStroke(new BasicStroke(3));
                g2d.draw(circle);

                g2d.dispose();
            }
        };
        avatarPanel.setPreferredSize(new Dimension(150, 150));
        avatarPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        avatarPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectAvatarImage();
            }
        });

        // Create a wrapper for Avatar section to hold Image and Bottom Controls
        JPanel avatarContainer = new JPanel(new BorderLayout());
        avatarContainer.add(avatarPanel, BorderLayout.CENTER);

        JPanel avatarInfoPanel = new JPanel(new BorderLayout(5, 5));
        avatarIdLabel = new JLabel("ID: 0");
        avatarIdLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarInfoPanel.add(avatarIdLabel, BorderLayout.NORTH);

        JButton btnSaveAvatar = new JButton("Save Avatar");
        btnSaveAvatar.setFont(new Font("SansSerif", Font.BOLD, 10));
        btnSaveAvatar.setBackground(new Color(13, 110, 253));
        btnSaveAvatar.setForeground(Color.WHITE);
        btnSaveAvatar.addActionListener(e -> {
            if (currentNpc == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn NPC trước!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (avatarImage == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn ảnh Avatar!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // Update avatar ID for current NPC in DB
                ShopManagerDAO.gI().updateNpcAvatar(currentNpc.id, avatarId);
                JOptionPane.showMessageDialog(this, "Lưu Avatar thành công!", "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);

                // Reload list to reflect changes if needed (though avatar ID isn't shown in
                // list usually)
                loadNpcListAsyncAndSelect(currentNpc.id);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi lưu DB: " + ex.getMessage(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        avatarInfoPanel.add(btnSaveAvatar, BorderLayout.SOUTH);
        avatarContainer.add(avatarInfoPanel, BorderLayout.SOUTH);

        avatarSection.add(avatarContainer, BorderLayout.CENTER);

        leftPanel.add(avatarSection);
        leftPanel.add(Box.createVerticalStrut(15));

        // Part selection section
        JPanel partSection = new JPanel();
        partSection.setLayout(new BoxLayout(partSection, BoxLayout.Y_AXIS));
        partSection.setBorder(BorderFactory.createTitledBorder("Chọn Part"));

        // Head button
        btnHead = createPartSelectButton("Chọn Head", new Color(255, 100, 100), "head");
        partSection.add(btnHead);
        headInfoLabel = new JLabel("PartID: 0 | Icon: 0 | Pos: (0, 0)");
        headInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headInfoLabel.setFont(new Font("Consolas", Font.PLAIN, 10));
        partSection.add(headInfoLabel);
        partSection.add(Box.createVerticalStrut(8));

        // Body button
        btnBody = createPartSelectButton("Chọn Body", new Color(100, 255, 100), "body");
        partSection.add(btnBody);
        bodyInfoLabel = new JLabel("PartID: 0 | Icon: 0 | Pos: (0, 0)");
        bodyInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bodyInfoLabel.setFont(new Font("Consolas", Font.PLAIN, 10));
        partSection.add(bodyInfoLabel);
        partSection.add(Box.createVerticalStrut(8));

        // Leg button
        btnLeg = createPartSelectButton("Chọn Leg", new Color(100, 100, 255), "leg");
        partSection.add(btnLeg);
        legInfoLabel = new JLabel("PartID: 0 | Icon: 0 | Pos: (0, 0)");
        legInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        legInfoLabel.setFont(new Font("Consolas", Font.PLAIN, 10));
        partSection.add(legInfoLabel);
        partSection.add(Box.createVerticalStrut(10));

        // Cải trang button
        JButton btnCaiTrang = new JButton("Chọn Cải trang");
        btnCaiTrang.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCaiTrang.setMaximumSize(new Dimension(180, 40));
        btnCaiTrang.setBackground(new Color(156, 39, 176)); // Purple color
        btnCaiTrang.setForeground(Color.WHITE);
        btnCaiTrang.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCaiTrang.setFocusPainted(false);
        btnCaiTrang.addActionListener(e -> showCaiTrangDialog());
        partSection.add(btnCaiTrang);

        leftPanel.add(partSection);
        leftPanel.add(Box.createVerticalStrut(15));

        // Position info
        JPanel posPanel = new JPanel();
        posPanel.setLayout(new BoxLayout(posPanel, BoxLayout.Y_AXIS));
        posPanel.setBorder(BorderFactory.createTitledBorder("Điều khiển"));

        JLabel infoLabel = new JLabel(
                "<html><center>Di chuyển part:<br>- Kéo thả chuột<br>- Phím mũi tên<br>- Shift + mũi tên (x10)</center></html>");
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        posPanel.add(infoLabel);
        posPanel.add(Box.createVerticalStrut(10));

        // Reset position button
        JButton btnReset = new JButton("Reset vị trí");
        btnReset.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnReset.setMaximumSize(new Dimension(180, 30));
        btnReset.addActionListener(e -> resetPositions());
        posPanel.add(btnReset);

        leftPanel.add(posPanel);
        leftPanel.add(Box.createVerticalGlue());

        return leftPanel;
    }

    private JButton createPartSelectButton(String name, Color color, String partType) {
        JButton btn = new JButton(name);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(180, 40));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);

        btn.addActionListener(e -> {
            selectPartImage(partType);
        });

        return btn;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setPreferredSize(new Dimension(350, 0));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Output Data (Path từ Database)"));

        outputTextArea = new JTextArea();
        outputTextArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        outputTextArea.setEditable(true);
        outputTextArea.setBackground(new Color(40, 40, 40));
        outputTextArea.setForeground(new Color(200, 200, 200));
        outputTextArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 5, 5)); // 2 rows, 2 cols

        JButton btnExport = new JButton("Xuất Data (Manual)");
        btnExport.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnExport.setBackground(new Color(0, 153, 76));
        btnExport.setForeground(Color.WHITE);
        btnExport.addActionListener(e -> exportData());

        JButton btnCopy = new JButton("Copy");
        btnCopy.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnCopy.addActionListener(e -> {
            outputTextArea.selectAll();
            outputTextArea.copy();
            JOptionPane.showMessageDialog(this, "Đã copy vào clipboard!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnSavePart = new JButton("Lưu Part & NPC");
        btnSavePart.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnSavePart.setBackground(new Color(23, 162, 184));
        btnSavePart.setForeground(Color.WHITE);
        btnSavePart.addActionListener(e -> savePartToDB());

        btnPanel.add(btnExport);
        btnPanel.add(btnCopy);
        btnPanel.add(btnSavePart);

        rightPanel.add(btnPanel, BorderLayout.SOUTH);

        return rightPanel;
    }

    private void savePartToDB() {
        if (currentNpc == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn NPC trước!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // === Trường hợp sử dụng Cải trang ===
        // Khi dùng cải trang, các part ID đã tồn tại trong DB nên chỉ cần cập nhật
        // npc_template
        if (useCaiTrang) {
            saveCaiTrangToNpc();
            return;
        }

        // === Trường hợp thường (chọn part thủ công) ===
        // Kiểm tra xem có bộ phận nào được chọn không
        boolean hasHead = headIconId > 0;
        boolean hasBody = bodyIconId > 0;
        boolean hasLeg = legIconId > 0;

        // Kiểm tra xem NPC đã có các part chưa (để xác định Update hay New)
        boolean headIsUpdate = currentNpc.head > 0 && currentNpc.head != -1;
        boolean bodyIsUpdate = currentNpc.body > 0 && currentNpc.body != -1;
        boolean legIsUpdate = currentNpc.leg > 0 && currentNpc.leg != -1;

        // Kiểm tra xem có thay đổi gì không
        // Có thay đổi nếu:
        // - Có chọn part mới (hasHead/hasBody/hasLeg = true) và chưa có part đó
        // - Hoặc đang update part đã có
        boolean hasAnyChange = hasHead || hasBody || hasLeg;

        if (!hasAnyChange) {
            // Kiểm tra xem NPC đã có đủ part chưa
            boolean npcHasAllDefault = (currentNpc.head == DEFAULT_HEAD_ICON_ID || currentNpc.head == -1
                    || currentNpc.head == 0)
                    && (currentNpc.body == DEFAULT_BODY_ICON_ID || currentNpc.body == -1 || currentNpc.body == 0)
                    && (currentNpc.leg == DEFAULT_LEG_ICON_ID || currentNpc.leg == -1 || currentNpc.leg == 0);

            if (npcHasAllDefault) {
                JOptionPane.showMessageDialog(this,
                        "No change - Không có thay đổi!\n\nNPC đang sử dụng tất cả default part và bạn chưa chọn ảnh nào mới.",
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            int savedPartCount = 0;
            int finalHeadPartId, finalBodyPartId, finalLegPartId;

            @Override
            protected Void doInBackground() {
                try {
                    int currentMax = ShopManagerDAO.gI().getMaxPartId();

                    // Tính toán Part ID sẽ được tạo mới hoặc reuse
                    int[] targetIds = calculateTargetPartIds(currentMax);
                    int saveHeadId = targetIds[0];
                    int saveBodyId = targetIds[1];
                    int saveLegId = targetIds[2];

                    // Tọa độ DB
                    int hX = headPos.x / 4;
                    int hY = headPos.y / 4;
                    int bX = bodyPos.x / 4;
                    int bY = bodyPos.y / 4;
                    int lX = legPos.x / 4;
                    int lY = legPos.y / 4;

                    // Build message cho Part
                    StringBuilder msgBuilder = new StringBuilder();
                    msgBuilder.append("=== LƯU PART & CẬP NHẬT NPC ===\n\n");
                    msgBuilder.append("NPC: ").append(currentNpc.name).append(" (ID: ").append(currentNpc.id)
                            .append(")\n\n");
                    msgBuilder.append("--- PART SẼ LƯU ---\n\n");

                    String sbHead = null, sbBody = null, sbLeg = null;

                    // 1. Head Data
                    if (hasHead) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("[[").append(headIconId).append(",").append(hX).append(",").append(hY).append("]");
                        for (int i = 0; i < 2; i++)
                            sb.append(",[2955,0,0]");
                        sb.append("]");
                        sbHead = sb.toString();
                        String headStatus = headIsUpdate ? "[UPDATE]" : "[NEW]";
                        msgBuilder.append("HEAD ").append(headStatus).append(" (ID: ").append(saveHeadId).append("):\n")
                                .append(sbHead).append("\n\n");
                    } else {
                        msgBuilder.append("HEAD: Bỏ qua (dùng default ").append(DEFAULT_HEAD_ICON_ID).append(")\n\n");
                    }

                    // 2. Body Data
                    if (hasBody) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("[[2955,0,0],[").append(bodyIconId).append(",").append(bX).append(",").append(bY)
                                .append("]");
                        for (int i = 0; i < 15; i++)
                            sb.append(",[2955,0,0]");
                        sb.append("]");
                        sbBody = sb.toString();
                        String bodyStatus = bodyIsUpdate ? "[UPDATE]" : "[NEW]";
                        msgBuilder.append("BODY ").append(bodyStatus).append(" (ID: ").append(saveBodyId).append("):\n")
                                .append(sbBody).append("\n\n");
                    } else {
                        msgBuilder.append("BODY: Bỏ qua (dùng default ").append(DEFAULT_BODY_ICON_ID).append(")\n\n");
                    }

                    // 3. Leg Data
                    if (hasLeg) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("[[2955,0,0],[").append(legIconId).append(",").append(lX).append(",").append(lY)
                                .append("]");
                        for (int i = 0; i < 12; i++)
                            sb.append(",[2955,0,0]");
                        sb.append("]");
                        sbLeg = sb.toString();
                        String legStatus = legIsUpdate ? "[UPDATE]" : "[NEW]";
                        msgBuilder.append("LEG ").append(legStatus).append(" (ID: ").append(saveLegId).append("):\n")
                                .append(sbLeg).append("\n\n");
                    } else {
                        msgBuilder.append("LEG: Bỏ qua (dùng default ").append(DEFAULT_LEG_ICON_ID).append(")\n\n");
                    }

                    // Tính Part ID cuối cùng cho NPC
                    // - Nếu đã chọn ảnh mới: sử dụng saveHeadId/saveBodyId/saveLegId (đã được tính
                    // từ calculateTargetPartIds)
                    // - Nếu không chọn ảnh mới: giữ nguyên Part ID cũ của NPC (nếu có), hoặc dùng
                    // default
                    finalHeadPartId = hasHead ? saveHeadId
                            : (currentNpc.head > 0 ? currentNpc.head : DEFAULT_HEAD_ICON_ID);
                    finalBodyPartId = hasBody ? saveBodyId
                            : (currentNpc.body > 0 ? currentNpc.body : DEFAULT_BODY_ICON_ID);
                    finalLegPartId = hasLeg ? saveLegId : (currentNpc.leg > 0 ? currentNpc.leg : DEFAULT_LEG_ICON_ID);

                    // Kiểm tra có thay đổi gì không
                    boolean npcHeadChanged = finalHeadPartId != currentNpc.head;
                    boolean npcBodyChanged = finalBodyPartId != currentNpc.body;
                    boolean npcLegChanged = finalLegPartId != currentNpc.leg;
                    boolean hasNpcChange = npcHeadChanged || npcBodyChanged || npcLegChanged;

                    msgBuilder.append("--- NPC SẼ CẬP NHẬT ---\n\n");
                    if (!hasNpcChange) {
                        msgBuilder.append("(Không có thay đổi - giữ nguyên Part IDs)\n\n");
                    }

                    // Hiển thị trạng thái cho từng Part:
                    // - [UPDATE]: đã có part và đang thay đổi
                    // - [NEW]: chưa có part và đang thêm mới
                    // - (giữ nguyên): không thay đổi
                    // - (default): dùng default
                    String headNpcStatus;
                    if (hasHead) {
                        headNpcStatus = headIsUpdate ? " [UPDATE]" : " [NEW]";
                    } else if (npcHeadChanged) {
                        headNpcStatus = " [CHANGED]";
                    } else {
                        headNpcStatus = currentNpc.head > 0 ? " (giữ nguyên)" : " (default)";
                    }

                    String bodyNpcStatus;
                    if (hasBody) {
                        bodyNpcStatus = bodyIsUpdate ? " [UPDATE]" : " [NEW]";
                    } else if (npcBodyChanged) {
                        bodyNpcStatus = " [CHANGED]";
                    } else {
                        bodyNpcStatus = currentNpc.body > 0 ? " (giữ nguyên)" : " (default)";
                    }

                    String legNpcStatus;
                    if (hasLeg) {
                        legNpcStatus = legIsUpdate ? " [UPDATE]" : " [NEW]";
                    } else if (npcLegChanged) {
                        legNpcStatus = " [CHANGED]";
                    } else {
                        legNpcStatus = currentNpc.leg > 0 ? " (giữ nguyên)" : " (default)";
                    }

                    msgBuilder.append("Head Part ID: ").append(finalHeadPartId).append(headNpcStatus).append("\n");
                    msgBuilder.append("Body Part ID: ").append(finalBodyPartId).append(bodyNpcStatus).append("\n");
                    msgBuilder.append("Leg Part ID: ").append(finalLegPartId).append(legNpcStatus);

                    int option = JOptionPane.showConfirmDialog(CreateNPCScr.this, msgBuilder.toString(),
                            "Xác nhận Lưu Part & Cập nhật NPC",
                            JOptionPane.YES_NO_OPTION);

                    if (option == JOptionPane.YES_OPTION) {
                        // 1. Save Parts trước
                        if (hasHead && sbHead != null) {
                            ShopManagerDAO.gI().insertOrUpdatePart(saveHeadId, 0, sbHead);
                            savedPartCount++;
                        }
                        if (hasBody && sbBody != null) {
                            ShopManagerDAO.gI().insertOrUpdatePart(saveBodyId, 1, sbBody);
                            savedPartCount++;
                        }
                        if (hasLeg && sbLeg != null) {
                            ShopManagerDAO.gI().insertOrUpdatePart(saveLegId, 2, sbLeg);
                            savedPartCount++;
                        }

                        // 2. Update NPC với các Part ID mới
                        ShopManagerDAO.gI().updateNpcTemplateParts(currentNpc.id, finalHeadPartId, finalBodyPartId,
                                finalLegPartId);

                        return null;
                    } else {
                        throw new Exception("Người dùng đã hủy bỏ.");
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }

            @Override
            protected void done() {
                try {
                    get();

                    StringBuilder successMsg = new StringBuilder();
                    successMsg.append("✓ Đã lưu ").append(savedPartCount).append(" Part thành công!\n");
                    successMsg.append("✓ Đã cập nhật NPC (ID ").append(currentNpc.id).append(") thành công!\n\n");
                    successMsg.append("Part IDs:\n");
                    successMsg.append("- Head: ").append(finalHeadPartId).append(hasHead ? "" : " (default)")
                            .append("\n");
                    successMsg.append("- Body: ").append(finalBodyPartId).append(hasBody ? "" : " (default)")
                            .append("\n");
                    successMsg.append("- Leg: ").append(finalLegPartId).append(hasLeg ? "" : " (default)");

                    JOptionPane.showMessageDialog(CreateNPCScr.this, successMsg.toString(),
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Reload list và select lại NPC hiện tại
                    loadNpcListAsyncAndSelect(currentNpc.id);

                } catch (Exception e) {
                    if (!e.getMessage().contains("hủy bỏ")) {
                        JOptionPane.showMessageDialog(CreateNPCScr.this, "Lỗi/Hủy: " + e.getMessage(), "Thông báo",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        worker.execute();
    }

    /**
     * Lưu NPC khi sử dụng cải trang.
     * Vì các part ID của cải trang đã tồn tại trong DB,
     * nên chỉ cần cập nhật head/body/leg trong npc_template.
     */
    private void saveCaiTrangToNpc() {
        StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append("=== LƯU NPC VỚI CẢI TRANG ===").append("\n\n");
        msgBuilder.append("NPC: ").append(currentNpc.name).append(" (ID: ").append(currentNpc.id).append(")\n");
        msgBuilder.append("Cải trang: ").append(caiTrangName).append("\n\n");
        msgBuilder.append("--- Part IDs từ Cải trang ---\n\n");
        msgBuilder.append("Head Part ID: ").append(headId).append("\n");
        msgBuilder.append("Body Part ID: ").append(bodyId).append("\n");
        msgBuilder.append("Leg Part ID: ").append(legId).append("\n\n");
        msgBuilder.append("(Các Part ID đã tồn tại trong DB - chỉ cập nhật npc_template)");

        int option = JOptionPane.showConfirmDialog(this, msgBuilder.toString(),
                "Xác nhận Lưu NPC với Cải trang",
                JOptionPane.YES_NO_OPTION);

        if (option != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                // Chỉ cập nhật npc_template, không insert/update part
                ShopManagerDAO.gI().updateNpcTemplateParts(currentNpc.id, headId, bodyId, legId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    StringBuilder successMsg = new StringBuilder();
                    successMsg.append("✓ Đã cập nhật NPC (ID ").append(currentNpc.id).append(") thành công!\n\n");
                    successMsg.append("Cải trang: ").append(caiTrangName).append("\n");
                    successMsg.append("Part IDs:\n");
                    successMsg.append("- Head: ").append(headId).append("\n");
                    successMsg.append("- Body: ").append(bodyId).append("\n");
                    successMsg.append("- Leg: ").append(legId);

                    JOptionPane.showMessageDialog(CreateNPCScr.this, successMsg.toString(),
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);

                    // Reload list và select lại NPC hiện tại
                    loadNpcListAsyncAndSelect(currentNpc.id);

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CreateNPCScr.this,
                            "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    /**
     * Hiển thị dialog chọn cải trang.
     * Load danh sách cải trang từ item_template (type = 5), hiển thị grid với icon
     * và tên.
     */
    private void showCaiTrangDialog() {
        // Load cải trang list in background
        SwingWorker<java.util.List<CaiTrangTemplate>, Void> worker = new SwingWorker<>() {
            @Override
            protected java.util.List<CaiTrangTemplate> doInBackground() {
                return ShopManagerDAO.gI().getAllCaiTrang();
            }

            @Override
            protected void done() {
                try {
                    java.util.List<CaiTrangTemplate> caiTrangList = get();
                    if (caiTrangList.isEmpty()) {
                        JOptionPane.showMessageDialog(CreateNPCScr.this,
                                "Không tìm thấy cải trang nào trong database!",
                                "Thông báo", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Create dialog
                    JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(CreateNPCScr.this),
                            "Chọn Cải trang cho NPC", true);
                    dialog.setSize(900, 700);
                    dialog.setLocationRelativeTo(CreateNPCScr.this);

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

                        for (CaiTrangTemplate ct : caiTrangList) {
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

                            // Name label
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
                    JOptionPane.showMessageDialog(CreateNPCScr.this,
                            "Lỗi load cải trang: " + e.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    /**
     * Áp dụng cải trang cho NPC.
     * Sử dụng head, body, leg ID từ cải trang.
     * Load ảnh preview từ part data trong DB.
     */
    private void applyCaiTrang(CaiTrangTemplate ct) {
        // Đánh dấu đang sử dụng cải trang
        useCaiTrang = true;
        caiTrangName = ct.name;

        // Set Part IDs từ cải trang
        headId = ct.head;
        bodyId = ct.body;
        legId = ct.leg;

        // Reset icon IDs (sẽ được load từ part data)
        headIconId = 0;
        bodyIconId = 0;
        legIconId = 0;

        // Reset positions
        headPos = new Point(0, 0);
        bodyPos = new Point(0, 0);
        legPos = new Point(0, 0);

        // Clear images
        headImage = null;
        bodyImage = null;
        legImage = null;

        // Load part data từ DB và hiển thị preview
        NpcFullInfo tempNpc = new NpcFullInfo(currentNpc != null ? currentNpc.id : 0,
                currentNpc != null ? currentNpc.name : "", ct.head, ct.body, ct.leg,
                currentNpc != null ? currentNpc.avatar : 0);
        loadPartDataAsync(tempNpc);

        // Update UI labels
        updatePartInfoLabels();
        canvas.repaint();

        JOptionPane.showMessageDialog(this,
                "Đã áp dụng cải trang: " + ct.name + "\n" +
                        "Head: " + ct.head + ", Body: " + ct.body + ", Leg: " + ct.leg + "\n\n" +
                        "Nhấn 'Lưu Part & NPC' để lưu vào DB.\n" +
                        "(Chỉ cập nhật npc_template, không tạo part mới)",
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    // Đường dẫn mặc định
    private static final String IMAGE_DIR_PATH = System.getProperty("user.dir") + "/data/data/icon/x4";
    // Cache thư mục cuối cùng đã chọn (dùng chung cho cả avatar và part)
    private static String lastSelectedDir = null;

    private void selectAvatarImage() {
        // Sử dụng Windows Explorer native (FileDialog)
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        FileDialog dialog = new FileDialog(frame, "Chọn Avatar NPC", FileDialog.LOAD);

        // Set thư mục: ưu tiên thư mục cuối cùng đã chọn, nếu chưa có thì dùng mặc định
        dialog.setDirectory(lastSelectedDir != null ? lastSelectedDir : IMAGE_DIR_PATH);

        // Filter file ảnh
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg") || lower.endsWith(".gif");
        });

        dialog.setVisible(true);

        String fileName = dialog.getFile();
        if (fileName != null) {
            // Lưu lại thư mục đã chọn để lần sau mở nhanh hơn
            lastSelectedDir = dialog.getDirectory();
            File selectedFile = new File(dialog.getDirectory(), fileName);
            try {
                avatarImage = ImageIO.read(selectedFile);

                // Extract ID from filename
                String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
                try {
                    avatarId = Integer.parseInt(nameWithoutExt);
                } catch (NumberFormatException e) {
                    avatarId = 0;
                }

                avatarPanel.repaint();
                avatarIdLabel.setText("ID: " + avatarId);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Không thể load ảnh: " + e.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void selectPartImage(String partType) {
        String title;

        switch (partType) {
            case "head":
                title = "Chọn ảnh HEAD";
                break;
            case "body":
                title = "Chọn ảnh BODY";
                break;
            case "leg":
                title = "Chọn ảnh LEG";
                break;
            default:
                return;
        }

        // Sử dụng Windows Explorer native (FileDialog)
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        FileDialog dialog = new FileDialog(frame, title, FileDialog.LOAD);

        // Set thư mục: ưu tiên thư mục cuối cùng đã chọn, nếu chưa có thì dùng mặc định
        dialog.setDirectory(lastSelectedDir != null ? lastSelectedDir : IMAGE_DIR_PATH);

        // Filter file ảnh
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg") || lower.endsWith(".gif");
        });

        dialog.setVisible(true);

        String fileName = dialog.getFile();
        if (fileName != null) {
            // Lưu lại thư mục đã chọn để lần sau mở nhanh hơn
            lastSelectedDir = dialog.getDirectory();
            File selectedFile = new File(dialog.getDirectory(), fileName);
            try {
                BufferedImage img = ImageIO.read(selectedFile);

                // Extract ID from filename
                String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
                int id = 0;
                try {
                    id = Integer.parseInt(nameWithoutExt);
                } catch (NumberFormatException e) {
                    id = 0;
                }

                // Khi chọn part thủ công, tắt flag cải trang
                useCaiTrang = false;
                caiTrangName = "";

                switch (partType) {
                    case "head":
                        headImage = img;
                        headId = 0; // Reset ID để tính toán ID mới khi export
                        headIconId = id;
                        selectedPart = "head";
                        updatePartInfoLabels();
                        break;
                    case "body":
                        bodyImage = img;
                        bodyId = 0;
                        bodyIconId = id;
                        selectedPart = "body";
                        updatePartInfoLabels();
                        break;
                    case "leg":
                        legImage = img;
                        legId = 0;
                        legIconId = id;
                        selectedPart = "leg";
                        updatePartInfoLabels();
                        break;
                }

                canvas.repaint();
                canvas.requestFocusInWindow();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Không thể load ảnh: " + e.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updatePartInfoLabels() {
        headInfoLabel.setText(String.format("PartID: %d | Icon: %d | DB: (%d, %d) | Screen: (%d, %d)", headId,
                headIconId, headPos.x / 4, headPos.y / 4, headPos.x, headPos.y));
        bodyInfoLabel.setText(String.format("PartID: %d | Icon: %d | DB: (%d, %d) | Screen: (%d, %d)", bodyId,
                bodyIconId, bodyPos.x / 4, bodyPos.y / 4, bodyPos.x, bodyPos.y));
        legInfoLabel.setText(String.format("PartID: %d | Icon: %d | DB: (%d, %d) | Screen: (%d, %d)", legId, legIconId,
                legPos.x / 4, legPos.y / 4, legPos.x, legPos.y));
    }

    private void resetPositions() {
        headPos = new Point(0, 0);
        bodyPos = new Point(0, 0);
        legPos = new Point(0, 0);
        updatePartInfoLabels();
        canvas.repaint();
    }

    // Helper method to calculate IDs (Reusable logic)
    private int[] calculateTargetPartIds(int currentMax) {
        int[] ids = new int[3]; // 0: Head, 1: Body, 2: Leg
        int tempMax = currentMax;

        // Head
        if (currentNpc != null && currentNpc.head != -1) {
            ids[0] = currentNpc.head;
        } else {
            tempMax++;
            ids[0] = tempMax;
        }

        // Body
        if (currentNpc != null && currentNpc.body != -1) {
            ids[1] = currentNpc.body;
        } else {
            tempMax++;
            ids[1] = tempMax;
        }

        // Leg
        if (currentNpc != null && currentNpc.leg != -1) {
            ids[2] = currentNpc.leg;
        } else {
            tempMax++;
            ids[2] = tempMax;
        }

        return ids;
    }

    private void exportData() {
        // Tọa độ DB (Logic) = Screen / 4
        int headX = headPos.x / 4;
        int headY = headPos.y / 4;
        int bodyX = bodyPos.x / 4;
        int bodyY = bodyPos.y / 4;
        int legX = legPos.x / 4;
        int legY = legPos.y / 4;

        // Fetch Max ID Async to avoid UI freeze (Optional, but better for UX)
        // But for "Export Manual" button, verify quickly is ok.
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                return ShopManagerDAO.gI().getMaxPartId();
            }

            @Override
            protected void done() {
                try {
                    int currentMax = get();

                    // Logic tính ID: Reuse nếu có, tạo mới nếu chưa
                    int[] targetIds = calculateTargetPartIds(currentMax);
                    int displayHeadId = targetIds[0];
                    int displayBodyId = targetIds[1];
                    int displayLegId = targetIds[2];

                    StringBuilder sb = new StringBuilder();

                    String npcName = (currentNpc != null) ? currentNpc.name : "New NPC";
                    int npcId = (currentNpc != null) ? currentNpc.id : 0;

                    sb.append("=== NPC: ").append(npcName).append(" (ID: ").append(npcId).append(") ===\n\n");
                    sb.append("Avatar ID: ").append(avatarId).append("\n\n");

                    sb.append("Part IDs:\n");
                    // Head
                    if (headIconId <= 0) {
                        sb.append("- Head Part ID: default ").append(DEFAULT_HEAD_ICON_ID).append("\n");
                    } else {
                        sb.append("- Head Part ID: ").append(displayHeadId)
                                .append(currentNpc != null && currentNpc.head == displayHeadId ? " (Update Old)"
                                        : " (New)")
                                .append("\n");
                    }
                    // Body
                    if (bodyIconId <= 0) {
                        sb.append("- Body Part ID: default ").append(DEFAULT_BODY_ICON_ID).append("\n");
                    } else {
                        sb.append("- Body Part ID: ").append(displayBodyId)
                                .append(currentNpc != null && currentNpc.body == displayBodyId ? " (Update Old)"
                                        : " (New)")
                                .append("\n");
                    }
                    // Leg
                    if (legIconId <= 0) {
                        sb.append("- Leg Part ID: default ").append(DEFAULT_LEG_ICON_ID).append("\n\n");
                    } else {
                        sb.append("- Leg Part ID: ").append(displayLegId)
                                .append(currentNpc != null && currentNpc.leg == displayLegId ? " (Update Old)"
                                        : " (New)")
                                .append("\n\n");
                    }

                    sb.append("=== Path Data ===\n\n");

                    // Head Path (Type 0)
                    sb.append("Head Path (Type 0):\n");
                    if (headIconId <= 0) {
                        sb.append("default ").append(DEFAULT_HEAD_ICON_ID).append("\n\n");
                    } else {
                        sb.append("[[").append(headIconId).append(",").append(headX).append(",").append(headY)
                                .append("]");
                        for (int i = 0; i < 2; i++) {
                            sb.append(",[2955,0,0]");
                        }
                        sb.append("]\n\n");
                    }

                    // Body Path (Type 1)
                    sb.append("Body Path (Type 1):\n");
                    if (bodyIconId <= 0) {
                        sb.append("default ").append(DEFAULT_BODY_ICON_ID).append("\n\n");
                    } else {
                        sb.append("[[2955,0,0],[").append(bodyIconId).append(",").append(bodyX).append(",")
                                .append(bodyY).append("]");
                        for (int i = 0; i < 15; i++) {
                            sb.append(",[2955,0,0]");
                        }
                        sb.append("]\n\n");
                    }

                    // Leg Path (Type 2)
                    sb.append("Leg Path (Type 2):\n");
                    if (legIconId <= 0) {
                        sb.append("default ").append(DEFAULT_LEG_ICON_ID);
                    } else {
                        sb.append("[[2955,0,0],[").append(legIconId).append(",").append(legX).append(",").append(legY)
                                .append("]");
                        for (int i = 0; i < 12; i++) {
                            sb.append(",[2955,0,0]");
                        }
                        sb.append("]");
                    }

                    outputTextArea.setText(sb.toString());

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CreateNPCScr.this, "Error generating export data: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Canvas để hiển thị và chỉnh sửa vị trí các part
     * Trục Ox nằm ở vị trí 3/4 chiều cao (thấp xuống 1/4)
     */
    private class NPCCanvas extends JPanel implements KeyListener {

        private Point dragStart = null;
        private Point startDragPartPos = null; // Lưu vị trí Path gốc khi bắt đầu kéo
        private boolean isDragging = false;

        // Tính toán ORIGIN dựa trên kích thước thực tế
        private int getOriginX() {
            return getWidth() / 2;
        }

        private int getOriginY() {
            // Trục Ox ở vị trí 3/4 chiều cao (thấp xuống 1/4 so với tâm)
            return getHeight() * 3 / 4;
        }

        public NPCCanvas() {
            setBackground(new Color(30, 30, 30));
            setFocusable(true);

            // Tắt chức năng chuyển focus mặc định của phím TAB
            setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, java.util.Collections.emptySet());
            setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, java.util.Collections.emptySet());

            addKeyListener(this);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        // Left Click: Auto-select part under cursor
                        checkAndSelectPart(e.getPoint());

                        dragStart = e.getPoint();
                        Point currentPos = getSelectedPartPos();
                        startDragPartPos = new Point(currentPos.x, currentPos.y);

                        isDragging = true;
                        requestFocusInWindow();
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        // Right Click: Move currently selected part (Remote control)
                        // Don't change selection, just drag the current one
                        if (selectedPart != null) {
                            dragStart = e.getPoint();
                            Point currentPos = getSelectedPartPos();
                            startDragPartPos = new Point(currentPos.x, currentPos.y);

                            isDragging = true;
                            requestFocusInWindow();
                        }
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isDragging = false;
                    dragStart = null;
                    startDragPartPos = null;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isDragging && dragStart != null && selectedPart != null && startDragPartPos != null) {
                        int screenDx = e.getX() - dragStart.x;
                        int screenDy = e.getY() - dragStart.y;

                        // Logic: Screen Coordinate Mode
                        // Di chuyển chuột bao nhiêu pixel thì ảnh đi bấy nhiêu pixel (x1 Screen)
                        int dx = e.getX() - dragStart.x;
                        int dy = e.getY() - dragStart.y;

                        updateSelectedPartPos(startDragPartPos.x + dx, startDragPartPos.y + dy);

                        repaint();
                        updatePartInfoLabels();
                    }
                }
            });
        }

        private void checkAndSelectPart(Point clickPoint) {
            // Kiểm tra theo Z-index ngược để chọn đúng (Body -> Leg -> Head)
            // Vì Body vẽ sau cùng (trên cùng), nên check nó trước.
            if (isPointInPart(clickPoint, bodyImage, bodyPos)) {
                selectedPart = "body";
            } else if (isPointInPart(clickPoint, legImage, legPos)) {
                selectedPart = "leg";
            } else if (isPointInPart(clickPoint, headImage, headPos)) {
                selectedPart = "head";
            }
            repaint();
        }

        private boolean isPointInPart(Point click, BufferedImage img, Point partPos) {
            if (img == null)
                return false;

            // Xác định skeleton offset dựa trên partPos
            int skelX = 0;
            int skelY = 0;
            if (partPos == headPos) {
                skelX = SKEL_HEAD_X;
                skelY = SKEL_HEAD_Y;
            } else if (partPos == bodyPos) {
                skelX = SKEL_BODY_X;
                skelY = SKEL_BODY_Y;
            } else if (partPos == legPos) {
                skelX = SKEL_LEG_X;
                skelY = SKEL_LEG_Y;
            }

            // Tọa độ vẽ theo công thức: Origin + Skel*Zoom + Pos(đã là screen scale)
            int screenX = getOriginX() + skelX * ZOOM_LEVEL + partPos.x;
            int screenY = getOriginY() - skelY * ZOOM_LEVEL + partPos.y;

            Rectangle bounds = new Rectangle(screenX, screenY, img.getWidth(), img.getHeight());
            return bounds.contains(click);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int originX = getOriginX();
            int originY = getOriginY();

            // Vẽ grid
            g2d.setColor(new Color(50, 50, 50));
            int gridSize = 40;
            for (int x = 0; x <= w; x += gridSize) {
                g2d.drawLine(x, 0, x, h);
            }
            for (int y = 0; y <= h; y += gridSize) {
                g2d.drawLine(0, y, w, y);
            }

            // Vẽ trục Oxy
            g2d.setStroke(new BasicStroke(1));

            // Trục X (ngang)
            g2d.setColor(new Color(150, 50, 50));
            g2d.drawLine(0, originY, w, originY);

            // Trục Y (dọc)
            g2d.setColor(new Color(50, 150, 50));
            g2d.drawLine(originX, 0, originX, h);

            // Vẽ gốc tọa độ
            g2d.setColor(Color.WHITE);
            g2d.drawOval(originX - 3, originY - 3, 6, 6);

            // Vẽ các part theo thứ tự Layer (Z-index):
            // 1. Head (Vẽ trước - nằm dưới cùng)
            drawPart(g2d, headImage, headPos, "head");
            // 2. Leg
            drawPart(g2d, legImage, legPos, "leg");
            // 3. Body (Vẽ sau cùng - đè lên khớp nối với đầu và chân)
            drawPart(g2d, bodyImage, bodyPos, "body");

            // Hiển thị tọa độ part đang chọn (hiển thị DB coord)
            if (selectedPart != null) {
                Point pos = getSelectedPartPos();
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Consolas", Font.BOLD, 14));
                g2d.drawString(
                        "Selected: " + selectedPart.toUpperCase() + " [DB: " + pos.x / 4 + ", " + pos.y / 4 + "]",
                        10, h - 10);
            }

            // Hiển thị thông tin NPC đang load
            if (currentNpc != null) {
                g2d.setColor(new Color(100, 200, 255));
                g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2d.drawString("NPC: " + currentNpc.name + " (ID: " + currentNpc.id + ")", 10, 20);
            }

            g2d.dispose();
        }

        private void drawPart(Graphics2D g2d, BufferedImage img, Point pos, String partName) {
            if (img == null)
                return;

            int skelX = 0;
            int skelY = 0;

            switch (partName) {
                case "head":
                    skelX = SKEL_HEAD_X;
                    skelY = SKEL_HEAD_Y;
                    break;
                case "body":
                    skelX = SKEL_BODY_X;
                    skelY = SKEL_BODY_Y;
                    break;
                case "leg":
                    skelX = SKEL_LEG_X;
                    skelY = SKEL_LEG_Y;
                    break;
            }

            // Công thức:
            // X_screen = OriginX + skelDX * ZOOM + pos.x
            // Y_screen = OriginY - skelDY * ZOOM + pos.y

            int drawX = getOriginX() + skelX * ZOOM_LEVEL + pos.x;
            int drawY = getOriginY() - skelY * ZOOM_LEVEL + pos.y;

            g2d.drawImage(img, drawX, drawY, null);

            // Vẽ Anchor Point (Màu đỏ) tại vị trí pos
            g2d.setColor(Color.RED);
            g2d.fillOval(drawX - 3, drawY - 3, 6, 6);

            // Highlight nếu đang được chọn (Viền quanh ảnh)
            if (partName.equals(selectedPart)) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        0, new float[] { 5, 5 }, 0));
                g2d.drawRect(drawX, drawY, img.getWidth(), img.getHeight());
            }
        }

        /**
         * Di chuyển tương đối (dùng cho phím bấm)
         * 
         * @param dxDelta đơn vị Path
         * @param dyDelta đơn vị Path
         */
        private void moveSelectedPart(int dxDelta, int dyDelta) {
            if (selectedPart == null)
                return;

            switch (selectedPart) {
                case "head":
                    headPos.translate(dxDelta, dyDelta);
                    break;
                case "body":
                    bodyPos.translate(dxDelta, dyDelta);
                    break;
                case "leg":
                    legPos.translate(dxDelta, dyDelta);
                    break;
            }
        }

        /**
         * Cập nhật vị trí tuyệt đối (dùng cho mouse drag)
         * 
         * @param x Path X
         * @param y Path Y
         */
        private void updateSelectedPartPos(int x, int y) {
            if (selectedPart == null)
                return;

            switch (selectedPart) {
                case "head":
                    headPos.setLocation(x, y);
                    break;
                case "body":
                    bodyPos.setLocation(x, y);
                    break;
                case "leg":
                    legPos.setLocation(x, y);
                    break;
            }
        }

        private Point getSelectedPartPos() {
            if (selectedPart == null)
                return new Point(0, 0);

            switch (selectedPart) {
                case "head":
                    return headPos;
                case "body":
                    return bodyPos;
                case "leg":
                    return legPos;
                default:
                    return new Point(0, 0);
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            // TAB: Chuyển đổi qua lại giữa các part
            if (e.getKeyCode() == KeyEvent.VK_TAB) {
                cycleSelectedPart();
                return;
            }

            if (selectedPart == null)
                return;

            int step = e.isShiftDown() ? 10 : 1; // 1 pixel màn hình

            // Logic phím:
            // UP -> Ảnh lên trên -> Trên màn hình Y giảm.
            // Công thức YScreen = OriginY + (-SkelY + PathY)*Zoom.
            // Để YScreen giảm, PathY phải giảm.

            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    moveSelectedPart(-step, 0);
                    break;
                case KeyEvent.VK_RIGHT:
                    moveSelectedPart(step, 0);
                    break;
                case KeyEvent.VK_UP:
                    moveSelectedPart(0, -step); // Giảm Y
                    break;
                case KeyEvent.VK_DOWN:
                    moveSelectedPart(0, step); // Tăng Y
                    break;
            }
            repaint();
            updatePartInfoLabels();
        }

        private void cycleSelectedPart() {
            if (selectedPart == null) {
                selectedPart = "head";
            } else {
                switch (selectedPart) {
                    case "head":
                        selectedPart = "body";
                        break;
                    case "body":
                        selectedPart = "leg";
                        break;
                    case "leg":
                        selectedPart = "head";
                        break;
                    default:
                        selectedPart = "head";
                        break;
                }
            }
            repaint();
            updatePartInfoLabels();
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }
    }
}
