package com.emall.aftersales.repository;

import com.emall.aftersales.domain.AfterSalesRequest;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryAfterSalesRepository implements AfterSalesRepository {
    private final ConcurrentMap<Long, AfterSalesRequest> requests = new ConcurrentHashMap<>();

    @Override
    public AfterSalesRequest save(AfterSalesRequest request) {
        requests.put(request.requestId(), request);
        return request;
    }

    @Override
    public Optional<AfterSalesRequest> findById(long requestId) {
        return Optional.ofNullable(requests.get(requestId));
    }
}
