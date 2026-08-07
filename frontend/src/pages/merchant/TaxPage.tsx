import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { taxApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency, formatDate } from '../../utils';
import type { TaxInvoice, TaxSummary } from '../../types';

const emptyForm = { customerName: '', customerPhone: '', subtotal: '', tax: '', withholdingTax: '' };

export function TaxPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [invoices, setInvoices] = useState<TaxInvoice[]>([]);
  const [summary, setSummary] = useState<TaxSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const [inv, sum] = await Promise.all([taxApi.listInvoices(user.id), taxApi.summary(user.id)]);
      setInvoices(inv);
      setSummary(sum);
    } catch (err) {
      console.error('Failed to load tax data', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const submit = async () => {
    if (!user) return;
    setSubmitting(true);
    try {
      await taxApi.createInvoice(user.id, {
        customerName: form.customerName || undefined,
        customerPhone: form.customerPhone || undefined,
        subtotal: Number(form.subtotal),
        tax: Number(form.tax),
        withholdingTax: form.withholdingTax ? Number(form.withholdingTax) : undefined,
      });
      toast.success(t.tax.created);
      setShowForm(false);
      setForm(emptyForm);
      await load();
    } catch (err) {
      console.error('Failed to create tax invoice', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const summaryCards = summary
    ? [
        { label: t.tax.grossRevenue, value: formatCurrency(summary.grossRevenue) },
        { label: t.tax.salesTax, value: formatCurrency(summary.salesTaxCollected) },
        { label: t.tax.withholdingTax, value: formatCurrency(summary.withholdingTax) },
        { label: t.tax.netRevenue, value: formatCurrency(summary.netRevenue) },
        { label: t.tax.effectiveRate, value: `${summary.effectiveRatePct.toFixed(2)}%` },
      ]
    : [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.tax.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.tax.subtitle}</p>
        </div>
        <Button onClick={() => { setForm(emptyForm); setShowForm(true); }}>{t.tax.newInvoice}</Button>
      </div>

      <Card title={t.tax.summaryTitle}>
        <div className="grid sm:grid-cols-2 lg:grid-cols-5 gap-4">
          {summaryCards.map((s) => (
            <div key={s.label} className="border border-gray-200 rounded-lg p-4">
              <p className="text-xs uppercase tracking-wide text-gray-400">{s.label}</p>
              <p className="text-lg font-bold text-gray-900 mt-1">{s.value}</p>
            </div>
          ))}
        </div>
      </Card>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : invoices.length === 0 ? (
        <Card title={t.tax.invoicesTitle}><p className="text-center text-gray-500 py-6">{t.tax.noInvoices}</p></Card>
      ) : (
        <Card title={t.tax.invoicesTitle}>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                  <th className="pb-2 pr-4">{t.tax.invoiceNo}</th>
                  <th className="pb-2 pr-4">{t.tax.customer}</th>
                  <th className="pb-2 pr-4">{t.tax.subtotal}</th>
                  <th className="pb-2 pr-4">{t.tax.tax}</th>
                  <th className="pb-2 pr-4">{t.tax.total}</th>
                  <th className="pb-2">{t.tax.date}</th>
                </tr>
              </thead>
              <tbody>
                {invoices.map((inv) => (
                  <tr key={inv.id} className="border-b border-gray-100">
                    <td className="py-2 pr-4 font-mono text-xs font-semibold text-blue-700">{inv.invoiceNo}</td>
                    <td className="py-2 pr-4">
                      <p className="font-medium text-gray-900">{inv.customerName || '-'}</p>
                      {inv.customerPhone && <p className="text-xs text-gray-400">{inv.customerPhone}</p>}
                    </td>
                    <td className="py-2 pr-4 text-gray-600">{formatCurrency(inv.subtotal)}</td>
                    <td className="py-2 pr-4 text-gray-600">{formatCurrency(inv.tax)}</td>
                    <td className="py-2 pr-4 font-semibold text-gray-900">{formatCurrency(inv.total)}</td>
                    <td className="py-2 text-gray-500">{inv.createdAt ? formatDate(inv.createdAt) : '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <Modal open={showForm} onClose={() => setShowForm(false)} title={t.tax.newInvoice}>
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.tax.customerName} value={form.customerName} onChange={(e) => setForm((f) => ({ ...f, customerName: e.target.value }))} />
            <Input label={t.tax.customerPhone} value={form.customerPhone} onChange={(e) => setForm((f) => ({ ...f, customerPhone: e.target.value }))} />
          </div>
          <Input label={t.tax.subtotal} type="number" min={0} value={form.subtotal} onChange={(e) => setForm((f) => ({ ...f, subtotal: e.target.value }))} />
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.tax.tax} type="number" min={0} value={form.tax} onChange={(e) => setForm((f) => ({ ...f, tax: e.target.value }))} />
            <Input label={t.tax.withholding} type="number" min={0} value={form.withholdingTax} onChange={(e) => setForm((f) => ({ ...f, withholdingTax: e.target.value }))} />
          </div>
          <div className="flex space-x-3">
            <Button onClick={submit} loading={submitting} disabled={!form.subtotal} className="flex-1">{t.common.create}</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
