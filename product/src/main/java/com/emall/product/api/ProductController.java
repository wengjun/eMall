package com.emall.product.api;

import com.emall.common.api.ApiResponse;
import com.emall.product.domain.Product;
import com.emall.product.domain.ProductStatus;
import com.emall.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Product> create(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.ok(productService.create(
                request.spuId(), request.title(), request.category(), request.price()));
    }

    @GetMapping("/{skuId}")
    public ApiResponse<Product> getProduct(@PathVariable long skuId) {
        return ApiResponse.ok(productService.get(skuId));
    }

    @GetMapping
    public ApiResponse<List<Product>> search(@RequestParam(defaultValue = "") String keyword,
                                             @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(productService.search(keyword, limit));
    }

    @PatchMapping("/{skuId}/price")
    public ApiResponse<Product> changePrice(@PathVariable long skuId, @Valid @RequestBody ChangePriceRequest request) {
        return ApiResponse.ok(productService.changePrice(skuId, request.price()));
    }

    @PatchMapping("/{skuId}/status")
    public ApiResponse<Product> changeStatus(@PathVariable long skuId,
                                             @Valid @RequestBody ChangeStatusRequest request) {
        return ApiResponse.ok(productService.changeStatus(skuId, request.status()));
    }

    @PatchMapping("/{skuId}/title")
    public ApiResponse<Product> rename(@PathVariable long skuId, @Valid @RequestBody RenameProductRequest request) {
        return ApiResponse.ok(productService.rename(skuId, request.title()));
    }

    public record CreateProductRequest(
            @Positive long spuId,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 60) String category,
            @NotNull @DecimalMin("0.01") BigDecimal price
    ) {
    }

    public record ChangePriceRequest(@NotNull @DecimalMin("0.01") BigDecimal price) {
    }

    public record ChangeStatusRequest(@NotNull ProductStatus status) {
    }

    public record RenameProductRequest(@NotBlank @Size(max = 120) String title) {
    }
}
