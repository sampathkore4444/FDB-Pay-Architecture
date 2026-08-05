import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { authApi, paymentLinksApi, merchantApi, walletApi, transferApi } from '../../services/api';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency } from '../../utils';
import type { PaymentLinkPublic } from '../../types';

export function PaymentLinkPayPage() {
  const { token } = useParams<{ token: string }>();
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const login = useAuthStore((s) => s.login);
  const [link, setLink] = useState<PaymentLinkPublic | null>(null);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [failed, setFailed] = useState('');
  const [paid, setPaid] = useState(false);
  const [form, setForm] = useState({ phone: '', pin: '' });
  const [loginLoading, setLoginLoading] = useState(false);

  useEffect(() => {
    if (!token) return;
    paymentLinksApi
      .getByToken(token)
      .then((data) => {
        setLink(data);
        if (data.status !== 'ACTIVE') setFailed(t.paymentLinks.notActive);
      })
      .catch(() => setFailed(t.paymentLinks.invalid))
      .finally(() => setLoading(false));
  }, [token]);

  const handleLogin = async () => {
    setLoginLoading(true);
    try {
      const res = await authApi.login({ phone: form.phone, pin: form.pin });
      login(res.user, res.accessToken, res.refreshToken);
    } catch {
      toast.error(t.common.error);
    } finally {
      setLoginLoading(false);
    }
  };

  const handlePay = async () => {
    if (!user || !link) return;
    setPaying(true);
    try {
      const merchant = await merchantApi.getProfile(link.merchantId);
      const wallet = await walletApi.getWallet(merchant.userId);
      await transferApi.initiate(user.id, {
        recipientIdentifier: wallet.id,
        amount: link.amount,
        type: 'PAYMENT_LINK',
        description: link.description ? `Payment link - ${link.description}` : 'Payment link',
      });
      await paymentLinksApi.markPaid(link.token);
      setPaid(true);
      toast.success(t.paymentLinks.paymentSuccessful);
    } catch (err) {
      console.error('Payment failed', err);
      toast.error(t.paymentLinks.paymentFailed);
    } finally {
      setPaying(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-500">{t.common.loading}</p>
      </div>
    );
  }

  if (failed || !link) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 max-w-sm w-full text-center">
          <h1 className="text-xl font-bold text-gray-900 mb-2">{t.paymentLinks.invalid}</h1>
          <p className="text-gray-500 text-sm">{failed || t.paymentLinks.invalid}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 max-w-md w-full">
        <p className="text-sm text-gray-400">{t.paymentLinks.payFrom}</p>
        <h1 className="text-2xl font-bold text-gray-900 mt-1">{link.merchantName}</h1>
        {link.description && <p className="text-gray-600 mt-2">{link.description}</p>}

        <div className="mt-6 bg-gray-50 rounded-lg p-4 flex items-center justify-between">
          <span className="text-gray-600">{t.paymentLinks.amount}</span>
          <span className="text-2xl font-bold text-green-600">{formatCurrency(link.amount)}</span>
        </div>

        {paid ? (
          <div className="mt-6 text-center bg-green-50 border border-green-200 rounded-lg p-4">
            <p className="text-green-700 font-medium">{t.paymentLinks.paymentSuccessful}</p>
          </div>
        ) : user ? (
          <div className="mt-6 space-y-3">
            <Button className="w-full" onClick={handlePay} loading={paying}>
              {t.paymentLinks.payNow}
            </Button>
          </div>
        ) : (
          <div className="mt-6 space-y-4">
            <p className="text-sm text-gray-600">{t.paymentLinks.loginToPay}</p>
            <Input placeholder={t.paymentLinks.phone} value={form.phone} onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))} />
            <Input type="password" placeholder={t.paymentLinks.pin} value={form.pin} onChange={(e) => setForm((f) => ({ ...f, pin: e.target.value }))} />
            <Button className="w-full" onClick={handleLogin} loading={loginLoading} disabled={!form.phone || !form.pin}>
              {t.paymentLinks.login}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
