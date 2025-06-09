package dev.qf.client;

import common.Cart;
import common.Menu;
import common.OrderItem;
import common.registry.RegistryManager;
import common.registry.RegistryManager;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class UserMainUI extends JFrame {
    private final Cart cart = new Cart();
    private final CartController cartController = new CartController(cart);
    private final OptionSelectionController optionController = new OptionSelectionController();
    private final JPanel cartPanel = new JPanel();
    private final JPanel menuPanel = new JPanel(new GridLayout(0, 3, 10, 10));    private final List<common.Menu> allMenus;

    public UserMainUI() {
        allMenus = RegistryManager.MENUS.getAll();

        setTitle("카페 키오스크");
        setSize(400, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // === [상단] 카테고리 패널 ===
        JPanel categoryPanel = new JPanel(new FlowLayout());
        // 누가 이거 하드코딩하래요?
//        JButton coffeeBtn = new JButton("커피");
//        JButton teaBtn = new JButton("티");
//        JButton allBtn = new JButton("전체");
//
//        coffeeBtn.addActionListener(e -> displayMenusByCategory("cate001"));
//        teaBtn.addActionListener(e -> displayMenusByCategory("cate002"));
//        allBtn.addActionListener(e -> displayMenusByCategory(null));
//
//        categoryPanel.add(coffeeBtn);
//        categoryPanel.add(teaBtn);
//        categoryPanel.add(allBtn);

        RegistryManager.CATEGORIES.getAll().forEach(category -> {
            JButton button = new JButton(category.cateName());
            button.addActionListener(e -> displayMenusByCategory(category.cateId()));
            categoryPanel.add(button);
        });

        add(categoryPanel, BorderLayout.NORTH);

        // === [중단] 메뉴 패널 ===
        JScrollPane menuScrollPane = new JScrollPane(menuPanel);
        menuScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        menuScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(menuScrollPane, BorderLayout.CENTER);

        // === [하단] 장바구니 패널 ===
        cartPanel.setLayout(new BoxLayout(cartPanel, BoxLayout.Y_AXIS));
        JScrollPane cartScrollPane = new JScrollPane(cartPanel);
        cartScrollPane.setPreferredSize(new Dimension(400, 150));
        add(cartScrollPane, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void displayMenusByCategory(String cateId) {
        menuPanel.removeAll();
        List<Menu> filtered = (cateId == null)
                ? allMenus
                : RegistryManager.CATEGORIES.getById(cateId).orElseThrow().menus();

        for (Menu menu : filtered) {
            System.out.println("--- Debugging Menu ---");
            System.out.println("Menu ID: " + menu.id());
            System.out.println("Menu Name: " + menu.name());
            System.out.println("Menu Price: " + menu.price());
            System.out.println("Menu Image Path: " + menu.imagePath());
            System.out.println("Menu Sold Out: " + menu.soldOut()); // 품절 여부도 확인
            System.out.println("--------------------");

            JPanel menuItemPanel = new JPanel();
            menuItemPanel.setLayout(new BoxLayout(menuItemPanel, BoxLayout.Y_AXIS));
            menuItemPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JLabel imgLabel = new JLabel();
            // 이미지 로딩 및 크기 조절
            try {
                String imagePathString = menu.imagePath().toString().replace("\\", "/");
                java.net.URL imageUrl = getClass().getClassLoader().getResource(imagePathString);
                if (imageUrl != null) {
                    // 이미지 URL을 찾았으면 ImageIcon을 생성하고 크기를 조절합니다.
                    ImageIcon icon = new ImageIcon(imageUrl);
                    Image image = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                    imgLabel = new JLabel(new ImageIcon(image));
                    // 로드 성공 로그 추가
                    System.out.println("이미지가 성공적으로 로드되었습니다. 메뉴: " + menu.name() + " (ID: " + menu.id() + "), 경로: " + imagePathString);
                } else {
                    // 이미지를 찾지 못했을 때: 에러 로그 출력 및 대체 이미지/텍스트 표시
                    System.err.println("이미지를 찾을 수 없습니다. 메뉴: " + menu.name() + " (ID: " + menu.id() + "), 검색 경로: " + imagePathString);
                    imgLabel = new JLabel("이미지 없음"); // 대체 텍스트
                    imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    imgLabel.setPreferredSize(new Dimension(100, 100));
                    imgLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2)); // 테두리 추가
                }
                imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                imgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent evt) {
                        new OptionSelectUI(menu, cartController, optionController, UserMainUI.this);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();  // 또는 사용자에게 기본 이미지로 대체하거나 에러 표시
            }

            // 메뉴명과 가격
            JLabel nameLabel = new JLabel(menu.name(), SwingConstants.CENTER);
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel priceLabel = new JLabel("₩" + menu.price(), SwingConstants.CENTER);
            priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            menuItemPanel.add(imgLabel);
            menuItemPanel.add(Box.createVerticalStrut(5));
            menuItemPanel.add(nameLabel);
            menuItemPanel.add(priceLabel);

            menuPanel.add(menuItemPanel);
        }

        menuPanel.revalidate();
        menuPanel.repaint();
    }

    public void refreshCart() {
        cartPanel.removeAll();

        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));

        for (var entry : cart.getItems()) {
            OrderItem item = entry;
            int quantity = entry.getQuantity();

            JLabel label = new JLabel(item.getOrderDescription() + " x" + quantity + " = ₩" + (item.getTotalPrice() * quantity));

            JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            linePanel.add(label);
            linePanel.setOpaque(false);
            linePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));

            itemPanel.add(linePanel);
        }
        // 총합 패널 (아랫쪽, 오른쪽 정렬)
        JLabel totalLabel = new JLabel("총 합계: ₩" + cartController.getCartTotal());
        totalLabel.setFont(new Font("Dialog", Font.BOLD, 14));

        JButton payButton = new JButton("결제");
        payButton.setFont(new Font("Dialog", Font.BOLD, 14));
        payButton.addActionListener(e -> new PaymentUI(cartController.getCartTotal(), cart, this));

        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BorderLayout());
        summaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        summaryPanel.add(totalLabel, BorderLayout.WEST);
        summaryPanel.add(payButton, BorderLayout.EAST);

        // cartPanel을 상하로 분할
        cartPanel.setLayout(new BorderLayout());
        cartPanel.add(itemPanel, BorderLayout.CENTER);
        cartPanel.add(summaryPanel, BorderLayout.SOUTH);

        cartPanel.revalidate();
        cartPanel.repaint();
    }
}
