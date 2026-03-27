import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Scan as ScanIcon } from 'lucide-react';

/**
 * Mock QR Scanner component with animations.
 */
export const Scanner: React.FC = () => {
  return (
    <div className="relative w-64 h-64 mx-auto mb-12">
      <div className="absolute inset-0 border-2 border-white/5 rounded-[48px] glass shadow-inner"></div>
      
      {/* Dynamic Inner Glow */}
      <div className="absolute inset-8 bg-indigo-500/5 rounded-3xl blur-2xl animate-pulse-soft"></div>
      
      {/* Laser line animation */}
      <motion.div
        animate={{ top: ['15%', '85%', '15%'] }}
        transition={{ duration: 3.5, repeat: Infinity, ease: "easeInOut" }}
        className="absolute left-6 right-6 h-[2px] bg-gradient-to-r from-transparent via-indigo-500 to-transparent shadow-[0_0_20px_rgba(99,102,241,1)] z-10"
      />

      <div className="absolute inset-0 flex items-center justify-center opacity-5">
        <ScanIcon size={140} strokeWidth={1} />
      </div>

      {/* Corners */}
      <div className="absolute top-0 left-0 w-10 h-10 border-t-2 border-l-2 border-indigo-500/50 rounded-tl-[40px]"></div>
      <div className="absolute top-0 right-0 w-10 h-10 border-t-2 border-r-2 border-indigo-500/50 rounded-tr-[40px]"></div>
      <div className="absolute bottom-0 left-0 w-10 h-10 border-b-2 border-l-2 border-indigo-500/50 rounded-bl-[40px]"></div>
      <div className="absolute bottom-0 right-0 w-10 h-10 border-b-2 border-r-2 border-indigo-500/50 rounded-br-[40px]"></div>
      
      {/* Animated dots */}
      <motion.div 
        animate={{ scale: [1, 1.2, 1], opacity: [0.5, 1, 0.5] }}
        transition={{ duration: 2, repeat: Infinity }}
        className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-1 h-1 bg-indigo-500 rounded-full shadow-[0_0_10px_rgba(99,102,241,1)]"
      />
    </div>
  );
};
