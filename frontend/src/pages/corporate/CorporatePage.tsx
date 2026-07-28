import { useRef, useState } from 'react';
import { useAuthStore } from '../../store/authStore';
import { corporateApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency, formatDate } from '../../utils';

interface Disbursement {
  id: string;
  batchId: string;
  status: string;
  totalAmount: number;
  totalRecipients: number;
  processedCount: number;
  failedCount: number;
  createdAt: string;
}

export function CorporatePage() {
  const user = useAuthStore((s) => s.user);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [disbursements, setDisbursements] = useState<Disbursement[]>([]);
  const [description, setDescription] = useState('');
  const [uploading, setUploading] = useState(false);
  const [reconPeriod, setReconPeriod] = useState('');
  const [reconResult, setReconResult] = useState<{ period: string; totalTransactions: number; totalAmount: number; discrepancies: number; status: string } | null>(null);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !user) return;
    setUploading(true);
    try {
      const result = await corporateApi.bulkDisburse(user.id, {
        fileRef: file.name,
        description: description || `Bulk disbursement - ${file.name}`,
      });
      const newEntry: Disbursement = {
        id: result.id,
        batchId: result.batchId,
        status: 'PENDING',
        totalAmount: 0,
        totalRecipients: 0,
        processedCount: 0,
        failedCount: 0,
        createdAt: new Date().toISOString(),
      };
      setDisbursements([newEntry, ...disbursements]);
      alert(`Bulk disbursement initiated. Batch ID: ${result.batchId}`);
      setDescription('');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Upload failed';
      alert(msg);
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleReconciliation = async () => {
    if (!user || !reconPeriod) return;
    try {
      const result = await corporateApi.getReconciliation(user.id, reconPeriod);
      setReconResult(result);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch reconciliation';
      alert(msg);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Corporate Portal</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card title="Bulk Disbursement">
          <p className="text-gray-500 mb-4">Upload a CSV file to disburse salaries or payments to multiple wallets.</p>
          <Input
            label="Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="e.g. July 2026 payroll"
          />
          <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center mt-4">
            <p className="text-gray-400">Drag & drop CSV file here</p>
            <p className="text-sm text-gray-400 mt-1">or</p>
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv"
              className="hidden"
              onChange={handleFileUpload}
            />
            <Button
              variant="secondary"
              className="mt-3"
              onClick={() => fileInputRef.current?.click()}
              disabled={uploading}
            >
              {uploading ? 'Uploading...' : 'Choose File'}
            </Button>
          </div>
        </Card>

        <Card title="Reconciliation">
          <p className="text-gray-500 mb-4">Download reconciliation reports for your transactions.</p>
          <Input
            label="Period (e.g. 2026-07)"
            value={reconPeriod}
            onChange={(e) => setReconPeriod(e.target.value)}
            placeholder="YYYY-MM"
          />
          <Button variant="secondary" className="mt-4" onClick={handleReconciliation} disabled={!reconPeriod}>
            Fetch Report
          </Button>
          {reconResult && (
            <div className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">Total Transactions:</span>
                <span className="font-medium">{reconResult.totalTransactions}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Total Amount:</span>
                <span className="font-medium">{formatCurrency(reconResult.totalAmount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Discrepancies:</span>
                <span className={`font-medium ${reconResult.discrepancies > 0 ? 'text-red-600' : 'text-green-600'}`}>
                  {reconResult.discrepancies}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Status:</span>
                <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                  reconResult.status === 'RECONCILED' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                }`}>{reconResult.status}</span>
              </div>
            </div>
          )}
        </Card>
      </div>

      {disbursements.length > 0 && (
        <Card title="Recent Disbursements">
          <div className="space-y-2">
            {disbursements.map((d) => (
              <div key={d.id} className="flex justify-between items-center text-sm p-2 bg-gray-50 rounded">
                <div>
                  <span className="font-medium">{d.batchId}</span>
                  <span className="text-gray-500 ml-2">{formatDate(d.createdAt)}</span>
                </div>
                <div className="text-right">
                  <span className="font-medium">{formatCurrency(d.totalAmount)}</span>
                  <span className={`ml-2 px-2 py-0.5 rounded text-xs font-medium ${
                    d.status === 'COMPLETED' ? 'bg-green-100 text-green-700' :
                    d.status === 'PROCESSING' ? 'bg-blue-100 text-blue-700' :
                    d.status === 'FAILED' ? 'bg-red-100 text-red-700' :
                    'bg-yellow-100 text-yellow-700'
                  }`}>{d.status}</span>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
