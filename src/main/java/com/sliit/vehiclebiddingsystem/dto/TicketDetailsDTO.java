package com.sliit.vehiclebiddingsystem.dto;

import java.time.LocalDateTime;

import com.sliit.vehiclebiddingsystem.entity.CustomerQuery;

public class TicketDetailsDTO {
    private Long queryId;
    private String subject;
    private String description;
    private CustomerQuery.Status status;
    private CustomerQuery.Priority priority;
    private CustomerQuery.QueryType queryType;
    private String username;
    private String assignedToUsername;
    private String resolutionNotes;
    private LocalDateTime resolvedAt;
    private Long relatedAuctionId;
    private Long relatedListingId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TicketDetailsDTO() {}

    public TicketDetailsDTO(CustomerQuery customerQuery) {
        this.queryId = customerQuery.getQueryId();
        this.subject = customerQuery.getSubject();
        this.description = customerQuery.getDescription();
        this.status = customerQuery.getStatus();
        this.priority = customerQuery.getPriority();
        this.queryType = customerQuery.getQueryType();
        this.username = customerQuery.getUser() != null ? customerQuery.getUser().getUsername() : null;
        this.assignedToUsername = customerQuery.getAssignedTo() != null ? customerQuery.getAssignedTo().getUsername() : null;
        this.resolutionNotes = customerQuery.getResolutionNotes();
        this.resolvedAt = customerQuery.getResolvedAt();
        this.relatedAuctionId = customerQuery.getRelatedAuctionId();
        this.relatedListingId = customerQuery.getRelatedListingId();
        this.createdAt = customerQuery.getCreatedAt();
        this.updatedAt = customerQuery.getUpdatedAt();
    }

    // Getters and Setters
    public Long getQueryId() { return queryId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CustomerQuery.Status getStatus() { return status; }
    public void setStatus(CustomerQuery.Status status) { this.status = status; }

    public CustomerQuery.Priority getPriority() { return priority; }
    public void setPriority(CustomerQuery.Priority priority) { this.priority = priority; }

    public CustomerQuery.QueryType getQueryType() { return queryType; }
    public void setQueryType(CustomerQuery.QueryType queryType) { this.queryType = queryType; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAssignedToUsername() { return assignedToUsername; }
    public void setAssignedToUsername(String assignedToUsername) { this.assignedToUsername = assignedToUsername; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public Long getRelatedAuctionId() { return relatedAuctionId; }
    public void setRelatedAuctionId(Long relatedAuctionId) { this.relatedAuctionId = relatedAuctionId; }

    public Long getRelatedListingId() { return relatedListingId; }
    public void setRelatedListingId(Long relatedListingId) { this.relatedListingId = relatedListingId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


