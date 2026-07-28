import axios from 'axios';
import type {
  ApiResponse, AuthResponse, User, Wallet, Transaction, Merchant, Biller, BillLookup,
  MoneyRequest, Invoice, InvoiceItem, RemittanceCorridor, Remittance,
  Promotion, CashbackWallet, SupportTicket, TicketMessage, SupportStats,
} from '../types';
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

  getKycPending: (page = 0, size = 20) =>
    api.get<ApiResponse<{ requests: unknown[] }>>(`/admin/kyc/pending?page=${page}&size=${size}`).then((r) => r.data.data),

  reviewKyc: (kycId: string, data: { status: string; reason?: string }) =>
    api.put<ApiResponse<void>>(`/admin/kyc/${kycId}/review`, data).then((r) => r.data.data),

  getAmlAlerts: (params: { severity?: string; status?: string; page?: number }) => {
    const query = new URLSearchParams(params as Record<string, string>).toString();
    return api.get<ApiResponse<{ alerts: unknown[] }>>(`/admin/aml/alerts${query ? `?${query}` : ''}`).then((r) => r.data.data);
  },

  actionAmlAlert: (alertId: string, data: { action: string; reason?: string }) =>
    api.post<ApiResponse<void>>(`/admin/aml/${alertId}/action`, data).then((r) => r.data.data),

  getUsers: (params: { search?: string; status?: string; page?: number }) => {
    const query = new URLSearchParams(params as Record<string, string>).toString();
    return api.get<ApiResponse<{ users: unknown[] }>>(`/admin/users${query ? `?${query}` : ''}`).then((r) => r.data.data);
  },

  updateUserStatus: (userId: string, data: { status: string; reason?: string }) =>
    api.put<ApiResponse<void>>(`/admin/users/${userId}/status`, data).then((r) => r.data.data),

  getMerchants: (params: { search?: string; status?: string; page?: number }) => {
    const query = new URLSearchParams(params as Record<string, string>).toString();
    return api.get<ApiResponse<{ merchants: unknown[] }>>(`/admin/merchants${query ? `?${query}` : ''}`).then((r) => r.data.data);
  },

  updateMerchantStatus: (merchantId: string, data: { status: string; reason?: string }) =>
    api.put<ApiResponse<void>>(`/admin/merchants/${merchantId}/status`, data).then((r) => r.data.data),
};

export const airtimeApi = {
  topup: (userId: string, data: { provider: string; phone: string; amount: number }) =>
    api.post<ApiResponse<{ id: string }>>(`/airtime/topup?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  getHistory: (userId: string, page = 0, size = 20) =>
    api.get<ApiResponse<{ transactions: Transaction[] }>>(`/airtime/history?userId=${userId}&page=${page}&size=${size}`).then((r) => r.data.data?.transactions || []),

  getProviders: () =>
    api.get<ApiResponse<{ id: string; name: string }[]>>('/airtime/providers').then((r) => r.data.data),
};

export const savingsApi = {
  createPocket: (userId: string, data: { name: string; goalAmount: number; targetDate?: string }) =>
    api.post<ApiResponse<{ id: string }>>(`/savings/pockets?userId=${userId}`, data).then((r) => r.data.data),

  getPockets: (userId: string) =>
    api.get<ApiResponse<{ id: string; name: string; currentAmount: number; goalAmount: number; targetDate: string; interestEarned: number; status: string; createdAt: string }[]>>(`/savings/pockets?userId=${userId}`).then((r) => r.data.data || []),

  deposit: (userId: string, data: { pocketId: string; amount: number }) =>
    api.post<ApiResponse<void>>(`/savings/deposit?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  withdraw: (userId: string, data: { pocketId: string; amount: number }) =>
    api.post<ApiResponse<void>>(`/savings/withdraw?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  getTransactions: (userId: string, pocketId: string) =>
    api.get<ApiResponse<{ entries: Transaction[] }>>(`/savings/transactions?userId=${userId}&pocketId=${pocketId}`).then((r) => r.data.data?.entries || []),
};

export const disputeApi = {
  create: (userId: string, data: { transactionId: string; type: string; amount: number; description: string }) =>
    api.post<ApiResponse<{ id: string }>>(`/disputes?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  getDispute: (disputeId: string) =>
    api.get<ApiResponse<Record<string, unknown>>>(`/disputes/${disputeId}`).then((r) => r.data.data),

  addEvidence: (disputeId: string, data: { description: string }) =>
    api.post<ApiResponse<void>>(`/disputes/${disputeId}/evidence`, data).then((r) => r.data.data),

  resolve: (disputeId: string, data: { action: string; notes: string }) =>
    api.post<ApiResponse<void>>(`/disputes/${disputeId}/resolve`, data).then((r) => r.data.data),

  getMyDisputes: (userId: string) =>
    api.get<ApiResponse<{ id: string; transactionId: string; type: string; amount: number; description: string; status: string; evidenceList: { id: string; description: string; createdAt: string }[]; createdAt: string; resolvedAt?: string }[]>>(`/disputes/my?userId=${userId}`).then((r) => r.data.data || []),

  getAllDisputes: (page = 0, size = 50) =>
    api.get<ApiResponse<{ id: string; transactionId: string; type: string; amount: number; description: string; status: string; evidenceList: { id: string; description: string; createdAt: string }[]; createdAt: string; resolvedAt?: string }[]>>(`/disputes/all?page=${page}&size=${size}`).then((r) => r.data.data || []),

  getStats: () =>
    api.get<ApiResponse<{ totalOpen: number; totalResolved: number; avgResolutionDays: number }>>('/disputes/stats').then((r) => r.data.data),
};

export const settlementApi = {
  trigger: (userId: string) =>
    api.post<ApiResponse<{ batchId: string }>>(`/settlements/trigger?userId=${userId}`).then((r) => r.data.data),

  getSettlement: (batchId: string) =>
    api.get<ApiResponse<Record<string, unknown>>>(`/settlements/${batchId}`).then((r) => r.data.data),

  getMerchantSettlements: (merchantId: string) =>
    api.get<ApiResponse<{ id: string; status: string; totalAmount: number; totalFees: number; merchantCount: number; reconciliationStatus: string; createdAt: string }[]>>(`/settlements/merchant?merchantId=${merchantId}`).then((r) => r.data.data || []),

  getBatchSummary: () =>
    api.get<ApiResponse<{ id: string; status: string; totalAmount: number; totalFees: number; merchantCount: number; reconciliationStatus: string; createdAt: string }[]>>('/settlements/summary').then((r) => r.data.data || []),
};

export const auditApi = {
  getAuditLog: (params: Record<string, string> = {}) => {
    const query = new URLSearchParams(params).toString();
    return api.get<ApiResponse<{ id: string; actorId: string; actorName?: string; action: string; resourceType: string; resourceId: string; details?: string; timestamp: string }[]>>(`/audit/log${query ? `?${query}` : ''}`).then((r) => r.data.data || []);
  },

  getResourceAuditLog: (resourceType: string, resourceId: string) =>
    api.get<ApiResponse<{ id: string; actorId: string; actorName?: string; action: string; resourceType: string; resourceId: string; details?: string; timestamp: string }[]>>(`/audit/resource?resourceType=${resourceType}&resourceId=${resourceId}`).then((r) => r.data.data || []),

  getSummary: () =>
    api.get<ApiResponse<{ totalEvents: number; uniqueActors: number; topActions: { action: string; count: number }[] }>>('/audit/summary').then((r) => r.data.data),

  exportLog: (format: 'csv' | 'json') =>
    api.get(`/audit/export?format=${format}`, { responseType: format === 'csv' ? 'text' : 'json' }).then((r) => r.data),
};

export const staffApi = {
  addStaff: (ownerId: string, data: { userId: string; role: string; dailyLimit: number }) =>
    api.post<ApiResponse<{ id: string }>>(`/staff?merchantId=${ownerId}`, data).then((r) => r.data.data),

  removeStaff: (ownerId: string, staffId: string) =>
    api.delete<ApiResponse<void>>(`/staff/${staffId}?merchantId=${ownerId}`).then((r) => r.data.data),

  getStaff: (ownerId: string) =>
    api.get<ApiResponse<{ id: string; userId: string; userName: string; userPhone: string; role: string; dailyLimit: number; status: string }[]>>(`/staff?merchantId=${ownerId}`).then((r) => r.data.data || []),

  changeRole: (ownerId: string, staffId: string, newRole: string) =>
    api.put<ApiResponse<void>>(`/staff/${staffId}/role?merchantId=${ownerId}&role=${newRole}`).then((r) => r.data.data),
};

export const directoryApi = {
  searchMerchants: (query: string, category?: string) =>
    api.get<ApiResponse<{ id: string; businessName: string; category: string; distance?: number; rating?: number; address?: string; qrStaticUrl?: string }[]>>(`/directory/search?query=${query}${category ? `&category=${category}` : ''}`).then((r) => r.data.data || []),

  getNearbyMerchants: (category?: string) =>
    api.get<ApiResponse<{ id: string; businessName: string; category: string; distance?: number; rating?: number; address?: string; qrStaticUrl?: string }[]>>(`/directory/nearby${category ? `?category=${category}` : ''}`).then((r) => r.data.data || []),
};

export const scheduledPaymentApi = {
  create: (userId: string, data: { recipient: string; amount: number; frequency: string; startDate: string; description?: string }) =>
    api.post<ApiResponse<{ id: string }>>(`/scheduled?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  getMySchedules: (userId: string) =>
    api.get<ApiResponse<{ id: string; recipient: string; amount: number; frequency: string; status: string; startDate: string; nextExecution: string; description?: string; createdAt: string }[]>>(`/scheduled/my?userId=${userId}`).then((r) => r.data.data || []),

  pause: (userId: string, scheduleId: string) =>
    api.put<ApiResponse<void>>(`/scheduled/${scheduleId}/pause?userId=${userId}`).then((r) => r.data.data),

  resume: (userId: string, scheduleId: string) =>
    api.put<ApiResponse<void>>(`/scheduled/${scheduleId}/resume?userId=${userId}`).then((r) => r.data.data),

  cancel: (userId: string, scheduleId: string) =>
    api.delete<ApiResponse<void>>(`/scheduled/${scheduleId}?userId=${userId}`).then((r) => r.data.data),
};

export const payrollApi = {
  createPayrollRun: (userId: string, data: { employees: { name: string; phone: string; salary: number }[]; payDate: string }) =>
    api.post<ApiResponse<{ id: string }>>(`/payroll/run?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  submitPayroll: (userId: string) =>
    api.post<ApiResponse<void>>(`/payroll/submit?userId=${userId}`).then((r) => r.data.data),

  approvePayroll: (userId: string, runId: string) =>
    api.post<ApiResponse<void>>(`/payroll/approve?userId=${userId}&runId=${runId}`).then((r) => r.data.data),

  rejectPayroll: (userId: string, runId: string) =>
    api.post<ApiResponse<void>>(`/payroll/reject?userId=${userId}&runId=${runId}`).then((r) => r.data.data),

  getPayrollRun: (runId?: string) =>
    api.get<ApiResponse<{ id: string; status: string; totalAmount: number; employeeCount: number; payDate: string; createdAt: string; rejectionReason?: string; employees?: { name: string; phone: string; salary: number }[] }[]>>(`/payroll/runs${runId ? `/${runId}` : ''}`).then((r) => r.data.data || []),
};

export const requestMoneyApi = {
  createRequest: (userId: string, data: { targetPhone: string; amount: number; description?: string }) =>
    api.post<ApiResponse<MoneyRequest>>(`/request-money?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  getMyRequests: (userId: string) =>
    api.get<ApiResponse<MoneyRequest[]>>(`/request-money/my?userId=${userId}`).then((r) => r.data.data || []),

  respondToRequest: (userId: string, requestId: string, action: 'ACCEPT' | 'CANCEL') =>
    api.put<ApiResponse<void>>(`/request-money/${requestId}/respond?targetUserId=${userId}`, { action }).then((r) => r.data.data),
};

export const invoiceApi = {
  create: (userId: string, data: { customerPhone: string; customerName: string; items: Omit<InvoiceItem, 'id'>[]; tax: number; dueDate: string }) =>
    api.post<ApiResponse<Invoice>>(`/invoices?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  send: (userId: string, invoiceId: string) =>
    api.put<ApiResponse<void>>(`/invoices/${invoiceId}/send?userId=${userId}`).then((r) => r.data.data),

  markPaid: (userId: string, invoiceId: string) =>
    api.put<ApiResponse<void>>(`/invoices/${invoiceId}/paid?userId=${userId}`).then((r) => r.data.data),

  cancel: (userId: string, invoiceId: string) =>
    api.put<ApiResponse<void>>(`/invoices/${invoiceId}/cancel?userId=${userId}`).then((r) => r.data.data),

  getByMerchant: (userId: string) =>
    api.get<ApiResponse<Invoice[]>>(`/invoices?userId=${userId}`).then((r) => r.data.data || []),
};

export const remittanceApi = {
  getCorridors: () =>
    api.get<ApiResponse<RemittanceCorridor[]>>('/remittance/corridors').then((r) => r.data.data || []),

  getQuote: (corridorId: string, sourceAmount: number) =>
    api.get<ApiResponse<{ destAmount: number; fee: number; exchangeRate: number; totalDest: number }>>(`/remittance/quote?corridorId=${corridorId}&sourceAmount=${sourceAmount}`).then((r) => r.data.data),

  initiate: (userId: string, data: { corridorId: string; sourceAmount: number; recipientPhone: string; senderName: string; senderPhone: string }) =>
    api.post<ApiResponse<Remittance>>(`/remittance/initiate?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  getMyRemittances: (userId: string) =>
    api.get<ApiResponse<Remittance[]>>(`/remittance/my?userId=${userId}`).then((r) => r.data.data || []),
};

export const promotionsApi = {
  getActive: () =>
    api.get<ApiResponse<Promotion[]>>('/promotions/active').then((r) => r.data.data || []),

  validateCode: (code: string) =>
    api.get<ApiResponse<Promotion>>(`/promotions/validate?code=${code}`).then((r) => r.data.data),

  apply: (userId: string, code: string) =>
    api.post<ApiResponse<Promotion>>(`/promotions/apply?userId=${userId}`, { code, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  getCashbackWallet: (userId: string) =>
    api.get<ApiResponse<CashbackWallet>>(`/promotions/cashback?userId=${userId}`).then((r) => r.data.data),

  redeemCashback: (userId: string, amount: number) =>
    api.post<ApiResponse<void>>(`/promotions/cashback/redeem?userId=${userId}`, { amount, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),
};

export const agentApi = {
  getAccount: (userId: string) =>
    api.get<ApiResponse<{ id: string; userId: string; floatBalance: number; commissionBalance: number; status: string }>>(`/agent/account`, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data),

  cashIn: (userId: string, data: { customerPhone: string; amount: number }) =>
    api.post<ApiResponse<{ id: string; status: string }>>(`/agent/cash-in`, { ...data, idempotencyKey: `idem_${Date.now()}` }, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data),

  cashOut: (userId: string, data: { customerPhone: string; amount: number }) =>
    api.post<ApiResponse<{ id: string; status: string }>>(`/agent/cash-out`, { ...data, idempotencyKey: `idem_${Date.now()}` }, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data),

  getFloatHistory: (userId: string, page = 0, size = 20) =>
    api.get<ApiResponse<{ entries: { id: string; type: string; amount: number; description: string; createdAt: string }[] }>>(`/agent/float-history?page=${page}&size=${size}`, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data?.entries || []),
};

export const corporateApi = {
  bulkDisburse: (userId: string, data: { fileRef: string; description?: string }) =>
    api.post<ApiResponse<{ id: string; batchId: string }>>(`/corp/bulk-disburse`, { ...data, idempotencyKey: `idem_${Date.now()}` }, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data),

  getBulkStatus: (userId: string, batchId: string) =>
    api.get<ApiResponse<{ id: string; status: string; totalAmount: number; totalRecipients: number; processedCount: number; failedCount: number; createdAt: string }>>(`/corp/bulk-disburse/${batchId}`, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data),

  getReconciliation: (userId: string, period: string) =>
    api.get<ApiResponse<{ period: string; totalTransactions: number; totalAmount: number; discrepancies: number; status: string }>>(`/corp/reconciliation?period=${period}`, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data),
};

export const supportApi = {
  createTicket: (userId: string, data: { subject: string; category: string; priority: string; message: string }) =>
    api.post<ApiResponse<SupportTicket>>(`/support/tickets?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  addMessage: (userId: string, ticketId: string, message: string) =>
    api.post<ApiResponse<TicketMessage>>(`/support/tickets/message?userId=${userId}&ticketId=${ticketId}`, { message }).then((r) => r.data.data),

  getMyTickets: (userId: string) =>
    api.get<ApiResponse<SupportTicket[]>>(`/support/tickets/my?userId=${userId}`).then((r) => r.data.data || []),

  getTicket: (ticketId: string) =>
    api.get<ApiResponse<SupportTicket>>(`/support/tickets/${ticketId}`).then((r) => r.data.data),

  resolve: (userId: string, ticketId: string) =>
    api.post<ApiResponse<void>>(`/support/tickets/resolve?userId=${userId}&ticketId=${ticketId}`).then((r) => r.data.data),

  escalate: (userId: string, ticketId: string) =>
    api.post<ApiResponse<void>>(`/support/tickets/escalate?userId=${userId}&ticketId=${ticketId}`).then((r) => r.data.data),

  getStats: () =>
    api.get<ApiResponse<SupportStats>>('/support/stats').then((r) => r.data.data),
};
