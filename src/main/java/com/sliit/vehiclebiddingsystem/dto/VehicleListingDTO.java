package com.sliit.vehiclebiddingsystem.dto;

import com.sliit.vehiclebiddingsystem.entity.VehicleListing;

public class VehicleListingDTO {
    private Long listingId;
    private String make;
    private String model;
    private Integer year;
    private Integer mileage;
    private VehicleListing.Condition condition;
    private VehicleListing.Status status;
    private String fuelType;
    private String transmission;
    private String description;
    private String rejectionReason;
    private String inspectorNotes;
    private String sellerUsername;
    private Long sellerId;
    private java.util.List<ImageInfo> images;

    // Default constructor
    public VehicleListingDTO() {}

    // Constructor from entity
    public VehicleListingDTO(VehicleListing listing) {
        this.listingId = listing.getListingId();
        this.make = listing.getMake();
        this.model = listing.getModel();
        this.year = listing.getYear();
        this.mileage = listing.getMileage();
        this.condition = listing.getCondition();
        this.status = listing.getStatus();
        this.fuelType = listing.getFuelType();
        this.transmission = listing.getTransmission();
        this.description = listing.getDescription();
        this.rejectionReason = listing.getRejectionReason();
        this.inspectorNotes = listing.getInspectorNotes();
        
        // Safely get seller info without triggering lazy loading
        if (listing.getSeller() != null) {
            this.sellerId = listing.getSeller().getUserId();
            this.sellerUsername = listing.getSeller().getUsername();
        }
        
        // Safely get images without triggering lazy loading
        if (listing.getImages() != null && !listing.getImages().isEmpty()) {
            this.images = listing.getImages().stream()
                .map(image -> new ImageInfo(image.getImageId(), image.getImageUrl()))
                .collect(java.util.stream.Collectors.toList());
        } else {
            this.images = new java.util.ArrayList<>();
        }
    }

    // Getters and setters
    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMileage() {
        return mileage;
    }

    public void setMileage(Integer mileage) {
        this.mileage = mileage;
    }

    public VehicleListing.Condition getCondition() {
        return condition;
    }

    public void setCondition(VehicleListing.Condition condition) {
        this.condition = condition;
    }

    public VehicleListing.Status getStatus() {
        return status;
    }

    public void setStatus(VehicleListing.Status status) {
        this.status = status;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getInspectorNotes() {
        return inspectorNotes;
    }

    public void setInspectorNotes(String inspectorNotes) {
        this.inspectorNotes = inspectorNotes;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public java.util.List<ImageInfo> getImages() {
        return images;
    }

    public void setImages(java.util.List<ImageInfo> images) {
        this.images = images;
    }

    // Inner class for image information
    public static class ImageInfo {
        private Long imageId;
        private String imageUrl;

        public ImageInfo() {}

        public ImageInfo(Long imageId, String imageUrl) {
            this.imageId = imageId;
            this.imageUrl = imageUrl;
        }

        public Long getImageId() {
            return imageId;
        }

        public void setImageId(Long imageId) {
            this.imageId = imageId;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }
}