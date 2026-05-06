package com.emall.advertising;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AdvertisingService {
    private final AdvertisingRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    AdvertisingService(AdvertisingRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    AdCampaign createCampaign(long merchantId, String name, BigDecimal dailyBudget, BigDecimal bidAmount,
                              Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ad campaign end time must be after start time");
        }
        Instant now = Instant.now();
        return repository.saveCampaign(new AdCampaign(idGenerator.nextId(), merchantId, name, dailyBudget,
                BigDecimal.ZERO, bidAmount, AdStatus.DRAFT, startsAt, endsAt, now, now));
    }

    @Transactional
    AdCampaign changeStatus(long campaignId, AdStatus status) {
        AdCampaign campaign = requireCampaign(campaignId);
        return repository.saveCampaign(campaign.changeStatus(status));
    }

    @Transactional
    AdCreative addCreative(long campaignId, long skuId, String title, String targetUrl) {
        requireCampaign(campaignId);
        return repository.saveCreative(new AdCreative(idGenerator.nextId(), campaignId, skuId, title, targetUrl,
                true));
    }

    @Transactional
    KeywordTarget addTarget(long campaignId, String keyword, BigDecimal bidMultiplier) {
        requireCampaign(campaignId);
        return repository.saveTarget(new KeywordTarget(idGenerator.nextId(), campaignId, normalize(keyword),
                bidMultiplier, true));
    }

    SponsoredResult rank(String keyword, int limit) {
        Instant now = Instant.now();
        List<SponsoredItem> items = repository.findTargets(normalize(keyword)).stream()
                .map(target -> toSponsoredItem(target, now))
                .flatMap(List::stream)
                .sorted(Comparator.comparing(SponsoredItem::score).reversed())
                .limit(Math.max(1, Math.min(limit, 20)))
                .toList();
        return new SponsoredResult(normalize(keyword), items);
    }

    @Transactional
    AdEvent recordEvent(long campaignId, long creativeId, String eventType) {
        AdCampaign campaign = requireCampaign(campaignId);
        BigDecimal cost = "click".equals(normalize(eventType)) ? campaign.bidAmount() : BigDecimal.ZERO;
        if (campaign.usedBudget().add(cost).compareTo(campaign.dailyBudget()) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "ad campaign budget is exhausted");
        }
        repository.saveCampaign(campaign.consume(cost));
        return repository.saveEvent(new AdEvent(idGenerator.nextId(), campaignId, creativeId, normalize(eventType),
                cost, Instant.now()));
    }

    private List<SponsoredItem> toSponsoredItem(KeywordTarget target, Instant now) {
        AdCampaign campaign = repository.findCampaign(target.campaignId()).orElse(null);
        if (campaign == null || campaign.status() != AdStatus.ACTIVE || now.isBefore(campaign.startsAt())
                || now.isAfter(campaign.endsAt()) || campaign.usedBudget().compareTo(campaign.dailyBudget()) >= 0) {
            return List.of();
        }
        BigDecimal score = campaign.bidAmount().multiply(target.bidMultiplier());
        return repository.findCreatives(campaign.campaignId()).stream()
                .map(creative -> new SponsoredItem(campaign.campaignId(), creative.creativeId(), creative.skuId(),
                        score, creative.title(), creative.targetUrl()))
                .toList();
    }

    private AdCampaign requireCampaign(long campaignId) {
        return repository.findCampaign(campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "ad campaign not found"));
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "advertising value must not be blank");
        }
        return normalized;
    }
}
