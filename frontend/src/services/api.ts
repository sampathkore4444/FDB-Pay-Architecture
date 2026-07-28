import axios from 'axios';
import type { ApiResponse, AuthResponse, User, Wallet, Transaction, Merchant, Biller, BillLookup } from '../types';
import { useAuthStore } from '../store/authStore';

const api = axios.create({
  baseURL: '/v1',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  register: (data: { phone: string; name: string; email?: string; pin: string }) =>
    api.post<ApiResponse<AuthResponse>>('/auth/register', data).then((r) => r.data.data),

  login: (data: { phone: string; pin: string }) =>
    api.post<ApiResponse<AuthResponse>>('/auth/login', data).then((r) => r.data.data),

  sendOtp: (phone: string) =>
    api.post<ApiResponse<void>>(`/auth/otp/send?phone=${phone}`).then((r) => r.data),

  verifyOtp: (phone: string, code: string) =>
    api.post<ApiResponse<void>>('/auth/otp/verify', { phone, code }).then((r) => r.data),

  getProfile: (userId: string) =>
    api.get<ApiResponse<User>>(`/auth/profile?userId=${userId}`).then((r) => r.data.data),
};

export const walletApi = {
  getWallet: (userId: string) =>
    api.get<ApiResponse<Wallet>>(`/wallet?userId=${userId}`).then((r) => r.data.data),

  getLedger: (userId: string, page = 0, size = 20) =>
    api.get<ApiResponse<{ entries: Transaction[] }>>(`/wallet/ledger?userId=${userId}&page=${page}&size=${size}`).then((r) => r.data.data),

  topUp: (userId: string, amount: number, channel: string) =>
    api.post<ApiResponse<Wallet>>(`/wallet/topup?userId=${userId}`, { amount, channel, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  withdraw: (userId: string, amount: number) =>
    api.post<ApiResponse<Wallet>>(`/wallet/withdraw?userId=${userId}&amount=${amount}&idempotencyKey=idem_${Date.now()}`).then((r) => r.data.data),
};

export const transferApi = {
  initiate: (userId: string, data: { recipientIdentifier: string; amount: number; type?: string; description?: string }) =>
    api.post<ApiResponse<Transaction>>(`/transfer?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  getStatus: (id: string) =>
    api.get<ApiResponse<Transaction>>(`/transfer/${id}`).then((r) => r.data.data),

  getHistory: (userId: string, page = 0, size = 20) =>
    api.get<ApiResponse<{ transactions: Transaction[] }>>(`/transfer/history?userId=${userId}&page=${page}&size=${size}`).then((r) => r.data.data),
};

export const merchantApi = {
  register: (userId: string, data: { businessName: string; businessType?: string; category?: string; address?: string }) =>
    api.post<ApiResponse<Merchant>>(`/merchant/register?userId=${userId}`, data).then((r) => r.data.data),

  getProfile: (merchantId: string) =>
    api.get<ApiResponse<Merchant>>(`/merchant/profile?merchantId=${merchantId}`).then((r) => r.data.data),

  generateQr: (merchantId: string, type = 'static', amount?: number) =>
    api.post<ApiResponse<{ qrData: string }>>(`/merchant/qr/generate?merchantId=${merchantId}&type=${type}${amount ? `&amount=${amount}` : ''}`).then((r) => r.data.data),
};

export const billApi = {
  getCategories: () =>
    api.get<ApiResponse<{ id: string; name: string }[]>>('/bills/categories').then((r) => r.data.data),

  getBillers: (category: string) =>
    api.get<ApiResponse<Biller[]>>(`/bills/billers?category=${category}`).then((r) => r.data.data),

  lookupBill: (billerId: string, account: string) =>
    api.get<ApiResponse<BillLookup>>(`/bills/billers/${billerId}/lookup?account=${account}`).then((r) => r.data.data),

  payBill: (userId: string, data: { billerId: string; accountNumber: string; amount: number }) =>
    api.post<ApiResponse<Transaction>>(`/bills/pay?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),
};

export const adminApi = {
  getDashboard: () =>
    api.get<ApiResponse<Record<string, unknown>>>('/admin/dashboard').then((r) => r.data.data),

  getPendingKyc: (page = 0, size = 20) =>
    api.get<ApiResponse<{ requests: unknown[] }>>(`/admin/kyc/pending?page=${page}&size=${size}`).then((r) => r.data.data),
};
