import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle, XCircle, AlertCircle, Info } from 'lucide-react';
import { PaymentResponse } from '../types/payment';

interface Props {
  status: PaymentResponse | null;
  onClose: () => void;
}

/**
 * Overlay component for transaction feedback.
 * (Principles: Pure UI component driven by status state)
 */
export const StatusOverlay: React.FC<Props> = ({ status, onClose }) => {
  if (!status) return null;

  const config = {
    EXITO: {
      icon: <CheckCircle className="text-emerald-400" size={48} />,
      bg: 'bg-emerald-400/10',
      title: '¡Pago Exitoso!'
    },
    SALDO_INSUFICIENTE: {
      icon: <AlertCircle className="text-amber-400" size={48} />,
      bg: 'bg-amber-400/10',
      title: 'Saldo Insuficiente'
    },
    FALLO: {
      icon: <XCircle className="text-rose-400" size={48} />,
      bg: 'bg-rose-400/10',
      title: 'Error en el Pago'
    },
    SERVICE_UNAVAILABLE: {
      icon: <Info className="text-blue-400" size={48} />,
      bg: 'bg-blue-400/10',
      title: 'Indisponibilidad'
    },
    default: {
      icon: <XCircle className="text-rose-400" size={48} />,
      bg: 'bg-rose-400/10',
      title: 'Error'
    }
  };

  const current = config[status.status as keyof typeof config] || config.default;
  const isIdempotency = status.message.includes('idempotente');

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-slate-950/80 backdrop-blur-xl"
      >
        <motion.div
          initial={{ scale: 0.9, y: 20 }}
          animate={{ scale: 1, y: 0 }}
          className="glass max-w-sm w-full p-8 rounded-[32px] text-center"
        >
          <div className={`w-20 h-20 rounded-full ${current.bg} flex items-center justify-center mx-auto mb-6`}>
            {isIdempotency ? <Info className="text-blue-400" size={48} /> : current.icon}
          </div>
          
          <h2 className="text-2xl font-bold mb-3">
            {isIdempotency ? 'Pago Duplicado' : current.title}
          </h2>
          
          <p className="text-slate-400 mb-8 leading-relaxed">
            {status.message}
          </p>

          <button
            onClick={onClose}
            className="w-full py-4 bg-white/5 hover:bg-white/10 rounded-2xl transition-colors font-semibold"
          >
            {status.status === 'SUCCESS' ? 'Cerrar' : 'Entendido'}
          </button>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
};
