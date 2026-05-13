package com.emall.advertising;

import static com.emall.common.persistence.RowMaps.booleanValue;
import static com.emall.common.persistence.RowMaps.decimalValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
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
        return Optional.ofNullable(advertisingMapper.findCampaign(campaignId)).map(this::mapCampaign);
    }

    @Override
    public AdCreative saveCreative(AdCreative creative) {
        advertisingMapper.saveCreative(creative);
        return creative;
    }

    @Override
    public List<AdCreative> findCreatives(long campaignId) {
        return advertisingMapper.findCreatives(campaignId).stream().map(this::mapCreative).toList();
    }

    @Override
    public KeywordTarget saveTarget(KeywordTarget target) {
        advertisingMapper.saveTarget(target);
        return target;
    }

    @Override
    public List<KeywordTarget> findTargets(String keyword) {
        return advertisingMapper.findTargets(keyword).stream().map(this::mapTarget).toList();
    }

    @Override
    public AdEvent saveEvent(AdEvent event) {
        advertisingMapper.saveEvent(event);
        return event;
    }

    private AdCampaign mapCampaign(Map<String, Object> row) {
        return new AdCampaign(longValue(row, "campaign_id"), longValue(row, "merchant_id"),
                stringValue(row, "name"), decimalValue(row, "daily_budget"), decimalValue(row, "used_budget"),
                decimalValue(row, "bid_amount"), AdStatus.valueOf(stringValue(row, "status")),
                instantValue(row, "starts_at"), instantValue(row, "ends_at"), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private AdCreative mapCreative(Map<String, Object> row) {
        return new AdCreative(longValue(row, "creative_id"), longValue(row, "campaign_id"),
                longValue(row, "sku_id"), stringValue(row, "title"), stringValue(row, "target_url"),
                booleanValue(row, "active"));
    }

    private KeywordTarget mapTarget(Map<String, Object> row) {
        return new KeywordTarget(longValue(row, "target_id"), longValue(row, "campaign_id"),
                stringValue(row, "keyword"), decimalValue(row, "bid_multiplier"), booleanValue(row, "active"));
    }
}
