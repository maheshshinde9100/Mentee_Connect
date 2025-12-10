package com.mahesh.mentee_connect.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.mahesh.mentee_connect.websocket.SignalingHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SignalingHandler signalingHandler;

    public WebSocketConfig(SignalingHandler signalingHandler) {
        this.signalingHandler = signalingHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Spring WebSocket endpoints registered via WebSocketConfigurer are at ROOT level
        // They are NOT affected by server.servlet.context-path=/api
        // Register at /signal - accessible at ws://localhost:8080/signal
        // Also register at /api/signal for compatibility
        registry.addHandler(signalingHandler, "/signal")
                .setAllowedOrigins("http://localhost:3000", "https://menteeconnect.vercel.app")
                .setAllowedOriginPatterns("*");
        registry.addHandler(signalingHandler, "/api/signal")
                .setAllowedOrigins("http://localhost:3000", "https://menteeconnect.vercel.app")
                .setAllowedOriginPatterns("*");
    }
}
