import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { merchantApi, staffApi, storeApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { cn } from '../../utils';
import type { StaffAccount, Store } from '../../types';

const PERMISSIONS = ['terminal', 'refunds', 'reports', 'links', 'settlements', 'staff', 'inventory'];

const ROLE_DEFAULT_PERMISSIONS: Record<string, string[]> = {
  owner: ['terminal', 'refunds', 'reports', 'links', 'settlements', 'staff'],
  manager: ['terminal', 'refunds', 'reports', 'links', 'settlements'],
  cashier: ['terminal'],
  viewer: ['reports'],
};

export function StaffManagementPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [staff, setStaff] = useState<StaffAccount[]>([]);
  const [stores, setStores] = useState<Store[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAdd, setShowAdd] = useState(false);
  const [showRemove, setShowRemove] = useState<string | null>(null);
  const [showRoleChange, setShowRoleChange] = useState<StaffAccount | null>(null);
  const [showPermissions, setShowPermissions] = useState<StaffAccount | null>(null);

  const [addUserId, setAddUserId] = useState('');
  const [addRole, setAddRole] = useState('cashier');
  const [addLimit, setAddLimit] = useState<number>(500000);
  const [addStoreId, setAddStoreId] = useState('');
  const [addPermissions, setAddPermissions] = useState<string[]>(ROLE_DEFAULT_PERMISSIONS.cashier);
  const [newRole, setNewRole] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [merchantId, setMerchantId] = useState<string | null>(null);

  const loadStaff = async () => {
    if (!merchantId) return;
    setLoading(true);
    try {
      const data = await staffApi.getStaff(merchantId);
      setStaff(data);
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
      .then(async (profile) => {
        setMerchantId(profile.id);
        try {
          setStores(await storeApi.getStores(user.id));
        } catch (err) {
          console.error('Failed to load stores', err);
        }
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, [user]);

  useEffect(() => {
    if (merchantId) {
      loadStaff();
    }
  }, [merchantId]);

  const togglePermission = (list: string[], perm: string): string[] =>
    list.includes(perm) ? list.filter((p) => p !== perm) : [...list, perm];

  const handleRoleSelect = (role: string) => {
    setAddRole(role);
    setAddPermissions(ROLE_DEFAULT_PERMISSIONS[role] || []);
  };

  const handleAdd = async () => {
    if (!merchantId || !user || !addUserId) return;
    setSubmitting(true);
    try {
      await staffApi.addStaff(merchantId, user.id, {
        userId: addUserId,
        role: addRole,
        dailyLimit: addLimit,
        storeId: addStoreId || undefined,
        permissions: addPermissions,
      });
      setShowAdd(false);
      setAddUserId('');
      setAddRole('cashier');
      setAddLimit(500000);
      setAddStoreId('');
      setAddPermissions(ROLE_DEFAULT_PERMISSIONS.cashier);
      await loadStaff();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRemove = async () => {
    if (!merchantId || !showRemove) return;
    setSubmitting(true);
    try {
      await staffApi.removeStaff(merchantId, showRemove);
      setShowRemove(null);
      await loadStaff();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRoleChange = async () => {
    if (!merchantId || !showRoleChange || !newRole) return;
    setSubmitting(true);
    try {
      await staffApi.changeRole(merchantId, showRoleChange.id, newRole);
      setShowRoleChange(null);
      setNewRole('');
      await loadStaff();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleSavePermissions = async () => {
    if (!merchantId || !showPermissions) return;
    setSubmitting(true);
    try {
      await staffApi.updatePermissions(merchantId, showPermissions.id, showPermissions.permissions || []);
      setShowPermissions(null);
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

  const storeName = (storeId?: string) => stores.find((s) => s.id === storeId)?.name;

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
              <div key={s.id} className="flex items-start justify-between p-4 bg-gray-50 rounded-lg">
                <div className="space-y-1">
                  <p className="text-sm font-medium text-gray-900">{s.userName || s.userPhone}</p>
                  <div className="flex items-center space-x-3 text-xs text-gray-500">
                    <span>{s.userPhone}</span>
                    {s.storeId && <span>{t.staff.store}: {storeName(s.storeId) || s.storeId.slice(0, 8)}</span>}
                    <span className={cn('px-2 py-0.5 rounded-full',
                      s.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    )}>
                      {s.status === 'ACTIVE' ? t.common.active : t.staff.deactivated}
                    </span>
                  </div>
                  <div className="flex flex-wrap gap-1 pt-1">
                    {(s.permissions || []).map((p) => (
                      <span key={p} className="px-2 py-0.5 rounded bg-blue-100 text-blue-700 text-xs">{p}</span>
                    ))}
                  </div>
                </div>
                <div className="flex flex-col items-end space-y-2">
                  <div className="text-right">
                    <span className="text-xs text-gray-500">{roleLabel(s.role)}</span>
                    <p className="text-xs text-gray-400">{t.staff.dailyLimit}: {(s.dailyLimit ?? 0).toLocaleString()} MMK</p>
                  </div>
                  <div className="flex space-x-1">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => setShowPermissions({ ...s, permissions: s.permissions || ROLE_DEFAULT_PERMISSIONS[s.role] || [] })}
                    >
                      {t.staff.permissions}
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => { setShowRoleChange(s); setNewRole(s.role); }}
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
              onChange={(e) => handleRoleSelect(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
            >
              <option value="cashier">{t.staff.roles.cashier}</option>
              <option value="manager">{t.staff.roles.manager}</option>
              <option value="viewer">{t.staff.roles.viewer}</option>
              <option value="owner">{t.staff.roles.owner}</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.staff.store}</label>
            <select
              value={addStoreId}
              onChange={(e) => setAddStoreId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
            >
              <option value="">{t.staff.allStores}</option>
              {stores.map((st) => <option key={st.id} value={st.id}>{st.name}</option>)}
            </select>
          </div>
          <Input
            label={t.staff.dailyLimit}
            type="number"
            value={addLimit || ''}
            onChange={(e) => setAddLimit(Number(e.target.value))}
            placeholder="500000"
          />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">{t.staff.permissions}</label>
            <div className="grid grid-cols-2 gap-2">
              {PERMISSIONS.map((p) => (
                <label key={p} className="flex items-center space-x-2 text-sm text-gray-700">
                  <input
                    type="checkbox"
                    checked={addPermissions.includes(p)}
                    onChange={() => setAddPermissions((prev) => togglePermission(prev, p))}
                    className="rounded border-gray-300 text-blue-600"
                  />
                  <span>{p}</span>
                </label>
              ))}
            </div>
          </div>
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
              <option value="owner">{t.staff.roles.owner}</option>
            </select>
          </div>
          <div className="flex space-x-3">
            <Button onClick={handleRoleChange} loading={submitting} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowRoleChange(null)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!showPermissions} onClose={() => setShowPermissions(null)} title={t.staff.permissions}>
        <div className="space-y-4">
          <p className="text-sm text-gray-700">{showPermissions?.userName || showPermissions?.userPhone}</p>
          <div className="grid grid-cols-2 gap-2">
            {PERMISSIONS.map((p) => (
              <label key={p} className="flex items-center space-x-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={(showPermissions?.permissions || []).includes(p)}
                  onChange={() =>
                    setShowPermissions((prev) => (prev ? { ...prev, permissions: togglePermission(prev.permissions || [], p) } : prev))
                  }
                  className="rounded border-gray-300 text-blue-600"
                />
                <span>{p}</span>
              </label>
            ))}
          </div>
          <div className="flex space-x-3">
            <Button onClick={handleSavePermissions} loading={submitting} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowPermissions(null)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
