import { useEffect, useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
} from 'recharts';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { analyticsApi, walletApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency, formatDate } from '../../utils';
import type { MerchantAnalyticsSummary, MerchantAnalyticsBenchmark } from '../../types';

export function MerchantAnalyticsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [walletId, setWalletId] = useState<string | null>(null);
  const [summary, setSummary] = useState<MerchantAnalyticsSummary | null>(null);
  const [benchmark, setBenchmark] = useState<MerchantAnalyticsBenchmark | null>(null);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [loading, setLoading] = useState(true);

  const load = async () => {
    if (!walletId) return;
    setLoading(true);
    try {
      const [s, b] = await Promise.all([
        analyticsApi.getSummary(walletId, startDate || undefined, endDate || undefined),
        analyticsApi.getBenchmark(walletId, startDate || undefined, endDate || undefined),
      ]);
      setSummary(s);
      setBenchmark(b);
    } catch (err) {
      console.error('Failed to load analytics', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!user) return;
    walletApi
      .getWallet(user.id)
      .then((w) => setWalletId(w.id))
      .catch((err) => console.error('Failed to load wallet', err));
  }, [user]);

  useEffect(() => {
    load();
  }, [walletId]);

  const vsPct = benchmark?.vsAveragePercent ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-gray-900">{t.analytics.title}</h1>
        <div className="flex items-end space-x-3">
          <Input type="date" label={t.reports.fromDate} value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          <Input type="date" label={t.reports.toDate} value={endDate} onChange={(e) => setEndDate(e.target.value)} />
          <Button onClick={load} loading={loading}>{t.common.filter}</Button>
        </div>
      </div>

      {loading && !summary ? (
        <div className="text-center py-12 text-gray-500">{t.common.loading}</div>
      ) : !summary ? (
        <Card>
          <p className="text-center text-gray-500 py-8">{t.common.noData}</p>
        </Card>
      ) : (
        <>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <Card title={t.analytics.totalSales}>
              <p className="text-2xl font-bold text-green-600">{formatCurrency(summary.totalSales)}</p>
            </Card>
            <Card title={t.analytics.transactionCount}>
              <p className="text-2xl font-bold text-gray-900">{summary.saleCount}</p>
            </Card>
            <Card title={t.analytics.avgTransactionValue}>
              <p className="text-2xl font-bold text-blue-600">{formatCurrency(summary.avgTransactionValue)}</p>
            </Card>
            <Card title={t.analytics.refunds}>
              <p className="text-2xl font-bold text-red-600">{formatCurrency(summary.refundAmount)}</p>
              <p className="text-xs text-gray-400 mt-1">{summary.refundCount} {t.analytics.transactions}</p>
            </Card>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card title={t.analytics.dailySales} className="lg:col-span-2">
              {summary.dailySeries.length === 0 ? (
                <p className="text-center text-gray-500 py-8">{t.common.noData}</p>
              ) : (
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={summary.dailySeries}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                    <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                    <YAxis tick={{ fontSize: 11 }} tickFormatter={(v: number) => (v >= 1000 ? `${v / 1000}k` : String(v))} />
                    <Tooltip formatter={(value) => formatCurrency(Number(value))} labelFormatter={(l) => String(l)} />
                    <Bar dataKey="amount" name={t.analytics.sales} fill="#16a34a" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Card>

            <Card title={t.analytics.paymentMethods}>
              {summary.paymentMethods.length === 0 ? (
                <p className="text-center text-gray-500 py-8">{t.common.noData}</p>
              ) : (
                <div className="space-y-3">
                  {summary.paymentMethods.map((m) => (
                    <div key={m.method} className="flex justify-between items-center text-sm">
                      <span className="text-gray-600">{m.method}</span>
                      <span className="font-medium">{formatCurrency(m.amount)}</span>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card title={t.analytics.benchmark}>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-gray-600">{t.analytics.you}</span>
                  <span className="font-bold text-green-600">{formatCurrency(benchmark?.merchantTotalSales ?? 0)}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-600">{t.analytics.platformAverage}</span>
                  <span className="font-bold">{formatCurrency(benchmark?.platformAvgTransactionValue ?? 0)}</span>
                </div>
                <div className="pt-2 border-t border-gray-100">
                  <p className="text-sm text-gray-600">{t.analytics.vsAverage}</p>
                  <p className={`text-2xl font-bold ${vsPct >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {vsPct >= 0 ? '+' : ''}{vsPct.toFixed(1)}%
                  </p>
                </div>
              </div>
            </Card>

            <Card title={t.analytics.topCustomers} className="lg:col-span-2">
              {summary.topCustomers.length === 0 ? (
                <p className="text-center text-gray-500 py-8">{t.common.noData}</p>
              ) : (
                <div className="space-y-3">
                  {summary.topCustomers.map((c, i) => (
                    <div key={c.counterpartyWalletId} className="flex items-center justify-between text-sm">
                      <div className="flex items-center space-x-3">
                        <span className="w-6 h-6 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center text-xs font-bold">
                          {i + 1}
                        </span>
                        <span className="text-gray-700">{c.counterpartyWalletId.slice(0, 8)}…</span>
                        <span className="text-xs text-gray-400">{c.count} {t.analytics.transactions}</span>
                      </div>
                      <span className="font-medium">{formatCurrency(c.amount)}</span>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>
        </>
      )}

      {summary && summary.dailySeries.length > 0 && (
        <p className="text-xs text-gray-400">{t.analytics.lastUpdated}: {formatDate(new Date().toISOString())}</p>
      )}
    </div>
  );
}
