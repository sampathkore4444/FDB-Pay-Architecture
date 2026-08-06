import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { discountApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import type { DiscountCode } from '../../types';

const emptyForm = { code: '', type: 'PERCENT', value: '', minSpend: '', maxUses: '', validTo: '' };

export function CouponsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [codes, setCodes] = useState<DiscountCode[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setCodes(await discountApi.list(user.id));
    } catch (err) {
      console.error('Failed to load discount codes', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const handleSave = async () => {
    if (!user || !form.code || !form.value) return;
    setSubmitting(true);
    try {
      await discountApi.create(user.id, {
        code: form.code.toUpperCase(),
        type: form.type,
        value: Number(form.value),
        minSpend: form.minSpend ? Number(form.minSpend) : undefined,
        maxUses: form.maxUses ? Number(form.maxUses) : undefined,
        validTo: form.validTo ? new Date(form.validTo).toISOString() : undefined,
      });
      toast.success(t.coupons.created);
      setShowForm(false);
      setForm(emptyForm);
      await load();
    } catch (err) {
      console.error('Failed to create discount code', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const toggle = async (code: DiscountCode) => {
    if (!user) return;
    try {
      await discountApi.toggle(user.id, code.id);
      await load();
    } catch (err) {
      console.error('Failed to toggle code', err);
      toast.error(t.common.loadFailed);
    }
  };

  const remove = async (code: DiscountCode) => {
    if (!user || !window.confirm(t.coupons.deleteConfirm)) return;
    try {
      await discountApi.delete(user.id, code.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to delete code', err);
      toast.error(t.common.loadFailed);
    }
  };

  const set = (key: keyof typeof emptyForm, value: string) => setForm((f) => ({ ...f, [key]: value }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.coupons.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.coupons.subtitle}</p>
        </div>
        <Button onClick={() => { setForm(emptyForm); setShowForm(true); }}>{t.coupons.createCode}</Button>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : codes.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.coupons.noCodes}</p></Card>
      ) : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                  <th className="pb-2 pr-4">{t.coupons.code}</th>
                  <th className="pb-2 pr-4">{t.coupons.discount}</th>
                  <th className="pb-2 pr-4">{t.coupons.usage}</th>
                  <th className="pb-2 pr-4">{t.common.status}</th>
                  <th className="pb-2">{t.common.actions}</th>
                </tr>
              </thead>
              <tbody>
                {codes.map((code) => (
                  <tr key={code.id} className="border-b border-gray-100">
                    <td className="py-2 pr-4 font-medium text-gray-900">{code.code}</td>
                    <td className="py-2 pr-4 text-gray-900">{code.type === 'PERCENT' ? `${code.value}%` : `${code.value} MMK`}{code.minSpend ? ` (min ${code.minSpend})` : ''}</td>
                    <td className="py-2 pr-4 text-gray-600">{code.usedCount}/{code.maxUses ?? '∞'}</td>
                    <td className="py-2 pr-4"><span className={`px-2 py-0.5 rounded text-xs font-medium ${code.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{code.status}</span></td>
                    <td className="py-2 space-x-2">
                      <Button size="sm" variant="secondary" onClick={() => toggle(code)}>{code.status === 'ACTIVE' ? t.common.deactivate : t.common.activate}</Button>
                      <Button size="sm" variant="danger" onClick={() => remove(code)}>{t.common.delete}</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <Modal open={showForm} onClose={() => setShowForm(false)} title={t.coupons.createCode}>
        <div className="space-y-4">
          <Input label={t.coupons.code} value={form.code} onChange={(e) => set('code', e.target.value)} placeholder="TEA10" />
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t.coupons.type}</label>
              <select value={form.type} onChange={(e) => set('type', e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                <option value="PERCENT">PERCENT</option>
                <option value="FIXED">FIXED</option>
              </select>
            </div>
            <Input label={t.coupons.value} type="number" value={form.value} onChange={(e) => set('value', e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.coupons.minSpend} type="number" value={form.minSpend} onChange={(e) => set('minSpend', e.target.value)} />
            <Input label={t.coupons.maxUses} type="number" value={form.maxUses} onChange={(e) => set('maxUses', e.target.value)} />
          </div>
          <Input label={t.coupons.validTo} type="date" value={form.validTo} onChange={(e) => set('validTo', e.target.value)} />
          <div className="flex space-x-3">
            <Button onClick={handleSave} loading={submitting} disabled={!form.code || !form.value} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
