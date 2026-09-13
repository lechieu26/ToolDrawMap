package com.girlkun.tool.screens.boss_scr;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/** Edits dialogue without exposing the server's JSON syntax. */
class BossChatEditor extends JPanel {
    private String json;
    private final JButton edit = new JButton();

    BossChatEditor(String initial) {
        super(new BorderLayout());
        add(edit, BorderLayout.CENTER);
        edit.addActionListener(e -> openEditor());
        setText(initial);
    }

    void setText(String value) {
        json = value == null ? "[]" : value;
        Object parsed = JSONValue.parse(json);
        edit.setText(parsed instanceof JSONArray
                ? "Sửa thoại (" + ((JSONArray) parsed).size() + " câu)..." : "Kiểm tra dữ liệu thoại...");
    }

    String getText() { return json; }

    private static String speaker(String prefix) {
        if ("-1".equals(prefix)) return "Boss";
        if ("-2".equals(prefix)) return "Người chơi";
        if ("-3".equals(prefix)) return "Boss cha";
        return "Mã " + prefix;
    }

    private void openEditor() {
        Object parsed = JSONValue.parse(json);
        if (!(parsed instanceof JSONArray)) {
            JOptionPane.showMessageDialog(this, "Dữ liệu thoại cũ không phải mảng JSON hợp lệ. Dữ liệu được giữ nguyên.");
            return;
        }
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Người nói", "Nội dung"}, 0);
        JComboBox<String> speakers = new JComboBox<>(new String[]{"Boss", "Người chơi", "Boss cha"});
        for (Object value : (JSONArray) parsed) {
            if (!(value instanceof String)) {
                JOptionPane.showMessageDialog(this, "Dữ liệu thoại có phần tử không phải văn bản. Dữ liệu được giữ nguyên.");
                return;
            }
            String text = (String) value;
            String who = "Boss (không có mã)";
            if (text.matches("^\\|-?\\d+\\|[\\s\\S]*")) {
                int end = text.indexOf('|', 1);
                who = speaker(text.substring(1, end));
                text = text.substring(end + 1);
            }
            boolean found = false;
            for (int i = 0; i < speakers.getItemCount(); i++) {
                if (who.equals(speakers.getItemAt(i))) found = true;
            }
            if (!found) speakers.addItem(who);
            model.addRow(new Object[]{who, text});
        }
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Soạn câu thoại", Dialog.ModalityType.APPLICATION_MODAL);
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(speakers));
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(580);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(new JLabel("Chọn người nói; nhấp đúp vào nội dung để nhập câu thoại thuần văn bản."), BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton add = new JButton("+ Câu thoại"), remove = new JButton("Xóa"), up = new JButton("Lên"), down = new JButton("Xuống");
        JButton save = new JButton("Áp dụng"), cancel = new JButton("Hủy");
        add.addActionListener(e -> {
            if (!commit(table)) return;
            model.addRow(new Object[]{"Boss", ""});
            int row = model.getRowCount() - 1;
            table.setRowSelectionInterval(row, row);
            table.editCellAt(row, 1);
            table.getEditorComponent().requestFocusInWindow();
        });
        remove.addActionListener(e -> {
            if (!commit(table)) return;
            int row = table.getSelectedRow();
            if (row >= 0) model.removeRow(row);
        });
        up.addActionListener(e -> move(table, model, -1));
        down.addActionListener(e -> move(table, model, 1));
        save.addActionListener(e -> {
            if (!commit(table)) return;
            JSONArray result = new JSONArray();
            for (int row = 0; row < model.getRowCount(); row++) {
                String who = String.valueOf(model.getValueAt(row, 0));
                String text = String.valueOf(model.getValueAt(row, 1));
                String prefix = "Boss".equals(who) ? "-1" : "Người chơi".equals(who) ? "-2"
                        : "Boss cha".equals(who) ? "-3" : who.startsWith("Mã ") ? who.substring(3) : null;
                result.add(prefix == null ? text : "|" + prefix + "|" + text);
            }
            setText(result.toJSONString());
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());
        for (JButton button : new JButton[]{add, remove, up, down, cancel, save}) actions.add(button);
        content.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(820, 430);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static boolean commit(JTable table) {
        return !table.isEditing() || table.getCellEditor().stopCellEditing();
    }

    private static void move(JTable table, DefaultTableModel model, int direction) {
        if (!commit(table)) return;
        int row = table.getSelectedRow(), target = row + direction;
        if (row >= 0 && target >= 0 && target < model.getRowCount()) {
            model.moveRow(row, row, target);
            table.setRowSelectionInterval(target, target);
        }
    }
}
