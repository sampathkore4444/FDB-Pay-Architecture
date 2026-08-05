import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { merchantApi, riskApi, walletApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { formatDate } from '../../utils';
import type { Merchant, RiskAlert } from '../../types';

const SEVERITY_STYLES: Record<string, string> = {
  LOW: 'bg-gray-100 text-gray-700',
  MEDIUM: 'bg-yellow-100 text-yellow-700',
  HIGH: 'bg-orange-100 text-orange-700',
  CRITICAL: 'bg-red-100 text-red-700',
};

export function RiskAlertsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [alerts, setAlerts] = useState<RiskAlert[]>([]);
  const [loading, setLoading] = useState(true);

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
      setAlerts(await riskApi.getAlerts(user.id, wallet.id));
    } catch (err) {
      console.error('Failed to load risk alerts', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (merchant) load();
  }, [merchant]);

  const handleAcknowledge = async (alert: RiskAlert) => {
    if (!user) return;
    try {
      await riskApi.acknowledge(user.id, alert.id);
      await load();
    } catch (err) {
      console.error('Failed to acknowledge', err);
      toast.error(t.common.loadFailed);
    }
  };

  const open = alerts.filter((a) => a.status === 'OPEN');

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t.risk.title}</h1>
        <p className="text-sm text-gray-500 mt-1">{t.risk.subtitle}</p>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : open.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.risk.noAlerts}</p></Card>
      ) : (
        <Card>
          <div className="space-y-3">
            {alerts.map((alert) => (
              <div key={alert.id} className="flex flex-wrap items-start justify-between gap-3 border border-gray-200 rounded-lg p-4">
                <div>
                  <div className="flex items-center space-x-3">
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${SEVERITY_STYLES[alert.severity] || 'bg-gray-100 text-gray-700'}`}>{alert.severity}</span>
                    <span className="font-semibold text-gray-900">{alert.title}</span>
                    <span className="text-xs text-gray-400">{formatDate(alert.createdAt)}</span>
                  </div>
                  {alert.message && <p className="text-sm text-gray-600 mt-1">{alert.message}</p>}
                  {alert.alertType && <p className="text-xs text-gray-400 mt-1">{alert.alertType}</p>}
                </div>
                {alert.status === 'OPEN' ? (
                  <Button size="sm" variant="secondary" onClick={() => handleAcknowledge(alert)}>{t.risk.acknowledge}</Button>
                ) : (
                  <span className="text-xs text-gray-400 mt-1">{t.risk.acknowledgedAt} {alert.acknowledgedAt ? formatDate(alert.acknowledgedAt) : ''}</span>
                )}
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
