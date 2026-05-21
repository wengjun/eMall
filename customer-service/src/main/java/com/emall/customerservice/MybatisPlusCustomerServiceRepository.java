package com.emall.customerservice;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusCustomerServiceRepository implements CustomerServiceRepository {
    private final CustomerServiceMapper customerServiceMapper;
    private final ServiceTicketMapper ticketMapper;
    private final ArbitrationCaseMapper arbitrationMapper;
    private final CompensationRecordMapper compensationMapper;
    private final ServiceQualityReviewMapper reviewMapper;

    MybatisPlusCustomerServiceRepository(CustomerServiceMapper customerServiceMapper, ServiceTicketMapper ticketMapper,
            ArbitrationCaseMapper arbitrationMapper, CompensationRecordMapper compensationMapper,
            ServiceQualityReviewMapper reviewMapper) {
        this.customerServiceMapper = customerServiceMapper;
        this.ticketMapper = ticketMapper;
        this.arbitrationMapper = arbitrationMapper;
        this.compensationMapper = compensationMapper;
        this.reviewMapper = reviewMapper;
    }

    @Override
    public ServiceTicket saveTicket(ServiceTicket ticket) {
        customerServiceMapper.saveTicket(ticket);
        return ticket;
    }

    @Override
    public Optional<ServiceTicket> findTicket(long ticketId) {
        return Optional.ofNullable(ticketMapper.selectById(ticketId));
    }

    @Override
    public List<ServiceTicket> findTickets() {
        return ticketMapper.selectList(null);
    }

    @Override
    public ArbitrationCase saveArbitration(ArbitrationCase arbitration) {
        customerServiceMapper.saveArbitration(arbitration);
        return arbitration;
    }

    @Override
    public Optional<ArbitrationCase> findArbitration(long arbitrationId) {
        return Optional.ofNullable(arbitrationMapper.selectById(arbitrationId));
    }

    @Override
    public List<ArbitrationCase> findArbitrations() {
        return arbitrationMapper.selectList(null);
    }

    @Override
    public CompensationRecord saveCompensation(CompensationRecord compensation) {
        compensationMapper.insert(compensation);
        return compensation;
    }

    @Override
    public List<CompensationRecord> findCompensations() {
        return compensationMapper.selectList(null);
    }

    @Override
    public KnowledgeArticle saveArticle(KnowledgeArticle article) {
        customerServiceMapper.saveArticle(article);
        return article;
    }

    @Override
    public ServiceQualityReview saveReview(ServiceQualityReview review) {
        reviewMapper.insert(review);
        return review;
    }

}
