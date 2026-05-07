package com.emall.search.api;

import com.emall.common.api.ApiResponse;
import com.emall.search.domain.SearchDocument;
import com.emall.search.domain.SearchResult;
import com.emall.search.service.SearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/documents")
    public ApiResponse<SearchDocument> index(@Valid @RequestBody IndexDocumentRequest request) {
        return ApiResponse.ok(searchService.index(request.skuId(), request.title(), request.category(), request.price(),
                request.tags(), request.saleable()));
    }

    @GetMapping("/documents/{skuId}")
    public ApiResponse<SearchDocument> get(@PathVariable long skuId) {
        return ApiResponse.ok(searchService.get(skuId));
    }

    @GetMapping
    public ApiResponse<SearchResult> search(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(searchService.search(keyword, limit));
    }

    @DeleteMapping("/documents/{skuId}")
    public ApiResponse<Void> delete(@PathVariable long skuId) {
        searchService.delete(skuId);
        return ApiResponse.ok(null);
    }

    public record IndexDocumentRequest(@Positive long skuId, @NotBlank String title, @NotBlank String category,
            @NotNull @DecimalMin("0.01") BigDecimal price, Set<String> tags, boolean saleable) {
    }
}
