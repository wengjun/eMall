package com.emall.loadtest;

import java.util.ArrayList;
import java.util.List;

final class TrafficModel {
    private final LoadTestOptions options;
    private final List<WeightedScenario> mix;
    private final int totalWeight;

    TrafficModel(LoadTestOptions options) {
        this.options = options;
        this.mix = parse(options.trafficMix());
        this.totalWeight = mix.stream().mapToInt(WeightedScenario::weight).sum();
    }

    LoadScenario scenario(long requestSequence) {
        if (options.scenario() != LoadScenario.PRODUCTION_MIX) {
            return options.scenario();
        }
        int selected = Math.floorMod(hash(requestSequence), totalWeight);
        int cumulative = 0;
        for (WeightedScenario entry : mix) {
            cumulative += entry.weight();
            if (selected < cumulative) {
                return entry.scenario();
            }
        }
        throw new IllegalStateException("traffic mix selection did not converge");
    }

    long userId(long requestSequence) {
        return options.userId() + Math.floorMod(hash(requestSequence * 31L), options.userCardinality());
    }

    long skuId(long requestSequence, boolean forceHotSku) {
        if (forceHotSku || options.skuCardinality() == 1
                || Math.floorMod(hash(requestSequence * 17L), 100) < options.hotSkuPercent()) {
            return options.skuId();
        }
        return options.skuId() + 1L + Math.floorMod(hash(requestSequence * 47L), options.skuCardinality() - 1);
    }

    String deviceId(long requestSequence) {
        long cardinality = Math.max(1L, Math.min(options.userCardinality(), options.maxInflight() * 20L));
        return "loadtest-" + Math.floorMod(hash(requestSequence * 13L), cardinality);
    }

    boolean includes(LoadScenario scenario) {
        return options.scenario() == scenario || options.scenario() == LoadScenario.PRODUCTION_MIX
                && mix.stream().anyMatch(entry -> entry.scenario() == scenario);
    }

    private List<WeightedScenario> parse(String specification) {
        List<WeightedScenario> entries = new ArrayList<>();
        for (String token : specification.split(",")) {
            String[] parts = token.trim().split(":", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("invalid traffic mix entry: " + token);
            }
            LoadScenario scenario = LoadScenario.from(parts[0].trim());
            int weight = Integer.parseInt(parts[1].trim());
            if (scenario == LoadScenario.PRODUCTION_MIX || weight <= 0) {
                throw new IllegalArgumentException("traffic mix entries require a concrete scenario and weight");
            }
            entries.add(new WeightedScenario(scenario, weight));
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("traffic mix must not be empty");
        }
        return List.copyOf(entries);
    }

    private int hash(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return (int) (mixed ^ mixed >>> 32);
    }

    private record WeightedScenario(LoadScenario scenario, int weight) {
    }
}
