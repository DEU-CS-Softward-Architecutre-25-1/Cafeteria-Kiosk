package dev.qf.client;

import common.Order;
import common.OrderService;
import common.registry.RegistryManager;
import dev.qf.client.event.DataReceivedEvent;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OwnerMainUI extends JFrame {
    private JTable orderTable;
    private DefaultTableModel tableModel;
    private final OrderService orderService;
    private final String userRole;
    private final String userId;
    private JLabel welcomeLabel;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E) a h시 mm분", Locale.KOREAN);

    public OwnerMainUI(String userId, String userRole) {
        this.userId = userId;
        this.userRole = userRole;
        this.orderService = Main.getClientOrderService();

        initializeUI();
        loadOrderData();

        DataReceivedEvent.EVENT.register((handler, registry) -> {
            if (registry == RegistryManager.ORDERS) {
                SwingUtilities.invokeLater(this::loadOrderData);
            }
        });
    }

    private void initializeUI() {
        String roleText = "OWNER".equals(userRole) ? "관리자" : "사용자";
        setTitle(String.format("카페테리아 키오스크 - %s (%s)", userId, roleText));
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // 전체 레이아웃
        setLayout(new BorderLayout(10, 10));

        // 상단 웰컴 패널 추가
        add(createWelcomePanel(), BorderLayout.NORTH);

        // 중앙 패널 (주문 관리)
        add(createCenterPanel(), BorderLayout.CENTER);

        // 하단 네비게이션 패널
        add(createNavigationPanel(), BorderLayout.SOUTH);

        // 주문 테이블 클릭 이벤트
        orderTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = orderTable.rowAtPoint(evt.getPoint());
                if (row != -1 && evt.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    int orderId = (Integer) tableModel.getValueAt(row, 0);
                    OrderDetailView detailView = new OrderDetailView(orderId, OwnerMainUI.this);
                    detailView.setVisible(true);
                }
            }
        });
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        panel.setBackground(new Color(70, 130, 180));

        String roleText = "OWNER".equals(userRole) ? "관리자" : "사용자";
        welcomeLabel = new JLabel(String.format("환영합니다, %s님! (%s)", userId, roleText), SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        welcomeLabel.setForeground(Color.WHITE);

        panel.add(welcomeLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // 주문 관리 제목과 새로고침 버튼
        JPanel headerPanel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("주문 관리", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton refreshButton = createStyledButton("주문 새로고침", new Color(70, 130, 180));
        refreshButton.addActionListener(this::handleRefresh);
        refreshPanel.add(refreshButton);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(refreshPanel, BorderLayout.CENTER);

        // 주문 테이블 설정
        String[] columnNames = {"주문번호", "시간", "상태"};
        tableModel = new DefaultTableModel(columnNames, 0);
        orderTable = new JTable(tableModel);

        Font bigFont = new Font("맑은 고딕", Font.PLAIN, 20);
        orderTable.setFont(bigFont);
        orderTable.setRowHeight(32);

        JTableHeader tableHeader = orderTable.getTableHeader();
        tableHeader.setFont(new Font("맑은 고딕", Font.BOLD, 20));

        tableHeader.setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        orderTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        orderTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        orderTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(orderTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("주문 목록 (클릭하여 상세보기)"));

        centerPanel.add(headerPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        return centerPanel;
    }

    private JPanel createNavigationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // 왼쪽패널
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton kioskButton = createStyledButton("키오스크", new Color(60, 179, 113));
        kioskButton.addActionListener(e -> openKiosk());
        leftPanel.add(kioskButton);

        // 관리자만 볼 수 있는 버튼
        if ("OWNER".equals(userRole)) {
            JButton menuButton = createStyledButton("메뉴 관리", new Color(220, 20, 60));
            menuButton.addActionListener(e -> openMenuManagement());
            leftPanel.add(menuButton);

            JButton categoryButton = createStyledButton("카테고리 관리", new Color(138, 43, 226));
            categoryButton.addActionListener(e -> openCategoryManagement());
            leftPanel.add(categoryButton);
        }

        // 오른쪽패널
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton logoutButton = createStyledButton("로그아웃", new Color(105, 105, 105));
        logoutButton.addActionListener(e -> logout());
        rightPanel.add(logoutButton);

        JButton exitButton = createStyledButton("종료", new Color(178, 34, 34));
        exitButton.addActionListener(e -> exitApplication());
        rightPanel.add(exitButton);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(140, 40));
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


    private void openKiosk() {
        try {
            UserMainUI kioskUI = new UserMainUI();
            kioskUI.setVisible(true);
        } catch (Exception e) {
            showErrorMessage("키오스크 화면을 여는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void openMenuManagement() {
        try {
            MenuManagementUI menuUI = new MenuManagementUI();
            menuUI.setVisible(true);
        } catch (Exception e) {
            showErrorMessage("메뉴 관리 화면을 여는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void openCategoryManagement() {
        try {
            CategoryManagementUI categoryUI = new CategoryManagementUI();
            categoryUI.setVisible(true);
        } catch (Exception e) {
            showErrorMessage("카테고리 관리 화면을 여는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void logout() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "로그아웃 하시겠습니까?",
                "로그아웃 확인",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            dispose();
            new LoginUI().setVisible(true);
        }
    }

    private void exitApplication() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "프로그램을 종료하시겠습니까?",
                "종료 확인",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
    public void loadOrderData() {
        SwingWorker<List<Order>, Void> worker = new SwingWorker<>() {
            @Override
            protected @Nullable List<Order> doInBackground() {
                return orderService.getOrderList();
            }

            @Override
            protected void done() {
                try {
                    updateOrderTable(get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private String convertStatusToKorean(common.OrderStatus status) {
        return switch (status) {
            case ACCEPTED -> "수락됨";
            case CANCELED -> "취소됨";
            case PENDING -> "대기중";
            default -> "알수없음";
        };
    }

    private void updateOrderTable(List<Order> orders) {
        tableModel.setRowCount(0);
        if (orders != null) {
            for (Order order : new ArrayList<>(orders)) {
                Object[] rowData = {
                        order.orderId(),
                        order.orderTime().format(TIME_FORMATTER),
                        convertStatusToKorean(order.status())
                };
                tableModel.addRow(rowData);
            }
        }
    }

    private void handleRefresh(ActionEvent e) {
        ((ClientOrderService) orderService).requestOrderListUpdate();
    }

    public OrderService getOrderService() {
        return orderService;
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "오류",
                JOptionPane.ERROR_MESSAGE
        );
    }

    static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setHorizontalAlignment(SwingConstants.CENTER);

            if (value != null) {
                String status = value.toString();
                if ("수락됨".equals(status)) {
                    c.setForeground(Color.BLUE);
                } else if ("취소됨".equals(status)) {
                    c.setForeground(Color.RED);
                } else {
                    c.setForeground(Color.BLACK);
                }
            } else {
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }
}
