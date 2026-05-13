package com.emall.advertising;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusAdvertisingRepository implements AdvertisingRepository {
    private final AdvertisingMapper advertisingMapper;

    MybatisPlusAdvertisingRepository(AdvertisingMapper advertisingMapper) {
        this.advertisingMapper = advertisingMapper;
    }

    @Override
    public AdCampaign saveCampaign(AdCampaign campaign) {
        advertisingMapper.saveCampaign(campaign);
        return campaign;
    }

    @Override
    public Optional<AdCampaign> findCampaign(long campaignId) {
        return Optional.ofNullable(advertisingMapper.findCampaign(campaignId));
    }

    @Override
    public AdCreative saveCreative(AdCreative creative) {
        advertisingMapper.saveCreative(creative);
        return creative;
    }

    @Override
    public List<AdCreative> findCreatives(long campaignId) {
        return advertisingMapper.findCreatives(campaignId);
    }

    @Override
    public KeywordTarget saveTarget(KeywordTarget target) {
        advertisingMapper.saveTarget(target);
        return target;
    }

    @Override
    public List<KeywordTarget> findTargets(String keyword) {
        return advertisingMapper.findTargets(keyword);
    }

    @Override
    public AdEvent saveEvent(AdEvent event) {
        advertisingMapper.saveEvent(event);
        return event;
    }

}
