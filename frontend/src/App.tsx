import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'sonner';
import { DashboardLayout } from './components/layout/DashboardLayout';
import { LoginPage } from './pages/auth/LoginPage';
import { WalletPage } from './pages/wallet/WalletPage';
import { TransferPage } from './pages/transfer/TransferPage';
import { BillsPage } from './pages/bills/BillsPage';
import { MerchantPage } from './pages/merchant/MerchantPage';
import { AgentPage } from './pages/agent/AgentPage';
import { AdminPage } from './pages/admin/AdminPage';
import { CorporatePage } from './pages/corporate/CorporatePage';

function App() {
  return (
    <BrowserRouter>
      <Toaster position="top-right" />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<LoginPage />} />

        <Route element={<DashboardLayout />}>
          <Route path="/wallet" element={<WalletPage />} />
          <Route path="/transfer" element={<TransferPage />} />
          <Route path="/bills" element={<BillsPage />} />
          <Route path="/merchant" element={<MerchantPage />} />
          <Route path="/agent" element={<AgentPage />} />
          <Route path="/admin" element={<AdminPage />} />
          <Route path="/corporate" element={<CorporatePage />} />
        </Route>

        <Route path="*" element={<Navigate to="/wallet" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
