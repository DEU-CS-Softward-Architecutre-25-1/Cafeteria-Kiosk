package dev.qf.client.event;

import common.event.Event;
import common.event.EventFactory;

// 로그인 결과를 정의하고 EventFactory를 통해 배열 기반의 이벤트 구조를 생성
public interface LoginEvent {
    Event<LoginEvent> EVENT = EventFactory.createArrayBacked(
            LoginEvent.class,
            (listeners) -> (id, success, role) -> {
                for (LoginEvent listener : listeners) {
                    listener.onLogin(id, success, role);
                }
            }
    );

    // id는 입력한 id, success는 로그인 성공여부, role은 사용자인지 관리자인지 구분 여부
    void onLogin(String id, boolean success, String role);
}
