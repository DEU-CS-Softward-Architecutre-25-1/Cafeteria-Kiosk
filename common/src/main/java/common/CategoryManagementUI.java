package common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;

public class CategoryManagementUI extends JFrame {
    // 카테고리 리스트와 컨트롤러
    private final List<Category> categoryList = new ArrayList<>();
    private final AddCategory addCategoryController = new AddCategory(categoryList);
    private final DeleteCategory deleteCategoryController = new DeleteCategory(categoryList);

    private JPanel categoriesPanel;
    private JButton addCategoryButton;
    private JButton saveCategoryButton;
    private JButton closeButton;

    public CategoryManagementUI() {
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
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initEventHandlers() {
        addCategoryButton.addActionListener(e -> {
            categoryManagementButton();
            refreshCategories();
        });
        saveCategoryButton.addActionListener(e -> categorySaveButton());
        closeButton.addActionListener(e -> categoryClose());
    }

    // 카테고리 추가 팝업 창 열기
    public void categoryManagementButton() {
        AddCategoryUI dialog = new AddCategoryUI(this, addCategoryController);
        dialog.setVisible(true);
    }

    // 카테고리 목록 UI
    private void refreshCategories() {
        categoriesPanel.removeAll();
        for (Category category : categoryList) {
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            JButton btnCat = new JButton(category.cateName());
            JButton btnDel = new JButton("X");
            btnDel.setMargin(new Insets(1, 1, 1, 1));

            // 삭제 버튼
            btnDel.addActionListener(ev -> {
                deleteCategoryController.deleteCategory(category.cateId());
                JOptionPane.showMessageDialog(this, "삭제되었습니다.");
                refreshCategories();
            });

            item.add(btnCat);
            item.add(btnDel);
            categoriesPanel.add(item);
        }
        categoriesPanel.revalidate();
        categoriesPanel.repaint();
    }

    // 저장 버튼
    public void categorySaveButton() {
        JOptionPane.showMessageDialog(this, "저장되었습니다.");
    }

    // 닫기 버튼
    public void categoryClose() {
        dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CategoryManagementUI().setVisible(true));
    }
}
