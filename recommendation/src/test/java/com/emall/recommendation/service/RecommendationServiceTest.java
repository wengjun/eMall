package com.emall.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.recommendation.domain.BehaviorType;
import com.emall.recommendation.domain.Experiment;
import com.emall.recommendation.domain.ExperimentStatus;
import com.emall.recommendation.domain.RecommendationItem;
import com.emall.recommendation.domain.UserPreference;
import com.emall.recommendation.repository.InMemoryRecommendationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationServiceTest {
    private final InMemoryRecommendationRepository repository = new InMemoryRecommendationRepository();
    private final RecommendationService service = new RecommendationService(repository, new SnowflakeIdGenerator(13L));

    @Test
    void ranksItemsWithUserAffinityAndActiveExperimentStrategy() {
        service.upsertItemFeature(1001L, "phone", new BigDecimal("0.70"), new BigDecimal("0.20"), true);
        service.upsertItemFeature(1002L, "book", new BigDecimal("0.40"), new BigDecimal("0.95"), true);
        service.upsertPreference(2001L, "phone", 90);

        List<RecommendationItem> balanced = service.recommend(2001L, "home", 2);

        assertThat(balanced).extracting(RecommendationItem::skuId).containsExactly(1001L, 1002L);
        assertThat(balanced.get(0).experimentBucket()).isEqualTo("control");
        assertThat(balanced.get(0).strategyCode()).isEqualTo("balanced");

        Experiment experiment = service.createExperiment("home", "popularity ranking", 100, "balanced", "popularity");
        service.changeExperimentStatus(experiment.experimentId(), ExperimentStatus.ACTIVE);

        List<RecommendationItem> treatment = service.recommend(2001L, "home", 2);

        assertThat(treatment).extracting(RecommendationItem::skuId).containsExactly(1002L, 1001L);
        assertThat(treatment.get(0).experimentBucket()).isEqualTo("treatment");
        assertThat(treatment.get(0).strategyCode()).isEqualTo("popularity");
    }

    @Test
    void behaviorEventsUpdateUserPreference() {
        service.recordBehavior(2001L, 1001L, "phone", BehaviorType.PURCHASE, Instant.now());

        UserPreference preference = service.findPreferences(2001L).get(0);

        assertThat(preference.categoryCode()).isEqualTo("phone");
        assertThat(preference.affinityScore()).isEqualTo(15);
    }
}
