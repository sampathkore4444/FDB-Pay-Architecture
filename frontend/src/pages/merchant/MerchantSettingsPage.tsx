import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { payoutApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import type { MerchantPreferences } from '../../types';

const defaults: MerchantPreferences = { settlementPreferredTime: '12:00', alertLargeOrderThreshold: 0, alertDailySurgeThreshold: 0, webhookUrl: '' };

export function MerchantSettingsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [prefs, setPrefs] = useState<MerchantPreferences>(defaults);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setPrefs((await payoutApi.getPreferences(user.id)) ?? defaults);
    } catch (err) {
      console.error('Failed to load preferences', err);
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
      await payoutApi.updatePreferences(user.id, {
        settlementPreferredTime: prefs.settlementPreferredTime || undefined,
        alertLargeOrderThreshold: prefs.alertLargeOrderThreshold ? Number(prefs.alertLargeOrderThreshold) : undefined,
        alertDailySurgeThreshold: prefs.alertDailySurgeThreshold ? Number(prefs.alertDailySurgeThreshold) : undefined,
        webhookUrl: prefs.webhookUrl || undefined,
      });
      toast.success(t.merchantSettings.saved);
      await load();
    } catch (err) {
      console.error('Failed to save preferences', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const set = (key: keyof MerchantPreferences, value: string) => setPrefs((p) => ({ ...p, [key]: value }));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t.merchantSettings.title}</h1>
        <p className="text-sm text-gray-500 mt-1">{t.merchantSettings.subtitle}</p>
      </div>

      <Card title={t.merchantSettings.preferences}>
        {loading ? (
          <p className="text-center text-gray-500 py-10">{t.common.loading}</p>
        ) : (
          <div className="space-y-4">
            <div className="grid sm:grid-cols-2 gap-4">
              <Input label={t.merchantSettings.settlementTime} type="time" value={prefs.settlementPreferredTime || ''} onChange={(e) => set('settlementPreferredTime', e.target.value)} />
              <Input label={t.merchantSettings.largeOrderThreshold} type="number" value={prefs.alertLargeOrderThreshold ? String(prefs.alertLargeOrderThreshold) : ''} onChange={(e) => set('alertLargeOrderThreshold', e.target.value)} />
            </div>
            <Input label={t.merchantSettings.dailySurgeThreshold} type="number" value={prefs.alertDailySurgeThreshold ? String(prefs.alertDailySurgeThreshold) : ''} onChange={(e) => set('alertDailySurgeThreshold', e.target.value)} />
            <Input label={t.merchantSettings.webhookUrl} type="url" value={prefs.webhookUrl || ''} onChange={(e) => set('webhookUrl', e.target.value)} placeholder="https://example.com/webhook" />
            <div className="flex justify-end">
              <Button onClick={handleSave} loading={submitting}>{t.common.save}</Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
