package com.example.tamagotchi_server.config.handler;

import com.example.tamagotchi_server.security.JwtProvider;
import com.example.tamagotchi_server.service.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket handler that authenticates connections via JWT token in query parameter.
 * Connection URL: ws://server/ws?token=<jwt>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthWebSocketHandler extends TextWebSocketHandler {

    private final JwtProvider jwtProvider;
    private final WebSocketSessionService sessionService;

    private static final String USER_ID_ATTR = "userId";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractAndValidateToken(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        session.getAttributes().put(USER_ID_ATTR, userId);
        sessionService.registerSession(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Client messages are not expected; this is a server-push channel
        log.debug("Received message from client (ignored): {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(USER_ID_ATTR);
        if (userId != null) {
            sessionService.removeSession(userId, session);
        }
    }

    private Long extractAndValidateToken(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;

        Map<String, String> params = UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .toSingleValueMap();

        String token = params.get("token");
        if (token == null || !jwtProvider.validateToken(token)) {
            log.warn("WebSocket connection rejected: invalid or missing JWT");
            return null;
        }

        return jwtProvider.getUserIdFromToken(token);
    }
}
