import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { referralApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency } from '../../utils';
import type { ReferralProgram } from '../../types';

const emptyForm = { code: '', referralBonus: '', referredBonus: '' };

export function ReferralPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [programs, setPrograms] = useState<ReferralProgram[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setPrograms(await referralApi.list(user.id));
    } catch (err) {
      console.error('Failed to load referral programs', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const generateCode = async () => {
    try {
      const code = await referralApi.generateCode();
      setForm((f) => ({ ...f, code }));
    } catch (err) {
      console.error('Failed to generate referral code', err);
      toast.error(t.common.loadFailed);
    }
  };

  const openCreate = () => {
    setForm(emptyForm);
    setShowForm(true);
    generateCode();
  };

  const handleSave = async () => {
    if (!user || !form.code) return;
    setSubmitting(true);
    try {
      await referralApi.create(user.id, {
        code: form.code,
        referralBonus: Number(form.referralBonus || 0),
        referredBonus: Number(form.referredBonus || 0),
      });
      toast.success(t.referral.created);
      setShowForm(false);
      setForm(emptyForm);
      await load();
    } catch (err) {
      console.error('Failed to create referral program', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const toggle = async (program: ReferralProgram) => {
    if (!user) return;
    try {
      await referralApi.toggle(user.id, program.id);
      await load();
    } catch (err) {
      console.error('Failed to toggle program', err);
      toast.error(t.common.loadFailed);
    }
  };

  const remove = async (program: ReferralProgram) => {
    if (!user || !window.confirm(t.referral.deleteConfirm)) return;
    try {
      await referralApi.delete(user.id, program.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to delete program', err);
      toast.error(t.common.loadFailed);
    }
  };

  const copy = (code: string) => {
    navigator.clipboard?.writeText(code).catch(() => undefined);
    toast.success(t.referral.copied);
  };

  const set = (key: keyof typeof emptyForm, value: string) => setForm((f) => ({ ...f, [key]: value }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.referral.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.referral.subtitle}</p>
        </div>
        <Button onClick={openCreate}>{t.referral.createProgram}</Button>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : programs.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.referral.noPrograms}</p></Card>
      ) : (
        <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
          {programs.map((program) => (
            <Card key={program.id}>
              <div className="flex items-start justify-between">
                <button onClick={() => copy(program.code)} className="font-mono font-bold text-emerald-700 hover:underline" title={t.referral.clickToCopy}>{program.code}</button>
                <span className={`px-2 py-0.5 rounded text-xs font-medium ${program.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{program.status}</span>
              </div>
              <p className="mt-3 text-sm text-gray-600">{t.referral.referrerEarns} <strong>{formatCurrency(program.referralBonus)}</strong> · {t.referral.friendEarns} <strong>{formatCurrency(program.referredBonus)}</strong></p>
              <p className="mt-1 text-xs text-gray-400">{program.uses} {t.referral.usesLabel}</p>
              <div className="mt-4 flex justify-end space-x-2">
                <Button size="sm" variant="secondary" onClick={() => toggle(program)}>{program.status === 'ACTIVE' ? t.common.deactivate : t.common.activate}</Button>
                <Button size="sm" variant="danger" onClick={() => remove(program)}>{t.common.delete}</Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={showForm} onClose={() => setShowForm(false)} title={t.referral.createProgram}>
        <div className="space-y-4">
          <Input label={t.referral.code} value={form.code} onChange={(e) => set('code', e.target.value)} />
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.referral.referralBonus} type="number" value={form.referralBonus} onChange={(e) => set('referralBonus', e.target.value)} />
            <Input label={t.referral.referredBonus} type="number" value={form.referredBonus} onChange={(e) => set('referredBonus', e.target.value)} />
          </div>
          <div className="flex space-x-3">
            <Button onClick={handleSave} loading={submitting} disabled={!form.code} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
