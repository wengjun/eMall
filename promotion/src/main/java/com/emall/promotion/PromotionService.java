package com.emall.promotion;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PromotionService {
    private final PromotionRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    PromotionService(PromotionRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    PromotionCampaign createCampaign(String name, PromotionType type, BigDecimal thresholdAmount,
            BigDecimal benefitValue, BigDecimal budgetAmount, int priority, boolean stackable, Instant startsAt,
            Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "campaign end time must be after start time");
        }
        Instant now = Instant.now();
        return repository.saveCampaign(
                new PromotionCampaign(idGenerator.nextId(), name, type, thresholdAmount, benefitValue, budgetAmount,
                        BigDecimal.ZERO, priority, stackable, CampaignStatus.DRAFT, startsAt, endsAt, now, now));
    }

    @Transactional
    PromotionCampaign changeStatus(long campaignId, CampaignStatus status) {
        PromotionCampaign campaign = repository.findCampaign(campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "promotion campaign not found"));
        return repository.saveCampaign(campaign.changeStatus(status));
    }

    @Transactional
    PromotionQuote quote(long userId, BigDecimal orderAmount) {
        Instant now = Instant.now();
        BigDecimal discount = BigDecimal.ZERO;
        List<Long> applied = new ArrayList<>();
        for (PromotionCampaign campaign : repository.findActiveCampaigns()) {
            if (now.isBefore(campaign.startsAt()) || now.isAfter(campaign.endsAt())) {
                continue;
            }
            if (orderAmount.compareTo(campaign.thresholdAmount()) < 0 || !hasBudget(campaign)) {
                continue;
            }
            BigDecimal benefit = benefit(campaign, orderAmount);
            if (benefit.signum() <= 0) {
                continue;
            }
            discount = discount.add(benefit);
            applied.add(campaign.campaignId());
            repository.saveCampaign(campaign.consume(benefit));
            if (!campaign.stackable()) {
                break;
            }
        }
        BigDecimal payable = orderAmount.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new PromotionQuote(userId, orderAmount, discount.setScale(2, RoundingMode.HALF_UP), payable, applied);
    }

    CampaignCalendar calendar(String month) {
        YearMonth.parse(month);
        List<PromotionCampaign> campaigns = repository.findCampaigns();
        BigDecimal totalBudget =
                campaigns.stream().map(PromotionCampaign::budgetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal usedBudget =
                campaigns.stream().map(PromotionCampaign::usedBudget).reduce(BigDecimal.ZERO, BigDecimal::add);
        int active = (int) campaigns.stream().filter(campaign -> campaign.status() == CampaignStatus.ACTIVE).count();
        return new CampaignCalendar(month, active, totalBudget, usedBudget);
    }

    private boolean hasBudget(PromotionCampaign campaign) {
        return campaign.usedBudget().compareTo(campaign.budgetAmount()) < 0;
    }

    private BigDecimal benefit(PromotionCampaign campaign, BigDecimal orderAmount) {
        return switch (campaign.type()) {
            case AMOUNT_OFF, GIFT, BUNDLE, COUPON_PACKAGE -> campaign.benefitValue();
            case PERCENT_OFF ->
                orderAmount.multiply(campaign.benefitValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        };
    }
}
