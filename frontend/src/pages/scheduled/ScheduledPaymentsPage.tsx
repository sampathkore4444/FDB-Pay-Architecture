import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { scheduledPaymentApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatDate, cn } from '../../utils';

interface ScheduledPayment {
  id: string;
  recipient: string;
  amount: number;
  frequency: string;
  status: string;
  startDate: string;
  nextExecution: string;
  description?: string;
  createdAt: string;
}

const frequencies = ['daily', 'weekly', 'biweekly', 'monthly'] as const;

export function ScheduledPaymentsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [payments, setPayments] = useState<ScheduledPayment[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [recipient, setRecipient] = useState('');
  const [amount, setAmount] = useState<number>(0);
  const [frequency, setFrequency] = useState('monthly');
  const [startDate, setStartDate] = useState('');
  const [description, setDescription] = useState('');

  const loadPayments = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await scheduledPaymentApi.getMySchedules(user.id);
      setPayments(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPayments();
  }, [user]);

  const handleCreate = async () => {
    if (!user || !recipient || !amount || !startDate) return;
    setSubmitting(true);
    try {
      await scheduledPaymentApi.create(user.id, {
        recipient,
        amount,
        frequency,
        startDate,
        description,
      });
      setShowCreate(false);
      setRecipient('');
      setAmount(0);
      setFrequency('monthly');
      setStartDate('');
      setDescription('');
      await loadPayments();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handlePause = async (id: string) => {
    if (!user) return;
    try {
      await scheduledPaymentApi.pause(user.id, id);
      await loadPayments();
    } catch (err) {
      console.error(err);
    }
  };

  const handleResume = async (id: string) => {
    if (!user) return;
    try {
      await scheduledPaymentApi.resume(user.id, id);
      await loadPayments();
    } catch (err) {
      console.error(err);
    }
  };

  const handleCancel = async (id: string) => {
    if (!user) return;
    try {
      await scheduledPaymentApi.cancel(user.id, id);
      await loadPayments();
    } catch (err) {
      console.error(err);
    }
  };

  const freqLabel = (f: string) => {
    const map: Record<string, string> = {
      daily: t.scheduled.frequencies.daily,
      weekly: t.scheduled.frequencies.weekly,
      biweekly: t.scheduled.frequencies.biweekly,
      monthly: t.scheduled.frequencies.monthly,
    };
    return map[f] || f;
  };

  const statusColor = (s: string) => {
    const m: Record<string, string> = {
      ACTIVE: 'bg-green-100 text-green-800',
      PAUSED: 'bg-yellow-100 text-yellow-800',
      CANCELLED: 'bg-red-100 text-red-800',
    };
    return m[s] || 'bg-gray-100 text-gray-800';
  };

  if (loading) return <div className="text-center py-8">{t.common.loading}</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t.scheduled.title}</h1>
        <Button onClick={() => setShowCreate(true)}>{t.scheduled.create}</Button>
      </div>

      {payments.length === 0 ? (
        <Card>
          <p className="text-center text-gray-500 py-8">{t.scheduled.noSchedules}</p>
        </Card>
      ) : (
        <div className="space-y-2">
          {payments.map((p) => (
            <div key={p.id} className="bg-white border border-gray-200 rounded-xl p-4">
              <div className="flex items-center justify-between">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="text-sm font-medium text-gray-900">{p.recipient}</span>
                    <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(p.status))}>{p.status}</span>
                  </div>
                  <div className="flex items-center space-x-3 text-xs text-gray-500">
                    <span>{p.amount.toLocaleString()} MMK</span>
                    <span>{freqLabel(p.frequency)}</span>
                    <span>{t.scheduled.nextExecution}: {formatDate(p.nextExecution)}</span>
                  </div>
                  <p className="text-xs text-gray-400">{t.scheduled.startDate}: {formatDate(p.startDate)}</p>
                </div>
                <div className="flex space-x-1">
                  {p.status === 'ACTIVE' && (
                    <Button size="sm" variant="ghost" onClick={() => handlePause(p.id)}>{t.scheduled.pause}</Button>
                  )}
                  {p.status === 'PAUSED' && (
                    <Button size="sm" variant="ghost" onClick={() => handleResume(p.id)}>{t.scheduled.resume}</Button>
                  )}
                  {p.status !== 'CANCELLED' && (
                    <Button size="sm" variant="danger" onClick={() => handleCancel(p.id)}>{t.scheduled.cancel}</Button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title={t.scheduled.create}>
        <div className="space-y-4">
          <Input label={t.scheduled.recipient} value={recipient} onChange={(e) => setRecipient(e.target.value)} placeholder="+959XXXXXXXX" />
          <Input label={t.common.amount} type="number" value={amount || ''} onChange={(e) => setAmount(Number(e.target.value))} placeholder="0" />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.scheduled.frequency}</label>
            <select
              value={frequency}
              onChange={(e) => setFrequency(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
            >
              {frequencies.map((f) => (
                <option key={f} value={f}>{freqLabel(f)}</option>
              ))}
            </select>
          </div>
          <Input label={t.scheduled.startDate} type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          <Input label={t.common.description} value={description} onChange={(e) => setDescription(e.target.value)} />
          <div className="flex space-x-3">
            <Button onClick={handleCreate} loading={submitting} className="flex-1">{t.common.submit}</Button>
            <Button variant="secondary" onClick={() => setShowCreate(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
