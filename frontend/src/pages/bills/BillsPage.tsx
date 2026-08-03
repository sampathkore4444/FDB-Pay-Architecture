import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { billApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import type { Biller } from '../../types';

export function BillsPage() {
  const user = useAuthStore((s) => s.user);
  const [categories, setCategories] = useState<{ id: string; name: string }[]>([]);
  const [billers, setBillers] = useState<Biller[]>([]);
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedBiller, setSelectedBiller] = useState('');
  const [accountNumber, setAccountNumber] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    billApi.getCategories().then(setCategories).catch(console.error);
  }, []);

  useEffect(() => {
    if (selectedCategory) {
      billApi.getBillers(selectedCategory).then(setBillers).catch(console.error);
    }
  }, [selectedCategory]);

  const handlePay = async () => {
    if (!user || !selectedBiller || !accountNumber) return;
    setLoading(true);
    try {
      await billApi.payBill(user.id, {
        billerId: selectedBiller,
        accountNumber,
        amount: 10000,
      });
      toast.success('Bill payment successful!');
    } catch (err) {
      toast.error('Payment failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Pay Bills</h1>

      <Card title="Select Biller">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
            >
              <option value="">Select category</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
              ))}
            </select>
          </div>

          {billers.length > 0 && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Biller</label>
              <select
                value={selectedBiller}
                onChange={(e) => setSelectedBiller(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
              >
                <option value="">Select biller</option>
                {billers.map((biller) => (
                  <option key={biller.id} value={biller.id}>{biller.name}</option>
                ))}
              </select>
            </div>
          )}

          <Input
            label="Account Number"
            value={accountNumber}
            onChange={(e) => setAccountNumber(e.target.value)}
            placeholder="Enter your account number"
          />

          <Button onClick={handlePay} loading={loading} className="w-full">
            Pay Bill
          </Button>
        </div>
      </Card>
    </div>
  );
}
