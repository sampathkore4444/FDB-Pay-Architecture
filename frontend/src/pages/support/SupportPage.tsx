import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { supportApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatDate, cn } from '../../utils';
import type { SupportTicket, SupportStats, TicketMessage } from '../../types';

export function SupportPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [tab, setTab] = useState<'my' | 'manager'>('my');
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [stats, setStats] = useState<SupportStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [showDetail, setShowDetail] = useState<SupportTicket | null>(null);
  const [messages, setMessages] = useState<TicketMessage[]>([]);
  const [submitting, setSubmitting] = useState(false);

  const [subject, setSubject] = useState('');
  const [category, setCategory] = useState('OTHER');
  const [priority, setPriority] = useState('MEDIUM');
  const [message, setMessage] = useState('');
  const [replyMessage, setReplyMessage] = useState('');

  const loadData = async () => {
    setLoading(true);
    try {
      const [tData, sData] = await Promise.all([
        user ? supportApi.getMyTickets(user.id) : Promise.resolve([]),
        supportApi.getStats(),
      ]);
      setTickets(tData);
      setStats(sData);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user]);

  const handleCreate = async () => {
    if (!user || !subject || !message) return;
    setSubmitting(true);
    try {
      await supportApi.createTicket(user.id, { subject, category, priority, message });
      setShowCreate(false);
      setSubject('');
      setCategory('OTHER');
      setPriority('MEDIUM');
      setMessage('');
      await loadData();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleReply = async () => {
    if (!user || !showDetail || !replyMessage) return;
    setSubmitting(true);
    try {
      await supportApi.addMessage(user.id, showDetail.id, replyMessage);
      setReplyMessage('');
      const [updated, updatedMessages] = await Promise.all([
        supportApi.getTicket(showDetail.id),
        supportApi.getMessages(showDetail.id),
      ]);
      setShowDetail(updated);
      setMessages(updatedMessages);
      await loadData();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const openDetail = async (ticket: SupportTicket) => {
    setShowDetail(ticket);
    setMessages([]);
    try {
      setMessages(await supportApi.getMessages(ticket.id));
    } catch (err) {
      console.error(err);
    }
  };

  const handleResolve = async (ticketId: string) => {
    if (!user) return;
    setSubmitting(true);
    try {
      await supportApi.resolve(user.id, ticketId);
      setShowDetail(null);
      await loadData();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEscalate = async (ticketId: string) => {
    if (!user) return;
    setSubmitting(true);
    try {
      await supportApi.escalate(user.id, ticketId);
      setShowDetail(null);
      await loadData();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const statusColor = (s: string) => {
    const m: Record<string, string> = {
      OPEN: 'bg-yellow-100 text-yellow-800',
      IN_PROGRESS: 'bg-blue-100 text-blue-800',
      WAITING_CUSTOMER: 'bg-purple-100 text-purple-800',
      WAITING_INTERNAL: 'bg-indigo-100 text-indigo-800',
      RESOLVED: 'bg-green-100 text-green-800',
      ESCALATED: 'bg-red-100 text-red-800',
      CLOSED: 'bg-gray-100 text-gray-600',
    };
    return m[s] || 'bg-gray-100 text-gray-800';
  };

  const priorityColor = (p: string) => {
    const m: Record<string, string> = {
      LOW: 'bg-gray-100 text-gray-600',
      MEDIUM: 'bg-blue-100 text-blue-700',
      HIGH: 'bg-orange-100 text-orange-700',
      URGENT: 'bg-red-100 text-red-700',
    };
    return m[p] || 'bg-gray-100 text-gray-600';
  };

  const displayTickets = tab === 'my' ? tickets : tickets;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.support.title}</h1>

      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card>
            <p className="text-sm text-gray-500">{t.support.totalOpen}</p>
            <p className="text-2xl font-bold text-yellow-600">{stats.totalOpen}</p>
          </Card>
          <Card>
            <p className="text-sm text-gray-500">{t.support.totalResolved}</p>
            <p className="text-2xl font-bold text-green-600">{stats.totalResolved}</p>
          </Card>
          <Card>
            <p className="text-sm text-gray-500">{t.support.avgResponseTime}</p>
            <p className="text-2xl font-bold text-blue-600">{stats.avgResponseTimeHours} {t.support.hours}</p>
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
            {t.support.myTickets}
          </button>
          <button
            onClick={() => setTab('manager')}
            className={cn('px-4 py-2 text-sm font-medium rounded-md transition-colors',
              tab === 'manager' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600 hover:text-gray-900'
            )}
          >
            {t.support.managerView}
          </button>
        </div>
        <Button onClick={() => setShowCreate(true)}>{t.support.createTicket}</Button>
      </div>

      {loading ? (
        <div className="text-center py-8">{t.common.loading}</div>
      ) : displayTickets.length === 0 ? (
        <Card>
          <p className="text-center text-gray-500 py-8">{t.support.noTickets}</p>
        </Card>
      ) : (
        <div className="space-y-2">
          {displayTickets.map((ticket) => (
            <div key={ticket.id} className="bg-white border border-gray-200 rounded-xl p-4 hover:bg-gray-50 transition-colors">
              <div className="flex items-center justify-between">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="text-sm font-medium text-gray-900">{ticket.subject}</span>
                    <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(ticket.status))}>
                      {t.support.status[ticket.status as keyof typeof t.support.status]}
                    </span>
                    <span className={cn('text-xs px-2 py-0.5 rounded-full', priorityColor(ticket.priority))}>
                      {t.support.priorities[ticket.priority as keyof typeof t.support.priorities]}
                    </span>
                  </div>
                  <p className="text-sm text-gray-500">{ticket.category} &middot; {ticket.messageCount} messages</p>
                  <p className="text-xs text-gray-400">{formatDate(ticket.createdAt)}</p>
                </div>
                <div className="flex space-x-2">
                  <Button size="sm" variant="ghost" onClick={() => openDetail(ticket)}>
                    {t.common.viewDetails}
                  </Button>
                  {tab === 'manager' && (ticket.status === 'OPEN' || ticket.status === 'IN_PROGRESS') && (
                    <>
                      <Button size="sm" onClick={() => handleResolve(ticket.id)} loading={submitting}>
                        {t.support.resolve}
                      </Button>
                      <Button size="sm" variant="ghost" onClick={() => handleEscalate(ticket.id)} loading={submitting}>
                        {t.support.escalate}
                      </Button>
                    </>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title={t.support.createTicket}>
        <div className="space-y-4">
          <Input
            label={t.support.subject}
            placeholder={t.support.subjectPlaceholder}
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
          />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.support.category}</label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
            >
              <option value="SETTLEMENT">{t.support.categories.SETTLEMENT}</option>
              <option value="COMPLIANCE">{t.support.categories.COMPLIANCE}</option>
              <option value="API">{t.support.categories.API}</option>
              <option value="ACCOUNT">{t.support.categories.ACCOUNT}</option>
              <option value="OTHER">{t.support.categories.OTHER}</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.support.priority}</label>
            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
            >
              <option value="LOW">{t.support.priorities.LOW}</option>
              <option value="MEDIUM">{t.support.priorities.MEDIUM}</option>
              <option value="HIGH">{t.support.priorities.HIGH}</option>
              <option value="URGENT">{t.support.priorities.URGENT}</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.support.message}</label>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              rows={4}
              placeholder={t.support.messagePlaceholder}
            />
          </div>
          <div className="flex space-x-3">
            <Button onClick={handleCreate} loading={submitting} className="flex-1">{t.support.submitTicket}</Button>
            <Button variant="secondary" onClick={() => setShowCreate(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!showDetail} onClose={() => setShowDetail(null)} title={t.support.ticketDetail}>
        {showDetail && (
          <div className="space-y-4">
            <div className="space-y-1 text-sm">
              <p><span className="font-medium">{t.support.subject}:</span> {showDetail.subject}</p>
              <p><span className="font-medium">{t.support.category}:</span> {showDetail.category}</p>
              <div className="flex items-center space-x-2">
                <span className="font-medium">{t.common.status}:</span>
                <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(showDetail.status))}>
                  {t.support.status[showDetail.status as keyof typeof t.support.status]}
                </span>
                <span className={cn('text-xs px-2 py-0.5 rounded-full', priorityColor(showDetail.priority))}>
                  {t.support.priorities[showDetail.priority as keyof typeof t.support.priorities]}
                </span>
              </div>
            </div>

            <div>
              <h4 className="font-medium text-gray-900 mb-2">{t.support.ticketDetail}</h4>
              <div className="space-y-2 max-h-60 overflow-y-auto">
                {messages.map((msg) => (
                  <div key={msg.id} className={cn('rounded-lg p-3 text-sm', msg.senderId === user?.id ? 'bg-blue-50 ml-8' : 'bg-gray-50 mr-8')}>
                    <p className="font-medium text-gray-700">{msg.senderType}</p>
                    <p className="text-gray-600 mt-1">{msg.message}</p>
                    <p className="text-xs text-gray-400 mt-1">{formatDate(msg.createdAt)}</p>
                  </div>
                ))}
              </div>
            </div>

            {showDetail.status !== 'CLOSED' && showDetail.status !== 'RESOLVED' && (
              <div className="flex items-center space-x-2">
                <Input
                  placeholder={t.support.messagePlaceholder2}
                  value={replyMessage}
                  onChange={(e) => setReplyMessage(e.target.value)}
                  className="flex-1"
                />
                <Button onClick={handleReply} loading={submitting} size="sm">
                  {t.support.sendMessage}
                </Button>
              </div>
            )}

            {tab === 'manager' && (showDetail.status === 'OPEN' || showDetail.status === 'IN_PROGRESS') && (
              <div className="flex space-x-3 pt-2 border-t">
                <Button onClick={() => handleResolve(showDetail.id)} loading={submitting} className="flex-1">
                  {t.support.resolve}
                </Button>
                <Button onClick={() => handleEscalate(showDetail.id)} loading={submitting} variant="ghost" className="flex-1">
                  {t.support.escalate}
                </Button>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
