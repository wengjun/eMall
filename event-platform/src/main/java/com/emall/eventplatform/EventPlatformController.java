package com.emall.eventplatform;

import com.emall.common.api.ApiResponse;
import com.emall.common.privacy.SensitiveDataType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/event-platform")
class EventPlatformController {
    private final EventPlatformService eventPlatformService;

    EventPlatformController(EventPlatformService eventPlatformService) {
        this.eventPlatformService = eventPlatformService;
    }

    @PostMapping("/schemas")
    ApiResponse<EventSchema> registerSchema(@Valid @RequestBody RegisterSchemaRequest request) {
        return ApiResponse.ok(eventPlatformService.registerSchema(request.eventName(), request.version(),
                request.owner(), request.jsonSchema()));
    }

    @PatchMapping("/schemas/activate")
    ApiResponse<EventSchema> activateSchema(@Valid @RequestBody ActivateSchemaRequest request) {
        return ApiResponse.ok(eventPlatformService.activateSchema(request.eventName(), request.version()));
    }

    @PostMapping("/field-classifications")
    ApiResponse<EventFieldClassification> classifyField(@Valid @RequestBody ClassifyFieldRequest request) {
        return ApiResponse.ok(eventPlatformService.classifyField(request.eventName(), request.version(),
                request.fieldName(), request.sensitivity(), request.required(), request.exportedToWarehouse()));
    }

    @PostMapping("/events")
    ApiResponse<TrackingEvent> ingestEvent(@Valid @RequestBody IngestEventRequest request) {
        return ApiResponse.ok(eventPlatformService.ingestEvent(request.eventName(), request.version(),
                request.eventKey(), request.userKey(), request.payload(), request.occurredAt()));
    }

    @PostMapping("/offsets")
    ApiResponse<PipelineOffset> commitOffset(@Valid @RequestBody CommitOffsetRequest request) {
        return ApiResponse.ok(eventPlatformService.commitOffset(request.consumerGroup(), request.topicName(),
                request.processedOffset()));
    }

    @PostMapping("/materializations")
    ApiResponse<MetricMaterialization> materializeMetric(@Valid @RequestBody MaterializeMetricRequest request) {
        return ApiResponse.ok(
                eventPlatformService.materializeMetric(request.eventName(), request.metricName(), request.windowKey()));
    }

    @GetMapping("/summary")
    ApiResponse<EventPlatformSummary> summary() {
        return ApiResponse.ok(eventPlatformService.summary());
    }

    record RegisterSchemaRequest(@NotBlank String eventName, @Min(1) int version, @NotBlank String owner,
            @NotBlank String jsonSchema) {
    }

    record ActivateSchemaRequest(@NotBlank String eventName, @Min(1) int version) {
    }

    record ClassifyFieldRequest(@NotBlank String eventName, @Min(1) int version, @NotBlank String fieldName,
            @NotNull SensitiveDataType sensitivity, boolean required, boolean exportedToWarehouse) {
    }

    record IngestEventRequest(@NotBlank String eventName, @Min(1) int version, @NotBlank String eventKey,
            @NotBlank String userKey, @NotBlank String payload, Instant occurredAt) {
    }

    record CommitOffsetRequest(@NotBlank String consumerGroup, @NotBlank String topicName,
            @Min(0) long processedOffset) {
    }

    record MaterializeMetricRequest(@NotBlank String eventName, @NotBlank String metricName,
            @NotBlank String windowKey) {
    }
}
