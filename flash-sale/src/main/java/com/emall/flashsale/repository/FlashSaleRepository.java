package com.emall.flashsale.repository;

import com.emall.flashsale.domain.FlashSaleCampaign;
import com.emall.flashsale.domain.FlashSaleOrderRequest;
import com.emall.flashsale.domain.FlashSaleStock;
import com.emall.flashsale.domain.FlashSaleToken;
import java.util.List;
import java.util.Optional;

public interface FlashSaleRepository {
    FlashSaleCampaign saveCampaign(FlashSaleCampaign campaign);

    Optional<FlashSaleCampaign> findCampaign(long campaignId);

    FlashSaleStock saveStock(FlashSaleStock stock);

    Optional<FlashSaleStock> findStock(long campaignId);

    boolean reserveTokenStock(long campaignId, int quantity);

    boolean moveTokenStockToQueue(long campaignId, int quantity);

    FlashSaleToken saveToken(FlashSaleToken token);

    Optional<FlashSaleToken> findToken(String token);

    int countTokensByUser(long campaignId, long userId);

    FlashSaleOrderRequest saveOrderRequest(FlashSaleOrderRequest request);

    Optional<FlashSaleOrderRequest> findOrderRequest(long requestId);

    List<FlashSaleOrderRequest> findQueuedRequests(long campaignId, int limit);

    int countQueuedRequests(long campaignId);
}
