import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { payoutApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import type { PayoutAccount, Payout, ContractInfo } from '../../types';

const emptyForm = { bankName: '', accountName: '', accountNumber: '', branch: '', isDefault: false };

export function PayoutPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [accounts, setAccounts] = useState<PayoutAccount[]>([]);
  const [payouts, setPayouts] = useState<Payout[]>([]);
  const [balance, setBalance] = useState<number | null>(null);
  const [contract, setContract] = useState<ContractInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [showPayoutForm, setShowPayoutForm] = useState(false);
  const [payoutAccountId, setPayoutAccountId] = useState('');
  const [payoutAmount, setPayoutAmount] = useState('');
  const [payoutRequiresApproval, setPayoutRequiresApproval] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const [acc, hist, bal, ctr] = await Promise.all([
        payoutApi.listAccounts(user.id),
        payoutApi.listPayouts(user.id),
        payoutApi.availableBalance(user.id),
        payoutApi.contract(user.id),
      ]);
      setAccounts(acc);
      setPayouts(hist);
      setBalance(bal);
      setContract(ctr);
    } catch (err) {
      console.error('Failed to load payout data', err);
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

  const handlePayout = async () => {
    if (!user || !payoutAccountId || !payoutAmount) return;
    setSubmitting(true);
    try {
      await payoutApi.requestPayout(user.id, { accountId: payoutAccountId, amount: Number(payoutAmount), requireApproval: payoutRequiresApproval || undefined });
      toast.success(t.payout.payoutRequested);
      setShowPayoutForm(false);
      setPayoutAmount('');
      setPayoutRequiresApproval(false);
      await load();
    } catch (err) {
      console.error('Failed to request payout', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const review = async (payout: Payout, action: 'approve' | 'reject') => {
    if (!user) return;
    if (!window.confirm(`${t.payout[action]} ${payout.amount.toLocaleString()} MMK?`)) return;
    setSubmitting(true);
    try {
      if (action === 'approve') await payoutApi.approvePayout(user.id, payout.id);
      else await payoutApi.rejectPayout(user.id, payout.id);
      toast.success(action === 'approve' ? t.payout.payoutApproved : t.payout.payoutRejected);
      await load();
    } catch (err) {
      console.error(`Failed to ${action} payout`, err);
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
        <div className="flex space-x-2">
          <Button variant="secondary" onClick={() => setShowPayoutForm(true)} disabled={accounts.length === 0}>{t.payout.requestPayout}</Button>
          <Button onClick={() => { setForm(emptyForm); setShowForm(true); }}>{t.payout.addAccount}</Button>
        </div>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : (
        <>
          <div className="grid md:grid-cols-3 gap-4">
            <Card>
              <p className="text-sm text-gray-500">{t.payout.availableBalance}</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">{balance?.toLocaleString() ?? '—'} MMK</p>
            </Card>
            <Card>
              <p className="text-sm text-gray-500">{t.payout.feeRate}</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">{contract?.feeRate != null ? `${(contract.feeRate * 100).toFixed(1)}%` : '—'}</p>
            </Card>
            <Card>
              <p className="text-sm text-gray-500">{t.payout.reserve}</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">{contract?.rollingReserveRate != null ? `${contract.rollingReserveRate}%` : '—'}</p>
              {contract?.settlementType && <p className="text-xs text-gray-400 mt-1">Settlement {contract.settlementType} · {contract.settlementFrequencyDays}d</p>}
            </Card>
          </div>

          {accounts.length === 0 ? (
            <Card><p className="text-center text-gray-500 py-6">{t.payout.noAccounts}</p></Card>
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

          <Card title={t.payout.history} subtitle={t.payout.historySubtitle}>
            {payouts.length === 0 ? (
              <p className="text-center text-gray-500 py-6">{t.payout.noPayouts}</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                      <th className="pb-2 pr-4">{t.payout.account}</th>
                      <th className="pb-2 pr-4">{t.payout.amount}</th>
                      <th className="pb-2 pr-4">{t.common.status}</th>
                      <th className="pb-2 pr-4">{t.payout.reference}</th>
                      <th className="pb-2">{t.payout.date}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {payouts.map((p) => (
                      <tr key={p.id} className="border-b border-gray-100">
                        <td className="py-2 pr-4 text-gray-900">{p.accountLabel}</td>
                        <td className="py-2 pr-4 text-gray-900">{p.amount.toLocaleString()} MMK</td>
                        <td className="py-2 pr-4">
                          <span className={`px-2 py-0.5 rounded text-xs font-medium ${p.status === 'COMPLETED' ? 'bg-green-100 text-green-700' : p.status === 'FAILED' || p.status === 'REJECTED' ? 'bg-red-100 text-red-700' : p.status === 'APPROVED' ? 'bg-blue-100 text-blue-700' : 'bg-yellow-100 text-yellow-700'}`}>{p.status}</span>
                        </td>
                        <td className="py-2 pr-4 font-mono text-xs text-gray-500">{p.reference ?? '—'}</td>
                        <td className="py-2 pr-4 text-gray-500">{p.completedAt ? new Date(p.completedAt).toLocaleString() : '—'}</td>
                        <td className="py-2">
                          {p.status === 'PENDING' && (
                            <div className="flex space-x-2">
                              <Button size="sm" onClick={() => review(p, 'approve')}>{t.payout.approve}</Button>
                              <Button size="sm" variant="danger" onClick={() => review(p, 'reject')}>{t.payout.reject}</Button>
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </>
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

      <Modal open={showPayoutForm} onClose={() => setShowPayoutForm(false)} title={t.payout.requestPayout}>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.payout.account}</label>
            <select value={payoutAccountId} onChange={(e) => setPayoutAccountId(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
              <option value="">{t.payout.selectAccount}</option>
              {accounts.map((account) => (
                <option key={account.id} value={account.id}>{account.bankName} · {account.accountName}</option>
              ))}
            </select>
          </div>
          <Input label={t.payout.amount} type="number" value={payoutAmount} onChange={(e) => setPayoutAmount(e.target.value)} />
          <label className="flex items-center space-x-2 text-sm text-gray-700">
            <input type="checkbox" checked={payoutRequiresApproval} onChange={(e) => setPayoutRequiresApproval(e.target.checked)} className="rounded" />
            <span>{t.payout.requireApproval}</span>
          </label>
          <p className="text-xs text-gray-500">{t.payout.availableBalance}: {balance?.toLocaleString() ?? '—'} MMK</p>
          <div className="flex space-x-3">
            <Button onClick={handlePayout} loading={submitting} disabled={!payoutAccountId || !payoutAmount} className="flex-1">{t.payout.confirmPayout}</Button>
            <Button variant="secondary" onClick={() => setShowPayoutForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

