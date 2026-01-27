package com.girlkun.tool.screens.draw_map_scr;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * ImagePickerDialog - Hiển thị danh sách ảnh dạng thumbnail giống Explorer
 * với tree folder bên trái
 */
public class ImagePickerDialog extends JDialog {

    private static final int THUMBNAIL_SIZE = 100;
    private static final int GRID_GAP = 10;
    private static final int COLUMNS = 5;

    private File selectedFile;
    private JPanel gridPanel;
    private JLabel statusLabel;
    private JTextField searchField;
    private List<ImageItem> imageItems = new ArrayList<>();
    private List<ImageItem> filteredItems = new ArrayList<>();
    private ImageItem selectedItem;
    private BufferedImage folderThumbnail;
    private SwingWorker<Void, ImageItem> currentWorker;

    // Tree components
    private JTree folderTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private File currentDirectory;
    private String initialDirectory;

    public ImagePickerDialog(Window owner, String title, String initialDir) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.initialDirectory = initialDir;
        loadFolderIcons();
        initUI();
        initTreeWithDirectory(initialDir);
        setSize(1100, 700);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(45, 45, 48));

        // TOP: Search Bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(30, 30, 32));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel searchLabel = new JLabel("🔍 Tìm kiếm: ");
        searchLabel.setForeground(Color.WHITE);
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBackground(new Color(60, 60, 65));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 85)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterImages();
            }
        });

        JButton btnRefresh = createStyledButton("� Refresh", new Color(66, 133, 244), Color.WHITE);
        btnRefresh.addActionListener(e -> {
            if (currentDirectory != null) {
                loadImages(currentDirectory.getAbsolutePath());
            }
        });

        topPanel.add(searchLabel, BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(btnRefresh, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // LEFT: Folder Tree
        JPanel treePanel = createTreePanel();

        // CENTER: Image Grid
        JPanel imagePanel = createImagePanel();

        // SplitPane for Tree and Images
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, imagePanel);
        splitPane.setDividerLocation(280);
        splitPane.setDividerSize(4);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setBackground(new Color(45, 45, 48));
        add(splitPane, BorderLayout.CENTER);

        // BOTTOM: Status and Buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(30, 30, 32));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        statusLabel = new JLabel("Chưa chọn ảnh nào");
        statusLabel.setForeground(new Color(180, 180, 180));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton btnCancel = createStyledButton("Hủy", new Color(108, 117, 125), Color.WHITE);
        btnCancel.addActionListener(e -> {
            selectedFile = null;
            dispose();
        });

        JButton btnSelect = createStyledButton("Chọn ảnh này", new Color(40, 167, 69), Color.WHITE);
        btnSelect.addActionListener(e -> {
            if (selectedItem != null) {
                selectedFile = selectedItem.file;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một ảnh!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        });

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSelect);

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createTreePanel() {
        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBackground(new Color(35, 35, 38));
        treePanel.setPreferredSize(new Dimension(280, 0));

        // Tree header
        JLabel treeHeader = new JLabel("📁 Thư mục");
        treeHeader.setForeground(Color.WHITE);
        treeHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        treeHeader.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));
        treeHeader.setBackground(new Color(30, 30, 32));
        treeHeader.setOpaque(true);
        treePanel.add(treeHeader, BorderLayout.NORTH);

        // Create tree
        rootNode = new DefaultMutableTreeNode("Root");
        treeModel = new DefaultTreeModel(rootNode);
        folderTree = new JTree(treeModel);
        folderTree.setRootVisible(false);
        folderTree.setShowsRootHandles(true);
        folderTree.setBackground(new Color(35, 35, 38));
        folderTree.setForeground(Color.WHITE);
        folderTree.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Custom tree renderer
        folderTree.setCellRenderer(new FolderTreeCellRenderer());

        // Tree selection listener
        folderTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
            if (node != null && node.getUserObject() instanceof FolderNode) {
                FolderNode folderNode = (FolderNode) node.getUserObject();
                currentDirectory = folderNode.file;
                loadImages(folderNode.file.getAbsolutePath());
            }
        });

        // Lazy loading - load children when expanding
        folderTree.addTreeWillExpandListener(new javax.swing.event.TreeWillExpandListener() {
            @Override
            public void treeWillExpand(javax.swing.event.TreeExpansionEvent event) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                loadChildFolders(node);
            }

            @Override
            public void treeWillCollapse(javax.swing.event.TreeExpansionEvent event) {
                // Do nothing
            }
        });

        JScrollPane treeScroll = new JScrollPane(folderTree);
        treeScroll.setBorder(BorderFactory.createEmptyBorder());
        treeScroll.getViewport().setBackground(new Color(35, 35, 38));
        treePanel.add(treeScroll, BorderLayout.CENTER);

        return treePanel;
    }

    private JPanel createImagePanel() {
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBackground(new Color(45, 45, 48));

        // Use a wrapper to prevent vertical stretching of grid cells
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(45, 45, 48));

        gridPanel = new JPanel();
        gridPanel.setBackground(new Color(45, 45, 48));
        GridLayout gridLayout = new GridLayout(0, 1, GRID_GAP, GRID_GAP);
        gridPanel.setLayout(gridLayout);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(GRID_GAP, GRID_GAP, GRID_GAP, GRID_GAP));

        wrapper.add(gridPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBackground(new Color(45, 45, 48));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(new Color(45, 45, 48));

        // Dynamic column calculation
        wrapper.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = wrapper.getWidth();
                // Estimate item width including gaps
                // Item pref width is THUMBNAIL_SIZE + 10. + Gap.
                int itemWidth = THUMBNAIL_SIZE + 20;
                int columns = Math.max(1, (width - GRID_GAP) / (itemWidth + GRID_GAP));

                if (columns != gridLayout.getColumns()) {
                    gridLayout.setColumns(columns);
                    gridPanel.revalidate();
                }
            }
        });

        imagePanel.add(scrollPane, BorderLayout.CENTER);

        return imagePanel;
    }

    private void initTreeWithDirectory(String directory) {
        rootNode.removeAllChildren();

        File[] roots = File.listRoots();
        File dir = new File(directory);
        boolean dirExists = directory != null && !directory.isEmpty() && dir.exists();

        // Tìm root drive của thư mục target (ví dụ E:\)
        File rootOfDir = null;
        if (dirExists) {
            File temp = dir;
            while (temp.getParentFile() != null) {
                temp = temp.getParentFile();
            }
            rootOfDir = temp;
        }

        DefaultMutableTreeNode targetRootNode = null;

        // Xây dựng tầng 1 (Drives)
        for (File root : roots) {
            FolderNode folderNode = new FolderNode(root, root.getAbsolutePath());
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(folderNode);
            // Add dummy child for lazy loading
            node.add(new DefaultMutableTreeNode("Loading..."));
            rootNode.add(node);

            // Nếu drive này dẫn tới thư mục đích
            if (dirExists && rootOfDir != null && root.equals(rootOfDir)) {
                targetRootNode = node;
            }
        }

        treeModel.reload();

        // Tự động mở đường dẫn tới thư mục ban đầu
        if (dirExists && targetRootNode != null) {
            // Danh sách các folder cần mở từ root -> target (không bao gồm root)
            java.util.List<File> pathToDir = new ArrayList<>();
            File temp = dir;

            // Truy vết ngược từ target lên drive
            File rootFile = ((FolderNode) targetRootNode.getUserObject()).file;
            while (temp != null && !temp.equals(rootFile)) {
                pathToDir.add(0, temp);
                temp = temp.getParentFile();
            }

            // Bắt đầu mở rộng từ node drive
            DefaultMutableTreeNode currentNode = targetRootNode;

            // Expand node hiện tại để load con
            expandNode(currentNode);

            // Duyệt qua từng cấp folder trong path
            for (File pathFile : pathToDir) {
                // Tìm node con tương ứng với pathFile
                boolean found = false;
                for (int i = 0; i < currentNode.getChildCount(); i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) currentNode.getChildAt(i);
                    if (child.getUserObject() instanceof FolderNode) {
                        FolderNode fn = (FolderNode) child.getUserObject();
                        // So sánh absolute path hoặc file object
                        if (fn.file.equals(pathFile)) {
                            currentNode = child; // Đi sâu xuống
                            expandNode(currentNode); // Expand tiếp
                            found = true;
                            break;
                        }
                    }
                }

                // Nếu đường dẫn bị đứt đoạn (không tìm thấy subfolder), dừng lại
                if (!found)
                    break;
            }

            // Cuối cùng: Select node đích
            TreePath path = new TreePath(currentNode.getPath());
            folderTree.setSelectionPath(path);
            folderTree.scrollPathToVisible(path);

            // Load ảnh
            currentDirectory = dir;
            loadImages(directory);
        }
    }

    // Helper để expand và load lazy children
    private void expandNode(DefaultMutableTreeNode node) {
        // Load children thật nếu chưa load
        loadChildFolders(node);
        // Báo model update structure
        treeModel.reload(node);
        // Mở rộng trên UI
        folderTree.expandPath(new TreePath(node.getPath()));
    }

    private void expandToNode(DefaultMutableTreeNode node) {
        if (node != null) {
            TreePath path = new TreePath(node.getPath());
            folderTree.expandPath(path.getParentPath());
        }
    }

    private void loadChildFolders(DefaultMutableTreeNode node) {
        if (node.getUserObject() instanceof FolderNode) {
            FolderNode folderNode = (FolderNode) node.getUserObject();

            // Check if already loaded (not dummy)
            if (node.getChildCount() == 1) {
                Object firstChild = ((DefaultMutableTreeNode) node.getFirstChild()).getUserObject();
                if (firstChild instanceof String && "Loading...".equals(firstChild)) {
                    node.removeAllChildren();
                    loadFolderChildren(node, folderNode.file);
                    treeModel.reload(node);
                }
            } else if (node.getChildCount() == 0) {
                loadFolderChildren(node, folderNode.file);
                treeModel.reload(node);
            }
        }
    }

    private void loadFolderChildren(DefaultMutableTreeNode parentNode, File folder) {
        File[] subDirs = folder.listFiles(File::isDirectory);
        if (subDirs != null) {
            // Sort by name
            Arrays.sort(subDirs, Comparator.comparing(f -> f.getName().toLowerCase()));

            for (File subDir : subDirs) {
                // Skip hidden and system folders
                if (subDir.isHidden() || subDir.getName().startsWith(".")) {
                    continue;
                }

                FolderNode folderNode = new FolderNode(subDir, subDir.getName());
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(folderNode);

                // Add dummy child if has subdirectories
                File[] subSubDirs = subDir.listFiles(File::isDirectory);
                if (subSubDirs != null && subSubDirs.length > 0) {
                    childNode.add(new DefaultMutableTreeNode("Loading..."));
                }

                parentNode.add(childNode);
            }
        }
    }

    private void loadFolderIcons() {
        try {
            File iconFile = new File("data/folder.png");
            if (iconFile.exists()) {
                BufferedImage original = ImageIO.read(iconFile);
                folderThumbnail = createThumbnail(original, THUMBNAIL_SIZE);
            } else {
                folderThumbnail = new BufferedImage(THUMBNAIL_SIZE, THUMBNAIL_SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = folderThumbnail.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(220, 180, 50));
                g.fillRoundRect(10, 20, 80, 60, 5, 5);
                g.fillRoundRect(10, 10, 40, 20, 5, 5);
                g.dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadImages(String directory) {
        // Cancel previous worker if running
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        imageItems.clear();
        filteredItems.clear();
        gridPanel.removeAll();
        selectedItem = null;

        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            statusLabel.setText("Thư mục không tồn tại: " + directory);
            statusLabel.setForeground(new Color(220, 53, 69));
            gridPanel.revalidate();
            gridPanel.repaint();
            return;
        }

        Comparator<File> sorter = (f1, f2) -> {
            String name1 = f1.getName();
            String name2 = f2.getName();
            String base1 = name1.contains(".") ? name1.substring(0, name1.lastIndexOf('.')) : name1;
            String base2 = name2.contains(".") ? name2.substring(0, name2.lastIndexOf('.')) : name2;
            try {
                int num1 = Integer.parseInt(base1);
                int num2 = Integer.parseInt(base2);
                return Integer.compare(num1, num2);
            } catch (NumberFormatException e) {
                return name1.compareToIgnoreCase(name2);
            }
        };

        // 1. Load Sub-directories (Display immediately)
        File[] subDirs = dir.listFiles(File::isDirectory);
        if (subDirs != null) {
            Arrays.sort(subDirs, sorter);
            for (File folder : subDirs) {
                if (!folder.isHidden() && !folder.getName().startsWith(".")) {
                    ImageItem item = new ImageItem(folder, true, 0, 0, folderThumbnail);
                    imageItems.add(item);
                    filteredItems.add(item);
                    addImageToGrid(item);
                }
            }
        }

        // 2. Load Images (Background Task)
        File[] files = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".gif");
        });

        // NOTICE: This tool is NOT recursive. It only lists files in the current
        // directory.
        // If images appear, they are physically present in this folder.

        if (files != null)
            Arrays.sort(files, sorter);

        final File[] imageFiles = (files != null) ? files : new File[0];

        if (imageItems.isEmpty() && imageFiles.length == 0) {
            statusLabel.setText("Thư mục trống");
            statusLabel.setForeground(new Color(255, 193, 7));
            gridPanel.revalidate();
            gridPanel.repaint();
            return;
        }

        statusLabel.setText("Đang tải " + imageFiles.length + " ảnh...");
        statusLabel.setForeground(new Color(66, 133, 244));

        currentWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (File file : imageFiles) {
                    if (isCancelled())
                        return null;

                    try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
                        if (in == null)
                            continue;

                        Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
                        if (readers.hasNext()) {
                            ImageReader reader = readers.next();
                            try {
                                reader.setInput(in);

                                // Get dimensions without loading the whole image
                                int width = reader.getWidth(0);
                                int height = reader.getHeight(0);

                                // Calculate subsampling (skip pixels) for faster loading
                                int sub = 1;
                                if (width > THUMBNAIL_SIZE * 2 || height > THUMBNAIL_SIZE * 2) {
                                    sub = Math.max(width / (THUMBNAIL_SIZE * 2), height / (THUMBNAIL_SIZE * 2));
                                }

                                ImageReadParam param = reader.getDefaultReadParam();
                                param.setSourceSubsampling(sub, sub, 0, 0);

                                BufferedImage smallImg = reader.read(0, param);
                                BufferedImage thumbnail = createThumbnail(smallImg, THUMBNAIL_SIZE);

                                publish(new ImageItem(file, false, width, height, thumbnail));
                            } finally {
                                reader.dispose();
                            }
                        }
                    } catch (IOException | OutOfMemoryError e) {
                        // Free memory if necessary
                        System.gc();
                    }
                }
                return null;
            }

            @Override
            protected void process(List<ImageItem> chunks) {
                if (isCancelled())
                    return;
                for (ImageItem item : chunks) {
                    imageItems.add(item);
                    filteredItems.add(item);
                    addImageToGrid(item);
                }
            }

            @Override
            protected void done() {
                if (!isCancelled()) {
                    statusLabel.setText("Hiển thị " + imageItems.size() + " mục | Click để chọn");
                    statusLabel.setForeground(new Color(40, 167, 69));
                }
            }
        };
        currentWorker.execute();
    }

    private BufferedImage createThumbnail(BufferedImage original, int size) {
        // Calculate scale to maintain aspect ratio
        double scale = Math.min((double) size / original.getWidth(), (double) size / original.getHeight());
        int newW = (int) (original.getWidth() * scale);
        int newH = (int) (original.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(new Color(55, 55, 60));
        g.fillRect(0, 0, size, size);

        // Center the image
        int x = (size - newW) / 2;
        int y = (size - newH) / 2;
        g.drawImage(original, x, y, newW, newH, null);
        g.dispose();

        return scaled;
    }

    private void addImageToGrid(ImageItem item) {
        JPanel card = createImageCard(item);
        gridPanel.add(card);
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel createImageCard(ImageItem item) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(55, 55, 60));
        card.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 75), 2));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(THUMBNAIL_SIZE + 10, THUMBNAIL_SIZE + 30));

        // Image/Icon
        JLabel imgLabel = new JLabel(new ImageIcon(item.thumbnail));
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 2, 5));
        card.add(imgLabel, BorderLayout.CENTER);

        // Filename
        String fileName = item.file.getName();
        if (fileName.length() > 15) {
            fileName = fileName.substring(0, 12) + "...";
        }
        JLabel nameLabel = new JLabel(fileName, SwingConstants.CENTER);
        nameLabel.setForeground(new Color(200, 200, 200));
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 5, 5));
        card.add(nameLabel, BorderLayout.SOUTH);

        // Tooltip
        if (item.isDirectory) {
            card.setToolTipText("<html><b>" + item.file.getName() + "</b><br>Thư mục</html>");
        } else {
            card.setToolTipText("<html><b>" + item.file.getName() + "</b><br>Kích thước: "
                    + item.originalWidth + " x " + item.originalHeight + "</html>");
        }

        // Click handler
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (item.isDirectory) {
                    if (e.getClickCount() == 2) {
                        navigateToFolder(item.file);
                    }
                    // For directories, maybe we update selection too?
                    // But typically 'selecting' a directory in an Image Picker (which is for
                    // picking files)
                    // doesn't do much. But we'll highlight it anyway.
                    selectImage(item, card);
                } else {
                    selectImage(item, card);
                    if (e.getClickCount() == 2) {
                        selectedFile = item.file;
                        dispose();
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (item != selectedItem) {
                    card.setBackground(new Color(70, 70, 75));
                    card.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 105), 2));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (item != selectedItem) {
                    card.setBackground(new Color(55, 55, 60));
                    card.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 75), 2));
                }
            }
        });

        item.card = card;
        return card;
    }

    private void navigateToFolder(File folder) {
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
        if (currentNode == null)
            return;

        // Ensure expanded
        if (!folderTree.isExpanded(new TreePath(currentNode.getPath()))) {
            folderTree.expandPath(new TreePath(currentNode.getPath()));
        }

        // Use a slight delay to allow expansion listener to populate children first if
        // needed
        // But since loadChildFolders is synchronous in treeWillExpand, it typically
        // works immediately.

        for (int i = 0; i < currentNode.getChildCount(); i++) {
            TreeNode childNode = currentNode.getChildAt(i);
            if (childNode instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode dmtNode = (DefaultMutableTreeNode) childNode;
                if (dmtNode.getUserObject() instanceof FolderNode) {
                    if (((FolderNode) dmtNode.getUserObject()).file.equals(folder)) {
                        TreePath path = new TreePath(dmtNode.getPath());
                        folderTree.setSelectionPath(path);
                        folderTree.scrollPathToVisible(path);
                        return;
                    }
                }
            }
        }
    }

    private void selectImage(ImageItem item, JPanel card) {
        // Clear previous selection
        if (selectedItem != null && selectedItem.card != null) {
            selectedItem.card.setBackground(new Color(55, 55, 60));
            selectedItem.card.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 75), 2));
        }

        selectedItem = item;
        card.setBackground(new Color(0, 100, 180));
        card.setBorder(BorderFactory.createLineBorder(new Color(0, 150, 255), 3));

        statusLabel.setText("Đã chọn: " + item.file.getName() + " (" + item.originalWidth + "x"
                + item.originalHeight + ")");
        statusLabel.setForeground(new Color(66, 133, 244));
    }

    private void filterImages() {
        String query = searchField.getText().toLowerCase().trim();
        filteredItems.clear();
        gridPanel.removeAll();

        for (ImageItem item : imageItems) {
            if (item.file.getName().toLowerCase().contains(query)) {
                filteredItems.add(item);
                addImageToGrid(item);
            }
        }

        if (filteredItems.isEmpty()) {
            statusLabel.setText("Không tìm thấy ảnh phù hợp với: \"" + query + "\"");
            statusLabel.setForeground(new Color(255, 193, 7));
        } else {
            statusLabel.setText("Hiển thị " + filteredItems.size() + "/" + imageItems.size() + " ảnh");
            statusLabel.setForeground(new Color(40, 167, 69));
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(bgColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bgColor.brighter());
                } else {
                    g2.setColor(bgColor);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(fgColor);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    /**
     * Hiển thị dialog và trả về file được chọn
     */
    public File showDialog() {
        setVisible(true);
        return selectedFile;
    }

    /**
     * Phương thức tĩnh để mở Image Picker
     */
    public static File pickImage(Window owner, String title, String initialDir) {
        ImagePickerDialog dialog = new ImagePickerDialog(owner, title, initialDir);
        return dialog.showDialog();
    }

    // Inner class for folder node in tree
    private static class FolderNode {
        File file;
        String displayName;

        FolderNode(File file, String displayName) {
            this.file = file;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // Custom tree cell renderer
    private class FolderTreeCellRenderer extends DefaultTreeCellRenderer {

        private Icon folderIcon;

        public FolderTreeCellRenderer() {
            try {
                File iconFile = new File("data/folder.png");
                if (iconFile.exists()) {
                    ImageIcon original = new ImageIcon(ImageIO.read(iconFile));
                    Image scaled = original.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                    folderIcon = new ImageIcon(scaled);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {

            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            if (sel) {
                label.setBackground(new Color(0, 100, 180));
                label.setForeground(Color.WHITE);
                label.setOpaque(true);
            } else {
                label.setBackground(new Color(35, 35, 38));
                label.setForeground(new Color(220, 220, 220));
                label.setOpaque(true);
            }

            // Set folder icon
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node.getUserObject() instanceof FolderNode) {
                    if (folderIcon != null) {
                        label.setIcon(folderIcon);
                    } else {
                        label.setIcon(expanded ? getOpenIcon() : getClosedIcon());
                    }
                    label.setText(node.getUserObject().toString());
                }
            }

            label.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            return label;
        }
    }

    // Inner class for image item
    private static class ImageItem {
        File file;
        boolean isDirectory;
        int originalWidth;
        int originalHeight;
        BufferedImage thumbnail;
        JPanel card;

        ImageItem(File file, boolean isDirectory, int origW, int origH, BufferedImage thumbnail) {
            this.file = file;
            this.isDirectory = isDirectory;
            this.originalWidth = origW;
            this.originalHeight = origH;
            this.thumbnail = thumbnail;
        }
    }
}
