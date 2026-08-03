import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { walletApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { TransactionList } from '../../components/tables/TransactionList';
import { formatCurrency } from '../../utils';
import { useTranslation } from '../../i18n';
import type { Wallet, Transaction } from '../../types';

const QUICK_AMOUNTS = [10000, 50000, 100000, 500000];

function getApiErrorMessage(err: unknown, fallback: string): string {
  const anyErr = err as { response?: { data?: { error?: { message?: string } } }; message?: string };
  return anyErr.response?.data?.error?.message || anyErr.message || fallback;
}

export function WalletPage() {
  const user = useAuthStore((s) => s.user);
  const { t } = useTranslation();
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState<'topup' | 'withdraw' | null>(null);
  const [amount, setAmount] = useState('');
  const [channel, setChannel] = useState('bank');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchData = async () => {
    if (!user) return;
    try {
      const [walletData, txnData] = await Promise.all([
        walletApi.getWallet(user.id),
        walletApi.getLedger(user.id, 0, 10),
      ]);
      setWallet(walletData);
      setTransactions(txnData.entries || []);
    } catch (err) {
      console.error('Failed to load wallet data:', err);
    }
  };

  useEffect(() => {
    setLoading(true);
    fetchData().finally(() => setLoading(false));
  }, [user]);

  const openModal = (type: 'topup' | 'withdraw') => {
    setModal(type);
    setAmount('');
    setChannel('bank');
    setError(null);
  };

  const closeModal = () => {
    setModal(null);
    setAmount('');
    setChannel('bank');
    setError(null);
  };

  const submitTopUp = async () => {
    if (!user) return;
    const value = Number(amount);
    if (!amount || !value || value <= 0) {
      setError(t.wallet.enterValidAmount);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await walletApi.topUp(user.id, value, channel);
      toast.success(t.wallet.topUpSuccess);
      closeModal();
      await fetchData();
    } catch (err) {
      toast.error(getApiErrorMessage(err, t.wallet.topUpFailed));
    } finally {
      setSubmitting(false);
    }
  };

  const submitWithdraw = async () => {
    if (!user) return;
    const value = Number(amount);
    if (!amount || !value || value <= 0) {
      setError(t.wallet.enterValidAmount);
      return;
    }
    if (value > (wallet?.balanceAvailable || 0)) {
      setError(t.wallet.insufficientBalance);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await walletApi.withdraw(user.id, value);
      toast.success(t.wallet.withdrawSuccess);
      closeModal();
      await fetchData();
    } catch (err) {
      toast.error(getApiErrorMessage(err, t.wallet.withdrawFailed));
    } finally {
      setSubmitting(false);
    }
  };

  const submit = () => {
    if (modal === 'topup') return submitTopUp();
    if (modal === 'withdraw') return submitWithdraw();
  };

  if (loading) return <div className="text-center py-8">{t.common.loading}</div>;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.wallet.title}</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card title={t.wallet.balance} className="col-span-1">
          <p className="text-3xl font-bold text-gray-900">{formatCurrency(wallet?.balanceTotal || 0)}</p>
          <p className="text-sm text-gray-500 mt-1">MMK</p>
        </Card>

        <Card title={t.wallet.available} className="col-span-1">
          <p className="text-2xl font-bold text-green-600">{formatCurrency(wallet?.balanceAvailable || 0)}</p>
        </Card>

        <Card title={t.wallet.held} className="col-span-1">
          <p className="text-2xl font-bold text-orange-600">{formatCurrency((wallet?.balanceHeld || 0) + (wallet?.balanceFrozen || 0))}</p>
        </Card>
      </div>

      <div className="flex space-x-3">
        <Button onClick={() => openModal('topup')}>{t.wallet.topUp}</Button>
        <Button variant="secondary" onClick={() => openModal('withdraw')}>{t.wallet.withdraw}</Button>
      </div>

      <Card title={t.wallet.history}>
        <TransactionList transactions={transactions} />
      </Card>

      <Modal open={modal !== null} onClose={closeModal} title={modal === 'topup' ? t.wallet.topUpTitle : t.wallet.withdrawTitle}>
        <div className="space-y-4">
          <div className="flex justify-between items-center bg-gray-50 rounded-lg px-4 py-3">
            <span className="text-sm text-gray-500">{t.wallet.availableBalance}</span>
            <span className="text-lg font-semibold text-gray-900">{formatCurrency(wallet?.balanceAvailable || 0)}</span>
          </div>

          <Input
            label={t.wallet.amount}
            type="number"
            min={1000}
            step={1000}
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder="0"
            error={error || undefined}
          />

          <div className="flex gap-2">
            {QUICK_AMOUNTS.map((q) => (
              <Button key={q} variant="secondary" size="sm" onClick={() => setAmount(String(q))}>
                {formatCurrency(q)}
              </Button>
            ))}
          </div>

          {modal === 'topup' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t.wallet.channel}</label>
              <select
                value={channel}
                onChange={(e) => setChannel(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="bank">{t.wallet.bank}</option>
                <option value="cash">{t.wallet.cash}</option>
                <option value="card">{t.wallet.card}</option>
              </select>
            </div>
          )}

          <div className="flex gap-2 justify-end pt-2">
            <Button variant="ghost" onClick={closeModal}>
              {t.common.cancel}
            </Button>
            <Button onClick={submit} loading={submitting}>
              {t.common.confirm}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
