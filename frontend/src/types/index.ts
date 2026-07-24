export interface ApiError {
  code: string;
  message: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: ApiError | null;
  timestamp: string;
  requestId: string;
}

export interface AuditResponse {
  url: string;
  httpStatus: number;
  responseTimeMs: number;
  pageTitle: string | null;
  timestamp: string;
}

export interface AuditRequest {
  url: string;
}
