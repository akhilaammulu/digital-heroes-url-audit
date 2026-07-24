import React from 'react';
import { AuditResponse } from '../types';
import { Globe, Clock, FileText, Calendar, ShieldCheck, Zap } from 'lucide-react';

interface ResultCardProps {
  result: AuditResponse;
}

export const ResultCard: React.FC<ResultCardProps> = ({ result }) => {
  const { url, httpStatus, responseTimeMs, pageTitle, timestamp } = result;

  // Determine status styling
  const isSuccess = httpStatus >= 200 && httpStatus < 300;
  const isRedirect = httpStatus >= 300 && httpStatus < 400;
  
  let statusColorClass = 'text-rose-400 bg-rose-500/10 border-rose-500/20';
  let statusText = 'Error';
  if (isSuccess) {
    statusColorClass = 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20';
    statusText = 'Success';
  } else if (isRedirect) {
    statusColorClass = 'text-amber-400 bg-amber-500/10 border-amber-500/20';
    statusText = 'Redirect';
  }

  // Determine speed performance badge
  let speedColorClass = 'text-emerald-400';
  let speedLabel = 'Fast';
  if (responseTimeMs > 1000) {
    speedColorClass = 'text-rose-400';
    speedLabel = 'Slow';
  } else if (responseTimeMs > 300) {
    speedColorClass = 'text-amber-400';
    speedLabel = 'Average';
  }

  return (
    <div className="w-full bg-white/[0.02] border border-white/5 rounded-3xl p-6 md:p-8 backdrop-blur-md shadow-2xl relative overflow-hidden animate-in fade-in zoom-in-95 duration-300">
      {/* Decorative gradient blur */}
      <div className="absolute -top-12 -right-12 w-32 h-32 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-white/5">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-emerald-500/15 text-emerald-400 rounded-xl">
            <ShieldCheck size={24} />
          </div>
          <div>
            <h3 className="text-white text-lg font-bold">Audit Completed</h3>
            <p className="text-gray-400 text-xs mt-0.5">URL scanned and verified successfully</p>
          </div>
        </div>

        <div className={`self-start md:self-auto px-4 py-2 rounded-2xl border text-sm font-semibold flex items-center gap-1.5 ${statusColorClass}`}>
          <span className="w-2 h-2 rounded-full bg-current animate-ping" />
          <span>HTTP {httpStatus} - {statusText}</span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-6">
        {/* URL Row */}
        <div className="flex gap-3">
          <div className="p-2 bg-white/[0.04] text-gray-400 rounded-lg self-start">
            <Globe size={18} />
          </div>
          <div className="min-w-0">
            <span className="text-gray-500 text-xs font-semibold uppercase tracking-wider block">Audited URL</span>
            <a
              href={url}
              target="_blank"
              rel="noopener noreferrer"
              className="text-white text-sm hover:text-indigo-400 transition-colors block break-all font-medium mt-1 underline decoration-white/20 hover:decoration-indigo-400/50"
            >
              {url}
            </a>
          </div>
        </div>

        {/* Response Time Row */}
        <div className="flex gap-3">
          <div className="p-2 bg-white/[0.04] text-gray-400 rounded-lg self-start">
            <Clock size={18} />
          </div>
          <div>
            <span className="text-gray-500 text-xs font-semibold uppercase tracking-wider block">Response Time</span>
            <div className="flex items-baseline gap-2 mt-1">
              <span className="text-white text-lg font-bold">{responseTimeMs} ms</span>
              <span className={`text-xs font-medium px-2 py-0.5 rounded bg-white/[0.03] flex items-center gap-1 ${speedColorClass}`}>
                <Zap size={10} className="fill-current" />
                {speedLabel}
              </span>
            </div>
          </div>
        </div>

        {/* Page Title Row */}
        <div className="flex gap-3 md:col-span-2">
          <div className="p-2 bg-white/[0.04] text-gray-400 rounded-lg self-start">
            <FileText size={18} />
          </div>
          <div>
            <span className="text-gray-500 text-xs font-semibold uppercase tracking-wider block">Page Title</span>
            <span className={`text-sm block mt-1 leading-relaxed ${pageTitle ? 'text-white font-medium' : 'text-gray-500 italic'}`}>
              {pageTitle || 'No title tag found on target page'}
            </span>
          </div>
        </div>

        {/* Timestamp Row */}
        <div className="flex gap-3 md:col-span-2">
          <div className="p-2 bg-white/[0.04] text-gray-400 rounded-lg self-start">
            <Calendar size={18} />
          </div>
          <div>
            <span className="text-gray-500 text-xs font-semibold uppercase tracking-wider block">Scanned At</span>
            <span className="text-white text-sm font-medium block mt-1">
              {new Date(timestamp).toLocaleString(undefined, {
                dateStyle: 'medium',
                timeStyle: 'medium',
              })}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
