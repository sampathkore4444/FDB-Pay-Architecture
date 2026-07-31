import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { merchantApi, settlementApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency, formatDate, cn } from '../../utils';

interface MerchantSettlement {
  id: string;
  status: string;
  totalAmount: number;
  totalFees: number;
  transactionCount: number;
  settlementRef?: string;
  createdAt: string;
}

export function SettlementsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [settlements, setSettlements] = useState<MerchantSettlement[]>([]);
  const [loading, setLoading] = useState(true);
  const [triggering, setTriggering] = useState(false);
  const [selectedSettlement, setSelectedSettlement] = useState<MerchantSettlement | null>(null);
  const [merchantId, setMerchantId] = useState<string | null>(null);

  const loadSettlements = async () => {
    if (!user || !merchantId) return;
    setLoading(true);
    try {
      const data = await settlementApi.getMerchantSettlements(merchantId);
      setSettlements(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!user) return;
    merchantApi
      .getProfile(user.id)
      .then((profile) => {
        setMerchantId(profile.id);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, [user]);

  useEffect(() => {
    if (merchantId) {
      loadSettlements();
    }
  }, [merchantId]);

  const handleTrigger = async () => {
    if (!merchantId) return;
    setTriggering(true);
    try {
      await settlementApi.trigger(merchantId);
      await loadSettlements();
    } catch (err) {
      console.error(err);
    } finally {
      setTriggering(false);
    }
  };

  const totalSettled = settlements.reduce((s, b) => s + b.totalAmount, 0);
  const totalFees = settlements.reduce((s, b) => s + b.totalFees, 0);

  const statusColor = (s: string) => {
    const m: Record<string, string> = {
      SETTLED: 'bg-green-100 text-green-800',
      COMPLETED: 'bg-green-100 text-green-800',
      PENDING: 'bg-yellow-100 text-yellow-800',
      PROCESSING: 'bg-blue-100 text-blue-800',
      FAILED: 'bg-red-100 text-red-800',
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
          <p className="text-2xl font-bold text-gray-900">{settlements.length}</p>
        </Card>
      </div>

      <Card title={t.settlement.history}>
        {settlements.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.settlement.noSettlements}</p>
        ) : (
          <div className="space-y-2">
            {settlements.map((settlement) => (
              <div
                key={settlement.id}
                className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
                onClick={() => setSelectedSettlement(settlement)}
              >
                <div className="space-y-1">
                  <p className="text-sm font-medium text-gray-900">{t.settlement.batch}: {settlement.id.slice(0, 8)}</p>
                  <p className="text-xs text-gray-500">{formatDate(settlement.createdAt)}</p>
                </div>
                <div className="flex items-center space-x-3">
                  <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(settlement.status))}>{settlement.status}</span>
                  <span className="text-sm font-semibold text-gray-900">{formatCurrency(settlement.totalAmount)}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Modal open={!!selectedSettlement} onClose={() => setSelectedSettlement(null)} title={`${t.settlement.batch} - ${t.common.details}`}>
        {selectedSettlement && (
          <div className="space-y-4 text-sm">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-gray-500">{t.settlement.settlementDate}</p>
                <p className="font-medium">{formatDate(selectedSettlement.createdAt)}</p>
              </div>
              <div>
                <p className="text-gray-500">{t.common.status}</p>
                <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(selectedSettlement.status))}>{selectedSettlement.status}</span>
              </div>
              <div>
                <p className="text-gray-500">{t.settlement.totalSettled}</p>
                <p className="font-medium text-green-600">{formatCurrency(selectedSettlement.totalAmount)}</p>
              </div>
              <div>
                <p className="text-gray-500">{t.settlement.totalFees}</p>
                <p className="font-medium text-blue-600">{formatCurrency(selectedSettlement.totalFees)}</p>
              </div>
              {selectedSettlement.settlementRef && (
                <div className="col-span-2">
                  <p className="text-gray-500">Ref</p>
                  <p className="font-mono text-xs bg-gray-50 px-2 py-1 rounded">{selectedSettlement.settlementRef}</p>
                </div>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
