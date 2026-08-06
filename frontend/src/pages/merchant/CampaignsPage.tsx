import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { cashbackApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency } from '../../utils';
import type { CashbackCampaign } from '../../types';

const emptyForm = { name: '', percent: '', budget: '', startsAt: '', endsAt: '' };

export function CampaignsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [campaigns, setCampaigns] = useState<CashbackCampaign[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setCampaigns(await cashbackApi.list(user.id));
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
      ) : campaigns.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.campaigns.noCampaigns}</p></Card>
      ) : (
        <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
          {campaigns.map((campaign) => (
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
          ))}
        </div>
      )}

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
