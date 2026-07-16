package com.emall.common.controlplane;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

public class ControlPlaneRuntimeGuard implements ApplicationRunner {
    private final Environment environment;
    private final ControlPlaneProperties properties;
    private final ControlPlaneOperationStore store;
    private final List<ControlPlaneAdapter> adapters;

    public ControlPlaneRuntimeGuard(Environment environment, ControlPlaneProperties properties,
            ControlPlaneOperationStore store, List<ControlPlaneAdapter> adapters) {
        this.environment = environment;
        this.properties = properties;
        this.store = store;
        this.adapters = adapters;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!isProduction()) {
            return;
        }
        if (store instanceof InMemoryControlPlaneOperationStore) {
            throw new IllegalStateException("production control plane requires durable operation storage");
        }
        Set<ControlPlaneTarget> configured = EnumSet.noneOf(ControlPlaneTarget.class);
        adapters.forEach(adapter -> configured.add(adapter.target()));
        if (!configured.containsAll(properties.getRequiredTargets())) {
            Set<ControlPlaneTarget> missing = EnumSet.copyOf(properties.getRequiredTargets());
            missing.removeAll(configured);
            throw new IllegalStateException("missing production control-plane adapters: " + missing);
        }
        rejectLocalEndpoint(ControlPlaneTarget.NACOS_CONFIG, properties.getNacos().getBaseUrl());
        rejectLocalEndpoint(ControlPlaneTarget.KUBERNETES_RESOURCE, properties.getKubernetes().getBaseUrl());
        rejectLocalEndpoint(ControlPlaneTarget.INFRASTRUCTURE_API, properties.getInfrastructure().getBaseUrl());
        requireCredential(ControlPlaneTarget.NACOS_CONFIG, properties.getNacos().getAccessToken(),
                "Nacos access token");
        requireCredential(ControlPlaneTarget.INFRASTRUCTURE_API, properties.getInfrastructure().getBearerToken(),
                "infrastructure operator bearer token");
        requireKubernetesToken();
    }

    private boolean isProduction() {
        String runtimeMode = environment.getProperty("emall.runtime.mode", "");
        return environment.acceptsProfiles(Profiles.of("prod", "production")) || "prod".equalsIgnoreCase(runtimeMode)
                || "production".equalsIgnoreCase(runtimeMode);
    }

    private void rejectLocalEndpoint(ControlPlaneTarget target, String endpoint) {
        if (!properties.getRequiredTargets().contains(target)) {
            return;
        }
        String host = URI.create(endpoint).getHost();
        if (host == null || host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1")) {
            throw new IllegalStateException("production control-plane endpoint must not be local: " + target);
        }
    }

    private void requireCredential(ControlPlaneTarget target, String credential, String description) {
        if (properties.getRequiredTargets().contains(target) && !StringUtils.hasText(credential)) {
            throw new IllegalStateException("production control plane requires " + description);
        }
    }

    private void requireKubernetesToken() {
        if (!properties.getRequiredTargets().contains(ControlPlaneTarget.KUBERNETES_RESOURCE)
                || StringUtils.hasText(properties.getKubernetes().getBearerToken())) {
            return;
        }
        String tokenFile = properties.getKubernetes().getBearerTokenFile();
        try {
            if (StringUtils.hasText(tokenFile) && Files.isRegularFile(Path.of(tokenFile))) {
                return;
            }
        } catch (InvalidPathException ignored) {
            // Report one stable startup error below.
        }
        throw new IllegalStateException("production control plane requires a Kubernetes service-account token");
    }
}
