import { useEffect, useState } from 'react';
import { useAuthStore } from '../../store/authStore';
import { merchantApi, settlementApi, walletApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { formatCurrency, formatDate } from '../../utils';
import { useTranslation } from '../../i18n';

export function MerchantPage() {
  const user = useAuthStore((s) => s.user);
  const { t } = useTranslation();
  const [balance, setBalance] = useState(0);
  const [merchant, setMerchant] = useState<{ id: string; businessName: string; qrStaticUrl?: string } | null>(null);
  const [settlements, setSettlements] = useState<{ id: string; status: string; totalAmount: number; createdAt: string }[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    const fetchData = async () => {
      try {
        const [wallet, merchantProfile] = await Promise.all([
          walletApi.getWallet(user.id).catch(() => null),
          merchantApi.getProfile(user.id).catch(() => null),
        ]);
        if (wallet) setBalance(wallet.balanceTotal);
        if (merchantProfile) {
          setMerchant(merchantProfile);
          const settlementData = await settlementApi.getMerchantSettlements(merchantProfile.id).catch(() => []);
          setSettlements(settlementData);
        }
      } catch (err) {
        console.error('Failed to load merchant data', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [user]);

  const handleGenerateQr = async () => {
    if (!merchant) return;
    try {
      const qr = await merchantApi.generateQr(merchant.id);
      setMerchant({ ...merchant, qrStaticUrl: qr.qrData });
    } catch {
      alert('QR generation failed');
    }
  };

  if (loading) {
    return <div className="text-center py-12 text-gray-500">{t.common.loading}</div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{merchant?.businessName ?? 'Merchant Dashboard'}</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card title={t.wallet.balance}>
          <p className="text-3xl font-bold text-green-600">{formatCurrency(balance)}</p>
          <p className="text-sm text-gray-500 mt-1">{t.merchant.nextSettlement}</p>
        </Card>

        <Card title={t.merchant.qrCode}>
          <div className="text-center py-4">
            {merchant?.qrStaticUrl ? (
              <div className="w-48 h-48 bg-white border rounded-lg mx-auto flex items-center justify-center p-2">
                <img src={merchant.qrStaticUrl} alt="QR Code" className="max-w-full max-h-full" />
              </div>
            ) : (
              <div className="w-48 h-48 bg-gray-100 rounded-lg mx-auto flex items-center justify-center">
                <p className="text-gray-400">QR Code</p>
              </div>
            )}
            <Button variant="secondary" className="mt-4" onClick={handleGenerateQr}>
              {t.merchant.generateQr}
            </Button>
          </div>
        </Card>

        <Card title={t.merchant.recentSettlements}>
          {settlements.length === 0 ? (
            <p className="text-center text-gray-500 py-4">{t.common.noData}</p>
          ) : (
            <div className="space-y-2">
              {settlements.slice(0, 5).map((s) => (
                <div key={s.id} className="flex justify-between items-center text-sm">
                  <span className="text-gray-600">{formatDate(s.createdAt)}</span>
                  <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                    s.status === 'COMPLETED' ? 'bg-green-100 text-green-700' :
                    s.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' :
                    'bg-gray-100 text-gray-700'
                  }`}>{s.status}</span>
                  <span className="font-medium">{formatCurrency(s.totalAmount)}</span>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      <Card title={t.merchant.quickActions}>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Button variant="secondary" className="w-full" onClick={() => window.location.href = '/invoices'}>
            {t.nav.invoices}
          </Button>
          <Button variant="secondary" className="w-full" onClick={() => window.location.href = '/staff'}>
            {t.nav.staff}
          </Button>
          <Button variant="secondary" className="w-full" onClick={() => window.location.href = '/settlements'}>
            {t.nav.settlements}
          </Button>
          <Button variant="secondary" className="w-full" onClick={() => window.location.href = '/inventory'}>
            {t.nav.inventory}
          </Button>
        </div>
      </Card>
    </div>
  );
}
