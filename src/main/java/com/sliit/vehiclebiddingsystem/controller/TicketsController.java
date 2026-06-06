package com.sliit.vehiclebiddingsystem.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sliit.vehiclebiddingsystem.dto.ConversationDTO;
import com.sliit.vehiclebiddingsystem.dto.TicketDetailsDTO;
import com.sliit.vehiclebiddingsystem.dto.UserProfileDTO;
import com.sliit.vehiclebiddingsystem.entity.CustomerQuery;
import com.sliit.vehiclebiddingsystem.entity.TicketConversation;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.CustomerQueryRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.service.CustomerQueryService;
import com.sliit.vehiclebiddingsystem.service.TicketConversationService;

@Controller
@RequestMapping("/tickets")
public class TicketsController {

    @Autowired
    private CustomerQueryRepository customerQueryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerQueryService customerQueryService;

    @Autowired
    private TicketConversationService ticketConversationService;

    @GetMapping
    public String viewTickets(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username);

        if (currentUser == null) {
            return "redirect:/login";
        }

        // Get user's tickets
        List<CustomerQuery> userTickets = customerQueryRepository.findByUserUserIdOrderByCreatedAtDesc(currentUser.getUserId());

        // Create UserProfileDTO for header
        UserProfileDTO userProfile = new UserProfileDTO(
            currentUser.getUsername(),
            currentUser.getEmail(),
            currentUser.getPhone(),
            currentUser.getRole(),
            currentUser.getProfilePicture()
        );

        model.addAttribute("pageTitle", "My Tickets - Lanka Auto Traders");
        model.addAttribute("currentUser", userProfile);
        model.addAttribute("tickets", userTickets);
        model.addAttribute("ticketCount", userTickets.size());

        return "tickets";
    }

    @GetMapping("/{ticketId}")
    public String viewTicketDetails(@PathVariable Long ticketId, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username);

        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<CustomerQuery> ticketOpt = customerQueryRepository.findById(ticketId);
        if (!ticketOpt.isPresent()) {
            return "redirect:/tickets?error=ticket_not_found";
        }

        CustomerQuery ticket = ticketOpt.get();
        
        // Verify the ticket belongs to the current user
        if (!ticket.getUser().getUserId().equals(currentUser.getUserId())) {
            return "redirect:/tickets?error=access_denied";
        }

        // Get conversation history
        List<TicketConversation> conversations = ticketConversationService.getConversationsByQueryId(ticketId);
        
        // Convert to DTOs
        TicketDetailsDTO ticketDTO = new TicketDetailsDTO(ticket);
        List<ConversationDTO> conversationDTOs = conversations.stream()
                .map(ConversationDTO::new)
                .collect(java.util.stream.Collectors.toList());

        // Create UserProfileDTO for header
        UserProfileDTO userProfile = new UserProfileDTO(
            currentUser.getUsername(),
            currentUser.getEmail(),
            currentUser.getPhone(),
            currentUser.getRole(),
            currentUser.getProfilePicture()
        );

        model.addAttribute("pageTitle", "Ticket #" + ticketId + " - Lanka Auto Traders");
        model.addAttribute("currentUser", userProfile);
        model.addAttribute("ticket", ticketDTO);
        model.addAttribute("conversations", conversationDTOs);

        return "ticket-details";
    }

    @PostMapping("/{ticketId}/reply")
    public String replyToTicket(
            @PathVariable Long ticketId,
            @RequestParam String message,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "User not found. Please log in again.");
                return "redirect:/tickets/" + ticketId;
            }

            Optional<CustomerQuery> ticketOpt = customerQueryRepository.findById(ticketId);
            if (!ticketOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Ticket not found.");
                return "redirect:/tickets";
            }

            CustomerQuery ticket = ticketOpt.get();
            
            // Verify the ticket belongs to the current user
            if (!ticket.getUser().getUserId().equals(currentUser.getUserId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You can only reply to your own tickets.");
                return "redirect:/tickets";
            }

            // Add user message to conversation
            ticketConversationService.addUserMessage(ticket, currentUser, message);

            // Update ticket status to IN_PROGRESS if it was OPEN
            if (ticket.getStatus() == CustomerQuery.Status.OPEN) {
                ticket.setStatus(CustomerQuery.Status.IN_PROGRESS);
                customerQueryRepository.save(ticket);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Your message has been sent successfully!");
            return "redirect:/tickets/" + ticketId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while sending your message. Please try again.");
            return "redirect:/tickets/" + ticketId;
        }
    }

    @PostMapping("/{ticketId}/close")
    public String closeTicket(
            @PathVariable Long ticketId,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "User not found. Please log in again.");
                return "redirect:/tickets/" + ticketId;
            }

            Optional<CustomerQuery> ticketOpt = customerQueryRepository.findById(ticketId);
            if (!ticketOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Ticket not found.");
                return "redirect:/tickets";
            }

            CustomerQuery ticket = ticketOpt.get();
            
            // Verify the ticket belongs to the current user
            if (!ticket.getUser().getUserId().equals(currentUser.getUserId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You can only close your own tickets.");
                return "redirect:/tickets";
            }

            // Check if ticket is already closed
            if (ticket.getStatus() == CustomerQuery.Status.CLOSED) {
                redirectAttributes.addFlashAttribute("errorMessage", "This ticket is already closed.");
                return "redirect:/tickets/" + ticketId;
            }

            // Close the ticket
            ticket.setStatus(CustomerQuery.Status.CLOSED);
            ticket.setUpdatedAt(java.time.LocalDateTime.now());
            customerQueryRepository.save(ticket);

            // Add system notification about ticket closure
            ticketConversationService.addSystemNotification(ticket, "Ticket closed by user.");

            redirectAttributes.addFlashAttribute("successMessage", "Ticket has been closed successfully!");
            return "redirect:/tickets/" + ticketId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while closing the ticket. Please try again.");
            return "redirect:/tickets/" + ticketId;
        }
    }
}
