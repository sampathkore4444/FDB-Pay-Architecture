import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { fraudRuleApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import type { FraudRule } from '../../types';

const emptyForm = { name: '', ruleType: 'MAX_AMOUNT', threshold: '' };

export function FraudRulesPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [rules, setRules] = useState<FraudRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setRules(await fraudRuleApi.list(user.id));
    } catch (err) {
      console.error('Failed to load fraud rules', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const handleSave = async () => {
    if (!user || !form.name || !form.threshold) return;
    setSubmitting(true);
    try {
      await fraudRuleApi.create(user.id, { name: form.name, ruleType: form.ruleType, threshold: Number(form.threshold) });
      toast.success(t.fraudRules.created);
      setShowForm(false);
      setForm(emptyForm);
      await load();
    } catch (err) {
      console.error('Failed to create fraud rule', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const toggle = async (rule: FraudRule) => {
    if (!user) return;
    try {
      await fraudRuleApi.toggle(user.id, rule.id);
      await load();
    } catch (err) {
      console.error('Failed to toggle rule', err);
      toast.error(t.common.loadFailed);
    }
  };

  const remove = async (rule: FraudRule) => {
    if (!user || !window.confirm(t.fraudRules.deleteConfirm)) return;
    try {
      await fraudRuleApi.delete(user.id, rule.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to delete rule', err);
      toast.error(t.common.loadFailed);
    }
  };

  const set = (key: keyof typeof emptyForm, value: string) => setForm((f) => ({ ...f, [key]: value }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.fraudRules.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.fraudRules.subtitle}</p>
        </div>
        <Button onClick={() => { setForm(emptyForm); setShowForm(true); }}>{t.fraudRules.createRule}</Button>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : rules.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.fraudRules.noRules}</p></Card>
      ) : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                  <th className="pb-2 pr-4">{t.fraudRules.name}</th>
                  <th className="pb-2 pr-4">{t.fraudRules.ruleType}</th>
                  <th className="pb-2 pr-4">{t.fraudRules.threshold}</th>
                  <th className="pb-2 pr-4">{t.common.status}</th>
                  <th className="pb-2">{t.common.actions}</th>
                </tr>
              </thead>
              <tbody>
                {rules.map((rule) => (
                  <tr key={rule.id} className="border-b border-gray-100">
                    <td className="py-2 pr-4 font-medium text-gray-900">{rule.name}</td>
                    <td className="py-2 pr-4 text-gray-600">{rule.ruleType === 'MAX_AMOUNT' ? t.fraudRules.maxAmount : t.fraudRules.maxVelocity}</td>
                    <td className="py-2 pr-4 text-gray-600">{rule.threshold.toLocaleString()} MMK</td>
                    <td className="py-2 pr-4"><span className={`px-2 py-0.5 rounded text-xs font-medium ${rule.enabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{rule.enabled ? t.common.active : t.common.inactive}</span></td>
                    <td className="py-2 space-x-2">
                      <Button size="sm" variant="secondary" onClick={() => toggle(rule)}>{rule.enabled ? t.common.deactivate : t.common.activate}</Button>
                      <Button size="sm" variant="danger" onClick={() => remove(rule)}>{t.common.delete}</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <Modal open={showForm} onClose={() => setShowForm(false)} title={t.fraudRules.createRule}>
        <div className="space-y-4">
          <Input label={t.fraudRules.name} value={form.name} onChange={(e) => set('name', e.target.value)} placeholder={t.fraudRules.namePlaceholder} />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.fraudRules.ruleType}</label>
            <select value={form.ruleType} onChange={(e) => set('ruleType', e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
              <option value="MAX_AMOUNT">{t.fraudRules.maxAmount}</option>
              <option value="MAX_VELOCITY">{t.fraudRules.maxVelocity}</option>
            </select>
          </div>
          <Input label={t.fraudRules.threshold} type="number" value={form.threshold} onChange={(e) => set('threshold', e.target.value)} />
          <div className="flex space-x-3">
            <Button onClick={handleSave} loading={submitting} disabled={!form.name || !form.threshold} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
