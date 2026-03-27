import { useState, useCallback } from 'react';
import { PaymentRequest, PaymentResponse } from '../types/payment';
import { paymentService } from '../services/paymentService';

/**
 * Hook to manage payment logic.
 * (Principles: Encapsulation of state and side effects)
 */
export const usePayment = () => {
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<PaymentResponse | null>(null);

  const executePayment = useCallback(async (request: PaymentRequest) => {
    setLoading(true);
    setStatus(null);
    try {
      const response = await paymentService.processPayment(request);
      setStatus(response);
    } catch (err: any) {
      setStatus({
        payment_id: request.payment_id,
        status: 'ERROR',
        message: err.message,
        timestamp: new Date().toISOString()
      });
    } finally {
      setLoading(false);
    }
  }, []);

  const resetStatus = useCallback(() => setStatus(null), []);

  const fetchBalance = useCallback(async (id: string) => {
    return await paymentService.fetchBalance(id);
  }, []);

  return { loading, status, executePayment, resetStatus, fetchBalance };
};
