import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { disputeApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatDate } from '../../utils';
import { cn } from '../../utils';

interface Dispute {
  id: string;
  transactionId: string;
  type: string;
  amount: number;
  description: string;
  status: string;
  evidenceList: { id: string; description: string; createdAt: string }[];
  createdAt: string;
  resolvedAt?: string;
}

interface DisputeStats {
  totalOpen: number;
  totalResolved: number;
  avgResolutionDays: number;
}

export function DisputesPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [tab, setTab] = useState<'my' | 'all'>('my');
  const [disputes, setDisputes] = useState<Dispute[]>([]);
  const [stats, setStats] = useState<DisputeStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [showDetail, setShowDetail] = useState<Dispute | null>(null);
  const [showEvidence, setShowEvidence] = useState<string | null>(null);
  const [showResolve, setShowResolve] = useState<string | null>(null);

  const [txnId, setTxnId] = useState('');
  const [disputeType, setDisputeType] = useState('');
  const [disputeAmount, setDisputeAmount] = useState<number>(0);
  const [disputeDesc, setDisputeDesc] = useState('');
  const [evidenceDesc, setEvidenceDesc] = useState('');
  const [resolveAction, setResolveAction] = useState('refund');
  const [resolveNotes, setResolveNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadDisputes = async () => {
    setLoading(true);
    try {
      if (tab === 'my' && user) {
        const data = await disputeApi.getMyDisputes(user.id);
        setDisputes(data);
      } else if (tab === 'all') {
        const data = await disputeApi.getAllDisputes();
        setDisputes(data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadStats = async () => {
    try {
      const data = await disputeApi.getStats();
      setStats(data);
    } catch {}
  };

  useEffect(() => {
    loadDisputes();
    loadStats();
  }, [tab, user]);

  const handleCreate = async () => {
    if (!user || !txnId || !disputeType) return;
    setSubmitting(true);
    try {
      await disputeApi.create(user.id, {
        transactionId: txnId,
        type: disputeType,
        amount: disputeAmount,
        description: disputeDesc,
      });
      setShowCreate(false);
      setTxnId('');
      setDisputeType('');
      setDisputeAmount(0);
      setDisputeDesc('');
      await loadDisputes();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleAddEvidence = async () => {
    if (!showEvidence || !evidenceDesc) return;
    setSubmitting(true);
    try {
      await disputeApi.addEvidence(showEvidence, { description: evidenceDesc });
      setShowEvidence(null);
      setEvidenceDesc('');
      await loadDisputes();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleResolve = async () => {
    if (!showResolve) return;
    setSubmitting(true);
    try {
      await disputeApi.resolve(showResolve, { action: resolveAction, notes: resolveNotes });
      setShowResolve(null);
      setResolveAction('refund');
      setResolveNotes('');
      await loadDisputes();
      await loadStats();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const statusColor = (s: string) => {
    const m: Record<string, string> = {
      OPEN: 'bg-yellow-100 text-yellow-800',
      RESOLVED: 'bg-green-100 text-green-800',
      DISMISSED: 'bg-gray-100 text-gray-800',
      PENDING: 'bg-blue-100 text-blue-800',
    };
    return m[s] || 'bg-gray-100 text-gray-800';
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.dispute.title}</h1>

      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card>
            <p className="text-sm text-gray-500">{t.dispute.totalOpen}</p>
            <p className="text-2xl font-bold text-yellow-600">{stats.totalOpen}</p>
          </Card>
          <Card>
            <p className="text-sm text-gray-500">{t.dispute.totalResolved}</p>
            <p className="text-2xl font-bold text-green-600">{stats.totalResolved}</p>
          </Card>
          <Card>
            <p className="text-sm text-gray-500">{t.dispute.avgResolutionTime}</p>
            <p className="text-2xl font-bold text-blue-600">{stats.avgResolutionDays} {t.dispute.days}</p>
          </Card>
        </div>
      )}

      <div className="flex items-center justify-between">
        <div className="flex space-x-1 bg-gray-100 rounded-lg p-1">
          <button
            onClick={() => setTab('my')}
            className={cn('px-4 py-2 text-sm font-medium rounded-md transition-colors',
              tab === 'my' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600 hover:text-gray-900'
            )}
          >
            {t.dispute.myDisputes}
          </button>
          <button
            onClick={() => setTab('all')}
            className={cn('px-4 py-2 text-sm font-medium rounded-md transition-colors',
              tab === 'all' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600 hover:text-gray-900'
            )}
          >
            {t.dispute.allDisputes}
          </button>
        </div>
        <Button onClick={() => setShowCreate(true)}>{t.dispute.create}</Button>
      </div>

      {loading ? (
        <div className="text-center py-8">{t.common.loading}</div>
      ) : disputes.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-8">{t.common.noData}</p></Card>
      ) : (
        <div className="space-y-2">
          {disputes.map((d) => (
            <div key={d.id} className="bg-white border border-gray-200 rounded-xl p-4 hover:bg-gray-50 transition-colors">
              <div className="flex items-center justify-between">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="text-sm font-medium text-gray-900">{t.dispute.disputeId}: {d.id.slice(0, 8)}</span>
                    <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(d.status))}>{d.status}</span>
                  </div>
                  <p className="text-sm text-gray-500">{t.dispute.transactionId}: {d.transactionId.slice(0, 8)} &middot; {t.dispute.disputeType}: {d.type}</p>
                  <p className="text-xs text-gray-400">{formatDate(d.createdAt)}</p>
                </div>
                <div className="flex space-x-2">
                  <Button size="sm" variant="ghost" onClick={() => setShowDetail(d)}>{t.common.viewDetails}</Button>
                  <Button size="sm" variant="ghost" onClick={() => setShowEvidence(d.id)}>{t.dispute.addEvidence}</Button>
                  {tab === 'all' && d.status === 'OPEN' && (
                    <Button size="sm" variant="ghost" onClick={() => setShowResolve(d.id)}>{t.dispute.resolve}</Button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title={t.dispute.create}>
        <div className="space-y-4">
          <Input label={t.dispute.transactionId} value={txnId} onChange={(e) => setTxnId(e.target.value)} placeholder="TXN-..." />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.dispute.disputeType}</label>
            <select
              value={disputeType}
              onChange={(e) => setDisputeType(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
            >
              <option value="">{t.dispute.selectTransaction}</option>
              <option value="UNAUTHORIZED">{t.dispute.disputeTypes.unauthorized}</option>
              <option value="DUPLICATE">{t.dispute.disputeTypes.duplicate}</option>
              <option value="NOT_RECEIVED">{t.dispute.disputeTypes.notReceived}</option>
              <option value="OVERCHARGE">{t.dispute.disputeTypes.incorrect}</option>
              <option value="OTHER">{t.dispute.disputeTypes.other}</option>
            </select>
          </div>
          <Input label={t.dispute.amountDisputed} type="number" value={disputeAmount || ''} onChange={(e) => setDisputeAmount(Number(e.target.value))} placeholder="0" />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.common.description}</label>
            <textarea
              value={disputeDesc}
              onChange={(e) => setDisputeDesc(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              rows={3}
              placeholder={t.dispute.disputeDescription}
            />
          </div>
          <div className="flex space-x-3">
            <Button onClick={handleCreate} loading={submitting} className="flex-1">{t.dispute.submitDispute}</Button>
            <Button variant="secondary" onClick={() => setShowCreate(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!showDetail} onClose={() => setShowDetail(null)} title={t.dispute.details}>
        {showDetail && (
          <div className="space-y-4">
            <div className="space-y-2 text-sm">
              <p><span className="font-medium">{t.dispute.disputeId}:</span> {showDetail.id}</p>
              <p><span className="font-medium">{t.dispute.transactionId}:</span> {showDetail.transactionId}</p>
              <p><span className="font-medium">{t.common.type}:</span> {showDetail.type}</p>
              <p><span className="font-medium">{t.common.amount}:</span> {showDetail.amount.toLocaleString()} MMK</p>
              <p><span className="font-medium">{t.common.status}:</span> {showDetail.status}</p>
              <p><span className="font-medium">{t.common.description}:</span> {showDetail.description}</p>
            </div>
            <div>
              <h4 className="font-medium text-gray-900 mb-2">{t.dispute.evidenceList}</h4>
              {showDetail.evidenceList.length === 0 ? (
                <p className="text-sm text-gray-500">{t.dispute.noEvidence}</p>
              ) : (
                <div className="space-y-2">
                  {showDetail.evidenceList.map((e) => (
                    <div key={e.id} className="bg-gray-50 rounded-lg p-3">
                      <p className="text-sm">{e.description}</p>
                      <p className="text-xs text-gray-400">{formatDate(e.createdAt)}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </Modal>

      <Modal open={!!showEvidence} onClose={() => setShowEvidence(null)} title={t.dispute.addEvidence}>
        <div className="space-y-4">
          <Input label={t.dispute.evidenceDescription} value={evidenceDesc} onChange={(e) => setEvidenceDesc(e.target.value)} />
          <div className="flex space-x-3">
            <Button onClick={handleAddEvidence} loading={submitting} className="flex-1">{t.common.submit}</Button>
            <Button variant="secondary" onClick={() => setShowEvidence(null)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!showResolve} onClose={() => setShowResolve(null)} title={t.dispute.resolve}>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.dispute.resolveAction}</label>
            <select
              value={resolveAction}
              onChange={(e) => setResolveAction(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
            >
              <option value="refund">{t.dispute.resolveOptions.refund}</option>
              <option value="partial">{t.dispute.resolveOptions.partial}</option>
              <option value="dismiss">{t.dispute.resolveOptions.dismiss}</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.dispute.resolutionNotes}</label>
            <textarea
              value={resolveNotes}
              onChange={(e) => setResolveNotes(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              rows={3}
            />
          </div>
          <div className="flex space-x-3">
            <Button onClick={handleResolve} loading={submitting} className="flex-1">{t.dispute.resolveDispute}</Button>
            <Button variant="secondary" onClick={() => setShowResolve(null)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
