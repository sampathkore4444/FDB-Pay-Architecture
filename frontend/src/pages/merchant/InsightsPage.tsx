import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { insightsApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency } from '../../utils';
import type { FeeCalculation, CashFlowForecast, MonitoringStatus, BestSeller, RepeatCustomer } from '../../types';

export function InsightsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [feeAmount, setFeeAmount] = useState('100000');
  const [fee, setFee] = useState<FeeCalculation | null>(null);
  const [cashFlow, setCashFlow] = useState<CashFlowForecast | null>(null);
  const [monitoring, setMonitoring] = useState<MonitoringStatus | null>(null);
  const [bestSellers, setBestSellers] = useState<BestSeller[]>([]);
  const [repeatCustomers, setRepeatCustomers] = useState<RepeatCustomer[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<'all' | 'fees' | 'cashflow' | 'monitoring' | 'sales'>('all');

  useEffect(() => {
    if (!user) return;
    setLoading(true);
    Promise.all([
      insightsApi.feeCalculator(user.id, Number(feeAmount) || 0),
      insightsApi.cashFlow(user.id),
      insightsApi.monitoring(user.id),
      insightsApi.bestSellers(user.id),
      insightsApi.repeatCustomers(user.id),
    ])
      .then(([f, cf, m, b, r]) => {
        setFee(f);
        setCashFlow(cf);
        setMonitoring(m);
        setBestSellers(b);
        setRepeatCustomers(r);
      })
      .catch((err) => {
        console.error('Failed to load insights', err);
        toast.error(t.common.loadFailed);
      })
      .finally(() => setLoading(false));
  }, [user]);

  const recalcFee = async () => {
    if (!user) return;
    try {
      setFee(await insightsApi.feeCalculator(user.id, Number(feeAmount) || 0));
    } catch (err) {
      console.error('Failed to calculate fee', err);
      toast.error(t.common.loadFailed);
    }
  };

  const tabs = [
    { key: 'all', label: t.common.all },
    { key: 'fees', label: t.insights.feeCalculator },
    { key: 'cashflow', label: t.insights.cashFlow },
    { key: 'monitoring', label: t.insights.monitoring },
    { key: 'sales', label: `${t.insights.bestSellers} / ${t.insights.repeatCustomers}` },
  ] as const;

  const cashFlowMax = cashFlow ? Math.max(1, ...cashFlow.months.map((m) => m.revenue ?? 0), ...cashFlow.months.map((m) => m.projection ?? 0)) : 1;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t.insights.title}</h1>
        <p className="text-sm text-gray-500 mt-1">{t.insights.subtitle}</p>
      </div>

      <div className="flex items-center space-x-3">
        {tabs.map((tb) => (
          <button
            key={tb.key}
            onClick={() => setTab(tb.key)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${tab === tb.key ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}
          >
            {tb.label}
          </button>
        ))}
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : (
        <>
          {tab === 'all' || tab === 'fees' ? (
            <Card title={t.insights.feeCalculator}>
              <div className="flex items-end space-x-3 mb-4">
                <div className="flex-1">
                  <Input label={t.insights.amount} type="number" min={0} value={feeAmount} onChange={(e) => setFeeAmount(e.target.value)} />
                </div>
                <Button onClick={recalcFee}>{t.insights.calculate}</Button>
              </div>
              {fee && (
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="border border-gray-200 rounded-lg p-4">
                    <p className="text-xs uppercase tracking-wide text-gray-400">{t.insights.feeSchedule}</p>
                    <p className="text-lg font-bold text-gray-900 mt-1">{fee.feeSchedule}</p>
                  </div>
                  <div className="border border-gray-200 rounded-lg p-4">
                    <p className="text-xs uppercase tracking-wide text-gray-400">{t.insights.feeRate}</p>
                    <p className="text-lg font-bold text-gray-900 mt-1">{fee.feeRate.toFixed(2)}%</p>
                  </div>
                  <div className="border border-gray-200 rounded-lg p-4">
                    <p className="text-xs uppercase tracking-wide text-gray-400">{t.insights.fee}</p>
                    <p className="text-lg font-bold text-red-600 mt-1">{formatCurrency(fee.fee)}</p>
                  </div>
                  <div className="border border-gray-200 rounded-lg p-4">
                    <p className="text-xs uppercase tracking-wide text-gray-400">{t.insights.net}</p>
                    <p className="text-lg font-bold text-green-600 mt-1">{formatCurrency(fee.net)}</p>
                  </div>
                </div>
              )}
            </Card>
          ) : null}

          {tab === 'all' || tab === 'cashflow' ? (
            <Card title={t.insights.cashFlow} subtitle={t.insights.cashFlowSubtitle}>
              {cashFlow && (
                <>
                  <div className="grid grid-cols-3 gap-4 mb-6">
                    <div className="border border-gray-200 rounded-lg p-4">
                      <p className="text-xs uppercase tracking-wide text-gray-400">{t.insights.projectedAnnual}</p>
                      <p className="text-lg font-bold text-gray-900 mt-1">{formatCurrency(cashFlow.projectedAnnual)}</p>
                    </div>
                    <div className="border border-gray-200 rounded-lg p-4">
                      <p className="text-xs uppercase tracking-wide text-gray-400">{t.insights.averageMonthly}</p>
                      <p className="text-lg font-bold text-gray-900 mt-1">{formatCurrency(cashFlow.averageMonthly)}</p>
                    </div>
                    <div className="border border-gray-200 rounded-lg p-4">
                      <p className="text-xs uppercase tracking-wide text-gray-400">{t.insights.growthRate}</p>
                      <p className="text-lg font-bold text-gray-900 mt-1">{cashFlow.growthRatePct}%</p>
                    </div>
                  </div>
                  <div className="flex items-end space-x-1 h-48 overflow-x-auto">
                    {cashFlow.months.map((m, i) => {
                      const value = m.revenue ?? m.projection ?? 0;
                      const isProjection = m.revenue == null;
                      return (
                        <div key={`${m.month}-${i}`} className="flex-1 min-w-[24px] flex flex-col items-center justify-end">
                          <span className="text-[10px] text-gray-500">{formatCurrency(value)}</span>
                          <div
                            className={`w-full mt-1 rounded-t ${isProjection ? 'bg-purple-400' : 'bg-blue-500'}`}
                            style={{ height: `${Math.round((value / cashFlowMax) * 120)}px` }}
                          />
                          <span className="text-[10px] text-gray-400 mt-1 whitespace-nowrap">{m.month}</span>
                        </div>
                      );
                    })}
                  </div>
                  <div className="flex items-center space-x-4 mt-2 text-xs text-gray-500">
                    <span className="flex items-center space-x-1"><span className="w-3 h-3 bg-blue-500 rounded inline-block" /> {t.insights.actual}</span>
                    <span className="flex items-center space-x-1"><span className="w-3 h-3 bg-purple-400 rounded inline-block" /> {t.insights.projected}</span>
                  </div>
                </>
              )}
            </Card>
          ) : null}

          {tab === 'all' || tab === 'monitoring' ? (
            <Card title={t.insights.monitoring} subtitle={t.insights.monitoringSubtitle}>
              {monitoring && (
                <>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                    <div className="border border-gray-200 rounded-lg p-4">
                      <p className="text-xs uppercase tracking-wide text-gray-400">{t.insights.recentTransactions}</p>
                      <p className="text-lg font-bold text-gray-900 mt-1">{monitoring.recentTransactions}</p>
                    </div>
                    <div className="border border-gray-200 rounded-lg p-4">
                      <p className="text-xs uppercase tracking-wide text-gray-400">{t.insights.failedTransactions}</p>
                      <p className="text-lg font-bold text-red-600 mt-1">{monitoring.failedTransactions}</p>
                    </div>
                    <div className="border border-gray-200 rounded-lg p-4">
                      <p className="text-xs uppercase tracking-wide text-gray-400">{t.insights.pendingRefunds}</p>
                      <p className="text-lg font-bold text-yellow-600 mt-1">{monitoring.pendingRefunds}</p>
                    </div>
                    <div className="border border-gray-200 rounded-lg p-4">
                      <p className="text-xs uppercase tracking-wide text-gray-400">{t.insights.anomalyScore}</p>
                      <p className={`text-lg font-bold mt-1 ${monitoring.anomalyScore > 10 ? 'text-red-600' : 'text-gray-900'}`}>{monitoring.anomalyScore.toFixed(1)}%</p>
                    </div>
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-gray-700 mb-2">{t.insights.alerts}</p>
                    {monitoring.alerts.length === 0 ? (
                      <p className="text-sm text-gray-500">{t.insights.noAlerts}</p>
                    ) : (
                      <ul className="space-y-2">
                        {monitoring.alerts.map((alert, i) => (
                          <li key={i} className="text-sm bg-yellow-50 border border-yellow-200 text-yellow-800 rounded-lg px-3 py-2">{alert}</li>
                        ))}
                      </ul>
                    )}
                  </div>
                </>
              )}
            </Card>
          ) : null}

          {tab === 'all' || tab === 'sales' ? (
            <div className="grid lg:grid-cols-2 gap-4">
              <Card title={t.insights.bestSellers}>
                {bestSellers.length === 0 ? (
                  <p className="text-center text-gray-500 py-6">{t.insights.noBestSellers}</p>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                          <th className="pb-2 pr-4">{t.insights.product}</th>
                          <th className="pb-2 pr-4">{t.insights.unitsSold}</th>
                          <th className="pb-2">{t.insights.revenue}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {bestSellers.map((b) => (
                          <tr key={b.productId} className="border-b border-gray-100">
                            <td className="py-2 pr-4 font-medium text-gray-900">{b.productName}</td>
                            <td className="py-2 pr-4 text-gray-600">{b.unitsSold}</td>
                            <td className="py-2 font-semibold text-gray-900">{formatCurrency(b.revenue)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </Card>

              <Card title={t.insights.repeatCustomers}>
                {repeatCustomers.length === 0 ? (
                  <p className="text-center text-gray-500 py-6">{t.insights.noRepeatCustomers}</p>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                          <th className="pb-2 pr-4">{t.insights.customer}</th>
                          <th className="pb-2 pr-4">{t.insights.orderCount}</th>
                          <th className="pb-2 pr-4">{t.insights.totalSpent}</th>
                          <th className="pb-2">{t.insights.repeatRate}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {repeatCustomers.map((c, i) => (
                          <tr key={i} className="border-b border-gray-100">
                            <td className="py-2 pr-4 font-medium text-gray-900">{c.customerPhone}</td>
                            <td className="py-2 pr-4 text-gray-600">{c.orderCount}</td>
                            <td className="py-2 pr-4 text-gray-600">{formatCurrency(c.totalSpent)}</td>
                            <td className="py-2 font-semibold text-gray-900">{(c.repeatRate * 100).toFixed(0)}%</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </Card>
            </div>
          ) : null}
        </>
      )}
    </div>
  );
}
