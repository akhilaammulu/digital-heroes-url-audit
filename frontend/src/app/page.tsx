"use client";

import { useAudit } from '../hooks/useAudit';
import { Header } from '../components/Header';
import { Footer } from '../components/Footer';
import { AuditForm, AuditFormData } from '../components/AuditForm';
import { ResultCard } from '../components/ResultCard';
import { ErrorCard } from '../components/ErrorCard';
import { Loader2, Search } from 'lucide-react';

export default function Home() {
  const { isLoading, result, error, executeAudit } = useAudit();

  const handleAuditSubmit = (data: AuditFormData) => {
    executeAudit(data.url);
  };

  return (
    <main className="min-h-screen bg-[#07070a] bg-[radial-gradient(ellipse_80%_80%_at_50%_-20%,rgba(99,102,241,0.15),rgba(255,255,255,0))] text-white py-16 px-4 flex flex-col justify-between relative overflow-hidden">
      {/* Decorative blurred gradients */}
      <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] bg-blue-500/[0.03] rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] bg-indigo-500/[0.03] rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-4xl mx-auto flex-grow flex flex-col justify-center">
        <Header />

        <div className="w-full bg-white/[0.02] border border-white/5 rounded-3xl p-6 md:p-8 backdrop-blur-md shadow-2xl relative overflow-hidden mb-8">
          <div className="absolute top-0 left-0 w-full h-[2px] bg-gradient-to-r from-blue-500 via-indigo-500 to-violet-500" />
          <h2 className="text-lg font-bold mb-4 text-white flex items-center gap-2">
            <Search size={18} className="text-indigo-400" />
            <span>Scan New URL</span>
          </h2>
          <AuditForm onSubmit={handleAuditSubmit} isLoading={isLoading} />
        </div>

        {isLoading && (
          <div className="w-full py-12 flex flex-col items-center justify-center gap-4 bg-white/[0.01] border border-white/5 rounded-3xl backdrop-blur-sm animate-pulse mb-8">
            <Loader2 size={36} className="text-indigo-400 animate-spin" />
            <div className="text-center">
              <p className="text-white font-semibold text-base">Running Audit...</p>
              <p className="text-gray-400 text-xs mt-1">Requesting target headers, calculating latency, and parsing title tags</p>
            </div>
          </div>
        )}

        {!isLoading && result && (
          <div className="mb-8">
            <ResultCard result={result} />
          </div>
        )}

        {!isLoading && error && (
          <div className="mb-8">
            <ErrorCard error={error} />
          </div>
        )}
      </div>

      <Footer />
    </main>
  );
}
