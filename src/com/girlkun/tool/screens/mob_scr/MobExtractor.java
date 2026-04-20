package com.girlkun.tool.screens.mob_scr;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * NRO Mob Data Extractor - JInternalFrame version
 * Converts NRO mob .bin data and associated atlas PNG into individual frame PNGs.
 */
public class MobExtractor extends JInternalFrame {

    private DefaultListModel<String> listModel;
    private JList<String> fileList;
    private JTextField outputField;
    private JTextArea logArea;
    private ButtonGroup scaleGroup;
    private List<String> files = new ArrayList<>();

    public MobExtractor() {
        super("NRO Mob Data Extractor", true, true, true, true);
        setSize(800, 600);
        initUI();
    }

    private void initUI() {
        Color bgColor = new Color(30,30,30);
        Color itemBgColor = new Color(18, 18, 18);
        Color textColor = new Color(255, 255, 255);
        Color accentColor = new Color(0, 120, 212);
        Color greenColor = new Color(76, 175, 80);
        Color redColor = new Color(211, 47, 47);
        Color grayColor = new Color(96, 125, 139);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(mainPanel);

        // Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerPanel.setBackground(bgColor);
        
        JButton chooseBtn = createStyledButton("Chọn file Mob data", greenColor);
        chooseBtn.addActionListener(e -> addFiles());
        headerPanel.add(chooseBtn);

        JButton clearBtn = createStyledButton("Clear List", redColor);
        clearBtn.addActionListener(e -> clearFiles());
        headerPanel.add(clearBtn);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Center Content
        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setBackground(bgColor);

        // List Section (Input Files)
        JPanel listOuter = new JPanel(new BorderLayout(0, 5));
        listOuter.setBackground(bgColor);
        JLabel listLabel = new JLabel("Danh sách file đã chọn:");
        listLabel.setForeground(textColor);
        listOuter.add(listLabel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setBackground(itemBgColor);
        fileList.setForeground(textColor);
        fileList.setSelectionBackground(new Color(0, 77, 128));
        fileList.setSelectionForeground(textColor);
        fileList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Enable Drag and Drop from Explorer
        fileList.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    java.util.List<File> droppedFiles = (java.util.List<File>) support.getTransferable().getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                    for (File f : droppedFiles) {
                        if (f.isFile() && (f.getName().toLowerCase().endsWith(".bin") || !f.getName().contains("."))) {
                            String path = f.getAbsolutePath();
                            if (!files.contains(path)) {
                                files.add(path);
                                listModel.addElement(path);
                            }
                        }
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        fileList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeSelectedFiles();
                }
            }
        });
        
        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setBorder(BorderFactory.createLineBorder(new Color(51, 51, 51)));
        listOuter.add(listScroll, BorderLayout.CENTER);
        centerPanel.add(listOuter, BorderLayout.CENTER);

        // Options Section
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBackground(bgColor);

        // Output Path
        JPanel outputPanel = new JPanel(new BorderLayout(10, 0));
        outputPanel.setBackground(bgColor);
        outputPanel.setMaximumSize(new Dimension(2000, 40));
        JLabel outputLabel = new JLabel("Thư mục đầu ra:");
        outputLabel.setForeground(textColor);
        outputPanel.add(outputLabel, BorderLayout.WEST);

        outputField = new JTextField();
        outputField.setText(new File("output").getAbsolutePath());
        outputField.setBackground(new Color(51, 51, 51));
        outputField.setForeground(textColor);
        outputPanel.add(outputField, BorderLayout.CENTER);

        JButton browseBtn = createStyledButton("Browse", grayColor);
        browseBtn.addActionListener(e -> browseOutput());
        outputPanel.add(browseBtn, BorderLayout.EAST);
        optionsPanel.add(outputPanel);
        optionsPanel.add(Box.createVerticalStrut(15));

        // Scale Section
        JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        scalePanel.setBackground(bgColor);
        JLabel scaleLabel = new JLabel("Tỉ lệ (Scale):");
        scaleLabel.setForeground(textColor);
        scalePanel.add(scaleLabel);

        scaleGroup = new ButtonGroup();
        for (int i = 1; i <= 4; i++) {
            JRadioButton rb = new JRadioButton("x" + i);
            rb.setActionCommand(String.valueOf(i));
            rb.setBackground(bgColor);
            rb.setForeground(new Color(204, 204, 204));
            if (i == 1) rb.setSelected(true);
            scaleGroup.add(rb);
            scalePanel.add(rb);
        }
        optionsPanel.add(scalePanel);
        centerPanel.add(optionsPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Footer Section
        JPanel footerPanel = new JPanel(new BorderLayout(0, 15));
        footerPanel.setBackground(bgColor);

        // Action Buttons
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setBackground(bgColor);
        btnPanel.setPreferredSize(new Dimension(0, 45));

        JButton startBtn = createStyledButton("BẮT ĐẦU TRÍCH XUẤT", accentColor);
        startBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        startBtn.addActionListener(e -> runExtract());
        btnPanel.add(startBtn);

        JButton openBtn = createStyledButton("Mở thư mục", grayColor);
        openBtn.addActionListener(e -> openOutputFolder());
        btnPanel.add(openBtn);
        footerPanel.add(btnPanel, BorderLayout.NORTH);

        // Log Section
        JPanel logOuter = new JPanel(new BorderLayout(0, 5));
        logOuter.setBackground(bgColor);
        JLabel logLabel = new JLabel("Nhật ký (Log):");
        logLabel.setForeground(textColor);
        logOuter.add(logLabel, BorderLayout.NORTH);

        logArea = new JTextArea(8, 0);
        logArea.setEditable(false);
        logArea.setBackground(itemBgColor);
        logArea.setForeground(new Color(212, 212, 212));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logOuter.add(new JScrollPane(logArea), BorderLayout.CENTER);
        footerPanel.add(logOuter, BorderLayout.CENTER);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        return btn;
    }

    private void addFiles() {
        FileDialog dialog = new FileDialog((Frame)null, "Chọn file Mob data", FileDialog.LOAD);
        dialog.setMultipleMode(true);
        dialog.setFile("*");
        dialog.setVisible(true);

        File[] selectedFiles = dialog.getFiles();
        if (selectedFiles != null && selectedFiles.length > 0) {
            for (File f : selectedFiles) {
                String path = f.getAbsolutePath();
                if (!files.contains(path)) {
                    files.add(path);
                    listModel.addElement(path);
                }
            }
        }
    }

    private void clearFiles() {
        files.clear();
        listModel.clear();
    }

    private void removeSelectedFiles() {
        int[] selectedIndices = fileList.getSelectedIndices();
        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            int index = selectedIndices[i];
            files.remove(index);
            listModel.remove(index);
        }
    }

    private void browseOutput() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Chọn thư mục đầu ra");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void openOutputFolder() {
        String path = outputField.getText().trim();
        File folder = new File(path);
        if (folder.exists()) {
            try {
                Desktop.getDesktop().open(folder);
            } catch (Exception e) {
                log("Không thể mở thư mục: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Thư mục không tồn tại!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void runExtract() {
        if (files.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bạn chưa chọn file nào!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String outBase = outputField.getText().trim();
        int tScale = Integer.parseInt(scaleGroup.getSelection().getActionCommand());

        logArea.setText("");
        log("--- Bắt đầu trích xuất ---");
        log("Scale mục tiêu: x" + tScale);

        new Thread(() -> {
            int errorCount = 0;
            for (String f : files) {
                try {
                    extractFrames(f, outBase, tScale);
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> log("LỖI NGHIÊM TRỌNG trên " + new File(f).getName() + ": " + e.getMessage()));
                    errorCount++;
                }
            }
            final int finalErrorCount = errorCount;
            SwingUtilities.invokeLater(() -> {
                log("--- Hoàn tất ---");
                if (finalErrorCount == 0) {
                    JOptionPane.showMessageDialog(this, "Đã trích xuất thành công tài nguyên!", "Xong", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Hoàn tất với " + finalErrorCount + " lỗi. Kiểm tra log.", "Xong", JOptionPane.WARNING_MESSAGE);
                }
                openOutputFolder();
            });
        }).start();
    }

    private void extractFrames(String filePath, String outputBase, int targetScale) throws Exception {
        File file = new File(filePath);
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        String nameNoExt = dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
        final String fn = fileName;
        SwingUtilities.invokeLater(() -> log("Processing: " + fn));

        MobData data = parseNroBin(filePath);
        if (data == null) {
            SwingUtilities.invokeLater(() -> log("Failed to parse " + file.getName()));
            return;
        }

        BufferedImage atlas = ImageIO.read(new ByteArrayInputStream(data.pngData));
        if (atlas == null) {
            SwingUtilities.invokeLater(() -> log("Failed to load image for " + file.getName()));
            return;
        }

        int metaW = data.maxXMeta == 0 ? 1 : data.maxXMeta;
        double scaleX = (double) atlas.getWidth() / metaW;
        int actualScale = (int) Math.round(scaleX);
        if (actualScale < 1) actualScale = 1;

        final int finalActualScale = actualScale;
        SwingUtilities.invokeLater(() -> {
            log("  Detected Actual Scale: x" + finalActualScale);
            log("  Target Output Scale: x" + targetScale);
        });

        Map<Integer, SpriteInfoLocal> spriteMap = new HashMap<>();
        for (SpriteInfoLocal info : data.imgInfo) {
            spriteMap.put(info.id, info);
        }

        for (int i = 0; i < data.frames.size(); i++) {
            List<PartLocal> parts = data.frames.get(i);
            if (parts.isEmpty()) continue;

            List<Rectangle> rects = new ArrayList<>();
            for (PartLocal p : parts) {
                SpriteInfoLocal sInfo = spriteMap.get(p.imgId);
                if (sInfo == null) continue;
                int w = sInfo.w * actualScale;
                int h = sInfo.h * actualScale;
                int dx = p.dx * actualScale;
                int dy = p.dy * actualScale;
                rects.add(new Rectangle(dx, dy, w, h));
            }

            if (rects.isEmpty()) continue;

            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

            for (Rectangle r : rects) {
                if (r.x < minX) minX = r.x;
                if (r.y < minY) minY = r.y;
                if (r.x + r.width > maxX) maxX = r.x + r.width;
                if (r.y + r.height > maxY) maxY = r.y + r.height;
            }

            int fw = maxX - minX;
            int fh = maxY - minY;
            if (fw <= 0 || fh <= 0) continue;

            BufferedImage frameImg = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frameImg.createGraphics();

            for (PartLocal p : parts) {
                SpriteInfoLocal sInfo = spriteMap.get(p.imgId);
                if (sInfo == null) continue;
                
                int sx = sInfo.x * actualScale;
                int sy = sInfo.y * actualScale;
                int sw = sInfo.w * actualScale;
                int sh = sInfo.h * actualScale;
                int dx = p.dx * actualScale;
                int dy = p.dy * actualScale;

                if (sx < 0) sx = 0;
                if (sy < 0) sy = 0;
                if (sx + sw > atlas.getWidth()) sw = atlas.getWidth() - sx;
                if (sy + sh > atlas.getHeight()) sh = atlas.getHeight() - sy;

                if (sw > 0 && sh > 0) {
                    BufferedImage spriteImg = atlas.getSubimage(sx, sy, sw, sh);
                    g.drawImage(spriteImg, dx - minX, dy - minY, null);
                }
            }
            g.dispose();

            if (targetScale != actualScale) {
                double ratio = (double) targetScale / actualScale;
                int targetW = (int) (fw * ratio);
                int targetH = (int) (fh * ratio);
                if (targetW > 0 && targetH > 0) {
                    BufferedImage scaledImg = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = scaledImg.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2.drawImage(frameImg, 0, 0, targetW, targetH, null);
                    g2.dispose();
                    frameImg = scaledImg;
                }
            }

            File mobDir = new File(outputBase, nameNoExt);
            if (!mobDir.exists()) mobDir.mkdirs();
            File outFile = new File(mobDir, nameNoExt + "_" + i + ".png");
            ImageIO.write(frameImg, "PNG", outFile);
        }

        SwingUtilities.invokeLater(() -> log("  Extracted " + data.frames.size() + " frames to " + nameNoExt));
    }

    private MobData parseNroBin(String path) throws IOException {
        byte[] fileData = Files.readAllBytes(Paths.get(path));
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(fileData));

        try {
            int tFlag = dis.readByte();
            int dLen = dis.readInt();
            
            int nImg = dis.readUnsignedByte();
            List<SpriteInfoLocal> imgInfo = new ArrayList<>();
            int maxX = 0;
            
            for (int i = 0; i < nImg; i++) {
                int id = dis.readByte();
                int x, y;
                if (tFlag == 1) {
                    x = dis.readUnsignedByte();
                    y = dis.readUnsignedByte();
                } else if (tFlag == 2) {
                    x = dis.readShort();
                    y = dis.readShort();
                } else {
                    x = dis.readUnsignedByte();
                    y = dis.readUnsignedByte();
                }
                int w = dis.readUnsignedByte();
                int h = dis.readUnsignedByte();
                
                imgInfo.add(new SpriteInfoLocal(id, x, y, w, h));
                if (x + w > maxX) maxX = x + w;
            }
            
            int nFrames = dis.readShort();
            List<List<PartLocal>> frames = new ArrayList<>();
            for (int i = 0; i < nFrames; i++) {
                int nParts = dis.readUnsignedByte();
                List<PartLocal> parts = new ArrayList<>();
                for (int j = 0; j < nParts; j++) {
                    int dx = dis.readShort();
                    int dy = dis.readShort();
                    int imgId = dis.readByte();
                    parts.add(new PartLocal(dx, dy, imgId));
                }
                frames.add(parts);
            }
            
            int pngStart = 5 + dLen; 
            if (pngStart + 4 <= fileData.length) {
                dis = new DataInputStream(new ByteArrayInputStream(fileData, pngStart, fileData.length - pngStart));
                int pngLen = dis.readInt();
                if (pngStart + 4 + pngLen <= fileData.length) {
                    byte[] pngData = new byte[pngLen];
                    dis.readFully(pngData);
                    
                    MobData md = new MobData();
                    md.imgInfo = imgInfo;
                    md.frames = frames;
                    md.pngData = pngData;
                    md.maxXMeta = maxX;
                    return md;
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static class MobData {
        List<SpriteInfoLocal> imgInfo;
        List<List<PartLocal>> frames;
        byte[] pngData;
        int maxXMeta;
    }

    static class SpriteInfoLocal {
        int id, x, y, w, h;
        SpriteInfoLocal(int id, int x, int y, int w, int h) {
            this.id = id; this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    static class PartLocal {
        int dx, dy, imgId;
        PartLocal(int dx, int dy, int imgId) {
            this.dx = dx; this.dy = dy; this.imgId = imgId;
        }
    }
}
