import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { Link } from 'react-router-dom';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { merchantApi, walletApi, customerApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency } from '../../utils';
import type { Merchant, CustomerInsight, MerchantReview, SegmentSummary } from '../../types';

const emptyReview = { customerName: '', customerPhone: '', rating: '5', comment: '' };

export function CustomersPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [insights, setInsights] = useState<CustomerInsight[]>([]);
  const [segments, setSegments] = useState<SegmentSummary[]>([]);
  const [reviews, setReviews] = useState<MerchantReview[]>([]);
  const [loading, setLoading] = useState(true);
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [reviewForm, setReviewForm] = useState(emptyReview);
  const [replyFor, setReplyFor] = useState<MerchantReview | null>(null);
  const [replyText, setReplyText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [searchPhone, setSearchPhone] = useState('');

  useEffect(() => {
    if (!user) return;
    merchantApi.getProfile(user.id).then(setMerchant).catch(() => toast.error(t.common.loadFailed));
  }, [user]);

  const load = async () => {
    if (!user || !merchant) return;
    setLoading(true);
    try {
      const wallet = await walletApi.getWallet(merchant.userId);
      const [c, s, r] = await Promise.all([customerApi.insights(user.id, wallet.id), customerApi.segments(user.id, wallet.id), customerApi.listReviews(user.id)]);
      setInsights(c);
      setSegments(s);
      setReviews(r);
    } catch (err) {
      console.error('Failed to load customers', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (merchant) load();
  }, [merchant]);

  const submitReview = async () => {
    if (!user) return;
    setSubmitting(true);
    try {
      await customerApi.createReview(user.id, {
        customerName: reviewForm.customerName || undefined,
        customerPhone: reviewForm.customerPhone || undefined,
        rating: Number(reviewForm.rating),
        comment: reviewForm.comment || undefined,
      });
      toast.success(t.customers.reviewCreated);
      setShowReviewForm(false);
      setReviewForm(emptyReview);
      await load();
    } catch (err) {
      console.error('Failed to create review', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const submitReply = async () => {
    if (!user || !replyFor) return;
    setSubmitting(true);
    try {
      await customerApi.replyReview(user.id, replyFor.id, replyText);
      toast.success(t.customers.replySent);
      setReplyFor(null);
      setReplyText('');
      await load();
    } catch (err) {
      console.error('Failed to reply', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const removeReview = async (review: MerchantReview) => {
    if (!user || !window.confirm(t.customers.deleteReviewConfirm)) return;
    try {
      await customerApi.deleteReview(user.id, review.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to delete review', err);
      toast.error(t.common.loadFailed);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.customers.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.customers.subtitle}</p>
        </div>
        <Button onClick={() => { setReviewForm(emptyReview); setShowReviewForm(true); }}>{t.customers.addReview}</Button>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : (
        <>
          <Card title={t.customers.searchCustomer}>
            <div className="flex items-center space-x-2">
              <Input placeholder={t.customers.searchPlaceholder} value={searchPhone} onChange={(e) => setSearchPhone(e.target.value)} />
              <Link to={`/merchant/customers/${encodeURIComponent(searchPhone)}`}>
                <Button disabled={!searchPhone.trim()}>{t.customers.viewDetail}</Button>
              </Link>
            </div>
          </Card>

          <Card title={t.customers.insightsTitle}>
            {insights.length === 0 ? (
              <p className="text-center text-gray-500 py-6">{t.common.noData}</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                      <th className="pb-2 pr-4">{t.customers.wallet}</th>
                      <th className="pb-2 pr-4">{t.customers.totalSpend}</th>
                      <th className="pb-2 pr-4">{t.customers.transactions}</th>
                      <th className="pb-2 pr-4">{t.customers.tier}</th>
                      <th className="pb-2">{t.customers.points}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {insights.map((c) => (
                      <tr key={c.walletId} className="border-b border-gray-100">
                        <td className="py-2 pr-4 font-mono text-xs text-gray-500">{c.walletId.slice(0, 8)}…</td>
                        <td className="py-2 pr-4 text-gray-900">{formatCurrency(c.totalSpend)}</td>
                        <td className="py-2 pr-4 text-gray-600">{c.transactionCount}</td>
                        <td className="py-2 pr-4"><span className="px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-700">{c.tier || '-'}</span></td>
                        <td className="py-2 text-gray-900">{c.loyaltyPoints ?? 0}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card title={t.customers.segmentsTitle}>
            {segments.length === 0 ? (
              <p className="text-center text-gray-500 py-6">{t.common.noData}</p>
            ) : (
              <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {segments.map((seg) => (
                  <div key={seg.segment} className="border border-gray-200 rounded-lg p-4">
                    <h4 className="font-semibold text-gray-900">{seg.segment}</h4>
                    <p className="text-sm text-gray-500 mt-1">{seg.customerCount} {t.customers.customers}</p>
                    <p className="text-lg font-bold text-gray-900 mt-2">{formatCurrency(seg.totalSpend)}</p>
                  </div>
                ))}
              </div>
            )}
          </Card>

          <Card title={t.customers.reviewsTitle}>
            {reviews.length === 0 ? (
              <p className="text-center text-gray-500 py-6">{t.customers.noReviews}</p>
            ) : (
              <div className="space-y-4">
                {reviews.map((review) => (
                  <div key={review.id} className="border border-gray-200 rounded-lg p-4">
                    <div className="flex items-start justify-between">
                      <div>
                        <p className="font-medium text-gray-900">{review.customerName || review.customerPhone || '-'}</p>
                        <p className="text-sm text-gray-500 mt-0.5">{'★'.repeat(Math.min(5, Math.max(0, review.rating)))} <span className="text-gray-300">{'★'.repeat(5 - Math.min(5, Math.max(0, review.rating)))}</span></p>
                      </div>
                      <span className={`px-2 py-0.5 rounded text-xs font-medium ${review.status === 'PUBLISHED' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{review.status}</span>
                    </div>
                    {review.comment && <p className="text-sm text-gray-600 mt-2">{review.comment}</p>}
                    {review.adminReply && <p className="text-sm text-blue-700 mt-2"><strong>{t.customers.merchantReply}:</strong> {review.adminReply}</p>}
                    <div className="mt-3 flex justify-end space-x-2">
                      <Button size="sm" variant="secondary" onClick={() => { setReplyFor(review); setReplyText(review.adminReply || ''); }}>{t.customers.reply}</Button>
                      <Button size="sm" variant="danger" onClick={() => removeReview(review)}>{t.common.delete}</Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </>
      )}

      <Modal open={showReviewForm} onClose={() => setShowReviewForm(false)} title={t.customers.addReview}>
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.customers.customerName} value={reviewForm.customerName} onChange={(e) => setReviewForm((f) => ({ ...f, customerName: e.target.value }))} />
            <Input label={t.customers.customerPhone} value={reviewForm.customerPhone} onChange={(e) => setReviewForm((f) => ({ ...f, customerPhone: e.target.value }))} />
          </div>
          <Input label={t.customers.rating} type="number" min={1} max={5} value={reviewForm.rating} onChange={(e) => setReviewForm((f) => ({ ...f, rating: e.target.value }))} />
          <Input label={t.customers.comment} value={reviewForm.comment} onChange={(e) => setReviewForm((f) => ({ ...f, comment: e.target.value }))} />
          <div className="flex space-x-3">
            <Button onClick={submitReview} loading={submitting} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowReviewForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!replyFor} onClose={() => setReplyFor(null)} title={t.customers.reply}>
        <div className="space-y-4">
          <Input label={t.customers.replyText} value={replyText} onChange={(e) => setReplyText(e.target.value)} />
          <div className="flex space-x-3">
            <Button onClick={submitReply} loading={submitting} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setReplyFor(null)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
