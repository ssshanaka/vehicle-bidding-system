package com.sliit.vehiclebiddingsystem.dto;

/**
 * DTO for homepage statistics
 */
public class HomepageStatsDto {
    private long totalListings;
    private long activeAuctions;
    private long totalUsers;
    private long completedAuctions;

    // Constructors
    public HomepageStatsDto() {}

    public HomepageStatsDto(long totalListings, long activeAuctions, long totalUsers, long completedAuctions) {
        this.totalListings = totalListings;
        this.activeAuctions = activeAuctions;
        this.totalUsers = totalUsers;
        this.completedAuctions = completedAuctions;
    }

    // Getters and Setters
    public long getTotalListings() {
        return totalListings;
    }

    public void setTotalListings(long totalListings) {
        this.totalListings = totalListings;
    }

    public long getActiveAuctions() {
        return activeAuctions;
    }

    public void setActiveAuctions(long activeAuctions) {
        this.activeAuctions = activeAuctions;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getCompletedAuctions() {
        return completedAuctions;
    }

    public void setCompletedAuctions(long completedAuctions) {
        this.completedAuctions = completedAuctions;
    }
}
