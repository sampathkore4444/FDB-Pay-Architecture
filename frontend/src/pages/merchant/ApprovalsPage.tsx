import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { approvalApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { formatCurrency, formatDate } from '../../utils';
import type { ApprovalRequest } from '../../types';

export function ApprovalsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [approvals, setApprovals] = useState<ApprovalRequest[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState<string | null>(null);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setApprovals(await approvalApi.list(user.id, statusFilter || undefined));
    } catch (err) {
      console.error('Failed to load approvals', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user, statusFilter]);

  const review = async (approval: ApprovalRequest, action: 'approve' | 'reject') => {
    if (!user) return;
    if (!window.confirm(`${t.approvals.reviewConfirm} ${approval.type} ${formatCurrency(approval.amount)}`)) return;
    setProcessing(approval.id);
    try {
      if (action === 'approve') await approvalApi.approve(user.id, approval.id);
      else await approvalApi.reject(user.id, approval.id);
      toast.success(action === 'approve' ? t.approvals.approved : t.approvals.rejected);
      await load();
    } catch (err) {
      console.error(`Failed to ${action} approval`, err);
      toast.error(t.common.loadFailed);
    } finally {
      setProcessing(null);
    }
  };

  const statusBadge = (status: string) => {
    const map: Record<string, string> = {
      PENDING: 'bg-yellow-100 text-yellow-700',
      APPROVED: 'bg-green-100 text-green-700',
      REJECTED: 'bg-red-100 text-red-700',
    };
    return <span className={`px-2 py-0.5 rounded text-xs font-medium ${map[status] || 'bg-gray-100 text-gray-600'}`}>{status}</span>;
  };

  const typeLabel = (type: string) =>
    type === 'REFUND' ? t.approvals.refund : type === 'PAYOUT' ? t.approvals.payout : type;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t.approvals.title}</h1>
        <p className="text-sm text-gray-500 mt-1">{t.approvals.subtitle}</p>
      </div>

      <div className="flex items-center space-x-3">
        {['', 'PENDING', 'APPROVED', 'REJECTED'].map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${statusFilter === s ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}
          >
            {s === '' ? t.common.all : s}
          </button>
        ))}
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : approvals.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.approvals.noApprovals}</p></Card>
      ) : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                  <th className="pb-2 pr-4">{t.approvals.type}</th>
                  <th className="pb-2 pr-4">{t.approvals.amount}</th>
                  <th className="pb-2 pr-4">{t.approvals.initiator}</th>
                  <th className="pb-2 pr-4">{t.approvals.status}</th>
                  <th className="pb-2 pr-4">{t.approvals.date}</th>
                  <th className="pb-2">{t.common.actions}</th>
                </tr>
              </thead>
              <tbody>
                {approvals.map((approval) => (
                  <tr key={approval.id} className="border-b border-gray-100">
                    <td className="py-2 pr-4">
                      <span className="px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-700">{typeLabel(approval.type)}</span>
                    </td>
                    <td className="py-2 pr-4 font-semibold text-gray-900">{formatCurrency(approval.amount)}</td>
                    <td className="py-2 pr-4 text-gray-600">{approval.initiatorName || '-'}</td>
                    <td className="py-2 pr-4">{statusBadge(approval.status)}</td>
                    <td className="py-2 pr-4 text-gray-500">{approval.createdAt ? formatDate(approval.createdAt) : '-'}</td>
                    <td className="py-2">
                      {approval.status === 'PENDING' ? (
                        <div className="flex space-x-2">
                          <Button size="sm" onClick={() => review(approval, 'approve')} loading={processing === approval.id}>{t.approvals.approve}</Button>
                          <Button size="sm" variant="danger" onClick={() => review(approval, 'reject')} loading={processing === approval.id}>{t.approvals.reject}</Button>
                        </div>
                      ) : (
                        <span className="text-xs text-gray-400">{approval.reviewedBy || '-'}</span>
                      )}
                    </td>
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
