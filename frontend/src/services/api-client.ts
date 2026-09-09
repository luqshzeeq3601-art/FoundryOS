import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiResponse } from '../types';

let accessToken: string | null = localStorage.getItem('factoryos_access_token');
let activePlantId: string | null = localStorage.getItem('factoryos_active_plant_id');
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
    localStorage.setItem('factoryos_access_token', token);
  } else {
    localStorage.removeItem('factoryos_access_token');
  }
};

export const getStoredAccessToken = () => accessToken;

export const setStoredActivePlantId = (plantId: string | null) => {
  activePlantId = plantId;
  if (plantId) {
    localStorage.setItem('factoryos_active_plant_id', plantId);
  } else {
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


