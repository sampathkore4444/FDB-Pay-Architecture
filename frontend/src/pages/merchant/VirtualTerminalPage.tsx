import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { merchantApi, merchantOpsApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency } from '../../utils';
import type { Merchant, TerminalFieldOption } from '../../types';

const DEFAULT_FIELDS: TerminalFieldOption[] = [
  { key: 'customerPhone', label: 'Customer Phone', enabled: true, required: true },
  { key: 'customerName', label: 'Customer Name', enabled: true, required: false },
  { key: 'cardLast4', label: 'Card Last 4', enabled: true, required: true },
  { key: 'description', label: 'Description', enabled: true, required: false },
];

const emptyForm = { customerPhone: '', customerName: '', cardLast4: '', amount: '', description: '' };

export function VirtualTerminalPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [fields, setFields] = useState<TerminalFieldOption[]>(DEFAULT_FIELDS);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [result, setResult] = useState<any>(null);

  useEffect(() => {
    if (!user) return;
    merchantApi
      .getProfile(user.id)
      .then((m) => {
        setMerchant(m);
        if (m.terminalFields) {
          try {
            const parsed = JSON.parse(m.terminalFields);
            if (Array.isArray(parsed) && parsed.length) setFields(parsed);
          } catch {
            /* keep defaults */
          }
        }
      })
      .catch(() => toast.error(t.common.loadFailed));
  }, [user]);

  const isVisible = (key: string) => fields.find((f) => f.key === key)?.enabled ?? true;

  const handleCharge = async () => {
    if (!user || !merchant) return;
    setSubmitting(true);
    setResult(null);
    try {
      const res = await merchantOpsApi.charge(merchant.userId, {
        customerPhone: form.customerPhone,
        customerName: form.customerName.trim() || undefined,
        cardLast4: form.cardLast4,
        amount: Number(form.amount),
        description: form.description.trim() || undefined,
      });
      setResult(res);
      toast.success(t.virtualTerminal.chargeSuccess);
      setForm((f) => ({ ...f, amount: '', description: '' }));
    } catch (err) {
      console.error('Charge failed', err);
      toast.error(t.virtualTerminal.chargeFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleField = (key: string, patch: Partial<TerminalFieldOption>) =>
    setFields((fs) => fs.map((f) => (f.key === key ? { ...f, ...patch } : f)));

  const handleSaveFields = async () => {
    if (!merchant) return;
    setSaving(true);
    try {
      await merchantApi.updateTerminalFields(merchant.id, JSON.stringify(fields));
      toast.success(t.common.success);
    } catch (err) {
      console.error('Save fields failed', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSaving(false);
    }
  };

  const canSubmit =
    (isVisible('customerPhone') ? form.customerPhone : true) &&
    (isVisible('cardLast4') ? form.cardLast4 : true) &&
    form.amount;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.virtualTerminal.title}</h1>

      <Card title={t.virtualTerminal.payment}>
        <div className="grid grid-cols-2 gap-4">
          {isVisible('customerPhone') && (
            <Input label={t.paymentLinks.customerPhone} value={form.customerPhone} onChange={(e) => setForm((f) => ({ ...f, customerPhone: e.target.value }))} />
          )}
          {isVisible('customerName') && (
            <Input label={t.paymentLinks.customerName} value={form.customerName} onChange={(e) => setForm((f) => ({ ...f, customerName: e.target.value }))} />
          )}
          {isVisible('cardLast4') && (
            <Input label={t.virtualTerminal.cardLast4} placeholder="4242" maxLength={4} value={form.cardLast4} onChange={(e) => setForm((f) => ({ ...f, cardLast4: e.target.value.replace(/\D/g, '') }))} />
          )}
          {isVisible('description') && (
            <Input label={t.virtualTerminal.description} value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />
          )}
          <Input type="number" label={t.virtualTerminal.amount} value={form.amount} onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value }))} />
        </div>
        <div className="mt-4 flex items-center justify-between">
          {result ? (
            <div className="text-sm bg-green-50 border border-green-200 text-green-700 rounded-lg px-4 py-2">
              {t.virtualTerminal.paid} <b>{formatCurrency(result.amount)}</b> · {result.id.slice(0, 8)}
            </div>
          ) : (
            <span />
          )}
          <Button onClick={handleCharge} loading={submitting} disabled={!canSubmit}>
            {t.virtualTerminal.charge}
          </Button>
        </div>
      </Card>

      <Card title={t.virtualTerminal.customizeFields}>
        <div className="divide-y divide-gray-100">
          {fields.map((f) => (
            <div key={f.key} className="flex items-center justify-between py-3">
              <span className="text-sm text-gray-700">{f.label}</span>
              <div className="flex items-center space-x-4 text-sm">
                <label className="flex items-center space-x-1.5">
                  <input type="checkbox" checked={f.enabled} onChange={(e) => toggleField(f.key, { enabled: e.target.checked, required: e.target.checked ? f.required : false })} className="h-4 w-4 rounded border-gray-300" />
                  <span>{t.virtualTerminal.enabled}</span>
                </label>
                <label className="flex items-center space-x-1.5">
                  <input type="checkbox" checked={f.required} disabled={!f.enabled} onChange={(e) => toggleField(f.key, { required: e.target.checked })} className="h-4 w-4 rounded border-gray-300" />
                  <span>{t.virtualTerminal.required}</span>
                </label>
              </div>
            </div>
          ))}
        </div>
        <div className="mt-4 flex justify-end">
          <Button variant="secondary" onClick={handleSaveFields} loading={saving}>
            {t.common.save}
          </Button>
        </div>
      </Card>
    </div>
  );
}
