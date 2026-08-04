import axios from 'axios';
import type {
  ApiResponse, AuthResponse, User, Wallet, Transaction, Merchant, Biller, BillLookup,
  MoneyRequest, Invoice, InvoiceItem, RemittanceCorridor, Remittance,
  Promotion, CashbackWallet, SupportTicket, TicketMessage, SupportStats,
  ReferenceType, ReferenceTypeSummary, ReferenceValue, ReferenceDataLookup,
  PromotionStatus, PromotionValidation, PromotionUsage,
  MerchantAnalyticsSummary, MerchantAnalyticsBenchmark, AnalyticsTransactionRow,
} from '../types';
import { useAuthStore } from '../store/authStore';

function safeParseInvoiceItems(items: string): InvoiceItem[] {
  try {
    const parsed = JSON.parse(items);
    return Array.isArray(parsed) ? (parsed as InvoiceItem[]) : [];
  } catch {
    return [];
  }
}

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
    api.get<ApiResponse<{ content: Array<{ id: string; type: string; amount: number; description: string; txnId: string; createdAt: string }> }>>(`/wallet/ledger?userId=${userId}&page=${page}&size=${size}`).then((r) => ({
      entries: (r.data.data?.content || []).map((e) => ({
        id: e.id,
        idempotencyKey: '',
        type: e.type,
        status: 'COMPLETED' as const,
        amount: e.amount,
        fee: 0,
        currency: 'MMK',
        description: e.description,
        referenceId: e.txnId,
        createdAt: e.createdAt,
      })),
    })),

  topUp: (userId: string, amount: number, channel: string) =>
    api.post<ApiResponse<Wallet>>(`/wallet/topup?userId=${userId}`, { amount, channel, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  withdraw: (userId: string, amount: number) =>
    api.post<ApiResponse<Wallet>>(`/wallet/withdraw?userId=${userId}`, { amount, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),
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
    api.post<ApiResponse<Merchant>>(`/merchant?userId=${userId}`, data).then((r) => r.data.data),

  getProfile: (userId: string) =>
    api.get<ApiResponse<Merchant>>(`/merchant/by-user/${userId}`).then((r) => r.data.data),

  generateQr: (merchantId: string, _type = 'static', _amount?: number) =>
    api.get<ApiResponse<{ qrUrl: string; deepLink: string }>>(`/merchant/${merchantId}/qr`).then((r) => ({ qrData: r.data.data?.deepLink || r.data.data?.qrUrl || '' })),
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

  getKycPending: (page = 0, size = 20, status?: string) =>
    api.get<ApiResponse<{ requests: unknown[] }>>(`/admin/kyc/pending?page=${page}&size=${size}${status ? `&status=${status}` : ''}`).then((r) => r.data.data),

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

export const refDataApi = {
  getTypes: (page = 0, size = 50) =>
    api.get<ApiResponse<{ content: ReferenceTypeSummary[]; totalElements: number }>>(`/refdata/types?page=${page}&size=${size}`).then((r) => r.data.data),

  getType: (id: string) =>
    api.get<ApiResponse<ReferenceType>>(`/refdata/types/${id}`).then((r) => r.data.data),

  createType: (data: { code: string; description: string; active?: boolean }) =>
    api.post<ApiResponse<ReferenceType>>('/refdata/types', data).then((r) => r.data.data),

  updateType: (id: string, data: { description: string; active: boolean }) =>
    api.put<ApiResponse<ReferenceType>>(`/refdata/types/${id}`, data).then((r) => r.data.data),

  deleteType: (id: string) =>
    api.delete<ApiResponse<void>>(`/refdata/types/${id}`).then((r) => r.data.data),

  addValue: (typeId: string, data: { value: string; code: string; sortOrder?: number; active?: boolean }) =>
    api.post<ApiResponse<ReferenceValue>>(`/refdata/types/${typeId}/values`, data).then((r) => r.data.data),

  updateValue: (id: string, data: { value: string; code: string; sortOrder: number; active: boolean }) =>
    api.put<ApiResponse<ReferenceValue>>(`/refdata/values/${id}`, data).then((r) => r.data.data),

  deleteValue: (id: string) =>
    api.delete<ApiResponse<void>>(`/refdata/values/${id}`).then((r) => r.data.data),

  getLookup: (code: string) =>
    api.get<ApiResponse<ReferenceDataLookup>>(`/refdata/type/${code}`).then((r) => r.data.data),
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
    api.post<ApiResponse<void>>(`/savings/pockets/${data.pocketId}/deposit?userId=${userId}`, { amount: data.amount, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  withdraw: (userId: string, data: { pocketId: string; amount: number }) =>
    api.post<ApiResponse<void>>(`/savings/pockets/${data.pocketId}/withdraw?userId=${userId}`, { amount: data.amount, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  getTransactions: (userId: string, pocketId: string) =>
    api.get<ApiResponse<Transaction[]>>(`/savings/pockets/${pocketId}/transactions?userId=${userId}`).then((r) => r.data.data || []),
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
    api.get<ApiResponse<{ content: { id: string; transactionId: string; type: string; amount: number; description: string; status: string; evidenceList: { id: string; description: string; createdAt: string }[]; createdAt: string; resolvedAt?: string }[] }>>(`/disputes/my?userId=${userId}`).then((r) => r.data.data?.content || []),

  getAllDisputes: (page = 0, size = 50) =>
    api.get<ApiResponse<{ content: { id: string; transactionId: string; type: string; amount: number; description: string; status: string; evidenceList: { id: string; description: string; createdAt: string }[]; createdAt: string; resolvedAt?: string }[] }>>(`/disputes/all?page=${page}&size=${size}`).then((r) => r.data.data?.content || []),

  getStats: () =>
    api.get<ApiResponse<{ totalOpen: number; totalResolved: number; avgResolutionDays: number }>>('/disputes/stats').then((r) => r.data.data),
};

export const settlementApi = {
  trigger: (merchantId: string) =>
    api.post<ApiResponse<{ id: string; batchId: string }>>('/settlements/trigger', { merchantId }).then((r) => r.data.data),

  getSettlement: (batchId: string) =>
    api.get<ApiResponse<Record<string, unknown>>>(`/settlements/${batchId}`).then((r) => r.data.data),

  getMerchantSettlements: (merchantId: string) =>
    api.get<ApiResponse<{ content: { id: string; status: string; grossAmount: number; fees: number; netAmount: number; settlementRef?: string; transactionCount: number; createdAt: string }[] }>>(`/settlements/merchant/${merchantId}`).then((r) => (r.data.data?.content || []).map((s) => ({
      id: s.id,
      status: s.status,
      totalAmount: s.netAmount ?? s.grossAmount ?? 0,
      totalFees: s.fees ?? 0,
      transactionCount: s.transactionCount ?? 0,
      settlementRef: s.settlementRef,
      createdAt: s.createdAt,
    }))),

  getBatchSummary: () =>
    api.get<ApiResponse<Record<string, unknown>>>('/settlements/summary').then((r) => r.data.data || {}),
};

export const auditApi = {
  getAuditLog: (params: Record<string, string> = {}) => {
    const query = new URLSearchParams(params).toString();
    return api
      .get<ApiResponse<{
        content: { id: string; actorId: string; actorName?: string; action: string; resourceType: string; resourceId: string; createdAt: string; newValues?: string }[];
      }>>(`/audit/log${query ? `?${query}` : ''}`)
      .then((r) => (r.data.data?.content || []).map((e) => ({
        id: e.id,
        actorId: e.actorId,
        actorName: e.actorName,
        action: e.action,
        resourceType: e.resourceType,
        resourceId: e.resourceId,
        details: e.newValues,
        timestamp: e.createdAt,
      })));
  },

  getResourceAuditLog: (resourceType: string, resourceId: string) =>
    api
      .get<ApiResponse<{
        content: { id: string; actorId: string; actorName?: string; action: string; resourceType: string; resourceId: string; createdAt: string; newValues?: string }[];
      }>>(`/audit/resource?resourceType=${resourceType}&resourceId=${resourceId}`)
      .then((r) => (r.data.data?.content || []).map((e) => ({
        id: e.id,
        actorId: e.actorId,
        actorName: e.actorName,
        action: e.action,
        resourceType: e.resourceType,
        resourceId: e.resourceId,
        details: e.newValues,
        timestamp: e.createdAt,
      }))),

  getSummary: () =>
    api.get<ApiResponse<{ totalEvents: number; uniqueActors: number; topActions: { action: string; count: number }[] }>>('/audit/summary').then((r) => r.data.data),

  exportLog: (format: 'csv' | 'json') =>
    api.get(`/audit/export?format=${format}`, { responseType: format === 'csv' ? 'text' : 'json' }).then((r) => r.data),
};

export const staffApi = {
  addStaff: (merchantId: string, userId: string, data: { userId: string; role: string; dailyLimit: number }) =>
    api.post<ApiResponse<{ id: string }>>(`/staff?merchantId=${merchantId}&userId=${userId}`, { userId: data.userId, role: data.role.toUpperCase(), dailyLimit: data.dailyLimit, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  removeStaff: (ownerId: string, staffId: string) =>
    api.delete<ApiResponse<void>>(`/staff/${staffId}?merchantId=${ownerId}`).then((r) => r.data.data),

  getStaff: (ownerId: string) =>
    api.get<ApiResponse<{ id: string; userId: string; userName: string; userPhone: string; role: string; dailyLimit: number; status: string }[]>>(`/staff?merchantId=${ownerId}`).then((r) => (r.data.data || []).map((s) => ({ ...s, role: (s.role || '').toLowerCase() }))),

  changeRole: (ownerId: string, staffId: string, newRole: string) =>
    api.put<ApiResponse<void>>(`/staff/${staffId}/role?merchantId=${ownerId}&role=${newRole.toUpperCase()}`).then((r) => r.data.data),
};

interface DirectoryEntry {
  id: string;
  businessName: string;
  category: string;
  distance?: number;
  rating?: number;
  address?: string;
  qrStaticUrl?: string;
}

export const directoryApi = {
  searchMerchants: (query: string, category?: string) => {
    const params = new URLSearchParams();
    if (query) params.set('query', query);
    if (category) params.set('category', category);
    const qs = params.toString();
    return api.get<ApiResponse<{ content: DirectoryEntry[] }>>(`/directory/search${qs ? `?${qs}` : ''}`).then((r) => r.data.data?.content || []);
  },

  getNearbyMerchants: (category?: string, coords?: { latitude: number; longitude: number }, radius = 5.0) => {
    const params = new URLSearchParams();
    if (coords) {
      params.set('latitude', String(coords.latitude));
      params.set('longitude', String(coords.longitude));
      params.set('radius', String(radius));
    }
    if (category) params.set('category', category);
    const qs = params.toString();
    return api.get<ApiResponse<{ content: DirectoryEntry[] }>>(`/directory/nearby${qs ? `?${qs}` : ''}`).then((r) => r.data.data?.content || []);
  },
};

export const scheduledPaymentApi = {
  create: (userId: string, data: { recipient: string; amount: number; frequency: string; startDate: string; description?: string }) =>
    api.post<ApiResponse<{ id: string }>>(`/scheduled?userId=${userId}`, {
      recipientIdentifier: data.recipient,
      amount: data.amount,
      type: 'P2P',
      frequency: data.frequency.toUpperCase(),
      description: data.description,
      idempotencyKey: `idem_${Date.now()}`,
    }).then((r) => r.data.data),

  getMySchedules: (userId: string) =>
    api.get<ApiResponse<{ content: Array<{ id: string; recipientIdentifier: string; amount: number; frequency: string; status: string; nextExecutionDate: string | null; lastExecutionDate: string | null; description?: string; createdAt: string }> }>>(`/scheduled/my?userId=${userId}`).then((r) =>
      (r.data.data?.content || []).map((s) => ({
        id: s.id,
        recipient: s.recipientIdentifier,
        amount: s.amount,
        frequency: (s.frequency || '').toLowerCase(),
        status: s.status,
        startDate: s.createdAt,
        nextExecution: s.nextExecutionDate || s.createdAt,
        description: s.description,
        createdAt: s.createdAt,
      })),
    ),

  pause: (userId: string, scheduleId: string) =>
    api.put<ApiResponse<void>>(`/scheduled/${scheduleId}/pause?userId=${userId}`).then((r) => r.data.data),

  resume: (userId: string, scheduleId: string) =>
    api.put<ApiResponse<void>>(`/scheduled/${scheduleId}/resume?userId=${userId}`).then((r) => r.data.data),

  cancel: (userId: string, scheduleId: string) =>
    api.delete<ApiResponse<void>>(`/scheduled/${scheduleId}?userId=${userId}`).then((r) => r.data.data),
};

export const payrollApi = {
  createPayrollRun: (userId: string, data: { period: string; employees: { employeeId: string; employeeName: string; phone: string; amount: number }[] }) =>
    api.post<ApiResponse<{ id: string; status: string }>>(`/payroll/create`, data, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data),

  submitPayroll: (userId: string, runId: string) =>
    api.post<ApiResponse<{ id: string; status: string }>>(`/payroll/${runId}/submit`, null, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data),

  approvePayroll: (userId: string, runId: string) =>
    api.put<ApiResponse<{ id: string; status: string }>>(`/payroll/${runId}/approve`, null, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data),

  getPayrollRun: (userId: string) =>
    api.get<ApiResponse<Array<{ id: string; status: string; totalAmount: number; totalEmployees: number; period: string; createdAt: string; employees?: { employeeName: string; phone: string; amount: number }[] }>>>(`/payroll/history`, { headers: { 'X-User-Id': userId } }).then((r) =>
      (r.data.data || []).map((run) => ({
        id: run.id,
        status: run.status,
        totalAmount: run.totalAmount ?? 0,
        employeeCount: run.totalEmployees ?? 0,
        payDate: run.period || run.createdAt,
        createdAt: run.createdAt,
        employees: run.employees?.map((e) => ({ name: e.employeeName, phone: e.phone, salary: e.amount })),
      })),
    ),
};

export const requestMoneyApi = {
  createRequest: (userId: string, data: { targetPhone: string; amount: number; description?: string }) =>
    api.post<ApiResponse<Omit<MoneyRequest, 'requesterId'> & { requesterUserId: string }>>(`/request-money?userId=${userId}`, { ...data, idempotencyKey: `idem_${Date.now()}` }).then((r) => ({
      ...r.data.data,
      requesterId: r.data.data.requesterUserId,
    })),

  getMyRequests: (userId: string) =>
    api.get<ApiResponse<{ content: Array<Omit<MoneyRequest, 'requesterId'> & { requesterUserId: string }> }>>(`/request-money/my?userId=${userId}`).then((r) =>
      (r.data.data?.content || []).map((rq) => ({
        ...rq,
        requesterId: rq.requesterUserId,
      })),
    ),

  getReceivedRequests: (phone: string) =>
    api.get<ApiResponse<{ content: Array<Omit<MoneyRequest, 'requesterId'> & { requesterUserId: string }> }>>(`/request-money/phone/${encodeURIComponent(phone)}`).then((r) =>
      (r.data.data?.content || []).map((rq) => ({
        ...rq,
        requesterId: rq.requesterUserId,
      })),
    ),

  respondToRequest: (userId: string, requestId: string, action: 'ACCEPT' | 'CANCEL') =>
    api.put<ApiResponse<void>>(`/request-money/${requestId}/respond?targetUserId=${userId}`, { action }).then((r) => r.data.data),
};

export const invoiceApi = {
  create: (userId: string, data: { customerPhone: string; customerName: string; items: Omit<InvoiceItem, 'id'>[]; tax: number; dueDate: string }) => {
    const subtotal = data.items.reduce((sum, item) => sum + item.quantity * item.price, 0);
    return api
      .post<ApiResponse<Invoice>>(`/invoices?userId=${userId}`, {
        customerPhone: data.customerPhone,
        customerName: data.customerName,
        items: JSON.stringify(data.items),
        subtotal,
        tax: data.tax,
        total: subtotal + data.tax,
        dueDate: data.dueDate,
        idempotencyKey: `idem_${Date.now()}`,
      })
      .then((r) => r.data.data);
  },

  send: (userId: string, invoiceId: string) =>
    api.put<ApiResponse<void>>(`/invoices/${invoiceId}/send?userId=${userId}`).then((r) => r.data.data),

  markPaid: (userId: string, invoiceId: string) =>
    api.put<ApiResponse<void>>(`/invoices/${invoiceId}/paid?userId=${userId}`).then((r) => r.data.data),

  cancel: (userId: string, invoiceId: string) =>
    api.put<ApiResponse<void>>(`/invoices/${invoiceId}/cancel?userId=${userId}`).then((r) => r.data.data),

  getByMerchant: (userId: string) =>
    api.get<ApiResponse<{ content: Invoice[] }>>(`/invoices?userId=${userId}&page=0&size=100`).then((r) =>
      ((r.data.data?.content || []) as Invoice[]).map((inv) => ({
        ...inv,
        items: typeof inv.items === 'string' ? safeParseInvoiceItems(inv.items) : inv.items,
      })),
    ),
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

  validateCode: (userId: string, code: string, amount: number) =>
    api.post<ApiResponse<PromotionValidation>>(`/promotions/validate?promoCode=${encodeURIComponent(code)}&amount=${amount}&userId=${userId}`).then((r) => r.data.data),

  apply: (userId: string, code: string, amount: number) =>
    api.post<ApiResponse<PromotionUsage>>(`/promotions/apply?userId=${userId}`, { promoCode: code, transactionAmount: amount, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  getCashbackWallet: (userId: string) =>
    api.get<ApiResponse<CashbackWallet | null>>(`/promotions/cashback-wallet?userId=${userId}`).then((r) => ({
      id: r.data.data?.id || '',
      balance: r.data.data?.balance ?? 0,
      totalEarned: r.data.data?.totalEarned ?? 0,
      totalRedeemed: r.data.data?.totalRedeemed ?? 0,
      currency: 'MMK',
    })),

  redeemCashback: (userId: string, amount: number) =>
    api.post<ApiResponse<void>>(`/promotions/cashback-redeem?userId=${userId}`, { amount, idempotencyKey: `idem_${Date.now()}` }).then((r) => r.data.data),

  getMy: (userId: string, page = 0, size = 50) =>
    api.get<ApiResponse<{ content: Promotion[]; totalElements: number }>>(`/promotions/my?userId=${userId}&page=${page}&size=${size}`).then((r) => r.data.data),

  create: (data: Partial<Promotion>) =>
    api.post<ApiResponse<Promotion>>('/promotions', data).then((r) => r.data.data),

  setStatus: (id: string, status: PromotionStatus) =>
    api.put<ApiResponse<Promotion>>(`/promotions/${id}/status?status=${status}`).then((r) => r.data.data),

  deactivate: (id: string) =>
    api.delete<ApiResponse<void>>(`/promotions/${id}`).then((r) => r.data.data),
};

export const analyticsApi = {
  getSummary: (walletId: string, startDate?: string, endDate?: string) =>
    api.get<ApiResponse<MerchantAnalyticsSummary>>(`/transfer/analytics/summary?walletId=${walletId}${startDate ? `&startDate=${startDate}` : ''}${endDate ? `&endDate=${endDate}` : ''}`).then((r) => r.data.data),

  getBenchmark: (walletId: string, startDate?: string, endDate?: string) =>
    api.get<ApiResponse<MerchantAnalyticsBenchmark>>(`/transfer/analytics/benchmark?walletId=${walletId}${startDate ? `&startDate=${startDate}` : ''}${endDate ? `&endDate=${endDate}` : ''}`).then((r) => r.data.data),

  getTransactions: (walletId: string, params: { startDate?: string; endDate?: string; direction?: string; minAmount?: number; maxAmount?: number; page?: number; size?: number }) =>
    api.get<ApiResponse<{ content: AnalyticsTransactionRow[]; totalElements: number }>>(`/transfer/analytics/transactions?walletId=${walletId}`, { params }).then((r) => r.data.data),
};

export const agentApi = {
  getAccount: (userId: string) =>
    api.get<ApiResponse<{ id: string; userId: string; floatBalance: number; commissionBalance: number; status: string }>>(`/agent/account`, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data),

  cashIn: (userId: string, data: { customerPhone: string; amount: number }) =>
    api.post<ApiResponse<{ id: string; status: string }>>(`/agent/cash-in`, { ...data, idempotencyKey: `idem_${Date.now()}` }, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data),

  cashOut: (userId: string, data: { customerPhone: string; amount: number }) =>
    api.post<ApiResponse<{ id: string; status: string }>>(`/agent/cash-out`, { ...data, idempotencyKey: `idem_${Date.now()}` }, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data),

  getFloatHistory: (userId: string, page = 0, size = 20) =>
    api.get<ApiResponse<Array<{ id: string; type: string; amount: number; description: string; createdAt: string }>>>(`/agent/float-history?page=${page}&size=${size}`, { headers: { 'X-User-Id': userId } }).then((r) => r.data.data || []),
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
    api.post<ApiResponse<TicketMessage>>(`/support/tickets/${ticketId}/messages?userId=${userId}`, { message }).then((r) => r.data.data),

  getMessages: (ticketId: string) =>
    api.get<ApiResponse<TicketMessage[]>>(`/support/tickets/${ticketId}/messages`).then((r) => r.data.data || []),

  getMyTickets: (userId: string) =>
    api.get<ApiResponse<{ content: SupportTicket[] }>>(`/support/my-tickets?userId=${userId}&page=0&size=100`).then((r) => r.data.data.content || []),

  getTicket: (ticketId: string) =>
    api.get<ApiResponse<SupportTicket>>(`/support/tickets/${ticketId}`).then((r) => r.data.data),

  resolve: (userId: string, ticketId: string) =>
    api.put<ApiResponse<SupportTicket>>(`/support/tickets/${ticketId}/resolve?userId=${userId}`).then((r) => r.data.data),

  escalate: (_userId: string, ticketId: string) =>
    api.put<ApiResponse<SupportTicket>>(`/support/tickets/${ticketId}/escalate`, { newPriority: 'URGENT', reason: 'Escalated by user' }).then((r) => r.data.data),

  getStats: () =>
    api.get<ApiResponse<SupportStats>>('/support/stats').then((r) => r.data.data),
};
