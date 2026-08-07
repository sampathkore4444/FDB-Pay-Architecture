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
  referenceId?: string;
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
  rollingReservePercent?: number;
  rollingReservePeriodDays?: number;
  rollingReserveBalance?: number;
  terminalFields?: string;
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

export type PromotionType = 'FIXED_DISCOUNT' | 'PERCENTAGE_DISCOUNT' | 'CASHBACK' | 'BOGO' | 'COUPON_CODE';
export type PromotionStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'EXPIRED';
export type FundingType = 'MERCHANT' | 'BANK';

export interface Promotion {
  id: string;
  title: string;
  description?: string;
  type: PromotionType;
  fundingType: FundingType;
  merchantId?: string;
  discountValue: number;
  maxDiscount?: number;
  minTransactionAmount?: number;
  maxUsageTotal: number;
  maxUsagePerUser: number;
  usageCount: number;
  remainingUses: number;
  isActive: boolean;
  startDate: string;
  endDate: string;
  status: PromotionStatus;
  promoCode?: string;
  createdAt: string;
  updatedAt: string;
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

export interface ReferenceValue {
  id: string;
  value: string;
  code: string;
  sortOrder: number;
  active: boolean;
}

export interface ReferenceTypeSummary {
  id: string;
  code: string;
  description: string;
  active: boolean;
  valueCount: number;
  updatedAt: string;
}

export interface ReferenceType {
  id: string;
  code: string;
  description: string;
  active: boolean;
  values: ReferenceValue[];
  createdAt: string;
  updatedAt: string;
}

export interface ReferenceDataLookup {
  code: string;
  description: string;
  values: { value: string; code: string }[];
}

export interface PaymentMethodBreakdown {
  method: string;
  count: number;
  amount: number;
}

export interface DailyPoint {
  date: string;
  count: number;
  amount: number;
}

export interface TopCustomer {
  counterpartyWalletId: string;
  count: number;
  amount: number;
}

export interface MerchantAnalyticsSummary {
  totalSales: number;
  saleCount: number;
  avgTransactionValue: number;
  refundCount: number;
  refundAmount: number;
  netSales: number;
  paymentMethods: PaymentMethodBreakdown[];
  dailySeries: DailyPoint[];
  topCustomers: TopCustomer[];
}

export interface MerchantAnalyticsBenchmark {
  merchantTotalSales: number;
  merchantSaleCount: number;
  merchantAvgTransactionValue: number;
  platformTotalSales: number;
  platformTransactionCount: number;
  platformAvgTransactionValue: number;
  vsAveragePercent: number | null;
}

export interface AnalyticsTransactionRow {
  id: string;
  direction: 'SALE' | 'REFUND';
  type: string;
  method: string;
  amount: number;
  fee: number;
  description?: string;
  senderWalletId: string;
  receiverWalletId: string;
  createdAt: string;
}

export interface PromotionValidation {
  valid: boolean;
  discount: number;
  message: string;
}

export interface PromotionUsage {
  id: string;
  promotionId: string;
  userId: string;
  transactionId: string;
  discountApplied: number;
  cashbackAmount: number;
  createdAt: string;
}

export type PaymentLinkStatus = 'ACTIVE' | 'PAID' | 'EXPIRED' | 'DEACTIVATED';

export interface PaymentLink {
  id: string;
  merchantId: string;
  token: string;
  amount: number;
  description?: string;
  customerPhone?: string;
  customerName?: string;
  status: PaymentLinkStatus;
  singleUse: boolean;
  reminderCount?: number;
  autoFollowUp?: boolean;
  followUpHours?: number;
  nextReminderAt?: string;
  paidAt?: string;
  expiresAt?: string;
  createdAt: string;
}

export interface PaymentLinkPublic {
  token: string;
  merchantId: string;
  merchantName: string;
  amount: number;
  description?: string;
  status: PaymentLinkStatus;
  createdAt: string;
}

export interface BulkOperationResult {
  transactionId: string;
  success: boolean;
  message: string;
}

export interface BulkOperationResponse {
  successCount: number;
  failedCount: number;
  results: BulkOperationResult[];
}

export interface GrossByType {
  type: string;
  count: number;
  volume: number;
  fees: number;
}

export interface FeeBreakdown {
  transactionFees: number;
  cardFees: number;
  refundFees: number;
  serviceFees: number;
}

export interface RollingReserveInfo {
  percent: number;
  heldThisPeriod: number;
  releasedThisPeriod: number;
  currentBalance: number;
}

export interface MerchantStatement {
  periodStart: string;
  periodEnd: string;
  totalVolume: number;
  transactionCount: number;
  feeBreakdown: FeeBreakdown;
  totalFees: number;
  netSales: number;
  refundCount: number;
  refundAmount: number;
  grossByType: GrossByType[];
  rollingReserve: RollingReserveInfo;
}

export interface TerminalFieldOption {
  key: string;
  label: string;
  enabled: boolean;
  required: boolean;
}

export interface StaffAccount {
  id: string;
  merchantId: string;
  userId: string;
  userName?: string;
  userPhone?: string;
  role: 'OWNER' | 'MANAGER' | 'CASHIER' | 'VIEWER' | string;
  status: string;
  dailyLimit?: number;
  storeId?: string;
  permissions?: string[];
  createdAt: string;
}

export interface Store {
  id: string;
  merchantId: string;
  name: string;
  address?: string;
  city?: string;
  phone?: string;
  status: string;
  createdAt: string;
}

export type ChargebackStatus = 'OPEN' | 'RESPONDED' | 'WON' | 'LOST' | 'CLOSED';
export type ChargebackReason =
  | 'DUPLICATE_CHARGE'
  | 'GOODS_NOT_RECEIVED'
  | 'GOODS_DEFECTIVE'
  | 'UNAUTHORIZED'
  | 'WRONG_AMOUNT'
  | 'CANCELLED_ORDER'
  | 'OTHER';

export interface ChargebackNote {
  id: string;
  authorType?: string;
  authorName?: string;
  message: string;
  createdAt: string;
}

export interface Chargeback {
  id: string;
  merchantId: string;
  transactionId?: string;
  amount: number;
  currency?: string;
  reasonCode?: ChargebackReason | string;
  status: ChargebackStatus | string;
  customerNotes?: string;
  deadline?: string;
  createdAt: string;
  updatedAt?: string;
  notes?: ChargebackNote[];
}

export interface FinancingEligibility {
  eligible: boolean;
  monthlyRevenue: number;
  threeMonthVolume: number;
  avgDailySales: number;
  estimatedLimit: number;
  maxTermMonths: number;
  message?: string;
}

export type FinancingStatus = 'PENDING' | 'APPROVED' | 'DECLINED' | 'DISBURSED';

export interface FinancingApplication {
  id: string;
  merchantId: string;
  requestedAmount: number;
  termMonths: number;
  purpose?: string;
  monthlyRevenue: number;
  estimatedLimit: number;
  status: FinancingStatus | string;
  adminNote?: string;
  createdAt: string;
  reviewedAt?: string;
}

export type RiskSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface RiskAlert {
  id: string;
  merchantId: string;
  alertType?: string;
  severity: RiskSeverity | string;
  title: string;
  message?: string;
  status: 'OPEN' | 'ACKNOWLEDGED' | string;
  createdAt: string;
  acknowledgedAt?: string;
}

export interface ReconciliationRow {
  date: string;
  grossSales: number;
  saleCount: number;
  refundAmount: number;
  refundCount: number;
  fees: number;
  netSales: number;
  settlementRef?: string;
  settledAt?: string;
  status: string;
}

export type ActiveStatus = 'ACTIVE' | 'INACTIVE';
export type DiscountType = 'PERCENT' | 'FIXED';
export type RecurringInterval = 'WEEKLY' | 'MONTHLY';
export type RecurringPlanStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED';
export type ReviewStatus = 'PUBLISHED' | 'HIDDEN';

export interface RecurringPlan {
  id: string;
  merchantId: string;
  name: string;
  description?: string;
  amount: number;
  customerPhone: string;
  customerName?: string;
  interval: RecurringInterval | string;
  dayOfWeek?: number;
  dayOfMonth?: number;
  time?: string;
  status: RecurringPlanStatus | string;
  maxCharges?: number;
  chargeCount: number;
  nextRunAt?: string;
  lastChargeAt?: string;
  createdAt: string;
}

export interface PayoutAccount {
  id: string;
  merchantId: string;
  bankName: string;
  accountName: string;
  accountNumber: string;
  branch?: string;
  isDefault?: boolean;
  status: ActiveStatus | string;
  createdAt?: string;
}

export interface MerchantPreferences {
  settlementPreferredTime?: string;
  alertLargeOrderThreshold?: number;
  alertDailySurgeThreshold?: number;
  webhookUrl?: string;
}

export interface DiscountCode {
  id: string;
  merchantId: string;
  code: string;
  type: DiscountType | string;
  value: number;
  minSpend?: number;
  maxUses?: number;
  usedCount: number;
  validFrom?: string;
  validTo?: string;
  status: ActiveStatus | string;
  createdAt: string;
}

export interface CashbackCampaign {
  id: string;
  merchantId: string;
  name: string;
  percent: number;
  budget?: number;
  spent?: number;
  startsAt?: string;
  endsAt?: string;
  status: ActiveStatus | string;
  createdAt?: string;
}

export interface Product {
  id: string;
  merchantId: string;
  name: string;
  price: number;
  description?: string;
  category?: string;
  imageUrl?: string;
  quantity?: number;
  lowStockThreshold?: number;
  taxRate?: number;
  deliverable?: boolean;
  deliveryContent?: string;
  status: ActiveStatus | string;
  createdAt?: string;
}

export interface CustomerInsight {
  walletId: string;
  totalSpend: number;
  transactionCount: number;
  lastPurchaseAt?: string;
  tier?: string;
  loyaltyPoints?: number;
}

export interface MerchantReview {
  id: string;
  merchantId: string;
  customerName?: string;
  customerPhone?: string;
  rating: number;
  comment?: string;
  status: ReviewStatus | string;
  adminReply?: string;
  createdAt: string;
}

export interface LoyaltySettings {
  enabled: boolean;
  pointsPerMmk: number;
  rewardThresholdPoints: number;
  rewardValue: number;
}

export interface ReferralProgram {
  id: string;
  merchantId: string;
  code: string;
  referralBonus: number;
  referredBonus: number;
  uses: number;
  status: ActiveStatus | string;
  createdAt?: string;
}

export interface ApiKey {
  id: string;
  name: string;
  keyPreview?: string;
  environment?: string;
  usageCount?: number;
  status: ActiveStatus | string;
  lastUsedAt?: string;
  createdAt?: string;
}

export interface ReportTemplate {
  id: string;
  merchantId: string;
  name: string;
  reportType: string;
  frequency: string;
  format: string;
  email?: string;
  enabled: boolean;
  createdAt?: string;
}

export interface MerchantAuditLogEntry {
  id: string;
  actorType?: string;
  actorName?: string;
  staffId?: string;
  action: string;
  entity: string;
  entityId?: string;
  details?: string;
  createdAt: string;
}

export interface AnalyticsCustomer {
  walletId: string;
  amount: number;
  count: number;
  lastPurchaseAt?: string;
}

export interface StorePerformance {
  storeId: string;
  amount: number;
  count: number;
}

export interface Payout {
  id: string;
  accountId: string;
  accountLabel: string;
  amount: number;
  status: string;
  reference?: string;
  failureReason?: string;
  createdAt?: string;
  completedAt?: string;
}

export interface ContractInfo {
  settlementType?: string;
  feeRate?: number;
  settlementFrequencyDays?: number;
  rollingReserveRate?: number;
}

export interface WebhookSubscription {
  id: string;
  event: string;
  url: string;
  enabled: boolean;
  createdAt?: string;
}

export interface WebhookDelivery {
  id: string;
  subscriptionId?: string;
  event: string;
  url: string;
  payload?: string;
  status: string;
  attempts?: number;
  statusCode?: number;
  error?: string;
  createdAt?: string;
  deliveredAt?: string;
}

export interface FraudRule {
  id: string;
  name: string;
  ruleType: string;
  threshold: number;
  enabled: boolean;
  createdAt?: string;
}

export interface MarketingCampaign {
  id: string;
  name: string;
  campaignType: string;
  audienceSegment: string;
  discountCodeId?: string;
  cashbackId?: string;
  status: ActiveStatus | string;
  createdAt?: string;
}

export interface SegmentSummary {
  segment: string;
  customerCount: number;
  totalSpend: number;
}

export interface MerchantOrderItem {
  productId: string;
  productName?: string;
  quantity: number;
  variantId?: string;
  sku?: string;
  unitPrice: number;
}

export interface MerchantOrder {
  id: string;
  merchantId: string;
  storeId?: string;
  customerPhone: string;
  customerName?: string;
  items: MerchantOrderItem[];
  subtotal: number;
  tax: number;
  taxRate?: number;
  total: number;
  status: string;
  refundAmount?: number;
  paidAt?: string;
  fulfilledAt?: string;
  createdAt?: string;
}

export interface ProductVariant {
  id: string;
  productId: string;
  sku: string;
  name: string;
  priceDelta: number;
  quantity: number;
  createdAt?: string;
}

export interface Refund {
  id: string;
  orderId: string;
  transactionId?: string;
  customerPhone: string;
  amount: number;
  reason?: string;
  status: string;
  createdAt?: string;
}

export interface ApprovalRequest {
  id: string;
  type: string;
  amount: number;
  refId: string;
  initiatorName?: string;
  status: string;
  reviewedBy?: string;
  reviewedAt?: string;
  createdAt?: string;
}

export interface CustomerDetail {
  phone: string;
  name?: string;
  totalSpent: number;
  orderCount: number;
  refundCount: number;
  refundedAmount: number;
  avgOrderValue: number;
  lastOrderDaysAgo: number;
  churnRisk: boolean;
  byMonth: Record<string, number>;
}

export interface CustomerTimelineEntry {
  type: string;
  title: string;
  detail?: string;
  at?: string;
}

export interface CustomerNote {
  id: string;
  customerPhone: string;
  note: string;
  createdBy?: string;
  createdAt?: string;
}

export interface NotificationTemplate {
  id: string;
  name: string;
  channel: string;
  subject?: string;
  body: string;
  triggerEvent: string;
  enabled: boolean;
  createdAt?: string;
}

export interface TaxInvoice {
  id: string;
  invoiceNo: string;
  customerName?: string;
  customerPhone?: string;
  subtotal: number;
  tax: number;
  withholdingTax?: number;
  total: number;
  issueDate?: string;
  createdAt?: string;
}

export interface TaxSummary {
  grossRevenue: number;
  salesTaxCollected: number;
  withholdingTax: number;
  netRevenue: number;
  effectiveRatePct: number;
  invoices: TaxInvoice[];
}

export interface FeeCalculation {
  amount: number;
  fee: number;
  net: number;
  feeSchedule: string;
  feeRate: number;
}

export interface CashFlowMonth {
  month: string;
  revenue?: number;
  projection?: number;
}

export interface CashFlowForecast {
  months: CashFlowMonth[];
  projectedAnnual: number;
  averageMonthly: number;
  growthRatePct: number;
  seasonalAdjustment: number;
}

export interface MonitoringStatus {
  recentTransactions: number;
  failedTransactions: number;
  pendingRefunds: number;
  anomalyScore: number;
  alerts: string[];
}

export interface BestSeller {
  productId: string;
  productName: string;
  unitsSold: number;
  revenue: number;
}

export interface RepeatCustomer {
  customerPhone: string;
  orderCount: number;
  totalSpent: number;
  repeatRate: number;
}

export interface AccountingExport {
  accountCode: string;
  description: string;
  amount: number;
}

export interface ChargebackEvidence {
  id: string;
  chargebackId: string;
  type: string;
  reference?: string;
  content?: string;
  createdAt?: string;
}

export interface ReferralRegistration {
  id: string;
  programId?: string;
  referredPhone: string;
  status: string;
  bonusPaid?: number;
  createdAt?: string;
}

export interface ReferralPerformance {
  totalRegistrations: number;
  convertedRegistrations: number;
  pendingRegistrations: number;
  conversionRatePct: number;
  totalBonusPaid: number;
  registrations: ReferralRegistration[];
}
