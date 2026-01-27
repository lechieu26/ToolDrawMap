package com.girlkun.tool.shopmanager.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;

public class DatePicker extends JDialog {
    private Date selectedDate;
    private final Calendar currentCal = Calendar.getInstance();
    private final JButton[] dayButtons = new JButton[49];
    private final JLabel lblMonthYear = new JLabel("", JLabel.CENTER);

    public DatePicker(Window owner) {
        super(owner, "Chọn ngày", ModalityType.APPLICATION_MODAL);
        initComponents();
        updateDisplay();
        this.setSize(400, 300);
        this.setLocationRelativeTo(owner);
    }

    private void initComponents() {
        JPanel pMain = new JPanel(new BorderLayout());

        // Header (Month/Year Navigation)
        JPanel pHeader = new JPanel(new BorderLayout());
        JButton btnPrev = new JButton("<<");
        JButton btnNext = new JButton(">>");

        btnPrev.addActionListener(e -> {
            currentCal.add(Calendar.MONTH, -1);
            updateDisplay();
        });

        btnNext.addActionListener(e -> {
            currentCal.add(Calendar.MONTH, 1);
            updateDisplay();
        });

        lblMonthYear.setFont(new Font("SansSerif", Font.BOLD, 14));

        pHeader.add(btnPrev, BorderLayout.WEST);
        pHeader.add(lblMonthYear, BorderLayout.CENTER);
        pHeader.add(btnNext, BorderLayout.EAST);
        pHeader.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Grid (Days)
        JPanel pGrid = new JPanel(new GridLayout(7, 7, 2, 2));
        String[] headers = { "CN", "T2", "T3", "T4", "T5", "T6", "T7" };

        for (int i = 0; i < 49; i++) {
            dayButtons[i] = new JButton();
            dayButtons[i].setFocusPainted(false);

            if (i < 7) {
                dayButtons[i].setText(headers[i]);
                dayButtons[i].setEnabled(false);
                dayButtons[i].setForeground(Color.BLUE);
            } else {
                dayButtons[i].addActionListener(e -> {
                    String text = ((JButton) e.getSource()).getText();
                    if (!text.isEmpty()) {
                        int day = Integer.parseInt(text);
                        currentCal.set(Calendar.DAY_OF_MONTH, day);
                        selectedDate = currentCal.getTime();
                        dispose();
                    }
                });
            }
            pGrid.add(dayButtons[i]);
        }
        pGrid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        pMain.add(pHeader, BorderLayout.NORTH);
        pMain.add(pGrid, BorderLayout.CENTER);

        this.setContentPane(pMain);
    }

    private void updateDisplay() {
        // Update Label
        int month = currentCal.get(Calendar.MONTH);
        int year = currentCal.get(Calendar.YEAR);
        lblMonthYear.setText(String.format("Tháng %d - %d", month + 1, year));

        // Determine start day
        Calendar cal = (Calendar) currentCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1 = Sunday
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Clear buttons
        for (int i = 7; i < 49; i++) {
            dayButtons[i].setText("");
            dayButtons[i].setEnabled(false);
        }

        // Fill buttons
        // Grid index starts at 7 (row 1).
        // Sunday (1) should be at k=7 + 0.
        // If startDayOfWeek is 1 (Sun), shift is 0.
        // If startDayOfWeek is 2 (Mon), shift is 1.
        int shift = startDayOfWeek - 1;

        for (int day = 1; day <= daysInMonth; day++) {
            int index = 7 + shift + (day - 1);
            if (index < 49) {
                dayButtons[index].setText(String.valueOf(day));
                dayButtons[index].setEnabled(true);
            }
        }
    }

    public Date getSelectedDate() {
        return selectedDate;
    }
}
