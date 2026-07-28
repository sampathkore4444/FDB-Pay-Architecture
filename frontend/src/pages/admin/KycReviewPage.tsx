import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { adminApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { toast } from 'sonner';

interface KycSubmission {
  id: string;
  userId: string;
  userName: string;
  userPhone: string;
  documentType: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  submittedAt: string;
  documentUrl?: string;
}

export function KycReviewPage() {
  const { t } = useTranslation();
  const [submissions, setSubmissions] = useState<KycSubmission[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('PENDING');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [selectedKyc, setSelectedKyc] = useState<KycSubmission | null>(null);
  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [reviewAction, setReviewAction] = useState<'APPROVED' | 'REJECTED'>('APPROVED');
  const [reviewReason, setReviewReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [stats] = useState({ pending: 0, approvedToday: 0, rejectedToday: 0 });

  const fetchSubmissions = async () => {
    setLoading(true);
    try {
      const data = await adminApi.getKycPending(page, 20);
      const list = (data as unknown as { requests: KycSubmission[] })?.requests || [];
      const filtered = statusFilter === 'ALL' ? list : list.filter((k) => k.status === statusFilter);
      setSubmissions(filtered);
      setTotalPages(Math.max(1, Math.ceil(list.length / 20)));
    } catch {
      toast.error(t.common.error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSubmissions();
  }, [page, statusFilter]);

  const openReview = (kyc: KycSubmission, action: 'APPROVED' | 'REJECTED') => {
    setSelectedKyc(kyc);
    setReviewAction(action);
    setReviewReason('');
    setReviewModalOpen(true);
  };

  const submitReview = async () => {
    if (!selectedKyc) return;
    setSubmitting(true);
    try {
      await adminApi.reviewKyc(selectedKyc.id, {
        status: reviewAction,
        reason: reviewReason || undefined,
      });
      toast.success(reviewAction === 'APPROVED' ? t.common.approved : t.common.rejected);
      setReviewModalOpen(false);
      fetchSubmissions();
    } catch {
      toast.error(t.common.error);
    } finally {
      setSubmitting(false);
    }
  };

  const statusColor = (s: string) => {
    if (s === 'PENDING') return 'bg-yellow-100 text-yellow-800';
    if (s === 'APPROVED') return 'bg-green-100 text-green-800';
    return 'bg-red-100 text-red-800';
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.admin.kyc}</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <p className="text-sm text-gray-500">{t.common.pending}</p>
          <p className="text-2xl font-bold text-yellow-600">{stats.pending}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">{t.common.approved} Today</p>
          <p className="text-2xl font-bold text-green-600">{stats.approvedToday}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">{t.common.rejected} Today</p>
          <p className="text-2xl font-bold text-red-600">{stats.rejectedToday}</p>
        </Card>
      </div>

      <Card>
        <div className="flex items-center gap-4 mb-4">
          <div className="flex gap-2">
            {['PENDING', 'APPROVED', 'REJECTED', 'ALL'].map((s) => (
              <Button
                key={s}
                variant={statusFilter === s ? 'primary' : 'secondary'}
                size="sm"
                onClick={() => { setStatusFilter(s); setPage(0); }}
              >
                {s === 'ALL' ? t.common.all : t.common[s.toLowerCase() as keyof typeof t.common] || s}
              </Button>
            ))}
          </div>
        </div>

        {loading ? (
          <p className="text-center text-gray-500 py-8">{t.common.loading}</p>
        ) : submissions.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.admin.noPending}</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.name}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.phone}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.type}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.date}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.status}</th>
                  <th className="text-right py-3 px-2 font-medium text-gray-500">{t.common.actions}</th>
                </tr>
              </thead>
              <tbody>
                {submissions.map((kyc) => (
                  <tr key={kyc.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-3 px-2 text-gray-900">{kyc.userName}</td>
                    <td className="py-3 px-2 text-gray-600">{kyc.userPhone}</td>
                    <td className="py-3 px-2 text-gray-600">{kyc.documentType}</td>
                    <td className="py-3 px-2 text-gray-600">{new Date(kyc.submittedAt).toLocaleDateString()}</td>
                    <td className="py-3 px-2">
                      <span className={`inline-flex px-2 py-1 rounded-full text-xs font-medium ${statusColor(kyc.status)}`}>
                        {t.common[kyc.status.toLowerCase() as keyof typeof t.common] || kyc.status}
                      </span>
                    </td>
                    <td className="py-3 px-2 text-right">
                      {kyc.status === 'PENDING' && (
                        <div className="flex gap-2 justify-end">
                          <Button variant="primary" size="sm" onClick={() => openReview(kyc, 'APPROVED')}>
                            {t.common.approved}
                          </Button>
                          <Button variant="danger" size="sm" onClick={() => openReview(kyc, 'REJECTED')}>
                            {t.common.rejected}
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

      <Modal open={reviewModalOpen} onClose={() => setReviewModalOpen(false)} title={reviewAction === 'APPROVED' ? t.common.approved : t.common.rejected}>
        {selectedKyc && (
          <div className="space-y-4">
            <div>
              <p className="text-sm text-gray-500">{t.common.name}</p>
              <p className="font-medium">{selectedKyc.userName}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">{t.common.type}</p>
              <p className="font-medium">{selectedKyc.documentType}</p>
            </div>
            {reviewAction === 'REJECTED' && (
              <Input
                label={t.payroll.rejectionReason}
                value={reviewReason}
                onChange={(e) => setReviewReason(e.target.value)}
                placeholder="Enter reason for rejection..."
              />
            )}
            <div className="flex gap-2 justify-end">
              <Button variant="ghost" onClick={() => setReviewModalOpen(false)}>
                {t.common.cancel}
              </Button>
              <Button variant={reviewAction === 'APPROVED' ? 'primary' : 'danger'} onClick={submitReview} loading={submitting}>
                {t.common.confirm}
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
