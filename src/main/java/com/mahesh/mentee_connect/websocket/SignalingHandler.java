package com.mahesh.mentee_connect.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Lightweight signaling handler for WebRTC calls.
 * Routes messages based on "to" or "targets" fields so mentors can invite specific mentees.
 */
@Component
public class SignalingHandler extends TextWebSocketHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // userId -> session
    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // System.out.println("WS: Connection attempt from URI: " + session.getUri());
        String userId = extractUserId(session.getUri());
        if (userId != null && !userId.isEmpty()) {
            SESSIONS.put(userId, session);
            session.getAttributes().put("userId", userId);
            // System.out.println("WS connected: " + userId + " (Total sessions: " + SESSIONS.size() + ")");
        } else {
            // System.out.println("WS connection rejected: missing or empty userId");
            try {
                session.close();
            } catch (IOException e) {
                System.err.println("Error closing session: " + e.getMessage());
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = MAPPER.readTree(message.getPayload());
        String senderId = (String) session.getAttributes().get("userId");

        // Route directly to "to"
        if (json.hasNonNull("to")) {
            String target = json.get("to").asText();
            sendTo(target, json);
            return;
        }

        // Route to list of targets
        if (json.has("targets") && json.get("targets").isArray()) {
            List<String> targets = MAPPER.convertValue(json.get("targets"), MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
            for (String t : targets) {
                sendTo(t, json);
            }
            return;
        }

        // Broadcast to everyone else
        for (Map.Entry<String, WebSocketSession> entry : SESSIONS.entrySet()) {
            if (!entry.getKey().equals(senderId)) {
                send(entry.getValue(), json);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            SESSIONS.remove(userId);
            // System.out.println("WS disconnected: " + userId);
        }
    }

    private void sendTo(String userId, JsonNode message) {
        WebSocketSession target = SESSIONS.get(userId);
        if (target != null && target.isOpen()) {
            send(target, message);
        }
    }

    private void send(WebSocketSession session, JsonNode message) {
        try {
            session.sendMessage(new TextMessage(MAPPER.writeValueAsString(message)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractUserId(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            // System.out.println("WS: URI or query is null");
            return null;
        }
        try {
            Map<String, String> params = List.of(uri.getQuery().split("&")).stream()
                    .map(p -> p.split("=", 2))
                    .filter(arr -> arr.length == 2)
                    .collect(Collectors.toMap(
                            arr -> arr[0], 
                            arr -> {
                                try {
                                    return java.net.URLDecoder.decode(arr[1], "UTF-8");
                                } catch (Exception e) {
                                    return arr[1];
                                }
                            }
                    ));
            String userId = params.get("userId");
            // System.out.println("WS: Extracted userId from query: " + userId);
            return userId;
        } catch (Exception e) {
            System.err.println("WS: Error extracting userId: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

