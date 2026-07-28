import { useEffect, useState } from 'react';
import { useAuthStore } from '../../store/authStore';
import { agentApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency, formatDate } from '../../utils';
import { useTranslation } from '../../i18n';

export function AgentPage() {
  const user = useAuthStore((s) => s.user);
  const { t } = useTranslation();
  const [account, setAccount] = useState<{ floatBalance: number; commissionBalance: number; status: string } | null>(null);
  const [history, setHistory] = useState<{ id: string; type: string; amount: number; description: string; createdAt: string }[]>([]);
  const [loading, setLoading] = useState(true);

  const [cashInPhone, setCashInPhone] = useState('');
  const [cashInAmount, setCashInAmount] = useState('');
  const [cashOutPhone, setCashOutPhone] = useState('');
  const [cashOutAmount, setCashOutAmount] = useState('');
  const [processing, setProcessing] = useState<'in' | 'out' | null>(null);

  useEffect(() => {
    if (!user) return;
    const fetchData = async () => {
      try {
        const [acct, hist] = await Promise.all([
          agentApi.getAccount(user.id).catch(() => null),
          agentApi.getFloatHistory(user.id).catch(() => []),
        ]);
        if (acct) setAccount(acct);
        setHistory(hist);
      } catch (err) {
        console.error('Failed to load agent data', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [user]);

  const handleCashIn = async () => {
    if (!user || !cashInPhone || !cashInAmount) return;
    setProcessing('in');
    try {
      await agentApi.cashIn(user.id, { customerPhone: cashInPhone, amount: Number(cashInAmount) });
      alert('Cash-in successful');
      setCashInPhone('');
      setCashInAmount('');
      const [acct, hist] = await Promise.all([
        agentApi.getAccount(user.id),
        agentApi.getFloatHistory(user.id),
      ]);
      setAccount(acct);
      setHistory(hist);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Cash-in failed';
      alert(msg);
    } finally {
      setProcessing(null);
    }
  };

  const handleCashOut = async () => {
    if (!user || !cashOutPhone || !cashOutAmount) return;
    setProcessing('out');
    try {
      await agentApi.cashOut(user.id, { customerPhone: cashOutPhone, amount: Number(cashOutAmount) });
      alert('Cash-out successful');
      setCashOutPhone('');
      setCashOutAmount('');
      const [acct, hist] = await Promise.all([
        agentApi.getAccount(user.id),
        agentApi.getFloatHistory(user.id),
      ]);
      setAccount(acct);
      setHistory(hist);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Cash-out failed';
      alert(msg);
    } finally {
      setProcessing(null);
    }
  };

  if (loading) {
    return <div className="text-center py-12 text-gray-500">{t.common.loading}</div>;
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Agent Portal</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card title="Float Balance">
          <p className="text-3xl font-bold text-green-600">{formatCurrency(account?.floatBalance ?? 0)}</p>
          <p className="text-sm text-gray-500 mt-1">Commission: {formatCurrency(account?.commissionBalance ?? 0)}</p>
          <p className="text-xs mt-2">
            <span className={`px-2 py-0.5 rounded text-xs font-medium ${
              account?.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
            }`}>{account?.status ?? 'UNKNOWN'}</span>
          </p>
        </Card>

        <Card title="Recent Activity">
          {history.length === 0 ? (
            <p className="text-center text-gray-500 py-4">{t.common.noData}</p>
          ) : (
            <div className="space-y-2 max-h-48 overflow-y-auto">
              {history.slice(0, 5).map((h) => (
                <div key={h.id} className="flex justify-between items-center text-sm">
                  <div>
                    <span className="font-medium">{h.type}</span>
                    <span className="text-gray-500 ml-2">{h.description}</span>
                  </div>
                  <div className="text-right">
                    <span className="font-medium">{formatCurrency(h.amount)}</span>
                    <span className="text-gray-400 text-xs block">{formatDate(h.createdAt)}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card title="Cash-In">
          <div className="space-y-4">
            <Input
              label="Customer Phone"
              value={cashInPhone}
              onChange={(e) => setCashInPhone(e.target.value)}
              placeholder="+959XXXXXXXX"
            />
            <Input
              label="Amount (MMK)"
              type="number"
              value={cashInAmount}
              onChange={(e) => setCashInAmount(e.target.value)}
              placeholder="0"
            />
            <Button
              className="w-full"
              onClick={handleCashIn}
              disabled={processing === 'in' || !cashInPhone || !cashInAmount}
            >
              {processing === 'in' ? 'Processing...' : 'Process Cash-In'}
            </Button>
          </div>
        </Card>

        <Card title="Cash-Out">
          <div className="space-y-4">
            <Input
              label="Customer Phone"
              value={cashOutPhone}
              onChange={(e) => setCashOutPhone(e.target.value)}
              placeholder="+959XXXXXXXX"
            />
            <Input
              label="Amount (MMK)"
              type="number"
              value={cashOutAmount}
              onChange={(e) => setCashOutAmount(e.target.value)}
              placeholder="0"
            />
            <Button
              variant="secondary"
              className="w-full"
              onClick={handleCashOut}
              disabled={processing === 'out' || !cashOutPhone || !cashOutAmount}
            >
              {processing === 'out' ? 'Processing...' : 'Process Cash-Out'}
            </Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
