import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { chargebackApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency, formatDate } from '../../utils';
import type { Chargeback } from '../../types';

const REASONS = ['DUPLICATE_CHARGE', 'GOODS_NOT_RECEIVED', 'GOODS_DEFECTIVE', 'UNAUTHORIZED', 'WRONG_AMOUNT', 'CANCELLED_ORDER', 'OTHER'];

const STATUS_STYLES: Record<string, string> = {
  OPEN: 'bg-yellow-100 text-yellow-700',
  RESPONDED: 'bg-blue-100 text-blue-700',
  WON: 'bg-green-100 text-green-700',
  LOST: 'bg-red-100 text-red-700',
  CLOSED: 'bg-gray-100 text-gray-600',
};

export function ChargebacksPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [items, setItems] = useState<Chargeback[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Chargeback | null>(null);
  const [note, setNote] = useState('');
  const [showOpen, setShowOpen] = useState(false);
  const [openAmount, setOpenAmount] = useState('');
  const [openReason, setOpenReason] = useState('DUPLICATE_CHARGE');
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setItems(await chargebackApi.getByMerchant(user.id));
    } catch (err) {
      console.error('Failed to load chargebacks', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const openDetail = async (cb: Chargeback) => {
    if (!user) return;
    try {
      setSelected(await chargebackApi.getDetail(user.id, cb.id));
    } catch (err) {
      console.error('Failed to load chargeback detail', err);
      toast.error(t.common.loadFailed);
    }
  };

  const handleNote = async () => {
    if (!user || !selected || !note.trim()) return;
    setSubmitting(true);
    try {
      const updated = await chargebackApi.addNote(user.id, selected.id, note.trim(), user.name);
      setSelected(updated);
      setNote('');
      await load();
    } catch (err) {
      console.error('Failed to add note', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRespond = async (status: string) => {
    if (!user || !selected) return;
    setSubmitting(true);
    try {
      const updated = await chargebackApi.respond(user.id, selected.id, status, `${user.name} responded with ${status}`);
      setSelected(updated);
      await load();
    } catch (err) {
      console.error('Failed to respond', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const handleOpen = async () => {
    if (!user || !openAmount) return;
    setSubmitting(true);
    try {
      await chargebackApi.open(user.id, { amount: Number(openAmount), reasonCode: openReason });
      toast.success(t.chargebacks.opened);
      setShowOpen(false);
      setOpenAmount('');
      setOpenReason('DUPLICATE_CHARGE');
      await load();
    } catch (err) {
      console.error('Failed to open chargeback', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const statusBadge = (s: string) => `px-2 py-0.5 rounded text-xs font-medium ${STATUS_STYLES[s] || 'bg-gray-100 text-gray-600'}`;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.chargebacks.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.chargebacks.subtitle}</p>
        </div>
        <Button onClick={() => setShowOpen(true)}>{t.chargebacks.openChargeback}</Button>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : items.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.noData}</p></Card>
      ) : (
        <Card>
          <div className="space-y-3">
            {items.map((cb) => (
              <div key={cb.id} className="flex flex-wrap items-center justify-between gap-3 border border-gray-200 rounded-lg p-4 cursor-pointer hover:bg-gray-50" onClick={() => openDetail(cb)}>
                <div>
                  <div className="flex items-center space-x-3">
                    <span className="font-semibold text-gray-900">{formatCurrency(cb.amount)}</span>
                    <span className={statusBadge(cb.status)}>{cb.status}</span>
                  </div>
                  <p className="text-xs text-gray-500 mt-1">
                    {cb.reasonCode || t.chargebacks.other} · {formatDate(cb.createdAt)}
                    {cb.deadline ? ` · ${t.chargebacks.deadline}: ${formatDate(cb.deadline)}` : ''}
                  </p>
                </div>
                <span className="text-sm text-blue-600">{t.common.viewDetails}</span>
              </div>
            ))}
          </div>
        </Card>
      )}

      <Modal open={showOpen} onClose={() => setShowOpen(false)} title={t.chargebacks.openChargeback}>
        <div className="space-y-4">
          <Input type="number" label={t.chargebacks.amount} value={openAmount} onChange={(e) => setOpenAmount(e.target.value)} />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.chargebacks.reasonCode}</label>
            <select value={openReason} onChange={(e) => setOpenReason(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-lg">
              {REASONS.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
          </div>
          <div className="flex space-x-3">
            <Button onClick={handleOpen} loading={submitting} disabled={!openAmount} className="flex-1">{t.common.submit}</Button>
            <Button variant="secondary" onClick={() => setShowOpen(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!selected} onClose={() => setSelected(null)} title={t.chargebacks.detail} large>
        {selected && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div><p className="text-gray-400">{t.chargebacks.amount}</p><p className="font-semibold text-gray-900">{formatCurrency(selected.amount)}</p></div>
              <div><p className="text-gray-400">{t.common.status}</p><span className={statusBadge(selected.status)}>{selected.status}</span></div>
              <div><p className="text-gray-400">{t.chargebacks.reasonCode}</p><p className="text-gray-900">{selected.reasonCode || 'OTHER'}</p></div>
              <div><p className="text-gray-400">{t.chargebacks.deadline}</p><p className="text-gray-900">{selected.deadline ? formatDate(selected.deadline) : '-'}</p></div>
            </div>
            {selected.customerNotes && (
              <p className="text-sm text-gray-700 bg-gray-50 rounded-lg p-3">{t.chargebacks.customerNotes}: {selected.customerNotes}</p>
            )}

            <div>
              <p className="text-xs uppercase text-gray-400 mb-2">{t.chargebacks.timeline}</p>
              <div className="space-y-2">
                {selected.notes?.map((n) => (
                  <div key={n.id} className="border border-gray-200 rounded-lg p-3">
                    <div className="flex items-center justify-between text-xs text-gray-400">
                      <span>{n.authorType} · {n.authorName || '-'}</span>
                      <span>{formatDate(n.createdAt)}</span>
                    </div>
                    <p className="text-sm text-gray-800 mt-1">{n.message}</p>
                  </div>
                ))}
                {(!selected.notes || selected.notes.length === 0) && <p className="text-sm text-gray-400">{t.common.noData}</p>}
              </div>
            </div>

            <div className="flex space-x-2">
              <Input value={note} onChange={(e) => setNote(e.target.value)} placeholder={t.chargebacks.addNote} />
              <Button variant="secondary" onClick={handleNote} loading={submitting} disabled={!note.trim()}>{t.common.submit}</Button>
            </div>

            <div className="flex flex-wrap gap-2">
              <Button size="sm" onClick={() => handleRespond('RESPONDED')} loading={submitting}>{t.chargebacks.responded}</Button>
              <Button size="sm" variant="secondary" onClick={() => handleRespond('WON')} loading={submitting}>{t.chargebacks.won}</Button>
              <Button size="sm" variant="danger" onClick={() => handleRespond('LOST')} loading={submitting}>{t.chargebacks.lost}</Button>
              <Button size="sm" variant="ghost" onClick={() => handleRespond('CLOSED')} loading={submitting}>{t.chargebacks.closed}</Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
