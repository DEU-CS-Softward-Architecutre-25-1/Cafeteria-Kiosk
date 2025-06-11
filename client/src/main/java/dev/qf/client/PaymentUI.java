package dev.qf.client;

import common.OrderItem;
import common.Cart;
import common.Order;
import common.network.packet.OrderStatusChangedC2SPacket;
import common.util.Container;
import dev.qf.client.network.KioskNettyClient;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.Random;

import common.Menu;
import common.Option;
import common.OptionGroup;
import common.OrderStatus;

public class PaymentUI extends JFrame {

    public PaymentUI(int totalPrice, Cart cart, UserMainUI parentUI) {
        setTitle("결제");
        setSize(400, 300);
        setLayout(new BorderLayout());

        // 주문 항목 영역
        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(itemPanel);

        for (OrderItem item : cart.getItems()) {
            // item.getQuantity() 사용
            JLabel label = new JLabel(item.getOrderDescription() + " x" + item.getQuantity() + " = ₩" + (item.getTotalPrice() * item.getQuantity()));
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

            int orderId = dev.qf.client.Main.getNextLocalOrderIdAndIncrement(); // Main 클래스에 메서드 추가 필요

            String customerName = "익명";

            Order newOrder = new Order(
                    orderId,
                    customerName,
                    LocalDateTime.now(),
                    common.OrderStatus.PENDING,
                    cart
            );

            // 생성된 Order 객체를 서버로 전송 시작
            KioskNettyClient client = (KioskNettyClient) Container.get(common.network.Connection.class);
            if (client != null && client.isConnected()) {
                client.sendSerializable(new OrderStatusChangedC2SPacket(newOrder));
                System.out.println("주문 정보(ID: " + newOrder.orderId() + ") 서버로 전송 요청 완료.");
            } else {
                System.err.println("서버 연결 실패: 주문 정보를 전송할 수 없습니다.");
                JOptionPane.showMessageDialog(this,
                        "서버 연결 실패: 주문 정보를 전송할 수 없습니다.",
                        "오류", JOptionPane.ERROR_MESSAGE);
            }

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
