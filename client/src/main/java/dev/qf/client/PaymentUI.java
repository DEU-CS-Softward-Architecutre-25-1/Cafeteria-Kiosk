package dev.qf.client;

import common.OrderItem;
import common.Cart;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class PaymentUI extends JFrame {

    public PaymentUI(int totalPrice, Cart cart, UserMainUI parentUI) {
        setTitle("결제");
        setSize(400, 300);
        setLayout(new BorderLayout());

        // 주문 항목 영역
        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(itemPanel);

        for (Map.Entry<OrderItem, Integer> entry : cart.getItems().entrySet()) {
            OrderItem item = entry.getKey();
            int qty = entry.getValue();
            JLabel label = new JLabel(item.getOrderDescription() + " x" + qty + " = ₩" + (item.getTotalPrice() * qty));
            itemPanel.add(label);
        }

        // 총합 금액 표시 (왼쪽 정렬)
        JLabel totalLabel = new JLabel("총 합계: ₩" + totalPrice);
        totalLabel.setFont(new Font("Dialog", Font.BOLD, 16));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.add(totalLabel);

        // 결제 버튼 (오른쪽 정렬)
        JButton payButton = new JButton("결제하기");
        payButton.setFont(new Font("Dialog", Font.BOLD, 14));
        payButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "결제가 완료되었습니다.");
            cart.clear();
            parentUI.dispose(); // 기존 창 닫기
            new UserMainUI();   // 새 창 열기
            dispose();          // 결제 창 닫기
        });

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(payButton);

        // 하단 패널 조합
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(leftPanel, BorderLayout.WEST);
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }
}
