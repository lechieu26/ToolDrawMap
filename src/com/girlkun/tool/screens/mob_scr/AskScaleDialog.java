package com.girlkun.tool.screens.mob_scr;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog hỏi người dùng khi thay đổi scale.
 * Result: Boolean.TRUE = Resize, Boolean.FALSE = Keep, null = Cancel
 */
public class AskScaleDialog extends JDialog {
    private Boolean result = null;

    public AskScaleDialog(Component parent, int oldScale, int newScale, double ratio) {
        super(SwingUtilities.getWindowAncestor(parent), "Thay đổi Scale", ModalityType.APPLICATION_MODAL);
        setResizable(false);
        setLayout(new BorderLayout(10, 10));

        String msg = String.format(
            "Bạn đang đổi scale từ x%d sang x%d.\n" +
            "Tỉ lệ thay đổi kích thước: %.2f lần\n\n" +
            "Vui lòng chọn cách xử lý dữ liệu toạ độ:", oldScale, newScale, ratio);

        JLabel label = new JLabel("<html>" + msg.replace("\n", "<br>") + "</html>");
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        label.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        add(label, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton resizeBtn = new JButton("<html><center>Resize<br>(Tính lại tọa độ)</center></html>");
        resizeBtn.setPreferredSize(new Dimension(150, 50));
        resizeBtn.setBackground(new Color(0xDD, 0xDD, 0xFF));
        resizeBtn.addActionListener(e -> { result = Boolean.TRUE; dispose(); });

        JButton keepBtn = new JButton("<html><center>Keep Size<br>(Giữ nguyên số)</center></html>");
        keepBtn.setPreferredSize(new Dimension(150, 50));
        keepBtn.setBackground(new Color(0xDD, 0xFF, 0xDD));
        keepBtn.addActionListener(e -> { result = Boolean.FALSE; dispose(); });

        JButton cancelBtn = new JButton("<html><center>Cancel<br>(Hủy)</center></html>");
        cancelBtn.setPreferredSize(new Dimension(100, 50));
        cancelBtn.addActionListener(e -> { result = null; dispose(); });

        btnPanel.add(resizeBtn);
        btnPanel.add(keepBtn);
        btnPanel.add(cancelBtn);
        add(btnPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    public Boolean getResult() { return result; }

    public static Boolean show(Component parent, int oldScale, int newScale, double ratio) {
        AskScaleDialog dlg = new AskScaleDialog(parent, oldScale, newScale, ratio);
        dlg.setVisible(true);
        return dlg.getResult();
    }
}
