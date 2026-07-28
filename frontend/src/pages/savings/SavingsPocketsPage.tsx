import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { savingsApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency, formatDate } from '../../utils';

interface SavingsPocket {
  id: string;
  name: string;
  currentAmount: number;
  goalAmount: number;
  targetDate: string;
  interestEarned: number;
  status: string;
  createdAt: string;
}

export function SavingsPocketsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [pockets, setPockets] = useState<SavingsPocket[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [showDeposit, setShowDeposit] = useState<string | null>(null);
  const [showWithdraw, setShowWithdraw] = useState<string | null>(null);
  const [newName, setNewName] = useState('');
  const [newGoal, setNewGoal] = useState<number>(0);
  const [newTargetDate, setNewTargetDate] = useState('');
  const [actionAmount, setActionAmount] = useState<number>(0);
  const [submitting, setSubmitting] = useState(false);

  const loadPockets = async () => {
    if (!user) return;
    try {
      const data = await savingsApi.getPockets(user.id);
      setPockets(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPockets();
  }, [user]);

  const handleCreate = async () => {
    if (!user || !newName || !newGoal) return;
    setSubmitting(true);
    try {
      await savingsApi.createPocket(user.id, { name: newName, goalAmount: newGoal, targetDate: newTargetDate });
      setShowCreate(false);
      setNewName('');
      setNewGoal(0);
      setNewTargetDate('');
      await loadPockets();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeposit = async () => {
    if (!user || !showDeposit || !actionAmount) return;
    setSubmitting(true);
    try {
      await savingsApi.deposit(user.id, { pocketId: showDeposit, amount: actionAmount });
      setShowDeposit(null);
      setActionAmount(0);
      await loadPockets();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleWithdraw = async () => {
    if (!user || !showWithdraw || !actionAmount) return;
    setSubmitting(true);
    try {
      await savingsApi.withdraw(user.id, { pocketId: showWithdraw, amount: actionAmount });
      setShowWithdraw(null);
      setActionAmount(0);
      await loadPockets();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="text-center py-8">{t.common.loading}</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t.savings.title}</h1>
        <Button onClick={() => setShowCreate(true)}>{t.savings.create}</Button>
      </div>

      {pockets.length === 0 ? (
        <Card>
          <p className="text-center text-gray-500 py-8">{t.savings.noPockets}</p>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {pockets.map((pocket) => {
            const progress = pocket.goalAmount > 0 ? (pocket.currentAmount / pocket.goalAmount) * 100 : 0;
            return (
              <Card key={pocket.id}>
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="font-semibold text-gray-900">{pocket.name}</h3>
                    <span className={`text-xs px-2 py-0.5 rounded-full ${
                      pocket.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                    }`}>
                      {pocket.status}
                    </span>
                  </div>

                  <div>
                    <div className="flex justify-between text-sm text-gray-600 mb-1">
                      <span>{t.savings.currentAmount}: {formatCurrency(pocket.currentAmount)}</span>
                      <span>{t.savings.goalAmount}: {formatCurrency(pocket.goalAmount)}</span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2.5">
                      <div
                        className="bg-blue-600 h-2.5 rounded-full transition-all"
                        style={{ width: `${Math.min(progress, 100)}%` }}
                      />
                    </div>
                    <p className="text-xs text-gray-500 mt-1">{t.savings.progress}: {progress.toFixed(1)}%</p>
                  </div>

                  <div className="flex justify-between text-sm">
                    <span className="text-green-600 font-medium">
                      {t.savings.interestEarned}: {formatCurrency(pocket.interestEarned)}
                    </span>
                    {pocket.targetDate && (
                      <span className="text-gray-500">
                        {t.savings.targetDateLabel}: {formatDate(pocket.targetDate)}
                      </span>
                    )}
                  </div>

                  <div className="flex space-x-2">
                    <Button size="sm" onClick={() => { setShowDeposit(pocket.id); setActionAmount(0); }}>
                      {t.savings.deposit}
                    </Button>
                    <Button size="sm" variant="secondary" onClick={() => { setShowWithdraw(pocket.id); setActionAmount(0); }}>
                      {t.savings.withdraw}
                    </Button>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title={t.savings.create}>
        <div className="space-y-4">
          <Input
            label={t.savings.pocketName}
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder={t.savings.pocketNamePlaceholder}
          />
          <Input
            label={t.savings.goal}
            type="number"
            value={newGoal || ''}
            onChange={(e) => setNewGoal(Number(e.target.value))}
            placeholder={t.savings.goalAmountPlaceholder}
          />
          <Input
            label={t.savings.targetDate}
            type="date"
            value={newTargetDate}
            onChange={(e) => setNewTargetDate(e.target.value)}
          />
          <div className="flex space-x-3">
            <Button onClick={handleCreate} loading={submitting} className="flex-1">{t.savings.createPocket}</Button>
            <Button variant="secondary" onClick={() => setShowCreate(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!showDeposit} onClose={() => setShowDeposit(null)} title={t.savings.deposit}>
        <div className="space-y-4">
          <Input
            label={t.savings.depositAmount}
            type="number"
            value={actionAmount || ''}
            onChange={(e) => setActionAmount(Number(e.target.value))}
            placeholder="0"
          />
          <div className="flex space-x-3">
            <Button onClick={handleDeposit} loading={submitting} className="flex-1">{t.savings.makeDeposit}</Button>
            <Button variant="secondary" onClick={() => setShowDeposit(null)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!showWithdraw} onClose={() => setShowWithdraw(null)} title={t.savings.withdraw}>
        <div className="space-y-4">
          <Input
            label={t.savings.withdrawAmount}
            type="number"
            value={actionAmount || ''}
            onChange={(e) => setActionAmount(Number(e.target.value))}
            placeholder="0"
          />
          <div className="flex space-x-3">
            <Button onClick={handleWithdraw} loading={submitting} className="flex-1">{t.savings.makeWithdraw}</Button>
            <Button variant="secondary" onClick={() => setShowWithdraw(null)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
