package com.sliit.vehiclebiddingsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sliit.vehiclebiddingsystem.entity.CustomerQuery;
import com.sliit.vehiclebiddingsystem.entity.TicketConversation;

@Repository
public interface TicketConversationRepository extends JpaRepository<TicketConversation, Long> {

    List<TicketConversation> findByCustomerQueryOrderByCreatedAtAsc(CustomerQuery customerQuery);
    
    List<TicketConversation> findByCustomerQueryQueryIdOrderByCreatedAtAsc(Long queryId);
    
    @Query("SELECT tc FROM TicketConversation tc WHERE tc.customerQuery.queryId = :queryId ORDER BY tc.createdAt ASC")
    List<TicketConversation> findConversationsByQueryId(@Param("queryId") Long queryId);
    
    @Query("SELECT COUNT(tc) FROM TicketConversation tc WHERE tc.customerQuery.queryId = :queryId")
    long countByQueryId(@Param("queryId") Long queryId);
}
