import { useState } from 'react';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';

export function AgentPage() {
  const [customerPhone, setCustomerPhone] = useState('');
  const [amount, setAmount] = useState<number>(0);

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Agent Portal</h1>

      <Card title="Float Balance">
        <p className="text-3xl font-bold text-gray-900">MMK 0</p>
        <p className="text-sm text-gray-500 mt-1">Commission: MMK 0</p>
      </Card>

      <Card title="Cash-In">
        <div className="space-y-4">
          <Input
            label="Customer Phone"
            value={customerPhone}
            onChange={(e) => setCustomerPhone(e.target.value)}
            placeholder="+959XXXXXXXX"
          />
          <Input
            label="Amount (MMK)"
            type="number"
            value={amount || ''}
            onChange={(e) => setAmount(Number(e.target.value))}
            placeholder="0"
          />
          <Button className="w-full" onClick={() => alert('Cash-in coming soon')}>
            Process Cash-In
          </Button>
        </div>
      </Card>

      <Card title="Cash-Out">
        <div className="space-y-4">
          <Input
            label="Customer Phone"
            placeholder="+959XXXXXXXX"
          />
          <Input
            label="Amount (MMK)"
            type="number"
            placeholder="0"
          />
          <Button variant="secondary" className="w-full" onClick={() => alert('Cash-out coming soon')}>
            Process Cash-Out
          </Button>
        </div>
      </Card>
    </div>
  );
}
