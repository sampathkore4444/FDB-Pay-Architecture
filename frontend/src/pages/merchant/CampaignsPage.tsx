import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { cashbackApi, marketingCampaignApi, discountApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency } from '../../utils';
import type { CashbackCampaign, MarketingCampaign, DiscountCode } from '../../types';

const emptyForm = { name: '', percent: '', budget: '', startsAt: '', endsAt: '' };
const emptyMarketing = { name: '', campaignType: 'LOYALTY', audienceSegment: 'ALL', discountCodeId: '' };

export function CampaignsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [campaigns, setCampaigns] = useState<CashbackCampaign[]>([]);
  const [marketingCampaigns, setMarketingCampaigns] = useState<MarketingCampaign[]>([]);
  const [discountCodes, setDiscountCodes] = useState<DiscountCode[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [showMarketingForm, setShowMarketingForm] = useState(false);
  const [marketingForm, setMarketingForm] = useState(emptyMarketing);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const [cash, marketing, codes] = await Promise.all([
        cashbackApi.list(user.id),
        marketingCampaignApi.list(user.id),
        discountApi.list(user.id),
      ]);
      setCampaigns(cash);
      setMarketingCampaigns(marketing);
      setDiscountCodes(codes);
    } catch (err) {
      console.error('Failed to load campaigns', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const handleSave = async () => {
    if (!user || !form.name || !form.percent) return;
    setSubmitting(true);
    try {
      await cashbackApi.create(user.id, {
        name: form.name,
        percent: Number(form.percent),
        budget: form.budget ? Number(form.budget) : undefined,
        startsAt: form.startsAt ? new Date(form.startsAt).toISOString() : undefined,
        endsAt: form.endsAt ? new Date(form.endsAt).toISOString() : undefined,
      });
      toast.success(t.campaigns.created);
      setShowForm(false);
      setForm(emptyForm);
      await load();
    } catch (err) {
      console.error('Failed to create campaign', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const toggle = async (campaign: CashbackCampaign) => {
    if (!user) return;
    try {
      await cashbackApi.toggle(user.id, campaign.id);
      await load();
    } catch (err) {
      console.error('Failed to toggle campaign', err);
      toast.error(t.common.loadFailed);
    }
  };

  const remove = async (campaign: CashbackCampaign) => {
    if (!user || !window.confirm(t.campaigns.deleteConfirm)) return;
    try {
      await cashbackApi.delete(user.id, campaign.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to delete campaign', err);
      toast.error(t.common.loadFailed);
    }
  };

  const set = (key: keyof typeof emptyForm, value: string) => setForm((f) => ({ ...f, [key]: value }));

  const handleMarketingSave = async () => {
    if (!user || !marketingForm.name) return;
    setSubmitting(true);
    try {
      await marketingCampaignApi.create(user.id, {
        name: marketingForm.name,
        campaignType: marketingForm.campaignType,
        audienceSegment: marketingForm.audienceSegment,
        discountCodeId: marketingForm.discountCodeId || undefined,
      });
      toast.success(t.campaigns.marketingCreated);
      setShowMarketingForm(false);
      setMarketingForm(emptyMarketing);
      await load();
    } catch (err) {
      console.error('Failed to create marketing campaign', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleMarketing = async (campaign: MarketingCampaign) => {
    if (!user) return;
    try {
      await marketingCampaignApi.toggle(user.id, campaign.id);
      await load();
    } catch (err) {
      console.error('Failed to toggle marketing campaign', err);
      toast.error(t.common.loadFailed);
    }
  };

  const removeMarketing = async (campaign: MarketingCampaign) => {
    if (!user || !window.confirm(t.campaigns.deleteConfirm)) return;
    try {
      await marketingCampaignApi.delete(user.id, campaign.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to delete marketing campaign', err);
      toast.error(t.common.loadFailed);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.campaigns.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.campaigns.subtitle}</p>
        </div>
        <Button onClick={() => { setForm(emptyForm); setShowForm(true); }}>{t.campaigns.createCampaign}</Button>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : (
        <>
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">{t.campaigns.marketingTitle}</h2>
            <Button size="sm" onClick={() => { setMarketingForm(emptyMarketing); setShowMarketingForm(true); }}>{t.campaigns.createCampaign}</Button>
          </div>
          <Card>
            {marketingCampaigns.length === 0 ? (
              <p className="text-center text-gray-500 py-6">{t.campaigns.noCampaigns}</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                      <th className="pb-2 pr-4">{t.campaigns.name}</th>
                      <th className="pb-2 pr-4">{t.campaigns.type}</th>
                      <th className="pb-2 pr-4">{t.campaigns.audience}</th>
                      <th className="pb-2 pr-4">{t.common.status}</th>
                      <th className="pb-2">{t.common.actions}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {marketingCampaigns.map((campaign) => (
                      <tr key={campaign.id} className="border-b border-gray-100">
                        <td className="py-2 pr-4 font-medium text-gray-900">{campaign.name}</td>
                        <td className="py-2 pr-4 text-gray-600">{campaign.campaignType}</td>
                        <td className="py-2 pr-4 text-gray-600">{campaign.audienceSegment}</td>
                        <td className="py-2 pr-4"><span className={`px-2 py-0.5 rounded text-xs font-medium ${campaign.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{campaign.status}</span></td>
                        <td className="py-2 space-x-2">
                          <Button size="sm" variant="secondary" onClick={() => toggleMarketing(campaign)}>{campaign.status === 'ACTIVE' ? t.common.deactivate : t.common.activate}</Button>
                          <Button size="sm" variant="danger" onClick={() => removeMarketing(campaign)}>{t.common.delete}</Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <h2 className="text-lg font-semibold text-gray-900">{t.campaigns.cashbackTitle}</h2>
          <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
            {campaigns.length === 0 ? (
              <Card><p className="text-center text-gray-500 py-6">{t.campaigns.noCampaigns}</p></Card>
            ) : (
              campaigns.map((campaign) => (
                <Card key={campaign.id}>
                  <div className="flex items-start justify-between">
                    <h3 className="font-semibold text-gray-900">{campaign.name}</h3>
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${campaign.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{campaign.status}</span>
                  </div>
                  <p className="mt-2 text-2xl font-bold text-gray-900">{campaign.percent}%</p>
                  <p className="text-sm text-gray-500 mt-1">{t.campaigns.budget}: {formatCurrency(campaign.budget ?? 0)} · {t.campaigns.spent}: {formatCurrency(campaign.spent ?? 0)}</p>
                  {campaign.endsAt && <p className="text-xs text-gray-400 mt-1">{t.campaigns.endsAt}: {new Date(campaign.endsAt).toLocaleDateString()}</p>}
                  <div className="mt-4 flex justify-end space-x-2">
                    <Button size="sm" variant="secondary" onClick={() => toggle(campaign)}>{campaign.status === 'ACTIVE' ? t.common.deactivate : t.common.activate}</Button>
                    <Button size="sm" variant="danger" onClick={() => remove(campaign)}>{t.common.delete}</Button>
                  </div>
                </Card>
              ))
            )}
          </div>
        </>
      )}

      <Modal open={showMarketingForm} onClose={() => setShowMarketingForm(false)} title={t.campaigns.createCampaign}>
        <div className="space-y-4">
          <Input label={t.campaigns.name} value={marketingForm.name} onChange={(e) => setMarketingForm((f) => ({ ...f, name: e.target.value }))} />
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t.campaigns.type}</label>
              <select value={marketingForm.campaignType} onChange={(e) => setMarketingForm((f) => ({ ...f, campaignType: e.target.value }))} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                <option value="LOYALTY">LOYALTY</option>
                <option value="DISCOUNT">DISCOUNT</option>
                <option value="CASHBACK">CASHBACK</option>
                <option value="PROMOTION">PROMOTION</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t.campaigns.audience}</label>
              <select value={marketingForm.audienceSegment} onChange={(e) => setMarketingForm((f) => ({ ...f, audienceSegment: e.target.value }))} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                <option value="ALL">ALL</option>
                <option value="HIGH_VALUE">HIGH_VALUE</option>
                <option value="LOYAL">LOYAL</option>
                <option value="NEW">NEW</option>
                <option value="AT_RISK">AT_RISK</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.campaigns.discountCode}</label>
            <select value={marketingForm.discountCodeId} onChange={(e) => setMarketingForm((f) => ({ ...f, discountCodeId: e.target.value }))} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
              <option value="">—</option>
              {discountCodes.map((code) => (
                <option key={code.id} value={code.id}>{code.code}</option>
              ))}
            </select>
          </div>
          <div className="flex space-x-3">
            <Button onClick={handleMarketingSave} loading={submitting} disabled={!marketingForm.name} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowMarketingForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={showForm} onClose={() => setShowForm(false)} title={t.campaigns.createCampaign}>
        <div className="space-y-4">
          <Input label={t.campaigns.name} value={form.name} onChange={(e) => set('name', e.target.value)} />
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.campaigns.percent} type="number" value={form.percent} onChange={(e) => set('percent', e.target.value)} />
            <Input label={t.campaigns.budget} type="number" value={form.budget} onChange={(e) => set('budget', e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.campaigns.startsAt} type="date" value={form.startsAt} onChange={(e) => set('startsAt', e.target.value)} />
            <Input label={t.campaigns.endsAt} type="date" value={form.endsAt} onChange={(e) => set('endsAt', e.target.value)} />
          </div>
          <div className="flex space-x-3">
            <Button onClick={handleSave} loading={submitting} disabled={!form.name || !form.percent} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
