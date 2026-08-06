import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { recurringBillingApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency } from '../../utils';
import type { RecurringPlan } from '../../types';

const emptyForm = {
  name: '',
  description: '',
  amount: '',
  customerPhone: '',
  customerName: '',
  interval: 'MONTHLY',
  dayOfMonth: '1',
  time: '09:00',
  maxCharges: '',
};

export function RecurringBillingPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [plans, setPlans] = useState<RecurringPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<RecurringPlan | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setPlans(await recurringBillingApi.list(user.id));
    } catch (err) {
      console.error('Failed to load recurring plans', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setShowForm(true);
  };

  const openEdit = (plan: RecurringPlan) => {
    setEditing(plan);
    setForm({
      name: plan.name,
      description: plan.description || '',
      amount: String(plan.amount),
      customerPhone: plan.customerPhone,
      customerName: plan.customerName || '',
      interval: plan.interval || 'MONTHLY',
      dayOfMonth: plan.dayOfMonth ? String(plan.dayOfMonth) : '1',
      time: plan.time || '09:00',
      maxCharges: plan.maxCharges ? String(plan.maxCharges) : '',
    });
    setShowForm(true);
  };

  const handleSave = async () => {
    if (!user || !form.name || !form.amount) return;
    setSubmitting(true);
    try {
      const payload = {
        name: form.name,
        description: form.description || undefined,
        amount: Number(form.amount),
        customerPhone: form.customerPhone,
        customerName: form.customerName || undefined,
        interval: form.interval,
        dayOfMonth: form.interval === 'MONTHLY' ? Number(form.dayOfMonth) : undefined,
        time: form.time || undefined,
        maxCharges: form.maxCharges ? Number(form.maxCharges) : undefined,
      };
      if (editing) {
        await recurringBillingApi.update(user.id, editing.id, payload);
        toast.success(t.recurring.updated);
      } else {
        await recurringBillingApi.create(user.id, payload);
        toast.success(t.recurring.created);
      }
      setShowForm(false);
      await load();
    } catch (err) {
      console.error('Failed to save plan', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleStatus = async (plan: RecurringPlan) => {
    if (!user) return;
    try {
      await recurringBillingApi.setStatus(user.id, plan.id, plan.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE');
      toast.success(plan.status === 'ACTIVE' ? t.recurring.paused : t.recurring.activated);
      await load();
    } catch (err) {
      console.error('Failed to toggle plan', err);
      toast.error(t.common.loadFailed);
    }
  };

  const runNow = async (plan: RecurringPlan) => {
    if (!user) return;
    try {
      await recurringBillingApi.runNow(user.id, plan.id);
      toast.success(t.recurring.runSuccess);
      await load();
    } catch (err) {
      console.error('Failed to run plan', err);
      toast.error(t.common.loadFailed);
    }
  };

  const removePlan = async (plan: RecurringPlan) => {
    if (!user || !window.confirm(t.recurring.deleteConfirm)) return;
    try {
      await recurringBillingApi.delete(user.id, plan.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to delete plan', err);
      toast.error(t.common.loadFailed);
    }
  };

  const set = (key: keyof typeof emptyForm, value: string) => setForm((f) => ({ ...f, [key]: value }));

  const statusStyle = (status: string) =>
    status === 'ACTIVE' ? 'bg-green-100 text-green-700' : status === 'PAUSED' ? 'bg-yellow-100 text-yellow-700' : 'bg-gray-100 text-gray-600';

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.recurring.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.recurring.subtitle}</p>
        </div>
        <Button onClick={openCreate}>{t.recurring.createPlan}</Button>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : plans.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.recurring.noPlans}</p></Card>
      ) : (
        <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
          {plans.map((plan) => (
            <Card key={plan.id}>
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold text-gray-900">{plan.name}</h3>
                  <p className="text-sm text-gray-500 mt-1">{plan.customerName || plan.customerPhone}</p>
                </div>
                <span className={`px-2 py-0.5 rounded text-xs font-medium ${statusStyle(plan.status)}`}>{plan.status}</span>
              </div>
              <div className="mt-3 text-2xl font-bold text-gray-900">{formatCurrency(plan.amount)}</div>
              <div className="mt-2 text-sm text-gray-500">
                {plan.interval} · {plan.time || t.recurring.anytime} · {plan.chargeCount}/{plan.maxCharges ?? '∞'} {t.recurring.charges}
              </div>
              {plan.nextRunAt && <p className="text-xs text-gray-400 mt-1">{t.recurring.nextRun}: {new Date(plan.nextRunAt).toLocaleString()}</p>}
              <div className="mt-4 flex flex-wrap gap-2">
                <Button size="sm" variant="ghost" onClick={() => openEdit(plan)}>{t.common.edit}</Button>
                <Button size="sm" variant="secondary" onClick={() => toggleStatus(plan)}>{plan.status === 'ACTIVE' ? t.recurring.pause : t.recurring.resume}</Button>
                <Button size="sm" variant="secondary" onClick={() => runNow(plan)}>{t.recurring.runNow}</Button>
                <Button size="sm" variant="danger" onClick={() => removePlan(plan)}>{t.common.delete}</Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={showForm} onClose={() => setShowForm(false)} title={editing ? t.recurring.editPlan : t.recurring.createPlan} large>
        <div className="space-y-4">
          <Input label={t.recurring.planName} value={form.name} onChange={(e) => set('name', e.target.value)} />
          <Input label={t.recurring.description} value={form.description} onChange={(e) => set('description', e.target.value)} />
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.recurring.amount} type="number" value={form.amount} onChange={(e) => set('amount', e.target.value)} />
            <Input label={t.recurring.maxCharges} type="number" value={form.maxCharges} onChange={(e) => set('maxCharges', e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.recurring.customerPhone} value={form.customerPhone} onChange={(e) => set('customerPhone', e.target.value)} />
            <Input label={t.recurring.customerName} value={form.customerName} onChange={(e) => set('customerName', e.target.value)} />
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t.recurring.interval}</label>
              <select value={form.interval} onChange={(e) => set('interval', e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                <option value="WEEKLY">WEEKLY</option>
                <option value="MONTHLY">MONTHLY</option>
              </select>
            </div>
            <Input label={t.recurring.dayOfMonth} type="number" value={form.dayOfMonth} onChange={(e) => set('dayOfMonth', e.target.value)} />
            <Input label={t.recurring.time} type="time" value={form.time} onChange={(e) => set('time', e.target.value)} />
          </div>
          <div className="flex space-x-3">
            <Button onClick={handleSave} loading={submitting} disabled={!form.name || !form.amount} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
