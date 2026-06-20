package com.sliit.vehiclebiddingsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for displaying auction information on the homepage
 */
public class HomepageAuctionDto {
    private Long id;
    private String vehicleMake;
    private String vehicleModel;
    private Integer vehicleYear;
    private String vehicleImageUrl;
    private BigDecimal currentBid;
    private BigDecimal startingBid;
    private LocalDateTime endTime;
    private LocalDateTime startTime;
    private String status;
    private Integer bidCount;
    private String condition;

    // Constructors
    public HomepageAuctionDto() {}

    public HomepageAuctionDto(Long id, String vehicleMake, String vehicleModel, Integer vehicleYear, 
                             String vehicleImageUrl, BigDecimal currentBid, BigDecimal startingBid,
                             LocalDateTime endTime, LocalDateTime startTime, String status, 
                             Integer bidCount, String condition) {
        this.id = id;
        this.vehicleMake = vehicleMake;
        this.vehicleModel = vehicleModel;
        this.vehicleYear = vehicleYear;
        this.vehicleImageUrl = vehicleImageUrl;
        this.currentBid = currentBid;
        this.startingBid = startingBid;
        this.endTime = endTime;
        this.startTime = startTime;
        this.status = status;
        this.bidCount = bidCount;
        this.condition = condition;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public void setVehicleMake(String vehicleMake) {
        this.vehicleMake = vehicleMake;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public Integer getVehicleYear() {
        return vehicleYear;
    }

    public void setVehicleYear(Integer vehicleYear) {
        this.vehicleYear = vehicleYear;
    }

    public String getVehicleImageUrl() {
        return vehicleImageUrl;
    }

    public void setVehicleImageUrl(String vehicleImageUrl) {
        this.vehicleImageUrl = vehicleImageUrl;
    }

    public BigDecimal getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(BigDecimal currentBid) {
        this.currentBid = currentBid;
    }

    public BigDecimal getStartingBid() {
        return startingBid;
    }

    public void setStartingBid(BigDecimal startingBid) {
        this.startingBid = startingBid;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getBidCount() {
        return bidCount;
    }

    public void setBidCount(Integer bidCount) {
        this.bidCount = bidCount;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
