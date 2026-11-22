package com.mahesh.mentee_connect.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mahesh.mentee_connect.security.jwt.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC signaling relay. Every message is routed to an explicit recipient
 * ("to" or "targets"); nothing is broadcast implicitly.
 */
@Component
public class SignalingHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SignalingHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractQueryParam(session.getUri(), "token");
        if (token == null || token.isBlank()) {
            log.warn("Signal connection rejected: missing token");
            closeQuietly(session);
            return;
        }

        String userId;
        try {
            if (!jwtUtils.validateJwtToken(token)) {
                log.warn("Signal connection rejected: invalid token");
                closeQuietly(session);
                return;
            }
            userId = jwtUtils.getUserIdFromJwtToken(token);
        } catch (Exception e) {
            log.warn("Signal connection rejected: {}", e.getMessage());
            closeQuietly(session);
            return;
        }

        if (userId == null || userId.isBlank()) {
            closeQuietly(session);
            return;
        }

        session.getAttributes().put("userId", userId);
        WebSocketSession previous = SESSIONS.put(userId, session);
        if (previous != null && previous.isOpen() && previous != session) {
            closeQuietly(previous);
        }
        log.info("Signal connected for user {}", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String senderId = (String) session.getAttributes().get("userId");
        if (senderId == null) {
            closeQuietly(session);
            return;
        }

        JsonNode json;
        try {
            json = MAPPER.readTree(message.getPayload());
        } catch (Exception e) {
            log.debug("Ignoring malformed signal from {}: {}", senderId, e.getMessage());
            return;
        }

        ObjectNode outgoing = json.isObject() ? (ObjectNode) json : MAPPER.createObjectNode().set("payload", json);
        // Never trust a client-supplied "from" — always stamp the authenticated sender.
        outgoing.put("from", senderId);

        if (json.hasNonNull("to")) {
            sendTo(json.get("to").asText(), outgoing);
            return;
        }

        if (json.has("targets") && json.get("targets").isArray()) {
            List<String> targets = MAPPER.convertValue(
                    json.get("targets"),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
            for (String target : targets) {
                if (target != null && !target.isBlank()) {
                    sendTo(target, outgoing);
                }
            }
            return;
        }

        if (json.path("broadcast").asBoolean(false)) {
            SESSIONS.forEach((id, ws) -> {
                if (!id.equals(senderId)) {
                    send(ws, outgoing);
                }
            });
            return;
        }

        log.debug("Ignoring unroutable signal '{}' from {}", json.path("type").asText("?"), senderId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            SESSIONS.remove(userId, session);
            log.info("Signal disconnected for user {}", userId);
        }
    }

    private void sendTo(String userId, JsonNode message) {
        WebSocketSession target = SESSIONS.get(userId);
        if (target == null) {
            log.debug("Signal target {} is not connected", userId);
            return;
        }
        send(target, message);
    }

    private void send(WebSocketSession session, JsonNode message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String payload = MAPPER.writeValueAsString(message);
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException e) {
            log.debug("Failed to deliver signal to {}: {}", session.getId(), e.getMessage());
        }
    }

    private String extractQueryParam(URI uri, String name) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String pair : uri.getQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(name)) {
                try {
                    return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException ignored) {
        }
    }
}
