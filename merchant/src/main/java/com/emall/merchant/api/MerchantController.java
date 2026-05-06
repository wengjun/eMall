package com.emall.merchant.api;

import com.emall.common.api.ApiResponse;
import com.emall.merchant.domain.CommissionRule;
import com.emall.merchant.domain.Invoice;
import com.emall.merchant.domain.Merchant;
import com.emall.merchant.domain.MerchantStatus;
import com.emall.merchant.domain.Settlement;
import com.emall.merchant.domain.Store;
import com.emall.merchant.domain.StoreStatus;
import com.emall.merchant.service.MerchantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {
    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Merchant> register(@Valid @RequestBody RegisterMerchantRequest request) {
        return ApiResponse.ok(merchantService.registerMerchant(request.name(), request.contactEmail()));
    }

    @GetMapping("/{merchantId}")
    public ApiResponse<Merchant> getMerchant(@PathVariable long merchantId) {
        return ApiResponse.ok(merchantService.getMerchant(merchantId));
    }

    @PatchMapping("/{merchantId}/status")
    public ApiResponse<Merchant> changeMerchantStatus(@PathVariable long merchantId,
                                                      @Valid @RequestBody ChangeMerchantStatusRequest request) {
        return ApiResponse.ok(merchantService.changeMerchantStatus(merchantId, request.status()));
    }

    @PostMapping("/{merchantId}/stores")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Store> createStore(@PathVariable long merchantId,
                                          @Valid @RequestBody CreateStoreRequest request) {
        return ApiResponse.ok(merchantService.createStore(merchantId, request.name(), request.description()));
    }

    @GetMapping("/{merchantId}/stores")
    public ApiResponse<List<Store>> stores(@PathVariable long merchantId) {
        return ApiResponse.ok(merchantService.findStores(merchantId));
    }

    @PatchMapping("/stores/{storeId}/status")
    public ApiResponse<Store> changeStoreStatus(@PathVariable long storeId,
                                                @Valid @RequestBody ChangeStoreStatusRequest request) {
        return ApiResponse.ok(merchantService.changeStoreStatus(storeId, request.status()));
    }

    @PostMapping("/{merchantId}/commission-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommissionRule> createCommissionRule(
            @PathVariable long merchantId,
            @Valid @RequestBody CreateCommissionRuleRequest request) {
        return ApiResponse.ok(merchantService.createCommissionRule(
                merchantId, request.rate(), request.effectiveFrom()));
    }

    @PostMapping("/{merchantId}/settlements")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Settlement> createSettlement(@PathVariable long merchantId,
                                                    @Valid @RequestBody CreateSettlementRequest request) {
        return ApiResponse.ok(merchantService.createSettlement(
                merchantId, request.grossAmount(), request.periodStart(), request.periodEnd()));
    }

    @GetMapping("/{merchantId}/settlements")
    public ApiResponse<List<Settlement>> settlements(@PathVariable long merchantId) {
        return ApiResponse.ok(merchantService.findSettlements(merchantId));
    }

    @PostMapping("/settlements/{settlementId}/pay")
    public ApiResponse<Settlement> paySettlement(@PathVariable long settlementId) {
        return ApiResponse.ok(merchantService.paySettlement(settlementId));
    }

    @PostMapping("/settlements/{settlementId}/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Invoice> issueInvoice(@PathVariable long settlementId,
                                             @Valid @RequestBody IssueInvoiceRequest request) {
        return ApiResponse.ok(merchantService.issueInvoice(settlementId, request.invoiceTitle()));
    }

    @GetMapping("/{merchantId}/invoices")
    public ApiResponse<List<Invoice>> invoices(@PathVariable long merchantId) {
        return ApiResponse.ok(merchantService.findInvoices(merchantId));
    }

    public record RegisterMerchantRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Email @Size(max = 180) String contactEmail
    ) {
    }

    public record ChangeMerchantStatusRequest(@NotNull MerchantStatus status) {
    }

    public record CreateStoreRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Size(max = 300) String description
    ) {
    }

    public record ChangeStoreStatusRequest(@NotNull StoreStatus status) {
    }

    public record CreateCommissionRuleRequest(
            @NotNull @DecimalMin("0.000000") @DecimalMax("1.000000") BigDecimal rate,
            @NotNull Instant effectiveFrom
    ) {
    }

    public record CreateSettlementRequest(
            @NotNull @DecimalMin("0.01") BigDecimal grossAmount,
            @NotNull Instant periodStart,
            @NotNull Instant periodEnd
    ) {
    }

    public record IssueInvoiceRequest(@NotBlank @Size(max = 120) String invoiceTitle) {
    }
}
