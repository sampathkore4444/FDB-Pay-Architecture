import { useEffect, useState } from 'react';
import { walletApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { TransactionList } from '../../components/tables/TransactionList';
import { formatCurrency } from '../../utils';
import type { Wallet, Transaction } from '../../types';

export function WalletPage() {
  const user = useAuthStore((s) => s.user);
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    const fetchData = async () => {
      try {
        const [walletData, txnData] = await Promise.all([
          walletApi.getWallet(user.id),
          walletApi.getLedger(user.id, 0, 10),
        ]);
        setWallet(walletData);
        setTransactions(txnData.entries || []);
      } catch (err) {
        console.error('Failed to load wallet data:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [user]);

  if (loading) return <div className="text-center py-8">Loading...</div>;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">My Wallet</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card title="Total Balance" className="col-span-1">
          <p className="text-3xl font-bold text-gray-900">{formatCurrency(wallet?.balanceTotal || 0)}</p>
          <p className="text-sm text-gray-500 mt-1">MMK</p>
        </Card>

        <Card title="Available" className="col-span-1">
          <p className="text-2xl font-bold text-green-600">{formatCurrency(wallet?.balanceAvailable || 0)}</p>
        </Card>

        <Card title="Held / Frozen" className="col-span-1">
          <p className="text-2xl font-bold text-orange-600">{formatCurrency((wallet?.balanceHeld || 0) + (wallet?.balanceFrozen || 0))}</p>
        </Card>
      </div>

      <div className="flex space-x-3">
        <Button onClick={() => alert('Top-up flow coming soon')}>Top Up</Button>
        <Button variant="secondary" onClick={() => alert('Withdraw flow coming soon')}>Withdraw</Button>
      </div>

      <Card title="Recent Transactions">
        <TransactionList transactions={transactions} />
      </Card>
    </div>
  );
}
