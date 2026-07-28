import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { staffApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { cn } from '../../utils';

interface StaffMember {
  id: string;
  userId: string;
  userName: string;
  userPhone: string;
  role: string;
  dailyLimit: number;
  status: string;
}

export function StaffManagementPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [staff, setStaff] = useState<StaffMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAdd, setShowAdd] = useState(false);
  const [showRemove, setShowRemove] = useState<string | null>(null);
  const [showRoleChange, setShowRoleChange] = useState<{ id: string; currentRole: string } | null>(null);

  const [addUserId, setAddUserId] = useState('');
  const [addRole, setAddRole] = useState('cashier');
  const [addLimit, setAddLimit] = useState<number>(500000);
  const [newRole, setNewRole] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadStaff = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await staffApi.getStaff(user.id);
      setStaff(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStaff();
  }, [user]);

  const handleAdd = async () => {
    if (!user || !addUserId) return;
    setSubmitting(true);
    try {
      await staffApi.addStaff(user.id, {
        userId: addUserId,
        role: addRole,
        dailyLimit: addLimit,
      });
      setShowAdd(false);
      setAddUserId('');
      setAddRole('cashier');
      setAddLimit(500000);
      await loadStaff();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRemove = async () => {
    if (!user || !showRemove) return;
    setSubmitting(true);
    try {
      await staffApi.removeStaff(user.id, showRemove);
      setShowRemove(null);
      await loadStaff();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRoleChange = async () => {
    if (!user || !showRoleChange || !newRole) return;
    setSubmitting(true);
    try {
      await staffApi.changeRole(user.id, showRoleChange.id, newRole);
      setShowRoleChange(null);
      setNewRole('');
      await loadStaff();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const roleLabel = (role: string) => {
    const labels: Record<string, string> = {
      cashier: t.staff.roles.cashier,
      manager: t.staff.roles.manager,
      viewer: t.staff.roles.viewer,
    };
    return labels[role] || role;
  };

  if (loading) return <div className="text-center py-8">{t.common.loading}</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t.staff.title}</h1>
        <Button onClick={() => setShowAdd(true)}>{t.staff.addStaff}</Button>
      </div>

      <Card title={t.staff.staffList}>
        {staff.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.staff.noStaff}</p>
        ) : (
          <div className="space-y-2">
            {staff.map((s) => (
              <div key={s.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div className="space-y-1">
                  <p className="text-sm font-medium text-gray-900">{s.userName || s.userPhone}</p>
                  <div className="flex items-center space-x-3 text-xs text-gray-500">
                    <span>{s.userPhone}</span>
                    <span className={cn('px-2 py-0.5 rounded-full',
                      s.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    )}>
                      {s.status === 'ACTIVE' ? t.common.active : t.staff.deactivated}
                    </span>
                  </div>
                </div>
                <div className="flex items-center space-x-4">
                  <div className="text-right">
                    <span className="text-xs text-gray-500">{roleLabel(s.role)}</span>
                    <p className="text-xs text-gray-400">Limit: {s.dailyLimit.toLocaleString()} MMK</p>
                  </div>
                  <div className="flex space-x-1">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => { setShowRoleChange({ id: s.id, currentRole: s.role }); setNewRole(s.role); }}
                    >
                      {t.staff.changeRole}
                    </Button>
                    <Button
                      size="sm"
                      variant="danger"
                      onClick={() => setShowRemove(s.id)}
                    >
                      {t.staff.removeStaff}
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Modal open={showAdd} onClose={() => setShowAdd(false)} title={t.staff.addStaff}>
        <div className="space-y-4">
          <Input label={t.staff.selectUser} value={addUserId} onChange={(e) => setAddUserId(e.target.value)} placeholder="User ID" />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.staff.selectRole}</label>
            <select
              value={addRole}
              onChange={(e) => setAddRole(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
            >
              <option value="cashier">{t.staff.roles.cashier}</option>
              <option value="manager">{t.staff.roles.manager}</option>
              <option value="viewer">{t.staff.roles.viewer}</option>
            </select>
          </div>
          <Input
            label={t.staff.dailyLimit}
            type="number"
            value={addLimit || ''}
            onChange={(e) => setAddLimit(Number(e.target.value))}
            placeholder="500000"
          />
          <div className="flex space-x-3">
            <Button onClick={handleAdd} loading={submitting} className="flex-1">{t.common.submit}</Button>
            <Button variant="secondary" onClick={() => setShowAdd(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!showRemove} onClose={() => setShowRemove(null)} title={t.staff.removeStaff}>
        <div className="space-y-4">
          <p className="text-sm text-gray-700">{t.staff.confirmRemove}</p>
          <div className="flex space-x-3">
            <Button variant="danger" onClick={handleRemove} loading={submitting} className="flex-1">{t.common.confirm}</Button>
            <Button variant="secondary" onClick={() => setShowRemove(null)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!showRoleChange} onClose={() => setShowRoleChange(null)} title={t.staff.changeRole}>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.staff.selectRole}</label>
            <select
              value={newRole}
              onChange={(e) => setNewRole(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
            >
              <option value="cashier">{t.staff.roles.cashier}</option>
              <option value="manager">{t.staff.roles.manager}</option>
              <option value="viewer">{t.staff.roles.viewer}</option>
            </select>
          </div>
          <div className="flex space-x-3">
            <Button onClick={handleRoleChange} loading={submitting} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowRoleChange(null)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
