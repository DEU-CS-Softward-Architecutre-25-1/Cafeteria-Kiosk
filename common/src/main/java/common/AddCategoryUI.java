package common;

import javax.swing.*;
import java.awt.*;

public class AddCategoryUI extends JDialog {
    private JTextField cateIdText;
    private JTextField cateNameText;
    private JButton cateAddButton;
    private JButton cancelButton;

    private final AddCategory addCategoryController;

    public AddCategoryUI(Frame owner, AddCategory controller) {
        super(owner, "카테고리 추가", true);
        this.addCategoryController = controller;
        initComponents();
        initEventHandlers();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        cateIdText = new JTextField(15);
        cateNameText = new JTextField(15);
        cateAddButton = new JButton("추가");
        cancelButton = new JButton("닫기");

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.add(new JLabel("카테고리 ID :"));
        inputPanel.add(cateIdText);
        inputPanel.add(new JLabel("카테고리 이름 :"));
        inputPanel.add(cateNameText);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cateAddButton);
        buttonPanel.add(cancelButton);

        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(inputPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void initEventHandlers() {
        cateAddButton.addActionListener(e -> onAdd());
        cancelButton.addActionListener(e -> onCancel());
    }

    private void onAdd() {
        String id = getCategoryId();
        String name = getCategoryName();

        // ID와 이름 검증
        boolean success = addCategoryController.addCategory(id, name);
        if (!success) { // ID나 이름이 잘못되었을 경우 오류메시지 출력
            JOptionPane.showMessageDialog(this, "ID와 이름을 확인해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, name + " 카테고리가 추가되었습니다.");
        dispose();
    }

    // 닫기 버튼
    private void onCancel() {
        dispose();
    }

    public String getCategoryId() {
        return cateIdText.getText().trim();
    }

    public String getCategoryName() {
        return cateNameText.getText().trim();
    }

}
