package com.sliit.vehiclebiddingsystem.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sliit.vehiclebiddingsystem.entity.Report;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.service.PdfGenerationService;
import com.sliit.vehiclebiddingsystem.service.ReportService;

@Controller
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @GetMapping("/reports")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('CUSTOMER_SERVICE')")
    public String getAllReports(Model model, 
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = reportService.getAllReportsPaginated(pageable);

        model.addAttribute("reports", reports);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reports.getTotalPages());

        return "auction-reports";
    }

    @GetMapping("/reports/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('CUSTOMER_SERVICE')")
    public String getMyReports(Model model, 
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = reportService.getReportsByUserPaginated(user.getUserId(), pageable);

        model.addAttribute("reports", reports);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reports.getTotalPages());

        return "auction-reports";
    }

    @GetMapping("/reports/auction/{auctionId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('CUSTOMER_SERVICE')")
    public String getReportByAuction(@PathVariable Long auctionId, Model model) {
        Report report = reportService.getReportByAuctionId(auctionId);
        
        if (report == null) {
            model.addAttribute("auctionId", auctionId);
            model.addAttribute("reportExists", false);
            return "auction-report-details";
        }

        model.addAttribute("report", report);
        model.addAttribute("reportExists", true);

        return "auction-report-details";
    }

    @PostMapping("/reports/auction/{auctionId}/generate")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('CUSTOMER_SERVICE')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateReport(@PathVariable Long auctionId) {
        try {
            Report report = reportService.generateReport(auctionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reportId", report.getReportId());
            response.put("message", "Report generated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/reports/{reportId}/download")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('CUSTOMER_SERVICE')")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long reportId) {
        try {
            Report report = reportService.getReportById(reportId);
            if (report == null) {
                return ResponseEntity.notFound().build();
            }

            // Mark as downloaded
            reportService.markAsDownloaded(reportId);

            // Generate REAL PDF using the existing PdfGenerationService
            byte[] pdfBytes = pdfGenerationService.generateReportPdf(reportId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                    "auction-report-" + report.getAuction().getAuctionId() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/reports/{reportId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('CUSTOMER_SERVICE')")
    public String getReportDetails(@PathVariable Long reportId, Model model) {
        Report report = reportService.getReportById(reportId);
        if (report == null) {
            return "redirect:/reports";
        }

        model.addAttribute("report", report);
        model.addAttribute("reportExists", true);
        return "auction-report-details";
    }

    @GetMapping("/winner-confirmation/{auctionId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('CUSTOMER_SERVICE')")
    public String getWinnerConfirmation(@PathVariable Long auctionId, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        Report report = reportService.getReportByAuctionId(auctionId);
        if (report == null) {
            // Generate report if it doesn't exist
            report = reportService.generateReport(auctionId);
        }

        // Check if user is winner or seller
        boolean isWinner = report.getAuction().getWinner() != null && 
                          report.getAuction().getWinner().getUserId().equals(user.getUserId());
        boolean isSeller = report.getAuction().getListing().getSeller().getUserId().equals(user.getUserId());

        if (!isWinner && !isSeller) {
            return "redirect:/dashboard";
        }

        model.addAttribute("report", report);
        model.addAttribute("isWinner", isWinner);
        model.addAttribute("isSeller", isSeller);

        return "winner-confirmation";
    }
}
