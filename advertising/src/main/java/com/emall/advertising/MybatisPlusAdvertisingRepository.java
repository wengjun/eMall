package com.emall.advertising;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusAdvertisingRepository implements AdvertisingRepository {
    private final AdvertisingMapper advertisingMapper;
    private final AdCampaignMapper campaignMapper;
    private final AdCreativeMapper creativeMapper;
    private final KeywordTargetMapper targetMapper;
    private final AdEventMapper eventMapper;

    MybatisPlusAdvertisingRepository(AdvertisingMapper advertisingMapper, AdCampaignMapper campaignMapper,
            AdCreativeMapper creativeMapper, KeywordTargetMapper targetMapper, AdEventMapper eventMapper) {
        this.advertisingMapper = advertisingMapper;
        this.campaignMapper = campaignMapper;
        this.creativeMapper = creativeMapper;
        this.targetMapper = targetMapper;
        this.eventMapper = eventMapper;
    }

    @Override
    public AdCampaign saveCampaign(AdCampaign campaign) {
        advertisingMapper.saveCampaign(campaign);
        return campaign;
    }

    @Override
    public Optional<AdCampaign> findCampaign(long campaignId) {
        return Optional.ofNullable(campaignMapper.selectById(campaignId));
    }

    @Override
    public AdCreative saveCreative(AdCreative creative) {
        advertisingMapper.saveCreative(creative);
        return creative;
    }

    @Override
    public List<AdCreative> findCreatives(long campaignId) {
        return creativeMapper
                .selectList(new QueryWrapper<AdCreative>().eq("campaign_id", campaignId).eq("active", true));
    }

    @Override
    public KeywordTarget saveTarget(KeywordTarget target) {
        advertisingMapper.saveTarget(target);
        return target;
    }

    @Override
    public List<KeywordTarget> findTargets(String keyword) {
        return targetMapper.selectList(new QueryWrapper<KeywordTarget>().eq("keyword", keyword).eq("active", true));
    }

    @Override
    public AdEvent saveEvent(AdEvent event) {
        eventMapper.insert(event);
        return event;
    }
}
