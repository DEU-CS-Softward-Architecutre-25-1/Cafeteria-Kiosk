package dev.qf.client;

import com.google.common.collect.Lists;
import common.Category;
import common.MenuService;
import common.registry.RegistryManager;
import dev.qf.client.event.DataReceivedEvent;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class CategoryManagementUI extends JFrame {
    // 카테고리 리스트와 컨트롤러
    private final List<Category> categoryList = Lists.newArrayList(RegistryManager.CATEGORIES.getAll());
    private final AddCategory addCategoryController = new AddCategory(categoryList);
    private final DeleteCategory deleteCategoryController = new DeleteCategory(categoryList);
    private final MenuService menuService = MenuService.getInstance();

    private JPanel categoriesPanel;
    private JButton addCategoryButton;
    private JButton saveCategoryButton;
    private JButton closeButton;


    private final Set<String> deletedCategoryIds = new HashSet<>();
    private Timer updateTimer;

    public CategoryManagementUI() {
        initComponents();
        initEventHandlers();
        refreshCategories();

        // DataReceivedEvent 등록
        DataReceivedEvent.EVENT.register((handler, registry) -> {
            if (!registry.getClazz().equals(Category.class)) {
                return;
            }

            if (this.isVisible()) {
                SwingUtilities.invokeLater(() -> {
                    scheduleUpdate();
                });
            }
        });
    }

    private void scheduleUpdate() {
        if (updateTimer != null) {
            updateTimer.stop();
        }

        updateTimer = new Timer(100, e -> { // 100ms 지연
            onServerUpdate();
            this.categoryList.clear();
            this.categoryList.addAll(getAllCategoriesForDisplay());
            this.refreshCategories();
        });
        updateTimer.setRepeats(false);
        updateTimer.start();
    }

    public List<Category> getAllCategoriesForDisplay() {
        return RegistryManager.CATEGORIES.getAll().stream()
                .filter(category -> !deletedCategoryIds.contains(category.cateId()))
                .collect(Collectors.toList());
    }

    public void onServerUpdate() {
        // 현재 존재하는 카테고리 ID들 가져오기
        Set<String> currentCategoryIds = RegistryManager.CATEGORIES.getAll().stream()
                .map(Category::cateId)
                .collect(Collectors.toSet());
        deletedCategoryIds.removeIf(categoryId -> !currentCategoryIds.contains(categoryId));
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

    // 카테고리 추가 팝업 창 열기
    public void categoryManagementButton() {
        AddCategoryUI dialog = new AddCategoryUI(this, addCategoryController);
        dialog.setVisible(true);
    }

    // 카테고리 목록 UI
    private void refreshCategories() {
        categoriesPanel.removeAll();
        List<Category> displayCategories = getAllCategoriesForDisplay();

        for (Category category : displayCategories) {
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            JButton btnCat = new JButton(category.cateName());
            JButton btnDel = new JButton("X");
            btnDel.setMargin(new Insets(1, 1, 1, 1));

            // 삭제 버튼
            btnDel.addActionListener(ev -> {
                handleDeleteCategory(category);
            });
            item.add(btnCat);
            item.add(btnDel);
            categoriesPanel.add(item);
        }
        categoriesPanel.revalidate();
        categoriesPanel.repaint();
    }

    private void handleDeleteCategory(Category category) {
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
            String categoryId = category.cateId();
            String categoryName = category.cateName();

            deletedCategoryIds.add(categoryId);

            boolean success = deleteCategoryController.deleteCategory(categoryId);

            if (success) {
                // UI 새로고침
                SwingUtilities.invokeLater(() -> {
                    // 로컬 리스트에서도 제거
                    categoryList.removeIf(c -> c.cateId().equals(categoryId));
                    refreshCategories();
                });

                if (menuCount > 0) {
                    JOptionPane.showMessageDialog(this,
                            String.format("카테고리 '%s'과(와) 관련 메뉴 %d개 삭제가 요청되었습니다.", categoryName, menuCount));
                } else {
                    JOptionPane.showMessageDialog(this,
                            String.format("카테고리 '%s' 삭제가 요청되었습니다.", categoryName));
                }
            } else {
                deletedCategoryIds.remove(categoryId);
                JOptionPane.showMessageDialog(this, "삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 저장 버튼
    public void categorySaveButton() {
        JOptionPane.showMessageDialog(this, "저장되었습니다.");
    }

    // 닫기 버튼
    public void categoryClose() {
        dispose();
    }
}
