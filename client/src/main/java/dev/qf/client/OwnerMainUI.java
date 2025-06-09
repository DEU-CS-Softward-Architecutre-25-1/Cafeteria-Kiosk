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

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E) a h시 mm분", Locale.KOREAN);

    public OwnerMainUI() {
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
        setTitle("점주 주문 관리 시스템");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

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

        JPanel centerPanel = new JPanel(new BorderLayout());

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton refreshButton = new JButton("주문 새로고침");
        refreshButton.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        refreshButton.addActionListener(this::handleRefresh);
        refreshPanel.add(refreshButton);

        centerPanel.add(refreshPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        JPanel navigationButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton mainUiButton = new JButton("메인 UI");
        JButton menuButton = new JButton("메뉴 관리");
        JButton exitButton = new JButton("종료");

        mainUiButton.addActionListener(e -> showUserMainUI());
        menuButton.addActionListener(e -> showMenuManagement());
        exitButton.addActionListener(e -> System.exit(0));

        navigationButtonPanel.add(mainUiButton);
        navigationButtonPanel.add(menuButton);
        navigationButtonPanel.add(exitButton);

        setLayout(new BorderLayout());
        add(centerPanel, BorderLayout.CENTER);
        add(navigationButtonPanel, BorderLayout.SOUTH);

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

    private void showUserMainUI() {
        this.dispose();
        new UserMainUI().setVisible(true);
    }

    private void showMenuManagement() {
        this.dispose();
        new MenuManagementUI().setVisible(true);
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
