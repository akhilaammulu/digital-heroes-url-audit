import React from 'react';
import { ExternalLink } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="w-full max-w-4xl mx-auto mt-16 pt-8 border-t border-white/5 text-center">
      <p className="text-gray-500 text-sm font-medium flex items-center justify-center gap-1.5">
        <a
          href="https://digitalheroesco.com"
          target="_blank"
          rel="noopener noreferrer"
          className="text-indigo-400 hover:text-indigo-300 transition-colors inline-flex items-center gap-0.5 group font-semibold"
        >
          Built for Digital Heroes Training Task
          <ExternalLink size={12} className="opacity-60 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
        </a>
      </p>
      <p className="text-gray-600 text-xs mt-2">
        &copy; {new Date().getFullYear()} Digital Heroes Co. All rights reserved.
      </p>
    </footer>
  );
};
