package com.emall.common.controlplane;

import com.emall.common.web.OutboundHttpClientFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

public class KubernetesResourceControlPlaneAdapter implements ControlPlaneAdapter {
    private static final MediaType SERVER_SIDE_APPLY = MediaType.parseMediaType("application/apply-patch+yaml");
    private static final TypeReference<Map<String, Object>> JACKSON_MAP_TYPE = new TypeReference<>() {
    };
    private static final ParameterizedTypeReference<Map<String, Object>> HTTP_MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final ControlPlaneProperties.Kubernetes properties;
    private final ObjectMapper objectMapper;
    private final ControlPlaneJson json;

    public KubernetesResourceControlPlaneAdapter(OutboundHttpClientFactory clientFactory,
            ControlPlaneProperties.Kubernetes properties, ObjectMapper objectMapper) {
        SSLContext sslContext = clusterSslContext(properties.getCaCertificateFile());
        this.restClient = sslContext == null
                ? clientFactory.restClient("control-plane-kubernetes", properties.getBaseUrl())
                : clientFactory.restClient("control-plane-kubernetes", properties.getBaseUrl(), sslContext);
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.json = new ControlPlaneJson(objectMapper);
    }

    @Override
    public ControlPlaneTarget target() {
        return ControlPlaneTarget.KUBERNETES_RESOURCE;
    }

    @Override
    public Map<String, Object> captureRollbackState(ControlPlaneOperation operation) {
        Map<String, Object> current = read(operation.desiredState());
        if (current == null) {
            return Map.of("exists", false);
        }
        Map<String, Object> resource = mutableResource(current);
        return Map.of("exists", true, "resource", resource, "fingerprint", json.digest(resource));
    }

    @Override
    public void apply(ControlPlaneOperation operation) {
        Map<String, Object> manifest = ControlPlaneStateValues.map(operation.desiredState(), "manifest");
        applyManifest(operation.desiredState(), withDigest(manifest, operation.desiredDigest()));
    }

    @Override
    public ControlPlaneObservation observe(ControlPlaneOperation operation) {
        Map<String, Object> current = read(operation.desiredState());
        String digest = current == null ? null : desiredDigest(current);
        boolean converged = operation.desiredDigest().equals(digest);
        Map<String, Object> state = current == null
                ? Map.of("exists", false)
                : Map.of("exists", true, "desiredDigest", digest == null ? "" : digest);
        return new ControlPlaneObservation(converged, state,
                converged ? "converged" : "Kubernetes desired digest differs");
    }

    @Override
    public void rollback(ControlPlaneOperation operation) {
        if (Boolean.TRUE.equals(operation.rollbackState().get("exists"))) {
            Map<String, Object> resource = ControlPlaneStateValues.map(operation.rollbackState(), "resource");
            applyManifest(operation.desiredState(), resource);
        } else {
            delete(operation.desiredState());
        }
    }

    @Override
    public ControlPlaneObservation observeRollback(ControlPlaneOperation operation) {
        Map<String, Object> current = read(operation.desiredState());
        boolean expectedExists = Boolean.TRUE.equals(operation.rollbackState().get("exists"));
        boolean converged = expectedExists == (current != null);
        Map<String, Object> state = current == null ? Map.of("exists", false) : Map.of("exists", true);
        if (converged && expectedExists) {
            String expected = String.valueOf(operation.rollbackState().get("fingerprint"));
            converged = expected.equals(json.digest(mutableResource(current)));
        }
        return new ControlPlaneObservation(converged, state,
                converged ? "rollback converged" : "Kubernetes rollback differs");
    }

    private Map<String, Object> read(Map<String, Object> state) {
        try {
            return restClient.get().uri(resourcePath(state)).headers(this::authorize).retrieve().body(HTTP_MAP_TYPE);
        } catch (HttpClientErrorException.NotFound exception) {
            return null;
        }
    }

    private void applyManifest(Map<String, Object> state, Map<String, Object> manifest) {
        String body;
        try {
            body = objectMapper.writeValueAsString(manifest);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Kubernetes manifest is not JSON serializable", exception);
        }
        restClient.method(HttpMethod.PATCH).uri(resourcePath(state) + "?fieldManager=emall-control-plane&force=true")
                .headers(this::authorize).contentType(SERVER_SIDE_APPLY).body(body).retrieve().toBodilessEntity();
    }

    private void delete(Map<String, Object> state) {
        try {
            restClient.delete().uri(resourcePath(state)).headers(this::authorize).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ignored) {
            // Deletion is idempotent.
        }
    }

    private String resourcePath(Map<String, Object> state) {
        String apiVersion = ControlPlaneStateValues.text(state, "apiVersion");
        String namespace = ControlPlaneStateValues.optionalText(state, "namespace", properties.getNamespace());
        String plural = segment(ControlPlaneStateValues.text(state, "plural"));
        String name = segment(ControlPlaneStateValues.text(state, "name"));
        String prefix = apiVersion.contains("/") ? "/apis/" + apiVersion : "/api/" + apiVersion;
        return prefix + "/namespaces/" + segment(namespace) + '/' + plural + '/' + name;
    }

    private String segment(String value) {
        return UriUtils.encodePathSegment(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void authorize(HttpHeaders headers) {
        String token = bearerToken();
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
    }

    private String bearerToken() {
        if (properties.getBearerToken() != null && !properties.getBearerToken().isBlank()) {
            return properties.getBearerToken();
        }
        Path tokenFile = Path.of(properties.getBearerTokenFile());
        if (!Files.isRegularFile(tokenFile)) {
            return null;
        }
        try {
            return Files.readString(tokenFile).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read Kubernetes service-account token", exception);
        }
    }

    private SSLContext clusterSslContext(String certificateFile) {
        if (certificateFile == null || certificateFile.isBlank()) {
            return null;
        }
        Path path = Path.of(certificateFile);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (InputStream input = Files.newInputStream(path)) {
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(input);
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("kubernetes-cluster-ca", certificate);
            String trustManagerAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(trustManagerAlgorithm);
            trustManagers.init(trustStore);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagers.getTrustManagers(), null);
            return context;
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("cannot initialize Kubernetes cluster trust", exception);
        }
    }

    private Map<String, Object> withDigest(Map<String, Object> manifest, String digest) {
        Map<String, Object> copy = objectMapper.convertValue(manifest, JACKSON_MAP_TYPE);
        Map<String, Object> metadata = child(copy, "metadata");
        Map<String, Object> annotations = child(metadata, "annotations");
        annotations.put("control-plane.emall.com/desired-digest", digest);
        return copy;
    }

    private String desiredDigest(Map<String, Object> resource) {
        Object metadataValue = resource.get("metadata");
        if (!(metadataValue instanceof Map<?, ?> metadata)) {
            return null;
        }
        Object annotationsValue = metadata.get("annotations");
        if (!(annotationsValue instanceof Map<?, ?> annotations)) {
            return null;
        }
        Object value = annotations.get("control-plane.emall.com/desired-digest");
        return value == null ? null : value.toString();
    }

    private Map<String, Object> mutableResource(Map<String, Object> resource) {
        Map<String, Object> copy = objectMapper.convertValue(resource, JACKSON_MAP_TYPE);
        copy.remove("status");
        Map<String, Object> metadata = child(copy, "metadata");
        metadata.remove("creationTimestamp");
        metadata.remove("generation");
        metadata.remove("managedFields");
        metadata.remove("resourceVersion");
        metadata.remove("selfLink");
        metadata.remove("uid");
        return copy;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> child(Map<String, Object> parent, String name) {
        Object existing = parent.get(name);
        if (existing instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        Map<String, Object> child = new LinkedHashMap<>();
        parent.put(name, child);
        return child;
    }
}
