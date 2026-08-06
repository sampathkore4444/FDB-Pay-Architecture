import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { merchantAuditApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import type { MerchantAuditLogEntry } from '../../types';

export function MerchantAuditLogPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [entries, setEntries] = useState<MerchantAuditLogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    setLoading(true);
    merchantAuditApi
      .list(user.id)
      .then(setEntries)
      .catch((err) => {
        console.error('Failed to load audit log', err);
        toast.error(t.common.loadFailed);
      })
      .finally(() => setLoading(false));
  }, [user]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t.auditLog.title}</h1>
        <p className="text-sm text-gray-500 mt-1">{t.auditLog.subtitle}</p>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : entries.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.auditLog.noEntries}</p></Card>
      ) : (
        <Card>
          <div className="space-y-2">
            {entries.map((entry) => (
              <div key={entry.id} className="border border-gray-100 rounded-lg p-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <span className="px-2 py-0.5 rounded text-xs font-medium bg-emerald-100 text-emerald-700">{entry.action}</span>
                    <span className="text-sm font-medium text-gray-800">{entry.entity}</span>
                    {entry.actorName && <span className="text-xs text-gray-400">· {entry.actorName}</span>}
                  </div>
                  <span className="text-xs text-gray-400">{new Date(entry.createdAt).toLocaleString()}</span>
                </div>
                {entry.details && (
                  <button className="mt-1 text-xs text-emerald-600 hover:underline" onClick={() => setExpanded(expanded === entry.id ? null : entry.id)}>
                    {expanded === entry.id ? t.auditLog.hideDetails : t.auditLog.showDetails}
                  </button>
                )}
                {expanded === entry.id && entry.details && (
                  <pre className="mt-2 text-xs text-gray-600 bg-gray-50 rounded p-2 overflow-x-auto">{entry.details}</pre>
                )}
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
