package dev.qf.client;

import common.*;
import common.Menu;
import common.registry.RegistryManager;
import dev.qf.client.event.DataReceivedEvent;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class MenuManagementUI extends JFrame {
    private JTable menuTable;
    private MenuTableModel tableModel;
    private JComboBox<String> categoryFilter;

    private JLabel selectedMenuImage;
    private JLabel selectedMenuName;
    private JLabel selectedMenuPrice;
    private JLabel selectedMenuCategory;
    private JLabel selectedMenuStatus;
    private JTextArea selectedMenuDescription;

    private JButton categoryManagementButton;
    private JButton addMenuButton;
    private JButton deleteMenuButton;
    private JButton editMenuButton;
    private JButton toggleSoldOutButton;

    private final MenuService menuService;
    private Menu selectedMenu;

    // 서버 업데이트 디바운싱을 위한 Timer
    private Timer updateTimer;

    public MenuManagementUI() {
        this.menuService = MenuService.getInstance();

        initComponents();
        initEventHandlers();

        // DataReceivedEvent로 자동 업데이트
        DataReceivedEvent.EVENT.register((handler, registry) -> {
            this.scheduleUpdate();
        });

        refreshMenuList();
        refreshCategoryComboBox();
    }

    private void scheduleUpdate() {
        if (updateTimer != null) {
            updateTimer.stop();
        }

        updateTimer = new Timer(150, e -> {
            onServerUpdate();
            refreshCategoryComboBox();
            refreshMenuList();

            if (selectedMenu != null) {
                selectMenuInTable(selectedMenu.id());
            }
            System.out.println("Categories after update: " + RegistryManager.CATEGORIES.getAll().size());
            System.out.println("Menus after update: " + RegistryManager.MENUS.getAll().size());
        });
        updateTimer.setRepeats(false);
        updateTimer.start();
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

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        selectedMenuImage = new JLabel("이미지 없음");
        selectedMenuImage.setPreferredSize(new Dimension(150, 150));
        selectedMenuImage.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        selectedMenuImage.setHorizontalAlignment(SwingConstants.CENTER);
        detailPanel.add(selectedMenuImage, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        detailPanel.add(new JLabel("이름:"), gbc);
        gbc.gridx = 1;
        selectedMenuName = new JLabel("-");
        detailPanel.add(selectedMenuName, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        detailPanel.add(new JLabel("가격:"), gbc);
        gbc.gridx = 1;
        selectedMenuPrice = new JLabel("-");
        detailPanel.add(selectedMenuPrice, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        detailPanel.add(new JLabel("카테고리:"), gbc);
        gbc.gridx = 1;
        selectedMenuCategory = new JLabel("-");
        detailPanel.add(selectedMenuCategory, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        detailPanel.add(new JLabel("설명:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        selectedMenuDescription = new JTextArea(3, 20);
        selectedMenuDescription.setEditable(false);
        selectedMenuDescription.setLineWrap(true);
        selectedMenuDescription.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(selectedMenuDescription);
        detailPanel.add(descScrollPane, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0; gbc.weighty = 0;
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
    }

    public void refreshCategoryComboBox() {
        System.out.println("=== refreshCategoryComboBox called ===");
        String selectedItem = (String) categoryFilter.getSelectedItem();

        categoryFilter.removeAllItems();
        categoryFilter.addItem("전체");

        Set<String> addedCategories = new HashSet<>();
        addedCategories.add("전체");

        for (Category category : RegistryManager.CATEGORIES.getAll()) {
            if (!addedCategories.contains(category.cateName())) {
                categoryFilter.addItem(category.cateName());
                addedCategories.add(category.cateName());
                System.out.println("Added category: " + category.cateName());
            }
        }

        if (selectedItem != null && addedCategories.contains(selectedItem)) {
            categoryFilter.setSelectedItem(selectedItem);
        }

        System.out.println("Total categories in combobox: " + categoryFilter.getItemCount());
    }

    public void refreshMenuList() {
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        updateMenuItemsView(selectedCategory != null ? selectedCategory : "전체");
        clearMenuDetails();
    }

    public List<Menu> getAllMenusForDisplay() {
        return new ArrayList<>(RegistryManager.MENUS.getAll());
    }

    public List<Menu> getMenusByCategoryForDisplay(String categoryName) {
        List<Menu> allMenus = RegistryManager.MENUS.getAll();

        return allMenus.stream()
                .filter(menu -> {
                    // 이 메뉴가 해당 카테고리에 속하는지 확인
                    return RegistryManager.CATEGORIES.getAll().stream()
                            .anyMatch(category ->
                                    category.cateName().equals(categoryName) &&
                                            category.menus().stream().anyMatch(catMenu -> catMenu.id().equals(menu.id()))
                            );
                })
                .collect(Collectors.toList());
    }

    private void handleCategorySelection() {
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        updateMenuItemsView(selectedCategory);
    }

    private void updateMenuItemsView(String categoryName) {
        if ("전체".equals(categoryName)) {
            tableModel.setFilteredMenus(getAllMenusForDisplay());
        } else {
            tableModel.setFilteredMenus(getMenusByCategoryForDisplay(categoryName));
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
        selectedMenuPrice.setText(String.format("%d원", menu.price()));
        selectedMenuCategory.setText(getCategoryNameForMenu(menu));
        selectedMenuDescription.setText(menu.description() != null ? menu.description() : "");
        selectedMenuStatus.setText(menu.soldOut() ? "품절" : "판매중");

        loadAndDisplayImage(menu.imagePath().toString());
    }
    private String getCategoryNameForMenu(Menu menu) {
        return menu.id();
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
        AddMenuUI dialog = new AddMenuUI(this);
        dialog.setVisible(true);
    }

    private void handleDeleteMenu() {
        if (selectedMenu == null) {
            JOptionPane.showMessageDialog(this, "삭제할 메뉴를 먼저 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "정말로 '" + selectedMenu.name() + "' 메뉴를 삭제하시겠습니까?",
                "삭제 확인",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            String menuId = selectedMenu.id();
            String menuName = selectedMenu.name();

            System.out.println("Menu deletion requested, UI will completely remove the menu: " + menuName);
            System.out.println("Menu deletion requested: " + menuId);

            boolean success = menuService.deleteMenu(menuId);

            if (success) {
                SwingUtilities.invokeLater(() -> {
                    selectedMenu = null;
                    if (menuTable != null) {
                        menuTable.clearSelection();
                    }
                    clearMenuDetails();
                    refreshMenuList();
                    refreshCategoryComboBox();
                });

                JOptionPane.showMessageDialog(this,
                        "메뉴 '" + menuName + "'이(가) 삭제되었습니다.",
                        "삭제 완료",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "메뉴 삭제에 실패했습니다.",
                        "오류",
                        JOptionPane.ERROR_MESSAGE);
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

        String menuId = selectedMenu.id();
        String menuName = selectedMenu.name();
        boolean currentSoldOut = selectedMenu.soldOut();
        String toggleAction = currentSoldOut ? "판매중" : "품절";

        boolean success = menuService.toggleSoldOut(menuId);
        if (success) {
            JOptionPane.showMessageDialog(this,
                    "메뉴 '" + menuName + "'의 상태가 '" + toggleAction + "'으로 변경 요청되었습니다.");

            Timer refreshTimer = new Timer(200, e -> {
                SwingUtilities.invokeLater(() -> {
                    refreshMenuList();
                    selectMenuInTable(menuId);
                    updateSelectedMenuDetails(menuId);
                });
            });
            refreshTimer.setRepeats(false);
            refreshTimer.start();

        } else {
            JOptionPane.showMessageDialog(this, "상태 변경에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectMenuInTable(String menuId) {
        if (menuId == null || tableModel == null) return;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Menu menu = tableModel.getMenuAt(i);
            if (menu != null && menu.id().equals(menuId)) {
                menuTable.setRowSelectionInterval(i, i);
                menuTable.scrollRectToVisible(menuTable.getCellRect(i, 0, true));
                break;
            }
        }
    }

    private void updateSelectedMenuDetails(String menuId) {
        RegistryManager.MENUS.getById(menuId).ifPresentOrElse(
                updatedMenu -> {
                    selectedMenu = updatedMenu;
                    showMenuDetails(updatedMenu);
                    System.out.println("Menu details updated: " + updatedMenu.name() +
                            " - SoldOut: " + updatedMenu.soldOut());
                },
                () -> {
                    System.out.println("Menu not found in registry: " + menuId);
                    clearMenuDetails();
                }
        );
    }

    private void handleCategoryManagement() {
        dev.qf.client.CategoryManagementUI categoryUI = new dev.qf.client.CategoryManagementUI();
        categoryUI.setVisible(true);
    }

    public void onServerUpdate() {
        System.out.println("=== Server update received ===");

        if (selectedMenu != null) {
            String selectedMenuId = selectedMenu.id();
            RegistryManager.MENUS.getById(selectedMenuId).ifPresentOrElse(
                    updatedMenu -> {
                        if (!updatedMenu.equals(selectedMenu)) {
                            System.out.println("Selected menu updated: " + updatedMenu.name() +
                                    " - SoldOut changed: " + selectedMenu.soldOut() +
                                    " -> " + updatedMenu.soldOut());
                            selectedMenu = updatedMenu;
                            SwingUtilities.invokeLater(() -> showMenuDetails(updatedMenu));
                        }
                    },
                    () -> {
                        System.out.println("Selected menu was deleted: " + selectedMenuId);
                        selectedMenu = null;
                        SwingUtilities.invokeLater(() -> clearMenuDetails());
                    }
            );
        }

        System.out.println("=== Server update processing completed ===");
    }

    private class MenuTableModel extends AbstractTableModel {
        private final String[] columnNames = {"ID", "메뉴명", "카테고리", "가격", "상태"};
        private List<Menu> filteredMenus = new ArrayList<>();

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
                case 0 -> menu.id();
                case 1 -> menu.name();
                case 2 -> getCategoryNameForMenu(menu);
                case 3 -> String.format("%d원", menu.price());
                case 4 -> menu.soldOut() ? "품절" : "판매중";
                default -> null;
            };
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MenuManagementUI().setVisible(true));
    }
}
