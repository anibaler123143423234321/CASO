import axios from 'axios';
import { PaymentRequest, PaymentResponse } from '../types/payment';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083';

/**
 * Service to handle payment API calls.
 * (Isolation of concerns: API logic is separate from components)
 */
export const paymentService = {
  processPayment: async (request: PaymentRequest): Promise<PaymentResponse> => {
    try {
      const { data } = await axios.post<PaymentResponse>(`${API_BASE_URL}/payment`, request);
      return data;
    } catch (error: any) {
      if (error.response?.data) {
        return error.response.data as PaymentResponse;
      }
      throw new Error('No se pudo establecer conexión con el servidor.');
    }
  },
  fetchBalance: async (id: string): Promise<any> => {
    try {
      const { data } = await axios.get(`${API_BASE_URL}/accounts/${id}`);
      return data;
    } catch (error: any) {
      console.error('Error fetching balance:', error);
      return null;
    }
  }
};
