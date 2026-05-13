package com.emall.customerservice;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusCustomerServiceRepository implements CustomerServiceRepository {
    private final CustomerServiceMapper customerServiceMapper;

    MybatisPlusCustomerServiceRepository(CustomerServiceMapper customerServiceMapper) {
        this.customerServiceMapper = customerServiceMapper;
    }

    @Override
    public ServiceTicket saveTicket(ServiceTicket ticket) {
        customerServiceMapper.saveTicket(ticket);
        return ticket;
    }

    @Override
    public Optional<ServiceTicket> findTicket(long ticketId) {
        return Optional.ofNullable(customerServiceMapper.findTicket(ticketId));
    }

    @Override
    public List<ServiceTicket> findTickets() {
        return customerServiceMapper.findTickets();
    }

    @Override
    public ArbitrationCase saveArbitration(ArbitrationCase arbitration) {
        customerServiceMapper.saveArbitration(arbitration);
        return arbitration;
    }

    @Override
    public Optional<ArbitrationCase> findArbitration(long arbitrationId) {
        return Optional.ofNullable(customerServiceMapper.findArbitration(arbitrationId));
    }

    @Override
    public List<ArbitrationCase> findArbitrations() {
        return customerServiceMapper.findArbitrations();
    }

    @Override
    public CompensationRecord saveCompensation(CompensationRecord compensation) {
        customerServiceMapper.saveCompensation(compensation);
        return compensation;
    }

    @Override
    public List<CompensationRecord> findCompensations() {
        return customerServiceMapper.findCompensations();
    }

    @Override
    public KnowledgeArticle saveArticle(KnowledgeArticle article) {
        customerServiceMapper.saveArticle(article);
        return article;
    }

    @Override
    public ServiceQualityReview saveReview(ServiceQualityReview review) {
        customerServiceMapper.saveReview(review);
        return review;
    }

}
