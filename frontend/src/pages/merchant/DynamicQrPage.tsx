import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { merchantApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { copyToClipboard, formatCurrency } from '../../utils';
import type { Merchant } from '../../types';

export function DynamicQrPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [amount, setAmount] = useState('');
  const [qrUrl, setQrUrl] = useState('');
  const [generating, setGenerating] = useState(false);

  useEffect(() => {
    if (!user) return;
    merchantApi
      .getProfile(user.id)
      .then((m) => {
        setMerchant(m);
        setQrUrl(m.qrStaticUrl || '');
      })
      .catch(() => toast.error(t.common.loadFailed));
  }, [user]);

  const handleGenerate = async () => {
    if (!merchant) return;
    setGenerating(true);
    try {
      const res = await merchantApi.generateQr(merchant.id, 'dynamic', amount ? Number(amount) : undefined);
      setQrUrl(res.qrUrl);
      toast.success(t.dynamicQr.generated);
    } catch (err) {
      console.error('QR generation failed', err);
      toast.error(t.dynamicQr.generateFailed);
    } finally {
      setGenerating(false);
    }
  };

  const handleCopy = async () => {
    await copyToClipboard(qrUrl);
    toast.success(t.paymentLinks.copied);
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.dynamicQr.title}</h1>
      <p className="text-sm text-gray-500 -mt-4">{t.dynamicQr.subtitle}</p>

      <div className="grid md:grid-cols-2 gap-6">
        <Card title={t.dynamicQr.enterAmount}>
          <div className="space-y-4">
            <Input type="number" label={t.virtualTerminal.amount} value={amount} onChange={(e) => setAmount(e.target.value)} placeholder={t.dynamicQr.amountPlaceholder} />
            <div className="flex gap-2">
              <Button onClick={handleGenerate} loading={generating}>
                {t.dynamicQr.generate}
              </Button>
              {qrUrl && <Button variant="secondary" onClick={handleCopy}>{t.paymentLinks.copyLink}</Button>}
            </div>
            <p className="text-xs text-gray-400">{t.dynamicQr.oneTimeHint}</p>
          </div>
        </Card>

        <Card title={t.dynamicQr.preview}>
          <div className="flex flex-col items-center justify-center space-y-3 py-4">
            <img
              src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(qrUrl || merchant?.qrStaticUrl || '')}`}
              alt="QR code"
              className="w-48 h-48 border border-gray-200 rounded-lg"
            />
            {amount ? (
              <p className="font-semibold text-gray-900">{formatCurrency(Number(amount))}</p>
            ) : (
              <p className="text-sm text-gray-500">{merchant?.businessName}</p>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
}
