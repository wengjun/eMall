package com.emall.common.controlplane;

import java.util.Map;

public record ControlPlaneObservation(boolean converged, Map<String, Object> state, String detail) {
    public ControlPlaneObservation {
        state = state == null ? Map.of() : Map.copyOf(state);
        detail = detail == null ? "" : detail;
    }

    public static ControlPlaneObservation converged(Map<String, Object> state) {
        return new ControlPlaneObservation(true, state, "converged");
    }

    public static ControlPlaneObservation pending(Map<String, Object> state, String detail) {
        return new ControlPlaneObservation(false, state, detail);
    }
}
