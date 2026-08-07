import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { paymentLinksApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency, formatDate, copyToClipboard } from '../../utils';
import type { PaymentLink } from '../../types';

const emptyForm = { amount: '', description: '', customerPhone: '', customerName: '', autoFollowUp: false, followUpHours: '' };

export function MerchantPaymentLinksPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [links, setLinks] = useState<PaymentLink[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await paymentLinksApi.getMy(user.id, 0, 50);
      setLinks(data?.content ?? []);
    } catch (err) {
      console.error('Failed to load payment links', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const handleCreate = async () => {
    if (!user || !form.amount) return;
    setSubmitting(true);
    try {
      await paymentLinksApi.create(user.id, {
        amount: Number(form.amount),
        description: form.description.trim() || undefined,
        customerPhone: form.customerPhone.trim() || undefined,
        customerName: form.customerName.trim() || undefined,
        autoFollowUp: form.autoFollowUp || undefined,
        followUpHours: form.followUpHours ? Number(form.followUpHours) : undefined,
      });
      toast.success(t.paymentLinks.created);
      setForm(emptyForm);
      setShowForm(false);
      await load();
    } catch (err) {
      console.error('Failed to create payment link', err);
      toast.error(t.paymentLinks.createFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const handleCopy = async (link: PaymentLink) => {
    const url = `${window.location.origin}/pay/${link.token}`;
    await copyToClipboard(url);
    toast.success(t.paymentLinks.copied);
  };

  const handleDeactivate = async (link: PaymentLink) => {
    if (!user) return;
    try {
      await paymentLinksApi.deactivate(user.id, link.id);
      toast.success(t.common.success);
      await load();
    } catch (err) {
      console.error('Failed to deactivate link', err);
      toast.error(t.paymentLinks.deactivateFailed);
    }
  };

  const handleResend = async (link: PaymentLink) => {
    if (!user) return;
    try {
      await paymentLinksApi.resend(user.id, link.id);
      toast.success(t.paymentLinks.reminderSent);
      await load();
    } catch (err) {
      console.error('Failed to resend reminder', err);
      toast.error(t.paymentLinks.resendFailed);
    }
  };

  const set = (key: keyof typeof emptyForm, value: string | boolean) => setForm((f) => ({ ...f, [key]: value }));

  const statusBadge = (s: PaymentLink['status']) => {
    const map: Record<string, string> = {
      ACTIVE: 'bg-green-100 text-green-700',
      PAID: 'bg-blue-100 text-blue-700',
      EXPIRED: 'bg-gray-100 text-gray-600',
      DEACTIVATED: 'bg-red-100 text-red-700',
    };
    return map[s] || 'bg-gray-100 text-gray-600';
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t.paymentLinks.title}</h1>
        <Button onClick={() => setShowForm((v) => !v)}>
          {showForm ? t.common.cancel : t.paymentLinks.newLink}
        </Button>
      </div>

      {showForm && (
        <Card title={t.paymentLinks.newLink}>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            <Input type="number" label={t.paymentLinks.amount} value={form.amount} onChange={(e) => set('amount', e.target.value)} />
            <Input label={t.paymentLinks.description} value={form.description} onChange={(e) => set('description', e.target.value)} />
            <Input label={t.paymentLinks.customerPhone} value={form.customerPhone} onChange={(e) => set('customerPhone', e.target.value)} />
            <Input label={t.paymentLinks.customerName} value={form.customerName} onChange={(e) => set('customerName', e.target.value)} />
          </div>
          <div className="mt-4 flex items-center space-x-4">
            <label className="flex items-center space-x-2 text-sm text-gray-700">
              <input type="checkbox" checked={form.autoFollowUp} onChange={(e) => set('autoFollowUp', e.target.checked)} className="rounded" />
              <span>{t.paymentLinks.autoFollowUp}</span>
            </label>
            {form.autoFollowUp && (
              <Input type="number" min={1} placeholder={t.paymentLinks.followUpHoursPlaceholder} label={t.paymentLinks.followUpHours} value={form.followUpHours} onChange={(e) => set('followUpHours', e.target.value)} />
            )}
          </div>
          <div className="mt-4 flex justify-end">
            <Button onClick={handleCreate} loading={submitting} disabled={!form.amount}>
              {t.paymentLinks.create}
            </Button>
          </div>
        </Card>
      )}

      <Card>
        {loading ? (
          <div className="text-center py-12 text-gray-500">{t.common.loading}</div>
        ) : links.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.common.noData}</p>
        ) : (
          <div className="space-y-4">
            {links.map((link) => (
              <div key={link.id} className="flex flex-wrap items-center justify-between gap-4 border border-gray-200 rounded-lg p-4">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center space-x-3">
                    <h3 className="font-semibold text-gray-900">{formatCurrency(link.amount)}</h3>
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${statusBadge(link.status)}`}>{link.status}</span>
                  </div>
                  {link.description && <p className="text-sm text-gray-600 mt-1">{link.description}</p>}
                  <p className="text-xs text-gray-400 mt-1">
                    {t.paymentLinks.singleUse} · {formatDate(link.createdAt)}
                    {link.customerName ? ` · ${link.customerName}` : ''}
                    {link.reminderCount ? ` · ${t.paymentLinks.reminders}: ${link.reminderCount}` : ''}
                  </p>
                </div>
                <div className="flex items-center space-x-2">
                  {link.status === 'ACTIVE' && (
                    <>
                      {link.customerPhone && <Button variant="secondary" size="sm" onClick={() => handleResend(link)}>{t.paymentLinks.sendReminder}</Button>}
                      <Button variant="secondary" size="sm" onClick={() => handleCopy(link)}>{t.paymentLinks.copyLink}</Button>
                      <Button variant="danger" size="sm" onClick={() => handleDeactivate(link)}>{t.paymentLinks.deactivate}</Button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
