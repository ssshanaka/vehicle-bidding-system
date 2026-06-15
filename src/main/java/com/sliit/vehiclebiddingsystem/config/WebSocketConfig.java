package com.sliit.vehiclebiddingsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket configuration for real-time auction updates.
 * Enables STOMP over WebSocket for client subscriptions.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");  // Broker for subscriptions
        config.setApplicationDestinationPrefixes("/app");  // Prefix for client messages
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Configure CORS properly in production
                .withSockJS()
                .setStreamBytesLimit(512 * 1024)  // 512KB
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000);  // 30 seconds
    }

    @Override
    public void configureWebSocketTransport(@NonNull WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(128 * 1024);  // 128KB
        registration.setSendBufferSizeLimit(512 * 1024);  // 512KB
        registration.setSendTimeLimit(20 * 1000);  // 20 seconds
    }
}
