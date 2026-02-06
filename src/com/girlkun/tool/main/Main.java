package com.girlkun.tool.main;

import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;
import com.girlkun.database.GirlkunDB;
import com.girlkun.tool.screens.draw_map_scr.DrawMapScr;
import com.girlkun.tool.screens.npc_scr.CreateNPCScr;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.GroupLayout;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.GroupLayout.Alignment;

/**
 * Draw Map Tool - Tách từ GirlkunToolCBRO
 * Công cụ vẽ và chỉnh sửa map cho game NRO
 */
public class Main extends JFrame {

    public static Main I;
    private boolean fullScr = false;
    private Color unFullScreen = new Color(204, 204, 255);
    private Color fullScreen = new Color(255, 0, 204);
    private int cX, cY, cW, cH;

    private JMenuItem btnDrawMap;
    private JButton btnFullScreen;
    private JButton btnDrawMapToolbar;
    private JButton btnPathEditor;
    private JButton btnCreateItemIcon;
    private JButton btnMobEditor;
    private JButton btnImageScaler;
    private JButton btnTeaShopManager;
    private JButton btnCreateNPC;
    private JButton btnDbConfig;
    private JButton btnCreateBoss;
    private JButton btnEffectEditor;
    private JDesktopPane desktop;
    private JMenu jMenu1;
    private JMenuBar jMenuBar1;
    private JPanel jPanel1;
    private JScrollPane jScrollPane1;
    private JToolBar jToolBar1;

    public Main() {
        initComponents();
        setup();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                GirlkunDB.close();
            }
        });
        I = this;

        // Hiển thị DbConfigScr đầu tiên khi khởi động
        EventQueue.invokeLater(() -> {
            try {
                Thread.sleep(500); // Đợi UI load xong
                // Mở cửa sổ cấu hình DB đầu tiên
                openDbConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void initComponents() {
        jToolBar1 = new JToolBar();
        btnFullScreen = new JButton();
        jScrollPane1 = new JScrollPane();
        jPanel1 = new JPanel();
        desktop = new JDesktopPane();
        jMenuBar1 = new JMenuBar();
        jMenu1 = new JMenu();
        btnDrawMap = new JMenuItem();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        jToolBar1.setFloatable(false);
        jToolBar1.setOrientation(JToolBar.VERTICAL);
        jToolBar1.setRollover(true);

        btnFullScreen.setBackground(new Color(255, 0, 204));
        btnFullScreen.setForeground(Color.WHITE);
        btnFullScreen.setText("Full screen");
        btnFullScreen.setFocusable(false);
        btnFullScreen.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnFullScreen.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 45));
        btnFullScreen.addActionListener(this::btnFullScreenActionPerformed);
        jToolBar1.add(btnFullScreen);

        btnDbConfig = new JButton();
        btnDbConfig.setBackground(new Color(0, 128, 128));
        btnDbConfig.setForeground(Color.WHITE);
        btnDbConfig.setText("Cấu hình DB");
        btnDbConfig.setFocusable(false);
        btnDbConfig.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnDbConfig.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 45));
        btnDbConfig.addActionListener(this::btnDbConfigActionPerformed);
        jToolBar1.add(btnDbConfig);

        btnDrawMapToolbar = new JButton();
        btnDrawMapToolbar.setBackground(new Color(0, 153, 204));
        btnDrawMapToolbar.setForeground(Color.WHITE);
        btnDrawMapToolbar.setText("Draw Map");
        btnDrawMapToolbar.setFocusable(false);
        btnDrawMapToolbar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnDrawMapToolbar.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 45));
        btnDrawMapToolbar.addActionListener(this::btnDrawMapActionPerformed);
        jToolBar1.add(btnDrawMapToolbar);

        btnPathEditor = new JButton();
        btnPathEditor.setBackground(new Color(153, 102, 0));
        btnPathEditor.setForeground(Color.WHITE);
        btnPathEditor.setText("Path Editor");
        btnPathEditor.setFocusable(false);
        btnPathEditor.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnPathEditor.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 45));
        btnPathEditor.addActionListener(this::btnPathEditorActionPerformed);
        jToolBar1.add(btnPathEditor);

        btnCreateItemIcon = new JButton();
        btnCreateItemIcon.setBackground(new Color(102, 0, 153));
        btnCreateItemIcon.setForeground(Color.WHITE);
        btnCreateItemIcon.setText("Create Item icon");
        btnCreateItemIcon.setFocusable(false);
        btnCreateItemIcon.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnCreateItemIcon.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 45));
        btnCreateItemIcon.addActionListener(this::btnCreateItemIconActionPerformed);
        jToolBar1.add(btnCreateItemIcon);

        btnMobEditor = new JButton();
        btnMobEditor.setBackground(new Color(0, 153, 51));
        btnMobEditor.setForeground(Color.WHITE);
        btnMobEditor.setText("Create Mob");
        btnMobEditor.setFocusable(false);
        btnMobEditor.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnMobEditor.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 45));
        btnMobEditor.addActionListener(this::btnMobEditorActionPerformed);
        jToolBar1.add(btnMobEditor);

        btnImageScaler = new JButton();
        btnImageScaler.setBackground(new Color(204, 102, 0));
        btnImageScaler.setForeground(Color.WHITE);
        btnImageScaler.setText("Image Scaler");
        btnImageScaler.setFocusable(false);
        btnImageScaler.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnImageScaler.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 45));
        btnImageScaler.addActionListener(this::btnImageScalerActionPerformed);
        jToolBar1.add(btnImageScaler);

        btnTeaShopManager = new JButton();
        btnTeaShopManager.setBackground(new Color(204, 51, 102));
        btnTeaShopManager.setForeground(Color.WHITE);
        btnTeaShopManager.setText("Shop Manager");
        btnTeaShopManager.setFocusable(false);
        btnTeaShopManager.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnTeaShopManager.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 45));
        btnTeaShopManager.addActionListener(this::btnTeaShopManagerActionPerformed);
        jToolBar1.add(btnTeaShopManager);

        btnCreateNPC = new JButton();
        btnCreateNPC.setBackground(new Color(75, 0, 130));
        btnCreateNPC.setForeground(Color.WHITE);
        btnCreateNPC.setText("Create NPC");
        btnCreateNPC.setFocusable(false);
        btnCreateNPC.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnCreateNPC.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 45));
        btnCreateNPC.addActionListener(this::btnCreateNPCActionPerformed);
        jToolBar1.add(btnCreateNPC);

        btnCreateBoss = new JButton();
        btnCreateBoss.setBackground(new Color(178, 34, 34)); // Firebrick red
        btnCreateBoss.setForeground(Color.WHITE);
        btnCreateBoss.setText("Create Boss");
        btnCreateBoss.setFocusable(false);
        btnCreateBoss.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnCreateBoss.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 45));
        btnCreateBoss.addActionListener(this::btnCreateBossActionPerformed);
        jToolBar1.add(btnCreateBoss);

        btnEffectEditor = new JButton();
        btnEffectEditor.setBackground(new Color(255, 105, 180)); // Hot Pink
        btnEffectEditor.setForeground(Color.WHITE);
        btnEffectEditor.setText("Effect editor");
        btnEffectEditor.setFocusable(false);
        btnEffectEditor.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnEffectEditor.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 45));
        btnEffectEditor.addActionListener(this::btnEffectEditorActionPerformed);
        jToolBar1.add(btnEffectEditor);

        jPanel1.setPreferredSize(new Dimension(1200, 950));
        jPanel1.setLayout(new BorderLayout());

        desktop.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        desktop.setPreferredSize(new Dimension(1200, 950));

        GroupLayout desktopLayout = new GroupLayout(desktop);
        desktop.setLayout(desktopLayout);
        desktopLayout.setHorizontalGroup(desktopLayout.createParallelGroup(Alignment.LEADING).addGap(0, 32767, 32767));
        desktopLayout.setVerticalGroup(desktopLayout.createParallelGroup(Alignment.LEADING).addGap(0, 853, 32767));

        jPanel1.add(desktop, BorderLayout.CENTER);
        jScrollPane1.setViewportView(jPanel1);

        jMenu1.setText("Draw Map Tool");

        btnDrawMap.setAccelerator(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        btnDrawMap.setText("New Draw Map Window");
        btnDrawMap.addActionListener(this::btnDrawMapActionPerformed);
        jMenu1.add(btnDrawMap);

        jMenuBar1.add(jMenu1);
        setJMenuBar(jMenuBar1);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(jToolBar1, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 1669, Short.MAX_VALUE)));
        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 872, Short.MAX_VALUE)
                        .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jToolBar1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE)));

        pack();
    }

    private void btnFullScreenActionPerformed(ActionEvent evt) {
        fullScr = !fullScr;
        if (fullScr) {
            cX = getX();
            cY = getY();
            cW = getWidth();
            cH = getHeight();
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            btnFullScreen.setBackground(unFullScreen);
        } else {
            setSize(new Dimension(cW, cH));
            setLocation(cX, cY);
            btnFullScreen.setBackground(fullScreen);
        }
        dispose();
        setUndecorated(fullScr);
        setVisible(true);
    }

    private void btnDrawMapActionPerformed(ActionEvent evt) {
        openDrawMap();
    }

    private void minimizeAllFrames() {
        for (javax.swing.JInternalFrame frame : desktop.getAllFrames()) {
            try {
                if (!frame.isIcon()) {
                    frame.setIcon(true);
                }
            } catch (java.beans.PropertyVetoException e) {
                e.printStackTrace();
            }
        }
    }

    private void openDrawMap() {
        minimizeAllFrames();
        for (javax.swing.JInternalFrame frame : desktop.getAllFrames()) {
            if (frame instanceof DrawMapScr) {
                try {
                    if (frame.isIcon()) {
                        frame.setIcon(false);
                    }
                    frame.setSelected(true);
                    frame.moveToFront();
                } catch (java.beans.PropertyVetoException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        DrawMapScr drawMapScr = new DrawMapScr();
        desktop.add(drawMapScr);
        drawMapScr.setLocation(0, 0);
        drawMapScr.setVisible(true);
    }

    private void btnPathEditorActionPerformed(ActionEvent evt) {
        openPathEditor();
    }

    private void openPathEditor() {
        openExternalTool(
                "Path Editor",
                "src/PathEditor",
                "NROPartEditor.exe",
                "Chỉnh sửa animation path cho nhân vật NRO");
    }

    private void btnCreateItemIconActionPerformed(ActionEvent evt) {
        openSpriteEditor();
    }

    private void btnMobEditorActionPerformed(ActionEvent evt) {
        openMobEditor();
    }

    private void openMobEditor() {
        openExternalTool(
                "Mob Editor",
                "src/ModEditor",
                "MobEditor.exe",
                "Chỉnh sửa thông tin quái (Mob) cho game NRO");
    }

    private void btnImageScalerActionPerformed(ActionEvent evt) {
        openImageScaler();
    }

    private void openImageScaler() {
        minimizeAllFrames();
        // Check existing
        for (javax.swing.JInternalFrame frame : desktop.getAllFrames()) {
            if (frame instanceof com.girlkun.tool.screens.image_scaler.ImageScalerDialog) {
                try {
                    if (frame.isIcon()) {
                        frame.setIcon(false);
                    }
                    frame.setSelected(true);
                    frame.moveToFront();
                } catch (java.beans.PropertyVetoException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

        // New instance
        com.girlkun.tool.screens.image_scaler.ImageScalerDialog imageScaler = new com.girlkun.tool.screens.image_scaler.ImageScalerDialog();
        desktop.add(imageScaler);
        imageScaler.setLocation(0, 0);
        imageScaler.setVisible(true);
        try {
            imageScaler.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    private void btnTeaShopManagerActionPerformed(ActionEvent evt) {
        openTeaShopManager();
    }

    private void openTeaShopManager() {
        minimizeAllFrames();
        for (javax.swing.JInternalFrame frame : desktop.getAllFrames()) {
            if (frame instanceof com.girlkun.tool.shopmanager.ui.ShopManagerScr) {
                try {
                    if (frame.isIcon()) {
                        frame.setIcon(false);
                    }
                    frame.setSelected(true);
                    frame.moveToFront();
                } catch (java.beans.PropertyVetoException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

        com.girlkun.tool.shopmanager.ui.ShopManagerScr scr = new com.girlkun.tool.shopmanager.ui.ShopManagerScr();
        desktop.add(scr);
        scr.setLocation(0, 0);
        scr.setVisible(true);
        try {
            scr.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    private void btnCreateNPCActionPerformed(ActionEvent evt) {
        openCreateNPC();
    }

    private void openCreateNPC() {
        minimizeAllFrames();
        // Check existing
        for (javax.swing.JInternalFrame frame : desktop.getAllFrames()) {
            if (frame instanceof CreateNPCScr) {
                try {
                    if (frame.isIcon()) {
                        frame.setIcon(false);
                    }
                    frame.setSelected(true);
                    frame.moveToFront();
                } catch (java.beans.PropertyVetoException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

        // New instance
        CreateNPCScr createNPCScr = new CreateNPCScr();
        desktop.add(createNPCScr);
        createNPCScr.setLocation(0, 0);
        createNPCScr.setVisible(true);
        try {
            createNPCScr.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    private void btnDbConfigActionPerformed(ActionEvent evt) {
        openDbConfig();
    }

    private void openDbConfig() {
        minimizeAllFrames();
        // Check existing
        for (javax.swing.JInternalFrame frame : desktop.getAllFrames()) {
            if (frame instanceof com.girlkun.tool.shopmanager.ui.DbConfigScr) {
                try {
                    if (frame.isIcon()) {
                        frame.setIcon(false);
                    }
                    frame.setSelected(true);
                    frame.moveToFront();
                } catch (java.beans.PropertyVetoException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

        // New instance
        com.girlkun.tool.shopmanager.ui.DbConfigScr dbConfigScr = new com.girlkun.tool.shopmanager.ui.DbConfigScr();
        desktop.add(dbConfigScr);
        dbConfigScr.setLocation(0, 0);
        dbConfigScr.setVisible(true);
        try {
            dbConfigScr.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    private void btnCreateBossActionPerformed(ActionEvent evt) {
        openCreateBoss();
    }

    private void openCreateBoss() {
        minimizeAllFrames();
        // Check existing
        for (javax.swing.JInternalFrame frame : desktop.getAllFrames()) {
            if (frame instanceof com.girlkun.tool.screens.boss_scr.CreateBossScr) {
                try {
                    if (frame.isIcon()) {
                        frame.setIcon(false);
                    }
                    frame.setSelected(true);
                    frame.moveToFront();
                } catch (java.beans.PropertyVetoException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

        // New instance
        com.girlkun.tool.screens.boss_scr.CreateBossScr createBossScr = new com.girlkun.tool.screens.boss_scr.CreateBossScr();
        desktop.add(createBossScr);
        createBossScr.setLocation(0, 0);
        createBossScr.setVisible(true);
        try {
            createBossScr.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    private void btnEffectEditorActionPerformed(ActionEvent evt) {
        openEffectEditor();
    }

    private void openEffectEditor() {
        openExternalTool(
                "Effect Editor",
                "src/EffectEditor",
                "EffectEditor.exe",
                "Chỉnh sửa hiệu ứng cho game NRO");
    }

    private void openSpriteEditor() {
        openExternalTool(
                "Sprite Editor",
                "src/CreateItemIcon",
                "SpriteEditor.exe",
                "Tạo và chỉnh sửa Item icon, sprite");
    }

    private void openExternalTool(String name, String path, String exe, String desc) {
        minimizeAllFrames();
        // Check existing
        for (javax.swing.JInternalFrame frame : desktop.getAllFrames()) {
            if (frame instanceof com.girlkun.tool.screens.common.ExternalToolLauncher) {
                com.girlkun.tool.screens.common.ExternalToolLauncher tool = (com.girlkun.tool.screens.common.ExternalToolLauncher) frame;
                if (tool.getToolName().equals(name)) {
                    try {
                        if (frame.isIcon()) {
                            frame.setIcon(false);
                        }
                        frame.setSelected(true);
                        frame.moveToFront();
                    } catch (java.beans.PropertyVetoException e) {
                        e.printStackTrace();
                    }
                    return;
                }
            }
        }

        // New instance
        com.girlkun.tool.screens.common.ExternalToolLauncher launcher = new com.girlkun.tool.screens.common.ExternalToolLauncher(
                name, path, exe, desc);
        desktop.add(launcher);

        // Position - neo ở góc trên bên trái
        launcher.setLocation(0, 0);
        launcher.setVisible(true);
        try {
            launcher.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    private void setup() {
        setTitle("Draw Map Tool - NRO");
        setSize(1920, 1000);
        setLocationRelativeTo(null);
        jScrollPane1.getHorizontalScrollBar().setUnitIncrement(64);
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(64);
    }

    public static void main(String[] args) {
        // Setup Look and Feel
        FlatDarkPurpleIJTheme.setup();

        EventQueue.invokeLater(() -> {
            System.out.println("=== Draw Map Tool ===");
            System.out.println("Đang load dữ liệu...");

            // Load Manager (database data)
            Manager.gI();

            // Connect Shop Manager DB in background
            com.girlkun.tool.shopmanager.services.ShopManagerDAO.gI().initBackground();

            // Show Main window
            new Main().setVisible(true);
        });
    }

    public JDesktopPane getDesktop() {
        return desktop;
    }
}
