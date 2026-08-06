import { useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { merchantApi, merchantOpsApi, transferApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency, formatDate } from '../../utils';
import type { Merchant, Transaction } from '../../types';

export function BulkOperationsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [reason, setReason] = useState('');
  const [refundAmount, setRefundAmount] = useState('');
  const [batchText, setBatchText] = useState('');
  const [running, setRunning] = useState<'refund' | 'void' | 'batch' | null>(null);

  useEffect(() => {
    if (!user) return;
    merchantApi
      .getProfile(user.id)
      .then(async (m) => {
        setMerchant(m);
      const data = await transferApi.getHistory(user.id!, 0, 100);
      setTransactions(data ?? []);
      })
      .catch((err) => {
        console.error('Failed to load transactions', err);
        toast.error(t.common.loadFailed);
      })
      .finally(() => setLoading(false));
  }, [user]);

  const refundable = useMemo(() => transactions.filter((tx) => tx.status === 'COMPLETED'), [transactions]);
  const voidable = useMemo(() => transactions.filter((tx) => tx.status === 'PENDING'), [transactions]);

  const toggle = (id: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const toggleAll = () =>
    setSelected((prev) => (prev.size === refundable.length ? new Set<string>() : new Set(refundable.map((tx) => tx.id))));

  const handleRun = async (kind: 'refund' | 'void') => {
    if (!user || !merchant || selected.size === 0) return;
    setRunning(kind);
    try {
      const ids = [...selected];
      const res =
        kind === 'refund'
          ? await merchantOpsApi.bulkRefund(merchant.userId, ids, reason.trim() || undefined, refundAmount ? Number(refundAmount) : undefined)
          : await merchantOpsApi.bulkVoid(merchant.userId, ids);
      const succeeded = res?.successCount ?? 0;
      toast.success(t.bulkOperations.done(succeeded));
      setSelected(new Set());
      setReason('');
      setRefundAmount('');
      const data = await transferApi.getHistory(user.id!, 0, 100);
      setTransactions(data ?? []);
    } catch (err) {
      console.error(`${kind} failed`, err);
      toast.error(t.bulkOperations.failed);
    } finally {
      setRunning(null);
    }
  };

  const parseBatch = () =>
    batchText
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => {
        const parts = line.split(',').map((p) => p.trim());
        return { customerPhone: parts[0] || '', amount: Number(parts[1]), cardLast4: parts[2] || 'MOTO', customerName: parts[3] || undefined };
      })
      .filter((r) => r.customerPhone && !Number.isNaN(r.amount) && r.amount > 0);

  const handleBatch = async () => {
    if (!user || !merchant) return;
    const charges = parseBatch();
    if (charges.length === 0) {
      toast.error(t.bulkOperations.batchRowsInvalid);
      return;
    }
    setRunning('batch');
    try {
      const res = await merchantOpsApi.batchCharge(merchant.userId, charges);
      toast.success(t.bulkOperations.batchDone(res?.successCount ?? charges.length));
      setBatchText('');
    } catch (err) {
      console.error('Batch charge failed', err);
      toast.error(t.bulkOperations.failed);
    } finally {
      setRunning(null);
    }
  };

  const row = (tx: Transaction) => (
    <tr key={tx.id} className={selected.has(tx.id) ? 'bg-blue-50' : 'hover:bg-gray-50'}>
      <td className="px-4 py-3">
        <input type="checkbox" checked={selected.has(tx.id)} onChange={() => toggle(tx.id)} className="h-4 w-4 rounded border-gray-300" />
      </td>
      <td className="px-4 py-3 text-sm text-gray-700">{tx.type}</td>
      <td className="px-4 py-3 text-sm text-gray-500">{tx.id.slice(0, 8)}</td>
      <td className="px-4 py-3 text-sm font-medium text-gray-900">{formatCurrency(tx.amount)}</td>
      <td className="px-4 py-3 text-sm text-gray-500">{formatDate(tx.createdAt)}</td>
      <td className="px-4 py-3">
        <span className={`px-2 py-0.5 rounded text-xs font-medium ${tx.status === 'COMPLETED' ? 'bg-green-100 text-green-700' : tx.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : 'bg-gray-100 text-gray-500'}`}>
          {tx.status}
        </span>
      </td>
    </tr>
  );

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.bulkOperations.title}</h1>
      <p className="text-sm text-gray-500 -mt-4">{t.bulkOperations.subtitle}</p>

      <Card title={t.bulkOperations.selectRefunds}>
        <div className="flex items-center justify-between mb-3">
          <label className="flex items-center space-x-2 text-sm text-gray-600">
            <input type="checkbox" checked={selected.size === refundable.length && refundable.length > 0} onChange={toggleAll} className="h-4 w-4 rounded border-gray-300" />
            <span>{t.bulkOperations.selectAll} ({selected.size})</span>
          </label>
        </div>
        {loading ? (
          <p className="text-center text-gray-500 py-8">{t.common.loading}</p>
        ) : refundable.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.common.noData}</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead className="text-xs uppercase text-gray-400 border-b border-gray-200">
                <tr>
                  <th className="px-4 py-2 w-8" />
                  <th className="px-4 py-2">Type</th>
                  <th className="px-4 py-2">ID</th>
                  <th className="px-4 py-2">Amount</th>
                  <th className="px-4 py-2">Date</th>
                  <th className="px-4 py-2">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">{refundable.map(row)}</tbody>
            </table>
          </div>
        )}
        <div className="mt-4 flex flex-wrap items-end gap-3 justify-end">
          <Input label={t.bulkOperations.reason} value={reason} onChange={(e) => setReason(e.target.value)} placeholder={t.bulkOperations.reasonPlaceholder} />
          <Input label={t.bulkOperations.partialAmount} value={refundAmount} onChange={(e) => setRefundAmount(e.target.value)} placeholder={t.bulkOperations.partialAmountPlaceholder} />
          <Button loading={running === 'refund'} disabled={selected.size === 0} onClick={() => handleRun('refund')}>
            {t.bulkOperations.refund}
          </Button>
          <Button variant="secondary" loading={running === 'void'} disabled={voidable.length === 0} onClick={() => handleRun('void')}>
            {t.bulkOperations.void}
          </Button>
        </div>
      </Card>

      <Card title={t.bulkOperations.batchCharge} subtitle={t.bulkOperations.batchSubtitle}>
        <textarea
          value={batchText}
          onChange={(e) => setBatchText(e.target.value)}
          rows={5}
          placeholder={t.bulkOperations.batchPlaceholder}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <div className="mt-4 flex justify-end">
          <Button loading={running === 'batch'} disabled={!batchText.trim()} onClick={handleBatch}>
            {t.bulkOperations.runBatch}
          </Button>
        </div>
      </Card>
    </div>
  );
}
