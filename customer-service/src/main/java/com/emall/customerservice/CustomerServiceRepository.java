package com.emall.customerservice;

import java.util.List;
import java.util.Optional;

interface CustomerServiceRepository {
    ServiceTicket saveTicket(ServiceTicket ticket);

    Optional<ServiceTicket> findTicket(long ticketId);

    List<ServiceTicket> findTickets();

    ArbitrationCase saveArbitration(ArbitrationCase arbitration);

    Optional<ArbitrationCase> findArbitration(long arbitrationId);

    List<ArbitrationCase> findArbitrations();

    CompensationRecord saveCompensation(CompensationRecord compensation);

    List<CompensationRecord> findCompensations();

    KnowledgeArticle saveArticle(KnowledgeArticle article);

    ServiceQualityReview saveReview(ServiceQualityReview review);
}
