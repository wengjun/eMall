package com.emall.advertising;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryAdvertisingRepository implements AdvertisingRepository {
    private final ConcurrentMap<Long, AdCampaign> campaigns = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, AdCreative> creatives = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, KeywordTarget> targets = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, AdEvent> events = new ConcurrentHashMap<>();

    @Override
    public AdCampaign saveCampaign(AdCampaign campaign) {
        campaigns.put(campaign.campaignId(), campaign);
        return campaign;
    }

    @Override
    public Optional<AdCampaign> findCampaign(long campaignId) {
        return Optional.ofNullable(campaigns.get(campaignId));
    }

    @Override
    public AdCreative saveCreative(AdCreative creative) {
        creatives.put(creative.creativeId(), creative);
        return creative;
    }

    @Override
    public List<AdCreative> findCreatives(long campaignId) {
        return creatives.values().stream().filter(creative -> creative.campaignId() == campaignId)
                .filter(AdCreative::active).toList();
    }

    @Override
    public KeywordTarget saveTarget(KeywordTarget target) {
        targets.put(target.targetId(), target);
        return target;
    }

    @Override
    public List<KeywordTarget> findTargets(String keyword) {
        return targets.values().stream().filter(target -> target.keyword().equals(keyword))
                .filter(KeywordTarget::active).toList();
    }

    @Override
    public AdEvent saveEvent(AdEvent event) {
        events.put(event.eventId(), event);
        return event;
    }
}
