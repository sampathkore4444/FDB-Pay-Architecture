import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { remittanceApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatDate, cn } from '../../utils';
import type { RemittanceCorridor, Remittance } from '../../types';

export function RemittanceReceivePage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [corridors, setCorridors] = useState<RemittanceCorridor[]>([]);
  const [remittances, setRemittances] = useState<Remittance[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCorridor, setSelectedCorridor] = useState<RemittanceCorridor | null>(null);
  const [showInitiate, setShowInitiate] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [sourceAmount, setSourceAmount] = useState<number>(0);
  const [quote, setQuote] = useState<{ destAmount: number; fee: number; exchangeRate: number; totalDest: number } | null>(null);
  const [calculating, setCalculating] = useState(false);

  const [recipientPhone, setRecipientPhone] = useState('');
  const [senderName, setSenderName] = useState('');
  const [senderPhone, setSenderPhone] = useState('');

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        const [c, r] = await Promise.all([
          remittanceApi.getCorridors(),
          user ? remittanceApi.getMyRemittances(user.id) : Promise.resolve([]),
        ]);
        setCorridors(c);
        setRemittances(r);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [user]);

  const handleCalculate = async () => {
    if (!selectedCorridor || !sourceAmount) return;
    setCalculating(true);
    try {
      const q = await remittanceApi.getQuote(selectedCorridor.id, sourceAmount);
      setQuote(q);
    } catch (err) {
      console.error(err);
    } finally {
      setCalculating(false);
    }
  };

  const handleInitiate = async () => {
    if (!user || !selectedCorridor || !recipientPhone || !senderName || !senderPhone) return;
    setSubmitting(true);
    try {
      await remittanceApi.initiate(user.id, {
        corridorId: selectedCorridor.id,
        sourceAmount,
        recipientPhone,
        senderName,
        senderPhone,
      });
      setShowInitiate(false);
      setRecipientPhone('');
      setSenderName('');
      setSenderPhone('');
      const r = await remittanceApi.getMyRemittances(user.id);
      setRemittances(r);
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const statusColor = (s: string) => {
    const m: Record<string, string> = {
      PENDING: 'bg-yellow-100 text-yellow-800',
      PROCESSING: 'bg-blue-100 text-blue-800',
      COMPLETED: 'bg-green-100 text-green-800',
      FAILED: 'bg-red-100 text-red-800',
    };
    return m[s] || 'bg-gray-100 text-gray-800';
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.remittance.title}</h1>

      {loading ? (
        <div className="text-center py-8">{t.common.loading}</div>
      ) : (
        <>
          <Card title={t.remittance.corridors}>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {corridors.filter((c) => c.status === 'ACTIVE').map((corridor) => (
                <div
                  key={corridor.id}
                  onClick={() => {
                    setSelectedCorridor(corridor);
                    setQuote(null);
                    setSourceAmount(0);
                    setShowInitiate(true);
                  }}
                  className={cn(
                    'border rounded-xl p-4 cursor-pointer transition-colors hover:bg-gray-50',
                    selectedCorridor?.id === corridor.id ? 'border-blue-500 bg-blue-50' : 'border-gray-200'
                  )}
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-semibold text-gray-900">{corridor.sourceCountry} → MM</span>
                    <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full">
                      {t.common.active}
                    </span>
                  </div>
                  <div className="space-y-1 text-sm text-gray-600">
                    <p>{t.remittance.exchangeRate}: 1 {corridor.sourceCurrency} = {corridor.exchangeRate} MMK</p>
                    <p>{t.remittance.fee}: {corridor.fee} {corridor.sourceCurrency}</p>
                    <p>{t.remittance.limits}: {corridor.minAmount.toLocaleString()} - {corridor.maxAmount.toLocaleString()} {corridor.sourceCurrency}</p>
                    <p>{t.remittance.partner}: {corridor.partnerName}</p>
                    <p>{t.remittance.estimatedDelivery}: {corridor.estimatedDelivery}</p>
                  </div>
                </div>
              ))}
            </div>
          </Card>

          {remittances.length > 0 && (
            <Card title={t.remittance.history}>
              <div className="space-y-2">
                {remittances.map((r) => (
                  <div key={r.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className="text-sm font-medium">{r.senderName}</span>
                        <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(r.status))}>
                          {t.remittance.status[r.status as keyof typeof t.remittance.status]}
                        </span>
                      </div>
                      <p className="text-sm text-gray-500">{r.sourceAmount} {r.sourceCurrency} → {r.destAmount.toLocaleString()} MMK</p>
                      <p className="text-xs text-gray-400">{formatDate(r.createdAt)}</p>
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          )}
        </>
      )}

      <Modal open={showInitiate} onClose={() => setShowInitiate(false)} title={selectedCorridor ? `${selectedCorridor.sourceCountry} → MM` : ''}>
        {selectedCorridor && (
          <div className="space-y-4">
            <Input
              label={`${t.remittance.sourceAmount} (${selectedCorridor.sourceCurrency})`}
              type="number"
              placeholder={t.remittance.sourceAmountPlaceholder}
              value={sourceAmount || ''}
              onChange={(e) => { setSourceAmount(Number(e.target.value)); setQuote(null); }}
            />

            <Button onClick={handleCalculate} loading={calculating} variant="secondary" className="w-full">
              {t.remittance.calculate}
            </Button>

            {quote && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-4 space-y-2">
                <p className="text-sm text-green-700 font-medium">{t.remittance.quoteResult}</p>
                <p className="text-2xl font-bold text-green-800">{quote.totalDest.toLocaleString()} MMK</p>
                <p className="text-xs text-green-600">{t.remittance.afterFees} ({t.remittance.fee}: {quote.fee} {selectedCorridor.sourceCurrency})</p>
              </div>
            )}

            <Input
              label={t.remittance.recipientPhone}
              placeholder={t.remittance.recipientPhonePlaceholder}
              value={recipientPhone}
              onChange={(e) => setRecipientPhone(e.target.value)}
            />
            <Input
              label={t.remittance.senderName}
              placeholder={t.remittance.senderNamePlaceholder}
              value={senderName}
              onChange={(e) => setSenderName(e.target.value)}
            />
            <Input
              label={t.remittance.senderPhone}
              placeholder={t.remittance.senderPhonePlaceholder}
              value={senderPhone}
              onChange={(e) => setSenderPhone(e.target.value)}
            />

            <div className="flex space-x-3">
              <Button onClick={handleInitiate} loading={submitting} className="flex-1">{t.remittance.initiate}</Button>
              <Button variant="secondary" onClick={() => setShowInitiate(false)} className="flex-1">{t.common.cancel}</Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
