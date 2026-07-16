package com.emall.common.controlplane;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ControlPlaneRuntimeGuardTest {
    @Test
    void rejectsInMemoryOperationStoreInProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        ControlPlaneRuntimeGuard guard = new ControlPlaneRuntimeGuard(environment, new ControlPlaneProperties(),
                new InMemoryControlPlaneOperationStore(), List.of());

        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("durable operation storage");
    }

    @Test
    void treatsProductionRuntimeModeAsProductionWithoutSpringProfile() {
        MockEnvironment environment = new MockEnvironment().withProperty("emall.runtime.mode", "production");
        ControlPlaneRuntimeGuard guard = new ControlPlaneRuntimeGuard(environment, new ControlPlaneProperties(),
                new InMemoryControlPlaneOperationStore(), List.of());

        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("durable operation storage");
    }

    @Test
    void rejectsMissingInfrastructureCredentialInProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        ControlPlaneProperties properties = new ControlPlaneProperties();
        properties.setRequiredTargets(List.of(ControlPlaneTarget.INFRASTRUCTURE_API));
        properties.getInfrastructure().setBaseUrl("https://operator.example.test");
        ControlPlaneAdapter adapter = mock(ControlPlaneAdapter.class);
        when(adapter.target()).thenReturn(ControlPlaneTarget.INFRASTRUCTURE_API);
        ControlPlaneRuntimeGuard guard = new ControlPlaneRuntimeGuard(environment, properties,
                mock(ControlPlaneOperationStore.class), List.of(adapter));

        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("operator bearer token");
    }

    @Test
    void rejectsMissingKubernetesServiceAccountTokenInProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        ControlPlaneProperties properties = new ControlPlaneProperties();
        properties.setRequiredTargets(List.of(ControlPlaneTarget.KUBERNETES_RESOURCE));
        properties.getKubernetes().setBearerTokenFile("Z:/missing/control-plane-token");
        ControlPlaneAdapter adapter = mock(ControlPlaneAdapter.class);
        when(adapter.target()).thenReturn(ControlPlaneTarget.KUBERNETES_RESOURCE);
        ControlPlaneRuntimeGuard guard = new ControlPlaneRuntimeGuard(environment, properties,
                mock(ControlPlaneOperationStore.class), List.of(adapter));

        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("service-account token");
    }
}
