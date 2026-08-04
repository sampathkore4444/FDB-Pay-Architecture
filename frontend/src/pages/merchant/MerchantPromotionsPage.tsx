import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { promotionsApi, merchantApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatDate } from '../../utils';
import type { Promotion, PromotionType } from '../../types';

const typeOptions: { value: PromotionType; label: string }[] = [
  { value: 'FIXED_DISCOUNT', label: 'Fixed Discount' },
  { value: 'PERCENTAGE_DISCOUNT', label: 'Percentage Discount' },
  { value: 'CASHBACK', label: 'Cashback' },
  { value: 'BOGO', label: 'BOGO' },
  { value: 'COUPON_CODE', label: 'Coupon Code' },
];

const emptyForm = {
  title: '',
  description: '',
  type: 'CASHBACK' as PromotionType,
  discountValue: '',
  maxDiscount: '',
  minTransactionAmount: '',
  maxUsageTotal: '100',
  maxUsagePerUser: '1',
  startDate: '',
  endDate: '',
  promoCode: '',
};

export function MerchantPromotionsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [merchantId, setMerchantId] = useState<string | null>(null);
  const [promotions, setPromotions] = useState<Promotion[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState(emptyForm);

  useEffect(() => {
    if (!user) return;
    merchantApi
      .getProfile(user.id)
      .then((m) => setMerchantId(m.id))
      .catch((err) => console.error('Failed to load merchant profile', err));
  }, [user]);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await promotionsApi.getMy(user.id, 0, 100);
      setPromotions(data?.content ?? []);
    } catch (err) {
      console.error('Failed to load promotions', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const handleCreate = async () => {
    if (!user || !merchantId || !form.title.trim() || !form.discountValue || !form.startDate || !form.endDate) return;
    setSubmitting(true);
    try {
      const toDateTime = (d: string) => (d ? `${d}T00:00:00Z` : undefined);
      await promotionsApi.create({
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        type: form.type,
        fundingType: 'MERCHANT',
        merchantId,
        discountValue: Number(form.discountValue),
        maxDiscount: form.maxDiscount ? Number(form.maxDiscount) : undefined,
        minTransactionAmount: form.minTransactionAmount ? Number(form.minTransactionAmount) : undefined,
        maxUsageTotal: Number(form.maxUsageTotal || 100),
        maxUsagePerUser: Number(form.maxUsagePerUser || 1),
        startDate: toDateTime(form.startDate),
        endDate: toDateTime(form.endDate),
        promoCode: form.promoCode.trim() || undefined,
      });
      toast.success(t.promotions.created);
      setForm(emptyForm);
      setShowForm(false);
      await load();
    } catch (err) {
      console.error('Failed to create promotion', err);
      toast.error(t.promotions.createFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleStatus = async (p: Promotion) => {
    const next = p.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
    try {
      await promotionsApi.setStatus(p.id, next);
      toast.success(t.common.success);
      await load();
    } catch (err) {
      console.error('Failed to update status', err);
      toast.error(t.promotions.statusUpdateFailed);
    }
  };

  const handleDelete = async (p: Promotion) => {
    try {
      await promotionsApi.deactivate(p.id);
      toast.success(t.common.success);
      await load();
    } catch (err) {
      console.error('Failed to deactivate promotion', err);
      toast.error(t.promotions.statusUpdateFailed);
    }
  };

  const set = (key: keyof typeof emptyForm, value: string) => setForm((f) => ({ ...f, [key]: value }));

  const statusBadge = (s: Promotion['status']) => {
    const map: Record<string, string> = {
      ACTIVE: 'bg-green-100 text-green-700',
      PAUSED: 'bg-amber-100 text-amber-700',
      DRAFT: 'bg-yellow-100 text-yellow-700',
      EXPIRED: 'bg-gray-100 text-gray-600',
    };
    return map[s] || 'bg-gray-100 text-gray-600';
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t.promotions.title}</h1>
        <Button onClick={() => setShowForm((v) => !v)}>
          {showForm ? t.common.cancel : t.promotions.newCampaign}
        </Button>
      </div>

      {showForm && (
        <Card title={t.promotions.newCampaign}>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            <Input label={t.promotions.title} value={form.title} onChange={(e) => set('title', e.target.value)} placeholder={t.promotions.titlePlaceholder} />
            <Input label={t.promotions.description} value={form.description} onChange={(e) => set('description', e.target.value)} />
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t.promotions.typeLabel}</label>
              <select
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={form.type}
                onChange={(e) => set('type', e.target.value)}
              >
                {typeOptions.map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </div>
            <Input type="number" label={t.promotions.discountValue} value={form.discountValue} onChange={(e) => set('discountValue', e.target.value)} />
            <Input type="number" label={t.promotions.maxDiscount} value={form.maxDiscount} onChange={(e) => set('maxDiscount', e.target.value)} />
            <Input type="number" label={t.promotions.minAmount} value={form.minTransactionAmount} onChange={(e) => set('minTransactionAmount', e.target.value)} />
            <Input type="number" label={t.promotions.maxUsageTotal} value={form.maxUsageTotal} onChange={(e) => set('maxUsageTotal', e.target.value)} />
            <Input type="number" label={t.promotions.maxUsagePerUser} value={form.maxUsagePerUser} onChange={(e) => set('maxUsagePerUser', e.target.value)} />
            <Input label={t.promotions.promoCode} value={form.promoCode} onChange={(e) => set('promoCode', e.target.value)} placeholder={t.promotions.codePlaceholder} />
            <Input type="date" label={t.promotions.validFrom} value={form.startDate} onChange={(e) => set('startDate', e.target.value)} />
            <Input type="date" label={t.promotions.validTo} value={form.endDate} onChange={(e) => set('endDate', e.target.value)} />
          </div>
          <div className="mt-4 flex justify-end">
            <Button onClick={handleCreate} loading={submitting} disabled={!form.title.trim() || !form.discountValue || !form.startDate || !form.endDate}>
              {t.promotions.create}
            </Button>
          </div>
        </Card>
      )}

      <Card>
        {loading ? (
          <div className="text-center py-12 text-gray-500">{t.common.loading}</div>
        ) : promotions.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.common.noData}</p>
        ) : (
          <div className="space-y-4">
            {promotions.map((p) => (
              <div key={p.id} className="flex flex-wrap items-center justify-between gap-4 border border-gray-200 rounded-lg p-4">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center space-x-3">
                    <h3 className="font-semibold text-gray-900 truncate">{p.title}</h3>
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${statusBadge(p.status)}`}>{p.status}</span>
                  </div>
                  {p.promoCode && <p className="text-sm text-blue-600 font-medium mt-1">CODE: {p.promoCode}</p>}
                  <p className="text-sm text-gray-500 mt-1">
                    {p.type} · {p.discountValue}{p.type === 'PERCENTAGE_DISCOUNT' ? '%' : ' MMK'}
                    {p.minTransactionAmount != null && p.minTransactionAmount > 0 ? ` · Min ${p.minTransactionAmount}` : ''}
                  </p>
                  <p className="text-xs text-gray-400 mt-1">
                    {formatDate(p.startDate)} → {formatDate(p.endDate)} · {t.promotions.usageLimit}: {p.usageCount}/{p.maxUsageTotal}
                  </p>
                </div>
                <div className="flex items-center space-x-2">
                  {p.status === 'ACTIVE' ? (
                    <Button variant="secondary" size="sm" onClick={() => toggleStatus(p)}>{t.promotions.pause}</Button>
                  ) : p.status === 'PAUSED' ? (
                    <Button size="sm" onClick={() => toggleStatus(p)}>{t.promotions.activate}</Button>
                  ) : null}
                  <Button variant="danger" size="sm" onClick={() => handleDelete(p)}>{t.promotions.delete}</Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
