import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiResponse } from '../types';

let accessToken: string | null = localStorage.getItem('factoryos_access_token');
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
    const response = await apiClient.get<ApiResponse<T>>(url, { params });
    return response.data.data;
  },
  post: async <T>(url: string, body?: unknown): Promise<T> => {
    const response = await apiClient.post<ApiResponse<T>>(url, body);
    return response.data.data;
  },
  put: async <T>(url: string, body?: unknown): Promise<T> => {
    const response = await apiClient.put<ApiResponse<T>>(url, body);
    return response.data.data;
  },
  patch: async <T>(url: string, body?: unknown): Promise<T> => {
    const response = await apiClient.patch<ApiResponse<T>>(url, body);
    return response.data.data;
  },
  delete: async <T>(url: string, params?: Record<string, unknown>): Promise<T> => {
    const response = await apiClient.delete<ApiResponse<T>>(url, { params });
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
};

