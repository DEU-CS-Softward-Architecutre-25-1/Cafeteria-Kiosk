package dev.qf.client;

import common.Cart;
import common.Menu;
import common.OrderItem;
import common.registry.RegistryManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

public class UserMainUI extends JFrame {
    private final Cart cart = new Cart();
    private final CartController cartController = new CartController(cart);
    private final OptionSelectionController optionController = new OptionSelectionController();
    private final JPanel cartPanel = new JPanel();
    private final JPanel menuPanel = new JPanel(new GridLayout(0, 3, 10, 10));    private final List<common.Menu> allMenus;

    public UserMainUI() {
        setTitle("카페 키오스크");
        setSize(400, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 전체 메뉴 목록
//        allMenus = List.of(
//                new common.Menu("menu001", "아메리카노", 3000, "cate001", Path.of("/images/menu1.png"), "진한 에스프레소와 물", OptionGroup.loadOptionGroups("menu001")),
//                new common.Menu("menu002", "카페라떼", 3500, "cate001", Path.of("/images/menu2.png"), "에스프레소 + 스팀밀크", OptionGroup.loadOptionGroups("menu002")),
//                new common.Menu("menu003", "바닐라라떼", 4000, "cate001", Path.of("/images/menu3.png"), "바닐라향 가득한 라떼", OptionGroup.loadOptionGroups("menu003")),
//                new common.Menu("menu004", "아이스티", 3000, "cate002", Path.of("/images/menu4.png"), "상큼한 아이스티", OptionGroup.loadOptionGroups("menu004")),
//                new common.Menu("menu005", "허브티", 3200, "cate002", Path.of("/images/menu5.png"), "편안한 허브향", OptionGroup.loadOptionGroups("menu005"))
//        );
        allMenus = List.of();

        // === [상단] 카테고리 패널 ===
        JPanel categoryPanel = new JPanel(new FlowLayout());
        JButton coffeeBtn = new JButton("커피");
        JButton teaBtn = new JButton("티");
        JButton allBtn = new JButton("전체");

        coffeeBtn.addActionListener(e -> displayMenusByCategory("cate001"));
        teaBtn.addActionListener(e -> displayMenusByCategory("cate002"));
        allBtn.addActionListener(e -> displayMenusByCategory(null));

        categoryPanel.add(coffeeBtn);
        categoryPanel.add(teaBtn);
        categoryPanel.add(allBtn);

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
                : RegistryManager.CATEGORIES.getById(cateId).get().menus();

        for (Menu menu : filtered) {
            JPanel menuItemPanel = new JPanel();
            menuItemPanel.setLayout(new BoxLayout(menuItemPanel, BoxLayout.Y_AXIS));
            menuItemPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            // 이미지 로딩 및 크기 조절
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource(menu.imagePath().toString())));
            Image image = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            JLabel imgLabel = new JLabel(new ImageIcon(image));
            imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // 메뉴명과 가격
            JLabel nameLabel = new JLabel(menu.name(), SwingConstants.CENTER);
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel priceLabel = new JLabel("₩" + menu.price(), SwingConstants.CENTER);
            priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // 클릭 시 옵션 선택 UI 열기
            menuItemPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    new OptionSelectUI(menu, cartController, optionController, UserMainUI.this);
                }
            });

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
        for (var entry : cart.getItems().entrySet()) {
            OrderItem item = entry.getKey();
            int quantity = entry.getValue();
            JLabel label = new JLabel(item.getOrderDescription() + " x" + quantity + " = ₩" + (item.getTotalPrice() * quantity));
            cartPanel.add(label);
        }
        cartPanel.revalidate();
        cartPanel.repaint();
    }
}
