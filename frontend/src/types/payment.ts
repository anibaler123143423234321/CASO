export interface PaymentRequest {
  user_id: string;
  amount: number;
  currency: string;
  merchant_id: string;
  payment_id: string;
}

export interface PaymentResponse {
  payment_id: string;
  status: 'EXITO' | 'FALLO' | 'SALDO_INSUFICIENTE' | 'ERROR' | 'SERVICE_UNAVAILABLE';
  message: string;
  timestamp: string;
}
