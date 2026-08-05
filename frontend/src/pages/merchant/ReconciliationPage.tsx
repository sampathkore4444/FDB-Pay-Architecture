import { useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { merchantApi, reconciliationApi, walletApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { formatCurrency } from '../../utils';
import type { Merchant, ReconciliationRow } from '../../types';

const STATUS_STYLES: Record<string, string> = {
  MATCHED: 'bg-green-100 text-green-700',
  UNMATCHED: 'bg-yellow-100 text-yellow-700',
  NO_ACTIVITY: 'bg-gray-100 text-gray-600',
};

export function ReconciliationPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [rows, setRows] = useState<ReconciliationRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [from, setFrom] = useState(() => new Date(Date.now() - 30 * 86400000).toISOString().slice(0, 10));
  const [to, setTo] = useState(() => new Date().toISOString().slice(0, 10));

  useEffect(() => {
    if (!user) return;
    merchantApi
      .getProfile(user.id)
      .then(setMerchant)
      .catch(() => toast.error(t.common.loadFailed));
  }, [user]);

  const load = async () => {
    if (!user || !merchant) return;
    setLoading(true);
    try {
      const wallet = await walletApi.getWallet(merchant.userId);
      setRows(await reconciliationApi.get({ walletId: wallet.id, merchantId: merchant.id, from, to }));
    } catch (err) {
      console.error('Failed to load reconciliation', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (merchant) load();
  }, [merchant, from, to]);

  const handleCsv = async () => {
    if (!user || !merchant) return;
    try {
      const wallet = await walletApi.getWallet(merchant.userId);
      const csv = await reconciliationApi.downloadCsv({ walletId: wallet.id, merchantId: merchant.id, from, to });
      const blob = new Blob([csv], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `reconciliation-${from}-${to}.csv`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to download CSV', err);
      toast.error(t.common.loadFailed);
    }
  };

  const totals = useMemo(
    () =>
      rows.reduce(
        (acc, r) => ({
          gross: acc.gross + r.grossSales,
          fees: acc.fees + r.fees,
          net: acc.net + r.netSales,
          refunds: acc.refunds + r.refundAmount,
        }),
        { gross: 0, fees: 0, net: 0, refunds: 0 }
      ),
    [rows]
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.reconciliation.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.reconciliation.subtitle}</p>
        </div>
        <div className="flex items-end space-x-3">
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">{t.common.from}</label>
            <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="px-3 py-2 border border-gray-300 rounded-lg text-sm" />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">{t.common.to}</label>
            <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="px-3 py-2 border border-gray-300 rounded-lg text-sm" />
          </div>
          <Button variant="secondary" onClick={handleCsv}>{t.reconciliation.downloadCsv}</Button>
        </div>
      </div>

      <div className="grid md:grid-cols-4 gap-4">
        <Card><p className="text-sm text-gray-500">{t.reconciliation.grossSales}</p><p className="text-2xl font-bold text-gray-900 mt-1">{formatCurrency(totals.gross)}</p></Card>
        <Card><p className="text-sm text-gray-500">{t.reconciliation.fees}</p><p className="text-2xl font-bold text-gray-900 mt-1">{formatCurrency(totals.fees)}</p></Card>
        <Card><p className="text-sm text-gray-500">{t.reconciliation.refunds}</p><p className="text-2xl font-bold text-gray-900 mt-1">{formatCurrency(totals.refunds)}</p></Card>
        <Card><p className="text-sm text-gray-500">{t.reconciliation.netSales}</p><p className="text-2xl font-bold text-green-600 mt-1">{formatCurrency(totals.net)}</p></Card>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : rows.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.noData}</p></Card>
      ) : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                  <th className="pb-2 pr-4">{t.common.date}</th>
                  <th className="pb-2 pr-4">{t.reconciliation.grossSales}</th>
                  <th className="pb-2 pr-4">{t.reconciliation.saleCount}</th>
                  <th className="pb-2 pr-4">{t.reconciliation.refunds}</th>
                  <th className="pb-2 pr-4">{t.reconciliation.fees}</th>
                  <th className="pb-2 pr-4">{t.reconciliation.netSales}</th>
                  <th className="pb-2">{t.common.status}</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.date} className="border-b border-gray-100">
                    <td className="py-2 pr-4 text-gray-900">{r.date}</td>
                    <td className="py-2 pr-4 text-gray-900">{formatCurrency(r.grossSales)}</td>
                    <td className="py-2 pr-4 text-gray-600">{r.saleCount}</td>
                    <td className="py-2 pr-4 text-gray-900">{formatCurrency(r.refundAmount)}</td>
                    <td className="py-2 pr-4 text-gray-900">{formatCurrency(r.fees)}</td>
                    <td className="py-2 pr-4 text-green-700 font-medium">{formatCurrency(r.netSales)}</td>
                    <td className="py-2"><span className={`px-2 py-0.5 rounded text-xs font-medium ${STATUS_STYLES[r.status] || 'bg-gray-100 text-gray-600'}`}>{r.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
}
