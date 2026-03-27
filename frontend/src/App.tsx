import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Scanner } from './components/Scanner';
import { StatusOverlay } from './components/StatusOverlay';
import { usePayment } from './hooks/usePayment';
import { Loader2 } from 'lucide-react';

const App: React.FC = () => {
  const { loading, status, executePayment, resetStatus } = usePayment();
  const [paymentId, setPaymentId] = useState('');

  // Generate a random payment ID on mount or reset
  const generateId = () => {
    setPaymentId(`PAY-${Math.random().toString(36).substring(2, 11).toUpperCase()}`);
  };

  useEffect(() => {
    generateId();
  }, [status]);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    
    executePayment({
      user_id: formData.get('userId') as string,
      merchant_id: formData.get('merchantId') as string,
      amount: parseFloat(formData.get('amount') as string),
      currency: 'PEN',
      payment_id: paymentId
    });
  };

  return (
    <div className="flex items-center justify-center p-4 min-h-screen">
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-md w-full glass p-8 rounded-[48px] glow-border relative overflow-hidden"
      >
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-48 h-1 bg-gradient-to-r from-transparent via-indigo-500 to-transparent opacity-50"></div>
        
        <header className="text-center mb-10">
          <motion.div
            initial={{ scale: 0.9 }}
            animate={{ scale: 1 }}
            className="inline-block px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-[10px] uppercase tracking-[0.2em] font-bold mb-3"
          >
            Transaction Processor
          </motion.div>
          <h1 className="text-4xl font-extrabold tracking-tight text-white mb-2">
            Galaxy<span className="text-indigo-500">Pay</span>
          </h1>
          <div className="h-1 w-12 bg-indigo-500 mx-auto rounded-full"></div>
        </header>

        <Scanner />

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-4">
            <div className="group">
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-[0.15em] mb-2 ml-1">
                Usuario de Pago
              </label>
              <input
                name="userId"
                defaultValue="USER-001"
                required
                className="w-full bg-white/5 border border-white/5 rounded-2xl p-4 text-white placeholder:text-slate-500 focus:bg-indigo-500/5 focus:border-indigo-500/50 outline-none transition-all duration-300"
              />
            </div>

            <div className="group">
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-[0.15em] mb-2 ml-1">
                Comercio Destino
              </label>
              <input
                name="merchantId"
                defaultValue="MERCHANT-001"
                required
                className="w-full bg-white/5 border border-white/5 rounded-2xl p-4 text-white placeholder:text-slate-500 focus:bg-purple-500/5 focus:border-purple-500/50 outline-none transition-all duration-300"
              />
            </div>

            <div className="flex gap-4">
              <div className="flex-[2]">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-[0.15em] mb-2 ml-1">
                  Monto
                </label>
                <div className="relative">
                  <span className="absolute left-4 top-1/2 -translate-y-1/2 text-indigo-400 font-bold">S/</span>
                  <input
                    name="amount"
                    type="number"
                    defaultValue="100.00"
                    step="0.01"
                    required
                    className="w-full bg-white/5 border border-white/5 rounded-2xl p-4 pl-10 text-white focus:bg-indigo-500/5 focus:border-indigo-500/50 outline-none transition-all duration-300"
                  />
                </div>
              </div>
              <div className="flex-1">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-[0.15em] mb-2 ml-1">
                  Moneda
                </label>
                <div className="w-full bg-white/5 border border-white/5 rounded-2xl p-4 text-center text-slate-300 font-bold h-[58px] flex items-center justify-center">
                  PEN
                </div>
              </div>
            </div>
          </div>

          <div className="pt-4">
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              disabled={loading}
              className="w-full relative flex items-center justify-center p-5 bg-indigo-600 rounded-3xl font-bold text-lg hover:bg-indigo-500 shadow-[0_10px_30px_-10px_rgba(79,70,229,0.5)] transition-all disabled:opacity-50 disabled:cursor-not-allowed overflow-hidden"
            >
              {loading ? (
                <Loader2 className="animate-spin" />
              ) : (
                "Confirmar Pago"
              )}
            </motion.button>
          </div>
        </form>

        <StatusOverlay status={status} onClose={resetStatus} />
        
        <footer className="mt-10 text-center opacity-40 hover:opacity-100 transition-opacity">
          <p className="text-[9px] font-mono tracking-[0.2em]">
            SYSTEM_UID: {paymentId}
          </p>
        </footer>
      </motion.div>
    </div>
  );
};

export default App;
