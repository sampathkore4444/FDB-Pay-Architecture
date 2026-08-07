import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { accountingApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { formatCurrency } from '../../utils';
import type { AccountingExport } from '../../types';

export function AccountingPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [rows, setRows] = useState<AccountingExport[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    setLoading(true);
    accountingApi.export(user.id)
      .then(setRows)
      .catch((err) => {
        console.error('Failed to load accounting export', err);
        toast.error(t.common.loadFailed);
      })
      .finally(() => setLoading(false));
  }, [user]);

  const total = rows.reduce((sum, r) => sum + r.amount, 0);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t.accounting.title}</h1>
        <p className="text-sm text-gray-500 mt-1">{t.accounting.subtitle}</p>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : rows.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.accounting.noData}</p></Card>
      ) : (
        <Card title={t.accounting.exportTitle}>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                  <th className="pb-2 pr-4">{t.accounting.accountCode}</th>
                  <th className="pb-2 pr-4">{t.accounting.description}</th>
                  <th className="pb-2">{t.accounting.amount}</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row, i) => (
                  <tr key={i} className="border-b border-gray-100">
                    <td className="py-2 pr-4 font-mono text-xs font-semibold text-blue-700">{row.accountCode}</td>
                    <td className="py-2 pr-4 text-gray-900">{row.description}</td>
                    <td className={`py-2 font-semibold ${row.amount < 0 ? 'text-red-600' : 'text-gray-900'}`}>{formatCurrency(row.amount)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="border-t border-gray-200">
                  <td className="py-2 pr-4" colSpan={2} />
                  <td className="py-2 font-bold text-gray-900">{formatCurrency(total)}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
}
