package dev.qf.client;

import common.Category;
import common.MenuService;
import common.registry.RegistryManager;

import javax.swing.*;
import java.awt.*;
import java.awt.FileDialog;

public class AddMenuUI extends JDialog {
    private JTextField nameField;
    private JTextField priceField;
    private JTextField imagePathField;
    private JTextArea descriptionArea;
    private JLabel imagePreview;
    private JComboBox<String> categoryComboBox;
    private JCheckBox soldOutCheckBox;
    private JButton browseButton;
    private JButton addButton;
    private JButton cancelButton;

    private final MenuService menuService;

    public AddMenuUI(Frame owner) {
        super(owner, "메뉴 추가", true);
        this.menuService = MenuService.getInstance();
        initComponents();
        initEventHandlers();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        nameField = new JTextField(15);
        priceField = new JTextField(15);
        categoryComboBox = new JComboBox<>();
        descriptionArea = new JTextArea(3, 15);
        imagePathField = new JTextField(20);
        browseButton = new JButton("찾아보기");
        imagePreview = new JLabel("이미지 미리보기");
        imagePreview.setPreferredSize(new Dimension(200, 200));
        imagePreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        soldOutCheckBox = new JCheckBox("품절");
        addButton = new JButton("추가");
        cancelButton = new JButton("취소");

        initCategoryComboBox();

        JPanel mainPanel = new JPanel(new BorderLayout(20, 10));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("이미지 미리보기"));
        leftPanel.add(imagePreview, BorderLayout.CENTER);

        JPanel imagePathPanel = new JPanel(new BorderLayout(5, 0));
        imagePathPanel.add(imagePathField, BorderLayout.CENTER);
        imagePathPanel.add(browseButton, BorderLayout.EAST);
        leftPanel.add(imagePathPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("메뉴 정보"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        rightPanel.add(new JLabel("메뉴 이름:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        rightPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        rightPanel.add(new JLabel("가격:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        rightPanel.add(priceField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        rightPanel.add(new JLabel("카테고리:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        rightPanel.add(categoryComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        rightPanel.add(new JLabel("설명:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        rightPanel.add(descScrollPane, gbc);

        gbc.gridx = 1; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0;
        rightPanel.add(soldOutCheckBox, gbc);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);

        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void initCategoryComboBox() {
        categoryComboBox.removeAllItems();
        for (Category category : RegistryManager.CATEGORIES.getAll()) {
            categoryComboBox.addItem(category.cateId() + " - " + category.cateName());
        }
    }

    private void initEventHandlers() {
        addButton.addActionListener(e -> onAdd());
        cancelButton.addActionListener(e -> onCancel());
        browseButton.addActionListener(e -> onBrowseImage());

        imagePathField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
        });
    }

    private void onAdd() {
        try {
            String name = nameField.getText().trim();
            String priceText = priceField.getText().trim();
            String description = descriptionArea.getText().trim();
            String imagePath = imagePathField.getText().trim();
            boolean soldOut = soldOutCheckBox.isSelected();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "메뉴 이름을 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean nameExists = RegistryManager.MENUS.getAll().stream()
                    .anyMatch(menu -> menu.name().equals(name));
            if (nameExists) {
                JOptionPane.showMessageDialog(this, "이미 같은 이름의 메뉴가 존재합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int price;
            try {
                price = Integer.parseInt(priceText);
                if (price < 0) {
                    JOptionPane.showMessageDialog(this, "가격은 0 이상이어야 합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "올바른 가격을 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (categoryComboBox.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "카테고리를 선택해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String selectedCategory = categoryComboBox.getSelectedItem().toString();
            String categoryId = selectedCategory.split(" - ")[0];

            boolean success = menuService.registerMenu(name, price, categoryId, description, imagePath, soldOut);
            if (success) {
                JOptionPane.showMessageDialog(this, "메뉴가 추가되었습니다.");

                // 메뉴 추가 성공시 = 새로고침
                if (getOwner() instanceof MenuManagementUI) {
                    SwingUtilities.invokeLater(() -> {
                        ((MenuManagementUI) getOwner()).refreshMenuList();
                        ((MenuManagementUI) getOwner()).refreshCategoryComboBox();
                    });
                }

                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "메뉴 추가에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "오류가 발생했습니다: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        dispose();
    }

    private void onBrowseImage() {
        FileDialog fileDialog = new FileDialog(this, "이미지 파일 선택", FileDialog.LOAD);

        fileDialog.setFilenameFilter((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") ||
                    lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") ||
                    lowerName.endsWith(".gif") ||
                    lowerName.endsWith(".bmp");
        });

        String currentPath = imagePathField.getText().trim();
        if (!currentPath.isEmpty()) {
            java.io.File currentFile = new java.io.File(currentPath);
            if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                fileDialog.setDirectory(currentFile.getParentFile().getAbsolutePath());
            }
        } else {
            String userHome = System.getProperty("user.home");
            java.io.File picturesDir = new java.io.File(userHome, "Pictures");
            if (picturesDir.exists()) {
                fileDialog.setDirectory(picturesDir.getAbsolutePath());
            }
        }

        fileDialog.setVisible(true);

        String fileName = fileDialog.getFile();
        String directory = fileDialog.getDirectory();

        if (fileName != null && directory != null) {
            java.io.File selectedFile = new java.io.File(directory, fileName);
            imagePathField.setText(selectedFile.getAbsolutePath());
            updatePreview();
        }
    }

    private void updatePreview() {
        String imagePath = imagePathField.getText().trim();

        if (imagePath.isEmpty()) {
            imagePreview.setIcon(null);
            imagePreview.setText("이미지 미리보기");
            return;
        }

        try {
            java.io.File imageFile = new java.io.File(imagePath);
            if (!imageFile.exists()) {
                imagePreview.setIcon(null);
                imagePreview.setText("파일을 찾을 수 없음");
                return;
            }

            ImageIcon originalIcon = new ImageIcon(imagePath);
            Image originalImage = originalIcon.getImage();
            Image scaledImage = originalImage.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);

            imagePreview.setIcon(scaledIcon);
            imagePreview.setText("");

        } catch (Exception e) {
            imagePreview.setIcon(null);
            imagePreview.setText("이미지 로딩 오류");
        }
    }
}
