package com.example.noteuyg;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * AdvancedEditor - Comprehensive Java Swing text editor with modern UI
 */
public class AdvancedEditor extends JFrame {
    
    private static int newFileCounter = 1;
    private JTabbedPane tabbedPane;
    private boolean isDarkMode = false;
    private JLabel statusLabel;
    private JCheckBoxMenuItem darkModeMenuItem;
    private JSplitPane splitPane;
    private JTree fileTree;
    private DefaultMutableTreeNode rootNode;
    
    public AdvancedEditor() {
        // Set window properties
        setTitle("NotePad");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximize by default
        
        // Set application icon
        try {
            ImageIcon icon = new ImageIcon("C:\\projects\\logo.png");
            setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("Warning: Could not load application icon: " + e.getMessage());
        }
        
        // Initialize components
        initializeUI();
        
        // Create first tab
        createNewTab();
        
        setVisible(true);
    }
    
    private void initializeUI() {
        // Create file tree panel
        JPanel fileTreePanel = createFileTreePanel();
        
        // Create tabbed pane for multiple documents
        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(e -> updateStatusBar());
        
        // Enable FlatLaf close buttons on tabs
        tabbedPane.putClientProperty("JTabbedPane.tabClosable", true);
        
        // Add callback for tab close button
        tabbedPane.putClientProperty("JTabbedPane.tabCloseCallback", 
            (java.util.function.IntConsumer) this::closeTab);
        
        // Add right-click context menu for tabs
        addTabContextMenu();
        
        // Create split pane (file tree on left, editor on right)
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileTreePanel, tabbedPane);
        splitPane.setDividerLocation(250);
        splitPane.setOneTouchExpandable(true);
        add(splitPane, BorderLayout.CENTER);
        
        // Create status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusLabel = new JLabel("Lines: 0 | Characters: 0");
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);
        
        // Create menu bar
        createMenuBar();
        
        // Create toolbar
        createToolBar();
    }
    
    /**
     * Create file tree panel with directory browser
     */
    private JPanel createFileTreePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("File Explorer"));
        
        // Create root node
        rootNode = new DefaultMutableTreeNode("Select Directory...");
        fileTree = new JTree(rootNode);
        fileTree.setRootVisible(true);
        
        // Set custom cell renderer to display file names
        fileTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                    boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                
                if (value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    Object userObject = node.getUserObject();
                    
                    if (userObject instanceof File) {
                        File file = (File) userObject;
                        setText(file.getName());
                        
                        // Set icons
                        if (file.isDirectory()) {
                            setIcon(expanded ? getOpenIcon() : getClosedIcon());
                        } else {
                            setIcon(getLeafIcon());
                        }
                    }
                }
                
                return this;
            }
        });
        
        // Add mouse listener for double-click
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
                        fileTree.getLastSelectedPathComponent();
                    
                    if (node != null && node.getUserObject() instanceof File) {
                        File file = (File) node.getUserObject();
                        if (file.isFile()) {
                            openFileFromTree(file);
                        }
                    }
                }
            }
        });
        
        // Wrap tree in scroll pane
        JScrollPane treeScrollPane = new JScrollPane(fileTree);
        panel.add(treeScrollPane, BorderLayout.CENTER);
        
        // Add button to select root directory
        JButton selectDirButton = new JButton("Select Directory");
        selectDirButton.addActionListener(e -> selectRootDirectory());
        panel.add(selectDirButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Allow user to select a root directory for the file tree
     */
    private void selectRootDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        
        int result = chooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            loadDirectoryTree(selectedDir);
        }
    }
    
    /**
     * Load directory structure into tree
     */
    private void loadDirectoryTree(File directory) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        
        // Clear existing tree
        rootNode.removeAllChildren();
        rootNode.setUserObject(directory);
        
        // Load directory contents recursively
        loadDirectoryNodes(rootNode, directory);
        
        // Refresh tree
        ((DefaultTreeModel) fileTree.getModel()).reload();
        fileTree.expandRow(0);
    }
    
    /**
     * Recursively load directory nodes
     */
    private void loadDirectoryNodes(DefaultMutableTreeNode parentNode, File directory) {
        File[] files = directory.listFiles();
        
        if (files == null) {
            return;
        }
        
        // Sort: directories first, then files
        java.util.Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1;
            } else if (!f1.isDirectory() && f2.isDirectory()) {
                return 1;
            } else {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });
        
        for (File file : files) {
            // Skip hidden files
            if (file.isHidden()) {
                continue;
            }
            
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file);
            parentNode.add(childNode);
            
            // Recursively load subdirectories (limit depth to avoid performance issues)
            if (file.isDirectory() && parentNode.getLevel() < 5) {
                loadDirectoryNodes(childNode, file);
            }
        }
    }
    
    /**
     * Open file from tree double-click
     */
    private void openFileFromTree(File file) {
        try {
            // Check file type and open accordingly
            if (isImageFile(file.getName())) {
                openImageFile(file);
            } else if (isPdfFile(file.getName())) {
                openPdfFile(file);
            } else {
                openTextFile(file);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error opening file: " + e.getMessage(),
                "File Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem newItem = new JMenuItem("New");
        newItem.setAccelerator(KeyStroke.getKeyStroke("control N"));
        newItem.addActionListener(e -> createNewTab());
        
        JMenuItem openItem = new JMenuItem("Open");
        openItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
        openItem.addActionListener(e -> openFile());
        
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
        saveItem.addActionListener(e -> saveFile());
        
        JMenuItem saveEncryptedItem = new JMenuItem("Save Encrypted");
        saveEncryptedItem.setAccelerator(KeyStroke.getKeyStroke("control shift S"));
        saveEncryptedItem.addActionListener(e -> saveEncryptedFile());
        
        JMenuItem openEncryptedItem = new JMenuItem("Open Encrypted");
        openEncryptedItem.setAccelerator(KeyStroke.getKeyStroke("control shift O"));
        openEncryptedItem.addActionListener(e -> openEncryptedFile());
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke("alt F4"));
        exitItem.addActionListener(e -> System.exit(0));
        
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(openEncryptedItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveEncryptedItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        
        JMenuItem undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke("control Z"));
        undoItem.addActionListener(e -> performUndo());
        
        JMenuItem redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke("control Y"));
        redoItem.addActionListener(e -> performRedo());
        
        JMenuItem cutItem = new JMenuItem("Cut");
        cutItem.setAccelerator(KeyStroke.getKeyStroke("control X"));
        cutItem.addActionListener(e -> performCut());
        
        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.setAccelerator(KeyStroke.getKeyStroke("control C"));
        copyItem.addActionListener(e -> performCopy());
        
        JMenuItem pasteItem = new JMenuItem("Paste");
        pasteItem.setAccelerator(KeyStroke.getKeyStroke("control V"));
        pasteItem.addActionListener(e -> performPaste());
        
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        
        // View Menu
        JMenu viewMenu = new JMenu("View");
        
        darkModeMenuItem = new JCheckBoxMenuItem("Dark Mode");
        darkModeMenuItem.setAccelerator(KeyStroke.getKeyStroke("control D"));
        darkModeMenuItem.addActionListener(e -> toggleDarkMode());
        
        JMenuItem zoomInItem = new JMenuItem("Zoom In");
        zoomInItem.setAccelerator(KeyStroke.getKeyStroke("control PLUS"));
        
        JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
        zoomOutItem.setAccelerator(KeyStroke.getKeyStroke("control MINUS"));
        
        viewMenu.add(darkModeMenuItem);
        viewMenu.addSeparator();
        viewMenu.add(zoomInItem);
        viewMenu.add(zoomOutItem);
        
        // Tools Menu
        JMenu toolsMenu = new JMenu("Tools");
        
        JMenuItem findItem = new JMenuItem("Find");
        findItem.setAccelerator(KeyStroke.getKeyStroke("control F"));
        
        JMenuItem replaceItem = new JMenuItem("Replace");
        replaceItem.setAccelerator(KeyStroke.getKeyStroke("control H"));
        
        toolsMenu.add(findItem);
        toolsMenu.add(replaceItem);
        
        // Add all menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        
        setJMenuBar(menuBar);
    }
    
    /**
     * Create toolbar with common action buttons
     */
    private void createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        
        // New button with icon
        JButton newButton = new JButton(createSimpleIcon("📄"));
        newButton.setToolTipText("Create a new document (Ctrl+N)");
        newButton.addActionListener(e -> createNewTab());
        toolBar.add(newButton);
        
        toolBar.addSeparator();
        
        // Open button with icon
        JButton openButton = new JButton(createSimpleIcon("📂"));
        openButton.setToolTipText("Open a file (Ctrl+O)");
        openButton.addActionListener(e -> openFile());
        toolBar.add(openButton);
        
        // Save button with icon
        JButton saveButton = new JButton(createSimpleIcon("💾"));
        saveButton.setToolTipText("Save current file (Ctrl+S)");
        saveButton.addActionListener(e -> saveFile());
        toolBar.add(saveButton);
        
        toolBar.addSeparator();
        
        // Undo button with icon
        JButton undoButton = new JButton(createSimpleIcon("↶"));
        undoButton.setToolTipText("Undo last action (Ctrl+Z)");
        undoButton.addActionListener(e -> performUndo());
        toolBar.add(undoButton);
        
        // Redo button with icon
        JButton redoButton = new JButton(createSimpleIcon("↷"));
        redoButton.setToolTipText("Redo last action (Ctrl+Y)");
        redoButton.addActionListener(e -> performRedo());
        toolBar.add(redoButton);
        
        // Add toolbar to frame (below menu bar, above split pane)
        add(toolBar, BorderLayout.NORTH);
    }
    
    /**
     * Create a simple icon using emoji or text
     */
    private Icon createSimpleIcon(String text) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                                   RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getAscent();
                g2.drawString(text, x + (getIconWidth() - textWidth) / 2, 
                            y + (getIconHeight() + textHeight) / 2 - 2);
            }
            
            @Override
            public int getIconWidth() {
                return 24;
            }
            
            @Override
            public int getIconHeight() {
                return 24;
            }
        };
    }
    
    private void createNewTab() {
        // Create RSyntaxTextArea with syntax highlighting
        RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        
        // Set font to Consolas or Monospaced, 14pt (Notepad++ style)
        Font editorFont = new Font("Consolas", Font.PLAIN, 14);
        if (!editorFont.getFamily().equals("Consolas")) {
            // Fallback to Monospaced if Consolas not available
            editorFont = new Font("Monospaced", Font.PLAIN, 14);
        }
        textArea.setFont(editorFont);
        
        // Enable undo/redo (RSyntaxTextArea has built-in support)
        textArea.setEnabled(true);
        textArea.discardAllEdits(); // Clear undo history for new document
        
        // Track if document has been modified
        textArea.putClientProperty("modified", false);
        
        // Add document listener to update status bar and track modifications
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { 
                textArea.putClientProperty("modified", true);
                int index = tabbedPane.getSelectedIndex();
                if (index >= 0) {
                    updateTabTitle(index, true);
                }
                updateStatusBar(); 
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { 
                textArea.putClientProperty("modified", true);
                int index = tabbedPane.getSelectedIndex();
                if (index >= 0) {
                    updateTabTitle(index, true);
                }
                updateStatusBar(); 
            }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { 
                updateStatusBar(); 
            }
        });
        
        // Add caret listener to update cursor position in real-time
        textArea.addCaretListener(e -> updateStatusBar());
        
        // Wrap in RTextScrollPane (already correct)
        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);       // Show line numbers
        scrollPane.setFoldIndicatorEnabled(true);      // Enable code folding indicators
        
        // Apply theme (Monokai for dark mode, default for light mode)
        try {
            Theme theme;
            InputStream themeStream;
            
            if (isDarkMode) {
                // Load Monokai (dark) theme
                themeStream = getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml");
                
                if (themeStream == null) {
                    // Fallback to dark.xml
                    themeStream = getClass().getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/dark.xml");
                }
            } else {
                // Load default (light) theme
                themeStream = getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/default.xml");
            }
            
            if (themeStream != null) {
                theme = Theme.load(themeStream);
                theme.apply(textArea);
                themeStream.close();
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load theme for new tab: " + e.getMessage());
        }
        
        // Set gutter (line numbers strip) background color AFTER theme is applied
        // This ensures theme doesn't override our custom colors
        if (isDarkMode) {
            scrollPane.getGutter().setBackground(new Color(43, 43, 43));  // Dark gray background
            scrollPane.getGutter().setBorderColor(new Color(60, 60, 60));  // Slightly lighter border
        } else {
            scrollPane.getGutter().setBackground(new Color(240, 240, 240));  // Light gray background
            scrollPane.getGutter().setBorderColor(new Color(200, 200, 200));  // Gray border
        }
        
        // Add tab
        String tabTitle = "Untitled " + newFileCounter++;
        tabbedPane.addTab(tabTitle, scrollPane);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        
        // Update status bar
        updateStatusBar();
    }
    
    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Check file type and open accordingly
            if (isImageFile(selectedFile.getName())) {
                openImageFile(selectedFile);
            } else if (isPdfFile(selectedFile.getName())) {
                openPdfFile(selectedFile);
            } else {
                openTextFile(selectedFile);
            }
        }
    }
    
    /**
     * Check if file is an image based on extension
     */
    private boolean isImageFile(String filename) {
        String extension = "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = filename.substring(lastDot + 1).toLowerCase();
        }
        return extension.equals("jpg") || extension.equals("jpeg") || 
               extension.equals("png") || extension.equals("gif") || 
               extension.equals("bmp");
    }
    
    /**
     * Check if file is a PDF based on extension
     */
    private boolean isPdfFile(String filename) {
        String extension = "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = filename.substring(lastDot + 1).toLowerCase();
        }
        return extension.equals("pdf");
    }
    
    /**
     * Open and display an image file
     */
    private void openImageFile(File imageFile) {
        try {
            // Load image
            ImageIcon imageIcon = new ImageIcon(imageFile.getAbsolutePath());
            
            // Create JLabel to display image
            JLabel imageLabel = new JLabel(imageIcon);
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            
            // Wrap in JScrollPane
            JScrollPane scrollPane = new JScrollPane(imageLabel);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
            
            // Add new tab with image
            tabbedPane.addTab(imageFile.getName(), scrollPane);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
            tabbedPane.setToolTipTextAt(tabbedPane.getTabCount() - 1, imageFile.getAbsolutePath());
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error loading image: " + e.getMessage(),
                "Image Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Open and display a PDF file
     */
    private void openPdfFile(File pdfFile) {
        try {
            // Load PDF document
            PDDocument document = PDDocument.load(pdfFile);
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            
            // Create panel to hold all PDF pages
            JPanel pdfPanel = new JPanel();
            pdfPanel.setLayout(new BoxLayout(pdfPanel, BoxLayout.Y_AXIS));
            pdfPanel.setBackground(Color.GRAY);
            
            int pageCount = document.getNumberOfPages();
            
            // Render each page as an image
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                // Render page at 150 DPI for good quality
                BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, 150);
                
                // Create label with page image
                JLabel pageLabel = new JLabel(new ImageIcon(pageImage));
                pageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                pageLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                
                // Add some spacing between pages
                if (pageIndex > 0) {
                    pdfPanel.add(Box.createVerticalStrut(10));
                }
                
                pdfPanel.add(pageLabel);
            }
            
            // Close the document after rendering
            document.close();
            
            // Wrap in scroll pane
            JScrollPane scrollPane = new JScrollPane(pdfPanel);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
            
            // Add tab with PDF viewer
            String tabTitle = pdfFile.getName() + " (" + pageCount + " pages)";
            tabbedPane.addTab(tabTitle, scrollPane);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
            tabbedPane.setToolTipTextAt(tabbedPane.getTabCount() - 1, pdfFile.getAbsolutePath());
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error loading PDF: " + e.getMessage(),
                "PDF Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Open and display a text file
     */
    private void openTextFile(File selectedFile) {
        try {
            // Read file content with explicit UTF-8 encoding to handle Turkish characters correctly
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(selectedFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                // Remove last newline if file was not empty
                if (content.length() > 0 && content.charAt(content.length() - 1) == '\n') {
                    content.setLength(content.length() - 1);
                }
            }
            
            // Get current tab or create new one if none exists
            RSyntaxTextArea textArea = getCurrentTextArea();
            if (textArea == null) {
                createNewTab();
                textArea = getCurrentTextArea();
            }
            
            // Load content into text area
            textArea.setText(content.toString());
            textArea.setCaretPosition(0);
            
            // Set syntax highlighting based on file extension
            setSyntaxStyle(textArea, selectedFile.getName());
            
            // Update tab title with filename
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex >= 0) {
                tabbedPane.setTitleAt(selectedIndex, selectedFile.getName());
                tabbedPane.setToolTipTextAt(selectedIndex, selectedFile.getAbsolutePath());
            }
            
            // Store file path for saving
            textArea.putClientProperty("filePath", selectedFile.getAbsolutePath());
            textArea.putClientProperty("modified", false);  // Mark as not modified after loading
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Error reading file: " + e.getMessage(),
                "File Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void saveFile() {
        if (tabbedPane.getTabCount() == 0) {
            return;
        }
        
        RSyntaxTextArea textArea = getCurrentTextArea();
        if (textArea == null) {
            return;
        }
        
        // Check if file already has a path
        String filePath = (String) textArea.getClientProperty("filePath");
        
        if (filePath == null) {
            // No existing file, show save dialog
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            
            int result = fileChooser.showSaveDialog(this);
            
            if (result == JFileChooser.APPROVE_OPTION) {
                filePath = fileChooser.getSelectedFile().getAbsolutePath();
            } else {
                return; // User cancelled
            }
        }
        
        // Save file with UTF-8 encoding
        try {
            String content = textArea.getText();
            // Use UTF-8 encoding to properly save Turkish characters
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
                writer.write(content);
            }
            
            // Update tab title and store file path
            File savedFile = new File(filePath);
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex >= 0) {
                tabbedPane.setTitleAt(selectedIndex, savedFile.getName());
                tabbedPane.setToolTipTextAt(selectedIndex, filePath);
            }
            
            textArea.putClientProperty("filePath", filePath);
            textArea.putClientProperty("modified", false);  // Mark as saved
            
            // Remove asterisk from tab title
            if (selectedIndex >= 0) {
                updateTabTitle(selectedIndex, false);
            }
            
            JOptionPane.showMessageDialog(this,
                "File saved successfully: " + savedFile.getName(),
                "Save Successful",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Error saving file: " + e.getMessage(),
                "Save Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Save file with AES encryption
     */
    private void saveEncryptedFile() {
        if (tabbedPane.getTabCount() == 0) {
            return;
        }
        
        RSyntaxTextArea textArea = getCurrentTextArea();
        if (textArea == null) {
            return;
        }
        
        // Ask for password
        JPasswordField passwordField = new JPasswordField(20);
        JPasswordField confirmPasswordField = new JPasswordField(20);
        
        JPanel passwordPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        passwordPanel.add(new JLabel("Password:"));
        passwordPanel.add(passwordField);
        passwordPanel.add(new JLabel("Confirm:"));
        passwordPanel.add(confirmPasswordField);
        
        int result = JOptionPane.showConfirmDialog(this, passwordPanel,
            "Enter Password for Encryption", JOptionPane.OK_CANCEL_OPTION);
        
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be empty!",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match!",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Choose file to save
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setDialogTitle("Save Encrypted File");
        
        int fileResult = fileChooser.showSaveDialog(this);
        
        if (fileResult != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        
        try {
            String content = textArea.getText();
            byte[] encryptedData = encryptAES(content, password);
            
            // Save encrypted data
            Files.write(Paths.get(filePath), encryptedData);
            
            JOptionPane.showMessageDialog(this,
                "File encrypted and saved successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error encrypting file: " + e.getMessage(),
                "Encryption Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Open and decrypt an encrypted file
     */
    private void openEncryptedFile() {
        // Ask for password
        JPasswordField passwordField = new JPasswordField(20);
        
        JPanel passwordPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        passwordPanel.add(new JLabel("Password:"));
        passwordPanel.add(passwordField);
        
        int result = JOptionPane.showConfirmDialog(this, passwordPanel,
            "Enter Password to Decrypt", JOptionPane.OK_CANCEL_OPTION);
        
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        
        String password = new String(passwordField.getPassword());
        
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be empty!",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Choose file to open
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setDialogTitle("Open Encrypted File");
        
        int fileResult = fileChooser.showOpenDialog(this);
        
        if (fileResult != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        File selectedFile = fileChooser.getSelectedFile();
        
        try {
            // Read encrypted data
            byte[] encryptedData = Files.readAllBytes(Paths.get(selectedFile.getAbsolutePath()));
            
            // Decrypt
            String decryptedContent = decryptAES(encryptedData, password);
            
            // Create new tab or use current
            RSyntaxTextArea textArea = getCurrentTextArea();
            if (textArea == null) {
                createNewTab();
                textArea = getCurrentTextArea();
            }
            
            // Load decrypted content
            textArea.setText(decryptedContent);
            textArea.setCaretPosition(0);
            
            // Update tab title
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex >= 0) {
                tabbedPane.setTitleAt(selectedIndex, selectedFile.getName() + " (encrypted)");
                tabbedPane.setToolTipTextAt(selectedIndex, selectedFile.getAbsolutePath());
            }
            
            JOptionPane.showMessageDialog(this,
                "File decrypted successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error decrypting file: " + e.getMessage() + 
                "\nPlease check your password.",
                "Decryption Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Encrypt text using AES-256
     */
    private byte[] encryptAES(String plainText, String password) throws Exception {
        // Generate salt
        byte[] salt = "AdvancedEditor2026".getBytes();
        
        // Derive key from password
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
        
        // Create cipher
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        
        // Generate IV
        byte[] iv = new byte[16];
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes());
        System.arraycopy(hash, 0, iv, 0, 16);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        // Encrypt
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        
        // Return Base64 encoded
        return Base64.getEncoder().encode(encrypted);
    }
    
    /**
     * Decrypt data using AES-256
     */
    private String decryptAES(byte[] encryptedData, String password) throws Exception {
        // Decode Base64
        byte[] encrypted = Base64.getDecoder().decode(encryptedData);
        
        // Generate salt (same as encryption)
        byte[] salt = "AdvancedEditor2026".getBytes();
        
        // Derive key from password
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
        
        // Create cipher
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        
        // Generate IV (same as encryption)
        byte[] iv = new byte[16];
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes());
        System.arraycopy(hash, 0, iv, 0, 16);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        // Decrypt
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        
        return new String(decrypted, "UTF-8");
    }
    
    /**
     * Perform undo operation on current text area
     */
    private void performUndo() {
        RSyntaxTextArea textArea = getCurrentTextArea();
        if (textArea != null && textArea.canUndo()) {
            textArea.undoLastAction();
        }
    }
    
    /**
     * Perform redo operation on current text area
     */
    private void performRedo() {
        RSyntaxTextArea textArea = getCurrentTextArea();
        if (textArea != null && textArea.canRedo()) {
            textArea.redoLastAction();
        }
    }
    
    /**
     * Perform cut operation on current text area
     */
    private void performCut() {
        RSyntaxTextArea textArea = getCurrentTextArea();
        if (textArea != null) {
            textArea.cut();
        }
    }
    
    /**
     * Perform copy operation on current text area
     */
    private void performCopy() {
        RSyntaxTextArea textArea = getCurrentTextArea();
        if (textArea != null) {
            textArea.copy();
        }
    }
    
    /**
     * Perform paste operation on current text area
     */
    private void performPaste() {
        RSyntaxTextArea textArea = getCurrentTextArea();
        if (textArea != null) {
            textArea.paste();
        }
    }
    
    /**
     * Toggle between light and dark mode
     */
    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        
        try {
            // Switch application Look and Feel
            if (isDarkMode) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            
            // Update all components
            SwingUtilities.updateComponentTreeUI(this);
            
            // Apply RSyntaxTextArea theme to all open text editors
            applyEditorTheme();
            
            // Update checkbox state
            darkModeMenuItem.setSelected(isDarkMode);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error switching theme: " + e.getMessage(),
                "Theme Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Apply appropriate theme to all RSyntaxTextArea components
     */
    private void applyEditorTheme() {
        try {
            Theme theme;
            
            if (isDarkMode) {
                // Load dark theme (monokai) from RSyntaxTextArea resources
                InputStream is = getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml");
                
                if (is == null) {
                    // Fallback to dark.xml if monokai not found
                    is = getClass().getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/dark.xml");
                }
                
                if (is != null) {
                    theme = Theme.load(is);
                    is.close();
                } else {
                    System.err.println("Warning: Could not load dark theme for editor");
                    return;
                }
            } else {
                // Load default light theme
                InputStream is = getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/default.xml");
                
                if (is != null) {
                    theme = Theme.load(is);
                    is.close();
                } else {
                    System.err.println("Warning: Could not load light theme for editor");
                    return;
                }
            }
            
            // Apply theme to all open tabs with text editors
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                Component comp = tabbedPane.getComponentAt(i);
                
                if (comp instanceof RTextScrollPane) {
                    RTextScrollPane scrollPane = (RTextScrollPane) comp;
                    RSyntaxTextArea textArea = (RSyntaxTextArea) scrollPane.getTextArea();
                    theme.apply(textArea);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error applying editor theme: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Update status bar with current line and character count
     */
    /**
     * Add right-click context menu to tabbed pane
     */
    private void addTabContextMenu() {
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Middle mouse button (mouse wheel click) closes the tab
                if (e.getButton() == MouseEvent.BUTTON2) {
                    int clickedTab = tabbedPane.indexAtLocation(e.getX(), e.getY());
                    if (clickedTab >= 0) {
                        closeTab(clickedTab);
                    }
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showTabContextMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showTabContextMenu(e);
                }
            }
            
            private void showTabContextMenu(MouseEvent e) {
                // Find which tab was clicked
                int clickedTab = tabbedPane.indexAtLocation(e.getX(), e.getY());
                
                if (clickedTab >= 0) {
                    // Create popup menu
                    JPopupMenu popupMenu = new JPopupMenu();
                    
                    // Close menu item
                    JMenuItem closeItem = new JMenuItem("Close");
                    closeItem.addActionListener(evt -> closeTab(clickedTab));
                    popupMenu.add(closeItem);
                    
                    // Close All menu item
                    JMenuItem closeAllItem = new JMenuItem("Close All");
                    closeAllItem.addActionListener(evt -> closeAllTabs());
                    popupMenu.add(closeAllItem);
                    
                    // Close Others menu item
                    JMenuItem closeOthersItem = new JMenuItem("Close Others");
                    closeOthersItem.addActionListener(evt -> closeOtherTabs(clickedTab));
                    popupMenu.add(closeOthersItem);
                    
                    // Show popup menu
                    popupMenu.show(tabbedPane, e.getX(), e.getY());
                }
            }
        });
    }
    
    /**
     * Close all tabs
     */
    private void closeAllTabs() {
        // Close tabs from end to beginning to avoid index shifting issues
        while (tabbedPane.getTabCount() > 0) {
            closeTab(tabbedPane.getTabCount() - 1);
        }
    }
    
    /**
     * Update tab title to show modified state with asterisk
     */
    private void updateTabTitle(int index, boolean isModified) {
        if (index < 0 || index >= tabbedPane.getTabCount()) {
            return;
        }
        
        String currentTitle = tabbedPane.getTitleAt(index);
        
        if (isModified) {
            // Add asterisk if not already there
            if (!currentTitle.startsWith("*")) {
                tabbedPane.setTitleAt(index, "*" + currentTitle);
            }
        } else {
            // Remove asterisk if present
            if (currentTitle.startsWith("*")) {
                tabbedPane.setTitleAt(index, currentTitle.substring(1));
            }
        }
    }
    
    /**
     * Close all tabs except the specified one
     */
    private void closeOtherTabs(int keepIndex) {
        if (keepIndex < 0 || keepIndex >= tabbedPane.getTabCount()) {
            return;
        }
        
        // Close tabs after the kept tab (from end to beginning)
        for (int i = tabbedPane.getTabCount() - 1; i > keepIndex; i--) {
            closeTab(i);
        }
        
        // Close tabs before the kept tab (from end to beginning)
        // After closing tabs above, the kept tab may have shifted down
        while (tabbedPane.getTabCount() > 1 && tabbedPane.getTabCount() > 0) {
            if (tabbedPane.indexAtLocation(0, 0) != keepIndex) {
                closeTab(0);
            } else {
                break;
            }
        }
    }
    
    /**
     * Close tab at the given index, handling unsaved changes
     */
    private void closeTab(int index) {
        if (index < 0 || index >= tabbedPane.getTabCount()) {
            return;
        }
        
        // Get the component at the index
        Component comp = tabbedPane.getComponentAt(index);
        
        // Check if it's a text editor tab
        if (comp instanceof RTextScrollPane) {
            RTextScrollPane scrollPane = (RTextScrollPane) comp;
            RSyntaxTextArea textArea = (RSyntaxTextArea) scrollPane.getTextArea();
            
            // Check if document has been modified
            Boolean modified = (Boolean) textArea.getClientProperty("modified");
            if (modified != null && modified) {
                // Get filename for dialog
                String filename = tabbedPane.getTitleAt(index);
                
                // Show confirmation dialog
                int option = JOptionPane.showConfirmDialog(
                    this,
                    "Save changes to " + filename + "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (option == JOptionPane.YES_OPTION) {
                    // Save the file first
                    tabbedPane.setSelectedIndex(index);  // Switch to the tab to save it
                    saveFile();
                    
                    // Check if save was successful (modified flag should be false now)
                    Boolean stillModified = (Boolean) textArea.getClientProperty("modified");
                    if (stillModified != null && stillModified) {
                        // Save was cancelled or failed, don't close the tab
                        return;
                    }
                } else if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                    // User cancelled, don't close the tab
                    return;
                }
                // If NO_OPTION, just close without saving
            }
        }
        
        // Remove the tab
        tabbedPane.removeTabAt(index);
        updateStatusBar();
    }
    
    private void updateStatusBar() {
        RSyntaxTextArea textArea = getCurrentTextArea();
        
        if (textArea != null) {
            try {
                // Get cursor position
                int caretPosition = textArea.getCaretPosition();
                int line = textArea.getLineOfOffset(caretPosition);
                int column = caretPosition - textArea.getLineStartOffset(line);
                
                // Get character count
                int charCount = textArea.getText().length();
                
                // File encoding (default UTF-8)
                String encoding = "UTF-8";
                
                // Update status label with Line, Column, Characters, and Encoding
                statusLabel.setText(String.format(
                    "Line: %d, Column: %d | Characters: %d | Encoding: %s", 
                    line + 1, column + 1, charCount, encoding));
            } catch (Exception ex) {
                statusLabel.setText("Line: 1, Column: 1 | Characters: 0 | Encoding: UTF-8");
            }
        } else {
            statusLabel.setText("Line: 1, Column: 1 | Characters: 0 | Encoding: UTF-8");
        }
    }
    
    /**
     * Get the RSyntaxTextArea from the currently selected tab
     */
    private RSyntaxTextArea getCurrentTextArea() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex < 0) {
            return null;
        }
        
        Component component = tabbedPane.getComponentAt(selectedIndex);
        if (component instanceof RTextScrollPane) {
            RTextScrollPane scrollPane = (RTextScrollPane) component;
            return (RSyntaxTextArea) scrollPane.getTextArea();
        }
        
        return null;
    }
    
    /**
     * Set syntax highlighting style based on file extension
     */
    private void setSyntaxStyle(RSyntaxTextArea textArea, String filename) {
        String extension = "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = filename.substring(lastDot + 1).toLowerCase();
        }
        
        switch (extension) {
            case "java":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                break;
            case "py":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
                break;
            case "xml":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                break;
            case "html":
            case "htm":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
                break;
            case "css":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSS);
                break;
            case "js":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                break;
            case "json":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
                break;
            case "sql":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
                break;
            case "c":
            case "h":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
                break;
            case "cpp":
            case "cc":
            case "cxx":
            case "hpp":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
                break;
            case "cs":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSHARP);
                break;
            case "php":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PHP);
                break;
            case "rb":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY);
                break;
            case "sh":
            case "bash":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
                break;
            case "md":
            case "markdown":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
                break;
            default:
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                break;
        }
    }
    
    public static void main(String[] args) {
        // Set FlatLightLaf look and feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf theme");
            e.printStackTrace();
        }
        
        // Create and show the editor
        SwingUtilities.invokeLater(() -> new AdvancedEditor());
    }
}
