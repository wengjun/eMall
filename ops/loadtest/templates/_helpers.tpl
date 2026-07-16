{{- define "emall-loadtest.name" -}}
{{- printf "%s-%s" .Release.Name (.Values.runId | replace "_" "-" | replace "." "-") | trunc 52 | trimSuffix "-" -}}
{{- end -}}

{{- define "emall-loadtest.reportClaim" -}}
{{- if .Values.reports.existingClaim -}}
{{- .Values.reports.existingClaim -}}
{{- else -}}
{{- printf "%s-reports" (include "emall-loadtest.name" .) -}}
{{- end -}}
{{- end -}}

{{- define "emall-loadtest.commonEnv" -}}
- name: EMALL_BASE_URL
  value: {{ .Values.baseUrl | quote }}
- name: EMALL_LOAD_RUN_ID
  value: {{ .Values.runId | quote }}
- name: EMALL_LOAD_REPORT_DIR
  value: /reports
- name: EMALL_LOAD_RATE
  value: {{ .Values.ratePerSecond | quote }}
- name: EMALL_LOAD_DURATION_SECONDS
  value: {{ .Values.durationSeconds | quote }}
- name: EMALL_LOAD_MAX_INFLIGHT
  value: {{ .Values.maxInflightPerWorker | quote }}
- name: EMALL_LOAD_SCENARIO
  value: {{ .Values.scenario | quote }}
- name: EMALL_LOAD_PATTERN
  value: {{ .Values.pattern | quote }}
- name: EMALL_LOAD_BOOTSTRAP_DATA
  value: "false"
- name: EMALL_LOAD_AUTH_TOKEN
  {{- if .Values.auth.existingSecret }}
  valueFrom:
    secretKeyRef:
      name: {{ .Values.auth.existingSecret | quote }}
      key: {{ .Values.auth.tokenKey | quote }}
  {{- else }}
  value: ""
  {{- end }}
- name: EMALL_LOAD_PAYMENT_CALLBACK_SECRET
  {{- if .Values.auth.existingSecret }}
  valueFrom:
    secretKeyRef:
      name: {{ .Values.auth.existingSecret | quote }}
      key: {{ .Values.auth.paymentCallbackSecretKey | quote }}
  {{- else }}
  value: ""
  {{- end }}
- name: EMALL_LOAD_PAYMENT_ID_BASE
  value: {{ .Values.paymentFixtures.paymentIdBase | quote }}
- name: EMALL_LOAD_PAYMENT_TRADE_NO_PREFIX
  value: {{ .Values.paymentFixtures.tradeNoPrefix | quote }}
- name: EMALL_LOAD_PAYMENT_CHANNEL
  value: {{ .Values.paymentFixtures.channel | quote }}
{{- if .Values.identityFixtures.existingClaim }}
- name: EMALL_LOAD_IDENTITY_FIXTURE_FILE
  value: {{ .Values.identityFixtures.fileTemplate | quote }}
{{- end }}
- name: EMALL_LOAD_USER_CARDINALITY
  value: {{ .Values.traffic.userCardinality | quote }}
- name: EMALL_LOAD_SKU_CARDINALITY
  value: {{ .Values.traffic.skuCardinality | quote }}
- name: EMALL_LOAD_HOT_SKU_PERCENT
  value: {{ .Values.traffic.hotSkuPercent | quote }}
- name: EMALL_LOAD_TRAFFIC_MIX
  value: {{ .Values.traffic.mix | quote }}
- name: EMALL_LOAD_MAX_ERROR_RATE
  value: {{ .Values.thresholds.maxErrorRate | quote }}
- name: EMALL_LOAD_MAX_P95_MS
  value: {{ .Values.thresholds.maxP95Millis | quote }}
- name: EMALL_LOAD_MAX_SCHEDULER_LAG_MS
  value: {{ .Values.thresholds.maxSchedulerLagMillis | quote }}
- name: EMALL_LOAD_MAX_GENERATOR_CPU
  value: {{ .Values.thresholds.maxGeneratorCpu | quote }}
- name: EMALL_LOAD_ENVIRONMENT
  value: {{ .Values.evidence.environment | quote }}
- name: EMALL_LOAD_EVIDENCE_SCOPE
  value: {{ .Values.evidence.scope | quote }}
- name: EMALL_LOAD_GIT_COMMIT
  value: {{ .Values.evidence.gitCommit | quote }}
- name: EMALL_LOAD_DEPLOYMENT
  value: {{ .Values.evidence.deployment | quote }}
- name: EMALL_LOAD_TARGET_RESOURCES
  value: {{ .Values.evidence.targetResources | quote }}
- name: EMALL_LOAD_DATASET_USERS
  value: {{ .Values.evidence.datasetUsers | quote }}
- name: EMALL_LOAD_DATASET_SKUS
  value: {{ .Values.evidence.datasetSkus | quote }}
- name: EMALL_LOAD_SERVICE_INSTANCES
  value: {{ .Values.evidence.serviceInstances | quote }}
- name: EMALL_LOAD_CELL_COUNT
  value: {{ .Values.capacityModel.projectedCells | quote }}
- name: EMALL_LOAD_SCALING_EFFICIENCY
  value: {{ .Values.capacityModel.scalingEfficiency | quote }}
- name: EMALL_LOAD_CAPACITY_HEADROOM
  value: {{ .Values.capacityModel.headroom | quote }}
- name: EMALL_LOAD_TARGET_PEAK_CONCURRENCY
  value: {{ .Values.capacityModel.targetPeakConcurrency | quote }}
- name: EMALL_LOAD_SESSION_THINK_SECONDS
  value: {{ .Values.capacityModel.sessionThinkSeconds | quote }}
- name: EMALL_LOAD_FAULT_EXPERIMENT
  value: {{ .Values.evidence.faultExperiment | quote }}
{{- end -}}
