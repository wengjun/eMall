package com.emall.recommendation.domain;

import java.time.Instant;

public record UserPreference(long userId, String categoryCode, int affinityScore, Instant updatedAt) {
}
