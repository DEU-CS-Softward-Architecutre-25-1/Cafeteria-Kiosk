package dev.qf.client;

import dev.qf.client.event.LoginEvent;

import java.util.HashMap;
import java.util.Map;

public class LoginController {
    public static LoginEvent LoginEvent;
    private final Map<String, String> ownerAccounts = new HashMap<>();
    private final Map<String, String> userAccounts  = new HashMap<>();

    // 테스트용 관리자와 사용자 계정 지정
    public LoginController() {
        ownerAccounts.put("owner", "1234");
        userAccounts.put("user", "5678");
    }

    // 입력한 id, pw값 검증
    public String login(String id, String pw) {
        // 해당 id의 pw와 입력한 pw 비교
        boolean isOwner = pw != null && pw.equals(ownerAccounts.get(id));
        boolean isUser  = pw != null && pw.equals(userAccounts.get(id));

        String role = null;
        if (isOwner)  role = "OWNER"; // 관리자로 성공하였을 경우 role = OWNER
        else if (isUser) role = "USER"; // 사용자로 성공하였을 경우 role = USER

        // 로그인 성공시 문자열 반환
        dev.qf.client.event.LoginEvent.EVENT.invoker().onLogin(id, role != null, role);

        return role;
    }
}