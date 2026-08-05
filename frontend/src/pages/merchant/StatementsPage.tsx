import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { merchantApi, merchantOpsApi, walletApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency } from '../../utils';
import type { Merchant, MerchantStatement } from '../../types';

const SETTLEMENT_OPTIONS = ['T0', 'T1', 'T7'];

export function StatementsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [statement, setStatement] = useState<MerchantStatement | null>(null);
  const [loading, setLoading] = useState(true);
  const [from, setFrom] = useState(() => new Date(Date.now() - 30 * 86400000).toISOString().slice(0, 10));
  const [to, setTo] = useState(() => new Date().toISOString().slice(0, 10));
  const [percent, setPercent] = useState('5');
  const [periodDays, setPeriodDays] = useState('7');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!user) return;
    merchantApi
      .getProfile(user.id)
      .then(async (m) => {
        setMerchant(m);
        setPercent(String(m.rollingReservePercent ?? 5));
        setPeriodDays(String(m.rollingReservePeriodDays ?? 7));
      })
      .catch(() => toast.error(t.common.loadFailed));
  }, [user]);

  useEffect(() => {
    if (!merchant || !user) return;
    setLoading(true);
    walletApi
      .getWallet(merchant.userId)
      .then((wallet) => merchantOpsApi.getStatement(wallet.id, { from, to, rollingReservePercent: Number(percent), rollingReservePeriodDays: Number(periodDays) }))
      .then(setStatement)
      .catch((err) => {
        console.error('Failed to load statement', err);
        toast.error(t.common.loadFailed);
      })
      .finally(() => setLoading(false));
  }, [merchant, from, to, percent, periodDays]);

  const handleSettlementType = async (st: string) => {
    if (!merchant) return;
    setSaving(true);
    try {
      await merchantApi.updateSettlementType(merchant.id, st);
      toast.success(t.statements.settlementSaved);
    } catch (err) {
      console.error('Failed to update settlement type', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSaving(false);
    }
  };

  const handleReserve = async () => {
    if (!merchant) return;
    setSaving(true);
    try {
      await merchantApi.updateReserve(merchant.id, Number(percent), Number(periodDays));
      toast.success(t.statements.reserveSaved);
    } catch (err) {
      console.error('Failed to update reserve', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSaving(false);
    }
  };

  const feeRows = [
    { label: t.statements.transactionFees, value: statement?.feeBreakdown.transactionFees ?? 0 },
    { label: t.statements.cardFees, value: statement?.feeBreakdown.cardFees ?? 0 },
    { label: t.statements.refundFees, value: statement?.feeBreakdown.refundFees ?? 0 },
    { label: t.statements.serviceFees, value: statement?.feeBreakdown.serviceFees ?? 0 },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.statements.title}</h1>

      <Card title={t.statements.period}>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Input type="date" label={t.statements.from} value={from} onChange={(e) => setFrom(e.target.value)} />
          <Input type="date" label={t.statements.to} value={to} onChange={(e) => setTo(e.target.value)} />
          <Input type="number" label={t.statements.reservePercent} value={percent} onChange={(e) => setPercent(e.target.value)} />
          <Input type="number" label={t.statements.reservePeriodDays} value={periodDays} onChange={(e) => setPeriodDays(e.target.value)} />
        </div>
      </Card>

      {loading ? (
        <Card>
          <p className="text-center text-gray-500 py-10">{t.common.loading}</p>
        </Card>
      ) : statement ? (
        <>
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <Stat label={t.statements.totalVolume} value={formatCurrency(statement.totalVolume)} />
            <Stat label={t.statements.netSales} value={formatCurrency(statement.netSales)} accent="text-green-600" />
            <Stat label={t.statements.totalFees} value={formatCurrency(statement.totalFees)} accent="text-red-600" />
            <Stat label={t.statements.refundAmount} value={formatCurrency(statement.refundAmount)} accent="text-orange-600" />
          </div>

          <div className="grid md:grid-cols-2 gap-6">
            <Card title={t.statements.feeBreakdown}>
              <div className="divide-y divide-gray-100">
                {feeRows.map((r) => (
                  <div key={r.label} className="flex items-center justify-between py-2.5">
                    <span className="text-sm text-gray-600">{r.label}</span>
                    <span className="text-sm font-medium text-gray-900">{formatCurrency(r.value)}</span>
                  </div>
                ))}
                <div className="flex items-center justify-between py-2.5">
                  <span className="text-sm font-semibold text-gray-900">{t.statements.totalFees}</span>
                  <span className="text-sm font-semibold text-red-600">{formatCurrency(statement.totalFees)}</span>
                </div>
              </div>
              <div className="mt-4">
                <p className="text-xs uppercase text-gray-400 mb-2">{t.statements.grossByType}</p>
                {statement.grossByType.map((g) => (
                  <div key={g.type} className="flex items-center justify-between text-sm py-1">
                    <span className="text-gray-600">{g.type} ({g.count})</span>
                    <span className="font-medium text-gray-900">{formatCurrency(g.volume)}</span>
                  </div>
                ))}
              </div>
            </Card>

            <Card title={t.statements.rollingReserve}>
              <div className="divide-y divide-gray-100">
                <div className="flex items-center justify-between py-2.5">
                  <span className="text-sm text-gray-600">{t.statements.reserveRate}</span>
                  <span className="text-sm font-medium text-gray-900">{statement.rollingReserve.percent}%</span>
                </div>
                <div className="flex items-center justify-between py-2.5">
                  <span className="text-sm text-gray-600">{t.statements.held}</span>
                  <span className="text-sm font-medium text-gray-900">{formatCurrency(statement.rollingReserve.heldThisPeriod)}</span>
                </div>
                <div className="flex items-center justify-between py-2.5">
                  <span className="text-sm text-gray-600">{t.statements.released}</span>
                  <span className="text-sm font-medium text-gray-900">{formatCurrency(statement.rollingReserve.releasedThisPeriod)}</span>
                </div>
                <div className="flex items-center justify-between py-2.5">
                  <span className="text-sm font-semibold text-gray-900">{t.statements.currentBalance}</span>
                  <span className="text-sm font-semibold text-gray-900">{formatCurrency(statement.rollingReserve.currentBalance)}</span>
                </div>
              </div>
            </Card>
          </div>
        </>
      ) : (
        <Card>
          <p className="text-center text-gray-500 py-10">{t.common.noData}</p>
        </Card>
      )}

      <Card title={t.statements.settings}>
        <div className="flex flex-wrap items-center gap-3">
          <span className="text-sm text-gray-600">{t.statements.settlementCycle}</span>
          {SETTLEMENT_OPTIONS.map((opt) => (
            <button
              key={opt}
              disabled={saving}
              onClick={() => handleSettlementType(opt)}
              className={`px-4 py-2 rounded-lg text-sm font-medium border ${
                merchant?.settlementType === opt
                  ? 'bg-blue-600 text-white border-blue-600'
                  : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400'
              }`}
            >
              {opt}
            </button>
          ))}
        </div>
        <div className="mt-4 flex items-center gap-3 justify-end">
          <Button variant="secondary" onClick={handleReserve} loading={saving}>
            {t.common.save}
          </Button>
        </div>
      </Card>
    </div>
  );
}

function Stat({ label, value, accent = 'text-gray-900' }: { label: string; value: string; accent?: string }) {
  return (
    <Card>
      <p className="text-xs text-gray-400">{label}</p>
      <p className={`text-xl font-bold mt-1 ${accent}`}>{value}</p>
    </Card>
  );
}
