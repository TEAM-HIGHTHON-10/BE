package com.example.tamagotchi_server.service;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages WebSocket sessions keyed by userId.
 * A user may have multiple active sessions (e.g., multiple browser tabs).
 * Messages are only sent to the specific user — never broadcast.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionService {

    private final ObjectMapper objectMapper;

    /** Map of userId -> set of active WebSocket sessions */
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void registerSession(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("WebSocket session registered for user {}", userId);
    }

    public void removeSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) {
                sessions.remove(userId);
            }
        }
        log.info("WebSocket session removed for user {}", userId);
    }

    /**
     * Send any serializable message to all sessions of a given user.
     * Per-user isolation: only sessions belonging to this userId receive the message.
     */
    public void sendMessage(Long userId, Object message) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            log.debug("No active WebSocket sessions for user {}", userId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);

            for (WebSocketSession session : userSessions) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (IOException e) {
            log.error("Failed to send WebSocket message to user {}", userId, e);
        }
    }
}
