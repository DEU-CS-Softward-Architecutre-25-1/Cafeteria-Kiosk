package dev.qf.client;

import common.Order;
import common.OrderService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class OwnerMainUI extends JFrame {
    private JTable orderTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private OrderService orderService;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년M월d일(E) HH:mm", Locale.KOREAN);

    public OwnerMainUI() {
        initializeUI();
        setupOrderService();
    }

    private void initializeUI() {
        setTitle("점주 주문 관리 시스템");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String[] columnNames = {"주문번호", "시간", "상태"};
        tableModel = new DefaultTableModel(columnNames, 0);
        orderTable = new JTable(tableModel);

        Font bigFont = new Font("맑은 고딕", Font.PLAIN, 20);
        orderTable.setFont(bigFont);
        orderTable.setRowHeight(32);

        JTableHeader tableHeader = orderTable.getTableHeader();
        tableHeader.setFont(new Font("맑은 고딕", Font.BOLD, 20));

        // 주문상태 컬럼(2번 인덱스)에만 컬러 렌더러 적용
        orderTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());

        JPanel buttonPanel = new JPanel();
        refreshButton = new JButton("주문 새로고침");
        refreshButton.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        refreshButton.addActionListener(this::handleRefresh);
        buttonPanel.add(refreshButton);

        setLayout(new BorderLayout());
        add(new JScrollPane(orderTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        orderTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = orderTable.rowAtPoint(evt.getPoint());
                if (row != -1 && evt.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    int orderId = (Integer) orderTable.getValueAt(row, 0);
                    OrderDetailView detailView = new OrderDetailView(orderId, OwnerMainUI.this);
                    detailView.setVisible(true);
                }
            }
        });
    }

    private void setupOrderService() {
        this.orderService = new ClientOrderService();
        loadOrderData();
    }

    public void loadOrderData() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            List<Order> orders;
            @Override
            protected Void doInBackground() {
                orders = orderService.getOrderList();
                return null;
            }

            @Override
            protected void done() {
                updateOrderTable(orders);
            }
        };
        worker.execute();
    }

    private void updateOrderTable(List<Order> orders) {
        tableModel.setRowCount(0);
        for (Order order : orders) {
            Object[] rowData = {
                    order.orderId(),
                    order.orderTime().format(TIME_FORMATTER),
                    order.status()
            };
            tableModel.addRow(rowData);
        }
    }

    private void handleRefresh(ActionEvent e) {
        loadOrderData();
    }

    public OrderService getOrderService() {
        return orderService;
    }

    static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
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
