import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { promotionsApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatDate, cn } from '../../utils';
import type { Promotion, CashbackWallet } from '../../types';

export function PromotionsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [promotions, setPromotions] = useState<Promotion[]>([]);
  const [wallet, setWallet] = useState<CashbackWallet | null>(null);
  const [loading, setLoading] = useState(true);
  const [promoCode, setPromoCode] = useState('');
  const [amount, setAmount] = useState<number>(5000);
  const [applying, setApplying] = useState(false);
  const [promoResult, setPromoResult] = useState<{ discount: number; message: string } | null>(null);
  const [redeemAmount, setRedeemAmount] = useState<number>(0);
  const [redeeming, setRedeeming] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const [p, w] = await Promise.all([
        promotionsApi.getActive(),
        user ? promotionsApi.getCashbackWallet(user.id) : Promise.resolve(null),
      ]);
      setPromotions(p);
      setWallet(w);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user]);

  const handleApply = async () => {
    if (!user || !promoCode || !amount) return;
    setApplying(true);
    setPromoResult(null);
    try {
      const res = await promotionsApi.validateCode(user.id, promoCode, amount);
      setPromoResult(res);
    } catch (err) {
      console.error(err);
      setPromoResult({ discount: 0, message: 'Invalid or expired promo code' });
    } finally {
      setApplying(false);
    }
  };

  const handleRedeem = async () => {
    if (!user || !redeemAmount) return;
    setRedeeming(true);
    try {
      await promotionsApi.redeemCashback(user.id, redeemAmount);
      setRedeemAmount(0);
      await loadData();
    } catch (err) {
      console.error(err);
    } finally {
      setRedeeming(false);
    }
  };

  const handleRedeemAll = async () => {
    if (!user || !wallet || wallet.balance <= 0) return;
    setRedeeming(true);
    try {
      await promotionsApi.redeemCashback(user.id, wallet.balance);
      setRedeemAmount(0);
      await loadData();
    } catch (err) {
      console.error(err);
    } finally {
      setRedeeming(false);
    }
  };

  const typeColor = (type: string) => {
    const m: Record<string, string> = {
      DISCOUNT: 'bg-purple-100 text-purple-800',
      FIXED_DISCOUNT: 'bg-purple-100 text-purple-800',
      PERCENTAGE_DISCOUNT: 'bg-blue-100 text-blue-800',
      CASHBACK: 'bg-green-100 text-green-800',
      COUPON: 'bg-orange-100 text-orange-800',
      COUPON_CODE: 'bg-orange-100 text-orange-800',
      BOGO: 'bg-pink-100 text-pink-800',
    };
    return m[type] || 'bg-gray-100 text-gray-800';
  };

  const statusColor = (s: string) => {
    const m: Record<string, string> = {
      ACTIVE: 'bg-green-100 text-green-800',
      EXPIRED: 'bg-gray-100 text-gray-600',
      DRAFT: 'bg-yellow-100 text-yellow-800',
      PAUSED: 'bg-amber-100 text-amber-800',
    };
    return m[s] || 'bg-gray-100 text-gray-800';
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.promotions.title}</h1>

      {wallet && (
        <Card>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">{t.promotions.cashbackBalance}</p>
              <p className="text-2xl font-bold text-green-600">{wallet.balance.toLocaleString()} {wallet.currency}</p>
              <p className="text-xs text-gray-400">{t.promotions.totalEarned}: {wallet.totalEarned.toLocaleString()} | {t.promotions.totalRedeemed}: {wallet.totalRedeemed.toLocaleString()}</p>
            </div>
            {wallet.balance > 0 && (
              <div className="flex items-center space-x-2">
                <Input
                  type="number"
                  placeholder={t.promotions.redeemAmount}
                  value={redeemAmount || ''}
                  onChange={(e) => setRedeemAmount(Number(e.target.value))}
                  className="w-32"
                />
                <Button onClick={handleRedeem} loading={redeeming} size="sm">
                  {t.promotions.redeem}
                </Button>
                <Button onClick={handleRedeemAll} loading={redeeming} variant="secondary" size="sm">
                  {t.promotions.redeemAll}
                </Button>
              </div>
            )}
          </div>
        </Card>
      )}

      <Card>
        <div className="flex items-center space-x-3">
          <Input
            placeholder={t.promotions.codePlaceholder}
            value={promoCode}
            onChange={(e) => setPromoCode(e.target.value)}
            className="flex-1"
          />
          <Input
            type="number"
            placeholder={t.promotions.minAmount}
            value={amount || ''}
            onChange={(e) => setAmount(Number(e.target.value))}
            className="w-32"
          />
          <Button onClick={handleApply} loading={applying}>{t.promotions.apply}</Button>
        </div>
        {promoResult && (
          <p className={`mt-3 text-sm ${promoResult.discount > 0 ? 'text-green-600' : 'text-red-600'}`}>
            {promoResult.message}
          </p>
        )}
      </Card>

      {loading ? (
        <div className="text-center py-8">{t.common.loading}</div>
      ) : promotions.length === 0 ? (
        <Card>
          <p className="text-center text-gray-500 py-8">{t.promotions.noPromotions}</p>
        </Card>
      ) : (
        <div className="space-y-4">
          <h2 className="text-lg font-semibold text-gray-900">{t.promotions.activePromotions}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {promotions.map((promo) => (
              <div key={promo.id} className="bg-white border border-gray-200 rounded-xl p-5">
                <div className="flex items-start justify-between mb-3">
                  <div className="space-y-1">
                    <h3 className="font-semibold text-gray-900">{promo.title}</h3>
                    <p className="text-sm text-gray-500">{promo.description}</p>
                  </div>
                  <div className="flex space-x-1">
                    <span className={cn('text-xs px-2 py-0.5 rounded-full', typeColor(promo.type))}>
                      {t.promotions.type[promo.type as keyof typeof t.promotions.type]}
                    </span>
                    <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(promo.status))}>
                      {t.promotions.status[promo.status as keyof typeof t.promotions.status]}
                    </span>
                  </div>
                </div>
                <div className="space-y-1 text-sm text-gray-600">
                  {promo.discountValue > 0 && (
                    <p>{t.promotions.discountValue}: {promo.discountValue.toLocaleString()} {promo.type === 'CASHBACK' ? 'MMK' : '%'}</p>
                  )}
                  {promo.maxDiscount != null && promo.maxDiscount > 0 && (
                    <p>{t.promotions.maxDiscount}: {promo.maxDiscount.toLocaleString()} MMK</p>
                  )}
                  {promo.minTransactionAmount != null && promo.minTransactionAmount > 0 && (
                    <p>{t.promotions.minAmount}: {promo.minTransactionAmount.toLocaleString()} MMK</p>
                  )}
                  <p>{t.promotions.validFrom}: {formatDate(promo.startDate)}</p>
                  <p>{t.promotions.validTo}: {formatDate(promo.endDate)}</p>
                  <p>{t.promotions.usageLimit}: {promo.usageCount}/{promo.maxUsageTotal}</p>
                  {promo.promoCode && <p className="font-mono text-xs bg-gray-50 px-2 py-1 rounded">{promo.promoCode}</p>}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
