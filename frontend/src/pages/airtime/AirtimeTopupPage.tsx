import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { airtimeApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatDate } from '../../utils';
import type { Transaction } from '../../types';

const providers = ['MPT', 'Ooredoo', 'Mytel', 'Atom'] as const;
const quickAmounts = [1000, 3000, 5000, 10000, 20000, 50000];

export function AirtimeTopupPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [provider, setProvider] = useState<string>('');
  const [phone, setPhone] = useState('');
  const [amount, setAmount] = useState<number>(0);
  const [history, setHistory] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    airtimeApi.getHistory(user.id).then(setHistory).catch(console.error);
  }, [user]);

  const handleTopUp = async () => {
    if (!user || !provider || !phone || !amount) return;
    setLoading(true);
    setResult(null);
    try {
      await airtimeApi.topup(user.id, { provider, phone, amount });
      setResult(t.airtime.success);
      const updated = await airtimeApi.getHistory(user.id);
      setHistory(updated);
    } catch (err) {
      setResult(`${t.common.error}: ${err instanceof Error ? err.message : t.common.error}`);
    } finally {
      setLoading(false);
    }
  };

  const providerKeyMap: Record<string, keyof typeof t.airtime> = {
    MPT: 'mpt',
    Ooredoo: 'ooredoo',
    Mytel: 'mytel',
    Atom: 'atom',
  };

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.airtime.title}</h1>

      <Card>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">{t.airtime.selectProvider}</label>
            <div className="grid grid-cols-4 gap-2">
              {providers.map((p) => (
                <button
                  key={p}
                  onClick={() => setProvider(p)}
                  className={`px-3 py-2 rounded-lg border text-sm font-medium transition-colors ${
                    provider === p
                      ? 'bg-blue-600 text-white border-blue-600'
                      : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  {t.airtime[providerKeyMap[p]]}
                </button>
              ))}
            </div>
          </div>

          <Input
            label={t.airtime.enterPhone}
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder={t.airtime.phonePlaceholder}
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.airtime.amount}</label>
            <div className="grid grid-cols-3 gap-2 mb-2">
              {quickAmounts.map((qa) => (
                <button
                  key={qa}
                  onClick={() => setAmount(qa)}
                  className={`px-3 py-2 rounded-lg border text-sm transition-colors ${
                    amount === qa
                      ? 'bg-blue-600 text-white border-blue-600'
                      : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  {qa.toLocaleString()}
                </button>
              ))}
            </div>
            <Input
              label={`${t.airtime.amount}`}
              type="number"
              value={amount || ''}
              onChange={(e) => setAmount(Number(e.target.value))}
              placeholder="0"
            />
          </div>

          {result && (
            <p className={`text-sm ${result.includes(t.common.error) ? 'text-red-600' : 'text-green-600'}`}>
              {result}
            </p>
          )}

          <Button
            onClick={handleTopUp}
            loading={loading}
            className="w-full"
            disabled={!provider || !phone || !amount}
          >
            {t.airtime.topUp}
          </Button>
        </div>
      </Card>

      <Card title={t.airtime.history}>
        {history.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.common.noData}</p>
        ) : (
          <div className="space-y-2">
            {history.slice(0, 10).map((txn) => (
              <div key={txn.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div>
                  <p className="text-sm font-medium text-gray-900">{txn.description || txn.type}</p>
                  <p className="text-xs text-gray-500">{formatDate(txn.createdAt)}</p>
                </div>
                <span className="text-sm font-semibold text-green-600">-{txn.amount.toLocaleString()} MMK</span>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
