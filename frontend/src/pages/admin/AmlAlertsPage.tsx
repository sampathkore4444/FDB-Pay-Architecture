import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { adminApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { toast } from 'sonner';

interface AmlAlert {
  id: string;
  userId: string;
  userName: string;
  type: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  amount: number;
  status: 'OPEN' | 'DISMISSED' | 'ESCALATED' | 'BLOCKED';
  description: string;
  createdAt: string;
}

export function AmlAlertsPage() {
  const { t } = useTranslation();
  const [alerts, setAlerts] = useState<AmlAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [severityFilter, setSeverityFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [selectedAlert, setSelectedAlert] = useState<AmlAlert | null>(null);
  const [actionModalOpen, setActionModalOpen] = useState(false);
  const [actionType, setActionType] = useState('');
  const [actionReason, setActionReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [stats] = useState({ total: 0, highRisk: 0, resolvedToday: 0 });

  const fetchAlerts = async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = { page: String(page) };
      if (severityFilter) params.severity = severityFilter;
      if (statusFilter) params.status = statusFilter;
      const data = await adminApi.getAmlAlerts(params as { severity?: string; status?: string; page?: number });
      const list = (data as unknown as { alerts: AmlAlert[] })?.alerts || [];
      setAlerts(list);
      setTotalPages(Math.max(1, Math.ceil(list.length / 20)));
    } catch {
      toast.error(t.common.error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
  }, [page, severityFilter, statusFilter]);

  const openAction = (alert: AmlAlert, action: string) => {
    setSelectedAlert(alert);
    setActionType(action);
    setActionReason('');
    setActionModalOpen(true);
  };

  const submitAction = async () => {
    if (!selectedAlert) return;
    setSubmitting(true);
    try {
      await adminApi.actionAmlAlert(selectedAlert.id, {
        action: actionType,
        reason: actionReason || undefined,
      });
      toast.success(t.common.success);
      setActionModalOpen(false);
      fetchAlerts();
    } catch {
      toast.error(t.common.error);
    } finally {
      setSubmitting(false);
    }
  };

  const severityColor = (s: string) => {
    if (s === 'CRITICAL') return 'bg-red-100 text-red-800';
    if (s === 'HIGH') return 'bg-orange-100 text-orange-800';
    if (s === 'MEDIUM') return 'bg-yellow-100 text-yellow-800';
    return 'bg-blue-100 text-blue-800';
  };

  const statusColor = (s: string) => {
    if (s === 'OPEN') return 'bg-yellow-100 text-yellow-800';
    if (s === 'ESCALATED') return 'bg-orange-100 text-orange-800';
    if (s === 'BLOCKED') return 'bg-red-100 text-red-800';
    return 'bg-green-100 text-green-800';
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.admin.amlAlerts}</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <p className="text-sm text-gray-500">Total Alerts</p>
          <p className="text-2xl font-bold text-gray-900">{stats.total}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">High Risk</p>
          <p className="text-2xl font-bold text-red-600">{stats.highRisk}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">Resolved Today</p>
          <p className="text-2xl font-bold text-green-600">{stats.resolvedToday}</p>
        </Card>
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-4 mb-4">
          <div className="flex gap-2">
            {['', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((s) => (
              <Button
                key={s}
                variant={severityFilter === s ? 'primary' : 'secondary'}
                size="sm"
                onClick={() => { setSeverityFilter(s); setPage(0); }}
              >
                {s || t.common.all}
              </Button>
            ))}
          </div>
          <div className="flex gap-2">
            {['', 'OPEN', 'DISMISSED', 'ESCALATED', 'BLOCKED'].map((s) => (
              <Button
                key={s}
                variant={statusFilter === s ? 'primary' : 'ghost'}
                size="sm"
                onClick={() => { setStatusFilter(s); setPage(0); }}
              >
                {s || t.common.status}
              </Button>
            ))}
          </div>
        </div>

        {loading ? (
          <p className="text-center text-gray-500 py-8">{t.common.loading}</p>
        ) : alerts.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.admin.noAlerts}</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.name}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.type}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">Severity</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.amount}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.status}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.date}</th>
                  <th className="text-right py-3 px-2 font-medium text-gray-500">{t.common.actions}</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map((alert) => (
                  <tr key={alert.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-3 px-2 text-gray-900">{alert.userName}</td>
                    <td className="py-3 px-2 text-gray-600">{alert.type}</td>
                    <td className="py-3 px-2">
                      <span className={`inline-flex px-2 py-1 rounded-full text-xs font-medium ${severityColor(alert.severity)}`}>
                        {alert.severity}
                      </span>
                    </td>
                    <td className="py-3 px-2 text-gray-900">MMK {alert.amount.toLocaleString()}</td>
                    <td className="py-3 px-2">
                      <span className={`inline-flex px-2 py-1 rounded-full text-xs font-medium ${statusColor(alert.status)}`}>
                        {alert.status}
                      </span>
                    </td>
                    <td className="py-3 px-2 text-gray-600">{new Date(alert.createdAt).toLocaleDateString()}</td>
                    <td className="py-3 px-2 text-right">
                      {alert.status === 'OPEN' && (
                        <div className="flex gap-2 justify-end">
                          <Button variant="ghost" size="sm" onClick={() => openAction(alert, 'DISMISS')}>
                            Dismiss
                          </Button>
                          <Button variant="secondary" size="sm" onClick={() => openAction(alert, 'ESCALATE')}>
                            Escalate
                          </Button>
                          <Button variant="danger" size="sm" onClick={() => openAction(alert, 'BLOCK')}>
                            Block User
                          </Button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="flex justify-between items-center mt-4">
          <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
            {t.common.back}
          </Button>
          <span className="text-sm text-gray-500">Page {page + 1} of {totalPages}</span>
          <Button variant="secondary" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>
            {t.common.next}
          </Button>
        </div>
      </Card>

      <Modal open={actionModalOpen} onClose={() => setActionModalOpen(false)} title={`Alert Action: ${actionType}`}>
        {selectedAlert && (
          <div className="space-y-4">
            <div>
              <p className="text-sm text-gray-500">{t.common.name}</p>
              <p className="font-medium">{selectedAlert.userName}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">{t.common.type}</p>
              <p className="font-medium">{selectedAlert.type}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">{t.common.amount}</p>
              <p className="font-medium">MMK {selectedAlert.amount.toLocaleString()}</p>
            </div>
            <Input
              label="Reason"
              value={actionReason}
              onChange={(e) => setActionReason(e.target.value)}
              placeholder="Enter reason..."
            />
            <div className="flex gap-2 justify-end">
              <Button variant="ghost" onClick={() => setActionModalOpen(false)}>
                {t.common.cancel}
              </Button>
              <Button variant="danger" onClick={submitAction} loading={submitting}>
                {t.common.confirm}
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
