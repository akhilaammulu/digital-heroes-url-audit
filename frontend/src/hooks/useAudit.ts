import { useState } from 'react';
import { auditService } from '../services/auditService';
import { AuditResponse, ApiError } from '../types';

export const useAudit = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<AuditResponse | null>(null);
  const [error, setError] = useState<(ApiError & { requestId?: string }) | null>(null);

  const executeAudit = async (url: string) => {
    setIsLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await auditService.auditUrl({ url });
      if (response.success && response.data) {
        setResult(response.data);
      } else if (response.error) {
        setError({
          ...response.error,
          requestId: response.requestId,
        });
      }
    } catch {
      setError({
        code: 'UNEXPECTED_ERROR',
        message: 'An unexpected error occurred.',
        requestId: 'local-error',
      });
    } finally {
      setIsLoading(false);
    }
  };

  return {
    isLoading,
    result,
    error,
    executeAudit,
    clearState: () => {
      setResult(null);
      setError(null);
    },
  };
};
