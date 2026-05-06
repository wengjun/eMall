package com.emall.aftersales.repository;

import com.emall.aftersales.domain.AfterSalesRequest;
import java.util.Optional;

public interface AfterSalesRepository {
    AfterSalesRequest save(AfterSalesRequest request);

    Optional<AfterSalesRequest> findById(long requestId);
}
