package dev.qf.client;

import javax.swing.*;
import java.awt.*;
import common.*;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderDetailView extends JDialog {
    private OwnerMainUI ownerMainUI;
    private final int orderId;

    public OrderDetailView(int orderId, OwnerMainUI ownerMainUI) {
        this.orderId = orderId;
        this.ownerMainUI = ownerMainUI;

        setTitle("주문번호 " + orderId);
        setModal(true);
        setSize(400, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);

        JLabel orderNumLabel = new JLabel("주문번호 " + orderId, SwingConstants.CENTER);
        orderNumLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        orderNumLabel.setForeground(new Color(51, 153, 255));
        orderNumLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(Box.createVerticalStrut(16));
        contentPanel.add(orderNumLabel);
        contentPanel.add(Box.createVerticalStrut(12));

        Order order = getOrderDetail(orderId);
        Cart cart = order.cart();

        if (cart == null || cart.getItems().isEmpty()) {
            JLabel emptyLabel = new JLabel("주문 항목이 없습니다.");
            emptyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(emptyLabel);
        } else {
            for (var itemEntry : cart.getItems().entrySet()) {
                OrderItem orderItem = itemEntry.getKey();
                int quantity = itemEntry.getValue();

                JLabel menuLabel = new JLabel(orderItem.getMenuItem().name() + " " + quantity + "개");
                menuLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
                menuLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                contentPanel.add(menuLabel);

                String optionText = formatOptions(orderItem.getSelectedOptions());
                JLabel optionLabel = new JLabel(optionText);
                optionLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
                optionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                contentPanel.add(optionLabel);

                contentPanel.add(Box.createVerticalStrut(8));
            }
        }

        JPanel infoPanel = new JPanel(new GridLayout(1, 2, 2, 2));
        infoPanel.setOpaque(false);
        JLabel packLabel = new JLabel("포장 :");
        packLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        JLabel packValue = new JLabel("아니요");
        packValue.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
        infoPanel.add(packLabel);
        infoPanel.add(packValue);
        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(infoPanel);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(Color.WHITE);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        JButton cancelBtn = createButton("주문 취소", new Color(204, 0, 0));
        JButton acceptBtn = createButton("주문 수락", new Color(51, 153, 255));

        cancelBtn.addActionListener(e -> handleOrderAction(OrderStatus.CANCELED, "주문이 취소되었습니다."));
        acceptBtn.addActionListener(e -> handleOrderAction(OrderStatus.ACCEPTED, "주문이 수락되었습니다."));

        buttonPanel.add(cancelBtn);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(acceptBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JButton createButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        btn.setPreferredSize(new Dimension(140, 48));
        return btn;
    }

    private void handleOrderAction(OrderStatus status, String message) {
        ownerMainUI.getOrderService().updateOrderStatus(orderId, status);
        ownerMainUI.loadOrderData();
        JOptionPane.showMessageDialog(this, message);
        dispose();
    }

    private String formatOptions(Map<String, Option> selectedOptions) {
        if (selectedOptions.isEmpty()) return "(기본 옵션)";
        return selectedOptions.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue().name())
                .collect(Collectors.joining(", ", "(", ")"));
    }

    private Order getOrderDetail(int orderId) {
        for (Order order : ownerMainUI.getOrderService().getOrderList()) {
            if (order.orderId() == orderId) {
                return order;
            }
        }
        return Order.EMPTY;
    }
}
