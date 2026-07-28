import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { auditApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatDate } from '../../utils';

interface AuditEntry {
  id: string;
  actorId: string;
  actorName?: string;
  action: string;
  resourceType: string;
  resourceId: string;
  details?: string;
  timestamp: string;
}

interface AuditSummary {
  totalEvents: number;
  uniqueActors: number;
  topActions: { action: string; count: number }[];
}

export function AuditLogPage() {
  const { t } = useTranslation();
  const [entries, setEntries] = useState<AuditEntry[]>([]);
  const [summary, setSummary] = useState<AuditSummary | null>(null);
  const [loading, setLoading] = useState(true);

  const [actorFilter, setActorFilter] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [resourceFilter, setResourceFilter] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const loadData = async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = {};
      if (actorFilter) params.actor = actorFilter;
      if (actionFilter) params.action = actionFilter;
      if (resourceFilter) params.resource = resourceFilter;
      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;

      const [logData, summaryData] = await Promise.all([
        auditApi.getAuditLog(params),
        auditApi.getSummary(),
      ]);
      setEntries(logData);
      setSummary(summaryData);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleExport = async (format: 'csv' | 'json') => {
    try {
      const data = await auditApi.exportLog(format);
      const blob = new Blob([typeof data === 'string' ? data : JSON.stringify(data)], {
        type: format === 'csv' ? 'text/csv' : 'application/json',
      });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `audit-log.${format}`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
    }
  };

  if (loading) return <div className="text-center py-8">{t.common.loading}</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t.audit.title}</h1>
        <div className="flex space-x-2">
          <Button variant="secondary" size="sm" onClick={() => handleExport('csv')}>{t.audit.exportCsv}</Button>
          <Button variant="secondary" size="sm" onClick={() => handleExport('json')}>{t.audit.exportJson}</Button>
        </div>
      </div>

      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card>
            <p className="text-sm text-gray-500">{t.audit.totalEvents}</p>
            <p className="text-2xl font-bold text-gray-900">{summary.totalEvents}</p>
          </Card>
          <Card>
            <p className="text-sm text-gray-500">{t.audit.uniqueActors}</p>
            <p className="text-2xl font-bold text-blue-600">{summary.uniqueActors}</p>
          </Card>
          <Card title={t.audit.topActions}>
            <div className="space-y-1">
              {summary.topActions.slice(0, 3).map((a) => (
                <div key={a.action} className="flex justify-between text-sm">
                  <span className="text-gray-700">{a.action}</span>
                  <span className="font-medium">{a.count}</span>
                </div>
              ))}
            </div>
          </Card>
        </div>
      )}

      <Card>
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-3">
            <Input
              label={t.audit.searchByActor}
              value={actorFilter}
              onChange={(e) => setActorFilter(e.target.value)}
              placeholder={t.audit.searchByActor}
            />
            <Input
              label={t.audit.searchByAction}
              value={actionFilter}
              onChange={(e) => setActionFilter(e.target.value)}
              placeholder={t.audit.searchByAction}
            />
            <Input
              label={t.audit.searchByResource}
              value={resourceFilter}
              onChange={(e) => setResourceFilter(e.target.value)}
              placeholder={t.audit.searchByResource}
            />
            <Input
              label={t.audit.startDate}
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
            <Input
              label={t.audit.endDate}
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
            />
          </div>
          <Button onClick={loadData} size="sm">{t.common.search}</Button>
        </div>
      </Card>

      <Card title={t.audit.log}>
        {entries.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.audit.noEntries}</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left py-3 px-4 font-medium text-gray-600">{t.audit.timestamp}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-600">{t.audit.actor}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-600">{t.audit.action}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-600">{t.audit.resourceType}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-600">{t.audit.resource}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-600">{t.audit.details}</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((entry) => (
                  <tr key={entry.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-3 px-4 text-gray-500 whitespace-nowrap">{formatDate(entry.timestamp)}</td>
                    <td className="py-3 px-4 font-medium text-gray-900">{entry.actorName || entry.actorId.slice(0, 8)}</td>
                    <td className="py-3 px-4">
                      <span className="bg-blue-100 text-blue-800 text-xs px-2 py-0.5 rounded-full">{entry.action}</span>
                    </td>
                    <td className="py-3 px-4 text-gray-700">{entry.resourceType}</td>
                    <td className="py-3 px-4 text-gray-500">{entry.resourceId.slice(0, 8)}</td>
                    <td className="py-3 px-4 text-gray-500 max-w-xs truncate">{entry.details || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
