import React from 'react';
import { ApiError } from '../types';
import { ShieldX, Clock, ServerCrash, Hourglass, HelpCircle, CornerDownRight, Copy, Check } from 'lucide-react';

interface ErrorCardProps {
  error: ApiError & { requestId?: string };
}

export const ErrorCard: React.FC<ErrorCardProps> = ({ error }) => {
  const { code, message, requestId } = error;
  const [copied, setCopied] = React.useState(false);

  const copyRequestId = () => {
    if (requestId) {
      navigator.clipboard.writeText(requestId);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  // Classify errors to provide beautiful diagnostic messages and icons
  let title = 'Audit Failed';
  const description = message;
  let action = 'Please verify the URL and try again.';
  let Icon = HelpCircle;
  let colorClass = 'border-rose-500/20 bg-rose-500/5 text-rose-400';
  let iconBgClass = 'bg-rose-500/15 text-rose-400';

  switch (code) {
    case 'RATE_LIMIT_EXCEEDED':
      title = 'Rate Limit Exceeded (HTTP 429)';
      Icon = Clock;
      action = 'Please wait a minute before attempting another URL audit.';
      colorClass = 'border-amber-500/20 bg-amber-500/5 text-amber-400';
      iconBgClass = 'bg-amber-500/15 text-amber-400';
      break;
    case 'CONCURRENCY_LIMIT_EXCEEDED':
      title = 'Service Busy (HTTP 503)';
      Icon = ServerCrash;
      action = 'The server is handling maximum capacity. Please retry in a few seconds.';
      colorClass = 'border-amber-500/20 bg-amber-500/5 text-amber-400';
      iconBgClass = 'bg-amber-500/15 text-amber-400';
      break;
    case 'TIMEOUT':
      title = 'Gateway Timeout (HTTP 504)';
      Icon = Hourglass;
      action = 'The target website took too long to respond. Ensure it is accessible online.';
      break;
    case 'INVALID_URL':
    case 'INVALID_REQUEST':
      title = 'Invalid Request (HTTP 400)';
      Icon = ShieldX;
      action = 'Double-check the syntax of the URL (it must start with http:// or https://).';
      break;
    case 'CONNECTION_FAILURE':
    case 'DNS_FAILURE':
      title = 'Connection Failure (HTTP 502)';
      Icon = ServerCrash;
      action = 'The target server is offline, or the DNS resolution failed.';
      break;
    case 'UNEXPECTED_ERROR':
    default:
      title = 'Internal Server Error (HTTP 500)';
      Icon = ServerCrash;
      action = 'An unexpected backend error occurred. Please contact the administrator.';
      break;
  }

  return (
    <div className={`w-full border rounded-3xl p-6 md:p-8 backdrop-blur-md shadow-2xl relative overflow-hidden animate-in fade-in zoom-in-95 duration-300 ${colorClass}`}>
      <div className="absolute -top-12 -right-12 w-32 h-32 bg-current opacity-[0.03] rounded-full blur-3xl pointer-events-none" />

      <div className="flex items-start gap-4">
        <div className={`p-3 rounded-2xl shrink-0 ${iconBgClass}`}>
          <Icon size={24} />
        </div>
        <div className="flex-grow min-w-0">
          <h3 className="text-white text-lg font-bold">{title}</h3>
          <p className="text-gray-300 text-sm font-medium mt-2 leading-relaxed">
            {description}
          </p>

          <div className="mt-4 pt-4 border-t border-white/5 space-y-2">
            <div className="flex items-center gap-2 text-xs text-gray-400 font-medium">
              <CornerDownRight size={12} className="text-gray-500" />
              <span className="text-gray-500 uppercase tracking-wider font-semibold">Recommended Action:</span>
              <span className="text-gray-300">{action}</span>
            </div>

            {requestId && (
              <div className="flex flex-wrap items-center justify-between gap-2 bg-white/[0.02] border border-white/5 rounded-xl px-3.5 py-2 mt-3">
                <div className="flex items-center gap-2 text-xs text-gray-500 font-medium">
                  <span className="uppercase tracking-wider font-bold">Correlation ID:</span>
                  <code className="text-gray-300 select-all font-mono">{requestId}</code>
                </div>
                <button
                  onClick={copyRequestId}
                  className="p-1 hover:bg-white/5 rounded text-gray-400 hover:text-white transition-colors cursor-pointer"
                  title="Copy correlation ID"
                >
                  {copied ? <Check size={14} className="text-emerald-400" /> : <Copy size={14} />}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
