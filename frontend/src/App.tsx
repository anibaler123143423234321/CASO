import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Scanner } from './components/Scanner';
import { StatusOverlay } from './components/StatusOverlay';
import { usePayment } from './hooks/usePayment';
import { Loader2 } from 'lucide-react';

const App: React.FC = () => {
  const { loading, status, executePayment, resetStatus, fetchBalance } = usePayment();
  const [paymentId, setPaymentId] = useState('');
  
  // MOCK BALANCES (Initial 100M)
  const [userBalance, setUserBalance] = useState<number>(() => {
    const saved = localStorage.getItem('user_balance');
    return saved ? parseFloat(saved) : 100000000.00;
  });
  const [merchantBalance, setMerchantBalance] = useState<number>(() => {
    const saved = localStorage.getItem('merchant_balance');
    return saved ? parseFloat(saved) : 0.00;
  });
  
  const [userId, setUserId] = useState('USER-001');
  const [merchantId, setMerchantId] = useState('MERCHANT-001');

  // Persist balances whenever they change
  useEffect(() => {
    localStorage.setItem('user_balance', userBalance.toString());
    localStorage.setItem('merchant_balance', merchantBalance.toString());
  }, [userBalance, merchantBalance]);

  // Generate a random payment ID on mount or reset
  const generateId = () => {
    setPaymentId(`PAY-${Math.random().toString(36).substring(2, 11).toUpperCase()}`);
  };

  // Fetch initial balances on mount
  useEffect(() => {
    const initBalances = async () => {
      const uRes = await fetchBalance(userId);
      if (uRes && typeof uRes.balance === 'number') setUserBalance(uRes.balance);
      
      const mRes = await fetchBalance(merchantId);
      if (mRes && typeof mRes.balance === 'number') setMerchantBalance(mRes.balance);
    };
    initBalances();
  }, [fetchBalance]);

  useEffect(() => {
    generateId();
    
    if (status && status.status === 'EXITO') {
      const amount = parseFloat(localStorage.getItem('last_processed_amount') || '0');
      if (amount > 0) {
        setUserBalance(prev => (prev || 0) - amount);
        setMerchantBalance(prev => (prev || 0) + amount);
        localStorage.removeItem('last_processed_amount');
      }
    }
  }, [status]);

  const handleFetchUserBalance = async (val: string) => {
    setUserId(val);
    if (!val) return;
    
    const res = await fetchBalance(val);
    if (res && typeof res.balance === 'number') {
      setUserBalance(res.balance);
    }
    // Si falla, mantenemos el saldo actual (simulado)
  };

  const handleFetchMerchantBalance = async (val: string) => {
    setMerchantId(val);
    if (!val) return;
    
    const res = await fetchBalance(val);
    if (res && typeof res.balance === 'number') {
      setMerchantBalance(res.balance);
    }
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const amount = parseFloat(formData.get('amount') as string);
    
    // VALIDATION: Insufficient funds check (Local)
    if (userBalance < amount) {
      alert("Error: Saldo insuficiente para realizar esta operación.");
      return;
    }

    if (amount <= 0) {
      alert("Error: El monto debe ser mayor a 0.");
      return;
    }

    // Store amount temporarily to update balance on success
    localStorage.setItem('last_processed_amount', amount.toString());
    
    executePayment({
      user_id: userId,
      merchant_id: merchantId,
      amount: amount,
      currency: 'PEN',
      payment_id: paymentId
    });
  };

  return (
    <div className="flex flex-col items-center justify-center p-4 min-h-screen">
      {/* Wallet Summary */}
      <motion.div 
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="w-full max-w-md mb-8 grid grid-cols-2 gap-4"
      >
        <div className="glass p-6 rounded-[32px] border border-white/5 relative overflow-hidden group hover:border-indigo-500/30 transition-all">
          <div className="absolute top-0 right-0 w-16 h-16 bg-indigo-500/10 blur-2xl rounded-full -mr-8 -mt-8 group-hover:bg-indigo-500/20 transition-all"></div>
          <p className="text-[10px] text-slate-400 uppercase tracking-widest font-bold mb-1">Tu Saldo Principal</p>
          <p className="text-2xl font-black text-white tracking-tight">S/ {(userBalance || 0).toLocaleString('es-PE', { minimumFractionDigits: 2 })}</p>
        </div>
        <div className="glass p-6 rounded-[32px] border border-white/5 relative overflow-hidden group hover:border-purple-500/30 transition-all">
          <div className="absolute top-0 right-0 w-16 h-16 bg-purple-500/10 blur-2xl rounded-full -mr-8 -mt-8 group-hover:bg-purple-500/20 transition-all"></div>
          <p className="text-[10px] text-slate-400 uppercase tracking-widest font-bold mb-1">Recaudación Comercio</p>
          <p className="text-2xl font-black text-indigo-400 tracking-tight">S/ {(merchantBalance || 0).toLocaleString('es-PE', { minimumFractionDigits: 2 })}</p>
        </div>
      </motion.div>

      <motion.div 
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-md w-full glass p-8 rounded-[48px] glow-border relative overflow-hidden shadow-2xl"
      >
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-48 h-1 bg-gradient-to-r from-transparent via-indigo-500 to-transparent opacity-50"></div>
        
        <header className="text-center mb-10">
          <motion.div
            initial={{ scale: 0.9 }}
            animate={{ scale: 1 }}
            className="inline-block px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-[10px] uppercase tracking-[0.2em] font-bold mb-3"
          >
            Terminal Punto de Venta
          </motion.div>
          <h1 className="text-4xl font-extrabold tracking-tighter text-white mb-2 italic">
            GALAXY<span className="text-indigo-500">PAY</span>
          </h1>
          <div className="h-1 w-12 bg-indigo-500 mx-auto rounded-full"></div>
        </header>

        <Scanner />

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-5">
            <div className="group">
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-[0.15em] mb-2 ml-1">
                ID Usuario Solicitante
              </label>
              <input
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                onBlur={(e) => handleFetchUserBalance(e.target.value)}
                required
                placeholder="Ej. USER-001"
                className="w-full bg-white/5 border border-white/5 rounded-2xl p-4 text-white placeholder:text-slate-600 focus:bg-indigo-500/5 focus:border-indigo-500/50 outline-none transition-all duration-300 font-mono"
              />
            </div>

            <div className="group">
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-[0.15em] mb-2 ml-1">
                ID Comercio Destino
              </label>
              <input
                value={merchantId}
                onChange={(e) => setMerchantId(e.target.value)}
                onBlur={(e) => handleFetchMerchantBalance(e.target.value)}
                required
                placeholder="Ej. MERCHANT-001"
                className="w-full bg-white/5 border border-white/5 rounded-2xl p-4 text-white placeholder:text-slate-600 focus:bg-purple-500/5 focus:border-purple-500/50 outline-none transition-all duration-300 font-mono"
              />
            </div>

            <div className="flex gap-4">
              <div className="flex-[2]">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-[0.15em] mb-2 ml-1">
                  Monto de Transacción
                </label>
                <div className="relative">
                  <span className="absolute left-4 top-1/2 -translate-y-1/2 text-indigo-400 font-bold">S/</span>
                  <input
                    name="amount"
                    type="number"
                    defaultValue="10.00"
                    step="0.01"
                    min="0.01"
                    required
                    className="w-full bg-white/5 border border-white/5 rounded-2xl p-4 pl-10 text-white focus:bg-indigo-500/5 focus:border-indigo-500/50 outline-none transition-all duration-300 text-lg font-bold"
                  />
                </div>
              </div>
              <div className="flex-1">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-[0.15em] mb-2 ml-1">
                  Divisa
                </label>
                <div className="w-full bg-white/5 border border-white/5 rounded-2xl p-4 text-center text-slate-300 font-black h-[61px] flex items-center justify-center tracking-widest">
                  PEN
                </div>
              </div>
            </div>
          </div>

          <div className="pt-4">
            <motion.button
              whileHover={{ scale: 1.02, boxShadow: '0 20px 40px -15px rgba(79,70,229,0.5)' }}
              whileTap={{ scale: 0.98 }}
              disabled={loading}
              className="w-full relative flex items-center justify-center p-5 bg-gradient-to-br from-indigo-600 to-indigo-700 rounded-3xl font-black text-xl hover:from-indigo-500 hover:to-indigo-600 transition-all disabled:opacity-50 disabled:cursor-not-allowed overflow-hidden group shadow-xl"
            >
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-1000"></div>
              {loading ? (
                <Loader2 className="animate-spin" />
              ) : (
                "EFECTUAR PAGO"
              )}
            </motion.button>
          </div>
        </form>

        <StatusOverlay status={status} onClose={resetStatus} />
        
        <footer className="mt-10 text-center opacity-30 hover:opacity-100 transition-opacity">
          <p className="text-[10px] font-mono tracking-[0.3em] font-bold">
            TRX_SESSION: {paymentId}
          </p>
        </footer>
      </motion.div>
    </div>
  );
};

export default App;
