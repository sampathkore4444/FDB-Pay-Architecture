import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { payrollApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency, formatDate, cn } from '../../utils';

interface PayrollEmployee {
  name: string;
  phone: string;
  salary: number;
}

interface PayrollRun {
  id: string;
  status: string;
  totalAmount: number;
  employeeCount: number;
  payDate: string;
  createdAt: string;
  rejectionReason?: string;
  employees?: PayrollEmployee[];
}

export function PayrollPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [runs, setRuns] = useState<PayrollRun[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [showDetail, setShowDetail] = useState<PayrollRun | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [employees, setEmployees] = useState<PayrollEmployee[]>([]);
  const [empName, setEmpName] = useState('');
  const [empPhone, setEmpPhone] = useState('');
  const [empSalary, setEmpSalary] = useState<number>(0);
  const [payDate, setPayDate] = useState('');

  const loadRuns = async () => {
    setLoading(true);
    try {
      const data = await payrollApi.getPayrollRun();
      setRuns(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRuns();
  }, []);

  const handleAddEmployee = () => {
    if (!empName || !empPhone || !empSalary) return;
    setEmployees((prev) => [...prev, { name: empName, phone: empPhone, salary: empSalary }]);
    setEmpName('');
    setEmpPhone('');
    setEmpSalary(0);
  };

  const handleRemoveEmployee = (idx: number) => {
    setEmployees((prev) => prev.filter((_, i) => i !== idx));
  };

  const handleSubmit = async () => {
    if (!user || employees.length === 0 || !payDate) return;
    setSubmitting(true);
    try {
      await payrollApi.createPayrollRun(user.id, { employees, payDate });
      await payrollApi.submitPayroll(user.id);
      setShowCreate(false);
      setEmployees([]);
      setPayDate('');
      await loadRuns();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleApprove = async (id: string) => {
    if (!user) return;
    try {
      await payrollApi.approvePayroll(user.id, id);
      await loadRuns();
    } catch (err) {
      console.error(err);
    }
  };

  const handleReject = async (id: string) => {
    if (!user) return;
    try {
      await payrollApi.rejectPayroll(user.id, id);
      await loadRuns();
    } catch (err) {
      console.error(err);
    }
  };

  const totalAmount = employees.reduce((s, e) => s + e.salary, 0);

  const statusColor = (s: string) => {
    const m: Record<string, string> = {
      DRAFT: 'bg-gray-100 text-gray-800',
      PENDING_APPROVAL: 'bg-yellow-100 text-yellow-800',
      APPROVED: 'bg-green-100 text-green-800',
      REJECTED: 'bg-red-100 text-red-800',
      PROCESSED: 'bg-blue-100 text-blue-800',
    };
    return m[s] || 'bg-gray-100 text-gray-800';
  };

  if (loading) return <div className="text-center py-8">{t.common.loading}</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t.payroll.title}</h1>
        <Button onClick={() => setShowCreate(true)}>{t.payroll.createRun}</Button>
      </div>

      {runs.length === 0 ? (
        <Card>
          <p className="text-center text-gray-500 py-8">{t.payroll.noRuns}</p>
        </Card>
      ) : (
        <div className="space-y-2">
          {runs.map((run) => (
            <div
              key={run.id}
              className="bg-white border border-gray-200 rounded-xl p-4 hover:bg-gray-50 transition-colors cursor-pointer"
              onClick={() => setShowDetail(run)}
            >
              <div className="flex items-center justify-between">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="text-sm font-medium text-gray-900">{run.id.slice(0, 8)}</span>
                    <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(run.status))}>{run.status}</span>
                  </div>
                  <div className="flex items-center space-x-3 text-xs text-gray-500">
                    <span>{run.employeeCount} {t.payroll.employees}</span>
                    <span>{formatCurrency(run.totalAmount)}</span>
                    <span>{t.payroll.payDate}: {formatDate(run.payDate)}</span>
                  </div>
                  <p className="text-xs text-gray-400">{formatDate(run.createdAt)}</p>
                </div>
                <div className="flex space-x-1" onClick={(e) => e.stopPropagation()}>
                  {run.status === 'PENDING_APPROVAL' && (
                    <>
                      <Button size="sm" onClick={() => handleApprove(run.id)}>{t.payroll.approve}</Button>
                      <Button size="sm" variant="danger" onClick={() => handleReject(run.id)}>{t.payroll.reject}</Button>
                    </>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title={t.payroll.createRun}>
        <div className="space-y-4">
          <Input label={t.payroll.payDate} type="date" value={payDate} onChange={(e) => setPayDate(e.target.value)} />

          <div className="border border-gray-200 rounded-lg p-3 space-y-3">
            <h4 className="text-sm font-medium text-gray-700">{t.payroll.addEmployee}</h4>
            <div className="grid grid-cols-3 gap-2">
              <Input label={t.payroll.employeeName} value={empName} onChange={(e) => setEmpName(e.target.value)} />
              <Input label={t.payroll.employeePhone} value={empPhone} onChange={(e) => setEmpPhone(e.target.value)} />
              <Input label={t.payroll.salary} type="number" value={empSalary || ''} onChange={(e) => setEmpSalary(Number(e.target.value))} />
            </div>
            <Button size="sm" variant="secondary" onClick={handleAddEmployee}>{t.payroll.addEmployee}</Button>
          </div>

          {employees.length > 0 && (
            <div className="space-y-2">
              <div className="flex justify-between text-sm font-medium text-gray-700">
                <span>{t.payroll.employees} ({employees.length})</span>
                <span>{t.payroll.totalAmount}: {formatCurrency(totalAmount)}</span>
              </div>
              <div className="max-h-40 overflow-y-auto space-y-1">
                {employees.map((emp, idx) => (
                  <div key={idx} className="flex items-center justify-between p-2 bg-gray-50 rounded text-sm">
                    <span>{emp.name} - {emp.phone}</span>
                    <div className="flex items-center space-x-2">
                      <span className="font-medium">{formatCurrency(emp.salary)}</span>
                      <button onClick={() => handleRemoveEmployee(idx)} className="text-red-500 hover:text-red-700 text-xs">
                        ×
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div className="flex space-x-3">
            <Button onClick={handleSubmit} loading={submitting} className="flex-1" disabled={employees.length === 0 || !payDate}>
              {t.payroll.submitForApproval}
            </Button>
            <Button variant="secondary" onClick={() => setShowCreate(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!showDetail} onClose={() => setShowDetail(null)} title={`${t.payroll.title} - ${t.common.details}`}>
        {showDetail && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-500">{t.common.status}</p>
                <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(showDetail.status))}>{showDetail.status}</span>
              </div>
              <div>
                <p className="text-gray-500">{t.payroll.totalAmount}</p>
                <p className="font-medium">{formatCurrency(showDetail.totalAmount)}</p>
              </div>
              <div>
                <p className="text-gray-500">{t.payroll.payDate}</p>
                <p className="font-medium">{formatDate(showDetail.payDate)}</p>
              </div>
              <div>
                <p className="text-gray-500">{t.payroll.employees}</p>
                <p className="font-medium">{showDetail.employeeCount}</p>
              </div>
            </div>
            {showDetail.rejectionReason && (
              <div className="bg-red-50 rounded-lg p-3">
                <p className="text-sm font-medium text-red-800">{t.payroll.rejectionReason}:</p>
                <p className="text-sm text-red-700">{showDetail.rejectionReason}</p>
              </div>
            )}
            {showDetail.employees && showDetail.employees.length > 0 && (
              <div>
                <h4 className="font-medium text-gray-900 mb-2">{t.payroll.employees}</h4>
                <div className="space-y-1 max-h-40 overflow-y-auto">
                  {showDetail.employees.map((emp, idx) => (
                    <div key={idx} className="flex justify-between p-2 bg-gray-50 rounded text-sm">
                      <span>{emp.name} ({emp.phone})</span>
                      <span className="font-medium">{formatCurrency(emp.salary)}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
