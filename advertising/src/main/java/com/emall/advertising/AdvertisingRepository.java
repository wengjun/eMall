package com.emall.advertising;

import java.util.List;
import java.util.Optional;

interface AdvertisingRepository {
    AdCampaign saveCampaign(AdCampaign campaign);

    Optional<AdCampaign> findCampaign(long campaignId);

    AdCreative saveCreative(AdCreative creative);

    List<AdCreative> findCreatives(long campaignId);

    KeywordTarget saveTarget(KeywordTarget target);

    List<KeywordTarget> findTargets(String keyword);

    AdEvent saveEvent(AdEvent event);
}
