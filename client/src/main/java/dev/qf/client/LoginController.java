package dev.qf.client;

import dev.qf.client.event.LoginEvent;
import common.util.KioskLoggerFactory;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class LoginController {
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();

    private final Map<String, String> ownerAccounts = new HashMap<>();
    private final Map<String, String> userAccounts  = new HashMap<>();

    // 테스트용 관리자와 사용자 계정 지정
    public LoginController() {
        ownerAccounts.put("owner", "1234");
        ownerAccounts.put("manager", "1234");

        userAccounts.put("user", "5678");
        userAccounts.put("customer", "1234");

        LOGGER.info("LoginController 초기화 완료 - 관리자 계정: {}, 사용자 계정: {}",
                ownerAccounts.size(), userAccounts.size());
    }

    // 입력한 id, pw값 검증
    public String login(String id, String pw) {
        // 해당 id의 pw와 입력한 pw 비교
        boolean isOwner = pw != null && pw.equals(ownerAccounts.get(id));
        boolean isUser  = pw != null && pw.equals(userAccounts.get(id));
        String trimmedId = id.trim();
        String role = null;
        if (isOwner) {
            role = "OWNER"; // 관리자로 성공하였을 경우 role = OWNER
            LOGGER.info("관리자 로그인 성공: {}", trimmedId);
        } else if (isUser) {
            role = "USER"; // 사용자로 성공하였을 경우 role = USER
            LOGGER.info("사용자 로그인 성공: {}", trimmedId);
        } else {
            LOGGER.warn("로그인 실패: {} (잘못된 계정 정보)", trimmedId);
        }

        // 로그인 성공시 문자열 반환
        LoginEvent.EVENT.invoker().onLogin(trimmedId, role != null, role);

        return role;
    }
}
