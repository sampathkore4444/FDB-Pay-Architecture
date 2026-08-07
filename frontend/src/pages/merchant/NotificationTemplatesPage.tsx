import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { notificationTemplateApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import type { NotificationTemplate } from '../../types';

const emptyForm = { name: '', channel: 'SMS', subject: '', body: '', triggerEvent: 'PAYMENT_LINK_REMINDER', enabled: true };
const channels = ['SMS', 'EMAIL', 'PUSH'];
const events = ['PAYMENT_LINK_REMINDER', 'ORDER_CONFIRMATION', 'REFUND_ISSUED', 'PAYMENT_RECEIVED'];

export function NotificationTemplatesPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [templates, setTemplates] = useState<NotificationTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<NotificationTemplate | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setTemplates(await notificationTemplateApi.list(user.id));
    } catch (err) {
      console.error('Failed to load templates', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setShowForm(true);
  };

  const openEdit = (template: NotificationTemplate) => {
    setEditing(template);
    setForm({
      name: template.name,
      channel: template.channel,
      subject: template.subject || '',
      body: template.body,
      triggerEvent: template.triggerEvent,
      enabled: template.enabled,
    });
    setShowForm(true);
  };

  const submit = async () => {
    if (!user || !form.name || !form.body) return;
    setSubmitting(true);
    try {
      if (editing) {
        await notificationTemplateApi.update(user.id, editing.id, form);
        toast.success(t.notificationTemplates.updated);
      } else {
        await notificationTemplateApi.create(user.id, form);
        toast.success(t.notificationTemplates.created);
      }
      setShowForm(false);
      await load();
    } catch (err) {
      console.error('Failed to save template', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const remove = async (template: NotificationTemplate) => {
    if (!user || !window.confirm(t.notificationTemplates.deleteConfirm)) return;
    try {
      await notificationTemplateApi.delete(user.id, template.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to delete template', err);
      toast.error(t.common.loadFailed);
    }
  };

  const toggleEnabled = async (template: NotificationTemplate) => {
    if (!user) return;
    try {
      await notificationTemplateApi.update(user.id, template.id, { enabled: !template.enabled });
      await load();
    } catch (err) {
      console.error('Failed to toggle template', err);
      toast.error(t.common.loadFailed);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.notificationTemplates.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.notificationTemplates.subtitle}</p>
        </div>
        <Button onClick={openCreate}>{t.notificationTemplates.newTemplate}</Button>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : templates.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.notificationTemplates.noTemplates}</p></Card>
      ) : (
        <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
          {templates.map((template) => (
            <Card key={template.id}>
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold text-gray-900">{template.name}</h3>
                  <p className="text-sm text-gray-400 mt-1">{template.triggerEvent}</p>
                </div>
                <span className={`px-2 py-0.5 rounded text-xs font-medium ${template.enabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
                  {template.enabled ? t.common.active : t.common.inactive}
                </span>
              </div>
              <p className="text-sm text-gray-500 mt-2 line-clamp-3">{template.body}</p>
              <div className="mt-4 flex justify-end space-x-2">
                <Button size="sm" variant="secondary" onClick={() => toggleEnabled(template)}>{template.enabled ? t.common.deactivate : t.common.activate}</Button>
                <Button size="sm" variant="ghost" onClick={() => openEdit(template)}>{t.common.edit}</Button>
                <Button size="sm" variant="danger" onClick={() => remove(template)}>{t.common.delete}</Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={showForm} onClose={() => setShowForm(false)} title={editing ? t.common.edit : t.notificationTemplates.newTemplate}>
        <div className="space-y-4">
          <Input label={t.notificationTemplates.name} value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
          <div className="grid grid-cols-2 gap-4">
            <select
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={form.channel}
              onChange={(e) => setForm((f) => ({ ...f, channel: e.target.value }))}
            >
              {channels.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
            <select
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={form.triggerEvent}
              onChange={(e) => setForm((f) => ({ ...f, triggerEvent: e.target.value }))}
            >
              {events.map((ev) => <option key={ev} value={ev}>{ev}</option>)}
            </select>
          </div>
          <Input label={t.notificationTemplates.subject} value={form.subject} onChange={(e) => setForm((f) => ({ ...f, subject: e.target.value }))} />
          <Input label={t.notificationTemplates.body} placeholder={t.notificationTemplates.bodyPlaceholder} value={form.body} onChange={(e) => setForm((f) => ({ ...f, body: e.target.value }))} />
          <label className="flex items-center space-x-2 text-sm text-gray-700">
            <input type="checkbox" checked={form.enabled} onChange={(e) => setForm((f) => ({ ...f, enabled: e.target.checked }))} className="rounded" />
            <span>{t.notificationTemplates.enabled}</span>
          </label>
          <div className="flex space-x-3">
            <Button onClick={submit} loading={submitting} disabled={!form.name || !form.body} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
