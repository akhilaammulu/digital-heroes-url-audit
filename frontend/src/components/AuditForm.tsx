import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Globe, ArrowRight, Loader2, AlertCircle } from 'lucide-react';

const auditSchema = z.object({
  url: z.string()
    .min(1, 'URL is required')
    .url('Please enter a valid URL (must start with http:// or https://)')
    .refine(
      (val) => val.startsWith('http://') || val.startsWith('https://'),
      'URL must begin with http:// or https://'
    )
});

export type AuditFormData = z.infer<typeof auditSchema>;

interface AuditFormProps {
  onSubmit: (data: AuditFormData) => void;
  isLoading: boolean;
}

export const AuditForm: React.FC<AuditFormProps> = ({ onSubmit, isLoading }) => {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<AuditFormData>({
    resolver: zodResolver(auditSchema),
    defaultValues: {
      url: '',
    },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="w-full">
      <div className="flex flex-col md:flex-row gap-3">
        <div className="relative flex-grow">
          <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
            <Globe size={18} className={isLoading ? 'animate-pulse text-indigo-400' : ''} />
          </div>
          <input
            {...register('url')}
            type="text"
            disabled={isLoading}
            placeholder="https://example.com"
            className={`w-full pl-11 pr-4 py-4 rounded-2xl bg-white/[0.03] border ${
              errors.url
                ? 'border-rose-500/50 focus:border-rose-500 focus:ring-rose-500/10'
                : 'border-white/10 focus:border-indigo-500 focus:ring-indigo-500/10'
            } text-white placeholder-gray-500 focus:outline-none focus:ring-4 transition-all duration-200 disabled:opacity-50 text-base shadow-inner`}
          />
        </div>
        <button
          type="submit"
          disabled={isLoading}
          className="md:w-44 py-4 px-6 rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-semibold flex items-center justify-center gap-2 shadow-lg shadow-blue-500/20 active:scale-[0.98] transition-all duration-200 disabled:opacity-50 disabled:pointer-events-none hover:shadow-xl hover:shadow-indigo-500/30 cursor-pointer text-base"
        >
          {isLoading ? (
            <>
              <Loader2 size={18} className="animate-spin" />
              <span>Auditing...</span>
            </>
          ) : (
            <>
              <span>Audit URL</span>
              <ArrowRight size={18} />
            </>
          )}
        </button>
      </div>
      {errors.url && (
        <div className="flex items-center gap-2 mt-3 text-rose-400 text-sm bg-rose-500/5 border border-rose-500/10 px-4 py-2.5 rounded-xl animate-in fade-in slide-in-from-top-1 duration-200">
          <AlertCircle size={16} className="shrink-0" />
          <span>{errors.url.message}</span>
        </div>
      )}
    </form>
  );
};
