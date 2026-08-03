import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { requestMoneyApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatDate, cn, copyToClipboard, getApiErrorMessage } from '../../utils';
import type { MoneyRequest } from '../../types';

export function RequestMoneyPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [tab, setTab] = useState<'sent' | 'received'>('sent');
  const [sentRequests, setSentRequests] = useState<MoneyRequest[]>([]);
  const [receivedRequests, setReceivedRequests] = useState<MoneyRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [showLink, setShowLink] = useState<MoneyRequest | null>(null);
  const [copied, setCopied] = useState(false);

  const [targetPhone, setTargetPhone] = useState('');
  const [amount, setAmount] = useState<number>(0);
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadRequests = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const [sent, received] = await Promise.all([
        requestMoneyApi.getMyRequests(user.id),
        user.phone ? requestMoneyApi.getReceivedRequests(user.phone) : Promise.resolve([]),
      ]);
      setSentRequests(sent);
      setReceivedRequests(received);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
  }, [user]);

  const handleCreate = async () => {
    if (!user || !targetPhone || !amount) return;
    setSubmitting(true);
    try {
      const result = await requestMoneyApi.createRequest(user.id, {
        targetPhone,
        amount,
        description: description || undefined,
      });
      setShowCreate(false);
      setTargetPhone('');
      setAmount(0);
      setDescription('');
      setShowLink(result);
      await loadRequests();
    } catch (err) {
      toast.error(getApiErrorMessage(err, t.requestMoney.createFailed));
    } finally {
      setSubmitting(false);
    }
  };

  const handleRespond = async (requestId: string, action: 'ACCEPT' | 'CANCEL') => {
    if (!user) return;
    setSubmitting(true);
    try {
      await requestMoneyApi.respondToRequest(user.id, requestId, action);
      toast.success(action === 'ACCEPT' ? t.requestMoney.acceptSuccess : t.requestMoney.cancelSuccess);
      await loadRequests();
    } catch (err) {
      toast.error(getApiErrorMessage(err, t.requestMoney.actionFailed));
    } finally {
      setSubmitting(false);
    }
  };

  const handleCopyLink = async (link: string) => {
    try {
      await copyToClipboard(link);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      toast.error(t.requestMoney.copyFailed);
    }
  };

  const statusColor = (s: string) => {
    const m: Record<string, string> = {
      PENDING: 'bg-yellow-100 text-yellow-800',
      ACCEPTED: 'bg-green-100 text-green-800',
      EXPIRED: 'bg-gray-100 text-gray-600',
      CANCELLED: 'bg-red-100 text-red-800',
    };
    return m[s] || 'bg-gray-100 text-gray-800';
  };

  const displayRequests = tab === 'sent' ? sentRequests : receivedRequests;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t.requestMoney.title}</h1>
        <Button onClick={() => setShowCreate(true)}>{t.requestMoney.createRequest}</Button>
      </div>

      <div className="flex space-x-1 bg-gray-100 rounded-lg p-1 w-fit">
        <button
          onClick={() => setTab('sent')}
          className={cn('px-4 py-2 text-sm font-medium rounded-md transition-colors',
            tab === 'sent' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600 hover:text-gray-900'
          )}
        >
          {t.requestMoney.sentRequests}
        </button>
        <button
          onClick={() => setTab('received')}
          className={cn('px-4 py-2 text-sm font-medium rounded-md transition-colors',
            tab === 'received' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600 hover:text-gray-900'
          )}
        >
          {t.requestMoney.receivedRequests}
        </button>
      </div>

      {loading ? (
        <div className="text-center py-8">{t.common.loading}</div>
      ) : displayRequests.length === 0 ? (
        <Card>
          <p className="text-center text-gray-500 py-8">
            {tab === 'sent' ? t.requestMoney.noSent : t.requestMoney.noReceived}
          </p>
        </Card>
      ) : (
        <div className="space-y-2">
          {displayRequests.map((req) => (
            <div key={req.id} className="bg-white border border-gray-200 rounded-xl p-4 hover:bg-gray-50 transition-colors">
              <div className="flex items-center justify-between">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="text-sm font-medium text-gray-900">{req.targetPhone}</span>
                    <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(req.status))}>
                      {t.requestMoney.status[req.status as keyof typeof t.requestMoney.status]}
                    </span>
                  </div>
                  <p className="text-sm text-gray-500">{req.amount.toLocaleString()} MMK</p>
                  {req.description && <p className="text-xs text-gray-400">{req.description}</p>}
                  <p className="text-xs text-gray-400">{formatDate(req.createdAt)}</p>
                </div>
                <div className="flex space-x-2">
                  {tab === 'sent' && req.status === 'PENDING' && (
                    <>
                      <Button size="sm" variant="ghost" onClick={() => setShowLink(req)}>
                        {t.requestMoney.copyLink}
                      </Button>
                      <Button size="sm" variant="ghost" onClick={() => handleRespond(req.id, 'CANCEL')}>
                        {t.requestMoney.cancelRequest}
                      </Button>
                    </>
                  )}
                  {tab === 'received' && req.status === 'PENDING' && (
                    <>
                      <Button size="sm" onClick={() => handleRespond(req.id, 'ACCEPT')}>
                        {t.requestMoney.accept}
                      </Button>
                      <Button size="sm" variant="ghost" onClick={() => handleRespond(req.id, 'CANCEL')}>
                        {t.requestMoney.cancelRequest}
                      </Button>
                    </>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title={t.requestMoney.createRequest}>
        <div className="space-y-4">
          <Input
            label={t.requestMoney.targetPhone}
            placeholder={t.requestMoney.targetPhonePlaceholder}
            value={targetPhone}
            onChange={(e) => setTargetPhone(e.target.value)}
          />
          <Input
            label={t.requestMoney.amount}
            type="number"
            placeholder={t.requestMoney.amountPlaceholder}
            value={amount || ''}
            onChange={(e) => setAmount(Number(e.target.value))}
          />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.requestMoney.description}</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              rows={3}
              placeholder={t.requestMoney.descriptionPlaceholder}
            />
          </div>
          <div className="flex space-x-3">
            <Button onClick={handleCreate} loading={submitting} className="flex-1">{t.requestMoney.createRequest}</Button>
            <Button variant="secondary" onClick={() => setShowCreate(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!showLink} onClose={() => setShowLink(null)} title={t.requestMoney.paymentLink}>
        {showLink && (
          <div className="space-y-4">
            <div className="bg-gray-50 rounded-lg p-3 text-sm text-gray-700 break-all">
              {showLink.paymentLink}
            </div>
            <Button onClick={() => handleCopyLink(showLink.paymentLink)} className="w-full">
              {copied ? t.requestMoney.linkCopied : t.requestMoney.copyLink}
            </Button>
          </div>
        )}
      </Modal>
    </div>
  );
}
