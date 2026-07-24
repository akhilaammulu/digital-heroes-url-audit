import React from 'react';
import { Shield, Sparkles } from 'lucide-react';

export const Header: React.FC = () => {
  return (
    <header className="w-full max-w-4xl mx-auto mb-10 text-center">
      <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs font-semibold uppercase tracking-wider mb-6 animate-pulse">
        <Sparkles size={14} />
        <span>Digital Heroes Security</span>
      </div>
      <div className="flex items-center justify-center gap-3 mb-4">
        <div className="p-3 bg-gradient-to-tr from-blue-600 to-indigo-600 rounded-2xl shadow-lg shadow-blue-500/20 text-white">
          <Shield size={32} />
        </div>
        <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight text-white">
          URL <span className="bg-clip-text text-transparent bg-gradient-to-r from-blue-400 via-indigo-400 to-violet-400">Audit Service</span>
        </h1>
      </div>
      <p className="text-gray-400 text-base md:text-lg max-w-xl mx-auto font-light">
        Evaluate websites in real time. Validate HTTP status, page headers, response times, and caching configurations instantly.
      </p>
    </header>
  );
};
