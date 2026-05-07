package com.emall.customerservice;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer-service")
class CustomerServiceController {
    private final CustomerServiceService customerService;

    CustomerServiceController(CustomerServiceService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/tickets")
    ApiResponse<ServiceTicket> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        return ApiResponse.ok(customerService.createTicket(request.userId(), request.orderId(), request.category(),
                request.priority()));
    }

    @PatchMapping("/tickets/{ticketId}/route")
    ApiResponse<ServiceTicket> routeTicket(@PathVariable long ticketId,
            @Valid @RequestBody RouteTicketRequest request) {
        return ApiResponse.ok(customerService.routeTicket(ticketId, request.assignee()));
    }

    @PatchMapping("/tickets/{ticketId}/resolve")
    ApiResponse<ServiceTicket> resolveTicket(@PathVariable long ticketId) {
        return ApiResponse.ok(customerService.resolveTicket(ticketId));
    }

    @PostMapping("/arbitrations")
    ApiResponse<ArbitrationCase> openArbitration(@Valid @RequestBody OpenArbitrationRequest request) {
        return ApiResponse
                .ok(customerService.openArbitration(request.ticketId(), request.merchantId(), request.reason()));
    }

    @PatchMapping("/arbitrations/{arbitrationId}/status")
    ApiResponse<ArbitrationCase> closeArbitration(@PathVariable long arbitrationId,
            @Valid @RequestBody CloseArbitrationRequest request) {
        return ApiResponse.ok(customerService.closeArbitration(arbitrationId, request.status()));
    }

    @PostMapping("/compensations")
    ApiResponse<CompensationRecord> grantCompensation(@Valid @RequestBody GrantCompensationRequest request) {
        return ApiResponse.ok(customerService.grantCompensation(request.ticketId(), request.userId(), request.amount(),
                request.reason()));
    }

    @PostMapping("/knowledge")
    ApiResponse<KnowledgeArticle> publishArticle(@Valid @RequestBody PublishArticleRequest request) {
        return ApiResponse.ok(customerService.publishArticle(request.category(), request.title(), request.content()));
    }

    @PostMapping("/reviews")
    ApiResponse<ServiceQualityReview> reviewService(@Valid @RequestBody ReviewServiceRequest request) {
        return ApiResponse.ok(customerService.reviewService(request.ticketId(), request.score(), request.comment()));
    }

    @GetMapping("/summary")
    ApiResponse<CustomerServiceSummary> summary() {
        return ApiResponse.ok(customerService.summary());
    }

    record CreateTicketRequest(@Positive long userId, @Positive long orderId, @NotBlank String category,
            @NotBlank String priority) {
    }

    record RouteTicketRequest(@NotBlank String assignee) {
    }

    record OpenArbitrationRequest(@Positive long ticketId, @Positive long merchantId, @NotBlank String reason) {
    }

    record CloseArbitrationRequest(ArbitrationStatus status) {
    }

    record GrantCompensationRequest(@Positive long ticketId, @Positive long userId,
            @DecimalMin("0.01") BigDecimal amount, @NotBlank String reason) {
    }

    record PublishArticleRequest(@NotBlank String category, @NotBlank String title, @NotBlank String content) {
    }

    record ReviewServiceRequest(@Positive long ticketId, @Min(1) @Max(5) int score, @NotBlank String comment) {
    }
}
