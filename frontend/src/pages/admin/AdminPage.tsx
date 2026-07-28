import { useEffect, useState } from 'react';
import { adminApi } from '../../services/api';
import { Card } from '../../components/cards/Card';

export function AdminPage() {
  const [metrics, setMetrics] = useState<Record<string, unknown>>({});

  useEffect(() => {
    adminApi.getDashboard().then(setMetrics).catch(console.error);
  }, []);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Admin Dashboard</h1>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <p className="text-sm text-gray-500">Total Transactions</p>
          <p className="text-2xl font-bold text-gray-900">{String(metrics.totalTransactions || 0)}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">Active Users</p>
          <p className="text-2xl font-bold text-gray-900">{String(metrics.activeUsers || 0)}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">Total Volume</p>
          <p className="text-2xl font-bold text-gray-900">MMK {String(metrics.totalVolume || 0)}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">Success Rate</p>
          <p className="text-2xl font-bold text-gray-900">{String(metrics.successRate || 0)}%</p>
        </Card>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card title="KYC Pending Review">
          <p className="text-center text-gray-500 py-8">No pending KYC requests</p>
        </Card>

        <Card title="AML Alerts">
          <p className="text-center text-gray-500 py-8">No active alerts</p>
        </Card>
      </div>
    </div>
  );
}
