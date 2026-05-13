package com.emall.customerservice;

import static com.emall.common.persistence.RowMaps.decimalValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
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
        return Optional.ofNullable(customerServiceMapper.findTicket(ticketId)).map(this::mapTicket);
    }

    @Override
    public List<ServiceTicket> findTickets() {
        return customerServiceMapper.findTickets().stream().map(this::mapTicket).toList();
    }

    @Override
    public ArbitrationCase saveArbitration(ArbitrationCase arbitration) {
        customerServiceMapper.saveArbitration(arbitration);
        return arbitration;
    }

    @Override
    public Optional<ArbitrationCase> findArbitration(long arbitrationId) {
        return Optional.ofNullable(customerServiceMapper.findArbitration(arbitrationId)).map(this::mapArbitration);
    }

    @Override
    public List<ArbitrationCase> findArbitrations() {
        return customerServiceMapper.findArbitrations().stream().map(this::mapArbitration).toList();
    }

    @Override
    public CompensationRecord saveCompensation(CompensationRecord compensation) {
        customerServiceMapper.saveCompensation(compensation);
        return compensation;
    }

    @Override
    public List<CompensationRecord> findCompensations() {
        return customerServiceMapper.findCompensations().stream().map(this::mapCompensation).toList();
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

    private ServiceTicket mapTicket(Map<String, Object> row) {
        return new ServiceTicket(longValue(row, "ticket_id"), longValue(row, "user_id"),
                longValue(row, "order_id"), stringValue(row, "category"), stringValue(row, "priority"),
                TicketStatus.valueOf(stringValue(row, "status")), stringValue(row, "assignee"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private ArbitrationCase mapArbitration(Map<String, Object> row) {
        return new ArbitrationCase(longValue(row, "arbitration_id"), longValue(row, "ticket_id"),
                longValue(row, "merchant_id"), stringValue(row, "reason"),
                ArbitrationStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private CompensationRecord mapCompensation(Map<String, Object> row) {
        return new CompensationRecord(longValue(row, "compensation_id"), longValue(row, "ticket_id"),
                longValue(row, "user_id"), decimalValue(row, "amount"), stringValue(row, "reason"),
                instantValue(row, "created_at"));
    }
}
