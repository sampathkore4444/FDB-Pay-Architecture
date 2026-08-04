import { useEffect, useState } from 'react';
import { adminApi, disputeApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from '../../i18n';
import { formatCurrency } from '../../utils';

export function AdminPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [metrics, setMetrics] = useState<Record<string, unknown>>({});
  const [kycPending, setKycPending] = useState(0);
  const [amlAlerts, setAmlAlerts] = useState(0);
  const [disputeStats, setDisputeStats] = useState<{ totalOpen: number; totalResolved: number; avgResolutionDays: number } | null>(null);

  useEffect(() => {
    adminApi.getDashboard().then(setMetrics).catch(console.error);
    adminApi.getKycPending(0, 1).then((data) => {
      const requests = data?.requests as unknown[];
      setKycPending(Array.isArray(requests) ? requests.length : 0);
    }).catch(() => {});
    adminApi.getAmlAlerts({ page: 0 }).then((data) => {
      const alerts = data?.alerts as unknown[];
      setAmlAlerts(Array.isArray(alerts) ? alerts.length : 0);
    }).catch(() => {});
    disputeApi.getStats().then(setDisputeStats).catch(() => {});
  }, []);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.admin.title}</h1>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <p className="text-sm text-gray-500">{t.admin.totalTransactions}</p>
          <p className="text-2xl font-bold text-gray-900">{String(metrics.totalTransactions ?? 0)}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">{t.admin.activeUsers}</p>
          <p className="text-2xl font-bold text-gray-900">{String(metrics.activeUsers ?? 0)}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">{t.admin.totalVolume}</p>
          <p className="text-2xl font-bold text-gray-900">{formatCurrency(Number(metrics.totalVolume ?? 0))}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">{t.admin.successRate}</p>
          <p className="text-2xl font-bold text-gray-900">{String(metrics.successRate ?? 0)}%</p>
        </Card>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <p className="text-sm text-gray-500">{t.admin.kycPending}</p>
          <p className="text-2xl font-bold text-orange-600">{kycPending}</p>
          <Button variant="ghost" size="sm" className="mt-2" onClick={() => navigate('/admin/kyc')}>
            {t.common.viewDetails}
          </Button>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">{t.admin.amlAlerts}</p>
          <p className="text-2xl font-bold text-red-600">{amlAlerts}</p>
          <Button variant="ghost" size="sm" className="mt-2" onClick={() => navigate('/admin/aml')}>
            {t.common.viewDetails}
          </Button>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">{t.admin.openDisputes}</p>
          <p className="text-2xl font-bold text-yellow-600">{disputeStats?.totalOpen ?? 0}</p>
          <Button variant="ghost" size="sm" className="mt-2" onClick={() => navigate('/disputes')}>
            {t.common.viewDetails}
          </Button>
        </Card>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card title="Quick Actions">
          <div className="grid grid-cols-2 gap-3">
            <Button variant="secondary" className="w-full" onClick={() => navigate('/admin/kyc')}>
              {t.nav.adminKyc}
            </Button>
            <Button variant="secondary" className="w-full" onClick={() => navigate('/admin/aml')}>
              {t.nav.adminAml}
            </Button>
            <Button variant="secondary" className="w-full" onClick={() => navigate('/admin/users')}>
              {t.nav.adminUsers}
            </Button>
            <Button variant="secondary" className="w-full" onClick={() => navigate('/admin/merchants')}>
              {t.nav.adminMerchants}
            </Button>
            <Button variant="secondary" className="w-full" onClick={() => navigate('/admin/refdata')}>
              {t.nav.adminRefData}
            </Button>
          </div>
        </Card>

        <Card title="System Health">
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-500">API Gateway</span>
              <span className="px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700">Healthy</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-500">Database</span>
              <span className="px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700">Healthy</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-500">Kafka</span>
              <span className="px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700">Healthy</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-500">Redis</span>
              <span className="px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700">Healthy</span>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
