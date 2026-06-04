package com.sliit.vehiclebiddingsystem.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Bid;
import com.sliit.vehiclebiddingsystem.entity.Report;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.BidRepository;
import com.sliit.vehiclebiddingsystem.repository.ReportRepository;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    public Report getReportByAuctionId(Long auctionId) {
        return reportRepository.findByAuctionAuctionId(auctionId)
                .orElse(null);
    }

    public Report generateReport(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        if (!auction.getStatus().name().equals("CLOSED")) {
            throw new RuntimeException("Report can only be generated for closed auctions");
        }

        // Check if report already exists
        Report existingReport = getReportByAuctionId(auctionId);
        if (existingReport != null) {
            return existingReport;
        }

        // Generate new report
        Report report = new Report();
        report.setAuction(auction);
        report.setGeneratedAt(LocalDateTime.now());
        report.setRetentionEnd(LocalDateTime.now().plusDays(30)); // 30 days retention

        // Get bid data
        List<Bid> bids = bidRepository.findByAuctionAuctionIdOrderByAmountDesc(auctionId);
        
        // Set basic statistics
        report.setTotalBids(bids.size());
        report.setHighestBidAmount(auction.getHighestBid());
        report.setWinnerUsername(auction.getWinner() != null ? auction.getWinner().getUsername() : "No winner");
        report.setSellerUsername(auction.getListing().getSeller().getUsername());

        // Calculate auction duration
        Duration duration = Duration.between(auction.getStartTime(), auction.getEndTime());
        report.setAuctionDurationMinutes(duration.toMinutes());

        // Generate bid summary
        String bidSummary = generateBidSummary(bids);
        report.setBidSummary(bidSummary);

        // Generate participant list
        String participantList = generateParticipantList(bids);
        report.setParticipantList(participantList);

        // Generate vehicle details
        String vehicleDetails = generateVehicleDetails(auction);
        report.setVehicleDetails(vehicleDetails);

        // Generate timeline
        String timeline = generateTimeline(auction);
        report.setTimeline(timeline);

        // Generate contact details
        String contactDetails = generateContactDetails(auction);
        report.setContactDetails(contactDetails);

        // Enhanced detailed information
        report.setBidHistory(generateDetailedBidHistory(bids));
        report.setVehicleCondition(auction.getListing().getCondition().name());
        report.setVehicleFuelType(auction.getListing().getFuelType());
        report.setVehicleTransmission(auction.getListing().getTransmission());
        report.setVehicleMileage(auction.getListing().getMileage());
        
        // Contact information
        if (auction.getWinner() != null) {
            report.setWinnerEmail(auction.getWinner().getEmail());
            report.setWinnerPhone(auction.getWinner().getPhone());
        }
        report.setSellerEmail(auction.getListing().getSeller().getEmail());
        report.setSellerPhone(auction.getListing().getSeller().getPhone());
        
        // Set initial status for review
        report.setStatus(Report.Status.UNDER_REVIEW);

        return reportRepository.save(report);
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public Page<Report> getAllReportsPaginated(Pageable pageable) {
        return reportRepository.findAll(pageable);
    }

    public List<Report> getReportsByUser(Long userId) {
        return reportRepository.findByAuctionListingSellerUserId(userId);
    }

    public Page<Report> getReportsByUserPaginated(Long userId, Pageable pageable) {
        return reportRepository.findBySellerOrderByGeneratedAtDesc(userId, pageable);
    }

    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElse(null);
    }

    public Report markAsDownloaded(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus(Report.Status.DOWNLOADED);
        return reportRepository.save(report);
    }

    public void cleanupExpiredReports() {
        List<Report> expiredReports = reportRepository.findByRetentionEndBeforeAndStatusNot(
                LocalDateTime.now(), Report.Status.EXPIRED);
        
        for (Report report : expiredReports) {
            report.setStatus(Report.Status.EXPIRED);
        }
        
        reportRepository.saveAll(expiredReports);
    }

    private String generateBidSummary(List<Bid> bids) {
        if (bids.isEmpty()) {
            return "No bids placed";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Bid History:\n");
        
        for (int i = 0; i < bids.size(); i++) {
            Bid bid = bids.get(i);
            summary.append(String.format("%d. %s - $%.2f (%s)\n", 
                    i + 1, 
                    bid.getBidder().getUsername(), 
                    bid.getAmount(),
                    bid.getTimestamp()));
        }

        return summary.toString();
    }

    private String generateParticipantList(List<Bid> bids) {
        if (bids.isEmpty()) {
            return "No participants";
        }

        List<User> participants = bids.stream()
                .map(Bid::getBidder)
                .distinct()
                .collect(Collectors.toList());

        StringBuilder participantList = new StringBuilder();
        participantList.append("Participants:\n");
        
        for (User participant : participants) {
            participantList.append(String.format("- %s (%s)\n", 
                    participant.getUsername(), 
                    participant.getEmail()));
        }

        return participantList.toString();
    }

    private String generateVehicleDetails(Auction auction) {
        StringBuilder details = new StringBuilder();
        details.append("Vehicle Details:\n");
        details.append(String.format("Make: %s\n", auction.getListing().getMake()));
        details.append(String.format("Model: %s\n", auction.getListing().getModel()));
        details.append(String.format("Year: %d\n", auction.getListing().getYear()));
        details.append(String.format("Mileage: %d\n", auction.getListing().getMileage()));
        details.append(String.format("Condition: %s\n", auction.getListing().getCondition()));
        details.append(String.format("Transmission: %s\n", auction.getListing().getTransmission()));
        
        if (auction.getListing().getDescription() != null) {
            details.append(String.format("Description: %s\n", auction.getListing().getDescription()));
        }

        return details.toString();
    }

    private String generateTimeline(Auction auction) {
        StringBuilder timeline = new StringBuilder();
        timeline.append("Auction Timeline:\n");
        timeline.append(String.format("Start Time: %s\n", auction.getStartTime()));
        timeline.append(String.format("End Time: %s\n", auction.getEndTime()));
        timeline.append(String.format("Duration: %d minutes\n", 
                Duration.between(auction.getStartTime(), auction.getEndTime()).toMinutes()));
        
        if (auction.getCurrentEndTime() != null && !auction.getCurrentEndTime().equals(auction.getEndTime())) {
            timeline.append(String.format("Extended End Time: %s\n", auction.getCurrentEndTime()));
        }

        return timeline.toString();
    }

    private String generateContactDetails(Auction auction) {
        StringBuilder contacts = new StringBuilder();
        contacts.append("Contact Details:\n");
        
        if (auction.getWinner() != null) {
            contacts.append("Winner:\n");
            contacts.append(String.format("Username: %s\n", auction.getWinner().getUsername()));
            contacts.append(String.format("Email: %s\n", auction.getWinner().getEmail()));
            contacts.append(String.format("Phone: %s\n", auction.getWinner().getPhone()));
        } else {
            contacts.append("Winner: No winner\n");
        }

        contacts.append("\nSeller:\n");
        contacts.append(String.format("Username: %s\n", auction.getListing().getSeller().getUsername()));
        contacts.append(String.format("Email: %s\n", auction.getListing().getSeller().getEmail()));
        contacts.append(String.format("Phone: %s\n", auction.getListing().getSeller().getPhone()));

        return contacts.toString();
    }

    private String generateDetailedBidHistory(List<Bid> bids) {
        StringBuilder history = new StringBuilder();
        history.append("Detailed Bid History:\n");
        history.append("===================\n\n");
        
        if (bids.isEmpty()) {
            history.append("No bids were placed in this auction.\n");
            return history.toString();
        }
        
        // Sort bids by timestamp (chronological order)
        bids.sort((b1, b2) -> b1.getTimestamp().compareTo(b2.getTimestamp()));
        
        int bidNumber = 1;
        for (Bid bid : bids) {
            history.append(String.format("Bid #%d:\n", bidNumber));
            history.append(String.format("  Bidder: %s\n", bid.getBidder().getUsername()));
            history.append(String.format("  Amount: $%.2f\n", bid.getAmount()));
            history.append(String.format("  Time: %s\n", bid.getTimestamp()));
            history.append(String.format("  Email: %s\n", bid.getBidder().getEmail()));
            history.append(String.format("  Phone: %s\n", bid.getBidder().getPhone()));
            history.append("\n");
            bidNumber++;
        }
        
        // Add summary statistics
        history.append("Bid Summary:\n");
        history.append("============\n");
        history.append(String.format("Total Bids: %d\n", bids.size()));
        history.append(String.format("Highest Bid: $%.2f\n", bids.get(0).getAmount()));
        history.append(String.format("Lowest Bid: $%.2f\n", bids.get(bids.size() - 1).getAmount()));
        
        // Calculate average bid
        double averageBid = bids.stream().mapToDouble(Bid::getAmount).average().orElse(0.0);
        history.append(String.format("Average Bid: $%.2f\n", averageBid));
        
        // Count unique bidders
        long uniqueBidders = bids.stream().map(bid -> bid.getBidder().getUserId()).distinct().count();
        history.append(String.format("Unique Bidders: %d\n", uniqueBidders));
        
        return history.toString();
    }

    public Report reviewReport(Long reportId, String reviewedBy, String cseNotes) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        
        report.setReviewedBy(reviewedBy);
        report.setReviewedAt(LocalDateTime.now());
        report.setCseNotes(cseNotes);
        
        return reportRepository.save(report);
    }

    public Report approveReport(Long reportId, String approvedBy) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        
        report.setStatus(Report.Status.APPROVED);
        report.setIsApproved(true);
        report.setReviewedBy(approvedBy);
        report.setReviewedAt(LocalDateTime.now());
        
        return reportRepository.save(report);
    }

    public Report updateReportNotes(Long reportId, String cseNotes) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        
        report.setCseNotes(cseNotes);
        
        return reportRepository.save(report);
    }
}
