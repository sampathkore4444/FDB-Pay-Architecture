import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, PieChart, Pie, Cell, Legend,
} from 'recharts';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { analyticsApi, walletApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { formatCurrency } from '../../utils';
import type { MerchantAnalyticsBenchmark, MerchantAnalyticsSummary } from '../../types';

const PIE_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#06b6d4'];

export function BusinessDashboardPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [walletId, setWalletId] = useState<string | null>(null);
  const [summary, setSummary] = useState<MerchantAnalyticsSummary | null>(null);
  const [benchmark, setBenchmark] = useState<MerchantAnalyticsBenchmark | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    walletApi
      .getWallet(user.id)
      .then((w) => setWalletId(w.id))
      .catch((err) => console.error('Failed to load wallet', err));
  }, [user]);

  const load = async () => {
    if (!walletId) return;
    setLoading(true);
    try {
      const [s, b] = await Promise.all([analyticsApi.getSummary(walletId), analyticsApi.getBenchmark(walletId)]);
      setSummary(s);
      setBenchmark(b);
    } catch (err) {
      console.error('Failed to load dashboard', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [walletId]);

  const daily = useMemo(() => (summary?.dailySeries || []).map((d) => ({ date: d.date, sales: d.amount, count: d.count })), [summary]);
  const methods = useMemo(() => (summary?.paymentMethods || []).map((m) => ({ name: m.method, value: m.amount })), [summary]);
  const vsPct = benchmark?.vsAveragePercent ?? 0;

  const quickLinks = [
    { to: '/merchant/stores', label: t.stores.title },
    { to: '/merchant/chargebacks', label: t.chargebacks.title },
    { to: '/merchant/financing', label: t.financing.title },
    { to: '/merchant/risk-alerts', label: t.risk.title },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t.dashboard.title}</h1>
        <p className="text-sm text-gray-500 mt-1">{t.dashboard.subtitle}</p>
      </div>

      {loading || !summary ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : (
        <>
          <div className="grid md:grid-cols-3 xl:grid-cols-5 gap-4">
            <Card><p className="text-sm text-gray-500">{t.dashboard.totalSales}</p><p className="text-xl font-bold text-gray-900 mt-1">{formatCurrency(summary.totalSales)}</p></Card>
            <Card><p className="text-sm text-gray-500">{t.dashboard.saleCount}</p><p className="text-xl font-bold text-gray-900 mt-1">{summary.saleCount}</p></Card>
            <Card><p className="text-sm text-gray-500">{t.dashboard.avgTxn}</p><p className="text-xl font-bold text-gray-900 mt-1">{formatCurrency(summary.avgTransactionValue)}</p></Card>
            <Card><p className="text-sm text-gray-500">{t.dashboard.refunds}</p><p className="text-xl font-bold text-gray-900 mt-1">{formatCurrency(summary.refundAmount)}</p></Card>
            <Card><p className="text-sm text-gray-500">{t.dashboard.netSales}</p><p className="text-xl font-bold text-green-600 mt-1">{formatCurrency(summary.netSales)}</p></Card>
          </div>

          <div className="grid lg:grid-cols-3 gap-4">
            <Card className="lg:col-span-2">
              <h3 className="font-semibold text-gray-900 mb-4">{t.dashboard.salesTrend}</h3>
              <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={daily}>
                    <defs>
                      <linearGradient id="salesFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#3b82f6" stopOpacity={0.3} />
                        <stop offset="100%" stopColor="#3b82f6" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
                    <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                    <YAxis tick={{ fontSize: 12 }} />
                    <Tooltip formatter={(v) => formatCurrency(Number(v))} />
                    <Area type="monotone" dataKey="sales" stroke="#3b82f6" fill="url(#salesFill)" strokeWidth={2} />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </Card>

            <Card>
              <h3 className="font-semibold text-gray-900 mb-4">{t.dashboard.paymentMethods}</h3>
              {methods.length === 0 ? (
                <p className="text-sm text-gray-400 py-10 text-center">{t.common.noData}</p>
              ) : (
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={methods} dataKey="value" nameKey="name" innerRadius={50} outerRadius={80}>
                        {methods.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                      </Pie>
                      <Tooltip formatter={(v) => formatCurrency(Number(v))} />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}
            </Card>
          </div>

          <div className="grid md:grid-cols-3 gap-4">
            <Card>
              <h3 className="font-semibold text-gray-900 mb-3">{t.dashboard.benchmark}</h3>
              <p className="text-2xl font-bold text-gray-900">
                {vsPct > 0 ? '+' : ''}{vsPct}%
              </p>
              <p className="text-sm text-gray-500 mt-1">{t.dashboard.vsAverage}</p>
            </Card>
            <Card className="md:col-span-2">
              <h3 className="font-semibold text-gray-900 mb-3">{t.dashboard.quickActions}</h3>
              <div className="grid grid-cols-2 gap-3">
                {quickLinks.map((q) => (
                  <Link key={q.to} to={q.to} className="border border-gray-200 rounded-lg p-3 text-sm font-medium text-blue-600 hover:bg-blue-50">
                    {q.label}
                  </Link>
                ))}
              </div>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}
