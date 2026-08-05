import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { financingApi, merchantApi, walletApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency, formatDate } from '../../utils';
import type { FinancingApplication, FinancingEligibility, Merchant } from '../../types';

const STATUS_STYLES: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  APPROVED: 'bg-blue-100 text-blue-700',
  DISBURSED: 'bg-green-100 text-green-700',
  DECLINED: 'bg-red-100 text-red-700',
};

export function FinancingPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [walletId, setWalletId] = useState('');
  const [eligibility, setEligibility] = useState<FinancingEligibility | null>(null);
  const [applications, setApplications] = useState<FinancingApplication[]>([]);
  const [loading, setLoading] = useState(true);
  const [showApply, setShowApply] = useState(false);
  const [amount, setAmount] = useState('');
  const [term, setTerm] = useState('6');
  const [purpose, setPurpose] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!user) return;
    merchantApi
      .getProfile(user.id)
      .then((m) => setMerchant(m))
      .catch((err) => {
        console.error('Failed to load merchant profile', err);
        toast.error(t.common.loadFailed);
      });
  }, [user]);

  const load = async () => {
    if (!user || !merchant) return;
    setLoading(true);
    try {
      const wallet = await walletApi.getWallet(merchant.userId);
      const [elig, apps] = await Promise.all([
        financingApi.getEligibility(user.id, wallet.id),
        financingApi.getApplications(user.id),
      ]);
      setWalletId(wallet.id);
      setEligibility(elig);
      setApplications(apps);
    } catch (err) {
      console.error('Failed to load financing', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (merchant) load();
  }, [merchant]);

  const handleApply = async () => {
    if (!user || !eligibility || !walletId) return;
    const requested = Number(amount);
    if (!requested || requested > eligibility.estimatedLimit) return;
    setSubmitting(true);
    try {
      await financingApi.apply(user.id, walletId, { requestedAmount: requested, termMonths: Number(term), purpose });
      toast.success(t.financing.applied);
      setShowApply(false);
      setAmount('');
      setPurpose('');
      await load();
    } catch (err) {
      console.error('Failed to apply', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const terms = Math.min(eligibility?.maxTermMonths ?? 12, 12);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.financing.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.financing.subtitle}</p>
        </div>
        {eligibility?.eligible && (
          <Button onClick={() => setShowApply(true)}>{t.financing.applyNow}</Button>
        )}
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : (
        <>
          <div className="grid md:grid-cols-3 gap-4">
            <Card>
              <p className="text-sm text-gray-500">{t.financing.estimatedLimit}</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">{formatCurrency(eligibility?.estimatedLimit ?? 0)}</p>
            </Card>
            <Card>
              <p className="text-sm text-gray-500">{t.financing.monthlyRevenue}</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">{formatCurrency(eligibility?.monthlyRevenue ?? 0)}</p>
            </Card>
            <Card>
              <p className="text-sm text-gray-500">{t.financing.avgDailySales}</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">{formatCurrency(eligibility?.avgDailySales ?? 0)}</p>
            </Card>
          </div>

          {eligibility?.eligible ? (
            <Card>
              <h3 className="font-semibold text-gray-900 mb-2">{t.financing.terms}</h3>
              <p className="text-sm text-gray-600">{t.financing.limitInfo}</p>
              <ul className="text-sm text-gray-600 list-disc pl-5 mt-2 space-y-1">
                <li>{t.financing.maxTerm} {eligibility.maxTermMonths} {t.financing.months}</li>
                <li>{t.financing.threeMonthVolume} {formatCurrency(eligibility.threeMonthVolume)}</li>
              </ul>
            </Card>
          ) : (
            <Card><p className="text-center text-gray-500 py-6">{t.financing.notEligible}</p></Card>
          )}

          <div>
            <h3 className="font-semibold text-gray-900 mb-3">{t.financing.applications}</h3>
            {applications.length === 0 ? (
              <Card><p className="text-center text-gray-500 py-6">{t.common.noData}</p></Card>
            ) : (
              <Card>
                <div className="space-y-3">
                  {applications.map((app) => (
                    <div key={app.id} className="flex flex-wrap items-center justify-between gap-3 border border-gray-200 rounded-lg p-4">
                      <div>
                        <div className="flex items-center space-x-3">
                          <span className="font-semibold text-gray-900">{formatCurrency(app.requestedAmount)}</span>
                          <span className={`px-2 py-0.5 rounded text-xs font-medium ${STATUS_STYLES[app.status] || 'bg-gray-100 text-gray-600'}`}>{app.status}</span>
                        </div>
                        <p className="text-xs text-gray-500 mt-1">{app.termMonths} {t.financing.months} · {formatDate(app.createdAt)}{app.adminNote ? ` · ${app.adminNote}` : ''}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
            )}
          </div>
        </>
      )}

      <Modal open={showApply} onClose={() => setShowApply(false)} title={t.financing.applyNow}>
        <div className="space-y-4">
          <Input type="number" label={t.financing.amount} value={amount} onChange={(e) => setAmount(e.target.value)} />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.financing.term}</label>
            <select value={term} onChange={(e) => setTerm(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-lg">
              {Array.from({ length: terms }, (_, i) => i + 1).map((m) => <option key={m} value={m}>{m} {t.financing.months}</option>)}
            </select>
          </div>
          <Input label={t.financing.purpose} value={purpose} onChange={(e) => setPurpose(e.target.value)} placeholder={t.financing.purposePlaceholder} />
          <p className="text-xs text-gray-500">{t.financing.applyNote}</p>
          <div className="flex space-x-3">
            <Button onClick={handleApply} loading={submitting} disabled={!amount || Number(amount) <= 0 || Number(amount) > (eligibility?.estimatedLimit ?? 0)} className="flex-1">{t.common.submit}</Button>
            <Button variant="secondary" onClick={() => setShowApply(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
