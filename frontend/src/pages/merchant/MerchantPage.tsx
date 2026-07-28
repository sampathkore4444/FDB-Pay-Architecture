import { useEffect, useState } from 'react';
import { useAuthStore } from '../../store/authStore';
import { walletApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { formatCurrency } from '../../utils';
import type { Wallet } from '../../types';

export function MerchantPage() {
  const user = useAuthStore((s) => s.user);
  const [wallet, setWallet] = useState<Wallet | null>(null);

  useEffect(() => {
    if (!user) return;
    walletApi.getWallet(user.id).then(setWallet).catch(console.error);
  }, [user]);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Merchant Dashboard</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card title="Today's Settlement">
          <p className="text-3xl font-bold text-green-600">{formatCurrency(0)}</p>
          <p className="text-sm text-gray-500 mt-1">Next settlement: T+1</p>
        </Card>

        <Card title="QR Code">
          <div className="text-center py-4">
            <div className="w-48 h-48 bg-gray-100 rounded-lg mx-auto flex items-center justify-center">
              <p className="text-gray-400">QR Code</p>
            </div>
            <Button variant="secondary" className="mt-4" onClick={() => alert('QR generation coming soon')}>
              Generate QR
            </Button>
          </div>
        </Card>

        <Card title="Transactions" className="col-span-full">
          <p className="text-center text-gray-500 py-8">No transactions today</p>
        </Card>
      </div>
    </div>
  );
}
