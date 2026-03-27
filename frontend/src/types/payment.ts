export interface PaymentRequest {
  user_id: string;
  amount: number;
  currency: string;
  merchant_id: string;
  payment_id: string;
}

export interface PaymentResponse {
  payment_id: string;
  status: 'SUCCESS' | 'FAILED' | 'INSUFFICIENT_FUNDS' | 'SERVICE_UNAVAILABLE' | 'VALIDATION_ERROR' | 'ERROR';
  message: string;
  timestamp: string;
}
