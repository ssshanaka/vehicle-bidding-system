package com.sliit.vehiclebiddingsystem.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sliit.vehiclebiddingsystem.entity.CustomerQuery;
import com.sliit.vehiclebiddingsystem.entity.TicketConversation;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.TicketConversationRepository;

@Service
@Transactional
public class TicketConversationService {

    @Autowired
    private TicketConversationRepository ticketConversationRepository;

    public TicketConversation createConversation(CustomerQuery customerQuery, User user, String message, TicketConversation.MessageType messageType) {
        TicketConversation conversation = new TicketConversation(customerQuery, user, message, messageType);
        return ticketConversationRepository.save(conversation);
    }

    public List<TicketConversation> getConversationsByQueryId(Long queryId) {
        return ticketConversationRepository.findConversationsByQueryId(queryId);
    }

    public List<TicketConversation> getConversationsByCustomerQuery(CustomerQuery customerQuery) {
        return ticketConversationRepository.findByCustomerQueryOrderByCreatedAtAsc(customerQuery);
    }

    public long getConversationCount(Long queryId) {
        return ticketConversationRepository.countByQueryId(queryId);
    }

    public TicketConversation addUserMessage(CustomerQuery customerQuery, User user, String message) {
        return createConversation(customerQuery, user, message, TicketConversation.MessageType.USER_MESSAGE);
    }

    public TicketConversation addSupportResponse(CustomerQuery customerQuery, User supportUser, String message) {
        return createConversation(customerQuery, supportUser, message, TicketConversation.MessageType.SUPPORT_RESPONSE);
    }

    public TicketConversation addSystemNotification(CustomerQuery customerQuery, String message) {
        // System notifications don't have a specific user
        return createConversation(customerQuery, null, message, TicketConversation.MessageType.SYSTEM_NOTIFICATION);
    }
}
