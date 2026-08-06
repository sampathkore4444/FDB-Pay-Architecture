import { useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { analyticsApi, storeApi, walletApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { formatCurrency } from '../../utils';
import type { Store, StorePerformance } from '../../types';

export function StoreComparePage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [stores, setStores] = useState<Store[]>([]);
  const [performance, setPerformance] = useState<StorePerformance[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    setLoading(true);
    Promise.all([
      storeApi.getStores(user.id).catch(() => []),
      walletApi.getWallet(user.id).then((w) => analyticsApi.getStorePerformance(w.id)).catch(() => [] as StorePerformance[]),
    ])
      .then(([s, p]) => {
        setStores(s);
        setPerformance(p);
      })
      .catch((err) => {
        console.error('Failed to load store comparison', err);
        toast.error(t.common.loadFailed);
      })
      .finally(() => setLoading(false));
  }, [user]);

  const rows = useMemo(() => {
    const map = new Map(stores.map((s) => [s.id, s.name]));
    return performance
      .map((p) => ({ ...p, name: map.get(p.storeId) ?? p.storeId.slice(0, 8) }))
      .sort((a, b) => b.amount - a.amount);
  }, [stores, performance]);

  const totals = useMemo(() => {
    const amount = rows.reduce((sum, r) => sum + r.amount, 0);
    const count = rows.reduce((sum, r) => sum + r.count, 0);
    return { amount, count };
  }, [rows]);

  const maxAmount = Math.max(1, ...rows.map((r) => r.amount));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t.storeCompare.title}</h1>
        <p className="text-sm text-gray-500 mt-1">{t.storeCompare.subtitle}</p>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : (
        <>
          <div className="grid md:grid-cols-3 gap-4">
            <Card>
              <p className="text-sm text-gray-500">{t.storeCompare.totalStores}</p>
              <p className="mt-1 text-2xl font-bold text-gray-900">{stores.length}</p>
            </Card>
            <Card>
              <p className="text-sm text-gray-500">{t.storeCompare.totalVolume}</p>
              <p className="mt-1 text-2xl font-bold text-gray-900">{formatCurrency(totals.amount)}</p>
            </Card>
            <Card>
              <p className="text-sm text-gray-500">{t.storeCompare.totalTransactions}</p>
              <p className="mt-1 text-2xl font-bold text-gray-900">{totals.count}</p>
            </Card>
          </div>

          <Card title={t.storeCompare.performance}>
            {rows.length === 0 ? (
              <p className="text-center text-gray-500 py-10">{t.storeCompare.noData}</p>
            ) : (
              <div className="space-y-4">
                {rows.map((row) => (
                  <div key={row.storeId}>
                    <div className="flex items-center justify-between text-sm">
                      <span className="font-medium text-gray-800">{row.name}</span>
                      <span className="text-gray-600">{row.count} {t.storeCompare.txn} · {formatCurrency(row.amount)}</span>
                    </div>
                    <div className="mt-1 h-2 bg-gray-100 rounded-full overflow-hidden">
                      <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${(row.amount / maxAmount) * 100}%` }} />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </>
      )}
    </div>
  );
}
