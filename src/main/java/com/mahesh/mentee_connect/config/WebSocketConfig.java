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
        // Register at /api/signal to work within the context-path=/api
        // This ensures WebSocket connections work properly on Render and other platforms
        registry.addHandler(signalingHandler, "/api/signal")
                .setAllowedOrigins("http://localhost:3000", "https://menteeconnect.vercel.app")
                .setAllowedOriginPatterns("*");
    }
}
