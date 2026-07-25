import axios from 'axios';
import { ApiResponse, AuditResponse, AuditRequest } from '../types';

let apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

if (apiBaseUrl && !apiBaseUrl.startsWith('http://') && !apiBaseUrl.startsWith('https://')) {
  apiBaseUrl = `https://${apiBaseUrl}`;
}

const apiClient = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const auditService = {
  auditUrl: async (request: AuditRequest): Promise<ApiResponse<AuditResponse>> => {
    try {
      const response = await apiClient.post<ApiResponse<AuditResponse>>('/api/v1/audit', request);
      return response.data;
    } catch (error: unknown) {
      if (axios.isAxiosError(error) && error.response) {
        // Return the structured error response from the backend if available
        if (error.response.data && typeof error.response.data === 'object' && 'success' in error.response.data) {
          return error.response.data as ApiResponse<AuditResponse>;
        }
        
        // Otherwise parse manual status code mapping
        const status = error.response.status;
        let code = 'UNEXPECTED_ERROR';
        let message = 'An unexpected error occurred';
        
        if (status === 429) {
          code = 'RATE_LIMIT_EXCEEDED';
          message = 'Rate limit exceeded. Maximum 100 requests per minute per IP.';
        } else if (status === 503) {
          code = 'CONCURRENCY_LIMIT_EXCEEDED';
          message = 'Too many concurrent audits. Please try again later.';
        } else if (status === 504) {
          code = 'TIMEOUT';
          message = 'The URL request timed out.';
        } else if (status === 400) {
          code = 'INVALID_REQUEST';
          message = 'Request validation failed.';
        }

        return {
          success: false,
          data: null,
          error: { code, message },
          timestamp: new Date().toISOString(),
          requestId: error.response.headers?.['x-request-id'] || 'no-request-id',
        };
      }

      // Network errors (server down)
      return {
        success: false,
        data: null,
        error: {
          code: 'CONNECTION_FAILURE',
          message: 'Could not connect to the audit service. Please ensure the backend is running.',
        },
        timestamp: new Date().toISOString(),
        requestId: 'network-error',
      };
    }
  },
};
