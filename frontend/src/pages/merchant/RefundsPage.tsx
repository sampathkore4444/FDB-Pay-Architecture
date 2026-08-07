import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { refundApi, orderApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency, formatDate } from '../../utils';
import type { Refund, MerchantOrder } from '../../types';

const emptyForm = { orderId: '', amount: '', reason: '', requireApproval: false };

export function RefundsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [refunds, setRefunds] = useState<Refund[]>([]);
  const [orders, setOrders] = useState<MerchantOrder[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const [r, o] = await Promise.all([refundApi.list(user.id, statusFilter || undefined), orderApi.list(user.id)]);
      setRefunds(r);
      setOrders(o.filter((ord) => !['CANCELLED', 'REFUNDED'].includes(ord.status) && (ord.refundAmount ?? 0) < (ord.total ?? 0)));
    } catch (err) {
      console.error('Failed to load refunds', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user, statusFilter]);

  const openCreate = () => {
    setForm({ ...emptyForm, orderId: orders[0]?.id || '' });
    setShowForm(true);
  };

  const onOrderSelect = (orderId: string) => {
    const order = orders.find((o) => o.id === orderId);
    const max = order ? (order.total ?? 0) - (order.refundAmount ?? 0) : 0;
    setForm((f) => ({ ...f, orderId, amount: String(max) }));
  };

  const submit = async () => {
    if (!user || !form.orderId || !form.amount) return;
    setSubmitting(true);
    try {
      await refundApi.create(user.id, {
        orderId: form.orderId,
        amount: Number(form.amount),
        reason: form.reason || undefined,
        requireApproval: form.requireApproval,
      });
      toast.success(t.refunds.created);
      setShowForm(false);
      await load();
    } catch (err) {
      console.error('Failed to create refund', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const statusBadge = (status: string) => {
    const map: Record<string, string> = {
      PENDING: 'bg-yellow-100 text-yellow-700',
      APPROVED: 'bg-blue-100 text-blue-700',
      REJECTED: 'bg-gray-100 text-gray-600',
      COMPLETED: 'bg-green-100 text-green-700',
    };
    return <span className={`px-2 py-0.5 rounded text-xs font-medium ${map[status] || 'bg-gray-100 text-gray-600'}`}>{status}</span>;
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.refunds.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.refunds.subtitle}</p>
        </div>
        {orders.length > 0 && <Button onClick={openCreate}>{t.refunds.newRefund}</Button>}
      </div>

      <div className="flex items-center space-x-3">
        {['', 'PENDING', 'APPROVED', 'COMPLETED', 'REJECTED'].map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${statusFilter === s ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}
          >
            {s === '' ? t.common.all : s}
          </button>
        ))}
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : refunds.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.refunds.noRefunds}</p></Card>
      ) : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                  <th className="pb-2 pr-4">{t.refunds.order}</th>
                  <th className="pb-2 pr-4">{t.refunds.customer}</th>
                  <th className="pb-2 pr-4">{t.refunds.amount}</th>
                  <th className="pb-2 pr-4">{t.refunds.reason}</th>
                  <th className="pb-2 pr-4">{t.refunds.status}</th>
                  <th className="pb-2">{t.refunds.date}</th>
                </tr>
              </thead>
              <tbody>
                {refunds.map((refund) => (
                  <tr key={refund.id} className="border-b border-gray-100">
                    <td className="py-2 pr-4 font-mono text-xs text-gray-500">{refund.orderId.slice(0, 8)}…</td>
                    <td className="py-2 pr-4 text-gray-900">{refund.customerPhone}</td>
                    <td className="py-2 pr-4 font-semibold text-gray-900">{formatCurrency(refund.amount)}</td>
                    <td className="py-2 pr-4 text-gray-600">{refund.reason || '-'}</td>
                    <td className="py-2 pr-4">{statusBadge(refund.status)}</td>
                    <td className="py-2 text-gray-500">{refund.createdAt ? formatDate(refund.createdAt) : '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <Modal open={showForm} onClose={() => setShowForm(false)} title={t.refunds.newRefund}>
        <div className="space-y-4">
          <select
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={form.orderId}
            onChange={(e) => onOrderSelect(e.target.value)}
          >
            {orders.map((o) => (
              <option key={o.id} value={o.id}>
                {o.id.slice(0, 8)}… {o.customerName || o.customerPhone} — {formatCurrency(o.total)}
              </option>
            ))}
          </select>
          <Input label={t.refunds.amount} type="number" min={0} value={form.amount} onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value }))} />
          <Input label={t.refunds.reason} value={form.reason} onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))} />
          <label className="flex items-center space-x-2 text-sm text-gray-700">
            <input type="checkbox" checked={form.requireApproval} onChange={(e) => setForm((f) => ({ ...f, requireApproval: e.target.checked }))} className="rounded" />
            <span>{t.refunds.requireApproval}</span>
          </label>
          <div className="flex space-x-3">
            <Button onClick={submit} loading={submitting} className="flex-1">{t.common.submit}</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
