import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'sonner';
import { I18nProvider } from './i18n';
import { DashboardLayout } from './components/layout/DashboardLayout';
import { RoleGuard } from './components/auth/RoleGuard';
import { LoginPage } from './pages/auth/LoginPage';
import { WalletPage } from './pages/wallet/WalletPage';
import { TransferPage } from './pages/transfer/TransferPage';
import { RequestMoneyPage } from './pages/transfer/RequestMoneyPage';
import { BillsPage } from './pages/bills/BillsPage';
import { MerchantPage } from './pages/merchant/MerchantPage';
import { MerchantAnalyticsPage } from './pages/merchant/MerchantAnalyticsPage';
import { MerchantReportsPage } from './pages/merchant/MerchantReportsPage';
import { MerchantPromotionsPage } from './pages/merchant/MerchantPromotionsPage';
import { MerchantPaymentLinksPage } from './pages/merchant/MerchantPaymentLinksPage';
import { VirtualTerminalPage } from './pages/merchant/VirtualTerminalPage';
import { DynamicQrPage } from './pages/merchant/DynamicQrPage';
import { BulkOperationsPage } from './pages/merchant/BulkOperationsPage';
import { StatementsPage } from './pages/merchant/StatementsPage';
import { StoresPage } from './pages/merchant/StoresPage';
import { ChargebacksPage } from './pages/merchant/ChargebacksPage';
import { FinancingPage } from './pages/merchant/FinancingPage';
import { RiskAlertsPage } from './pages/merchant/RiskAlertsPage';
import { ReconciliationPage } from './pages/merchant/ReconciliationPage';
import { BusinessDashboardPage } from './pages/merchant/BusinessDashboardPage';
import { PaymentLinkPayPage } from './pages/payments/PaymentLinkPayPage';
import { InvoicesPage } from './pages/merchant/InvoicesPage';
import { InventoryPage } from './pages/merchant/InventoryPage';
import { AgentPage } from './pages/agent/AgentPage';
import { AdminPage } from './pages/admin/AdminPage';
import { KycReviewPage } from './pages/admin/KycReviewPage';
import { AmlAlertsPage } from './pages/admin/AmlAlertsPage';
import { UserManagementPage } from './pages/admin/UserManagementPage';
import { MerchantManagementPage } from './pages/admin/MerchantManagementPage';
import { ReferenceDataPage } from './pages/admin/ReferenceDataPage';
import { CorporatePage } from './pages/corporate/CorporatePage';
import { AirtimeTopupPage } from './pages/airtime/AirtimeTopupPage';
import { SavingsPocketsPage } from './pages/savings/SavingsPocketsPage';
import { DisputesPage } from './pages/disputes/DisputesPage';
import { SettlementsPage } from './pages/settlements/SettlementsPage';
import { AuditLogPage } from './pages/audit/AuditLogPage';
import { StaffManagementPage } from './pages/staff/StaffManagementPage';
import { MerchantDirectoryPage } from './pages/directory/MerchantDirectoryPage';
import { ScheduledPaymentsPage } from './pages/scheduled/ScheduledPaymentsPage';
import { PayrollPage } from './pages/payroll/PayrollPage';
import { RemittanceReceivePage } from './pages/remittance/RemittanceReceivePage';
import { PromotionsPage } from './pages/promotions/PromotionsPage';
import { SupportPage } from './pages/support/SupportPage';

const CONSUMER = ['CONSUMER'] as const;
const MERCHANT = ['MERCHANT'] as const;
const AGENT = ['AGENT'] as const;
const CORPORATE = ['CORPORATE'] as const;
const ADMIN = ['ADMIN'] as const;
const CONSUMER_MERCHANT = ['CONSUMER', 'MERCHANT'] as const;
const ALL = ['CONSUMER', 'MERCHANT', 'AGENT', 'CORPORATE', 'ADMIN'] as const;

function App() {
  return (
    <I18nProvider>
      <BrowserRouter>
        <Toaster position="top-right" />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<LoginPage />} />
          <Route path="/pay/:token" element={<PaymentLinkPayPage />} />

          <Route element={<DashboardLayout />}>
            {/* Consumer pages */}
            <Route path="/wallet" element={<RoleGuard allowedRoles={ALL}><WalletPage /></RoleGuard>} />
            <Route path="/transfer" element={<RoleGuard allowedRoles={ALL}><TransferPage /></RoleGuard>} />
            <Route path="/request-money" element={<RoleGuard allowedRoles={CONSUMER}><RequestMoneyPage /></RoleGuard>} />
            <Route path="/bills" element={<RoleGuard allowedRoles={CONSUMER}><BillsPage /></RoleGuard>} />
            <Route path="/airtime" element={<RoleGuard allowedRoles={CONSUMER}><AirtimeTopupPage /></RoleGuard>} />
            <Route path="/savings" element={<RoleGuard allowedRoles={CONSUMER}><SavingsPocketsPage /></RoleGuard>} />
            <Route path="/scheduled" element={<RoleGuard allowedRoles={CONSUMER}><ScheduledPaymentsPage /></RoleGuard>} />
            <Route path="/remittance" element={<RoleGuard allowedRoles={CONSUMER}><RemittanceReceivePage /></RoleGuard>} />
            <Route path="/promotions" element={<RoleGuard allowedRoles={CONSUMER_MERCHANT}><PromotionsPage /></RoleGuard>} />
            <Route path="/directory" element={<RoleGuard allowedRoles={CONSUMER}><MerchantDirectoryPage /></RoleGuard>} />

            {/* Merchant pages */}
            <Route path="/merchant" element={<RoleGuard allowedRoles={CONSUMER_MERCHANT}><MerchantPage /></RoleGuard>} />
            <Route path="/merchant/analytics" element={<RoleGuard allowedRoles={MERCHANT}><MerchantAnalyticsPage /></RoleGuard>} />
            <Route path="/merchant/reports" element={<RoleGuard allowedRoles={MERCHANT}><MerchantReportsPage /></RoleGuard>} />
            <Route path="/merchant/promotions" element={<RoleGuard allowedRoles={MERCHANT}><MerchantPromotionsPage /></RoleGuard>} />
            <Route path="/merchant/payment-links" element={<RoleGuard allowedRoles={MERCHANT}><MerchantPaymentLinksPage /></RoleGuard>} />
            <Route path="/merchant/terminal" element={<RoleGuard allowedRoles={MERCHANT}><VirtualTerminalPage /></RoleGuard>} />
            <Route path="/merchant/qr" element={<RoleGuard allowedRoles={MERCHANT}><DynamicQrPage /></RoleGuard>} />
            <Route path="/merchant/bulk" element={<RoleGuard allowedRoles={MERCHANT}><BulkOperationsPage /></RoleGuard>} />
            <Route path="/merchant/statements" element={<RoleGuard allowedRoles={MERCHANT}><StatementsPage /></RoleGuard>} />
            <Route path="/merchant/stores" element={<RoleGuard allowedRoles={MERCHANT}><StoresPage /></RoleGuard>} />
            <Route path="/merchant/chargebacks" element={<RoleGuard allowedRoles={MERCHANT}><ChargebacksPage /></RoleGuard>} />
            <Route path="/merchant/financing" element={<RoleGuard allowedRoles={MERCHANT}><FinancingPage /></RoleGuard>} />
            <Route path="/merchant/risk-alerts" element={<RoleGuard allowedRoles={MERCHANT}><RiskAlertsPage /></RoleGuard>} />
            <Route path="/merchant/reconciliation" element={<RoleGuard allowedRoles={MERCHANT}><ReconciliationPage /></RoleGuard>} />
            <Route path="/merchant/dashboard" element={<RoleGuard allowedRoles={MERCHANT}><BusinessDashboardPage /></RoleGuard>} />
            <Route path="/invoices" element={<RoleGuard allowedRoles={MERCHANT}><InvoicesPage /></RoleGuard>} />
            <Route path="/inventory" element={<RoleGuard allowedRoles={MERCHANT}><InventoryPage /></RoleGuard>} />
            <Route path="/staff" element={<RoleGuard allowedRoles={MERCHANT}><StaffManagementPage /></RoleGuard>} />
            <Route path="/settlements" element={<RoleGuard allowedRoles={MERCHANT}><SettlementsPage /></RoleGuard>} />

            {/* Agent pages */}
            <Route path="/agent" element={<RoleGuard allowedRoles={AGENT}><AgentPage /></RoleGuard>} />

            {/* Corporate pages */}
            <Route path="/corporate" element={<RoleGuard allowedRoles={CORPORATE}><CorporatePage /></RoleGuard>} />
            <Route path="/payroll" element={<RoleGuard allowedRoles={CORPORATE}><PayrollPage /></RoleGuard>} />

            {/* Support (all roles) */}
            <Route path="/support" element={<RoleGuard allowedRoles={ALL}><SupportPage /></RoleGuard>} />

            {/* Disputes (all roles) */}
            <Route path="/disputes" element={<RoleGuard allowedRoles={ALL}><DisputesPage /></RoleGuard>} />

            {/* Admin pages */}
            <Route path="/admin" element={<RoleGuard allowedRoles={ADMIN}><AdminPage /></RoleGuard>} />
            <Route path="/admin/kyc" element={<RoleGuard allowedRoles={ADMIN}><KycReviewPage /></RoleGuard>} />
            <Route path="/admin/aml" element={<RoleGuard allowedRoles={ADMIN}><AmlAlertsPage /></RoleGuard>} />
            <Route path="/admin/users" element={<RoleGuard allowedRoles={ADMIN}><UserManagementPage /></RoleGuard>} />
            <Route path="/admin/merchants" element={<RoleGuard allowedRoles={ADMIN}><MerchantManagementPage /></RoleGuard>} />
            <Route path="/admin/refdata" element={<RoleGuard allowedRoles={ADMIN}><ReferenceDataPage /></RoleGuard>} />
            <Route path="/audit" element={<RoleGuard allowedRoles={ADMIN}><AuditLogPage /></RoleGuard>} />
          </Route>

          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </I18nProvider>
  );
}

export default App;
