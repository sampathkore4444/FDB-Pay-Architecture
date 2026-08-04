import { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { toast } from 'sonner';
import { useAuthStore } from '../../store/authStore';
import { merchantApi, settlementApi, walletApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency, formatDate } from '../../utils';
import { useTranslation } from '../../i18n';
import type { Merchant } from '../../types';

export function MerchantPage() {
  const user = useAuthStore((s) => s.user);
  const { t } = useTranslation();
  const [balance, setBalance] = useState(0);
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [settlements, setSettlements] = useState<{ id: string; status: string; totalAmount: number; createdAt: string }[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({ businessName: '', businessType: '', category: '', address: '' });

  const isApplicant = user?.role === 'CONSUMER';

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
          await generateQrImage(merchantProfile.id);
        }
      } catch (err) {
        console.error('Failed to load merchant data', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [user]);

  const generateQrImage = async (merchantId: string): Promise<boolean> => {
    try {
      const qr = await merchantApi.generateQr(merchantId);
      if (!qr.qrData) return false;
      const dataUrl = await QRCode.toDataURL(qr.qrData, { width: 256, margin: 1 });
      setMerchant((m) => (m ? { ...m, qrStaticUrl: dataUrl } : m));
      return true;
    } catch {
      return false;
    }
  };

  const handleGenerateQr = async () => {
    if (!merchant) return;
    const ok = await generateQrImage(merchant.id);
    if (!ok) toast.error('QR generation failed');
  };

  const handleRegister = async () => {
    if (!user || !form.businessName.trim()) return;
    setSubmitting(true);
    try {
      const profile = await merchantApi.register(user.id, {
        businessName: form.businessName.trim(),
        businessType: form.businessType.trim() || undefined,
        category: form.category.trim() || undefined,
        address: form.address.trim() || undefined,
      });
      setMerchant(profile);
      toast.success(isApplicant ? t.merchant.apply : t.common.success);
    } catch (err) {
      console.error('Failed to register merchant', err);
      toast.error('Merchant registration failed');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="text-center py-12 text-gray-500">{t.common.loading}</div>;
  }

  if (!merchant) {
    return (
      <div className="max-w-lg mx-auto">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">{isApplicant ? t.merchant.apply : t.merchant.register}</h1>
        <Card title={t.merchant.profile}>
          <div className="space-y-4">
            <Input
              label={t.merchant.businessName}
              value={form.businessName}
              onChange={(e) => setForm((f) => ({ ...f, businessName: e.target.value }))}
              placeholder="e.g. Golden Tea Shop"
            />
            <Input
              label={t.merchant.businessType}
              value={form.businessType}
              onChange={(e) => setForm((f) => ({ ...f, businessType: e.target.value }))}
              placeholder="e.g. Restaurant"
            />
            <Input
              label={t.common.category}
              value={form.category}
              onChange={(e) => setForm((f) => ({ ...f, category: e.target.value }))}
              placeholder="e.g. food"
            />
            <Input
              label={t.merchant.address}
              value={form.address}
              onChange={(e) => setForm((f) => ({ ...f, address: e.target.value }))}
              placeholder="e.g. 123 Main Street, Yangon"
            />
            <Button className="w-full" onClick={handleRegister} disabled={submitting || !form.businessName.trim()}>
              {submitting ? t.common.loading : isApplicant ? t.merchant.submitApplication : t.common.submit}
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  if (merchant.status !== 'ACTIVE' && isApplicant) {
    return (
      <div className="max-w-xl mx-auto">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">{merchant.businessName}</h1>
        <Card>
          <div className="text-center py-6 space-y-4">
            <div className="mx-auto w-14 h-14 bg-yellow-100 rounded-full flex items-center justify-center">
              <span className="text-2xl">🕓</span>
            </div>
            <p className="text-gray-700">{t.merchant.applicationReview}</p>
            <div>
              <span className="inline-flex px-3 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                {t.common.pending}
              </span>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{merchant.businessName ?? 'Merchant Dashboard'}</h1>

      {merchant.status !== 'ACTIVE' && (
        <div className="bg-yellow-50 border border-yellow-200 text-yellow-800 rounded-lg px-4 py-3 text-sm">
          {t.merchant.profile} — {t.common.pending}
        </div>
      )}

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
          <Button variant="secondary" className="w-full" onClick={() => window.location.href = '/merchant/analytics'}>
            {t.nav.merchantAnalytics}
          </Button>
          <Button variant="secondary" className="w-full" onClick={() => window.location.href = '/merchant/reports'}>
            {t.nav.merchantReports}
          </Button>
          <Button variant="secondary" className="w-full" onClick={() => window.location.href = '/merchant/promotions'}>
            {t.nav.merchantPromotions}
          </Button>
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
