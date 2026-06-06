package com.sliit.vehiclebiddingsystem.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.sliit.vehiclebiddingsystem.entity.Report;

@Service
public class PdfGenerationService {

    @Autowired
    private ReportService reportService;

    public byte[] generateReportPdf(Long reportId) {
        try {
            Report report = reportService.getReportById(reportId);
            if (report == null) {
                throw new RuntimeException("Report not found");
            }

            String htmlContent = generateReportHtml(report);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            HtmlConverter.convertToPdf(htmlContent, outputStream);
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private String generateReportHtml(Report report) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Auction Report #").append(report.getReportId()).append("</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append("h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }");
        html.append("h2 { color: #34495e; margin-top: 30px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; font-weight: bold; }");
        html.append(".section { margin: 20px 0; }");
        html.append(".status-approved { color: #27ae60; font-weight: bold; }");
        html.append(".status-pending { color: #f39c12; font-weight: bold; }");
        html.append(".status-generated { color: #3498db; font-weight: bold; }");
        html.append(".bid-history { background-color: #f8f9fa; padding: 15px; border-radius: 5px; white-space: pre-wrap; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // Header
        html.append("<h1>Auction Report #").append(report.getReportId()).append("</h1>");
        html.append("<p><strong>Generated:</strong> ").append(report.getGeneratedAt().format(formatter)).append("</p>");
        html.append("<p><strong>Status:</strong> <span class='status-").append(report.getStatus().name().toLowerCase()).append("'>").append(report.getStatus().name()).append("</span></p>");

        // Vehicle Information
        html.append("<h2>Vehicle Information</h2>");
        html.append("<table>");
        html.append("<tr><th>Make</th><td>").append(report.getAuction().getListing().getMake()).append("</td></tr>");
        html.append("<tr><th>Model</th><td>").append(report.getAuction().getListing().getModel()).append("</td></tr>");
        html.append("<tr><th>Year</th><td>").append(report.getAuction().getListing().getYear()).append("</td></tr>");
        html.append("<tr><th>Condition</th><td>").append(getVehicleCondition(report)).append("</td></tr>");
        html.append("<tr><th>Fuel Type</th><td>").append(getVehicleFuelType(report)).append("</td></tr>");
        html.append("<tr><th>Transmission</th><td>").append(getVehicleTransmission(report)).append("</td></tr>");
        html.append("<tr><th>Mileage</th><td>").append(getVehicleMileage(report)).append(" miles</td></tr>");
        if (report.getAuction().getListing().getDescription() != null) {
            html.append("<tr><th>Description</th><td>").append(report.getAuction().getListing().getDescription()).append("</td></tr>");
        }
        html.append("</table>");

        // Auction Summary
        html.append("<h2>Auction Summary</h2>");
        html.append("<table>");
        html.append("<tr><th>Auction ID</th><td>").append(report.getAuction().getAuctionId()).append("</td></tr>");
        html.append("<tr><th>Start Time</th><td>").append(report.getAuction().getStartTime().format(formatter)).append("</td></tr>");
        html.append("<tr><th>End Time</th><td>").append(report.getAuction().getEndTime().format(formatter)).append("</td></tr>");
        html.append("<tr><th>Duration</th><td>").append(getDuration(report)).append("</td></tr>");
        html.append("<tr><th>Total Bids</th><td>").append(report.getTotalBids()).append("</td></tr>");
        html.append("<tr><th>Final Bid</th><td>$").append(String.format("%.2f", report.getHighestBidAmount() != null ? report.getHighestBidAmount() : 0.0)).append("</td></tr>");
        html.append("<tr><th>Winner</th><td>").append(report.getWinnerUsername() != null ? report.getWinnerUsername() : "No winner").append("</td></tr>");
        html.append("</table>");

        // Contact Information
        html.append("<h2>Contact Information</h2>");
        html.append("<table>");
        html.append("<tr><th colspan='2'>Winner</th></tr>");
        html.append("<tr><th>Username</th><td>").append(report.getWinnerUsername() != null ? report.getWinnerUsername() : "N/A").append("</td></tr>");
        html.append("<tr><th>Email</th><td>").append(getWinnerEmail(report)).append("</td></tr>");
        html.append("<tr><th>Phone</th><td>").append(getWinnerPhone(report)).append("</td></tr>");
        html.append("<tr><th colspan='2'>Seller</th></tr>");
        html.append("<tr><th>Username</th><td>").append(report.getSellerUsername()).append("</td></tr>");
        html.append("<tr><th>Email</th><td>").append(getSellerEmail(report)).append("</td></tr>");
        html.append("<tr><th>Phone</th><td>").append(getSellerPhone(report)).append("</td></tr>");
        html.append("</table>");

        // Bid History
        if (report.getBidHistory() != null && !report.getBidHistory().trim().isEmpty()) {
            html.append("<h2>Bid History</h2>");
            html.append("<div class='bid-history'>").append(report.getBidHistory()).append("</div>");
        }

        // CSE Notes
        if (report.getCseNotes() != null && !report.getCseNotes().trim().isEmpty()) {
            html.append("<h2>CSE Notes</h2>");
            html.append("<p>").append(report.getCseNotes()).append("</p>");
        }

        // Review Information
        if (report.getReviewedBy() != null) {
            html.append("<h2>Review Information</h2>");
            html.append("<table>");
            html.append("<tr><th>Reviewed By</th><td>").append(report.getReviewedBy()).append("</td></tr>");
            html.append("<tr><th>Reviewed At</th><td>").append(report.getReviewedAt() != null ? report.getReviewedAt().format(formatter) : "N/A").append("</td></tr>");
            html.append("<tr><th>Approved</th><td>").append(report.getIsApproved() != null && report.getIsApproved() ? "Yes" : "No").append("</td></tr>");
            html.append("</table>");
        }

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private String getVehicleCondition(Report report) {
        if (report.getVehicleCondition() != null) {
            return report.getVehicleCondition();
        }
        return report.getAuction().getListing().getCondition().name();
    }

    private String getVehicleFuelType(Report report) {
        if (report.getVehicleFuelType() != null) {
            return report.getVehicleFuelType();
        }
        return report.getAuction().getListing().getFuelType() != null ? report.getAuction().getListing().getFuelType() : "N/A";
    }

    private String getVehicleTransmission(Report report) {
        if (report.getVehicleTransmission() != null) {
            return report.getVehicleTransmission();
        }
        return report.getAuction().getListing().getTransmission() != null ? report.getAuction().getListing().getTransmission() : "N/A";
    }

    private String getVehicleMileage(Report report) {
        if (report.getVehicleMileage() != null) {
            return report.getVehicleMileage().toString();
        }
        return report.getAuction().getListing().getMileage() != null ? report.getAuction().getListing().getMileage().toString() : "N/A";
    }

    private String getWinnerEmail(Report report) {
        if (report.getWinnerEmail() != null) {
            return report.getWinnerEmail();
        }
        if (report.getAuction().getWinner() != null && report.getAuction().getWinner().getEmail() != null) {
            return report.getAuction().getWinner().getEmail();
        }
        return "N/A";
    }

    private String getWinnerPhone(Report report) {
        if (report.getWinnerPhone() != null) {
            return report.getWinnerPhone();
        }
        if (report.getAuction().getWinner() != null && report.getAuction().getWinner().getPhone() != null) {
            return report.getAuction().getWinner().getPhone();
        }
        return "N/A";
    }

    private String getSellerEmail(Report report) {
        if (report.getSellerEmail() != null) {
            return report.getSellerEmail();
        }
        return report.getAuction().getListing().getSeller().getEmail() != null ? report.getAuction().getListing().getSeller().getEmail() : "N/A";
    }

    private String getSellerPhone(Report report) {
        if (report.getSellerPhone() != null) {
            return report.getSellerPhone();
        }
        return report.getAuction().getListing().getSeller().getPhone() != null ? report.getAuction().getListing().getSeller().getPhone() : "N/A";
    }

    private String getDuration(Report report) {
        if (report.getAuctionDurationMinutes() != null) {
            long hours = report.getAuctionDurationMinutes() / 60;
            long minutes = report.getAuctionDurationMinutes() % 60;
            return hours + " hours " + minutes + " minutes";
        }
        return "N/A";
    }
}

