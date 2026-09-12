import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiResponse } from '../types';

let accessToken: string | null = localStorage.getItem('foundryos_access_token') || localStorage.getItem('factoryos_access_token');
let activePlantId: string | null = localStorage.getItem('foundryos_active_plant_id') || localStorage.getItem('factoryos_active_plant_id');
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else if (token) {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

export const setStoredAccessToken = (token: string | null) => {
  accessToken = token;
  if (token) {
    localStorage.setItem('foundryos_access_token', token);
  } else {
    localStorage.removeItem('foundryos_access_token');
    localStorage.removeItem('factoryos_access_token');
  }
};

export const getStoredAccessToken = () => accessToken;

export const setStoredActivePlantId = (plantId: string | null) => {
  activePlantId = plantId;
  if (plantId) {
    localStorage.setItem('foundryos_active_plant_id', plantId);
  } else {
    localStorage.removeItem('foundryos_active_plant_id');
    localStorage.removeItem('factoryos_active_plant_id');
  }
};

export const getStoredActivePlantId = () => activePlantId;

export const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Send HttpOnly refreshToken cookie
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (accessToken) {
      config.headers.set('Authorization', `Bearer ${accessToken}`);
    }
    if (activePlantId) {
      config.headers.set('X-Plant-ID', activePlantId);
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (!originalRequest || !error.response) {
      return Promise.reject(error);
    }

    // If 401 and not already a retry or refresh endpoint call
    if (error.response.status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/auth/login') && !originalRequest.url?.includes('/auth/refresh')) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.set('Authorization', `Bearer ${token}`);
            return apiClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const { data } = await axios.post<ApiResponse<{ accessToken: string }>>(
          '/api/v1/auth/refresh',
          {},
          { withCredentials: true }
        );

        const newAccessToken = data.data.accessToken;
        setStoredAccessToken(newAccessToken);
        processQueue(null, newAccessToken);
        originalRequest.headers.set('Authorization', `Bearer ${newAccessToken}`);
        return apiClient(originalRequest);
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        setStoredAccessToken(null);
        window.dispatchEvent(new Event('auth:unauthorized'));
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

// Type-safe convenience wrappers
export const api = {
  get: async <T>(url: string, params?: Record<string, unknown>): Promise<T> => {
    const config = { params, ...(url.startsWith('/api/') ? { baseURL: '' } : {}) };
    const response = await apiClient.get<ApiResponse<T>>(url, config);
    return response.data.data;
  },
  post: async <T>(url: string, body?: unknown): Promise<T> => {
    const config = url.startsWith('/api/') ? { baseURL: '' } : {};
    const response = await apiClient.post<ApiResponse<T>>(url, body, config);
    return response.data.data;
  },
  put: async <T>(url: string, body?: unknown): Promise<T> => {
    const config = url.startsWith('/api/') ? { baseURL: '' } : {};
    const response = await apiClient.put<ApiResponse<T>>(url, body, config);
    return response.data.data;
  },
  patch: async <T>(url: string, body?: unknown): Promise<T> => {
    const config = url.startsWith('/api/') ? { baseURL: '' } : {};
    const response = await apiClient.patch<ApiResponse<T>>(url, body, config);
    return response.data.data;
  },
  delete: async <T>(url: string, params?: Record<string, unknown>): Promise<T> => {
    const config = { params, ...(url.startsWith('/api/') ? { baseURL: '' } : {}) };
    const response = await apiClient.delete<ApiResponse<T>>(url, config);
    return response.data.data;
  },
};

// Telemetry & IIoT API (v2)
export const telemetryApi = {
  getLiveTelemetry: (machineId: string) => 
    api.get<import('../types').MachineLiveTelemetry>(`/api/v2/telemetry/machines/${machineId}/live`),
  
  getTagMappings: (machineId: string) => 
    api.get<import('../types').TagMapping[]>(`/api/v2/telemetry/machines/${machineId}/tags`),

  createTagMapping: (machineId: string, data: import('../types').CreateTagMappingRequest) => 
    api.post<import('../types').TagMapping>(`/api/v2/telemetry/machines/${machineId}/tags`, data),

  deleteTagMapping: (machineId: string, mappingId: string) => 
    api.delete<void>(`/api/v2/telemetry/machines/${machineId}/tags/${mappingId}`),

  ingestBatch: (data: import('../types').TelemetryBatchIngestRequest) => 
    api.post<import('../types').TelemetryIngestResponse>('/api/v2/telemetry/ingest', data),

  getHistory: (machineId: string, limit = 50) => 
    api.get<import('../types').TelemetryPoint[]>(`/api/v2/telemetry/machines/${machineId}/history`, { limit }),

  getTimeSeries: (machineId: string, tag: string, params?: { from?: string; to?: string; bucket?: string }) =>
    api.get<import('../types').TimeSeriesResponse>(`/api/v2/telemetry/machines/${machineId}/series`, { tag, ...params }),

  executeRetention: () =>
    api.post<import('../types').RetentionReport>('/api/v2/telemetry/retention/execute'),
};

// Downtime & Micro-Stop API (v2)
export const downtimeApi = {
  getPendingRootCauses: () =>
    api.get<import('../types').DowntimeEventDto[]>('/api/v2/downtime/pending-root-causes'),

  acknowledgeRootCause: (eventId: string, data: import('../types').AcknowledgeRootCauseRequest) =>
    api.post<import('../types').DowntimeEventDto>(`/api/v2/downtime/events/${eventId}/acknowledge-root-cause`, data),

  getMicroStopSummary: (machineId: string, params?: { from?: string; to?: string }) =>
    api.get<import('../types').MicroStopSummary>(`/api/v2/downtime/machines/${machineId}/micro-stops`, params),

  evaluateStream: (machineId: string) =>
    api.post<import('../types').AutomatedEvaluationResult>(`/api/v2/downtime/machines/${machineId}/evaluate`),
};

// Barcode & Traceability API (v2 Epic 8)
export const barcodeApi = {
  scan: (data: import('../types').BarcodeScanRequest) =>
    api.post<import('../types').BarcodeScanResponse>('/api/v2/barcode/scan', data),

  getLogs: () =>
    api.get<import('../types').BarcodeScanLogDto[]>('/api/v2/barcode/logs'),

  getBom: (productCode: string) =>
    api.get<import('../types').BomItemDto[]>(`/api/v2/barcode/bom/${productCode}`),
};

// Hierarchy & Multi-Tenant API (v2 Epic 5)
export const hierarchyApi = {
  getEnterprises: () =>
    api.get<import('../types').EnterpriseDto[]>('/api/v2/hierarchy/enterprises'),

  getEnterprise: (id: string) =>
    api.get<import('../types').EnterpriseDto>(`/api/v2/hierarchy/enterprises/${id}`),

  getAuthorizedPlants: () =>
    api.get<import('../types').PlantDto[]>('/api/v2/hierarchy/plants'),

  getPlantHierarchyTree: (plantId: string) =>
    api.get<import('../types').HierarchyTreeDto>(`/api/v2/hierarchy/plants/${plantId}/tree`),

  createPlant: (data: import('../types').CreatePlantRequest) =>
    api.post<import('../types').PlantDto>('/api/v2/hierarchy/plants', data),

  createArea: (plantId: string, data: import('../types').CreateAreaRequest) =>
    api.post<import('../types').ProductionAreaDto>(`/api/v2/hierarchy/plants/${plantId}/areas`, data),

  createLine: (areaId: string, data: import('../types').CreateLineRequest) =>
    api.post<import('../types').ProductionLineDto>(`/api/v2/hierarchy/areas/${areaId}/lines`, data),

  createWorkCell: (lineId: string, data: import('../types').CreateWorkCellRequest) =>
    api.post<import('../types').WorkCellDto>(`/api/v2/hierarchy/lines/${lineId}/cells`, data),

  getUserMemberships: (userId: string) =>
    api.get<import('../types').UserPlantMembershipDto[]>(`/api/v2/hierarchy/users/${userId}/memberships`),

  assignMembership: (data: import('../types').AssignPlantMembershipRequest) =>
    api.post<import('../types').UserPlantMembershipDto>('/api/v2/hierarchy/memberships', data),
};

// Auth API helpers
export const authApi = {
  switchPlant: (plantId: string) =>
    api.post<import('../types').LoginResponse>('/auth/switch-plant', { plantId }),
};

// Enterprise Fleet Analytics & Benchmarking API (v2 Epic 5 Story 2)
export const enterpriseAnalyticsApi = {
  getOeeMatrix: (params?: { interval?: string; enterpriseId?: string; status?: string }) =>
    api.get<import('../types').EnterpriseOeeMatrixDto>('/api/v2/analytics/enterprise/oee-matrix', params),

  downloadExport: async (format: 'csv' | 'pdf', interval: string = '24H') => {
    const url = `/api/v2/analytics/enterprise/export/${format}?interval=${encodeURIComponent(interval)}`;
    const response = await apiClient.get(url, {
      responseType: 'blob',
      baseURL: '',
    });
    
    // Trigger browser file download
    const blob = new Blob([response.data], {
      type: format === 'csv' ? 'text/csv;charset=utf-8;' : 'application/pdf',
    });
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.setAttribute('download', `factoryos_fleet_benchmark_${interval.toLowerCase()}_${new Date().toISOString().slice(0, 10)}.${format}`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(downloadUrl);
  },

  triggerScheduledReport: (format: 'CSV' | 'PDF' = 'PDF', interval: string = '24H') =>
    api.post<import('../types').ScheduledReportResponseDto>(`/api/v2/analytics/enterprise/reports/scheduled?format=${format}&interval=${interval}`),
};

// Edge Store-and-Forward & Resilience API (v2 Epic 5 Story 3)
export const edgeApi = {
  getGateways: (plantId?: string) =>
    api.get<import('../types').EdgeGatewayDto[]>('/api/v2/edge/gateways', plantId ? { plantId } : undefined),

  getGateway: (code: string) =>
    api.get<import('../types').EdgeGatewayDto>(`/api/v2/edge/gateways/${code}`),

  createGateway: (data: import('../types').EdgeGatewayCreateRequest) =>
    api.post<import('../types').EdgeGatewayDto>('/api/v2/edge/gateways', data),

  recordHeartbeat: (code: string, data?: import('../types').EdgeGatewayHeartbeatRequest) =>
    api.post<import('../types').EdgeGatewayDto>(`/api/v2/edge/gateways/${code}/heartbeat`, data),

  getManifest: (code: string) =>
    api.get<import('../types').EdgeOfflineCacheManifestDto>(`/api/v2/edge/gateways/${code}/manifest`),

  syncBatch: (batch: import('../types').EdgeSyncBatchRequestDto) =>
    api.post<import('../types').EdgeSyncBatchResultDto>('/api/v2/edge/sync/batch', batch),

  getBatches: () =>
    api.get<import('../types').EdgeSyncBatchResultDto[]>('/api/v2/edge/batches'),

  getGatewayBatches: (code: string) =>
    api.get<import('../types').EdgeSyncBatchResultDto[]>(`/api/v2/edge/gateways/${code}/batches`),

  getBatchTransactions: (batchId: string) =>
    api.get<import('../types').EdgeTransactionLogDto[]>(`/api/v2/edge/batches/${batchId}/transactions`),

  getRecentTransactions: () =>
    api.get<import('../types').EdgeTransactionLogDto[]>('/api/v2/edge/transactions/recent'),

  simulateDisconnect: (data: import('../types').SimulateDisconnectRequestDto) =>
    api.post<import('../types').EdgeSyncBatchResultDto>('/api/v2/edge/simulate-disconnect', data),
};

// Digital SOP & Quality Gate API (v2 Epic 8 Story 2)
export const sopApi = {
  getSops: (params?: { productCode?: string; category?: string }) =>
    api.get<import('../types').SopDto[]>('/api/v2/sop/templates', params),

  getSopById: (id: string) =>
    api.get<import('../types').SopDto>(`/api/v2/sop/templates/${id}`),

  getSopByCode: (code: string) =>
    api.get<import('../types').SopDto>(`/api/v2/sop/templates/code/${code}`),

  createSop: (data: Partial<import('../types').SopDto>) =>
    api.post<import('../types').SopDto>('/api/v2/sop/templates', data),

  startSession: (data: import('../types').StartSopSessionRequestDto) =>
    api.post<import('../types').SopExecutionSessionDto>('/api/v2/sop/sessions/start', data),

  getSessionById: (sessionId: string) =>
    api.get<import('../types').SopExecutionSessionDto>(`/api/v2/sop/sessions/${sessionId}`),

  getSessionsForOrder: (orderId: string) =>
    api.get<import('../types').SopExecutionSessionDto[]>(`/api/v2/sop/sessions/order/${orderId}`),

  recordStepExecution: (sessionId: string, data: import('../types').RecordStepExecutionRequestDto) =>
    api.post<import('../types').SopStepExecutionRecordDto>(`/api/v2/sop/sessions/${sessionId}/steps`, data),

  signOffSession: (sessionId: string, data: import('../types').QualitySignOffRequestDto) =>
    api.post<import('../types').SopExecutionSessionDto>(`/api/v2/sop/sessions/${sessionId}/sign-off`, data),

  getGateStatusForOrder: (orderId: string) =>
    api.get<import('../types').QualityGateStatusDto>(`/api/v2/sop/gates/order/${orderId}`),

  signOffGate: (orderId: string, data: import('../types').QualitySignOffRequestDto) =>
    api.post<import('../types').QualityGateStatusDto>(`/api/v2/sop/gates/order/${orderId}/sign-off`, data),
};

// Spindle Vibration FFT & Machine Health API (v2 Epic 6 Story 1)
export const vibrationApi = {
  analyzeBurst: (data: import('../types').VibrationBurstIngestDto) =>
    api.post<import('../types').MachineHealthAssessmentDto>('/api/v2/vibration/analyze', data),

  simulateBurst: (data: import('../types').SimulateBurstRequestDto) =>
    api.post<import('../types').MachineHealthAssessmentDto>('/api/v2/vibration/simulate-burst', data),

  getSpectrum: (machineId: string) =>
    api.get<import('../types').FftSpectrumDto>(`/api/v2/vibration/machines/${machineId}/spectrum`),

  getHealthAssessment: (machineId: string) =>
    api.get<import('../types').MachineHealthAssessmentDto>(`/api/v2/vibration/machines/${machineId}/health`),

  getHealthHistory: (machineId: string) =>
    api.get<import('../types').MachineHealthAssessmentDto[]>(`/api/v2/vibration/machines/${machineId}/history`),

  getFleetHealthSummary: (plantId?: string) =>
    api.get<import('../types').FleetHealthSummaryDto>('/api/v2/vibration/fleet/health-summary', plantId ? { plantId } : undefined),
};

// ERP Synchronization Hub API (v2 Epic 7 Story 1)
export const erpApi = {
  getConnectors: () =>
    api.get<import('../types').ErpConnectorDto[]>('/api/v2/erp/connectors'),

  getConnectorById: (id: string) =>
    api.get<import('../types').ErpConnectorDto>(`/api/v2/erp/connectors/${id}`),

  createConnector: (data: import('../types').CreateErpConnectorRequest) =>
    api.post<import('../types').ErpConnectorDto>('/api/v2/erp/connectors', data),

  testConnector: (id: string) =>
    api.post<{ connectorId: string; success: boolean; status: string }>(`/api/v2/erp/connectors/${id}/test`),

  syncInbound: (id: string) =>
    api.post<{ connectorId: string; syncedCount: number; orders: string[] }>(`/api/v2/erp/connectors/${id}/sync-inbound`),

  submitConfirmation: (orderId: string, data: { confirmedGoodQty: number; confirmedScrapQty: number; scrapReason?: string; laborHours?: number; machineHours?: number }) =>
    api.post<import('../types').ErpOrderConfirmationDto>(`/api/v2/erp/orders/${orderId}/confirm`, data),

  getSyncLogs: () =>
    api.get<import('../types').ErpSyncLogDto[]>('/api/v2/erp/sync-logs'),

  getConfirmations: () =>
    api.get<import('../types').ErpOrderConfirmationDto[]>('/api/v2/erp/confirmations'),
};

// Material Backflushing & BOM Explosion API (v2 Epic 7 Story 2)
export const materialsApi = {
  getAllMaterials: () =>
    api.get<import('../types').MaterialDto[]>('/api/v2/materials'),

  getBomExplosion: (productCode: string, plannedQuantity: number = 100) =>
    api.get<import('../types').BomExplosionDto>(`/api/v2/materials/bom/${encodeURIComponent(productCode)}/explosion`, { plannedQuantity }),

  recordOutput: (orderId: string, data: import('../types').RecordProductionOutputRequest) =>
    api.post<import('../types').MaterialConsumptionRecordDto[]>(`/api/v2/materials/orders/${orderId}/record-output`, data),

  getConsumption: (orderId?: string) =>
    api.get<import('../types').MaterialConsumptionRecordDto[]>('/api/v2/materials/consumption', orderId ? { orderId } : undefined),

  getVarianceAlerts: () =>
    api.get<import('../types').MaterialConsumptionRecordDto[]>('/api/v2/materials/variance-alerts'),
};



