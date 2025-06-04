package dev.qf.client;

import common.Menu;
import common.Option;
import common.OptionGroup;
import common.OrderItem;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class OptionSelectUI extends JFrame {
    private final common.Menu selectedMenu;
    private final CartController cartController;
    private final OptionSelectionController optionController;
    private final UserMainUI parentUI;
    private final Map<OptionGroup, Option> selectedOptions = new HashMap<>(); // 타입 변경
    private final List<OptionGroup> optionGroups;

    public OptionSelectUI(Menu menu, CartController cartController, OptionSelectionController optionController, UserMainUI parentUI) {
        this.selectedMenu = menu;
        this.cartController = cartController;
        this.optionController = optionController;
        this.parentUI = parentUI;

        setTitle(menu.name() + " 옵션 선택");
        setSize(300, 400);
        setLayout(new BorderLayout());

        this.optionGroups = OptionGroup.loadOptionGroups(menu.id());

        JPanel optionPanel = new JPanel(new GridLayout(0, 1));
        for (OptionGroup group : optionGroups) {
            optionPanel.add(new JLabel("[" + group.name() + "]"));
            ButtonGroup btnGroup = new ButtonGroup();
            for (Option opt : group.options()) {
                JRadioButton rb = new JRadioButton(opt.name() + " (₩" + opt.extraCost() + ")");
                // OptionGroup 객체를 직접 키로 사용
                rb.addActionListener(e -> selectedOptions.put(group, opt));
                btnGroup.add(rb);
                optionPanel.add(rb);
            }
        }

        JButton addButton = new JButton("장바구니에 추가");
        addButton.addActionListener(e -> {
            // 타입 일치: Map<OptionGroup,Option> → Map<OptionGroup,Option>
            if (optionController.isValidOption(selectedOptions, optionGroups)) {
                OrderItem item = optionController.createOrderItem(selectedMenu, selectedOptions, 1);
                cartController.addItemToCart(item);
                parentUI.refreshCart();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "필수 옵션을 선택하세요.");
            }
        });

        add(new JScrollPane(optionPanel), BorderLayout.CENTER);
        add(addButton, BorderLayout.SOUTH);
        setVisible(true);
    }

    // toKeyMap 메서드 제거 (더 이상 필요 없음)
}
