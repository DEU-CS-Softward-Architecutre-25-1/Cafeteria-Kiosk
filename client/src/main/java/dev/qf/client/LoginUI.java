package dev.qf.client;

import dev.qf.client.event.LoginEvent;

import javax.swing.*;
import java.awt.*;


public class LoginUI extends JFrame {
    private JTextField idText;
    private JPasswordField pwText;
    private JButton loginButton, cancelButton;

    public LoginUI() {
        super("로그인");
        initComponents();
        initEventHandlers();
        setupLoginListener(); // 이벤트 리스너 등록
    }

    // 로그인 UI 구성
    private void initComponents() {
        // 1) ID/PW 입력 필드 생성
        idText = new JTextField(15);  // 너비를 약간 줄여서 컴팩트하게
        pwText = new JPasswordField(15);

        // 2) ID/PW 라벨 생성
        JLabel idLabel = new JLabel("ID: ");
        JLabel pwLabel = new JLabel("PW:");

        // 3) ID 입력 행: FlowLayout(LEFT, 5, 0)을 사용해 라벨과 필드를 붙임
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        idPanel.add(idLabel);
        idPanel.add(idText);

        // 4) PW 입력 행: FlowLayout(LEFT, 5, 0)을 사용해 라벨과 필드를 붙임
        JPanel pwPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pwPanel.add(pwLabel);
        pwPanel.add(pwText);

        // 5) 두 입력 행을 세로로 쌓는 패널 (BoxLayout.Y_AXIS)
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.add(Box.createVerticalStrut(10)); // 위쪽 여백
        inputPanel.add(idPanel);
        inputPanel.add(Box.createVerticalStrut(5));  // 행 간격을 5px로 줄임
        inputPanel.add(pwPanel);

        // 6) 로그인 버튼
        loginButton = new JButton("로그인");

        // 7) inputPanel과 로그인 버튼을 담을 중앙 패널
        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.add(inputPanel, BorderLayout.CENTER);
        center.add(loginButton, BorderLayout.EAST);

        // 8) 하단 패널: 닫기 버튼만 가운데에 배치
        cancelButton = new JButton("닫기");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        bottom.add(cancelButton);

        // 9) 전체 프레임에 두 패널 배치
        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);

        pack();                          // 컴포넌트 크기에 맞춰 창 크기 조정
        setLocationRelativeTo(null);     // 화면 정중앙에 배치
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // 이벤트 핸들러
    private void initEventHandlers() {
        // 로그인 버튼을 눌렀을때 이벤트 발생 기능
        loginButton.addActionListener(e -> {
            String id = idText.getText().trim();
            String pw = new String(pwText.getPassword()).trim();
            LoginController loginController = new LoginController();
            loginController.login(id, pw);
        });
        // 닫기 버튼을 누르면 종료
        cancelButton.addActionListener(e -> dispose());
    }

    private void setupLoginListener() {
        // 로그인 결과에 반응하는 리스너 등록
        LoginEvent.EVENT.register((id, success, role) -> {
            // 이 콜백은 로그인 시도가 끝날 때마다 호출됨
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    OwnerMainUI dashboard = new OwnerMainUI(id, role);

                    dashboard.setVisible(true);
                    dispose(); // 로그인 창 닫기

                    // 성공 메시지 표시
                    String roleText = "OWNER".equals(role) ? "관리자" : "사용자";
                    JOptionPane.showMessageDialog(
                            dashboard,
                            String.format("환영합니다, %s님! (%s)", id, roleText),
                            "로그인 성공",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
                // 입력값이 잘못 되었을 경우 else
                else {
                    JOptionPane.showMessageDialog(this,
                            "ID와 PW를 다시 확인해주세요.",
                            "로그인 오류",
                            JOptionPane.ERROR_MESSAGE);
                    pwText.setText("");
                    idText.requestFocus();
                }
            });
        });
    }
}
