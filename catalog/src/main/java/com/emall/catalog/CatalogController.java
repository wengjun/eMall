package com.emall.catalog;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
class CatalogController {
    private final CatalogService catalogService;

    CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/categories")
    ApiResponse<CategoryNode> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.ok(catalogService.createCategory(request.parentId(), request.categoryCode(), request.name(),
                request.leaf()));
    }

    @PostMapping("/categories/{categoryId}/templates")
    ApiResponse<AttributeTemplate> upsertTemplate(@PathVariable long categoryId,
            @Valid @RequestBody UpsertTemplateRequest request) {
        return ApiResponse.ok(
                catalogService.upsertTemplate(categoryId, request.requiredAttributes(), request.optionalAttributes()));
    }

    @PostMapping("/merchants/{merchantId}/brand-authorizations")
    ApiResponse<BrandAuthorization> authorizeBrand(@PathVariable long merchantId,
            @Valid @RequestBody AuthorizeBrandRequest request) {
        return ApiResponse.ok(catalogService.authorizeBrand(merchantId, request.brandCode()));
    }

    @PostMapping("/spus")
    ApiResponse<Spu> createSpu(@Valid @RequestBody CreateSpuRequest request) {
        return ApiResponse.ok(catalogService.createSpu(request.merchantId(), request.title(), request.categoryId(),
                request.brandCode()));
    }

    @PostMapping("/spus/{spuId}/skus")
    ApiResponse<Sku> createSku(@PathVariable long spuId, @Valid @RequestBody CreateSkuRequest request) {
        return ApiResponse
                .ok(catalogService.createSku(spuId, request.skuCode(), request.price(), request.attributes()));
    }

    @PatchMapping("/spus/{spuId}/review")
    ApiResponse<ListingReview> reviewListing(@PathVariable long spuId,
            @Valid @RequestBody ReviewListingRequest request) {
        return ApiResponse
                .ok(catalogService.reviewListing(spuId, request.approved(), request.qualityScore(), request.reason()));
    }

    @PatchMapping("/spus/{spuId}/publish")
    ApiResponse<Spu> publish(@PathVariable long spuId) {
        return ApiResponse.ok(catalogService.publish(spuId));
    }

    @GetMapping("/spus/{spuId}")
    ApiResponse<Spu> getSpu(@PathVariable long spuId) {
        return ApiResponse.ok(catalogService.getSpu(spuId));
    }

    @GetMapping("/spus/{spuId}/skus")
    ApiResponse<List<Sku>> findSkus(@PathVariable long spuId) {
        return ApiResponse.ok(catalogService.findSkus(spuId));
    }

    @GetMapping("/spus/{spuId}/violations")
    ApiResponse<List<ListingViolation>> findViolations(@PathVariable long spuId) {
        return ApiResponse.ok(catalogService.findViolations(spuId));
    }

    record CreateCategoryRequest(long parentId, @NotBlank String categoryCode, @NotBlank String name, boolean leaf) {
    }

    record UpsertTemplateRequest(@NotBlank String requiredAttributes, @NotBlank String optionalAttributes) {
    }

    record AuthorizeBrandRequest(@NotBlank String brandCode) {
    }

    record CreateSpuRequest(@Positive long merchantId, @NotBlank String title, @Positive long categoryId,
            @NotBlank String brandCode) {
    }

    record CreateSkuRequest(@NotBlank String skuCode, @DecimalMin("0.01") BigDecimal price,
            @NotBlank String attributes) {
    }

    record ReviewListingRequest(boolean approved, @Min(0) @Max(100) int qualityScore, @NotBlank String reason) {
    }
}
