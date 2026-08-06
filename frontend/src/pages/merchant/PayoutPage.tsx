import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { payoutApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import type { PayoutAccount } from '../../types';

const emptyForm = { bankName: '', accountName: '', accountNumber: '', branch: '', isDefault: false };

export function PayoutPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [accounts, setAccounts] = useState<PayoutAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setAccounts(await payoutApi.listAccounts(user.id));
    } catch (err) {
      console.error('Failed to load payout accounts', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const handleSave = async () => {
    if (!user || !form.bankName || !form.accountName || !form.accountNumber) return;
    setSubmitting(true);
    try {
      await payoutApi.createAccount(user.id, { ...form, isDefault: form.isDefault || undefined });
      toast.success(t.payout.created);
      setShowForm(false);
      await load();
    } catch (err) {
      console.error('Failed to save payout account', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const setDefault = async (account: PayoutAccount) => {
    if (!user) return;
    try {
      await payoutApi.setDefault(user.id, account.id);
      toast.success(t.payout.defaultSet);
      await load();
    } catch (err) {
      console.error('Failed to set default', err);
      toast.error(t.common.loadFailed);
    }
  };

  const remove = async (account: PayoutAccount) => {
    if (!user || !window.confirm(t.payout.deleteConfirm)) return;
    try {
      await payoutApi.deleteAccount(user.id, account.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to delete account', err);
      toast.error(t.common.loadFailed);
    }
  };

  const set = (key: keyof typeof emptyForm, value: string | boolean) => setForm((f) => ({ ...f, [key]: value }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.payout.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.payout.subtitle}</p>
        </div>
        <Button onClick={() => { setForm(emptyForm); setShowForm(true); }}>{t.payout.addAccount}</Button>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : accounts.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.payout.noAccounts}</p></Card>
      ) : (
        <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
          {accounts.map((account) => (
            <Card key={account.id}>
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold text-gray-900">{account.bankName}</h3>
                  <p className="text-sm text-gray-500 mt-1">{account.accountName}</p>
                  <p className="text-sm text-gray-500">{account.accountNumber}</p>
                  {account.branch && <p className="text-sm text-gray-400">{account.branch}</p>}
                </div>
                {account.isDefault && (
                  <span className="px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-700">{t.payout.default}</span>
                )}
              </div>
              <div className="mt-4 flex justify-end space-x-2">
                {!account.isDefault && (
                  <Button size="sm" variant="secondary" onClick={() => setDefault(account)}>{t.payout.setDefault}</Button>
                )}
                <Button size="sm" variant="danger" onClick={() => remove(account)}>{t.common.delete}</Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={showForm} onClose={() => setShowForm(false)} title={t.payout.addAccount}>
        <div className="space-y-4">
          <Input label={t.payout.bankName} value={form.bankName} onChange={(e) => set('bankName', e.target.value)} />
          <Input label={t.payout.accountName} value={form.accountName} onChange={(e) => set('accountName', e.target.value)} />
          <Input label={t.payout.accountNumber} value={form.accountNumber} onChange={(e) => set('accountNumber', e.target.value)} />
          <Input label={t.payout.branch} value={form.branch} onChange={(e) => set('branch', e.target.value)} />
          <label className="flex items-center space-x-2 text-sm text-gray-700">
            <input type="checkbox" checked={form.isDefault} onChange={(e) => set('isDefault', e.target.checked)} className="rounded" />
            <span>{t.payout.isDefault}</span>
          </label>
          <div className="flex space-x-3">
            <Button onClick={handleSave} loading={submitting} disabled={!form.bankName || !form.accountName || !form.accountNumber} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
