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

export interface MoneyRequest {
  id: string;
  requesterId: string;
  requesterName: string;
  targetPhone: string;
  amount: number;
  description?: string;
  paymentLink: string;
  status: 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'CANCELLED';
  createdAt: string;
  respondedAt?: string;
}

export interface InvoiceItem {
  id: string;
  name: string;
  quantity: number;
  price: number;
}

export interface Invoice {
  id: string;
  merchantId: string;
  customerPhone: string;
  customerName: string;
  items: InvoiceItem[];
  subtotal: number;
  tax: number;
  total: number;
  dueDate: string;
  status: 'DRAFT' | 'SENT' | 'PAID' | 'CANCELLED';
  createdAt: string;
  sentAt?: string;
  paidAt?: string;
}

export interface RemittanceCorridor {
  id: string;
  sourceCountry: string;
  sourceCurrency: string;
  destCurrency: string;
  exchangeRate: number;
  fee: number;
  minAmount: number;
  maxAmount: number;
  partnerName: string;
  estimatedDelivery: string;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface Remittance {
  id: string;
  corridorId: string;
  sourceCountry: string;
  senderName: string;
  senderPhone: string;
  sourceAmount: number;
  sourceCurrency: string;
  destAmount: number;
  destCurrency: string;
  exchangeRate: number;
  fee: number;
  recipientPhone: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  completedAt?: string;
}

export interface Promotion {
  id: string;
  title: string;
  description: string;
  type: 'DISCOUNT' | 'CASHBACK' | 'COUPON';
  discountValue: number;
  minAmount: number;
  maxDiscount: number;
  validFrom: string;
  validTo: string;
  usageLimit: number;
  usedCount: number;
  promoCode?: string;
  status: 'ACTIVE' | 'EXPIRED' | 'USED';
}

export interface CashbackWallet {
  id: string;
  balance: number;
  totalEarned: number;
  totalRedeemed: number;
  currency: string;
}

export interface TicketMessage {
  id: string;
  ticketId: string;
  senderId: string;
  senderType: string;
  message: string;
  attachments?: string | null;
  createdAt: string;
}

export interface SupportTicket {
  id: string;
  corporateUserId: string;
  subject: string;
  category: string;
  priority: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'WAITING_CUSTOMER' | 'WAITING_INTERNAL' | 'RESOLVED' | 'CLOSED';
  assignedManagerId?: string;
  messageCount: number;
  lastResponseAt?: string;
  slaDeadline?: string;
  createdAt: string;
  updatedAt: string;
  messages?: TicketMessage[];
}

export interface SupportStats {
  totalOpen: number;
  totalResolved: number;
  avgResponseTimeHours: number;
}
