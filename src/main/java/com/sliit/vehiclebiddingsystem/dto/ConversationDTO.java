package com.sliit.vehiclebiddingsystem.dto;

import java.time.LocalDateTime;

import com.sliit.vehiclebiddingsystem.entity.TicketConversation;

public class ConversationDTO {
    private Long conversationId;
    private String username;
    private String message;
    private TicketConversation.MessageType messageType;
    private LocalDateTime createdAt;

    public ConversationDTO() {}

    public ConversationDTO(TicketConversation conversation) {
        this.conversationId = conversation.getConversationId();
        this.username = conversation.getUser() != null ? conversation.getUser().getUsername() : null;
        this.message = conversation.getMessage();
        this.messageType = conversation.getMessageType();
        this.createdAt = conversation.getCreatedAt();
    }

    // Getters and Setters
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public TicketConversation.MessageType getMessageType() { return messageType; }
    public void setMessageType(TicketConversation.MessageType messageType) { this.messageType = messageType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}


