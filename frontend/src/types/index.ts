export interface User {
  id: string;
  phone: string;
  name: string;
  email?: string;
  status: 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'LOCKED' | 'CLOSED';
  kycTier: 'NONE' | 'BASIC' | 'ENHANCED' | 'FULL';
  role: 'CONSUMER' | 'MERCHANT' | 'AGENT' | 'CORPORATE' | 'ADMIN';
  referralCode?: string;
}

export interface Wallet {
  id: string;
  currency: string;
  status: string;
  balanceTotal: number;
  balanceAvailable: number;
  balanceHeld: number;
  balanceFrozen: number;
  dailyLimit: number;
  monthlyLimit: number;
  kycTier: string;
  createdAt: string;
}

export interface Transaction {
  id: string;
  idempotencyKey: string;
  type: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'REVERSED' | 'CANCELLED';
  senderWalletId?: string;
  receiverWalletId?: string;
  amount: number;
  fee: number;
  currency: string;
  description?: string;
  metadata?: Record<string, unknown>;
  createdAt: string;
  completedAt?: string;
  failureReason?: string;
}

export interface Merchant {
  id: string;
  userId: string;
  businessName: string;
  businessType?: string;
  category?: string;
  status: string;
  settlementType: string;
  feeSchedule: string;
  address?: string;
  qrStaticUrl?: string;
  createdAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  meta?: {
    requestId: string;
    timestamp: string;
    pagination?: {
      page: number;
      perPage: number;
      total: number;
      totalPages: number;
    };
  };
  error?: {
    code: string;
    message: string;
    details?: unknown;
  };
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}

export interface Biller {
  id: string;
  name: string;
}

export interface BillLookup {
  billerId: string;
  accountNumber: string;
  accountName: string;
  amount: number;
  dueDate: string;
}
