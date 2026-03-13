package com.girlkun.tool.screens.sprite_scr;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class FrameList extends JPanel {
    private DefaultListModel<FrameData> model = new DefaultListModel<>();
    private JList<FrameData> list = new JList<>(model);
    private JTextField edW = new JTextField(5);
    private JTextField edH = new JTextField(5);
    private java.util.function.Consumer<List<FrameData>> onSelectionChanged;
    private java.util.function.Consumer<List<FrameData>> onSaveSelected;
    private Runnable onDataChanged;

    public FrameList() {
        setLayout(new BorderLayout());
        setupUI();
    }

    private void setupUI() {
        JPanel top = new JPanel(new GridLayout(2, 1));
        
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnDel = createStyledButton("Del", new Color(178, 34, 34));
        JButton btnSaveSel = createStyledButton("Save Selected", new Color(0, 153, 204));
        row1.add(btnDel);
        row1.add(btnSaveSel);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("W:"));
        row2.add(edW);
        row2.add(new JLabel("H:"));
        row2.add(edH);
        JButton btnApply = createStyledButton("Set", new Color(0, 153, 51));
        row2.add(btnApply);
        
        top.add(row1);
        top.add(row2);
        add(top, BorderLayout.NORTH);

        list.setCellRenderer(new FrameRenderer());
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        add(new JScrollPane(list), BorderLayout.CENTER);

        // Actions
        btnDel.addActionListener(e -> deleteSelected());
        btnSaveSel.addActionListener(e -> {
            if (onSaveSelected != null) onSaveSelected.accept(list.getSelectedValuesList());
        });
        btnApply.addActionListener(e -> applySize());

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                updateSizeInputs();
                if (onSelectionChanged != null) onSelectionChanged.accept(list.getSelectedValuesList());
            }
        });

        // Enter on W/H fields
        ActionListener sizeAction = e -> applySize();
        edW.addActionListener(sizeAction);
        edH.addActionListener(sizeAction);
    }

    public void setOnSaveSelected(java.util.function.Consumer<List<FrameData>> listener) {
        this.onSaveSelected = listener;
    }

    public void setOnSelectionChanged(java.util.function.Consumer<List<FrameData>> listener) {
        this.onSelectionChanged = listener;
    }

    public void setOnDataChanged(Runnable listener) {
        this.onDataChanged = listener;
    }

    public void addFrame(FrameData data) {
        model.addElement(data);
        list.setSelectedValue(data, true);
        if (onDataChanged != null) onDataChanged.run();
    }

    public List<FrameData> getAllFrames() {
        List<FrameData> all = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) all.add(model.getElementAt(i));
        return all;
    }

    public List<FrameData> getSelectedFrames() {
        return list.getSelectedValuesList();
    }

    public void setSelectedFrames(List<FrameData> selected) {
        list.clearSelection();
        for (FrameData f : selected) {
            int idx = model.indexOf(f);
            if (idx != -1) list.addSelectionInterval(idx, idx);
        }
    }

    private void updateSizeInputs() {
        List<FrameData> sel = list.getSelectedValuesList();
        if (sel.size() == 1) {
            FrameData d = sel.get(0);
            edW.setText(String.valueOf((int)d.getDisplayWidth()));
            edH.setText(String.valueOf((int)d.getDisplayHeight()));
        } else {
            edW.setText("");
            edH.setText("");
        }
    }

    private void applySize() {
        try {
            int w = Integer.parseInt(edW.getText());
            int h = Integer.parseInt(edH.getText());
            for (FrameData d : list.getSelectedValuesList()) {
                d.setDisplayWidth(w);
                d.setDisplayHeight(h);
            }
            if (onDataChanged != null) onDataChanged.run();
            repaint();
        } catch (Exception e) {}
    }

    private void deleteSelected() {
        List<FrameData> sel = list.getSelectedValuesList();
        for (FrameData d : sel) model.removeElement(d);
        if (onDataChanged != null) onDataChanged.run();
    }

    public void refresh() {
        list.repaint();
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        if (bg != null) {
            btn.setBackground(bg);
            btn.setForeground(Color.WHITE);
        }
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusable(false);
        return btn;
    }

    private class FrameRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            FrameData data = (FrameData) value;
            
            // Icon
            int iw = 48, ih = 48;
            BufferedImage icon = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = icon.createGraphics();
            // Draw original image scaled to fit 48x48
            double ratio = Math.min((double)iw / data.getOriginalImage().getWidth(), (double)ih / data.getOriginalImage().getHeight());
            int dw = (int)(data.getOriginalImage().getWidth() * ratio);
            int dh = (int)(data.getOriginalImage().getHeight() * ratio);
            g2.drawImage(data.getOriginalImage(), (iw - dw) / 2, (ih - dh) / 2, dw, dh, null);
            g2.dispose();
            
            label.setIcon(new ImageIcon(icon));
            label.setText(data.toString());
            return label;
        }
    }
}
