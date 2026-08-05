import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { storeApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import type { Store } from '../../types';

const emptyForm = { name: '', address: '', city: '', phone: '' };

export function StoresPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [stores, setStores] = useState<Store[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Store | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setStores(await storeApi.getStores(user.id));
    } catch (err) {
      console.error('Failed to load stores', err);
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

  const openEdit = (store: Store) => {
    setEditing(store);
    setForm({ name: store.name, address: store.address || '', city: store.city || '', phone: store.phone || '' });
    setShowForm(true);
  };

  const handleSave = async () => {
    if (!user || !form.name) return;
    setSubmitting(true);
    try {
      if (editing) {
        await storeApi.updateStore(user.id, editing.id, form);
        toast.success(t.stores.updated);
      } else {
        await storeApi.createStore(user.id, form);
        toast.success(t.stores.created);
      }
      setShowForm(false);
      await load();
    } catch (err) {
      console.error('Failed to save store', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const set = (key: keyof typeof emptyForm, value: string) => setForm((f) => ({ ...f, [key]: value }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.stores.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.stores.subtitle}</p>
        </div>
        <Button onClick={openCreate}>{t.stores.addStore}</Button>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : stores.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.stores.noStores}</p></Card>
      ) : (
        <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
          {stores.map((store) => (
            <Card key={store.id}>
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold text-gray-900">{store.name}</h3>
                  <p className="text-sm text-gray-500 mt-1">{store.city}{store.address ? ` · ${store.address}` : ''}</p>
                  {store.phone && <p className="text-sm text-gray-500">{store.phone}</p>}
                </div>
                <span className="px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700">{store.status}</span>
              </div>
              <div className="mt-4 flex justify-end">
                <Button size="sm" variant="ghost" onClick={() => openEdit(store)}>{t.common.edit}</Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={showForm} onClose={() => setShowForm(false)} title={editing ? t.stores.editStore : t.stores.addStore}>
        <div className="space-y-4">
          <Input label={t.stores.name} value={form.name} onChange={(e) => set('name', e.target.value)} />
          <Input label={t.stores.address} value={form.address} onChange={(e) => set('address', e.target.value)} />
          <Input label={t.stores.city} value={form.city} onChange={(e) => set('city', e.target.value)} />
          <Input label={t.stores.phone} value={form.phone} onChange={(e) => set('phone', e.target.value)} />
          <div className="flex space-x-3">
            <Button onClick={handleSave} loading={submitting} disabled={!form.name} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
