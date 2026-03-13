package com.girlkun.tool.screens.sprite_scr;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

public class SpriteEditorScr extends JInternalFrame {
    private PreviewCrop preview;
    private CanvasView canvas;
    private FrameList frameList;
    private JTree outputTree;
    
    public SpriteEditorScr() {
        super("Sprite Editor", true, true, true, true);
        setSize(1600, 900);
        
        setupUI();
        initOutputDirectory();
    }

    private void setupUI() {
        JPanel central = new JPanel(new BorderLayout());
        setContentPane(central);

        // Components
        preview = new PreviewCrop();
        canvas = new CanvasView();
        frameList = new FrameList();

        // Left Panel: Atlas + Tree
        JPanel leftPanel = new JPanel(new BorderLayout());
        JButton btnLoadAtlas = createStyledButton("Load Atlas", new Color(0, 153, 204));
        btnLoadAtlas.addActionListener(e -> loadAtlas());
        
        JPanel atlasTop = new JPanel(new BorderLayout());
        atlasTop.add(btnLoadAtlas, BorderLayout.NORTH);
        atlasTop.add(preview, BorderLayout.CENTER);
        
        // Output Tree (Simplified version)
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.add(new JLabel("Output Folder"), BorderLayout.NORTH);
        outputTree = new JTree(new DefaultMutableTreeNode("ItemIcon")); 
        refreshOutputTree();
        outputPanel.add(new JScrollPane(outputTree), BorderLayout.CENTER);
        
        JPanel outputBtns = new JPanel(new GridLayout(2, 1));
        JButton btnOpenOutput = createStyledButton("Open Output Folder", new Color(153, 102, 0));
        btnOpenOutput.addActionListener(e -> openOutputFolder());
        JButton btnDeleteOutput = createStyledButton("Delete Output", new Color(178, 34, 34));
        btnDeleteOutput.addActionListener(e -> deleteOutput());
        outputBtns.add(btnOpenOutput);
        outputBtns.add(btnDeleteOutput);
        outputPanel.add(outputBtns, BorderLayout.SOUTH);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, atlasTop, outputPanel);
        leftSplit.setDividerLocation(500);
        leftPanel.add(leftSplit, BorderLayout.CENTER);

        // Center Panel: Canvas
        JPanel centerPanel = new JPanel(new BorderLayout());
        JPanel canvasTools = new JPanel(new FlowLayout(FlowLayout.LEFT));
        canvasTools.add(new JLabel("Canvas Editor"));
        JButton btnAddRef = createStyledButton("+ Add Ref Img", new Color(0, 153, 51));
        JButton btnResetCam = createStyledButton("Về giữa màn hình", new Color(102, 102, 102));
        canvasTools.add(Box.createHorizontalGlue());
        canvasTools.add(btnResetCam);
        canvasTools.add(btnAddRef);
        
        centerPanel.add(canvasTools, BorderLayout.NORTH);
        centerPanel.add(canvas, BorderLayout.CENTER);

        // Right Panel: Frame List
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Frame List"), BorderLayout.NORTH);
        rightPanel.add(frameList, BorderLayout.CENTER);
        JButton btnSaveAll = createStyledButton("Save All Frames", new Color(102, 0, 153));
        btnSaveAll.addActionListener(e -> saveAllFrames());
        rightPanel.add(btnSaveAll, BorderLayout.SOUTH);

        // Main Splitters
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setLeftComponent(leftPanel);
        
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightSplit.setLeftComponent(centerPanel);
        rightSplit.setRightComponent(rightPanel);
        
        mainSplit.setRightComponent(rightSplit);
        mainSplit.setDividerLocation(400);
        rightSplit.setDividerLocation(800);

        central.add(mainSplit, BorderLayout.CENTER);

        // Connect Signals
        preview.setOnCropListener(img -> {
            FrameData data = new FrameData(img);
            frameList.addFrame(data);
            canvas.syncFrames(frameList.getAllFrames());
        });

        frameList.setOnSelectionChanged(selected -> {
            canvas.updateSelection(selected);
        });

        frameList.setOnDataChanged(() -> {
            canvas.syncFrames(frameList.getAllFrames());
        });

        canvas.setOnSelectionChanged(selected -> {
            frameList.setSelectedFrames(selected);
        });

        canvas.setOnModified(() -> {
            frameList.refresh();
        });

        frameList.setOnSaveSelected(selected -> {
            if (selected.isEmpty()) {
                JOptionPane.showInternalMessageDialog(this, "No frames selected to save.");
                return;
            }
            saveFrames(selected, "Save Selected Frames");
        });

        btnAddRef.addActionListener(e -> addCanvasRef());
        btnResetCam.addActionListener(e -> canvas.resetCamera());
    }

    private void loadAtlas() {
        FileDialog fd = new FileDialog(com.girlkun.tool.main.Main.I, "Load Atlas", FileDialog.LOAD);
        fd.setFilenameFilter((dir, name) -> {
            String lowercaseName = name.toLowerCase();
            return lowercaseName.endsWith(".png") || lowercaseName.endsWith(".jpg") || lowercaseName.endsWith(".jpeg");
        });
        fd.setVisible(true);
        
        if (fd.getFile() != null) {
            File selectedFile = new File(fd.getDirectory(), fd.getFile());
            try {
                BufferedImage img = ImageIO.read(selectedFile);
                preview.loadImage(img);
            } catch (IOException e) {
                JOptionPane.showInternalMessageDialog(this, "Error loading image: " + e.getMessage());
            }
        }
    }

    private void addCanvasRef() {
        FileDialog fd = new FileDialog(com.girlkun.tool.main.Main.I, "Add Reference Image", FileDialog.LOAD);
        fd.setVisible(true);
        
        if (fd.getFile() != null) {
            File selectedFile = new File(fd.getDirectory(), fd.getFile());
            canvas.addCustomReference(selectedFile.getAbsolutePath());
        }
    }

    private void initOutputDirectory() {
        File out = new File("output/ItemIcon");
        if (!out.exists()) out.mkdirs();
        String[] sub = {"x1", "x2", "x3", "x4"};
        for (String s : sub) {
            File f = new File(out, s);
            if (!f.exists()) f.mkdir();
        }
    }

    private void refreshOutputTree() {
        File root = new File("output/ItemIcon");
        if (!root.exists()) return;
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("ItemIcon");
        addFiles(root, rootNode);
        outputTree.setModel(new DefaultTreeModel(rootNode));
    }

    private void addFiles(File file, DefaultMutableTreeNode node) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(f.getName());
                node.add(child);
                if (f.isDirectory()) {
                    addFiles(f, child);
                }
            }
        }
    }

    private void openOutputFolder() {
        try {
            Desktop.getDesktop().open(new File("output/ItemIcon"));
        } catch (Exception e) {
            JOptionPane.showInternalMessageDialog(this, "Could not open folder: " + e.getMessage());
        }
    }

    private void saveAllFrames() {
        saveFrames(frameList.getAllFrames(), "Save All Frames");
    }

    private void saveFrames(List<FrameData> frames, String title) {
        if (frames.isEmpty()) return;

        String name = JOptionPane.showInternalInputDialog(this, "Enter Sprite Name Prefix (e.g. 'Goku'):", title, JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        name = name.replaceAll("[^a-zA-Z0-9 _-]", "").trim();

        File outRoot = new File("output/ItemIcon");
        try {
            for (int i = 0; i < frames.size(); i++) {
                FrameData f = frames.get(i);
                String filename = (frames.size() > 1 ? name + "_" + i : name) + ".png";
                
                // Get scaled x4 (original edited size in editor is treated as x4)
                BufferedImage imgX4 = new BufferedImage((int)f.getDisplayWidth(), (int)f.getDisplayHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g4 = imgX4.createGraphics();
                g4.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g4.drawImage(f.getOriginalImage(), 0, 0, (int)f.getDisplayWidth(), (int)f.getDisplayHeight(), null);
                g4.dispose();
                
                ImageIO.write(imgX4, "PNG", new File(new File(outRoot, "x4"), filename));
                
                // x1
                int w1 = Math.max(1, (int)f.getDisplayWidth() / 4);
                int h1 = Math.max(1, (int)f.getDisplayHeight() / 4);
                saveScaled(imgX4, w1, h1, new File(new File(outRoot, "x1"), filename));
                
                // x2
                saveScaled(imgX4, w1 * 2, h1 * 2, new File(new File(outRoot, "x2"), filename));
                
                // x3
                saveScaled(imgX4, w1 * 3, h1 * 3, new File(new File(outRoot, "x3"), filename));
            }
            JOptionPane.showInternalMessageDialog(this, "Saved " + frames.size() + " frames successfully.");
            refreshOutputTree();
        } catch (Exception e) {
            JOptionPane.showInternalMessageDialog(this, "Error saving frames: " + e.getMessage());
        }
    }

    private void saveScaled(BufferedImage src, int w, int h, File target) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        ImageIO.write(img, "PNG", target);
    }

    private void deleteOutput() {
        int confirm = JOptionPane.showInternalConfirmDialog(this, "Delete all files in output/ItemIcon directory?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            deleteFolder(new File("output/ItemIcon"));
            initOutputDirectory();
            refreshOutputTree();
        }
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

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteFolder(f);
                else f.delete();
            }
        }
    }
}
