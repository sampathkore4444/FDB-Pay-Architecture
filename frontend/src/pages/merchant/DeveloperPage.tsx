import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { developerApi, webhookApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import type { ApiKey, ReportTemplate, WebhookSubscription, WebhookDelivery } from '../../types';

const emptyTemplate = { name: '', reportType: 'SUMMARY', frequency: 'DAILY', format: 'PDF', email: '' };
const emptyWebhook = { event: 'CHARGE_SUCCEEDED', url: '' };

export function DeveloperPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [templates, setTemplates] = useState<ReportTemplate[]>([]);
  const [webhooks, setWebhooks] = useState<WebhookSubscription[]>([]);
  const [deliveries, setDeliveries] = useState<WebhookDelivery[]>([]);
  const [loading, setLoading] = useState(true);
  const [showKeyForm, setShowKeyForm] = useState(false);
  const [keyName, setKeyName] = useState('');
  const [keyEnvironment, setKeyEnvironment] = useState('LIVE');
  const [showTemplateForm, setShowTemplateForm] = useState(false);
  const [templateForm, setTemplateForm] = useState(emptyTemplate);
  const [showWebhookForm, setShowWebhookForm] = useState(false);
  const [webhookForm, setWebhookForm] = useState(emptyWebhook);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const [k, tpl, wh, del] = await Promise.all([
        developerApi.listApiKeys(user.id),
        developerApi.listReportTemplates(user.id),
        webhookApi.list(user.id),
        webhookApi.deliveries(user.id),
      ]);
      setKeys(k);
      setTemplates(tpl);
      setWebhooks(wh);
      setDeliveries(del);
    } catch (err) {
      console.error('Failed to load developer data', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const createKey = async () => {
    if (!user || !keyName.trim()) return;
    setSubmitting(true);
    try {
      const key = await developerApi.createApiKey(user.id, keyName.trim(), keyEnvironment);
      toast.success(t.developer.keyCreated, { description: key.keyPreview });
      setShowKeyForm(false);
      setKeyName('');
      await load();
    } catch (err) {
      console.error('Failed to create API key', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const revokeKey = async (key: ApiKey) => {
    if (!user || !window.confirm(t.developer.revokeConfirm)) return;
    try {
      await developerApi.revokeApiKey(user.id, key.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to revoke key', err);
      toast.error(t.common.loadFailed);
    }
  };

  const recordUsage = async (key: ApiKey) => {
    if (!user) return;
    try {
      await developerApi.recordUsage(user.id, key.id);
      await load();
    } catch (err) {
      console.error('Failed to record usage', err);
      toast.error(t.common.loadFailed);
    }
  };

  const createTemplate = async () => {
    if (!user || !templateForm.name) return;
    setSubmitting(true);
    try {
      await developerApi.createReportTemplate(user.id, {
        name: templateForm.name,
        reportType: templateForm.reportType,
        frequency: templateForm.frequency,
        format: templateForm.format,
        email: templateForm.email || undefined,
      });
      toast.success(t.developer.templateCreated);
      setShowTemplateForm(false);
      setTemplateForm(emptyTemplate);
      await load();
    } catch (err) {
      console.error('Failed to create report template', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const removeTemplate = async (template: ReportTemplate) => {
    if (!user || !window.confirm(t.developer.deleteTemplateConfirm)) return;
    try {
      await developerApi.deleteReportTemplate(user.id, template.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to delete template', err);
      toast.error(t.common.loadFailed);
    }
  };

  const createWebhook = async () => {
    if (!user || !webhookForm.url.trim()) return;
    setSubmitting(true);
    try {
      await webhookApi.create(user.id, webhookForm);
      toast.success(t.developer.webhookCreated);
      setShowWebhookForm(false);
      setWebhookForm(emptyWebhook);
      await load();
    } catch (err) {
      console.error('Failed to create webhook', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleWebhook = async (sub: WebhookSubscription) => {
    if (!user) return;
    try {
      await webhookApi.toggle(user.id, sub.id);
      await load();
    } catch (err) {
      console.error('Failed to toggle webhook', err);
      toast.error(t.common.loadFailed);
    }
  };

  const testWebhook = async (sub: WebhookSubscription) => {
    if (!user) return;
    try {
      await webhookApi.test(user.id, sub.id);
      toast.success(t.developer.webhookTested);
      await load();
    } catch (err) {
      console.error('Failed to test webhook', err);
      toast.error(t.common.loadFailed);
    }
  };

  const removeWebhook = async (sub: WebhookSubscription) => {
    if (!user || !window.confirm(t.developer.deleteWebhookConfirm)) return;
    try {
      await webhookApi.delete(user.id, sub.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to delete webhook', err);
      toast.error(t.common.loadFailed);
    }
  };

  const replayDelivery = async (delivery: WebhookDelivery) => {
    if (!user) return;
    try {
      await webhookApi.replay(user.id, delivery.id);
      toast.success(t.developer.webhookReplayed);
      await load();
    } catch (err) {
      console.error('Failed to replay delivery', err);
      toast.error(t.common.loadFailed);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t.developer.title}</h1>
        <p className="text-sm text-gray-500 mt-1">{t.developer.subtitle}</p>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : (
        <>
          <Card title={t.developer.apiKeys} subtitle={t.developer.apiKeysSubtitle}>
            <div className="flex justify-end mb-3">
              <Button size="sm" onClick={() => setShowKeyForm(true)}>{t.developer.createKey}</Button>
            </div>
            {keys.length === 0 ? (
              <p className="text-center text-gray-500 py-6">{t.developer.noKeys}</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                      <th className="pb-2 pr-4">{t.developer.name}</th>
                      <th className="pb-2 pr-4">{t.developer.key}</th>
                      <th className="pb-2 pr-4">{t.developer.environment}</th>
                      <th className="pb-2 pr-4">{t.developer.usage}</th>
                      <th className="pb-2 pr-4">{t.common.status}</th>
                      <th className="pb-2">{t.common.actions}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {keys.map((key) => (
                      <tr key={key.id} className="border-b border-gray-100">
                        <td className="py-2 pr-4 font-medium text-gray-900">{key.name}</td>
                        <td className="py-2 pr-4 font-mono text-xs text-gray-500">{key.keyPreview ?? key.id.slice(0, 12)}…</td>
                        <td className="py-2 pr-4"><span className={`px-2 py-0.5 rounded text-xs font-medium ${key.environment === 'SANDBOX' ? 'bg-yellow-100 text-yellow-700' : 'bg-green-100 text-green-700'}`}>{key.environment ?? 'LIVE'}</span></td>
                        <td className="py-2 pr-4 text-gray-600">{key.usageCount ?? 0}</td>
                        <td className="py-2 pr-4"><span className={`px-2 py-0.5 rounded text-xs font-medium ${key.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{key.status}</span></td>
                        <td className="py-2 space-x-2">
                          {key.status === 'ACTIVE' && (
                            <>
                              <Button size="sm" variant="secondary" onClick={() => recordUsage(key)}>+1</Button>
                              <Button size="sm" variant="danger" onClick={() => revokeKey(key)}>{t.developer.revoke}</Button>
                            </>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card title={t.developer.webhooks} subtitle={t.developer.webhooksSubtitle}>
            <div className="flex justify-end mb-3">
              <Button size="sm" onClick={() => { setWebhookForm(emptyWebhook); setShowWebhookForm(true); }}>{t.developer.subscribe}</Button>
            </div>
            {webhooks.length === 0 ? (
              <p className="text-center text-gray-500 py-6">{t.developer.noWebhooks}</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                      <th className="pb-2 pr-4">{t.developer.event}</th>
                      <th className="pb-2 pr-4">{t.developer.url}</th>
                      <th className="pb-2 pr-4">{t.common.status}</th>
                      <th className="pb-2">{t.common.actions}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {webhooks.map((sub) => (
                      <tr key={sub.id} className="border-b border-gray-100">
                        <td className="py-2 pr-4 font-medium text-gray-900">{sub.event}</td>
                        <td className="py-2 pr-4 text-gray-500 break-all">{sub.url}</td>
                        <td className="py-2 pr-4"><span className={`px-2 py-0.5 rounded text-xs font-medium ${sub.enabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{sub.enabled ? t.common.active : t.common.inactive}</span></td>
                        <td className="py-2 space-x-2">
                          <Button size="sm" variant="secondary" onClick={() => testWebhook(sub)}>{t.developer.test}</Button>
                          <Button size="sm" variant="secondary" onClick={() => toggleWebhook(sub)}>{sub.enabled ? t.common.deactivate : t.common.activate}</Button>
                          <Button size="sm" variant="danger" onClick={() => removeWebhook(sub)}>{t.common.delete}</Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card title={t.developer.deliveryLog} subtitle={t.developer.deliveryLogSubtitle}>
            {deliveries.length === 0 ? (
              <p className="text-center text-gray-500 py-6">{t.developer.noDeliveries}</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                      <th className="pb-2 pr-4">{t.developer.event}</th>
                      <th className="pb-2 pr-4">{t.developer.url}</th>
                      <th className="pb-2 pr-4">{t.common.status}</th>
                      <th className="pb-2 pr-4">{t.developer.attempts}</th>
                      <th className="pb-2 pr-4">{t.developer.time}</th>
                      <th className="pb-2">{t.common.actions}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {deliveries.map((delivery) => (
                      <tr key={delivery.id} className="border-b border-gray-100">
                        <td className="py-2 pr-4 font-medium text-gray-900">{delivery.event}</td>
                        <td className="py-2 pr-4 text-gray-500 break-all max-w-xs">{delivery.url}</td>
                        <td className="py-2 pr-4">
                          <span className={`px-2 py-0.5 rounded text-xs font-medium ${delivery.status === 'SUCCESS' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>{delivery.status}{delivery.statusCode ? ` · ${delivery.statusCode}` : ''}</span>
                        </td>
                        <td className="py-2 pr-4 text-gray-600">{delivery.attempts ?? 1}</td>
                        <td className="py-2 pr-4 text-gray-500">{delivery.deliveredAt ? new Date(delivery.deliveredAt).toLocaleString() : '—'}</td>
                        <td className="py-2">
                          {delivery.status === 'FAILED' && <Button size="sm" variant="secondary" onClick={() => replayDelivery(delivery)}>{t.developer.replay}</Button>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card title={t.developer.reportTemplates} subtitle={t.developer.reportTemplatesSubtitle}>
            <div className="flex justify-end mb-3">
              <Button size="sm" onClick={() => { setTemplateForm(emptyTemplate); setShowTemplateForm(true); }}>{t.developer.newTemplate}</Button>
            </div>
            {templates.length === 0 ? (
              <p className="text-center text-gray-500 py-6">{t.developer.noTemplates}</p>
            ) : (
              <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
                {templates.map((template) => (
                  <div key={template.id} className="border border-gray-200 rounded-lg p-4">
                    <div className="flex items-start justify-between">
                      <h4 className="font-semibold text-gray-900">{template.name}</h4>
                      <span className={`px-2 py-0.5 rounded text-xs font-medium ${template.enabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{template.enabled ? t.common.active : t.common.inactive}</span>
                    </div>
                    <p className="text-sm text-gray-500 mt-2">{template.reportType} · {template.frequency} · {template.format}</p>
                    {template.email && <p className="text-xs text-gray-400 mt-1">{template.email}</p>}
                    <div className="mt-3 flex justify-end">
                      <Button size="sm" variant="danger" onClick={() => removeTemplate(template)}>{t.common.delete}</Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </>
      )}

      <Modal open={showKeyForm} onClose={() => setShowKeyForm(false)} title={t.developer.createKey}>
        <div className="space-y-4">
          <Input label={t.developer.name} value={keyName} onChange={(e) => setKeyName(e.target.value)} />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.developer.environment}</label>
            <select value={keyEnvironment} onChange={(e) => setKeyEnvironment(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
              <option value="LIVE">LIVE</option>
              <option value="SANDBOX">SANDBOX</option>
            </select>
          </div>
          <div className="flex space-x-3">
            <Button onClick={createKey} loading={submitting} disabled={!keyName.trim()} className="flex-1">{t.common.create}</Button>
            <Button variant="secondary" onClick={() => setShowKeyForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={showWebhookForm} onClose={() => setShowWebhookForm(false)} title={t.developer.subscribe}>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.developer.event}</label>
            <select value={webhookForm.event} onChange={(e) => setWebhookForm((f) => ({ ...f, event: e.target.value }))} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
              <option value="CHARGE_SUCCEEDED">CHARGE_SUCCEEDED</option>
              <option value="CHARGE_FAILED">CHARGE_FAILED</option>
              <option value="REFUND_SUCCEEDED">REFUND_SUCCEEDED</option>
              <option value="SETTLEMENT_COMPLETED">SETTLEMENT_COMPLETED</option>
              <option value="CUSTOMER_CREATED">CUSTOMER_CREATED</option>
            </select>
          </div>
          <Input label={t.developer.url} value={webhookForm.url} onChange={(e) => setWebhookForm((f) => ({ ...f, url: e.target.value }))} placeholder="https://your-server.com/webhooks/fdb" />
          <div className="flex space-x-3">
            <Button onClick={createWebhook} loading={submitting} disabled={!webhookForm.url.trim()} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowWebhookForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={showTemplateForm} onClose={() => setShowTemplateForm(false)} title={t.developer.newTemplate}>
        <div className="space-y-4">
          <Input label={t.developer.name} value={templateForm.name} onChange={(e) => setTemplateForm((f) => ({ ...f, name: e.target.value }))} />
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t.developer.reportType}</label>
              <select value={templateForm.reportType} onChange={(e) => setTemplateForm((f) => ({ ...f, reportType: e.target.value }))} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                <option value="SUMMARY">SUMMARY</option>
                <option value="TRANSACTIONS">TRANSACTIONS</option>
                <option value="SETTLEMENTS">SETTLEMENTS</option>
                <option value="RECONCILIATION">RECONCILIATION</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t.developer.frequency}</label>
              <select value={templateForm.frequency} onChange={(e) => setTemplateForm((f) => ({ ...f, frequency: e.target.value }))} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                <option value="DAILY">DAILY</option>
                <option value="WEEKLY">WEEKLY</option>
                <option value="MONTHLY">MONTHLY</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t.developer.format}</label>
              <select value={templateForm.format} onChange={(e) => setTemplateForm((f) => ({ ...f, format: e.target.value }))} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                <option value="PDF">PDF</option>
                <option value="CSV">CSV</option>
                <option value="EXCEL">EXCEL</option>
              </select>
            </div>
          </div>
          <Input label={t.developer.email} type="email" value={templateForm.email} onChange={(e) => setTemplateForm((f) => ({ ...f, email: e.target.value }))} />
          <div className="flex space-x-3">
            <Button onClick={createTemplate} loading={submitting} disabled={!templateForm.name} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowTemplateForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
