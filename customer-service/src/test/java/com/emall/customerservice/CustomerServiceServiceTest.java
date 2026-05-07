package com.emall.customerservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CustomerServiceServiceTest {
    private final InMemoryCustomerServiceRepository repository = new InMemoryCustomerServiceRepository();
    private final CustomerServiceService service =
            new CustomerServiceService(repository, new SnowflakeIdGenerator(43L));

    @Test
    void handlesTicketArbitrationCompensationAndReview() {
        ServiceTicket ticket = service.createTicket(1001L, 9001L, "refund", "high");
        ServiceTicket routed = service.routeTicket(ticket.ticketId(), "agent-a");
        ArbitrationCase arbitration = service.openArbitration(ticket.ticketId(), 2001L, "refund dispute");
        CompensationRecord compensation =
                service.grantCompensation(ticket.ticketId(), 1001L, new BigDecimal("10.00"), "service recovery");
        service.publishArticle("refund", "Refund policy", "Policy content");
        ServiceQualityReview review = service.reviewService(ticket.ticketId(), 5, "resolved quickly");

        assertThat(routed.status()).isEqualTo(TicketStatus.ROUTED);
        assertThat(arbitration.status()).isEqualTo(ArbitrationStatus.OPEN);
        assertThat(compensation.amount()).isEqualByComparingTo("10.00");
        assertThat(review.score()).isEqualTo(5);
        assertThat(service.summary().openArbitrations()).isEqualTo(1);
    }
}
