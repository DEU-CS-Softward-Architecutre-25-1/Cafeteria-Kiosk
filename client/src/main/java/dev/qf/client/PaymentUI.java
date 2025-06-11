package dev.qf.client;

import common.*;
import common.network.Connection;
import common.registry.RegistryManager;
import common.util.Container;
import common.util.KioskLoggerFactory;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class PaymentUI extends JFrame {
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();
    private static final AtomicInteger ORDER_ID_COUNTER = new AtomicInteger(1000);

    private final int totalPrice;
    private final Cart cart;
    private final UserMainUI parentUI;
    private JButton payButton;

    public PaymentUI(int totalPrice, Cart cart, UserMainUI parentUI) {
        this.totalPrice = totalPrice;
        this.cart = cart;
        this.parentUI = parentUI;

        setTitle("결제");
        setSize(500, 400);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(parentUI);

        initComponents();
    }

    private void initComponents() {
        // 상단 제목
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("결제 확인", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // 중앙 주문 내역
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        // 주문 항목 리스트
        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
        itemPanel.setBorder(BorderFactory.createTitledBorder("주문 내역"));

        for (OrderItem item : cart.getItems()) {
            int qty = item.getQuantity();
            int itemTotal = item.getTotalPrice();

            JPanel linePanel = new JPanel(new BorderLayout());
            linePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            JLabel nameLabel = new JLabel(item.getMenuItem().name());
            nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));

            String optionsText = "";
            if (!item.getSelectedOptions().isEmpty()) {
                optionsText = " (" + item.getSelectedOptions().values().stream()
                        .map(option -> option.name())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("") + ")";
            }

            JLabel detailLabel = new JLabel(String.format("x%d%s", qty, optionsText));
            detailLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            detailLabel.setForeground(Color.GRAY);

            JLabel priceLabel = new JLabel(String.format("₩%,d", itemTotal));
            priceLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));

            JPanel leftPanel = new JPanel();
            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
            leftPanel.add(nameLabel);
            leftPanel.add(detailLabel);

            linePanel.add(leftPanel, BorderLayout.WEST);
            linePanel.add(priceLabel, BorderLayout.EAST);

            itemPanel.add(linePanel);
        }

        JScrollPane scrollPane = new JScrollPane(itemPanel);
        scrollPane.setPreferredSize(new Dimension(450, 200));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // 총 금액 표시
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel totalLabel = new JLabel(String.format("총 합계: ₩%,d", totalPrice));
        totalLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        totalLabel.setForeground(new Color(220, 20, 60));
        totalPanel.add(totalLabel);
        centerPanel.add(totalPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton cancelButton = createStyledButton("취소", new Color(105, 105, 105));
        cancelButton.addActionListener(e -> dispose());

        payButton = createStyledButton("결제하기", new Color(70, 130, 180));
        payButton.addActionListener(e -> processPayment());

        buttonPanel.add(cancelButton);
        buttonPanel.add(payButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(120, 45));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 호버 효과
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private void showWarningMessage(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "경고",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void processPayment() {
        // 결제 처리 중 표시
        payButton.setText("처리 중...");
        payButton.setEnabled(false);

        try {
            // 고객 이름 입력 받기
            String customerName = JOptionPane.showInputDialog(
                    this,
                    "주문자 성함을 입력해주세요:",
                    "주문자 정보",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (customerName == null || customerName.trim().isEmpty()) {
                customerName = "고객" + System.currentTimeMillis() % 1000;
            }

            // ID 증가
            int orderId = ORDER_ID_COUNTER.getAndIncrement();
            LocalDateTime orderTime = LocalDateTime.now();

            // 장바구니 복사
            Cart orderCart = new Cart();
            for (OrderItem item : cart.getItems()) {
                orderCart.addItem(item);
            }

            Order newOrder = new Order(orderId, customerName.trim(), orderTime, OrderStatus.PENDING, orderCart);

            // 로컬 레지스트리에 주문 추가
            RegistryManager.ORDERS.unfreeze();
            RegistryManager.ORDERS.addOrder(newOrder);
            LOGGER.info("로컬 레지스트리에 주문 추가됨: {}", orderId);
            sendOrderToServer(newOrder);
            showPaymentComplete(orderId, customerName);
            refreshUI();
            dispose();

            LOGGER.info("주문 처리 완료 - 주문번호: {}, 고객: {}, 금액: ₩{}", orderId, customerName, totalPrice);

        } catch (Exception e) {
            LOGGER.error("결제 처리 중 오류 발생", e);
            showPaymentError();
        }
    }

    private void sendOrderToServer(Order order) {
        Connection connection = Container.get(Connection.class);
        if (connection != null) {
            try {
                common.network.packet.OrderStatusChangedC2SPacket orderPacket =
                        new common.network.packet.OrderStatusChangedC2SPacket(order);

                var future = connection.sendSerializable("server", orderPacket);
                if (future != null) {
                    future.addListener(f -> {
                        if (f.isSuccess()) {
                            LOGGER.info("주문이 서버로 성공적으로 전송됨: {}", order.orderId());
                        } else {
                            LOGGER.error("주문 서버 전송 실패: {}", f.cause().getMessage());
                        }
                    });
                }
                LOGGER.info("주문을 서버로 전송 중... 주문번호: {}", order.orderId());

            } catch (Exception e) {
                LOGGER.error("서버 전송 실패: {}", e.getMessage());
                showWarningMessage("주문은 접수되었지만 서버 전송에 실패했습니다.\n관리자에게 문의하세요.");
            }
        } else {
            LOGGER.warn("서버 연결이 없습니다. 주문이 로컬에만 저장됩니다.");
            showWarningMessage("서버 연결이 없어 주문이 임시 저장되었습니다.\n인터넷 연결을 확인하세요.");
        }
    }

    private void showPaymentComplete(int orderId, String customerName) {
        String message = String.format(
                "결제가 완료되었습니다!\n\n" +
                        "주문번호: %d\n" +
                        "주문자: %s\n" +
                        "총 금액: ₩%,d\n\n" +
                        "주문이 접수되었습니다.",
                orderId, customerName, totalPrice
        );

        JOptionPane.showMessageDialog(
                this,
                message,
                "결제 완료",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showPaymentError() {
        JOptionPane.showMessageDialog(
                this,
                "결제 처리 중 오류가 발생했습니다.\n다시 시도해주세요.",
                "결제 오류",
                JOptionPane.ERROR_MESSAGE
        );

        // 버튼 상태 복원
        payButton.setText("결제하기");
        payButton.setEnabled(true);
    }

    private void refreshUI() {
        // OwnerMainUI 새로고침
        ClientOrderService clientOrderService = Main.getClientOrderService();
        if (clientOrderService != null) {
            clientOrderService.refreshOwnerMainUIOrders();
            LOGGER.info("주문 관리 UI 새로고침 요청 완료");
        }

        // 장바구니 비우기
        cart.clear();

        // 부모 UI 새로고침
        if (parentUI != null) {
            parentUI.refreshCart();
        }
    }
}