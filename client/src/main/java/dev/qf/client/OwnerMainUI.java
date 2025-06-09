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
    private JButton refreshButton;
    private final OrderService orderService;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년M월d일(E) HH:mm", Locale.KOREAN);

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
                    int orderId = (Integer) tableModel.getValueAt(row, 0);
                    OrderDetailView detailView = new OrderDetailView(orderId, OwnerMainUI.this);
                    detailView.setVisible(true);
                }
            }
        });
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
    /*
    하나의 데이터 목록을 한쪽(네트워크 스레드)에서는 수정하고, 다른 한쪽(UI 스레드)에서는 읽으려고 동시에 달려들 때 오류
    발생하여 OwnerMainUI.java에서 테이블을 업데이트할 때, 원본 데이터 리스트의 사본을 만들어 전송하는것으로 수정
     */
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

    //'주문 새로고침' 버튼의 동작을 loadOrderData() 호출에서 requestOrderListUpdate() 호출로 변경.
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