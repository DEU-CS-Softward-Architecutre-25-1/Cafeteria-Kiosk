package dev.qf.client;

import common.Cart;
import common.Menu;
import common.OrderItem;
import common.registry.RegistryManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class UserMainUI extends JFrame {
    private final Cart cart = new Cart();
    private final CartController cartController = new CartController(cart);
    private final OptionSelectionController optionController = new OptionSelectionController();
    private final JPanel cartPanel = new JPanel();
    private final JPanel menuPanel = new JPanel(new GridLayout(0, 3, 10, 10));
    private final List<Menu> allMenus;

    public UserMainUI() {
        allMenus = RegistryManager.MENUS.getAll();

        setTitle("카페 키오스크");
        setSize(800, 700);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        initComponents();
        displayMenusByCategory(null); // 초기에 전체 메뉴 표시
        refreshCart();
    }

    private void initComponents() {
        // === [상단] 카테고리 패널 ===
        JPanel categoryPanel = new JPanel(new FlowLayout());

        // 전체 버튼
        JButton allBtn = new JButton("전체");
        allBtn.addActionListener(e -> displayMenusByCategory(null));
        categoryPanel.add(allBtn);

        // 카테고리별 버튼
        RegistryManager.CATEGORIES.getAll().forEach(category -> {
            JButton button = new JButton(category.cateName());
            button.addActionListener(e -> displayMenusByCategory(category.cateId()));
            categoryPanel.add(button);
        });

        add(categoryPanel, BorderLayout.NORTH);

        // === [중단] 메뉴 패널 ===
        JScrollPane menuScrollPane = new JScrollPane(menuPanel);
        menuScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        menuScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(menuScrollPane, BorderLayout.CENTER);

        // === [하단] 장바구니 패널 ===
        cartPanel.setLayout(new BoxLayout(cartPanel, BoxLayout.Y_AXIS));
        JScrollPane cartScrollPane = new JScrollPane(cartPanel);
        cartScrollPane.setPreferredSize(new Dimension(800, 180));
        add(cartScrollPane, BorderLayout.SOUTH);
    }

    private void displayMenusByCategory(String cateId) {
        menuPanel.removeAll();

        List<Menu> filtered = (cateId == null)
                ? allMenus
                : RegistryManager.CATEGORIES.getById(cateId).orElseThrow().menus();

        for (Menu menu : filtered) {
            JPanel menuItemPanel = createMenuItemPanel(menu);
            menuPanel.add(menuItemPanel);
        }

        menuPanel.revalidate();
        menuPanel.repaint();
    }

    private JPanel createMenuItemPanel(Menu menu) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        panel.setPreferredSize(new Dimension(200, 250));
        panel.setBackground(Color.WHITE);
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 클릭 이벤트
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMenuClick(menu);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(new Color(240, 240, 240));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(Color.WHITE);
            }
        });

        // 이미지
        JLabel imgLabel = createImageLabel(menu);
        imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(imgLabel);

        panel.add(Box.createVerticalStrut(5));

        // 메뉴명
        JLabel nameLabel = new JLabel(menu.name(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(nameLabel);

        // 가격
        JLabel priceLabel = new JLabel("₩" + String.format("%,d", menu.price()), SwingConstants.CENTER);
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(priceLabel);

        return panel;
    }

    private JLabel createImageLabel(Menu menu) {
        JLabel imgLabel = new JLabel();
        imgLabel.setPreferredSize(new Dimension(150, 120));
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);

        try {
            if (menu.imagePath() != null) {
                ImageIcon icon = new ImageIcon(menu.imagePath().toString());
                Image image = icon.getImage().getScaledInstance(150, 120, Image.SCALE_SMOOTH);
                imgLabel.setIcon(new ImageIcon(image));
            } else {
                imgLabel.setText("이미지 없음");
                imgLabel.setBackground(new Color(245, 245, 245));
                imgLabel.setOpaque(true);
            }
        } catch (Exception e) {
            imgLabel.setText("🍽️");
            imgLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 30));
            imgLabel.setBackground(new Color(245, 245, 245));
            imgLabel.setOpaque(true);
        }

        return imgLabel;
    }

    private void handleMenuClick(Menu menu) {
        // 옵션이 있는 메뉴
        if (menu.optionGroup() != null && !menu.optionGroup().isEmpty()) {
            new OptionSelectUI(menu, cartController, optionController, this);
        } else {
            // 옵션이 없는 메뉴는 바로 추가
            OrderItem item = new OrderItem(menu, java.util.Map.of(), 1);
            cartController.addItemToCart(item);
            refreshCart();
            JOptionPane.showMessageDialog(this, menu.name() + " 추가 완료!", "알림", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void refreshCart() {
        cartPanel.removeAll();

        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));

        // 장바구니 항목들
        for (OrderItem item : cart.getItems()) {
            int quantity = item.getQuantity();
            JLabel label = new JLabel(item.getOrderDescription() + " x" + quantity + " = ₩" + String.format("%,d", item.getTotalPrice()));

            JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            linePanel.add(label);
            linePanel.setOpaque(false);
            linePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));

            itemPanel.add(linePanel);
        }

        // 총합 패널
        JLabel totalLabel = new JLabel("총 합계: ₩" + String.format("%,d", cartController.getCartTotal()));
        totalLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));

        JButton payButton = new JButton("결제");
        payButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        payButton.setBackground(new Color(70, 130, 180));
        payButton.setForeground(Color.WHITE);
        payButton.addActionListener(e -> {
            if (cart.getItems().isEmpty()) {
                JOptionPane.showMessageDialog(this, "장바구니가 비어있습니다.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            PaymentUI paymentUI = new PaymentUI(cartController.getCartTotal(), cart, this);
            paymentUI.setVisible(true);
        });

        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        summaryPanel.add(totalLabel, BorderLayout.WEST);
        summaryPanel.add(payButton, BorderLayout.EAST);

        // cartPanel 구성
        cartPanel.setLayout(new BorderLayout());
        cartPanel.add(itemPanel, BorderLayout.CENTER);
        cartPanel.add(summaryPanel, BorderLayout.SOUTH);

        cartPanel.revalidate();
        cartPanel.repaint();
    }
}