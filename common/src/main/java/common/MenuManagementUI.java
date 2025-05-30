package common;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class MenuManagementUI extends JFrame {
    private JTable menuTable;
    private MenuTableModel tableModel;
    private JComboBox<String> categoryFilter;

    private JLabel selectedMenuImage;
    private JLabel selectedMenuName;
    private JLabel selectedMenuPrice;
    private JLabel selectedMenuCategory;
    private JTextArea selectedMenuDescription;
    private JLabel selectedMenuStatus;

    private JButton categoryManagementButton;
    private JButton addMenuButton;
    private JButton deleteMenuButton;
    private JButton editMenuButton;
    private JButton toggleSoldOutButton;

    private final MenuService menuService;
    private final CategoryService categoryService;
    private Menu selectedMenu;

    public MenuManagementUI() {
        this.categoryService = CategoryService.getInstance();
        this.menuService = new MenuService(categoryService.getCategoryList());

        initComponents();
        initEventHandlers();

        categoryService.addChangeListener(() -> {
            SwingUtilities.invokeLater(() -> {
                refreshCategoryComboBox();
                refreshMenuList();
            });
        });

        refreshMenuList();
        refreshCategoryComboBox();
    }

    private void initComponents() {
        setTitle("메뉴 관리");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel filterPanel = createFilterPanel();
        add(filterPanel, BorderLayout.NORTH);

        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        JPanel leftPanel = createMenuTablePanel();
        centerSplitPane.setLeftComponent(leftPanel);

        JPanel rightPanel = createMenuDetailPanel();
        centerSplitPane.setRightComponent(rightPanel);

        centerSplitPane.setDividerLocation(400);
        add(centerSplitPane, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("카테고리 필터:"));

        categoryFilter = new JComboBox<>();
        categoryFilter.addItem("전체");
        panel.add(categoryFilter);

        return panel;
    }

    private JPanel createMenuTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("메뉴 목록"));

        tableModel = new MenuTableModel();
        menuTable = new JTable(tableModel);
        menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(menuTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMenuDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("메뉴 상세 정보"));

        JPanel detailPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;gbc.gridy = 0;gbc.gridwidth = 2;
        selectedMenuImage = new JLabel("이미지 없음");
        selectedMenuImage.setPreferredSize(new Dimension(150, 150));
        selectedMenuImage.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        selectedMenuImage.setHorizontalAlignment(SwingConstants.CENTER);
        detailPanel.add(selectedMenuImage, gbc);

        gbc.gridx = 0;gbc.gridy = 1;gbc.gridwidth = 1;
        detailPanel.add(new JLabel("이름:"), gbc);
        gbc.gridx = 1;
        selectedMenuName = new JLabel("-");
        detailPanel.add(selectedMenuName, gbc);

        gbc.gridx = 0;gbc.gridy = 2;
        detailPanel.add(new JLabel("가격:"), gbc);
        gbc.gridx = 1;
        selectedMenuPrice = new JLabel("-");
        detailPanel.add(selectedMenuPrice, gbc);

        gbc.gridx = 0;gbc.gridy = 3;
        detailPanel.add(new JLabel("카테고리:"), gbc);
        gbc.gridx = 1;
        selectedMenuCategory = new JLabel("-");
        detailPanel.add(selectedMenuCategory, gbc);

        gbc.gridx = 0;gbc.gridy = 4;
        detailPanel.add(new JLabel("설명:"), gbc);
        gbc.gridx = 1;gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;gbc.weighty = 1.0;
        selectedMenuDescription = new JTextArea(3, 20);
        selectedMenuDescription.setEditable(false);
        selectedMenuDescription.setLineWrap(true);
        selectedMenuDescription.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(selectedMenuDescription);
        detailPanel.add(descScrollPane, gbc);

        gbc.gridx = 0;gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;gbc.weighty = 0;
        detailPanel.add(new JLabel("상태:"), gbc);
        gbc.gridx = 1;
        selectedMenuStatus = new JLabel("-");
        detailPanel.add(selectedMenuStatus, gbc);

        panel.add(detailPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        categoryManagementButton = new JButton("카테고리 관리");
        addMenuButton = new JButton("메뉴 추가");
        deleteMenuButton = new JButton("메뉴 삭제");
        editMenuButton = new JButton("메뉴 수정");
        toggleSoldOutButton = new JButton("품절 상태 변경");

        panel.add(categoryManagementButton);
        panel.add(addMenuButton);
        panel.add(editMenuButton);
        panel.add(deleteMenuButton);
        panel.add(toggleSoldOutButton);

        return panel;
    }

    private void initEventHandlers() {
        menuTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleMenuSelection();
            }
        });

        categoryFilter.addActionListener(e -> handleCategorySelection());

        categoryManagementButton.addActionListener(e -> handleCategoryManagement());
        addMenuButton.addActionListener(e -> handleAddMenu());
        deleteMenuButton.addActionListener(e -> handleDeleteMenu());
        editMenuButton.addActionListener(e -> handleEditMenu());
        toggleSoldOutButton.addActionListener(e -> handleToggleSoldOut());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
            }
        });
    }

    private void refreshCategoryComboBox() {
        String selectedItem = (String) categoryFilter.getSelectedItem();
        categoryFilter.removeAllItems();
        categoryFilter.addItem("전체");

        for (Category category : categoryService.getAllCategories()) {
            categoryFilter.addItem(category.cateName());
        }

        if (selectedItem != null) {
            categoryFilter.setSelectedItem(selectedItem);
        }
    }

    private void refreshMenuList() {
        tableModel.refreshData();
        clearMenuDetails();
    }

    private void handleCategorySelection() {
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        updateMenuItemsView(selectedCategory);
    }

    private void updateMenuItemsView(String categoryName) {
        if ("전체".equals(categoryName)) {
            tableModel.setFilteredMenus(menuService.getAllMenus());
        } else {
            String categoryId = categoryService.getAllCategories().stream()
                    .filter(c -> c.cateName().equals(categoryName))
                    .map(Category::cateId)
                    .findFirst()
                    .orElse("");

            tableModel.setFilteredMenus(menuService.getMenusByCategory(categoryId));
        }
    }

    private void handleMenuSelection() {
        int selectedRow = menuTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
            selectedMenu = tableModel.getMenuAt(selectedRow);
            showMenuDetails(selectedMenu);
        } else {
            selectedMenu = null;
            clearMenuDetails();
        }
    }

    private void showMenuDetails(Menu menu) {
        if (menu == null) {
            clearMenuDetails();
            return;
        }

        selectedMenuName.setText(menu.name());
        selectedMenuPrice.setText(String.format("%.0f원", menu.price()));
        selectedMenuCategory.setText(menuService.getCategoryName(menu.categoryId()));
        selectedMenuDescription.setText(menu.description() != null ? menu.description() : "");
        selectedMenuStatus.setText(menu.soldOut() ? "품절" : "판매중");

        loadAndDisplayImage(menu.imagePath());
    }

    private void loadAndDisplayImage(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            selectedMenuImage.setIcon(null);
            selectedMenuImage.setText("이미지 없음");
            return;
        }

        try {
            java.io.File imageFile = new java.io.File(imagePath);
            if (!imageFile.exists()) {
                selectedMenuImage.setIcon(null);
                selectedMenuImage.setText("이미지 파일을 찾을 수 없음");
                return;
            }

            ImageIcon originalIcon = new ImageIcon(imagePath);

            Image originalImage = originalIcon.getImage();
            Image scaledImage = originalImage.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);

            selectedMenuImage.setIcon(scaledIcon);
            selectedMenuImage.setText("");
        } catch (Exception e) {
            selectedMenuImage.setIcon(null);
            selectedMenuImage.setText("이미지 로딩 오류");
            System.err.println("이미지 로딩 실패: " + imagePath + " - " + e.getMessage());
        }
    }

    private void clearMenuDetails() {
        selectedMenuName.setText("-");
        selectedMenuPrice.setText("-");
        selectedMenuCategory.setText("-");
        selectedMenuDescription.setText("");
        selectedMenuStatus.setText("-");
        selectedMenuImage.setIcon(null);
        selectedMenuImage.setText("이미지 없음");
        selectedMenu = null;
    }

    private void handleAddMenu() {
        AddMenuUI dialog = new AddMenuUI(this, menuService);
        dialog.setVisible(true);
        refreshMenuList();
        refreshCategoryComboBox();
    }

    private void handleDeleteMenu() {
        if (selectedMenu == null) {
            JOptionPane.showMessageDialog(this, "삭제할 메뉴를 먼저 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "'" + selectedMenu.name() + "' 메뉴를 삭제하시겠습니까?",
                "메뉴 삭제",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            boolean success = menuService.deleteMenu(selectedMenu.menuId());
            if (success) {
                selectedMenu = null;
                menuTable.clearSelection();
                clearMenuDetails();
                refreshMenuList();
                JOptionPane.showMessageDialog(this, "메뉴가 삭제되었습니다.");
            } else {
                JOptionPane.showMessageDialog(this, "메뉴 삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleEditMenu() {
        if (selectedMenu == null) {
            JOptionPane.showMessageDialog(this, "수정할 메뉴를 먼저 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "메뉴 수정 기능은 아직 구현되지 않았습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleToggleSoldOut() {
        if (selectedMenu == null) {
            JOptionPane.showMessageDialog(this, "상태를 변경할 메뉴를 먼저 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int menuId = selectedMenu.menuId();
        boolean success = menuService.toggleSoldOut(menuId);
        if (success) {
            selectedMenu = menuService.getMenuById(menuId).orElse(null);
            if (selectedMenu != null) {
                String status = selectedMenu.soldOut() ? "품절" : "판매중";
                JOptionPane.showMessageDialog(this, "메뉴 상태가 '" + status + "'으로 변경되었습니다.");
                showMenuDetails(selectedMenu);
                refreshMenuList();
            } else {
                clearMenuDetails();
                refreshMenuList();
            }
        } else {
            JOptionPane.showMessageDialog(this, "상태 변경에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCategoryManagement() {
        CategoryManagementUI categoryUI = new CategoryManagementUI(menuService);
        categoryUI.setVisible(true);
    }

    private class MenuTableModel extends AbstractTableModel {
        private final String[] columnNames = {"ID", "메뉴명", "카테고리", "가격", "상태"};
        private List<Menu> filteredMenus = new ArrayList<>();

        public void refreshData() {
            filteredMenus = new ArrayList<>(menuService.getAllMenus());
            fireTableDataChanged();
        }

        public void setFilteredMenus(List<Menu> menus) {
            this.filteredMenus = new ArrayList<>(menus);
            fireTableDataChanged();
        }

        public Menu getMenuAt(int row) {
            if (row >= 0 && row < filteredMenus.size()) {
                return filteredMenus.get(row);
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return filteredMenus.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= filteredMenus.size()) return null;

            Menu menu = filteredMenus.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> menu.menuId();
                case 1 -> menu.name();
                case 2 -> menuService.getCategoryName(menu.categoryId());
                case 3 -> String.format("%.0f원", menu.price());
                case 4 -> menu.soldOut() ? "품절" : "판매중";
                default -> null;
            };
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MenuManagementUI().setVisible(true));
    }
}