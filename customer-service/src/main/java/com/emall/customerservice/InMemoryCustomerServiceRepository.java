package com.emall.customerservice;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryCustomerServiceRepository implements CustomerServiceRepository {
    private final ConcurrentMap<Long, ServiceTicket> tickets = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ArbitrationCase> arbitrations = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, CompensationRecord> compensations = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, KnowledgeArticle> articles = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ServiceQualityReview> reviews = new ConcurrentHashMap<>();

    @Override
    public ServiceTicket saveTicket(ServiceTicket ticket) {
        tickets.put(ticket.ticketId(), ticket);
        return ticket;
    }

    @Override
    public Optional<ServiceTicket> findTicket(long ticketId) {
        return Optional.ofNullable(tickets.get(ticketId));
    }

    @Override
    public List<ServiceTicket> findTickets() {
        return List.copyOf(tickets.values());
    }

    @Override
    public ArbitrationCase saveArbitration(ArbitrationCase arbitration) {
        arbitrations.put(arbitration.arbitrationId(), arbitration);
        return arbitration;
    }

    @Override
    public Optional<ArbitrationCase> findArbitration(long arbitrationId) {
        return Optional.ofNullable(arbitrations.get(arbitrationId));
    }

    @Override
    public List<ArbitrationCase> findArbitrations() {
        return List.copyOf(arbitrations.values());
    }

    @Override
    public CompensationRecord saveCompensation(CompensationRecord compensation) {
        compensations.put(compensation.compensationId(), compensation);
        return compensation;
    }

    @Override
    public List<CompensationRecord> findCompensations() {
        return List.copyOf(compensations.values());
    }

    @Override
    public KnowledgeArticle saveArticle(KnowledgeArticle article) {
        articles.put(article.articleId(), article);
        return article;
    }

    @Override
    public ServiceQualityReview saveReview(ServiceQualityReview review) {
        reviews.put(review.reviewId(), review);
        return review;
    }
}
