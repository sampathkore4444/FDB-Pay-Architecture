import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';

export function CorporatePage() {
  return (
    <div className="max-w-lg mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Corporate Portal</h1>

      <Card title="Bulk Disbursement">
        <p className="text-gray-500 mb-4">Upload a CSV file to disburse salaries or payments to multiple wallets.</p>
        <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center">
          <p className="text-gray-400">Drag & drop CSV file here</p>
          <p className="text-sm text-gray-400 mt-1">or</p>
          <Button variant="secondary" className="mt-3" onClick={() => alert('File upload coming soon')}>
            Choose File
          </Button>
        </div>
      </Card>

      <Card title="Reconciliation">
        <p className="text-gray-500 mb-4">Download reconciliation reports for your transactions.</p>
        <Button variant="secondary" onClick={() => alert('Download coming soon')}>
          Download Report
        </Button>
      </Card>
    </div>
  );
}
