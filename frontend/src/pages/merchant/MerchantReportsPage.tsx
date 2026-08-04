import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { analyticsApi, walletApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency, formatDate } from '../../utils';
import type { AnalyticsTransactionRow } from '../../types';

const PAGE_SIZE = 20;

export function MerchantReportsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [walletId, setWalletId] = useState<string | null>(null);
  const [rows, setRows] = useState<AnalyticsTransactionRow[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [direction, setDirection] = useState('');
  const [minAmount, setMinAmount] = useState('');
  const [maxAmount, setMaxAmount] = useState('');

  useEffect(() => {
    if (!user) return;
    walletApi
      .getWallet(user.id)
      .then((w) => setWalletId(w.id))
      .catch((err) => console.error('Failed to load wallet', err));
  }, [user]);

  const load = async (p = 0) => {
    if (!walletId) return;
    setLoading(true);
    try {
      const data = await analyticsApi.getTransactions(walletId, {
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        direction: direction || undefined,
        minAmount: minAmount ? Number(minAmount) : undefined,
        maxAmount: maxAmount ? Number(maxAmount) : undefined,
        page: p,
        size: PAGE_SIZE,
      });
      setRows(data?.content ?? []);
      setTotalElements(data?.totalElements ?? 0);
      setPage(p);
    } catch (err) {
      console.error('Failed to load report', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(0);
  }, [walletId]);

  const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));

  const exportCsv = () => {
    const header = ['ID', 'Direction', 'Type', 'Method', 'Amount', 'Fee', 'Description', 'Sender', 'Receiver', 'Date'];
    const body = rows.map((r) => [
      r.id,
      r.direction,
      r.type,
      r.method,
      r.amount,
      r.fee,
      `"${(r.description || '').replace(/"/g, '""')}"`,
      r.senderWalletId,
      r.receiverWalletId,
      r.createdAt,
    ]);
    const csv = [header, ...body].map((row) => row.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `merchant-report-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const totalAmount = rows
    .filter((r) => r.direction === 'SALE')
    .reduce((sum, r) => sum + r.amount, 0);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-gray-900">{t.reports.title}</h1>
        <div className="flex items-center space-x-2">
          <Button variant="secondary" onClick={exportCsv} disabled={rows.length === 0}>
            {t.reports.exportCsv}
          </Button>
          <Button onClick={() => load(0)} loading={loading}>{t.common.filter}</Button>
        </div>
      </div>

      <Card>
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
          <Input type="date" label={t.reports.fromDate} value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          <Input type="date" label={t.reports.toDate} value={endDate} onChange={(e) => setEndDate(e.target.value)} />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.reports.direction}</label>
            <select
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={direction}
              onChange={(e) => setDirection(e.target.value)}
            >
              <option value="">{t.reports.all}</option>
              <option value="SALE">{t.reports.sales}</option>
              <option value="REFUND">{t.reports.refunds}</option>
            </select>
          </div>
          <Input type="number" label={t.reports.minAmount} value={minAmount} onChange={(e) => setMinAmount(e.target.value)} />
          <Input type="number" label={t.reports.maxAmount} value={maxAmount} onChange={(e) => setMaxAmount(e.target.value)} />
        </div>
      </Card>

      <Card>
        {loading ? (
          <div className="text-center py-12 text-gray-500">{t.common.loading}</div>
        ) : rows.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.common.noData}</p>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 text-left text-gray-500">
                    <th className="py-2 pr-4">{t.reports.direction}</th>
                    <th className="py-2 pr-4">{t.reports.type}</th>
                    <th className="py-2 pr-4">{t.reports.method}</th>
                    <th className="py-2 pr-4 text-right">{t.reports.amount}</th>
                    <th className="py-2 pr-4 text-right">{t.reports.fee}</th>
                    <th className="py-2 pr-4">{t.reports.date}</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.id} className="border-b border-gray-100">
                      <td className="py-2 pr-4">
                        <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${
                          r.direction === 'SALE' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                        }`}>
                          {r.direction === 'SALE' ? t.reports.sales : t.reports.refunds}
                        </span>
                      </td>
                      <td className="py-2 pr-4 text-gray-700">{r.type}</td>
                      <td className="py-2 pr-4 text-gray-700">{r.method}</td>
                      <td className="py-2 pr-4 text-right font-medium text-gray-900">{formatCurrency(r.amount)}</td>
                      <td className="py-2 pr-4 text-right text-gray-500">{formatCurrency(r.fee)}</td>
                      <td className="py-2 pr-4 text-gray-500">{formatDate(r.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between mt-4 text-sm text-gray-500">
              <span>
                {t.reports.summary}: <strong className="text-green-600">{formatCurrency(totalAmount)}</strong> ({rows.length} {t.analytics.transactions})
              </span>
              <div className="flex items-center space-x-2">
                <Button variant="secondary" size="sm" onClick={() => load(page - 1)} disabled={page <= 0}>
                  {t.common.previous}
                </Button>
                <span>{page + 1} / {totalPages}</span>
                <Button variant="secondary" size="sm" onClick={() => load(page + 1)} disabled={page + 1 >= totalPages}>
                  {t.common.next}
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>
    </div>
  );
}
