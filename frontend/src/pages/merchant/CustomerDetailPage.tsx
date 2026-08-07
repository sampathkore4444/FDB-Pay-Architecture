import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { customerInsightApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency, formatDate } from '../../utils';
import type { CustomerDetail, CustomerTimelineEntry, CustomerNote, MerchantOrder } from '../../types';

export function CustomerDetailPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const { phone } = useParams<{ phone: string }>();
  const [detail, setDetail] = useState<CustomerDetail | null>(null);
  const [orders, setOrders] = useState<MerchantOrder[]>([]);
  const [timeline, setTimeline] = useState<CustomerTimelineEntry[]>([]);
  const [notes, setNotes] = useState<CustomerNote[]>([]);
  const [loading, setLoading] = useState(true);
  const [noteText, setNoteText] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!user || !phone) return;
    setLoading(true);
    Promise.all([
      customerInsightApi.detail(user.id, phone).then(setDetail),
      customerInsightApi.orders(user.id, phone).then(setOrders),
      customerInsightApi.timeline(user.id, phone).then(setTimeline),
      customerInsightApi.notes(user.id, phone).then(setNotes),
    ])
      .catch((err) => {
        console.error('Failed to load customer detail', err);
        toast.error(t.common.loadFailed);
      })
      .finally(() => setLoading(false));
  }, [user, phone]);

  const addNote = async () => {
    if (!user || !phone || !noteText.trim()) return;
    setSubmitting(true);
    try {
      await customerInsightApi.addNote(user.id, phone, noteText.trim());
      toast.success(t.customerDetail.noteAdded);
      setNoteText('');
      setNotes(await customerInsightApi.notes(user.id, phone));
    } catch (err) {
      console.error('Failed to add note', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const statCards = detail
    ? [
        { label: t.customerDetail.totalSpent, value: formatCurrency(detail.totalSpent) },
        { label: t.customerDetail.orderCount, value: String(detail.orderCount) },
        { label: t.customerDetail.avgOrderValue, value: formatCurrency(detail.avgOrderValue) },
        { label: t.customerDetail.refundedAmount, value: formatCurrency(detail.refundedAmount) },
      ]
    : [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <Link to="/merchant/customers" className="text-sm text-blue-600 hover:underline">{t.customerDetail.back}</Link>
          <h1 className="text-2xl font-bold text-gray-900 mt-1">{t.customerDetail.title}: <span className="font-mono">{phone}</span></h1>
          <p className="text-sm text-gray-500 mt-1">{detail?.name || ''}</p>
        </div>
        {detail && (
          <span className={`px-3 py-1 rounded text-sm font-medium ${detail.churnRisk ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
            {detail.churnRisk ? t.customerDetail.atRisk : t.customerDetail.healthy}
          </span>
        )}
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : !detail ? (
        <Card><p className="text-center text-gray-500 py-10">{t.customers.customerNotFound}</p></Card>
      ) : (
        <>
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {statCards.map((s) => (
              <Card key={s.label}>
                <p className="text-xs uppercase tracking-wide text-gray-400">{s.label}</p>
                <p className="text-xl font-bold text-gray-900 mt-1">{s.value}</p>
              </Card>
            ))}
          </div>

          <Card title={t.customerDetail.monthlySpend}>
            {Object.keys(detail.byMonth).length === 0 ? (
              <p className="text-center text-gray-500 py-4">{t.common.noData}</p>
            ) : (
              <div className="flex items-end space-x-4 h-40">
                {Object.entries(detail.byMonth).map(([month, amount]) => {
                  const max = Math.max(1, ...Object.values(detail.byMonth));
                  return (
                    <div key={month} className="flex-1 flex flex-col items-center justify-end">
                      <span className="text-xs font-semibold text-gray-700">{formatCurrency(amount)}</span>
                      <div className="w-full mt-1 bg-blue-500 rounded-t" style={{ height: `${Math.round((amount / max) * 100)}%` }} />
                      <span className="text-xs text-gray-400 mt-1">{month}</span>
                    </div>
                  );
                })}
              </div>
            )}
          </Card>

          <div className="grid lg:grid-cols-2 gap-4">
            <Card title={t.customerDetail.timeline}>
              {timeline.length === 0 ? (
                <p className="text-center text-gray-500 py-6">{t.customerDetail.noTimeline}</p>
              ) : (
                <div className="space-y-3">
                  {timeline.map((entry, i) => (
                    <div key={i} className="flex items-start space-x-3 border-l-2 border-gray-200 pl-3">
                      <span className="text-xs font-semibold text-gray-400">
                        {entry.at ? formatDate(entry.at) : ''}
                      </span>
                      <div>
                        <p className="text-sm font-medium text-gray-900">{entry.title}</p>
                        {entry.detail && <p className="text-sm text-gray-500">{entry.detail}</p>}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </Card>

            <Card title={t.customerDetail.notesTitle}>
              {notes.length === 0 ? (
                <p className="text-center text-gray-500 py-4">{t.customerDetail.noNotes}</p>
              ) : (
                <div className="space-y-2 mb-4">
                  {notes.map((note) => (
                    <div key={note.id} className="border border-gray-200 rounded-lg p-3">
                      <p className="text-sm text-gray-700">{note.note}</p>
                      <p className="text-xs text-gray-400 mt-1">{note.createdBy || 'owner'} · {note.createdAt ? formatDate(note.createdAt) : ''}</p>
                    </div>
                  ))}
                </div>
              )}
              <div className="flex space-x-2">
                <Input placeholder={t.customerDetail.notePlaceholder} value={noteText} onChange={(e) => setNoteText(e.target.value)} />
                <Button onClick={addNote} loading={submitting}>{t.customerDetail.addNote}</Button>
              </div>
            </Card>
          </div>

          <Card title={t.customerDetail.orders}>
            {orders.length === 0 ? (
              <p className="text-center text-gray-500 py-6">{t.orders.noOrders}</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                      <th className="pb-2 pr-4">{t.orders.orderNumber}</th>
                      <th className="pb-2 pr-4">{t.orders.total}</th>
                      <th className="pb-2 pr-4">{t.orders.status}</th>
                      <th className="pb-2">{t.orders.date}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orders.map((order) => (
                      <tr key={order.id} className="border-b border-gray-100">
                        <td className="py-2 pr-4 font-mono text-xs text-gray-500">{order.id.slice(0, 8)}…</td>
                        <td className="py-2 pr-4 font-semibold text-gray-900">{formatCurrency(order.total)}</td>
                        <td className="py-2 pr-4"><span className="px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600">{order.status.replace(/_/g, ' ')}</span></td>
                        <td className="py-2 text-gray-500">{order.createdAt ? formatDate(order.createdAt) : '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </>
      )}
    </div>
  );
}
