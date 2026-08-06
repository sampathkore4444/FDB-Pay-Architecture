import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { loyaltyApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency } from '../../utils';
import type { LoyaltySettings } from '../../types';

const defaults: LoyaltySettings = { enabled: false, pointsPerMmk: 0, rewardThresholdPoints: 0, rewardValue: 0 };

export function LoyaltyPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [settings, setSettings] = useState<LoyaltySettings>(defaults);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setSettings((await loyaltyApi.get(user.id)) ?? defaults);
    } catch (err) {
      console.error('Failed to load loyalty settings', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const handleSave = async () => {
    if (!user) return;
    setSubmitting(true);
    try {
      await loyaltyApi.update(user.id, settings);
      toast.success(t.loyalty.saved);
      await load();
    } catch (err) {
      console.error('Failed to save loyalty settings', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const set = (key: keyof LoyaltySettings, value: string | boolean) => setSettings((s) => ({ ...s, [key]: value }));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t.loyalty.title}</h1>
        <p className="text-sm text-gray-500 mt-1">{t.loyalty.subtitle}</p>
      </div>

      <Card>
        {loading ? (
          <p className="text-center text-gray-500 py-10">{t.common.loading}</p>
        ) : (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-gray-700">{t.loyalty.enable}</span>
              <input type="checkbox" checked={settings.enabled} onChange={(e) => set('enabled', e.target.checked)} className="h-5 w-5 accent-emerald-600" />
            </div>
            <div className="grid sm:grid-cols-3 gap-4">
              <Input label={t.loyalty.pointsPerMmk} type="number" value={settings.pointsPerMmk} onChange={(e) => set('pointsPerMmk', e.target.value)} />
              <Input label={t.loyalty.rewardThreshold} type="number" value={settings.rewardThresholdPoints} onChange={(e) => set('rewardThresholdPoints', e.target.value)} />
              <Input label={t.loyalty.rewardValue} type="number" value={settings.rewardValue} onChange={(e) => set('rewardValue', e.target.value)} />
            </div>
            <p className="text-sm text-gray-500">{t.loyalty.preview}: 1 MMK → {settings.pointsPerMmk} {t.loyalty.pointsLabel}, {settings.rewardThresholdPoints} {t.loyalty.pointsLabel} → {formatCurrency(settings.rewardValue)}</p>
            <div className="flex justify-end">
              <Button onClick={handleSave} loading={submitting} disabled={!settings.enabled}>{t.common.save}</Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
