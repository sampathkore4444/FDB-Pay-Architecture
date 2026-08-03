import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { adminApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { toast } from 'sonner';
import type { User } from '../../types';

export function UserManagementPage() {
  const { t } = useTranslation();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [actionModalOpen, setActionModalOpen] = useState(false);
  const [actionStatus, setActionStatus] = useState('');
  const [actionReason, setActionReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = { page: String(page) };
      if (search) params.search = search;
      if (statusFilter) params.status = statusFilter;
      const data = await adminApi.getUsers(params as { search?: string; status?: string; page?: number });
      const list = (data as unknown as { users: User[] })?.users || [];
      setUsers(list);
      setTotalPages(Math.max(1, Math.ceil(list.length / 20)));
    } catch {
      toast.error(t.common.error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [page, statusFilter]);

  const handleSearch = () => {
    setPage(0);
    fetchUsers();
  };

  const openDetail = (user: User) => {
    setSelectedUser(user);
    setDetailModalOpen(true);
  };

  const openAction = (user: User, status: string) => {
    setSelectedUser(user);
    setActionStatus(status);
    setActionReason('');
    setActionModalOpen(true);
  };

  const submitAction = async () => {
    if (!selectedUser) return;
    setSubmitting(true);
    try {
      await adminApi.updateUserStatus(selectedUser.id, {
        status: actionStatus,
        reason: actionReason || undefined,
      });
      toast.success(t.common.success);
      setActionModalOpen(false);
      fetchUsers();
    } catch {
      toast.error(t.common.error);
    } finally {
      setSubmitting(false);
    }
  };

  const statusColor = (s: string) => {
    if (s === 'ACTIVE') return 'bg-green-100 text-green-800';
    if (s === 'SUSPENDED') return 'bg-red-100 text-red-800';
    if (s === 'PENDING') return 'bg-yellow-100 text-yellow-800';
    if (s === 'LOCKED') return 'bg-orange-100 text-orange-800';
    return 'bg-gray-100 text-gray-800';
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">User Management</h1>

      <Card>
        <div className="flex items-end gap-4 mb-4">
          <div className="flex-1">
            <Input
              label={t.common.search}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by phone or name..."
            />
          </div>
          <Button variant="primary" onClick={handleSearch}>
            {t.common.search}
          </Button>
          <div className="flex gap-2">
            {['', 'ACTIVE', 'SUSPENDED', 'PENDING'].map((s) => (
              <Button
                key={s}
                variant={statusFilter === s ? 'primary' : 'secondary'}
                size="sm"
                onClick={() => { setStatusFilter(s); setPage(0); }}
              >
                {s || t.common.all}
              </Button>
            ))}
          </div>
        </div>

        {loading ? (
          <p className="text-center text-gray-500 py-8">{t.common.loading}</p>
        ) : users.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.common.noData}</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.name}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.phone}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">KYC Tier</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.common.status}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">Registered</th>
                  <th className="text-right py-3 px-2 font-medium text-gray-500">{t.common.actions}</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-3 px-2 text-gray-900">{user.name}</td>
                    <td className="py-3 px-2 text-gray-600">{user.phone}</td>
                    <td className="py-3 px-2 text-gray-600">{user.kycTier}</td>
                    <td className="py-3 px-2">
                      <span className={`inline-flex px-2 py-1 rounded-full text-xs font-medium ${statusColor(user.status)}`}>
                        {user.status}
                      </span>
                    </td>
                    <td className="py-3 px-2 text-gray-600">-</td>
                    <td className="py-3 px-2 text-right">
                      <div className="flex gap-2 justify-end">
                        <Button variant="ghost" size="sm" onClick={() => openDetail(user)}>
                          {t.common.viewDetails}
                        </Button>
                        {user.status === 'ACTIVE' ? (
                          <Button variant="danger" size="sm" onClick={() => openAction(user, 'SUSPENDED')}>
                            Suspend
                          </Button>
                        ) : (
                          <Button variant="primary" size="sm" onClick={() => openAction(user, 'ACTIVE')}>
                            Activate
                          </Button>
                        )}
                      </div>
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

      <Modal open={detailModalOpen} onClose={() => setDetailModalOpen(false)} title={t.common.details}>
        {selectedUser && (
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-500">{t.common.name}</span>
              <span className="font-medium">{selectedUser.name}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">{t.common.phone}</span>
              <span className="font-medium">{selectedUser.phone}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">{t.common.email}</span>
              <span className="font-medium">{selectedUser.email || '-'}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">Role</span>
              <span className="font-medium">{selectedUser.role}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">KYC Tier</span>
              <span className="font-medium">{selectedUser.kycTier}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">{t.common.status}</span>
              <span className={`inline-flex px-2 py-1 rounded-full text-xs font-medium ${statusColor(selectedUser.status)}`}>
                {selectedUser.status}
              </span>
            </div>
            <div className="pt-2">
              <Button variant="ghost" onClick={() => setDetailModalOpen(false)} className="w-full">
                {t.common.close}
              </Button>
            </div>
          </div>
        )}
      </Modal>

      <Modal open={actionModalOpen} onClose={() => setActionModalOpen(false)} title={`${actionStatus === 'SUSPENDED' ? 'Suspend' : 'Activate'} User`}>
        {selectedUser && (
          <div className="space-y-4">
            <div>
              <p className="text-sm text-gray-500">{t.common.name}</p>
              <p className="font-medium">{selectedUser.name}</p>
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
              <Button variant={actionStatus === 'SUSPENDED' ? 'danger' : 'primary'} onClick={submitAction} loading={submitting}>
                {t.common.confirm}
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
