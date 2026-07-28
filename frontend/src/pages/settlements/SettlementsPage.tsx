import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { settlementApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency, formatDate, cn } from '../../utils';

interface SettlementBatch {
  id: string;
  status: string;
  totalAmount: number;
  totalFees: number;
  merchantCount: number;
  reconciliationStatus: string;
  createdAt: string;
}

export function SettlementsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [batches, setBatches] = useState<SettlementBatch[]>([]);
  const [loading, setLoading] = useState(true);
  const [triggering, setTriggering] = useState(false);
  const [selectedBatch, setSelectedBatch] = useState<SettlementBatch | null>(null);

  const loadBatches = async () => {
    setLoading(true);
    try {
      const data = await settlementApi.getBatchSummary();
      setBatches(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBatches();
  }, []);

  const handleTrigger = async () => {
    if (!user) return;
    setTriggering(true);
    try {
      await settlementApi.trigger(user.id);
      await loadBatches();
    } catch (err) {
      console.error(err);
    } finally {
      setTriggering(false);
    }
  };

  const totalSettled = batches.reduce((s, b) => s + b.totalAmount, 0);
  const totalFees = batches.reduce((s, b) => s + b.totalFees, 0);
  const merchantCount = batches.reduce((s, b) => s + b.merchantCount, 0);

  const statusColor = (s: string) => {
    const m: Record<string, string> = {
      SETTLED: 'bg-green-100 text-green-800',
      PENDING: 'bg-yellow-100 text-yellow-800',
      PROCESSING: 'bg-blue-100 text-blue-800',
      FAILED: 'bg-red-100 text-red-800',
    };
    return m[s] || 'bg-gray-100 text-gray-800';
  };

  const reconcileColor = (s: string) => {
    const m: Record<string, string> = {
      MATCHED: 'bg-green-100 text-green-800',
      UNMATCHED: 'bg-red-100 text-red-800',
      PENDING: 'bg-yellow-100 text-yellow-800',
    };
    return m[s] || 'bg-gray-100 text-gray-800';
  };

  if (loading) return <div className="text-center py-8">{t.common.loading}</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t.settlement.title}</h1>
        <Button onClick={handleTrigger} loading={triggering}>{t.settlement.triggerSettlement}</Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <p className="text-sm text-gray-500">{t.settlement.totalSettled}</p>
          <p className="text-2xl font-bold text-green-600">{formatCurrency(totalSettled)}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">{t.settlement.totalFees}</p>
          <p className="text-2xl font-bold text-blue-600">{formatCurrency(totalFees)}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">{t.settlement.merchantCount}</p>
          <p className="text-2xl font-bold text-gray-900">{merchantCount}</p>
        </Card>
      </div>

      <Card title={t.settlement.history}>
        {batches.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.settlement.noSettlements}</p>
        ) : (
          <div className="space-y-2">
            {batches.map((batch) => (
              <div
                key={batch.id}
                className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
                onClick={() => setSelectedBatch(batch)}
              >
                <div className="space-y-1">
                  <p className="text-sm font-medium text-gray-900">{t.settlement.batch}: {batch.id.slice(0, 8)}</p>
                  <p className="text-xs text-gray-500">{formatDate(batch.createdAt)}</p>
                </div>
                <div className="flex items-center space-x-3">
                  <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(batch.status))}>{batch.status}</span>
                  <span className={cn('text-xs px-2 py-0.5 rounded-full', reconcileColor(batch.reconciliationStatus))}>
                    {batch.reconciliationStatus}
                  </span>
                  <span className="text-sm font-semibold text-gray-900">{formatCurrency(batch.totalAmount)}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Modal open={!!selectedBatch} onClose={() => setSelectedBatch(null)} title={`${t.settlement.batch} - ${t.common.details}`}>
        {selectedBatch && (
          <div className="space-y-4 text-sm">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-gray-500">{t.settlement.settlementDate}</p>
                <p className="font-medium">{formatDate(selectedBatch.createdAt)}</p>
              </div>
              <div>
                <p className="text-gray-500">{t.common.status}</p>
                <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(selectedBatch.status))}>{selectedBatch.status}</span>
              </div>
              <div>
                <p className="text-gray-500">{t.settlement.totalSettled}</p>
                <p className="font-medium text-green-600">{formatCurrency(selectedBatch.totalAmount)}</p>
              </div>
              <div>
                <p className="text-gray-500">{t.settlement.totalFees}</p>
                <p className="font-medium text-blue-600">{formatCurrency(selectedBatch.totalFees)}</p>
              </div>
              <div>
                <p className="text-gray-500">{t.settlement.merchantCount}</p>
                <p className="font-medium">{selectedBatch.merchantCount}</p>
              </div>
              <div>
                <p className="text-gray-500">{t.settlement.reconciliationStatus}</p>
                <span className={cn('text-xs px-2 py-0.5 rounded-full', reconcileColor(selectedBatch.reconciliationStatus))}>
                  {selectedBatch.reconciliationStatus}
                </span>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
