package common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;

public class CategoryManagementUI extends JFrame {
    private final CategoryService categoryService = CategoryService.getInstance();
    private MenuService menuService;

    private JPanel categoriesPanel;
    private JButton addCategoryButton;
    private JButton saveCategoryButton;
    private JButton closeButton;

    public CategoryManagementUI() {
        this.menuService = new MenuService(categoryService.getCategoryList());
        initComponents();
        initEventHandlers();
        refreshCategories();
    }

    public CategoryManagementUI(MenuService menuService) {
        this.menuService = menuService;
        initComponents();
        initEventHandlers();
        refreshCategories();
    }

    private void initComponents() {
        categoriesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JScrollPane scrollPane = new JScrollPane(categoriesPanel);

        addCategoryButton = new JButton("카테고리 추가");
        saveCategoryButton = new JButton("저장");
        closeButton = new JButton("닫기");

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.add(addCategoryButton);
        bottomPanel.add(saveCategoryButton);
        bottomPanel.add(closeButton);

        setTitle("카테고리 관리");
        setLayout(new BorderLayout(10, 10));
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void initEventHandlers() {
        addCategoryButton.addActionListener(e -> {
            categoryManagementButton();
            refreshCategories();
        });
        saveCategoryButton.addActionListener(e -> categorySaveButton());
        closeButton.addActionListener(e -> categoryClose());
    }

    public void categoryManagementButton() {
        showAddCategoryDialog();
    }

    private void showAddCategoryDialog() {
        JDialog dialog = new JDialog(this, "카테고리 추가", true);

        JTextField cateIdField = new JTextField(15);
        JTextField cateNameField = new JTextField(15);
        JButton addButton = new JButton("추가");
        JButton cancelButton = new JButton("취소");

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.add(new JLabel("카테고리 ID:"));
        inputPanel.add(cateIdField);
        inputPanel.add(new JLabel("카테고리 이름:"));
        inputPanel.add(cateNameField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);

        dialog.setLayout(new BorderLayout(10, 10));
        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> {
            String id = cateIdField.getText().trim();
            String name = cateNameField.getText().trim();

            boolean success = categoryService.addCategory(id, name);
            if (success) {
                JOptionPane.showMessageDialog(dialog, name + " 카테고리가 추가되었습니다.");
                dialog.dispose();
                refreshCategories();
            } else {
                JOptionPane.showMessageDialog(dialog, "카테고리 추가에 실패했습니다.\nID와 이름을 확인해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void refreshCategories() {
        categoriesPanel.removeAll();
        for (Category category : categoryService.getAllCategories()) {
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            JButton btnCat = new JButton(category.cateName());
            JButton btnDel = new JButton("X");
            btnDel.setMargin(new Insets(1, 1, 1, 1));

            btnDel.addActionListener(ev -> {
                int menuCount = menuService.getMenusByCategory(category.cateId()).size();

                String message;
                if (menuCount > 0) {
                    message = String.format("'%s' 카테고리를 삭제하시겠습니까?\n이 카테고리에 속한 %d개의 메뉴도 함께 삭제됩니다.",
                            category.cateName(), menuCount);
                } else {
                    message = String.format("'%s' 카테고리를 삭제하시겠습니까?", category.cateName());
                }

                int result = JOptionPane.showConfirmDialog(this,
                        message,
                        "카테고리 삭제",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (result == JOptionPane.YES_OPTION) {
                    int deletedMenus = menuService.deleteMenusByCategory(category.cateId());
                    boolean success = categoryService.deleteCategory(category.cateId());

                    if (success) {
                        if (menuCount > 0) {
                            JOptionPane.showMessageDialog(this,
                                    String.format("카테고리와 관련 메뉴 %d개가 모두 삭제되었습니다.", menuCount));
                        } else {
                            JOptionPane.showMessageDialog(this, "카테고리가 삭제되었습니다.");
                        }
                        refreshCategories();
                    } else {
                        JOptionPane.showMessageDialog(this, "삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            item.add(btnCat);
            item.add(btnDel);
            categoriesPanel.add(item);
        }
        categoriesPanel.revalidate();
        categoriesPanel.repaint();
    }

    public void categorySaveButton() {
        JOptionPane.showMessageDialog(this, "저장되었습니다.");
    }

    public void categoryClose() {
        dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CategoryManagementUI().setVisible(true));
    }
}
