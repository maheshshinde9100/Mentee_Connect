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
        // The servlet context-path is already /api, so registering "/signal" exposes
        // the endpoint at /api/signal — matching REACT_APP_SIGNAL_URL on the client.
        registry.addHandler(signalingHandler, "/signal")
                .setAllowedOriginPatterns("*");
    }
}
