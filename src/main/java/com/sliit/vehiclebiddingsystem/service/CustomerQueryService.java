package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sliit.vehiclebiddingsystem.entity.CustomerQuery;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.CustomerQueryRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

@Service
@Transactional
public class CustomerQueryService {

    @Autowired
    private CustomerQueryRepository customerQueryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    public CustomerQuery createQuery(CustomerQuery query) {
        query.setCreatedAt(LocalDateTime.now());
        query.setStatus(CustomerQuery.Status.OPEN);
        
        CustomerQuery savedQuery = customerQueryRepository.save(query);
        
        // Log the query creation
        auditLogService.logSystemAction(
            "FLAG_REPORTING",
            savedQuery.getQueryId(),
            "CUSTOMER_QUERY",
            "Customer query submitted: " + query.getSubject()
        );
        
        return savedQuery;
    }

    public CustomerQuery updateQuery(CustomerQuery query) {
        query.setUpdatedAt(LocalDateTime.now());
        return customerQueryRepository.save(query);
    }

    public CustomerQuery assignQuery(Long queryId, Long assignedToUserId) {
        CustomerQuery query = customerQueryRepository.findById(queryId)
                .orElseThrow(() -> new RuntimeException("Query not found"));
        
        User assignedUser = userRepository.findById(assignedToUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        query.setAssignedTo(assignedUser);
        query.setStatus(CustomerQuery.Status.IN_PROGRESS);
        query.setUpdatedAt(LocalDateTime.now());
        
        CustomerQuery savedQuery = customerQueryRepository.save(query);
        
        // Log the assignment
        auditLogService.logAction(
            assignedUser,
            "FLAG_REPORTING",
            queryId,
            "CUSTOMER_QUERY",
            "Query assigned to: " + assignedUser.getUsername()
        );
        
        return savedQuery;
    }

    public CustomerQuery resolveQuery(Long queryId, String resolutionNotes, Long resolvedByUserId) {
        CustomerQuery query = customerQueryRepository.findById(queryId)
                .orElseThrow(() -> new RuntimeException("Query not found"));
        
        User resolvedBy = userRepository.findById(resolvedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        query.setStatus(CustomerQuery.Status.RESOLVED);
        query.setResolutionNotes(resolutionNotes);
        query.setResolvedAt(LocalDateTime.now());
        query.setUpdatedAt(LocalDateTime.now());
        
        CustomerQuery savedQuery = customerQueryRepository.save(query);
        
        // Log the resolution
        auditLogService.logAction(
            resolvedBy,
            "FLAG_REPORTING",
            queryId,
            "CUSTOMER_QUERY",
            "Query resolved: " + resolutionNotes
        );
        
        return savedQuery;
    }

    public CustomerQuery escalateQuery(Long queryId, String escalationReason, Long escalatedByUserId) {
        CustomerQuery query = customerQueryRepository.findById(queryId)
                .orElseThrow(() -> new RuntimeException("Query not found"));
        
        User escalatedBy = userRepository.findById(escalatedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        query.setStatus(CustomerQuery.Status.ESCALATED);
        query.setPriority(CustomerQuery.Priority.URGENT);
        query.setUpdatedAt(LocalDateTime.now());
        
        CustomerQuery savedQuery = customerQueryRepository.save(query);
        
        // Log the escalation
        auditLogService.logAction(
            escalatedBy,
            "FLAG_REPORTING",
            queryId,
            "CUSTOMER_QUERY",
            "Query escalated: " + escalationReason
        );
        
        return savedQuery;
    }

    public List<CustomerQuery> getAllQueries() {
        return customerQueryRepository.findAll();
    }

    public Page<CustomerQuery> getAllQueriesPaginated(Pageable pageable) {
        return customerQueryRepository.findAll(pageable);
    }

    public List<CustomerQuery> getQueriesByStatus(CustomerQuery.Status status) {
        return customerQueryRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Page<CustomerQuery> getQueriesByStatusPaginated(CustomerQuery.Status status, Pageable pageable) {
        return customerQueryRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    public List<CustomerQuery> getQueriesByPriority(CustomerQuery.Priority priority) {
        return customerQueryRepository.findByPriorityOrderByCreatedAtDesc(priority);
    }

    public Page<CustomerQuery> getQueriesByPriorityPaginated(CustomerQuery.Priority priority, Pageable pageable) {
        return customerQueryRepository.findByPriorityOrderByCreatedAtDesc(priority, pageable);
    }

    public List<CustomerQuery> getQueriesByType(CustomerQuery.QueryType queryType) {
        return customerQueryRepository.findByQueryTypeOrderByCreatedAtDesc(queryType);
    }

    public Page<CustomerQuery> getQueriesByTypePaginated(CustomerQuery.QueryType queryType, Pageable pageable) {
        return customerQueryRepository.findByQueryTypeOrderByCreatedAtDesc(queryType, pageable);
    }

    public List<CustomerQuery> getQueriesByUser(Long userId) {
        return customerQueryRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<CustomerQuery> getQueriesByUserPaginated(Long userId, Pageable pageable) {
        return customerQueryRepository.findByUserUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<CustomerQuery> getQueriesAssignedTo(Long userId) {
        return customerQueryRepository.findByAssignedToUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<CustomerQuery> getQueriesAssignedToPaginated(Long userId, Pageable pageable) {
        return customerQueryRepository.findByAssignedToUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<CustomerQuery> getRecentQueries(LocalDateTime since) {
        return customerQueryRepository.findRecentQueries(since);
    }

    public Page<CustomerQuery> getRecentQueriesPaginated(LocalDateTime since, Pageable pageable) {
        return customerQueryRepository.findRecentQueries(since, pageable);
    }

    public List<CustomerQuery> searchQueries(String keyword) {
        return customerQueryRepository.searchByKeyword(keyword);
    }

    public Page<CustomerQuery> searchQueriesPaginated(String keyword, Pageable pageable) {
        return customerQueryRepository.searchByKeyword(keyword, pageable);
    }

    public CustomerQuery getQueryById(Long queryId) {
        return customerQueryRepository.findById(queryId)
                .orElseThrow(() -> new RuntimeException("Query not found"));
    }

    public long getQueryCountByStatus(CustomerQuery.Status status) {
        return customerQueryRepository.countByStatus(status);
    }

    public long getQueryCountByPriority(CustomerQuery.Priority priority) {
        return customerQueryRepository.countByPriority(priority);
    }

    public long getRecentQueryCount(LocalDateTime since) {
        return customerQueryRepository.countRecentQueries(since);
    }

    public List<CustomerQuery> getQueriesRelatedToAuction(Long auctionId) {
        return customerQueryRepository.findByRelatedAuctionId(auctionId);
    }

    public List<CustomerQuery> getQueriesRelatedToListing(Long listingId) {
        return customerQueryRepository.findByRelatedListingId(listingId);
    }

    public void deleteQuery(Long queryId) {
        CustomerQuery query = customerQueryRepository.findById(queryId)
                .orElseThrow(() -> new RuntimeException("Query not found"));
        
        customerQueryRepository.delete(query);
        
        // Log the deletion
        auditLogService.logSystemAction(
            "FLAG_REPORTING",
            queryId,
            "CUSTOMER_QUERY",
            "Customer query deleted"
        );
    }
}
