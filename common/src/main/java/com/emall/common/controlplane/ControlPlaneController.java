package com.emall.common.controlplane;

import com.emall.common.api.ApiResponse;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/control-plane")
@ConditionalOnBean({ControlPlaneClient.class, ControlPlaneReconciler.class})
public class ControlPlaneController {
    private final ControlPlaneClient client;
    private final ControlPlaneReconciler reconciler;

    public ControlPlaneController(ControlPlaneClient client, ControlPlaneReconciler reconciler) {
        this.client = client;
        this.reconciler = reconciler;
    }

    @GetMapping("/operations/{operationId}")
    ApiResponse<ControlPlaneOperation> operation(@PathVariable String operationId) {
        return ApiResponse.ok(client.find(operationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "control-plane operation not found")));
    }

    @GetMapping("/operations/latest")
    ApiResponse<ControlPlaneOperation> latest(@RequestParam String module, @RequestParam String resourceType,
            @RequestParam String resourceId) {
        return ApiResponse.ok(client.findLatest(module, resourceType, resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "control-plane operation not found")));
    }

    @PostMapping("/operations/{operationId}/reconcile")
    ApiResponse<ControlPlaneOperation> reconcile(@PathVariable String operationId) {
        return ApiResponse.ok(reconciler.reconcileNow(operationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "control-plane operation not found")));
    }
}
